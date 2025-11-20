package com.eno.protokolle.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.OverScroller
import androidx.core.view.ViewCompat
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderSubTableLayout
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderTableLayout
import kotlin.math.max

class FixedHeaderTableLayoutWithFling @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FixedHeaderTableLayout(context, attrs, defStyleAttr) {

    private val scroller = OverScroller(context)
    private val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val main = getChildAtOrNull(0) as? FixedHeaderSubTableLayout ?: return false
            val contentW = (0 until main.childCount).maxOfOrNull { main.getChildAt(it).measuredWidth } ?: main.width
            val contentH = (0 until main.childCount).sumOf { main.getChildAt(it).measuredHeight }
            val maxX = max(0, contentW - main.width)
            val maxY = max(0, contentH - main.height)

            scroller.fling(
                main.scrollX, main.scrollY,
                (-velocityX).toInt(), (-velocityY).toInt(),
                0, maxX, 0, maxY
            )
            ViewCompat.postInvalidateOnAnimation(this@FixedHeaderTableLayoutWithFling) // triggert computeScroll()
            return true
        }
    })

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Fling-Gesten immer mithören
        detector.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_DOWN && !scroller.isFinished) {
            scroller.forceFinished(true) // laufenden Fling stoppen, neuer Touch übernimmt
        }
        // Rest macht die Original-Implementierung (Drag/Pinch etc.)
        return super.onTouchEvent(event)
    }

    override fun computeScroll() {
        super.computeScroll()
        if (scroller.computeScrollOffset()) {
            val x = scroller.currX
            val y = scroller.currY
            // Kinder 0..2 synchron scrollen: main (x,y), colHeader (x,0), rowHeader (0,y)
            (getChildAtOrNull(0) as? View)?.scrollTo(x, y)
            (getChildAtOrNull(1) as? View)?.scrollTo(x, 0)
            (getChildAtOrNull(2) as? View)?.scrollTo(0, y)
            ViewCompat.postInvalidateOnAnimation(this) // nächster Frame
        }
    }

    private fun ViewGroup.getChildAtOrNull(i: Int): View? =
        if (i in 0 until childCount) getChildAt(i) else null
}
