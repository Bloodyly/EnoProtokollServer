package com.eno.protokolle


import android.content.Intent   // <— WICHTIG
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.eno.protokolle.network.ReceiveAndDecode
import com.eno.protokolle.newmodel.ProtokollMapper
import com.eno.protokolle.ui.IntroActivity
import com.eno.protokolle.ui.SettingsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Falls weder Deep-Link (data) noch ein "mode"/"vertragsnummer"-Extra vorhanden ist → Intro zeigen
        val hasData = intent?.data != null
        val hasExtras = intent?.extras?.isEmpty == false
        if (intent?.data == null && intent?.extras?.isEmpty != false) {
            startActivity(Intent(this, IntroActivity::class.java))
            finish()
            return
        }

        // Hier erstmal keine UI – nur Flow "abrufen → mappen"
        lifecycleScope.launch(Dispatchers.IO) {
            val vn = "DEMO-VERTRAGSNUMMER" // TODO: aus Einstellungen/Intent
            val env = ReceiveAndDecode.receiveProtokollNew(this@MainActivity, vn)

            withContext(Dispatchers.Main) {
                if (env == null) {
                    Toast.makeText(this@MainActivity, "Kein Protokoll empfangen.", Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                // Transport → UI-Konstrukt
                val construct = ProtokollMapper.toConstruct(env)

                // Bis wir UI bauen: kurz loggen
                android.util.Log.i("Construct",
                    "PType=${construct.pType} WType=${construct.wType} " +
                            "Anlagen=${construct.anlagen.size} MelderTypes=${construct.melderTypes.size}"
                )

                // TODO: Im nächsten Schritt: construct an ViewModel/Adapter geben und UI zeichnen
            }
        }
    }
}
