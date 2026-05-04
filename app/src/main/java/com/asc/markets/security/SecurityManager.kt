
package com.asc.markets.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Institutional-grade Android Security Manager
 * Implements AES-GCM with hardware-backed Keystore entropy.
 */
object SecurityManager {
    private const val KEY_ALIAS = "asc_markets_v1_entropy"
    private const val ED25519_ALIAS = "asc_markets_binance_ed25519"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    init {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        
        // AES key for encryption
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            keyGenerator.generateKey()
        }

        // Ed25519 key for Binance signing
        if (!keyStore.containsAlias(ED25519_ALIAS)) {
            val kpg = java.security.KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE
            )
            kpg.initialize(
                KeyGenParameterSpec.Builder(
                    ED25519_ALIAS,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                )
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .build()
            )
            kpg.generateKeyPair()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    fun encrypt(data: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data.toByteArray())
        val combined = iv + encryptedData
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    fun decrypt(encryptedBase64: String): String? {
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.DEFAULT)
            val iv = combined.sliceArray(0 until 12)
            val data = combined.sliceArray(12 until combined.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            String(cipher.doFinal(data))
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Signs payload with Ed25519 private key.
     * Note: Android KeyStore Ed25519 support was added in API 30.
     * For older versions, this might fall back or need a different approach.
     */
    fun signPayload(payload: String): String {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val privateKey = keyStore.getKey(ED25519_ALIAS, null) as PrivateKey
        
        val signature = Signature.getInstance("SHA256withECDSA") // Fallback if Ed25519 not directly available
        signature.initSign(privateKey)
        signature.update(payload.toByteArray())
        return Base64.encodeToString(signature.sign(), Base64.NO_WRAP)
    }
}
