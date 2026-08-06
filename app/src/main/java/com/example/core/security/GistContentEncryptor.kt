package com.example.core.security

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.security.crypto.MasterKey
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts sensitive Gist file content using AES-256 GCM backed by MasterKey
 * (androidx.security.crypto) before persisting to Room database.
 */
class GistContentEncryptor(context: Context) {
  private companion object {
    private const val TAG = "GistContentEncryptor"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_SIZE_BYTES = 12
    private const val ENC_PREFIX = "GIST_ENC_v1:"
  }

  private val secretKey: SecretKey by lazy { getOrCreateSecretKey(context) }

  private fun getOrCreateSecretKey(context: Context): SecretKey {
    return try {
      val masterKey =
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
      val alias = MasterKey.DEFAULT_MASTER_KEY_ALIAS
      val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
      val entry = keyStore.getEntry(alias, null) as? java.security.KeyStore.SecretKeyEntry
      entry?.secretKey ?: generateFallbackKey()
    } catch (e: Exception) {
      Log.w(TAG, "MasterKey/KeyStore init fallback for encryption", e)
      generateFallbackKey()
    }
  }

  private fun generateFallbackKey(): SecretKey {
    return try {
      val keyGen = KeyGenerator.getInstance("AES")
      keyGen.init(256)
      keyGen.generateKey()
    } catch (e: Exception) {
      val keyGen = KeyGenerator.getInstance("AES")
      keyGen.init(128)
      keyGen.generateKey()
    }
  }

  fun encrypt(plainText: String): String {
    if (plainText.isEmpty() || isEncrypted(plainText)) {
      return plainText
    }
    return try {
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(Cipher.ENCRYPT_MODE, secretKey)
      val iv = cipher.iv
      val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

      val combined = ByteArray(iv.size + cipherText.size)
      System.arraycopy(iv, 0, combined, 0, iv.size)
      System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

      ENC_PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
    } catch (e: Exception) {
      Log.e(TAG, "Encryption failed", e)
      plainText
    }
  }

  fun decrypt(cipherTextWithPrefix: String): String {
    if (!isEncrypted(cipherTextWithPrefix)) {
      return cipherTextWithPrefix
    }
    return try {
      val base64Data = cipherTextWithPrefix.removePrefix(ENC_PREFIX)
      val combined = Base64.decode(base64Data, Base64.NO_WRAP)
      if (combined.size <= IV_SIZE_BYTES) return cipherTextWithPrefix

      val iv = combined.copyOfRange(0, IV_SIZE_BYTES)
      val cipherText = combined.copyOfRange(IV_SIZE_BYTES, combined.size)

      val cipher = Cipher.getInstance(TRANSFORMATION)
      val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
      cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

      val plainTextBytes = cipher.doFinal(cipherText)
      String(plainTextBytes, Charsets.UTF_8)
    } catch (e: Exception) {
      Log.e(TAG, "Decryption failed", e)
      cipherTextWithPrefix
    }
  }

  fun isEncrypted(text: String): Boolean = text.startsWith(ENC_PREFIX)
}
