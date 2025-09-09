package com.eno.protokolle.ui

import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.*
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eno.protokolle.R
import com.eno.protokolle.data.ProtokollRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FetchProgressDialog : AppCompatActivity() {

    private lateinit var progress: ProgressBar
    private lateinit var textTitle: TextView
    private lateinit var textLog: TextView
    private lateinit var btnContinue: Button

    private var success: Boolean = false
    private var mode: String = ""
    private var vn: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_fetch_progress)

        progress = findViewById(R.id.progress)
        textTitle = findViewById(R.id.textTitle)
        textLog = findViewById(R.id.textLog)
        btnContinue = findViewById(R.id.btnContinue)

        textLog.movementMethod = ScrollingMovementMethod.getInstance()

        mode = intent.getStringExtra("mode").orEmpty()
        vn = intent.getStringExtra("vertragsnummer").orEmpty()

        btnContinue.setOnClickListener {
            if (success) {
                // weiter zur Protokoll-Ansicht
                startActivity(Intent(this, com.eno.protokolle.ui.ProtokollActivity::class.java))
                finish()
            } else {
                // zurück zur Intro
                startActivity(Intent(this, com.eno.protokolle.ui.IntroActivity::class.java))
                finish()
            }
        }

        startFetch()
    }

    private fun log(line: String) {
        runOnUiThread {
            textLog.text = if (textLog.text.isNullOrEmpty()) line else "${textLog.text}\n$line"
            textLog.post {
                val layout = textLog.layout
                if (layout != null) {
                    val scroll = layout.getLineTop(textLog.lineCount) - textLog.height
                    textLog.scrollTo(0, if (scroll > 0) scroll else 0)
                }
            }
        }
    }

    private fun setResult(ok: Boolean, summary: String) {
        success = ok
        runOnUiThread {
            progress.visibility = View.GONE
            textTitle.text = summary
            btnContinue.isEnabled = true
            btnContinue.text = if (ok) "Continue" else "Zurück"
        }
    }

    private fun startFetch() {
        textTitle.text = if (vn.isNotBlank()) "Rufe Protokoll für VN $vn ab…" else "Abruf gestartet…"
        log("Lese Einstellungen…")

        lifecycleScope.launch {
            try {
                val prefs = withContext(Dispatchers.IO) {
                    com.eno.protokolle.prefs.AppPrefs.load(this@FetchProgressDialog)
                }
                log("Host: ${prefs.host}:${prefs.port}")
                if (prefs.host.isBlank() || prefs.port !in 1..65535) {
                    log("FEHLER: Ungültige Einstellungen. Bitte konfigurieren.")
                    setResult(false, "Abruf fehlgeschlagen")
                    return@launch
                }

                val vnToUse = vn.ifBlank {
                    log("FEHLER: Keine Vertragsnummer angegeben.")
                    setResult(false, "Abruf fehlgeschlagen")
                    return@launch
                }

                log("starte Protokoll Fetcher…")
                val appCtx = applicationContext
                val env = withContext(Dispatchers.IO) {
                    com.eno.protokolle.network.ReceiveAndDecode.fetchProtokoll(
                        context = appCtx,
                        vertragsnummer = vn
                    ) { msg -> log(msg) }  // ← schreibt live in dein Dialog-Textfeld
                }

                if (env == null) {
                    log("FEHLER: Keine Daten empfangen.")
                    setResult(false, "Abruf fehlgeschlagen")
                    return@launch
                }

                log("Mapping Daten…")
                val construct = withContext(Dispatchers.Default) {
                    com.eno.protokolle.newmodel.ProtokollMapper.toConstruct(env)
                }

                // Dem Repo geben, damit ProtokollActivity sicher Daten hat
                ProtokollRepo.construct = construct

                log("Bereit. Tippe auf Continue, um anzuzeigen.")
                setResult(true, "Abruf erfolgreich")
            } catch (t: Throwable) {
                log("FEHLER: ${t.javaClass.simpleName}: ${t.message}")
                setResult(false, "Abruf fehlgeschlagen")
            }
        }
    }
}