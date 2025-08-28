package com.eno.protokolle.network


import android.content.Context
import android.util.Log
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import com.eno.protokolle.newmodel.ProtokollEnvelope
import com.eno.protokolle.newmodel.ProtokollCodec
import android.content.Context.MODE_PRIVATE

object ReceiveAndDecode {

	// Neuer Empfang für das *neue* Protokoll-JSON
	suspend fun receiveProtokollNew(
        context: Context,
        vertragsnummer: String
    ): ProtokollEnvelope? {
        return try {
            val prefs = context.getSharedPreferences("app_settings", MODE_PRIVATE)
            val serverHost = prefs.getString("server", null)
            val port = prefs.getString("port", null)?.toIntOrNull()
            val privateKey = prefs.getString("key", null)
            val username = prefs.getString("user", null)
            val password = prefs.getString("password", null)

            if (serverHost.isNullOrEmpty() || port == null || privateKey.isNullOrEmpty() ||
                username.isNullOrEmpty() || password.isNullOrEmpty()
            ) {
                Log.e("ReceiveNew", "Einstellungen unvollständig!")
                return null
            }
            Socket(serverHost, port).use { socket ->
                val output = DataOutputStream(socket.getOutputStream())
                val input = DataInputStream(socket.getInputStream())

                // ✉️ Anfrage senden (username + pw + vn)
                val payload = "$username|$password|$vertragsnummer"
                val encryptedRequest = NetworkHelper.encryptAES(payload, privateKey)
                output.writeInt(encryptedRequest.size)
                output.write(encryptedRequest)
                output.flush()

                // 📥 Antwort lesen
                val length = input.readInt()
                val encryptedResponse = ByteArray(length)
                input.readFully(encryptedResponse)

                // 🔓 Antwort entschlüsseln
                val decryptedJson = NetworkHelper.decryptAES(encryptedResponse, privateKey)

                // 🧱 Byte-basierte JSON-Daten decodieren
                val env = ProtokollCodec.decode(decryptedJson)
 
                // 📥 Optional: lokal cachen
                ProtokollStorage.save(context, vertragsnummer, decryptedJson)

                env
            }
        } catch (e: Exception) {
            Log.e("ReceiveNew", "Fehler: ${e.message}", e)
            null
        }

    }
 }
