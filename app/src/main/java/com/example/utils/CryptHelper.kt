package com.example.utils

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptHelper {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    
    private fun deriveKeyAndIv(password: String): Pair<SecretKeySpec, IvParameterSpec> {
        val md = MessageDigest.getInstance("SHA-256")
        val keyBytes = md.digest(password.toByteArray(Charsets.UTF_8))
        // Use first 16 bytes for IV
        val ivBytes = ByteArray(16)
        System.arraycopy(keyBytes, 0, ivBytes, 0, 16)
        
        return Pair(
            SecretKeySpec(keyBytes, "AES"),
            IvParameterSpec(ivBytes)
        )
    }

    fun encrypt(data: ByteArray, password: String): String {
        return try {
            val (secretKey, iv) = deriveKeyAndIv(password)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)
            val encryptedBytes = cipher.doFinal(data)
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun decrypt(encryptedBase64: String, password: String): ByteArray? {
        return try {
            val (secretKey, iv) = deriveKeyAndIv(password)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)
            val decodedBytes = Base64.decode(encryptedBase64, Base64.DEFAULT)
            cipher.doFinal(decodedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
