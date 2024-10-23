package paycardscanner.camera

import android.content.Context
import android.graphics.Bitmap
import android.hardware.Camera
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import androidx.annotation.MainThread
import paycardscanner.camera.widget.PreviewLayout
import paycardscanner.ndk.DisplayConfigurationImpl
import paycardscanner.ndk.RecognitionConstants
import paycardscanner.ndk.RecognitionConstants.RecognitionMode
import paycardscanner.ndk.RecognitionCore
import paycardscanner.ndk.RecognitionResult
import paycardscanner.ndk.RecognitionStatusListener
import paycardscanner.utils.Constants
import java.util.Locale
import kotlin.math.sqrt

interface IScanManager {
    fun onResume()

    fun onPause()

    fun resumeScan()

    fun toggleFlash()

    fun resetResult()

    fun setRecognitionCoreIdle(idle: Boolean)

    @MainThread
    fun onCameraOpened(parameters: Camera.Parameters)

    @MainThread
    fun onOpenCameraError(e: Exception)

    @MainThread
    fun onRenderThreadError(e: Throwable)

    @MainThread
    fun onFrameProcessed(newBorders: Int)

    @MainThread
    fun onFpsReport(fpsReport: String?)

    @MainThread
    fun onAutoFocusMoving(isStart: Boolean, focusMode: String?)

    @MainThread
    fun onAutoFocusComplete(isSuccess: Boolean, focusMode: String?)

    fun freezeCameraPreview()

    fun unfreezeCameraPreview()
}

