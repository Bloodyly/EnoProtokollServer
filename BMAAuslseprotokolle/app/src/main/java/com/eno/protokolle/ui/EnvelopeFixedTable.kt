package com.eno.protokolle.ui

import android.content.Context
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import com.eno.protokolle.R
import com.eno.protokolle.newmodel.UiTable
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderSubTableLayout
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderTableLayout
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderTableRow
import kotlin.math.roundToInt

/**
 * Renderer für Zardozz FixedHeaderTableLayout (parallel zum alten Adapter).
 * Baut die 4 Subtables programmatically und hängt sie in der korrekten Reihenfolge an.
 *
 * Relevante Wiki-Punkte:
 * - Subtables programmatically erzeugen (kein XML) und gleiche Zeilen/Spalten-Kardinalität beachten
 * - addViews(main, columnHeader, rowHeader, corner) – Parameterreihenfolge ist wichtig!
 */
class EnvelopeFixedTable(
    private val ctx: Context,
    private val textSizeSp: Float = 14f,
    rowHeightDp: Int = 40,
    private val padHDp: Int = 8
) {

    // DP → PX
    private val dm = ctx.resources.displayMetrics
    private fun dp(v: Int) = (v * (if (dm.density > 0) dm.density else 1f)).roundToInt()
    private val rowH = dp(rowHeightDp)
    private val padH = dp(padHDp)

    fun renderInto(container: FixedHeaderTableLayout, table: UiTable) {
        container.removeAllViews()

        // Daten aufbereiten
        val headerRows: List<List<String>> = table.header
        val bodyRows: List<List<String>> = table.rows

        val maxColsHeader = headerRows.maxOfOrNull { it.size } ?: 0
        val maxColsBody   = bodyRows.maxOfOrNull { it.size } ?: 0
        val totalCols = maxOf(maxColsHeader, maxColsBody)

        // Subtables
        val mainTable = FixedHeaderSubTableLayout(ctx)
        val columnHeaderTable = FixedHeaderSubTableLayout(ctx)
        val rowHeaderTable = FixedHeaderSubTableLayout(ctx)
        val cornerTable = FixedHeaderSubTableLayout(ctx)

        // ---- Column Header (k Headerzeilen, N-1 Zellen ab Spalte 1) ----
        for (hr in headerRows) {
            val tr = FixedHeaderTableRow(ctx)
            for (c in 1 until totalCols) {
                val t = hr.getOrNull(c).orEmpty()
                tr.addView(makeHeaderCell(t))
            }
            columnHeaderTable.addView(tr)
        }

        // ---- Row Header (M Zeilen, 1 Zelle aus Spalte 0) ----
        for (r in bodyRows.indices) {
            val tr = FixedHeaderTableRow(ctx)
            val t = bodyRows[r].getOrNull(0).orEmpty()
            tr.addView(makeRowHeaderCell(t))
            rowHeaderTable.addView(tr)
        }

        // ---- Main (M Zeilen, N-1 Zellen ab Spalte 1) ----
        for (r in bodyRows.indices) {
            val tr = FixedHeaderTableRow(ctx)
            for (c in 1 until totalCols) {
                val t = bodyRows[r].getOrNull(c).orEmpty()
                tr.addView(makeBodyCell(t))
            }
            mainTable.addView(tr)
        }

        // ---- Corner (k Zeilen, 1 Zelle aus Header-Spalte 0) ----
        for (hr in headerRows) {
            val tr = FixedHeaderTableRow(ctx)
            tr.addView(makeCornerCell(hr.getOrNull(0).orEmpty()))
            cornerTable.addView(tr)
        }

        // Wichtig: Reihenfolge! (Wiki)
        container.addViews(mainTable, columnHeaderTable, rowHeaderTable, cornerTable)
        // calculatePanScale(...) lasse ich bewusst weg (API-Signatur variiert je Version)
    }

    // ----- Cell-Fabriken -----

    private fun makeHeaderCell(text: String) = baseCell(text).apply {
        setBackgroundResource(R.drawable.bg_header_cell_black)
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun makeRowHeaderCell(text: String) = baseCell(text).apply {
        setBackgroundResource(R.drawable.bg_header_cell_black)
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun makeCornerCell(text: String) = baseCell(text).apply {
        setBackgroundResource(R.drawable.bg_header_cell_black)
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun makeBodyCell(text: String) = baseCell(text).apply {
        setBackgroundResource(R.drawable.bg_cell_black)
    }

    private fun baseCell(text: String) =
        TextView(ctx).apply {
            this.text = text
            textSize = textSizeSp
            setPadding(padH, 0, padH, 0)
            ellipsize = TextUtils.TruncateAt.END
            maxLines = 1
            minHeight = rowH
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
}
