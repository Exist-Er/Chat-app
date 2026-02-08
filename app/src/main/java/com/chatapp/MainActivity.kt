package com.chatapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.chatapp.ui.screens.ChatScreen
import com.chatapp.ui.screens.LoginScreen
import com.chatapp.ui.theme.MangaTheme
import com.chatapp.ui.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = application as ChatApplication

        setContent {
            MangaTheme {
                var currentUserId by remember { mutableStateOf<String?>(null) }
                var recipientId by remember { mutableStateOf<String?>(null) }

                if (currentUserId == null) {
                    LoginScreen(
                        title = "WHO ARE YOU?",
                        placeholder = "Enter your user ID (e.g. alice)",
                        buttonText = "LOG IN",
                        onLogin = { id ->
                            val fullId = if (id.startsWith("dev_")) id else "dev_$id"
                            currentUserId = fullId
                            
                            // Register user (upload public key) then connect
                            lifecycleScope.launch {
                                app.repository.registerUser(fullId)
                                app.webSocketClient.connect(fullId)
                            }
                        }
                    )
                } else if (recipientId == null) {
                    LoginScreen(
                        title = "WHO TO CHAT?",
                        placeholder = "Enter recipient ID (e.g. bob)",
                        buttonText = "START CHAT",
                        onLogin = { id ->
                            val fullId = if (id.startsWith("dev_")) id else "dev_$id"
                            recipientId = fullId
                        }
                    )
                } else {
                    // Chat Interface
                    // Use a key to recreate ViewModel if IDs change
                    val chatId = recipientId!! // IN 1-to-1, chat ID is usually the other person or unique. 
                    // For now, let's treat chatId as recipientId for simplicity or generating a unique one.
                    // Actually, ChatID in 1-to-1 is usually a unique ID like "user1-user2" sorted.
                    // But Repository expects chatId to filter messages.
                    // Let's use the recipientId as chatId for this "Direct Message" view.
                    
                    val myId = currentUserId!!
                    val otherId = recipientId!!
                    
                    val viewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        key = "$myId-$otherId",
                        factory = ChatViewModel.Factory(
                            app.repository,
                            otherId, // ChatID = RecipientID (Simple assumption for now)
                            otherId,
                            myId
                        )
                    )
                    
                    val messages by viewModel.messages.collectAsState(initial = emptyList())
                    
                    ChatScreen(
                        messages = messages,
                        chatName = otherId.removePrefix("dev_").uppercase(),
                        currentUserId = myId,
                        onBack = { recipientId = null },
                        onSend = { content ->
                            viewModel.sendMessage(content)
                        }
                    )
                }
            }
        }
    }
}
