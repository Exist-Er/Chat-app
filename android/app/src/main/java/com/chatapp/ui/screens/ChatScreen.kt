package com.chatapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatapp.data.model.Message
import com.chatapp.data.model.MessageType
import com.chatapp.ui.theme.Black
import com.chatapp.ui.theme.MangaTheme
import com.chatapp.ui.theme.White
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatName: String = "Alice",
    onBack: () -> Unit = {},
    onSend: (String) -> Unit = {}
) {
    // Mock Data for Preview
    val messages = listOf(
        Message(UUID.randomUUID().toString(), "1", "Alice", "Me", "Hey! Look at this manga style.", 0),
        Message(UUID.randomUUID().toString(), "1", "Me", "Alice", "Wow, it's so stark. Black and White only?", 0),
        Message(UUID.randomUUID().toString(), "1", "Alice", "Me", "Yes! Like a panel. Very clean.", 0)
    )

    var inputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = chatName.uppercase(), // All caps for manga header style
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = White,
                    titleContentColor = Black,
                    navigationIconContentColor = Black,
                    actionIconContentColor = Black
                ),
                modifier = Modifier.border(bottom = 2.dp, color = Black) // Thick bottom border
            )
        },
        containerColor = White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(White)
        ) {
            // Message List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                reverseLayout = true // Chat starts from bottom
            ) {
                // Reversed for chat (mock logic needs to be reversed for display if list is "time ascending")
                items(messages.reversed()) { message ->
                    MessageBubble(message = message, isMe = message.senderId == "Me")
                    Spacer(modifier = Modifier.padding(4.dp))
                }
            }

            // Input Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(top = 2.dp, color = Black) // Thick top border for input
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .border(2.dp, Black, RoundedCornerShape(0.dp)), // Sharp corners
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = White,
                        unfocusedContainerColor = White,
                        focusedTextColor = Black,
                        unfocusedTextColor = Black,
                        cursorColor = Black,
                        focusedIndicatorColor = Color.Transparent, // We use our own border
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    placeholder = { Text("Write...", color = Color.Gray) }
                )
                
                Spacer(modifier = Modifier.padding(4.dp))
                
                // Send Button (Action Bubble)
                Box(
                    modifier = Modifier
                        .border(2.dp, Black, RoundedCornerShape(0.dp))
                        .background(Black)
                        .padding(12.dp)
                        .clickable { 
                            if (inputText.isNotBlank()) {
                                onSend(inputText)
                                inputText = ""
                            }
                        }
                ) {
                    Text(
                        text = "SEND",
                        color = White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, isMe: Boolean) {
    val bubbleColor = if (isMe) Black else White
    val textColor = if (isMe) White else Black
    val alignment = if (isMe) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp)
                .border(2.dp, Black, RoundedCornerShape(0.dp)) // Sharp borders always
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal // System font, normal weight for readability
            )
        }
    }
}

// Needed for clickable in Box
fun Modifier.clickable(onClick: () -> Unit): Modifier = androidx.compose.foundation.clickable(onClick = onClick)

@Preview
@Composable
fun ChatScreenPreview() {
    MangaTheme {
        ChatScreen()
    }
}
