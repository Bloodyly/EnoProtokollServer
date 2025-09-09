package com.eno.protokolle.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity
import com.eno.protokolle.prefs.AppPrefs
import com.eno.protokolle.R
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

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
            startActivity(Intent(this, ProtokollActivity::class.java).apply {
                putExtra("mode", "DEMO")
            })
        }

        findViewById<Button>(R.id.btnFetchByVn).setOnClickListener {
            val input = EditText(this).apply { hint = "Vertragsnummer" }
            AlertDialog.Builder(this)
                .setTitle("Vertragsnummer")
                .setView(input)
                .setPositiveButton("Abrufen") { _, _ ->
                    val vn = input.text?.toString()?.trim().orEmpty()
                    // -> FetchActivity starten
                    startActivity(Intent(this, FetchProgressDialog::class.java).apply {
                        putExtra("mode", "VN")
                        putExtra("vertragsnummer", vn)
                    })
                }
                .setNegativeButton("Abbrechen", null)
                .show()
        }
    }
}
