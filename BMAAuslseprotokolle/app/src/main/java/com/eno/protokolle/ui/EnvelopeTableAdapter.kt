package com.eno.protokolle.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.TextView
import com.eno.protokolle.R
import com.eno.protokolle.newmodel.UiTable
import com.celerysoft.tablefixheaders.adapter.BaseTableAdapter
import kotlin.math.max
import kotlin.math.roundToInt

class EnvelopeTableAdapter(
    private val ctx: Context,
    private val table: UiTable

) : BaseTableAdapter() {

    // --- Maße ---
    private val dm = ctx.resources.displayMetrics
    private fun dp(v: Int) = (v * (if (dm.density > 0) dm.density else 1f)).roundToInt()
    private val textSizeSp = 14f
    private val rowH = dp(40)
    private val headerH = dp(44)
    private val padH = dp(8)
    private val minEmptyColPx = dp(40) // Mindestbreite für komplett leere Spalten

    // --- Daten ---

    private val rows: List<List<String>> = table.rows
    private val headerRow: List<String> = table.header.lastOrNull() ?: emptyList()

    // „echte“ Spaltenanzahl = Max aus Headerbreite und längster Datenzeile
    private val totalCols: Int = run {
        val maxRow = rows.maxOfOrNull { it.size } ?: 0
        max(maxRow, headerRow.size)
    }

    // ViewPager-Fixierung: linke Spalte (-1) wird aus tatsächlicher Spalte 0 gespeist,
    // Datenbereich sind Spalten 1..totalCols-1 → Adapter gibt N-1 „rechte“ Spalten zurück.
    override fun getColumnCount(): Int = (totalCols - 1).coerceAtLeast(0)
    override fun getRowCount(): Int = rows.size
    override fun getBackgroundResId(row: Int, column: Int): Int = 0
    override fun getBackgroundHighlightResId(p0: Int, p1: Int): Int {
        TODO("Not yet implemented")
    }

    override fun getItem(p0: Int, p1: Int): Any? {
        TODO("Not yet implemented")
    }

    override fun getItemId(p0: Int, p1: Int): Long {
        TODO("Not yet implemented")
    }

    override fun isRowSelectable(p0: Int): Boolean {
        TODO("Not yet implemented")
    }

    // Mess-Paint
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = textSizeSp * (if (dm.scaledDensity > 0) dm.scaledDensity else 1f)
    }

    // Breitenberechnung (px) — einmal vorab gemessen
    private val leftColWidthPx: Int = measureColumnPx(0) // linke fixe Spalte = Spalte 0
    private val dataColWidthsPx: IntArray = IntArray(getColumnCount()) { i -> // i = 0..N-2
        val actualCol = i + 1
        measureColumnPx(actualCol)
    }

    // Höhe/Breite je Zelle (Header: row == -1, linke Spalte: column == -1)
    override fun getHeight(row: Int): Int = if (row == -1) headerH else rowH
    override fun getWidth(column: Int): Int =
        if (column == -1) leftColWidthPx else dataColWidthsPx.getOrElse(column) { minEmptyColPx }

    override fun getView(row: Int, column: Int, convertView: View?, parent: ViewGroup): View {
        return when {
            row == -1 && column == -1 -> { // oben links: Headertext der ersten Spalte
                headerText(headerRow.getOrNull(0).orEmpty(), leftColWidthPx)
            }
            row == -1 -> { // Spalten-Header (rechte Seite): Header von Spalte (column+1)
                val label = headerRow.getOrNull(column + 1).orEmpty()
                headerText(label, getWidth(column))
            }
            column == -1 -> { // Zeilen-Header links: Wert der Spalte 0
                val v = rows[row].getOrNull(0).orEmpty()
                cellText(v, leftColWidthPx)
            }
            else -> { // Datenzelle: Spalte (column+1)
                val v = rows[row].getOrNull(column + 1).orEmpty()
                cellText(v, getWidth(column))
            }
        }
    }

    override fun getItemViewType(row: Int, column: Int) = 0
    override fun getViewTypeCount() = 1

    // ---- Helpers ----

    /** misst die Spaltenbreite als max(Header, alle Zellen), inkl. Padding; leer -> Mindestbreite */
    private fun measureColumnPx(col: Int): Int {
        var maxPx = 0f
        // Header
        val h = headerRow.getOrNull(col).orEmpty()
        if (h.isNotEmpty()) maxPx = max(maxPx, paint.measureText(h))
        // Zellen
        for (r in rows) {
            val v = r.getOrNull(col).orEmpty()
            if (v.isNotEmpty()) maxPx = max(maxPx, paint.measureText(v))
        }
        // Padding addieren; Leer-Case abfangen
        val withPad = if (maxPx > 0f) maxPx + padH * 2 else minEmptyColPx.toFloat()
        return withPad.roundToInt()
    }

    private fun headerText(t: String, w: Int): TextView =
        TextView(ctx).apply {
            text = t
            textSize = textSizeSp
            setTypeface(typeface, Typeface.BOLD)
            setPadding(padH, 0, padH, 0)
            setBackgroundResource(R.drawable.bg_header_cell_black) // hellgrau + schwarze Linien
            layoutParams = AbsListView.LayoutParams(w, headerH)
            ellipsize = TextUtils.TruncateAt.END
            maxLines = 1
        }

    private fun cellText(t: String, w: Int): TextView =
        TextView(ctx).apply {
            text = t
            textSize = textSizeSp
            setPadding(padH, 0, padH, 0)
            setBackgroundResource(R.drawable.bg_cell_black) // weiß + schwarze Linien
            layoutParams = AbsListView.LayoutParams(w, rowH)
            ellipsize = TextUtils.TruncateAt.END
            maxLines = 1
        }
}
