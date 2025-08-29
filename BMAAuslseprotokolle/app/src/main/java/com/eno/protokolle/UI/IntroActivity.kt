package com.eno.protokolle

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity
import com.eno.protokolle.prefs.AppPrefs

class IntroActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)
        val prefs = AppPrefs.load(this)

        if (AppPrefs.isEmpty(prefs)) {
            startActivity(
                Intent(this, SettingsActivity::class.java)
                    .putExtra("fromStart", true)
            )
        }
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<Button>(R.id.btnLoadDemo).setOnClickListener {
            // Start Main im DEMO-Modus
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("mode", "DEMO")
            })
        }

        findViewById<Button>(R.id.btnFetchByVn).setOnClickListener {
            // VN per Dialog abfragen und Main starten
            val input = EditText(this).apply { hint = "Vertragsnummer" }
            AlertDialog.Builder(this)
                .setTitle("Vertragsnummer")
                .setView(input)
                .setPositiveButton("Abrufen") { _, _ ->
                    val vn = input.text?.toString()?.trim().orEmpty()
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        putExtra("mode", "VN")
                        putExtra("vertragsnummer", vn)
                    })
                }
                .setNegativeButton("Abbrechen", null)
                .show()
        }
    }
}