class CardScanManager(
    @RecognitionMode private val recognitionMode: Int = DEFAULT_RECOGNITION_MODE,
    private val context: Context,
    private val previewLayout: PreviewLayout,
    private val callbacks: Callbacks,
) : IScanManager {

    private val recognitionCore: RecognitionCore = RecognitionCore.getInstance(context)

    private val scanManagerHandler: ScanManagerHandler = ScanManagerHandler(this)

    private var renderThread: RenderThread? = null

    private var surfaceHolder: SurfaceHolder? = null

    private val windowRotationListener: WindowRotationListener

    private val displayConfiguration: DisplayConfigurationImpl

    private val surfaceView: SurfaceView
        get() = previewLayout.surfaceView

    private val display: Display
        get() = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> requireNotNull(context.display)
            else -> (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        }

    override fun onResume() {
        if (DBG) Log.d(TAG, "onResume()")
        renderThread = RenderThread(context, scanManagerHandler) { turnTorchOn: Boolean ->
            callbacks.onTorchStatusChanged(turnTorchOn)
        }.apply {
            name = "Camera thread"
            start()
            waitUntilReady()
        }

        surfaceHolder?.let { holder ->
            if (DBG) Log.d(TAG, "Sending previous surface")
            renderThread?.handler?.sendSurfaceAvailable(holder, false)
        } ?: run {
            if (DBG) Log.d(TAG, "No previous surface")
        }

        displayConfiguration.setCameraParameters(CameraUtils.getBackCameraSensorOrientation())

        recognitionCore.apply {
            setRecognitionMode(recognitionMode)
            setStatusListener(recognitionStatusListener)
            resetResult()
        }

        requireNotNull(renderThread?.handler).apply {
            sendOrientationChanged(CameraUtils.getBackCameraDataRotation(display))
            sendUnfreeze()
        }

        startShakeDetector()
        windowRotationListener.register(context, display) { refreshDisplayOrientation() }
        previewLayout.setIdleRecognitionState()
        setRecognitionCoreIdle(false)
    }

    override fun onPause() {
        if (DBG) Log.d(TAG, "onPause()")
        setRecognitionCoreIdle(true)
        stopShakeDetector()
        //previewLayout.setOnWindowFocusChangedListener(null)
        recognitionCore.setStatusListener(null)

        renderThread?.let { thread ->
            thread.handler.sendShutdown()
            runCatching {
                thread.join()
            }.onFailure {
                when (it) {
                    is InterruptedException -> callbacks.onOpenCameraError(it)
                }
            }
            renderThread = null
        }

        windowRotationListener.unregister()
    }

    override fun resumeScan() {
        setRecognitionCoreIdle(false)
    }

    override fun toggleFlash() {
        renderThread?.handler?.sendToggleFlash()
    }

    override fun resetResult() {
        if (DBG) Log.d(TAG, "resetResult()")
        recognitionCore.resetResult()
        renderThread?.handler?.sendResumeProcessFrames()
        unfreezeCameraPreview()
    }

    override fun setRecognitionCoreIdle(idle: Boolean) {
        if (DBG) Log.d(
            TAG,
            "setRecognitionCoreIdle() called with: idle = [$idle]"
        )
        recognitionCore.isIdle = idle

        when {
            idle -> renderThread?.handler?.sendPauseCamera()
            else -> renderThread?.handler?.sendResumeCamera()
        }
    }

    @MainThread
    override fun onCameraOpened(parameters: Camera.Parameters) {
        callbacks.onCameraOpened(parameters)
    }

    @MainThread
    override fun onOpenCameraError(e: Exception) {
        if (DBG) Log.d(
            TAG,
            "onOpenCameraError() called with: e = [$e]"
        )
        callbacks.onOpenCameraError(e)
        renderThread = null
    }

    @MainThread
    override fun onRenderThreadError(e: Throwable) {
        // XXX
        if (DBG) Log.d(
            TAG,
            "onRenderThreadError() called with: e = [$e]"
        )
        callbacks.onOpenCameraError(e as Exception)
        renderThread = null
    }

    @MainThread
    override fun onFrameProcessed(newBorders: Int) = Unit

    @MainThread
    override fun onFpsReport(fpsReport: String?) {
        callbacks.onFpsReport(fpsReport)
    }

    @MainThread
    override fun onAutoFocusMoving(isStart: Boolean, focusMode: String?) {
        callbacks.onAutoFocusMoving(isStart, focusMode)
    }

    @MainThread
    override fun onAutoFocusComplete(isSuccess: Boolean, focusMode: String?) {
        callbacks.onAutoFocusComplete(isSuccess, focusMode)
    }

    override fun freezeCameraPreview() {
        if (DBG) Log.d(TAG, "freezeCameraPreview() called with: " + "")
        renderThread?.handler?.sendFreeze()
    }

    override fun unfreezeCameraPreview() {
        if (DBG) Log.d(TAG, "unfreezeCameraPreview() called with: " + "")
        renderThread?.handler?.sendUnfreeze()
    }

    private fun startShakeDetector() {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (null != sensor) {
            sensorManager.registerListener(
                mShakeEventListener,
                sensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    private fun stopShakeDetector() {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.unregisterListener(mShakeEventListener)
    }

    private val recognitionStatusListener: RecognitionStatusListener =
        object : RecognitionStatusListener {
            private var recognitionCompleteTs: Long = 0

            override fun onRecognitionComplete(result: RecognitionResult) {
                this@CardScanManager.previewLayout.setSuccessRecognitionState()
                if (result.isFirst) {
                    renderThread?.handler?.sendPauseProcessFrames()
                    if (DBG) recognitionCompleteTs = System.nanoTime()
                }
                if (result.isFinal) {
                    val newTs = System.nanoTime()
                    if (DBG) Log.v(
                        TAG, String.format(
                            Locale.US, "Final result received after %.3f ms",
                            (newTs - recognitionCompleteTs) / 1000000f
                        )
                    )
                    freezeCameraPreview()
                }
                callbacks.onRecognitionComplete(result)
            }

            override fun onCardImageReceived(bitmap: Bitmap) {
                if (DBG) {
                    val newTs = System.nanoTime()
                    Log.v(
                        TAG, String.format(
                            Locale.US, "Card image received after %.3f ms",
                            (newTs - recognitionCompleteTs) / 1000000f
                        )
                    )
                }
                callbacks.onCardImageReceived(bitmap)
            }
        }

    private val mShakeEventListener: SensorEventListener = object : SensorEventListener {
        private val SHAKE_THRESHOLD = 3.3

        var lastUpdate: Long = 0
        val gravity: DoubleArray = DoubleArray(3)

        override fun onSensorChanged(event: SensorEvent) {
            val curTime = System.currentTimeMillis()
            // only allow one update every 100ms.
            val diffTime = (curTime - lastUpdate)
            if (500 < diffTime) {
                lastUpdate = curTime

                val alpha = 0.8f
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

                val x = event.values[0] - gravity[0]
                val y = event.values[1] - gravity[1]
                val z = event.values[2] - gravity[2]

                val speed = sqrt((x * x) + (y * y) + (z * z))

                if (SHAKE_THRESHOLD < speed) {
                    if (renderThread != null) {
                        if (DBG) Log.d(TAG, "shake focus request")
                        renderThread!!.handler.sendRequestFocus()
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        }
    }

    init {

        val display = display
        displayConfiguration = DisplayConfigurationImpl()
        displayConfiguration.setCameraParameters(CameraUtils.getBackCameraSensorOrientation())
        displayConfiguration.setDisplayParameters(display)
        recognitionCore.setDisplayConfiguration(displayConfiguration)

        val sh = surfaceView.holder
        sh.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                if (DBG) Log.d(
                    TAG,
                    "SurfaceView  surfaceCreated holder=$holder (static=$surfaceHolder)"
                )
                if (surfaceHolder != null) {
                    throw RuntimeException("sSurfaceHolder is already set")
                }

                surfaceHolder = holder

                if (renderThread != null) {
                    // Normal case -- render thread is running, tell it about the new surface.
                    renderThread?.handler?.sendSurfaceAvailable(holder, true)
                } else {
                    // Sometimes see this on 4.4.x N5: power off, power on, unlock, with device in
                    // landscape and a lock screen that requires portrait.  The surface-created
                    // message is showing up after onPause().
                    //
                    // Chances are good that the surface will be destroyed before the activity is
                    // unpaused, but we track it anyway.  If the activity is un-paused and we start
                    // the RenderThread, the SurfaceHolder will be passed in right after the thread
                    // is created.
                    if (DBG) Log.d(TAG, "render thread not running")
                }
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                if (DBG) Log.d(
                    TAG,
                    ("SurfaceView surfaceChanged fmt=" + format + " size=" + width + "x" + height +
                            " holder=" + holder)
                )

                if (renderThread != null) {
                    val rh = renderThread!!.handler
                    rh.sendSurfaceChanged(format, width, height)
                } else {
                    if (DBG) Log.d(TAG, "Ignoring surfaceChanged")
                }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                // In theory, we should tell the RenderThread that the surface has been destroyed.
                if (renderThread != null) {
                    val rh = renderThread!!.handler
                    rh.sendSurfaceDestroyed()
                }
                if (DBG) Log.d(
                    TAG,
                    "SurfaceView surfaceDestroyed holder=$holder"
                )
                surfaceHolder = null
            }
        })

        windowRotationListener = WindowRotationListener()
    }

    private fun refreshDisplayOrientation() {
        if (DBG) Log.d(TAG, "refreshDisplayOrientation()")

        displayConfiguration.setDisplayParameters(display)
        recognitionCore.setDisplayConfiguration(displayConfiguration)

        renderThread
            ?.handler
            ?.sendOrientationChanged(CameraUtils.getBackCameraDataRotation(display))
    }

    interface Callbacks {
        fun onCameraOpened(cameraParameters: Camera.Parameters?)
        fun onOpenCameraError(exception: Exception?)
        fun onRecognitionComplete(result: RecognitionResult?)
        fun onCardImageReceived(bitmap: Bitmap?)
        fun onFpsReport(report: String?)
        fun onAutoFocusMoving(start: Boolean, cameraFocusMode: String?)
        fun onAutoFocusComplete(success: Boolean, cameraFocusMode: String?)
        fun onTorchStatusChanged(turnTorchOn: Boolean)
    }

    companion object {
        private const val DEFAULT_RECOGNITION_MODE =
            (RecognitionConstants.RECOGNIZER_MODE_NUMBER or RecognitionConstants.RECOGNIZER_MODE_DATE
                    or RecognitionConstants.RECOGNIZER_MODE_NAME or RecognitionConstants.RECOGNIZER_MODE_GRAB_CARD_IMAGE)

        private val DBG = Constants.DEBUG

        private const val TAG = "ScanManager"
    }
}
