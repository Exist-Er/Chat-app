package com.example.chatapp.features.chat

import java.time.LocalDateTime

data class Message(
    val id: String,
    val content: String,
    val senderId: String,
    val receiverId: String? = null,
    val groupId: String? = null,
    val timestamp: LocalDateTime,
    val isTemporary: Boolean = false,
    val isMine: Boolean = false
)

data class Group(
    val id: String,
    val name: String,
    val memberIds: List<String>
)
