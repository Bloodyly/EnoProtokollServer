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

    fun load(ctx: Context): Model = with(ctx.getSharedPreferences(FILE, MODE_PRIVATE)) {
        Model(
            host = getString(K_HOST, "") ?: "",
            port = getInt(K_PORT, 0),
            user = getString(K_USER, "") ?: "",
            pass = getString(K_PASS, "") ?: "",
            aesB64 = getString(K_AES, "") ?: ""
        )
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
