package hcmus.bugscanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import hcmus.bugscanner.ui.chat.ChatMessage

/**
 * Bong bóng hiển thị nội dung tin nhắn của người dùng hoặc chatbot.
 */
@Composable
fun ChatBubble(message: ChatMessage, primaryColor: Color, darkText: Color) {
    val isUser = message.isUser
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFC8E6C9),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Rounded.SmartToy, contentDescription = null, modifier = Modifier.padding(6.dp), tint = darkText)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth(0.85f)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 20.dp
                    )
                )
                .background(
                    when {
                        message.isError -> Color(0xFFFFEBEE)
                        isUser -> primaryColor
                        else -> Color.White
                    }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = message.text,
                color = if (isUser) Color.White else if (message.isError) Color.Red else darkText,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = primaryColor.copy(alpha = 0.1f),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.padding(6.dp), tint = primaryColor)
            }
        }
    }
}

/**
 * Hiệu ứng hiển thị trạng thái đang chờ AI phản hồi.
 */
@Composable
fun TypingIndicator(darkText: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFFC8E6C9),
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Rounded.SmartToy, contentDescription = null, modifier = Modifier.padding(6.dp), tint = darkText)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp))
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text("BugScanner đang suy nghĩ...", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        }
    }
}