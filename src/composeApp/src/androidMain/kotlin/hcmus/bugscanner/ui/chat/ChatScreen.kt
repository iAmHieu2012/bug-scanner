package hcmus.bugscanner.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import hcmus.bugscanner.ui.components.ChatBubble
import hcmus.bugscanner.ui.components.TypingIndicator

/**
 * Màn hình giao diện nhắn tin với AI Chatbot.
 */
@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    var prompt by remember { mutableStateOf("") }
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()

    val primaryGreen = Color(0xFF2E7D32)
    val lightGreenBg = Color(0xFFF1F8E9)
    val darkGreenText = Color(0xFF1B5E20)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGreenBg)
    ) {
        Surface(
            color = Color.White.copy(alpha = 0.8f),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = primaryGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Trợ lý ảo BugScanner", fontWeight = FontWeight.Bold, color = darkGreenText)
                    Text("Luôn sẵn sàng giải đáp về côn trùng", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            reverseLayout = false,
            contentPadding = PaddingValues(top = 16.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message = message, primaryColor = primaryGreen, darkText = darkGreenText)
            }
            if (isTyping) {
                item {
                    TypingIndicator(darkGreenText)
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 16.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Hỏi BugScanner điều gì đó...") },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryGreen,
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedContainerColor = Color(0xFFF9FBE7),
                        unfocusedContainerColor = Color(0xFFF5F5F5)
                    ),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = {
                        viewModel.sendMessage(prompt)
                        prompt = ""
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .background(if (prompt.isNotBlank() && !isTyping) primaryGreen else Color.LightGray, CircleShape),
                    enabled = prompt.isNotBlank() && !isTyping
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi", tint = Color.White)
                }
            }
        }
    }
}