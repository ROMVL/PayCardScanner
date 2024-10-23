package paycardscanner.utils

import android.graphics.Bitmap
import android.hardware.Camera
import paycardscanner.camera.CardScanManager
import paycardscanner.ndk.RecognitionResult

fun cardScanManagerListener(
    onFlashAvailable: (Boolean) -> Unit,
    onResult: (RecognitionResult) -> Unit,
    onTorchStatusChanged: (Boolean) -> Unit,
) = object : CardScanManager.Callbacks {

    override fun onRecognitionComplete(result: RecognitionResult?) {
        result?.let { onResult.invoke(it) }
    }

    override fun onTorchStatusChanged(turnTorchOn: Boolean) = onTorchStatusChanged.invoke(turnTorchOn)

    override fun onCameraOpened(cameraParameters: Camera.Parameters?) {
        onFlashAvailable.invoke(cameraParameters?.getSupportedFlashModes()?.isNotEmpty() ?: false)
    }

    override fun onOpenCameraError(exception: Exception?) = Unit
    override fun onCardImageReceived(bitmap: Bitmap?) = Unit
    override fun onFpsReport(report: String?) = Unit
    override fun onAutoFocusMoving(start: Boolean, cameraFocusMode: String?) = Unit
    override fun onAutoFocusComplete(success: Boolean, cameraFocusMode: String?) = Unit
}
