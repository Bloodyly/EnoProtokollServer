package com.eno.protokolle.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout

class ZoomFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var scaleFactor = 1f
    private val minScale = 0.5f
    private val maxScale = 2.5f
    private var isScaling = false

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                isScaling = true
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val before = scaleFactor
                scaleFactor = (scaleFactor * detector.scaleFactor)
                    .coerceIn(minScale, maxScale)
                if (scaleFactor != before) {
                    scaleX = scaleFactor
                    scaleY = scaleFactor
                    invalidate()
                }
                return true
            }
            override fun onScaleEnd(detector: ScaleGestureDetector) {
                isScaling = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
    )

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Lass den Detector immer mithören; bei aktivem Zoom intercep­ten
        scaleDetector.onTouchEvent(ev)
        return isScaling || super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        return isScaling || super.onTouchEvent(event)
    }
}
