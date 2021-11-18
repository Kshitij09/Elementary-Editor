package com.kshitijpatil.elementaryeditor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toPoint
import androidx.core.graphics.toPointF


enum class Edge {
    LEFT, TOP, RIGHT, BOTTOM
}

data class DragAction(
    val initialTouchPosition: PointF,
    val lastTouchPosition: PointF,
    val pointerId: Int,
    val activeEdge: Edge
)

class CropOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.RED
        strokeWidth = 4f
    }
    var initialBounds: Rect? = null
    private var currentCropBounds: Rect? = null
    var onCropBoundsModifiedListener: OnCropBoundsModifiedListener? = null

    fun interface OnCropBoundsModifiedListener {
        fun onBoundsChanged(modified: Boolean)
    }

    /**
     * Set image bounds once the bitmap is rendered as per the
     * [ImageView.ScaleType][android.widget.ImageView.ScaleType]
     * for [CropOverlay] to start with.
     */
    fun setImageBounds(rect: Rect) {
        // note: creating new instance to avoid pass-by-reference issues
        initialBounds = Rect(rect.left, rect.top, rect.right, rect.bottom)
        currentCropBounds = Rect(rect.left, rect.top, rect.right, rect.bottom)
        invalidate()
    }

    /**
     * @return [IntArray] crop region coordinates in
     *  the (offsetX, offsetY, width, height) order. If the [initialBounds]
     *  or [currentCropBounds] are not initialized, the method will return null.
     */
    fun getCropBounds(): IntArray? {
        val initial = initialBounds ?: return null
        val altered = currentCropBounds ?: return null
        val offsetX = altered.left - initial.left
        val offsetY = altered.top - initial.top
        val width = altered.right - altered.left
        val height = altered.bottom - altered.top
        return intArrayOf(offsetX, offsetY, width, height)
    }

    /** Allows [onTouchEvent] to consume events offset by this value from the [currentCropBounds] */
    private val precisionOffset = 100f
    private var dragAction: DragAction? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        currentCropBounds?.let {
            canvas.drawRect(it, paint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchPosition = PointF(event.x, event.y).toPoint()
        if (!isWithinImageBounds(touchPosition)) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                getNearestEdge(event)?.let { activeEdge ->
                    dragAction = DragAction(
                        touchPosition.toPointF(),
                        touchPosition.toPointF(),
                        event.getPointerId(0),
                        activeEdge
                    )
                }
            }
            MotionEvent.ACTION_MOVE -> {
                dragAction?.apply {
                    val currX = event.getX(pointerId)
                    val currY = event.getY(pointerId)
                    lastTouchPosition.set(currX, currY)
                    updateCropBounds(this)
                }
            }
            MotionEvent.ACTION_UP -> {
                dragAction?.let { updateCropBounds(it) }
                dragAction = null
            }
        }
        return true
    }

    private fun updateCropBounds(dragAction: DragAction) {
        val bounds = currentCropBounds ?: return
        val viewBounds = initialBounds ?: return
        val updateX = dragAction.lastTouchPosition.x.toInt().coerceIn(
            viewBounds.left, viewBounds.right
        )
        val updateY = dragAction.lastTouchPosition.y.toInt().coerceIn(
            viewBounds.top, viewBounds.bottom
        )
        when (dragAction.activeEdge) {
            Edge.LEFT -> {
                bounds.set(updateX, bounds.top, bounds.right, bounds.bottom)
            }
            Edge.TOP -> {
                bounds.set(bounds.left, updateY, bounds.right, bounds.bottom)
            }
            Edge.RIGHT -> {
                bounds.set(bounds.left, bounds.top, updateX, bounds.bottom)
            }
            Edge.BOTTOM -> {
                bounds.set(bounds.left, bounds.top, bounds.right, updateY)
            }
        }
        currentCropBounds = bounds
        invalidate()
        updateCropBoundsModifiedState()
    }

    private fun updateCropBoundsModifiedState() {
        onCropBoundsModifiedListener?.onBoundsChanged(
            initialBounds != currentCropBounds
        )
    }

    private fun getNearestEdge(event: MotionEvent): Edge? {
        val bounds = currentCropBounds ?: return null
        val activeEdge = when {
            isClose(event.x, bounds.left.toFloat(), precisionOffset) -> Edge.LEFT
            isClose(event.y, bounds.top.toFloat(), precisionOffset) -> Edge.TOP
            isClose(event.x, bounds.right.toFloat(), precisionOffset) -> Edge.RIGHT
            isClose(event.y, bounds.bottom.toFloat(), precisionOffset) -> Edge.BOTTOM
            else -> null
        }
        return activeEdge
    }

    private fun isClose(src: Float, target: Float, tolerance: Float): Boolean {
        return src < (target + tolerance) && src > (target - tolerance)
    }

    private fun isWithinImageBounds(touchPosition: Point): Boolean {
        val bounds = initialBounds ?: return false
        val inHorizontalBounds =
            touchPosition.x > (bounds.left - precisionOffset) && touchPosition.x < (bounds.right + precisionOffset)
        val inVerticalBounds =
            touchPosition.y > (bounds.top - precisionOffset) && touchPosition.y < (bounds.bottom + precisionOffset)
        return inHorizontalBounds || inVerticalBounds
    }
}