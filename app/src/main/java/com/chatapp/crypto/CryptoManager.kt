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
    
    // 3. Helper for Base64 Keys
    fun getMyPublicKeyBase64(): String {
        val privateHandle = getOrGenerateIdentityKeysetHandle()
        val publicHandle = privateHandle.publicKeysetHandle
        
        val outputStream = java.io.ByteArrayOutputStream()
        com.google.crypto.tink.CleartextKeysetHandle.write(
            publicHandle,
            com.google.crypto.tink.JsonKeysetWriter.withOutputStream(outputStream)
        )
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    fun parsePublicKey(base64PublicKey: String): KeysetHandle {
        val keyBytes = Base64.decode(base64PublicKey, Base64.NO_WRAP)
        val inputStream = java.io.ByteArrayInputStream(keyBytes)
        return com.google.crypto.tink.CleartextKeysetHandle.read(
            com.google.crypto.tink.JsonKeysetReader.withInputStream(inputStream)
        )
    }

    // 4. Easy Encryption/Decryption Strings
    fun encryptToB64(plaintext: String, recipientPublicHandle: KeysetHandle): String {
        val encryptor = recipientPublicHandle.getPrimitive(HybridEncrypt::class.java)
        val ciphertext = encryptor.encrypt(plaintext.toByteArray(Charsets.UTF_8), null) // Context info null for now
        return Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    fun decryptFromB64(ciphertextB64: String): String {
        val ciphertext = Base64.decode(ciphertextB64, Base64.NO_WRAP)
        val handle = getOrGenerateIdentityKeysetHandle()
        val decryptor = handle.getPrimitive(HybridDecrypt::class.java)
        val plaintextBytes = decryptor.decrypt(ciphertext, null)
        return String(plaintextBytes, Charsets.UTF_8)
    }

    // 5. Group Key Management (Symmetric AEAD)
    
    fun generateGroupKey(): KeysetHandle {
        return KeysetHandle.generateNew(com.google.crypto.tink.aead.AeadKeyTemplates.AES128_GCM)
    }

    // Encrypt a group keyset for a specific member using their public key
    fun encryptGroupKeyForMember(groupKeyHandle: KeysetHandle, memberPublicHandle: KeysetHandle): String {
        // 1. Serialize group key to bytes
        val outputStream = java.io.ByteArrayOutputStream()
        com.google.crypto.tink.CleartextKeysetHandle.write(
            groupKeyHandle,
            com.google.crypto.tink.JsonKeysetWriter.withOutputStream(outputStream)
        )
        val groupKeyBytes = outputStream.toByteArray()
        val groupKeyJson = String(groupKeyBytes, Charsets.UTF_8)
        
        // 2. Encrypt the serialized key using the member's public key (Hybrid Encryption)
        return encryptToB64(groupKeyJson, memberPublicHandle)
    }

    // Decrypt a received group keyset using my private identity key
    fun importGroupKeyFromB64(encryptedGroupKeyB64: String): KeysetHandle {
        // 1. Decrypt the blob using my private key
        val groupKeyJson = decryptFromB64(encryptedGroupKeyB64)
        
        // 2. Parse the JSON back into a KeysetHandle
        val inputStream = java.io.ByteArrayInputStream(groupKeyJson.toByteArray(Charsets.UTF_8))
        return com.google.crypto.tink.CleartextKeysetHandle.read(
            com.google.crypto.tink.JsonKeysetReader.withInputStream(inputStream)
        )
    }

    // Symmetric Encryption for Group Messages
    fun encryptWithGroupKey(plaintext: String, groupKeyHandle: KeysetHandle): String {
        val aead = groupKeyHandle.getPrimitive(Aead::class.java)
        val ciphertext = aead.encrypt(plaintext.toByteArray(Charsets.UTF_8), null)
        return Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    fun decryptWithGroupKey(ciphertextB64: String, groupKeyHandle: KeysetHandle): String {
        val ciphertext = Base64.decode(ciphertextB64, Base64.NO_WRAP)
        val aead = groupKeyHandle.getPrimitive(Aead::class.java)
        val plaintextBytes = aead.decrypt(ciphertext, null)
        return String(plaintextBytes, Charsets.UTF_8)
    }

    // Legacy/Internal methods if needed, but the above are the main API now.
}
