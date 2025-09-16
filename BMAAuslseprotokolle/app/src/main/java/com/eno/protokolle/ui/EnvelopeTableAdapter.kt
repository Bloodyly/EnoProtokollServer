package com.eno.protokolle.ui


import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.LinearLayout
import android.widget.TextView
import com.eno.protokolle.newmodel.UiTable
import com.inqbarna.tablefixheaders.adapters.BaseTableAdapter
import kotlin.math.max
import kotlin.math.roundToInt
class EnvelopeTableAdapter(
    private val ctx: Context,
    private val table: UiTable
) : BaseTableAdapter() {

    private val density = ctx.resources.displayMetrics.scaledDensity
    private fun dp(v: Int) = (v * density).toInt()

    // Layout-Konstanten (gerne anpassen/parametrisieren)
    private val rowH = dp(40)
    private val headerH = dp(44)
    private val descW = dp(220) // linke (fixe) Spalte – enthält alle Descriptor-Spalten
    private val colW = dp(96)   // Breite pro Quartalsspalte

    private val qStart = table.qStartCol ?: table.rows.firstOrNull()?.size ?: 0
    private val descCount = qStart
    private val dataCols = (table.rows.firstOrNull()?.size ?: 0) - qStart // = quarterCount

    override fun getRowCount(): Int = table.rows.size
    override fun getColumnCount(): Int = dataCols

    override fun getWidth(column: Int): Int =
        if (column == -1) descW else colW

    override fun getHeight(row: Int): Int =
        if (row == -1) headerH else rowH

    override fun getView(row: Int, column: Int, convertView: View?, parent: ViewGroup): View {
        return when {
            row == -1 && column == -1 -> // oben links
                headerText("Beschreibung", getWidth(-1))

            row == -1 -> { // Spalten-Header: ab qStart
                val label = table.header.firstOrNull()?.getOrNull(qStart + column).orEmpty()
                headerText(label, getWidth(column))
            }

            column == -1 -> { // Zeilen-Header links: alle Descriptor-Spalten der Zeile zusammen
                descriptorCell(row)
            }

            else -> { // Datenzelle: Quartalsspalte
                val col = qStart + column
                val txt = table.rows[row].getOrNull(col).orEmpty()
                cellText(txt, getWidth(column))
            }
        }
    }

    override fun getItemViewType(row: Int, column: Int): Int = 0
    override fun getViewTypeCount(): Int = 1

    // ---- Helpers ----
    private fun headerText(t: String, w: Int): TextView =
        TextView(ctx).apply {
            text = t
            setPadding(dp(8), dp(6), dp(8), dp(6))
            setTypeface(typeface, Typeface.BOLD)
            setBackgroundColor(0xFFEFEFEF.toInt())
            layoutParams = AbsListView.LayoutParams(w, headerH)
            ellipsize = TextUtils.TruncateAt.END
            maxLines = 1
        }

    private fun cellText(t: String, w: Int): TextView =
        TextView(ctx).apply {
            text = t
            setPadding(dp(8), dp(6), dp(8), dp(6))
            layoutParams = AbsListView.LayoutParams(w, rowH)
            ellipsize = TextUtils.TruncateAt.END
            maxLines = 1
        }

    /** Linke, fixierte Zelle: fasst alle Descriptor-Spalten (0..qStart-1) horizontal zusammen. */
    private fun descriptorCell(row: Int): View {
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
            layoutParams = AbsListView.LayoutParams(descW, rowH)
        }
        for (c in 0 until descCount) {
            val text = table.rows[row].getOrNull(c).orEmpty()
            val tv = TextView(ctx).apply {
                this.text = text
                setPadding(0, 0, dp(12), 0)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                ellipsize = TextUtils.TruncateAt.END
                maxLines = 1
            }
            container.addView(tv)
        }
        return container
    }
}
