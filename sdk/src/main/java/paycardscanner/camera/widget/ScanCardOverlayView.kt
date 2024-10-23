package paycardscanner.camera.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import paycardscanner.sdk.R
import paycardscanner.utils.dpToPx

private const val CORNER_SIZE = 40f
private const val PATH_CORNER_RADIUS = 12f
private const val CUT_RECT_CORNER_RADIUS = 10f
private const val CORNER_STROKE_WIDTH = 4f
private const val RECT_CORNERS_ARRAY_SIZE = 8
private const val RECT_HORIZONTAL_MARGIN = 24

abstract class CardOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    abstract fun setCornersColor(@ColorInt color: Int)
}

class ScanCardOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : CardOverlayView(context, attrs, defStyle) {

    private val cornerSize = context.dpToPx(CORNER_SIZE)
    private val pathCornerRadius = context.dpToPx(PATH_CORNER_RADIUS)
    private val cutRectCornerRadius = context.dpToPx(CUT_RECT_CORNER_RADIUS)
    private val cornerStrokeWidth = context.dpToPx(CORNER_STROKE_WIDTH)
    private val cornerPadding = cornerStrokeWidth / 3f

    private val transparentColor = ContextCompat.getColor(context, R.color.paycardscanner_card_shadow_color)
    private val cornersColor = ContextCompat.getColor(context, android.R.color.white)
    private val rectCorners = FloatArray(RECT_CORNERS_ARRAY_SIZE) { cutRectCornerRadius }
    private val backgroundPath = Path()
    private val cornersPaint: Paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = cornerStrokeWidth
        isAntiAlias = true
        pathEffect = CornerPathEffect(pathCornerRadius)
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = cornersColor
    }

    private val centerY by lazy { (bottom - top) / 2f }

    private val centerRect: RectF by lazy {
        val leftSide = left + context.dpToPx(RECT_HORIZONTAL_MARGIN)
        val rightSide = right - context.dpToPx(RECT_HORIZONTAL_MARGIN)
        val height = (leftSide + rightSide) / 3.5f
        val topSide = centerY - height
        val bottomSide = centerY + height
        RectF(
            leftSide,
            topSide,
            rightSide,
            bottomSide,
        )
    }

    private val topLeftCornerPath by lazy {
        Path().apply {
            moveTo(centerRect.left - cornerPadding, centerRect.top + cornerSize)
            lineTo(centerRect.left - cornerPadding, centerRect.top - cornerPadding)
            lineTo(centerRect.left + cornerSize, centerRect.top - cornerPadding)
        }
    }

    private val topRightCornerPath by lazy {
        Path().apply {
            moveTo(centerRect.right - cornerSize, centerRect.top - cornerPadding)
            lineTo(centerRect.right + cornerPadding, centerRect.top - cornerPadding)
            lineTo(centerRect.right + cornerPadding, centerRect.top + cornerSize)
        }
    }

    private val bottomLeftCornerPath by lazy {
        Path().apply {
            moveTo(centerRect.left - cornerPadding, centerRect.bottom - cornerSize)
            lineTo(centerRect.left - cornerPadding, centerRect.bottom + cornerPadding)
            lineTo(centerRect.left + cornerSize, centerRect.bottom + cornerPadding)
        }
    }

    private val bottomRightCornerPath by lazy {
        Path().apply {
            moveTo(centerRect.right - cornerSize, centerRect.bottom + cornerPadding)
            lineTo(centerRect.right + cornerPadding, centerRect.bottom + cornerPadding)
            lineTo(centerRect.right + cornerPadding, centerRect.bottom - cornerSize)
        }
    }

    override fun onDraw(canvas: Canvas) = with(canvas) {
        super.onDraw(this)
        drawBackground()
        drawPath(topLeftCornerPath, cornersPaint)
        drawPath(topRightCornerPath, cornersPaint)
        drawPath(bottomLeftCornerPath, cornersPaint)
        drawPath(bottomRightCornerPath, cornersPaint)
    }

    override fun setCornersColor(@ColorInt color: Int) {
        cornersPaint.color = color
        invalidate()
    }

    private fun Canvas.drawBackground() = with(backgroundPath) {
        reset()
        addRoundRect(centerRect, rectCorners, Path.Direction.CW)
        fillType = Path.FillType.INVERSE_EVEN_ODD
        clipPath(backgroundPath)
        drawColor(transparentColor)
    }
}
