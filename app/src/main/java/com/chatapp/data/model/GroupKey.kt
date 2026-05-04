package com.chatapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "group_keys")
data class GroupKey(
    @PrimaryKey
    val groupId: String,
    val keyVersion: Int,
    val encryptedKeyB64: String, // Encrypted with my public key (for distribution) or just the raw material? 
                                 // Wait, if I'm the creator, I store the key. If I'm a member, I receive it encrypted with my pub key.
                                 // So I should store it as received. 
                                 // ACTUALLY: Tink handles keysets. The `encryptedKeyB64` here is the blob I can feed into `importGroupKeyFromB64`.
                                 // `importGroupKeyFromB64` expects it to be encrypted with my *private* identity key (hybrid decryption).
                                 // So yes, this stores the blob allocated to ME.
    val timestamp: Long
)
