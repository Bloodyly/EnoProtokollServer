package com.eno.protokolle.network

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object NetworkHelper {

    // 🔐 AES Verschlüsselung/Entschlüsselung
    fun encryptAES(input: String, keyBase64: String): ByteArray {
        val key = Base64.decode(keyBase64, Base64.DEFAULT)
        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher.doFinal(input.toByteArray(Charsets.UTF_8))
    }

    fun decryptAES(input: ByteArray, keyBase64: String): String {
        val key = Base64.decode(keyBase64, Base64.DEFAULT)
        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return String(cipher.doFinal(input), Charsets.UTF_8)
    }
}