package com.eno.protokolle.network

import android.content.Context
import android.util.Log
import com.eno.protokolle.newmodel.ProtokollCodec
import com.eno.protokolle.newmodel.ProtokollEnvelope
import com.eno.protokolle.prefs.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket

object ReceiveAndDecode {

    // Neuer Empfang für das *neue* Protokoll-JSON
    suspend fun receiveProtokollNew(
        context: Context,
        vertragsnummer: String
    ): ProtokollEnvelope? = withContext(Dispatchers.IO) {
        try {
            val prefs = AppPrefs.load(context)
            if (AppPrefs.isEmpty(prefs)) {
                Log.e("ReceiveNew", "Settings leer: Host/Port fehlen")
                return@withContext null
            }

            val serverHost = prefs.host
            val port = prefs.port
            val privateKey = prefs.aesB64
            val username = prefs.user
            val password = prefs.pass

            // Minimal-Validierung: falls du user/pass/aes zwingend brauchst
            if (serverHost.isBlank() || port !in 1..65535 ||
                privateKey.isBlank() || username.isBlank() || password.isBlank()
            ) {
                Log.e("ReceiveNew", "Einstellungen unvollständig!")
                return@withContext null
            }

            // Mit Timeout verbinden

            Socket().use { socket ->
                socket.connect(InetSocketAddress(serverHost, port), /* timeout ms */ 5000)

                DataOutputStream(socket.getOutputStream()).use { output ->
                    DataInputStream(socket.getInputStream()).use { input ->

                        // ✉️ Anfrage senden (username|password|vertragsnummer)
                        val payload = "$username|$password|$vertragsnummer"
                        val encryptedRequest = NetworkHelper.encryptAES(payload, privateKey)
                        output.writeInt(encryptedRequest.size)
                        output.write(encryptedRequest)
                        output.flush()

                        // 📥 Antwort lesen (Length-Prefix)
                        val length = input.readInt()
                        if (length <= 0 || length > 10 * 1024 * 1024) {
                            // einfacher Schutz gegen Unsinn
                            Log.e("ReceiveNew", "Unerwartete Antwortlänge: $length")
                            return@withContext null
                        }
                        val encryptedResponse = ByteArray(length)
                        input.readFully(encryptedResponse)

                        // 🔓 Entschlüsseln (AES/ECB entsprechend deiner NetworkHelper-Implementierung)
                        val decryptedJson = NetworkHelper.decryptAES(encryptedResponse, privateKey)

                        // 🧱 JSON decodieren
                        val env = ProtokollCodec.decode(decryptedJson)

                        // 💾 Optional: lokal cachen
                        ProtokollStorage.save(context, vertragsnummer, decryptedJson)

                        return@withContext env
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ReceiveNew", "Fehler: ${e.message}", e)
            null
        }
    }
}
