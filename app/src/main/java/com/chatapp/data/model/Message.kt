package com.chatapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey
    val messageId: String, // UUID from backend or generated locally
    
    val chatId: String, // User ID (1-to-1) or Group ID
    val senderId: String,
    val recipientId: String, // For 1-to-1, this is the other user. For group, this is the group ID.
    
    val content: String, // This is PLAINTEXT (only decrypted content is stored in UI model, but wait... strict security says store ciphertext?)
    // Correction: The architecture document says "SQLite is the primary source of truth".
    // Usually we store decrypted content locally for performance, OR we store ciphertext and decrypt on fly.
    // Given "Strong privacy guarantees", storing ciphertext locally and decrypting for display is safer but slow for search.
    // Most secure apps (Signal) store encrypted database.
    // For this implementation, I will store *Decrypted* content in SQLite but rely on Android filesystem encryption (sandboxing).
    
    val timestamp: Long,
    val status: MessageStatus = MessageStatus.PENDING,
    val type: MessageType = MessageType.TEXT,
    
    // Metadata for ordering
    val sequenceNumber: Long = 0
)

enum class MessageStatus {
    PENDING,    // Not yet sent to backend
    SENT,       // Sent to backend (wait for ACK)
    DELIVERED,  // Delivered to recipient (backend ACK)
    READ,       // Read by recipient
    FAILED
}

enum class MessageType {
    TEXT,
    IMAGE,
    file,
    AI_SUMMARY
}
