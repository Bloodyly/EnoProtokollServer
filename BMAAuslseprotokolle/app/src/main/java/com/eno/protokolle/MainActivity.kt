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

    }
}
