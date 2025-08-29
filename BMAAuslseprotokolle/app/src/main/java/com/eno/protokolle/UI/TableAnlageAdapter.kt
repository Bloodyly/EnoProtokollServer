package com.eno.protokolle.ui

import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.inqbarna.tablefixheaders.adapters.BaseTableAdapter

class TableAnlageAdapter(
    private var headers: List<String>,
    private var leftCol: List<String>,           // optional – leere Liste blendet die Spalte aus
    private var rows: List<List<String>>
) : BaseTableAdapter() {

    fun update(h: List<String>, l: List<String>, r: List<List<String>>) {
        headers = h; leftCol = l; rows = r
    }

    override fun getRowCount(): Int = rows.size + 1
    override fun getColumnCount(): Int = (if (leftCol.isEmpty()) 0 else 1) + headers.size

    override fun getWidth(column: Int): Int =
        when (column) {
            0 -> if (leftCol.isEmpty()) dp(120) else dp(72)
            else -> dp(140)
        }

    override fun getHeight(row: Int): Int = if (row == 0) dp(40) else dp(36)

    override fun getView(row: Int, column: Int, parent: ViewGroup): View {
        val ctx = parent.context
        val tv = TextView(ctx).apply {
            setPadding(dp(12), dp(8), dp(12), dp(8))
            gravity = Gravity.CENTER_VERTICAL
            maxLines = 2
        }

        val hasLeft = leftCol.isNotEmpty()
        return if (row == 0) {
            tv.setTypeface(Typeface.DEFAULT_BOLD)
            val headerText = if (hasLeft && column == 0) "" else headers[column - if (hasLeft) 1 else 0]
            tv.text = headerText
            tv
        } else {
            val r = row - 1
            if (hasLeft && column == 0) {
                tv.setTypeface(Typeface.DEFAULT_BOLD)
                tv.text = leftCol.getOrNull(r).orEmpty()
            } else {
                val c = column - if (hasLeft) 1 else 0
                tv.text = rows.getOrNull(r)?.getOrNull(c).orEmpty()
            }
            tv
        }
    }

    private fun dp(v: Int): Int = (v * (density ?: 1f)).toInt()
    private var density: Float? = null
    override fun setAdapterView(view: View?) {
        super.setAdapterView(view)
        density = view?.resources?.displayMetrics?.density ?: 1f
    }

    override fun getItemViewType(row: Int, column: Int): Int = 0
    override fun getViewTypeCount(): Int = 1
    override fun getItem(row: Int, column: Int): Any? = null
}
