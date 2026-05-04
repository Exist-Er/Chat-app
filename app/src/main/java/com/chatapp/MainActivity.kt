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
                var isGroupChat by remember { mutableStateOf(false) }
                var isCreatingGroup by remember { mutableStateOf(false) }
                
                val myId = currentUserId ?: ""

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
                } else if (isCreatingGroup) {
                    com.chatapp.ui.screens.CreateGroupScreen(
                        onBack = { isCreatingGroup = false },
                        onCreateGroup = { name, members ->
                            lifecycleScope.launch {
                                val groupId = app.repository.createGroup(name, members, myId)
                                if (groupId != null) {
                                    recipientId = groupId
                                    isGroupChat = true
                                    isCreatingGroup = false
                                } else {
                                    isCreatingGroup = false
                                    // Handle failure? Toast?
                                }
                            }
                        }
                    )
                } else if (recipientId == null) {
                    LoginScreen(
                        title = "WHO TO CHAT?",
                        placeholder = "Enter recipient or group ID",
                        buttonText = "OPEN CHAT",
                        onLogin = { id ->
                            val fullId = if (id.startsWith("dev_")) id else "dev_$id"
                            recipientId = fullId
                            // Simple heuristic for now: longer ID = group? Or just default false.
                            // If it's a UUID, it's likely a group or message ID.
                            // But for now, default false. User can create group to enter group mode.
                            isGroupChat = false 
                        }
                    )
                    // Temporary Group creation trigger:
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier.fillMaxSize(), 
                        contentAlignment = androidx.compose.ui.Alignment.BottomCenter
                    ) {
                        androidx.compose.material3.TextButton(onClick = { isCreatingGroup = true }) {
                            androidx.compose.material3.Text("OR CREATE A NEW GROUP", color = androidx.compose.ui.graphics.Color.Black, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                        }
                    }
                } else {
                    // Chat Interface
                    val otherId = recipientId!!
                    
                    val viewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        key = "$myId-$otherId",
                        factory = ChatViewModel.Factory(
                            app.repository,
                            otherId, 
                            otherId,
                            myId,
                            isGroupChat 
                        )
                    )
                    
                    val messages by viewModel.messages.collectAsState(initial = emptyList())
                    
                    ChatScreen(
                        messages = messages,
                        chatName = if (isGroupChat) "Group Chat" else otherId.removePrefix("dev_").uppercase(), // TODO: Get group name from DB if group
                        currentUserId = myId,
                        isGroup = isGroupChat,
                        onBack = { 
                            recipientId = null 
                            isGroupChat = false
                        },
                        onSend = { content ->
                            viewModel.sendMessage(content)
                        }
                    )
                }
            }
        }
    }
}
