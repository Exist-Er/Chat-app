package com.chatapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatapp.ui.theme.Black
import com.chatapp.ui.theme.MangaTheme
import com.chatapp.ui.theme.White

@Composable
fun LoginScreen(
    title: String = "WHO ARE YOU?",
    placeholder: String = "Enter your name...",
    buttonText: String = "ENTER CHAT",
    onLogin: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Black,
            fontSize = 32.sp,
            color = Black,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // Bubbly Input
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Black, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = White,
                unfocusedContainerColor = White,
                focusedTextColor = Black,
                unfocusedTextColor = Black,
                cursorColor = Black,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            placeholder = { Text(placeholder, color = Color.Gray) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = {
                if (text.isNotBlank()) onLogin(text)
            })
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Bubbly Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Black, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Black)
                .clickable {
                    if (text.isNotBlank()) {
                        onLogin(text)
                    }
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buttonText,
                color = White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

@Preview
@Composable
fun LoginScreenPreview() {
    MangaTheme {
        LoginScreen(onLogin = {})
    }
}
