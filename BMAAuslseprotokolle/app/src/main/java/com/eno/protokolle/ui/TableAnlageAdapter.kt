package com.eno.protokolle.ui

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.setPadding
import com.celerysoft.tablefixheaders.adapter.BaseTableAdapter

class TableAnlageAdapter(
    private var headers: List<String>,         // Spaltenüberschriften (ohne linke Kopfspalte)
    private var leftColumn: List<String>,      // linke Kopfspalte (eine je Datenzeile)
    private var dataRows: List<List<String>>   // Datenzeilen (jede List<String> hat headers.size Elemente)
) : BaseTableAdapter() {

    fun update(newHeaders: List<String>, newLeft: List<String>, newRows: List<List<String>>) {
        headers = newHeaders
        leftColumn = newLeft
        dataRows = newRows
    }

    // --- Pflichtmethoden der BaseTableAdapter API ---

    override fun getColumnCount(): Int = 1 + headers.size       // 1 = linke Kopfspalte
    override fun getRowCount(): Int = 1 + leftColumn.size       // 1 = Header-Zeile

    override fun getWidth(column: Int): Int {
        val dp = if (column == 0) 72 else 140
        return dpToPx(dp)
    }

    override fun getHeight(row: Int): Int {
        val dp = if (row == 0) 44 else 40
        return dpToPx(dp)
    }

    // WICHTIG: Signatur MIT convertView!
    override fun getView(row: Int, column: Int, convertView: View?, parent: ViewGroup): View {
        val context: Context = parent.context

        val tv = (convertView as? TextView) ?: TextView(context).apply {
            setPadding(dpToPx(8))
            gravity = Gravity.CENTER_VERTICAL
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            isSingleLine = false
        }

        // Sicheres Lesen mit getOrElse, keine negativen Indizes
        val text = when {
            row == 0 && column == 0 -> ""
            row == 0               -> headers.getOrElse(column - 1) { "" }
            column == 0            -> leftColumn.getOrElse(row - 1) { "" }
            else                   -> dataRows.getOrElse(row - 1) { emptyList() }
                .getOrElse(column - 1) { "" }
        }
        tv.text = text

        tv.setTypeface(
            tv.typeface,
            if (row == 0 || column == 0) android.graphics.Typeface.BOLD
            else android.graphics.Typeface.NORMAL
        )

        return tv
    }


    // Optional aber oft von der Lib erwartet – 1 View-Typ reicht uns:
    override fun getViewTypeCount(): Int = 1
    override fun getBackgroundResId(p0: Int, p1: Int): Int {
        TODO("Not yet implemented")
    }

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

    override fun getItemViewType(row: Int, column: Int): Int = 0

    private fun dpToPx(dp: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            android.content.res.Resources.getSystem().displayMetrics
        ).toInt()
}
