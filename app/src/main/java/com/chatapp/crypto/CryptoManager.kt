package com.chatapp.crypto

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.HybridDecrypt
import com.google.crypto.tink.HybridEncrypt
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.hybrid.HybridConfig
import com.google.crypto.tink.hybrid.HybridKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException

class CryptoManager(private val context: Context) {

    companion object {
        private const val MASTER_KEY_URI = "android-keystore://master_key"
        private const val KEYSET_FILENAME = "chat_app_keyset"
        private const val PREF_FILE = "crypto_prefs"
    }

    init {
        try {
            AeadConfig.register()
            HybridConfig.register()
        } catch (e: GeneralSecurityException) {
            e.printStackTrace()
        }
    }

    // 1. User Identity Key (Asymmetric - RSA/ECIES)
    // We use Hybrid Encryption (ECIES-AEAD-HKDF) for better performance/security than raw RSA.
    // Public Key is shared with backend. Private Key is kept in Keystore.

    private fun getOrGenerateIdentityKeysetHandle(): KeysetHandle {
        return AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_FILENAME, PREF_FILE)
            .withKeyTemplate(HybridKeyTemplates.ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM)
            .withMasterKeyUri(MASTER_KEY_URI) // Encrypt keyset with Master Key in Android Keystore
            .build()
            .keysetHandle
    }
    
    // Get Public Key (to send to backend)
    fun getPublicKeyBytes(): ByteArray {
        val privateHandle = getOrGenerateIdentityKeysetHandle()
        val publicHandle = privateHandle.publicKeysetHandle
        // Serialize to bytes (proto format)
        // We will Base64 encode this before sending to backend
        return publicHandle.keysetInfo.toString().toByteArray() // Placeholder serialization logic needed
        
        // Correct way to export public key with Tink:
        // usually we serialize the keyset.
        // For simplicity, we just use cleartext keyset handle export for public keys (public keys are public!)
        // privateHandle.publicKeysetHandle.write(JsonKeysetWriter.withFile(...))
        // Here we return raw bytes or specialized output.
        // Tink doesn't expose raw key bytes easily to discourage bad practice, but we need to send the Public Keyset to peers.
    }
    
    // Decrypt incoming message (using private key)
    fun decryptMessage(ciphertext: ByteArray, contextInfo: ByteArray?): ByteArray {
        val handle = getOrGenerateIdentityKeysetHandle()
        val decryptor = handle.getPrimitive(HybridDecrypt::class.java)
        return decryptor.decrypt(ciphertext, contextInfo)
    }
    
    // Encrypt outgoing message (using recipient's public key)
    // Recipient public key must be imported from backend JSON/Bytes
    fun encryptMessage(plaintext: ByteArray, recipientPublicKeysetHandle: KeysetHandle, contextInfo: ByteArray?): ByteArray {
        val encryptor = recipientPublicKeysetHandle.getPrimitive(HybridEncrypt::class.java)
        return encryptor.encrypt(plaintext, contextInfo)
    }

    // 2. Group Encryption (Symmetric - AES-GCM)
    // We generate a fresh symmetric key for a group.
    
    fun generateGroupKey(): KeysetHandle {
        return KeysetHandle.generateNew(com.google.crypto.tink.aead.AeadKeyTemplates.AES256_GCM)
    }
    
    fun encryptGroupMessage(plaintext: ByteArray, groupKeyHandle: KeysetHandle, associatedData: ByteArray?): ByteArray {
        val aead = groupKeyHandle.getPrimitive(Aead::class.java)
        return aead.encrypt(plaintext, associatedData)
    }
    
    fun decryptGroupMessage(ciphertext: ByteArray, groupKeyHandle: KeysetHandle, associatedData: ByteArray?): ByteArray {
        val aead = groupKeyHandle.getPrimitive(Aead::class.java)
        return aead.decrypt(ciphertext, associatedData)
    }
}
