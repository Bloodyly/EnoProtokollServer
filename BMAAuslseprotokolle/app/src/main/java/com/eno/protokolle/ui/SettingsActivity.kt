package com.eno.protokolle.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.eno.protokolle.prefs.AppPrefs
import com.eno.protokolle.util.DebugLog
import com.eno.protokolle.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket



class SettingsActivity : ComponentActivity() {
    private lateinit var editServer: EditText
    private lateinit var editPort: EditText
    private lateinit var editUser: EditText
    private lateinit var editPass: EditText
    private var editAes: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_settings)

        editServer = findViewById(R.id.editServer)
        editPort   = findViewById(R.id.editPort)
        editUser   = findViewById(R.id.editUser)
        editPass   = findViewById(R.id.editPassword)
        editAes    = findViewById(resources.getIdentifier("editAesKey","id",packageName))

        val btnSave  = findViewById<Button>(R.id.btnSave)
        val btnTest  = findViewById<Button>(R.id.btnTest)
        val btnDebug = findViewById<Button?>(R.id.btnDebug) // siehe Layout unten

        // Prefs laden
        AppPrefs.load(this).also { p ->
            editServer.setText(p.host)
            if (p.port > 0) editPort.setText(p.port.toString())
            editUser.setText(p.user)
            editPass.setText(p.pass)
            editAes?.setText(p.aesB64)
        }

        btnSave.setOnClickListener {
            val model = readModelOrNull() ?: return@setOnClickListener
            AppPrefs.save(this, model)
            DebugLog.d("Settings gespeichert: ${model.host}:${model.port}, user=${model.user}")
            Toast.makeText(this, "Einstellungen gespeichert.", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnTest.setOnClickListener {
            val model = readModelOrNull() ?: return@setOnClickListener
            lifecycleScope.launchWhenStarted {
                val ok = testConnection(model.host, model.port)
                if (ok) {
                    DebugLog.d("Verbindungstest OK zu ${model.host}:${model.port}")
                    Toast.makeText(this@SettingsActivity, "Verbindung erfolgreich.", Toast.LENGTH_SHORT).show()
                } else {
                    DebugLog.e("Verbindungstest FEHLER zu ${model.host}:${model.port}")
                    Toast.makeText(this@SettingsActivity, "Verbindung fehlgeschlagen.", Toast.LENGTH_LONG).show()
                }
            }
        }


    }

    private fun readModelOrNull(): AppPrefs.Model? {
        val host = editServer.text?.toString()?.trim().orEmpty()
        val port = editPort.text?.toString()?.trim()?.toIntOrNull() ?: 0
        val user = editUser.text?.toString()?.trim().orEmpty()
        val pass = editPass.text?.toString()?.trim().orEmpty()
        val aes  = editAes?.text?.toString()?.trim().orEmpty()

        if (host.isEmpty() || port !in 1..65535) {
            Toast.makeText(this, "Bitte gültigen Host und Port eingeben.", Toast.LENGTH_SHORT).show()
            return null
        }
        return AppPrefs.Model(host, port, user, pass, aes)
    }

    private suspend fun testConnection(host: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            Socket().use { s ->
                s.connect(InetSocketAddress(host, port), 3000)
            }
        }.isSuccess
    }
}
