package com.eno.protokolle

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.eno.protokolle.ui.FetchProgressDialog
import com.eno.protokolle.ui.IntroActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data: Uri? = intent?.data
        if (data != null) {
            // VN aus Link extrahieren (unterstützt ?vn=123, ?vn123 und /protokoll/123)
            val vn = extractVnFromUri(data)
            if (!vn.isNullOrBlank()) {
                // ➜ genau wie in IntroActivity: Fetch starten
                startActivity(
                    Intent(this, FetchProgressDialog::class.java).apply {
                        putExtra("mode", "VN")
                        putExtra("vertragsnummer", vn)
                        // optional: Backstack aufräumen
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                )
                finish()
                return
            }
            // Ungültiger Link → Intro
            startActivity(Intent(this, IntroActivity::class.java))
            finish()
            return
        }

        // Kein Deep-Link → Intro wie gewohnt
        startActivity(Intent(this, IntroActivity::class.java))
        finish()
    }

    private fun extractVnFromUri(uri: Uri): String? {
        // 1) Standard: ?vn=123456
        uri.getQueryParameter("vn")?.let { if (it.isNotBlank()) return it }

        // 2) Robust: ?vn123456 (ohne '=')
        uri.query?.let { q ->
            val m = Regex("""(^|[?&])vn=?(\d{3,})""").find(q)
            m?.groupValues?.getOrNull(2)?.let { return it }
        }

        // 3) Alternativ: /protokoll/123456 (letzte Pfadkomponente numerisch)
        return uri.pathSegments.lastOrNull()?.takeIf { it.all(Char::isDigit) }
    }
}
