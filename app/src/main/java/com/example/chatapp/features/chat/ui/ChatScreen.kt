package com.example.chatapp.features.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatapp.features.chat.Message
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// Light Mode Bubbly Palette
private val SenderBubbleColor = Color(0xFFD1E4FF) // Soft Light Blue
private val ReceiverBubbleColor = Color(0xFFF1F1F1) // Very Light Grey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<Message>,
    onSendMessage: (String, Boolean) -> Unit, // Updated to take temporary flag
    onSearchDate: () -> Unit
) {
    var textState by remember { mutableStateOf("") }
    var isTempMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat App", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSearchDate) {
                        Icon(Icons.Default.Search, contentDescription = "Search by Date")
                    }
                    // Temporary Mode Toggle
                    Text(
                        text = if (isTempMode) "🔥 Temp Mode" else "Normal",
                        fontSize = 12.sp,
                        color = if (isTempMode) Color.Red else Color.Gray,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isTempMode) Color(0xFFFFEBEE) else Color.Transparent)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Switch(
                        checked = isTempMode,
                        onCheckedChange = { isTempMode = it },
                        modifier = Modifier.scale(0.8f) // Make it slightly smaller
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // ... rest remains similar but onSendMessage call changes
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                reverseLayout = true,
                contentPadding = PaddingValues(16.dp)
            ) {
                // messages is assumed to be sorted latest first for reverseLayout
                itemsIndexed(messages) { index, message ->
                    val prevMessage = if (index < messages.size - 1) messages[index + 1] else null
                    val isGrouped = prevMessage != null && 
                                    prevMessage.senderId == message.senderId &&
                                    ChronoUnit.MINUTES.between(prevMessage.timestamp, message.timestamp) < 15

                    ChatBubble(message, isGrouped)
                }
            }

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (textState.isNotBlank()) {
                            onSendMessage(textState, isTempMode)
                            textState = ""
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF6200EE))
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message, isGrouped: Boolean) {
    val alignment = if (message.isMine) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (message.isMine) SenderBubbleColor else ReceiverBubbleColor
    val corners = if (message.isMine) {
        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = if (isGrouped) 24.dp else 4.dp)
    } else {
        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = if (isGrouped) 24.dp else 4.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isGrouped) 2.dp else 4.dp),
        horizontalAlignment = alignment
    ) {
        if (!isGrouped && !message.isMine) {
            Text(
                text = "User ${message.senderId.take(4)}", 
                fontSize = 12.sp, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
            )
        }

        Box(
            modifier = Modifier
                .clip(corners)
                .background(bubbleColor)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column {
                Text(text = message.content, color = Color.Black)
                if (message.isTemporary) {
                    Text(
                        text = "🔥 Disappearing message",
                        fontSize = 8.sp,
                        color = Color.Red.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (!isGrouped) {
            Text(
                text = message.timestamp.format(DateTimeFormatter.ofPattern("HH:mm")),
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}
