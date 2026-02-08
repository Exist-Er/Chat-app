package com.chatapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatapp.data.model.Message
import com.chatapp.ui.theme.Black
import com.chatapp.ui.theme.MangaTheme
import com.chatapp.ui.theme.White
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<Message>,
    chatName: String = "Alice",
    currentUserId: String = "Me",
    onBack: () -> Unit = {},
    onSend: (String) -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.ArrowBack, // Changed to filled (auto-mirrored not available in Bom 2023.08)
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Text(
                        text = chatName.uppercase(),
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
                modifier = Modifier.drawBehind {
                    val strokeWidth = 2.dp.toPx()
                    drawLine(
                        color = Black,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = strokeWidth
                    )
                }
            )
        },
        containerColor = White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Ambiguity fix - pass paddingValues directly
                .background(White)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                reverseLayout = true
            ) {
                    items(messages.reversed()) { message ->
                        MessageBubble(message = message, isMe = message.senderId == currentUserId)
                        Spacer(modifier = Modifier.padding(4.dp))
                    }
            }

            // Bubbly Input Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pill-shaped Input
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .border(2.dp, Black, RoundedCornerShape(24.dp)) // Fully rounded border
                        .clip(RoundedCornerShape(24.dp)), // Clip content to pill shape
                    shape = RoundedCornerShape(24.dp), // Input field shape
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = White,
                        unfocusedContainerColor = White,
                        focusedTextColor = Black,
                        unfocusedTextColor = Black,
                        cursorColor = Black,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    placeholder = { Text("Write...", color = Color.Gray) },
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.padding(4.dp))
                
                // Rounded Send Button
                Box(
                    modifier = Modifier
                        .border(2.dp, Black, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Black)
                        .clickable { 
                            if (inputText.isNotBlank()) {
                                onSend(inputText)
                                inputText = ""
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
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
    
    // Bubbly shapes: 
    // If Me: TopLeft, TopRight, BottomLeft rounded. BottomRight sharp (or slightly less rounded).
    // Let's go with "Speech Bubble" style.
    val bubbleShape = if (isMe) {
        RoundedCornerShape(18.dp, 18.dp, 2.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 2.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp)
                .border(2.dp, Black, bubbleShape)
                .clip(bubbleShape)
                .background(bubbleColor)
                .padding(top = 10.dp, bottom = 10.dp, start = 14.dp, end = 14.dp)
        ) {
            Text(
                text = message.content,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Preview
@Composable
fun ChatScreenPreview() {
    val mockMessages = listOf(
        Message(UUID.randomUUID().toString(), "1", "Alice", "Me", "Hey! Does this look more bubbly?", 0),
        Message(UUID.randomUUID().toString(), "1", "Me", "Alice", "Yes! It still keeps the manga vibe though.", 0)
    )
    MangaTheme {
        ChatScreen(messages = mockMessages, currentUserId = "Me")
    }
}
