// src/main/java/com/eno/protokolle/network/Receive_and_Decode.kt
package com.eno.protokolle.network

import android.content.Context
import android.util.Base64
import android.util.Log
import com.eno.protokolle.newmodel.ProtokollCodec
import com.eno.protokolle.newmodel.ProtokollEnvelope
import com.eno.protokolle.prefs.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object ReceiveAndDecode {

    // -------- Konfiguration -------------------------------------------------

    // Optionaler Fallback-Key, falls in den Prefs keiner hinterlegt ist.
    // Du kannst hier Base64 ODER einen Roh-String eintragen. Beispiel (Base64 von "0123456789abcdef"):
    // private const val DEFAULT_KEY_B64_OR_RAW = "MDEyMzQ1Njc4OWFiY2RlZg=="
    private const val DEFAULT_KEY_B64_OR_RAW: String = "+FT7XNKeH9E7bg/WGWldaSkjMjdNMXbZH1c83PPkLbg="   // <- ggf. setzen

    // Die Web-API erwartet POST auf /get_protokoll mit verschlüsseltem Body
    private fun buildUrl(host: String, port: Int): String =
        "http://$host:$port/get_protokoll"

    // Prüft Antwort-Header auf Kompression
    private fun responseSaysGzip(resp: Response): Boolean {
        val v = (resp.header("X-Content-Compressed")
            ?: resp.header("X-Compressed")
            ?: resp.header("Content-Encoding")
            ?: "").lowercase()
        return v.contains("gzip") || v == "1" || v == "true" || v == "yes"
    }

    private val OCTET = "application/octet-stream".toMediaType()

    private val http by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
    // -------- Helper -------------------------------------------------------


    private fun firstLines(text: String, maxLines: Int = 5, maxCols: Int = 240): String {
        val lines = text.split('\n')
        val builder = StringBuilder()
        val take = lines.size.coerceAtMost(maxLines)
        for (i in 0 until take) {
            val line = lines[i].trimEnd('\r')
            val clip = if (line.length > maxCols) line.substring(0, maxCols) + " …" else line
            builder.append(clip).append('\n')
        }
        return builder.toString().trimEnd()
    }
    // -------- Key-Parsing & Krypto -----------------------------------------

    /**
     * Nimmt Base64 ODER Roh-String. Gibt AES-Key-Bytes zurück (16/24/32).
     * - Erst versucht Base64 strikt zu dekodieren.
     * - Wenn das nicht passt, als Roh-UTF8 interpretieren.
     * - Liefert null, wenn nichts davon 16/24/32 Byte hat.
     */
    private fun parseKeyFlexible(input: String?): ByteArray? {
        val s = (input ?: "").trim()
        if (s.isEmpty()) return null

        // 1) Base64 strikt probieren
        runCatching {
            val raw = Base64.decode(s, Base64.DEFAULT)
            if (raw.size == 16 || raw.size == 24 || raw.size == 32) return raw
        }

        // 2) Roh-String (UTF-8) probieren
        val raw2 = s.toByteArray(Charsets.UTF_8)
        if (raw2.size == 16 || raw2.size == 24 || raw2.size == 32) return raw2

        return null
    }

    /** Holt Key-Bytes aus Prefs oder Fallback; niemals leeres Array zurückgeben. */
    private fun resolveAesKeyBytes(prefsKey: String?, onLog: (String) -> Unit): ByteArray? {
        parseKeyFlexible(prefsKey)?.let { return it }
        parseKeyFlexible(DEFAULT_KEY_B64_OR_RAW)?.let {
            onLog("Hinweis: Fallback-AES-Key verwendet.")
            return it
        }
        onLog("Kein gültiger PRIVATE_KEY gefunden (Prefs oder Fallback).")
        return null
    }

    /** AES/ECB/PKCS5Padding – entschlüsseln. */
    private fun aesEcbDecrypt(cipher: ByteArray, keyBytes: ByteArray): ByteArray {
        val sk = SecretKeySpec(keyBytes, "AES")
        val c = Cipher.getInstance("AES/ECB/PKCS5Padding")
        c.init(Cipher.DECRYPT_MODE, sk)
        return c.doFinal(cipher)
    }

    /** AES/ECB/PKCS5Padding – verschlüsseln. */
    private fun aesEcbEncrypt(plain: ByteArray, keyBytes: ByteArray): ByteArray {
        val sk = SecretKeySpec(keyBytes, "AES")
        val c = Cipher.getInstance("AES/ECB/PKCS5Padding")
        c.init(Cipher.ENCRYPT_MODE, sk)
        return c.doFinal(plain)
    }

    /** ggf. GZIP entpacken (per Header oder Magic-Bytes erkannt). */
    private fun maybeGunzip(bytes: ByteArray, headerSaysGzip: Boolean): ByteArray {
        val looksGzip = bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()
        if (!headerSaysGzip && !looksGzip) return bytes
        return GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
    }

    // -------- Öffentliche API ----------------------------------------------

    /**
     * Holt ein Protokoll per HTTP-POST von der Web-API, entschlüsselt/entpackt und gibt das Envelope zurück.
     * Request-Body: verschlüsselte JSON { username, password, vn } als raw bytes.
     * Response-Body: verschlüsselte Bytes; Header X-Content-Compressed:gzip möglich.
     */
    suspend fun fetchProtokoll(
        context: Context,
        vertragsnummer: String,
        onLog: (String) -> Unit = {}
    ): ProtokollEnvelope? = withContext(Dispatchers.IO) {
        val prefs = AppPrefs.load(context)
        if (AppPrefs.isEmpty(prefs)) {
            onLog("Einstellungen unvollständig (Host/Port).")
            return@withContext null
        }
        if (vertragsnummer.isBlank()) {
            onLog("Vertragsnummer ist leer.")
            return@withContext null
        }

        // --- Key robust auflösen (kein Empty-Key mehr) ---
        val keyBytes = resolveAesKeyBytes(prefs.aesB64, onLog)
            ?: return@withContext null

        val url = buildUrl(prefs.host, prefs.port)
        onLog("Verbinde zu Web-API…")
        Log.d("ReceiveHTTP", "POST $url")

        try {
            // 1) Anfrage-JSON wie im Python-Client bauen
            val vn = vertragsnummer.trim().let { if (it.uppercase().startsWith("VN")) it else "VN$it" }
            val reqJson = """{"username":"${prefs.user}","password":"${prefs.pass}","vn":"$vn"}"""
            val encBody = aesEcbEncrypt(reqJson.toByteArray(Charsets.UTF_8), keyBytes)

            // 2) POST request vorbereiten (Body = ciphertext bytes)
            val request = Request.Builder()
                .url(url)
                .post(encBody.toRequestBody(OCTET))
                .header("Content-Type", OCTET.toString())
                .header("Accept", OCTET.toString())
                .header("X-Client", "Android")
                .build()

            // 3) Senden
            val resp = http.newCall(request).execute()
            resp.use { r ->
                if (!r.isSuccessful) {
                    val code = r.code
                    val msg = r.message
                    val bodyPreview = r.body?.string()?.take(300) ?: ""
                    onLog("HTTP-Fehler: $code $msg ${if (bodyPreview.isNotEmpty()) "– $bodyPreview" else ""}")
                    Log.e("ReceiveHTTP", "HTTP $code: $msg / $bodyPreview")
                    return@withContext null
                }

                val cipherBytes = r.body?.bytes()
                if (cipherBytes == null || cipherBytes.isEmpty()) {
                    onLog("Leerer Antwort-Body.")
                    return@withContext null
                }

                val headerSaysGzip = responseSaysGzip(r)
                onLog("Antwort empfangen (${cipherBytes.size} B, gzipHeader=$headerSaysGzip).")

                // 4) Entschlüsseln
                val decrypted = runCatching {
                    aesEcbDecrypt(cipherBytes, keyBytes)
                }.onFailure {
                    Log.e("ReceiveHTTP", "AES-Decrypt fehlgeschlagen", it)
                    onLog("AES-Decrypt fehlgeschlagen (Key korrekt?).")
                }.getOrNull() ?: return@withContext null

                // 5) ggf. ent-gzippen (per Header oder Magic-Bytes)
                val plainBytes = runCatching {
                    maybeGunzip(decrypted, headerSaysGzip)
                }.onFailure {
                    Log.e("ReceiveHTTP", "GZIP-Entpackung fehlgeschlagen", it)
                    onLog("GZIP-Entpackung fehlgeschlagen.")
                }.getOrNull() ?: return@withContext null

                val text = plainBytes.toString(Charsets.UTF_8)
                onLog("Payload entschlüsselt (${text.length} Zeichen).")

                // Optional: X-Format prüfen (JSON/TSV). Wir erwarten JSON für ProtokollCodec.
                val env = runCatching { ProtokollCodec.decode(text) }
                    .onFailure {
                        Log.e("ReceiveHTTP", "JSON Decode fehlgeschlagen", it)
                        onLog("JSON Decode fehlgeschlagen.")}
                    .getOrNull() ?: return@withContext null

                // 6) Optional local cache
                ProtokollStorage.save(context, vn, text)
                onLog("Protokoll gespeichert.")

                return@withContext env
            }
        } catch (t: Throwable) {
            Log.e("ReceiveHTTP", "Fehler: ${t.message}", t)
            onLog("Netzwerk-/Verarbeitungsfehler: ${t.message}")
            null
        }
    }
}
