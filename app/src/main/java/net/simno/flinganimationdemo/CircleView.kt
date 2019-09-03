package net.simno.flinganimationdemo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.DynamicAnimation.OnAnimationEndListener
import androidx.dynamicanimation.animation.DynamicAnimation.OnAnimationUpdateListener
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.FloatValueHolder
import kotlin.math.absoluteValue

class CircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val xAnimationUpdate = OnAnimationUpdateListener { _, newX, _ ->
        updatePosition(newX, circleY)
    }
    private val yAnimationUpdate = OnAnimationUpdateListener { _, newY, _ ->
        updatePosition(circleX, newY)
    }
    private val xAnimationEnd = OnAnimationEndListener { _, canceled, _, velocity ->
        if (!canceled && velocity.absoluteValue > 0 && ViewCompat.isAttachedToWindow(this)) {
            xVelocity = -velocity
            startXAnimation()
        }
    }
    private val yAnimationEnd = OnAnimationEndListener { _, canceled, _, velocity ->
        if (!canceled && velocity.absoluteValue > 0 && ViewCompat.isAttachedToWindow(this)) {
            yVelocity = -velocity
            startYAnimation()
        }
    }

    private val circleRadius = resources.getDimension(R.dimen.circle_radius)
    private val circleStrokeWidth = resources.getDimension(R.dimen.circle_stroke_width)
    private val minX = (circleStrokeWidth / 2f) + circleRadius
    private val minY = (circleStrokeWidth / 2f) + circleRadius

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.colorAccent)
        strokeWidth = circleStrokeWidth
    }

    var onPositionChangedListener: OnPositionChangedListener? = null
    var friction = MAX_FRICTION
        set(value) {
            field = value
            if (value == MAX_FRICTION) {
                stopAnimations()
            }
        }

    private var circleX = 0f
    private var circleY = 0f
    private var xVelocity = 0f
    private var yVelocity = 0f
    private var velocityTracker: VelocityTracker? = null
    private var xFling: FlingAnimation? = null
    private var yFling: FlingAnimation? = null
    private var lastSetPosition: PointF = PointF(0.5f, 0.5f)

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimations()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w != oldw || h != oldh) {
            setPosition(lastSetPosition)
        }
    }

    override fun hasOverlappingRendering(): Boolean = false

    override fun onDraw(canvas: Canvas) {
        canvas.drawCircle(circleX, circleY, circleRadius, paint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val index = event.actionIndex
        val action = event.actionMasked
        val pointerId = event.getPointerId(index)

        when (action) {
            MotionEvent.ACTION_DOWN -> onActionDown(event)
            MotionEvent.ACTION_MOVE -> onActionMove(event, pointerId)
            MotionEvent.ACTION_UP -> onActionUp()
            MotionEvent.ACTION_CANCEL -> onActionCancel()
        }

        return true
    }

    fun setPosition(position: PointF) {
        // Save the position if we call setPosition before onSizeChanged has been called.
        lastSetPosition = position

        // Don't set position if layout is not finished.
        if (width == 0 && height == 0) {
            return
        }

        // Convert position fraction [0.0-1.0] to pixels.
        val newX = position.x * (maxX() - minX) + minX
        val newY = (1 - position.y) * (maxY() - minY) + minY
        updatePosition(newX, newY)
    }

    private fun onActionDown(event: MotionEvent) {
        stopAnimations()
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        } else {
            velocityTracker?.clear()
        }
        velocityTracker?.addMovement(event)
        updatePosition(event.x, event.y)
    }

    private fun onActionMove(event: MotionEvent, pointerId: Int) {
        if (friction < MAX_FRICTION) {
            velocityTracker?.let {
                it.addMovement(event)
                it.computeCurrentVelocity(500, 10000f)
                xVelocity = it.getXVelocity(pointerId)
                yVelocity = it.getYVelocity(pointerId)
            }
        }
        updatePosition(event.x, event.y)
    }

    private fun onActionUp() {
        velocityTracker?.recycle()
        velocityTracker = null
        if (friction < MAX_FRICTION) {
            startXAnimation()
            startYAnimation()
        }
    }

    private fun onActionCancel() {
        velocityTracker?.recycle()
        velocityTracker = null
    }

    private fun maxX() = width - minX

    private fun maxY() = height - minY

    private fun stopAnimations() {
        xFling?.cancel()
        xFling = null
        yFling?.cancel()
        yFling = null
    }

    private fun startXAnimation() {
        xFling = createAnimation(circleX, xVelocity, maxX(), minX).apply {
            addUpdateListener(xAnimationUpdate)
            addEndListener(xAnimationEnd)
            start()
        }
    }

    private fun startYAnimation() {
        yFling = createAnimation(circleY, yVelocity, maxY(), minY).apply {
            addUpdateListener(yAnimationUpdate)
            addEndListener(yAnimationEnd)
            start()
        }
    }

    private fun createAnimation(
        startValue: Float,
        startVelocity: Float,
        maxValue: Float,
        minValue: Float
    ): FlingAnimation {
        return FlingAnimation(FloatValueHolder(startValue))
            .setStartVelocity(startVelocity)
            .setMaxValue(maxValue)
            .setMinValue(minValue)
            .setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS)
            .setFriction(friction)
    }

    private fun updatePosition(newX: Float, newY: Float) {
        val validX = newX.coerceIn(minX, maxX())
        val validY = newY.coerceIn(minY, maxY())

        if (validX == circleX && validY == circleY) {
            return
        }

        circleX = validX
        circleY = validY

        notifyPositionChanged()
        invalidate()
    }

    private fun notifyPositionChanged() {
        // Convert pixels to position fraction [0.0-1.0].
        val posX = ((circleX - minX) / (maxX() - minX)).coerceIn(0f, 1f)
        val posY = 1 - ((circleY - minY) / (maxY() - minY)).coerceIn(0f, 1f)
        onPositionChangedListener?.onPositionChanged(PointF(posX, posY))
    }

    interface OnPositionChangedListener {
        fun onPositionChanged(point: PointF)
    }

    companion object {
        val MAX_FRICTION = Float.MAX_VALUE
    }
}
