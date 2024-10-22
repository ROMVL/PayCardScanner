package lens24.camera.widget

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.view.children

class PreviewLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : FrameLayout(context, attrs, defStyle) {

    val surfaceView by lazy(LazyThreadSafetyMode.NONE) { SurfaceView(context) }

    @ColorInt
    var idleScanCardOverlayColor: Int = ContextCompat.getColor(context, android.R.color.white)

    @ColorInt
    var successScanCardOverlayColor: Int = ContextCompat.getColor(context, android.R.color.holo_green_light)

    private var overlayView: CardOverlayView? = null
        get() = requireNotNull(field)

    init {
        addView(surfaceView)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        children.find { it is CardOverlayView }
            ?.let { overlayView = it as CardOverlayView } ?: run {
                overlayView = ScanCardOverlayView(context)
                addView(overlayView)
            }
    }

    fun setSuccessRecognitionState() = overlayView?.setCornersColor(successScanCardOverlayColor)

    fun setIdleRecognitionState() = overlayView?.setCornersColor(idleScanCardOverlayColor)
}