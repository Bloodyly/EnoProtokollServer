package com.eno.protokolle.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import com.eno.protokolle.R

class FetchProgressDialog : DialogFragment() {

    private var textLog: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.dialog_fetch_progress, container, false)
        textLog = v.findViewById(R.id.textLog)
        return v
    }

    fun append(line: String) {
        // Thread-safe auf UI
        activity?.runOnUiThread {
            textLog?.apply {
                if (text.isNullOrEmpty()) text = line
                else text = "$text\n$line"
                // Auto-scroll ans Ende
                (parent as? ViewGroup)?.let { vg ->
                    // falls in ScrollView: scrolle runter
                    vg.post { this.layout()?.let { this.parent?.requestLayout() } }
                }
            }
        }
    }

    fun setTitleText(title: String) {
        activity?.runOnUiThread {
            view?.findViewById<TextView>(R.id.textTitle)?.text = title
        }
    }
}
