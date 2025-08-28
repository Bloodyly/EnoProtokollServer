package com.eno.protokolle

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.eno.protokolle.network.ReceiveAndDecode
import com.eno.protokolle.newmodel.`ProtokollMapper.kt`
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                val construct = `ProtokollMapper.kt`.toConstruct(env)

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
