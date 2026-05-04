package com.chatapp.data.repository

import android.util.Log
import com.chatapp.crypto.CryptoManager
import com.chatapp.data.local.MessageDao
import com.chatapp.data.model.Message
import com.chatapp.data.model.MessageStatus
import com.chatapp.data.model.MessageType
import com.chatapp.network.BackendEvent
import com.chatapp.network.ChatApi
import com.chatapp.network.WebSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository(
    private val messageDao: MessageDao,
    private val groupKeyDao: com.chatapp.data.local.GroupKeyDao,
    private val api: ChatApi,
    private val cryptoManager: CryptoManager,
    private val webSocketClient: WebSocketClient,
    private val scope: CoroutineScope
) {

    // ... existing registerUser and getMessages ...

    // 0. Register User (Send Public Key to Server)
    suspend fun registerUser(userId: String) {
        val publicKey = cryptoManager.getMyPublicKeyBase64()
        try {
            // Using ID as token in dev mode
            val request = com.chatapp.network.UserRegistrationRequest(
                google_id_token = userId, 
                public_key = publicKey,
                display_name = userId.removePrefix("dev_")
            )
            val response = api.register(request)
            if (!response.isSuccessful) {
                Log.e("ChatRepository", "Registration failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Registration error", e)
        }
    }

    // 1. Get Messages (Observe Local DB)
    fun getMessages(chatId: String): Flow<List<Message>> {
        return messageDao.getMessagesForChat(chatId)
    }

    // 1.1 Create Group
    suspend fun createGroup(groupName: String, memberIds: List<String>, creatorId: String): String? {
        val groupId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        
        // 1. Generate Group Key
        val groupKeyHandle = cryptoManager.generateGroupKey()
        
        // 2. Store Locally (Creator needs it too!)
        val myEncryptedKey = cryptoManager.encryptGroupKeyForMember(groupKeyHandle, cryptoManager.getOrGenerateIdentityKeysetHandle().publicKeysetHandle)
        // Wait, locally I can just store the serialized key encrypted with my public key? 
        // Or better yet, rely on `encryptGroupKeyForMember` which does hybrid encryption.
        // `importGroupKeyFromB64` expects hybrid decryption.
        
        val groupKeyEntity = com.chatapp.data.model.GroupKey(
            groupId = groupId,
            keyVersion = 1,
            encryptedKeyB64 = myEncryptedKey,
            timestamp = timestamp
        )
        groupKeyDao.insertGroupKey(groupKeyEntity)

        // 3. Send GROUP_KEY_UPDATE to all members (including self? No, self is handled locally. But consistency?)
        // Let's send to others.
        
        memberIds.forEach { memberId ->
            if (memberId == creatorId) return@forEach // already stored

            scope.launch {
                val keyResponse = api.getPublicKey(memberId)
                val memberPubKeyStr = keyResponse.body()?.public_key ?: return@launch
                val memberPubKeyHandle = cryptoManager.parsePublicKey(memberPubKeyStr)
                
                val encryptedKeyForMember = cryptoManager.encryptGroupKeyForMember(groupKeyHandle, memberPubKeyHandle)
                
                val event = BackendEvent(
                    event_id = UUID.randomUUID().toString(),
                    recipient_id = memberId,
                    sender_id = creatorId,
                    event_type = "GROUP_KEY_UPDATE",
                    sequence = 0,
                    timestamp = timestamp,
                    metadata = mapOf(
                        "group_id" to groupId,
                        "group_name" to groupName,
                        "key_version" to "1"
                    ),
                    encrypted_payload = encryptedKeyForMember
                )
                api.sendEvent(event)
            }
        }
        
        return groupId
    }

    // 2. Send Message
    suspend fun sendMessage(
        chatId: String,       // UserID or GroupID
        recipientId: String,  // UserID or GroupID
        content: String,
        senderId: String,
        isGroup: Boolean = false
    ) {
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        // A. Create Message Entity
        val message = Message(
            messageId = messageId,
            chatId = chatId,
            senderId = senderId,
            recipientId = recipientId,
            content = content, 
            timestamp = timestamp,
            status = MessageStatus.PENDING,
            type = MessageType.TEXT
        )

        messageDao.insertMessage(message)

        scope.launch(Dispatchers.IO) {
            try {
                // B. Encrypt
                val encryptedPayload: String
                if (isGroup) {
                    // Get latest group key
                    val latestKey = groupKeyDao.getLatestGroupKey(chatId)
                    if (latestKey == null) {
                        Log.e("ChatRepository", "No key for group $chatId")
                        messageDao.updateMessageStatus(messageId, MessageStatus.FAILED.name)
                        return@launch
                    }
                    val keyHandle = cryptoManager.importGroupKeyFromB64(latestKey.encryptedKeyB64)
                    encryptedPayload = cryptoManager.encryptWithGroupKey(content, keyHandle)
                } else {
                    // 1-to-1 Logic
                    val keyResponse = api.getPublicKey(recipientId)
                    val remotePubKeyStr = keyResponse.body()?.public_key ?: return@launch
                    val recipientKeyHandle = cryptoManager.parsePublicKey(remotePubKeyStr)
                    encryptedPayload = cryptoManager.encryptToB64(content, recipientKeyHandle)
                }
                
                // C. Send Event
                val event = BackendEvent(
                    event_id = UUID.randomUUID().toString(),
                    recipient_id = recipientId,
                    sender_id = senderId,
                    event_type = "MESSAGE",
                    sequence = 0,
                    timestamp = timestamp,
                    metadata = mutableMapOf("chat_id" to chatId).apply {
                        if (isGroup) put("group_id", chatId)
                    },
                    encrypted_payload = encryptedPayload
                )

                val response = api.sendEvent(event)
                if (response.isSuccessful && response.body()?.success == true) {
                    messageDao.updateMessageStatus(messageId, MessageStatus.SENT.name)
                } else {
                    messageDao.updateMessageStatus(messageId, MessageStatus.FAILED.name)
                }
            } catch (e: Exception) {
                Log.e("ChatRepository", "Error sending message", e)
                messageDao.updateMessageStatus(messageId, MessageStatus.FAILED.name)
            }
        }
    }

    // 3. Observe Incoming Messages
    fun observeIncomingEvents() {
        scope.launch {
            webSocketClient.incomingEvents.collect { event ->
                handleIncomingEvent(event)
            }
        }
    }

    private suspend fun handleIncomingEvent(event: BackendEvent) {
        when (event.event_type) {
            "GROUP_KEY_UPDATE" -> {
                try {
                    val groupId = event.metadata["group_id"] ?: return
                    val keyVersion = event.metadata["key_version"]?.toIntOrNull() ?: 1
                    
                    // Store the encrypted key blob directly. 
                    // It is encrypted with MY public key, so `importGroupKeyFromB64` can decrypt it using my private key later.
                    val groupKey = com.chatapp.data.model.GroupKey(
                        groupId = groupId,
                        keyVersion = keyVersion,
                        encryptedKeyB64 = event.encrypted_payload, // Store as received
                        timestamp = event.timestamp
                    )
                    groupKeyDao.insertGroupKey(groupKey)
                    webSocketClient.sendAck(event.event_id, event.recipient_id)
                } catch (e: Exception) {
                    Log.e("ChatRepository", "Error storing group key", e)
                }
            }
            "MESSAGE" -> {
                try {
                    val groupId = event.metadata["group_id"] as? String // Custom metadata for group messages?
                    
                    val decryptedContent = if (groupId != null) {
                        // Group Message
                        val localKey = groupKeyDao.getLatestGroupKey(groupId)
                        if (localKey != null) {
                            val keyHandle = cryptoManager.importGroupKeyFromB64(localKey.encryptedKeyB64)
                            cryptoManager.decryptWithGroupKey(event.encrypted_payload, keyHandle)
                        } else {
                            "<Encrypted Group Message - Key Missing>"
                        }
                    } else {
                        // 1-to-1 Message
                        cryptoManager.decryptFromB64(event.encrypted_payload)
                    }

                    val message = Message(
                        messageId = event.event_id,
                        chatId = groupId ?: event.sender_id ?: "Unknown",
                        senderId = event.sender_id ?: "Unknown",
                        recipientId = event.recipient_id,
                        content = decryptedContent,
                        timestamp = event.timestamp,
                        status = MessageStatus.DELIVERED,
                        type = MessageType.TEXT
                    )
                    
                    messageDao.insertMessage(message)
                    webSocketClient.sendAck(event.event_id, event.recipient_id)
                } catch (e: Exception) {
                    Log.e("ChatRepository", "Error processing incoming message", e)
                }
            }
        }
    }
}
