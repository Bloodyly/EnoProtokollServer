// AppPrefs.kt
package com.eno.protokolle.prefs

import android.content.Context
import android.content.Context.MODE_PRIVATE

object AppPrefs {
    private const val FILE = "settings"
    private const val K_HOST = "host"
    private const val K_PORT = "port"
    private const val K_USER = "user"
    private const val K_PASS = "pass"
    private const val K_AES  = "aes_b64"

    data class Model(
        val host: String = "",
        val port: Int = 0,
        val user: String = "",
        val pass: String = "",
        val aesB64: String = ""
    )

    fun load(ctx: Context): Model {
        val sp = ctx.getSharedPreferences(FILE, MODE_PRIVATE)

        val host = sp.getString(K_HOST, "") ?: ""
        val user = sp.getString(K_USER, "") ?: ""
        val pass = sp.getString(K_PASS, "") ?: ""
        val aes  = sp.getString(K_AES,  "") ?: ""

        // 🔧 Port robust laden + migrieren (String → Int)
        val port: Int = try {
            sp.getInt(K_PORT, 0)
        } catch (e: ClassCastException) {
            // Früher als String gespeichert
            val asString = sp.getString(K_PORT, "") ?: ""
            val parsed = asString.toIntOrNull() ?: 0
            // Aufräumen: künftig als Int persistieren
            sp.edit().remove(K_PORT).putInt(K_PORT, parsed).apply()
            parsed
        }

        return Model(host = host, port = port, user = user, pass = pass, aesB64 = aes)
    }

    fun save(ctx: Context, m: Model) {
        ctx.getSharedPreferences(FILE, MODE_PRIVATE).edit()
            .putString(K_HOST, m.host)
            .putInt(K_PORT, m.port)
            .putString(K_USER, m.user)
            .putString(K_PASS, m.pass)
            .putString(K_AES, m.aesB64)
            .apply()
    }

    fun isEmpty(m: Model): Boolean = m.host.isBlank() || m.port !in 1..65535
}
