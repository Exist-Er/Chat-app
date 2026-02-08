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
    private val api: ChatApi,
    private val cryptoManager: CryptoManager,
    private val webSocketClient: WebSocketClient,
    private val scope: CoroutineScope
) {

    // 1. Get Messages (Observe Local DB)
    // The UI collects this Flow. When DB changes, UI updates automatically.
    fun getMessages(chatId: String): Flow<List<Message>> {
        return messageDao.getMessagesForChat(chatId)
    }

    // 2. Send Message
    suspend fun sendMessage(
        chatId: String,       // The ID of the chat (User ID for 1-to-1)
        recipientId: String,  // Who receives it
        content: String,       // Plaintext content
        senderId: String      // Me
    ) {
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        // A. Create Message Entity
        val message = Message(
            messageId = messageId,
            chatId = chatId,
            senderId = senderId,
            recipientId = recipientId,
            content = content, // Storing plaintext locally (as discussed)
            timestamp = timestamp,
            status = MessageStatus.PENDING,
            type = MessageType.TEXT
        )

        // B. Save to Local DB (UI updates immediately)
        messageDao.insertMessage(message)

        // C. Network Operations (Background)
        scope.launch(Dispatchers.IO) {
            try {
                // 1. Get Recipient's Public Key
                // (Optimally caching this would be better, but fetching for now)
                val keyResponse = api.getPublicKey(recipientId)
                if (!keyResponse.isSuccessful) {
                    Log.e("ChatRepository", "Failed to get public key for $recipientId")
                    messageDao.updateMessageStatus(messageId, MessageStatus.FAILED.name) // Using name for String converter
                    return@launch
                }
                val remotePublicKeyStr = keyResponse.body()?.public_key ?: return@launch

                // 2. Encrypt Content
                // We need to parse the remote key string back to a handle/key
                // For this example assuming CryptoManager handles string -> key conversion internal logic
                // Real impl needs: cryptoManager.parsePublicKey(remotePublicKeyStr)
                // val encryptedBytes = cryptoManager.encryptMessage(content.toByteArray(), remotePublicKey)
                
                // Placeholder encryption until we implement key parsing in CryptoManager
                val encryptedPayload = "ENCRYPTED_${content}" // Mock encryption for flow
                
                // 3. Create Backend Event
                val event = BackendEvent(
                    event_id = UUID.randomUUID().toString(),
                    recipient_id = recipientId,
                    sender_id = senderId,
                    event_type = "MESSAGE",
                    sequence = 0, // Backend handles sequence usually or we track it
                    timestamp = timestamp,
                    metadata = mapOf("chat_id" to chatId),
                    encrypted_payload = encryptedPayload // Base64 string in real app
                )

                // 4. Send to Backend
                val response = api.sendEvent(event)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    // 5. Update Local DB to SENT
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

    // 3. Observe Incoming Messages (from WebSocket)
    fun observeIncomingEvents() {
        scope.launch {
            webSocketClient.incomingEvents.collect { event ->
                handleIncomingEvent(event)
            }
        }
    }

    private suspend fun handleIncomingEvent(event: BackendEvent) {
        if (event.event_type == "MESSAGE") {
            try {
                // 1. Decrypt
                // val decryptedContent = cryptoManager.decryptMessage(event.encrypted_payload)
                val decryptedContent = event.encrypted_payload.removePrefix("ENCRYPTED_") // Mock

                // 2. Save to DB
                val message = Message(
                    messageId = event.metadata["message_id"] as? String ?: UUID.randomUUID().toString(),
                    chatId = event.sender_id ?: "Unknown", // In 1-to-1, chat is the sender
                    senderId = event.sender_id ?: "Unknown",
                    recipientId = event.recipient_id,
                    content = decryptedContent,
                    timestamp = event.timestamp,
                    status = MessageStatus.DELIVERED, // Received means delivered
                    type = MessageType.TEXT
                )
                
                messageDao.insertMessage(message)

                // 3. Send ACK
                webSocketClient.sendAck(event.event_id, event.recipient_id)
                
            } catch (e: Exception) {
                Log.e("ChatRepository", "Error processing incoming message", e)
            }
        }
    }
}
