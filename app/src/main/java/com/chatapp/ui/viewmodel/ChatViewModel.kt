package com.chatapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chatapp.data.model.Message
import com.chatapp.data.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository,
    private val chatId: String,
    private val recipientId: String,
    private val senderId: String
) : ViewModel() {

    // Messages Flow
    // Observe messages for this specific chat
    val messages: Flow<List<Message>> = repository.getMessages(chatId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun sendMessage(content: String) {
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                recipientId = recipientId,
                content = content,
                senderId = senderId
            )
        }
    }

    // Factory to enable dependency injection without Hilt/Dagger
    class Factory(
        private val repository: ChatRepository,
        private val chatId: String,
        private val recipientId: String,
        private val senderId: String
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(repository, chatId, recipientId, senderId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
