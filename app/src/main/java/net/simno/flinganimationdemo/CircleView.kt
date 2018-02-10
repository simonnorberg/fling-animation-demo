package net.simno.flinganimationdemo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.support.animation.DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS
import android.support.animation.FlingAnimation
import android.support.animation.FloatValueHolder
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.VelocityTracker
import android.view.View

class CircleView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val circleRadius = resources.getDimension(R.dimen.circle_radius)
    private val circleStrokeWidth = resources.getDimension(R.dimen.circle_stroke_width)
    private val minPos = circleRadius + circleStrokeWidth / 2f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = circleStrokeWidth
    }
    var onPositionChangedListener: OnPositionChangedListener? = null
    var friction = 1f
    private var circleX = 0f
    private var circleY = 0f
    private var xVelocity = 0f
    private var yVelocity = 0f
    private var velocityTracker: VelocityTracker? = null
    private var xFling: FlingAnimation? = null
    private var yFling: FlingAnimation? = null

    fun setPosition(position: PointF) {
        val newX = position.x * (width - 2 * minPos) + minPos
        val newY = (1 - position.y) * (height - 2 * minPos) + minPos
        setCirclePosition(newX, newY)
    }

    override fun hasOverlappingRendering(): Boolean = false

    override fun onDraw(canvas: Canvas?) {
        canvas?.drawCircle(circleX, circleY, circleRadius, paint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val index = event.actionIndex
        val action = event.actionMasked
        val pointerId = event.getPointerId(index)

        when (action) {
            ACTION_DOWN -> {
                stopFlingAnimations()
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain()
                } else {
                    velocityTracker?.clear()
                }
                velocityTracker?.addMovement(event)
                setCirclePosition(event.x, event.y)
            }
            ACTION_MOVE -> {
                if (friction < MAX_FRICTION) {
                    velocityTracker?.let {
                        it.addMovement(event)
                        it.computeCurrentVelocity(500, 10000f)
                        xVelocity = it.getXVelocity(pointerId)
                        yVelocity = it.getYVelocity(pointerId)
                    }
                }
                setCirclePosition(event.x, event.y)
            }
            ACTION_UP -> {
                velocityTracker?.recycle()
                velocityTracker = null
                if (friction < MAX_FRICTION) {
                    startXFlingAnimation()
                    startYFlingAnimation()
                }
            }
            ACTION_CANCEL -> {
                velocityTracker?.recycle()
                velocityTracker = null
            }
        }

        return true
    }

    private fun stopFlingAnimations() {
        xFling?.cancel()
        xFling = null
        yFling?.cancel()
        yFling = null
    }

    private fun startXFlingAnimation() {
        xFling = FlingAnimation(FloatValueHolder(circleX))
                .setStartVelocity(xVelocity)
                .setMaxValue(width - minPos)
                .setMinValue(minPos)
                .setMinimumVisibleChange(MIN_VISIBLE_CHANGE_PIXELS)
                .setFriction(friction)
                .apply {
                    addUpdateListener({ _, newX, _ -> setCirclePosition(newX, circleY) })
                    addEndListener({ _, canceled, _, velocity ->
                        if (!canceled && Math.abs(velocity) > 0) {
                            xVelocity = -velocity
                            startXFlingAnimation()
                        }
                    })
                    start()
                }
    }

    private fun startYFlingAnimation() {
        yFling = FlingAnimation(FloatValueHolder(circleY))
                .setStartVelocity(yVelocity)
                .setMaxValue(height - minPos)
                .setMinValue(minPos)
                .setMinimumVisibleChange(MIN_VISIBLE_CHANGE_PIXELS)
                .setFriction(friction)
                .apply {
                    addUpdateListener({ _, newY, _ -> setCirclePosition(circleX, newY) })
                    addEndListener({ _, canceled, _, velocity ->
                        if (!canceled && Math.abs(velocity) > 0) {
                            yVelocity = -velocity
                            startYFlingAnimation()
                        }
                    })
                    start()
                }
    }

    private fun setCirclePosition(newX: Float, newY: Float) {
        val validX = getValidPx(newX, minPos, width - minPos)
        val validY = getValidPx(newY, minPos, height - minPos)

        if (validX == circleX && validY == circleY) {
            return
        }

        circleX = validX
        circleY = validY

        val posX = pxToPos(circleX - minPos, width - 2 * minPos)
        val posY = 1 - pxToPos(circleY - minPos, height - 2 * minPos)
        onPositionChangedListener?.onPositionChanged(PointF(posX, posY))

        invalidate()
    }

    private fun getValidPx(px: Float, min: Float, max: Float): Float =
            if (px < min) min else if (px > max) max else px

    private fun pxToPos(px: Float, max: Float): Float =
            if (px > 0) px / max else px

    interface OnPositionChangedListener {
        fun onPositionChanged(point: PointF)
    }

    companion object {
        val MAX_FRICTION = Float.MAX_VALUE
    }
}
