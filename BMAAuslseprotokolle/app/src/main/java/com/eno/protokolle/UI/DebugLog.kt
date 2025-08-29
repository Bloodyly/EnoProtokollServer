package com.eno.protokolle

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.eno.protokolle.util.DebugLog

class DebugActivity : ComponentActivity() {
    private lateinit var txt: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)

        txt = findViewById(R.id.txtLogs)
        val btnShare = findViewById<Button>(R.id.btnShare)
        val btnCopy  = findViewById<Button>(R.id.btnCopy)
        val btnClear = findViewById<Button>(R.id.btnClear)
        val btnRefresh = findViewById<Button>(R.id.btnRefresh)

        fun refresh() { txt.text = DebugLog.asText() }
        refresh()

        btnRefresh.setOnClickListener { refresh() }
        btnClear.setOnClickListener { DebugLog.clear(); refresh() }
        btnCopy.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("logs", DebugLog.asText()))
        }
        btnShare.setOnClickListener {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "BMA Debug-Logs")
                putExtra(Intent.EXTRA_TEXT, DebugLog.asText())
            }
            startActivity(Intent.createChooser(send, "Logs teilen"))
        }
    }
}
