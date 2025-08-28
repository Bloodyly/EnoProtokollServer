package com.eno.protokolle

import android.os.Bundle
import androidx.activity.ComponentActivity

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Wir verwenden dein Dialog-Layout als Activity-Content
        setContentView(R.layout.dialog_settings)
        // TODO: vorhandene Logik aus deinem bisherigen Settings-Dialog hier verdrahten
    }
}
