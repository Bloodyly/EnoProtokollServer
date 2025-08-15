package com.eno.protokolle.network

import android.content.Context
import android.util.Base64
import android.util.Log
import com.eno.protokolle.MeldeGruppe
import com.eno.protokolle.ProtokollListe
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.net.Socket
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object ReceiveAndDecode {

    // 🔑 Beispiel-Mapping für Wartungstyp
    private val wartungstypMap = mapOf<Byte, String>(
        0x01.toByte() to "Quartal",
        0x02.toByte() to "Halbjahr",
        0x03.toByte() to "Jahr"
    )

    // 🔧 Beispiel-Mapping für Gruppen-Typen
    private val gruppenTypMap = mapOf<Byte, String>(
        0x01.toByte() to "AM",
        0x02.toByte() to "NAM",
        0x03.toByte() to "Koppler",
        0x04.toByte() to "RAS",
        0x05.toByte() to "Sirene"
        // Weitere können ergänzt werden
    )

    // 🔧 Mapping für Wartungswerte (4 Bit)
    private val wartungByteMap = mapOf(
        0x1 to "n.i.O.",
        0x2 to "Q1",
        0x3 to "Q2",
        0x4 to "Q3",
        0x5 to "Q4",
        0x6 to "H1",
        0x7 to "H2",
        0x8 to "i.O.",
        0xF to "-"
    )

    // 🔧 Mapping für Meldertypen (4 Bit)
    private val melderTypMap = mapOf(
        0x0 to "", // Normal
        0x1 to "TD-Max",
        0x2 to "TD-Diff",
        0x3 to "ZD",
        0xF to "-"
    )

    suspend fun receiveProtokoll(
        context: Context,
        vertragsnummer: String
    ): ProtokollListe? {
        return try {
            val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

            val serverHost = prefs.getString("server", null)
            val port = prefs.getString("port", null)?.toIntOrNull()
            val privateKey = prefs.getString("key", null)
            val username = prefs.getString("user", null)
            val password = prefs.getString("password", null)

            if (serverHost.isNullOrEmpty() || port == null || privateKey.isNullOrEmpty() ||
                username.isNullOrEmpty() || password.isNullOrEmpty()
            ) {
                Log.e("ReceiveAndDecode", "Einstellungen unvollständig!")
                return null
            }
            Socket(serverHost, port).use { socket ->

                val output = DataOutputStream(socket.getOutputStream())
                val input = DataInputStream(socket.getInputStream())

                // ✉️ Anfrage senden (username + pw + vn)
                val payload = "$username|$password|$vertragsnummer"
                val encryptedRequest = encryptAES(payload, privateKey)
                output.writeInt(encryptedRequest.size)
                output.write(encryptedRequest)
                output.flush()

                // 📥 Antwort lesen
                val length = input.readInt()
                val encryptedResponse = ByteArray(length)
                input.readFully(encryptedResponse)

                // 🔓 Antwort entschlüsseln
                val decryptedJson = decryptAES(encryptedResponse, privateKey)

                // 🧱 Byte-basierte JSON-Daten decodieren
                decodeProtokoll(decryptedJson)
            }
        } catch (e: Exception) {
            Log.e("ReceiveAndDecode", "Fehler: ${e.message}", e)
            null
        }
    }

    // ✨ Beispielhafter Decodierer (wird angepasst, wenn genaue Struktur bekannt)
    private fun decodeProtokoll(json: String): ProtokollListe? {
        try {
            val raw = org.json.JSONObject(json)

            val vn = raw.getString("vertragsnummer")
            val kunde = raw.getString("kundenname")
            val wartungstypByte = raw.getInt("wartungstyp").toByte()
            val wartungstyp = wartungstypMap[wartungstypByte] ?: "Quartal"

            val gruppenJson = raw.getJSONArray("gruppen")
            val gruppen = mutableListOf<MeldeGruppe>()

            for (i in 0 until gruppenJson.length()) {
                val g = gruppenJson.getJSONObject(i)
                val typ = gruppenTypMap[g.getInt("typ").toByte()] ?: "Unbekannt"
                val gruppe = g.getInt("gruppe").toString()
                val anzahl = g.getInt("anzahl")

                val werteBytes = g.getJSONArray("werte")
                val werte = mutableListOf<String>()
                val melderTypen = mutableListOf<String>()

                for (j in 0 until werteBytes.length()) {
                    val byteVal = werteBytes.getInt(j).toByte()
                    val wartungBits = (byteVal.toInt() and 0xF0) ushr 4
                    val melderBits = byteVal.toInt() and 0x0F

                    val wartungWert = wartungByteMap[wartungBits] ?: "-"
                    val melderTyp = melderTypMap[melderBits] ?: ""

                    werte.add(wartungWert)
                    melderTypen.add(melderTyp)
                }

                gruppen.add(MeldeGruppe(typ, gruppe, anzahl, werte, melderTypen))
            }

            return ProtokollListe(vn, kunde, wartungstyp, gruppen)
        } catch (e: Exception) {
            Log.e("Decode", "Fehler beim Dekodieren: ${e.message}", e)
            return null
        }
    }

    // 🔐 AES Verschlüsselung/Entschlüsselung
    private fun encryptAES(input: String, keyBase64: String): ByteArray {
        val key = Base64.decode(keyBase64, Base64.DEFAULT)
        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher.doFinal(input.toByteArray(Charsets.UTF_8))
    }

    private fun decryptAES(input: ByteArray, keyBase64: String): String {
        val key = Base64.decode(keyBase64, Base64.DEFAULT)
        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return String(cipher.doFinal(input), Charsets.UTF_8)
    }
}
