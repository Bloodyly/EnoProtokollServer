package com.eno.protokolle.ui

import android.view.GestureDetector
import android.view.MotionEvent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.OverScroller
import androidx.core.view.ViewCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.eno.protokolle.R
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderTableLayout
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderSubTableLayout
import kotlin.math.max


private object AddFlingInertia {
    fun attach(container: FixedHeaderTableLayout) {
        // Wir hängen NUR Fling dran, Drag/Zoom von Zardozz bleibt unangetastet.
        val scroller = OverScroller(container.context)
        val detector = GestureDetector(container.context, object : GestureDetector.SimpleOnGestureListener() {
             override fun onFling(
                 e1: MotionEvent?,
                 e2: MotionEvent,
                 velocityX: Float,
                 velocityY: Float
             ): Boolean {
                // Kinder nach unserem Render: 0=main, 1=colHeader, 2=rowHeader, 3=corner
                val main = container.getChildAtOrNull(0) as? FixedHeaderSubTableLayout ?: return false
                val colH = container.getChildAtOrNull(1) as? FixedHeaderSubTableLayout
                val rowH = container.getChildAtOrNull(2) as? FixedHeaderSubTableLayout

                // Content-Grenzen bestimmen (Breite/Höhe der größten Row/Spalte)
                val contentW = (0 until main.childCount)
                    .asSequence()
                    .map { main.getChildAt(it).measuredWidth }
                    .maxOrNull() ?: main.width
                val contentH = (0 until main.childCount)
                    .asSequence()
                    .map { main.getChildAt(it).measuredHeight }
                    .sum() // Höhe ist Summe der Zeilen

                val maxX = max(0, contentW - main.width)
                val maxY = max(0, contentH - main.height)

                scroller.fling(
                    /* startX */ main.scrollX,
                    /* startY */ main.scrollY,
                    /* velocityX */ (-velocityX).toInt(),
                    /* velocityY */ (-velocityY).toInt(),
                    /* minX */ 0, /* maxX */ maxX,
                    /* minY */ 0, /* maxY */ maxY
                )

                ViewCompat.postOnAnimation(container, object : Runnable {
                    override fun run() {
                        if (scroller.computeScrollOffset()) {
                            val x = scroller.currX
                            val y = scroller.currY
                            // Main 2D, Header synchronisieren (H nur x, V nur y)
                            main.scrollTo(x, y)
                            colH?.scrollTo(x, 0)
                            rowH?.scrollTo(0, y)
                            ViewCompat.postOnAnimation(container, this)
                        }
                    }
                })
                return true
            }
        })

        // Detector "mithören" lassen; Original-Touch der Library bleibt aktiv (return false)
        container.setOnTouchListener { _, ev ->
            detector.onTouchEvent(ev)
            false
        }
        container.isClickable = true
        container.isFocusable = true
    }

    private fun ViewGroup.getChildAtOrNull(i: Int): View? = if (i in 0 until childCount) getChildAt(i) else null
}
class AnlagePageFragmentFixed : Fragment(R.layout.frag_anlage_page_fixed) {

    companion object {
        private const val KEY_INDEX = "anlage_index"
        fun new(index: Int) = AnlagePageFragmentFixed().apply {
            arguments = bundleOf(KEY_INDEX to index)
        }
    }

    private val vm: ProtokollViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val construct = vm.construct ?: return
        val index = requireArguments().getInt(KEY_INDEX)
        val anlage = construct.anlagen.getOrNull(index) ?: return

        val table = view.findViewById<FixedHeaderTableLayout>(R.id.tableCombined)

        val sections = buildList {
            add(UiTableSection("Melder", anlage.melder))
            anlage.hardware?.let { add(UiTableSection("Hardware", it)) }
        }

        MultiSectionFixedTable(requireContext(), textSizeSp = 14f, rowHeightDp = 40, padHDp = 8)
            .renderInto(table, sections)
        // ⬇️ genau HIER Fling aktivieren
        AddFlingInertia.attach(table)
    }
}