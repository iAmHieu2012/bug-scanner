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
import androidx.compose.ui.unit.dp
import hcmus.bugscanner.domain.model.ChatMessage

/**
 * Bong bóng hiển thị nội dung tin nhắn của người dùng hoặc chatbot.
 * Thiết kế theo chuẩn giao diện chat hiện đại:
 * - Tin nhắn AI: Neo bên trái, đuôi nhọn ở góc dưới trái.
 * - Tin nhắn User: Neo bên phải, đuôi nhọn ở góc dưới phải.
 *
 * @param message Đối tượng chứa nội dung, cờ định danh (AI/User) và trạng thái lỗi.
 */
@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 8.dp), // Thêm padding ngang để không bị sát lề
        // Đẩy bong bóng về đúng phía người gửi
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // --- Avatar của Chatbot (Bên trái) ---
        if (!isUser) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SmartToy,
                    contentDescription = "Bot Avatar",
                    modifier = Modifier.padding(6.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // --- Khung chứa nội dung tin nhắn ---
        Box(
            modifier = Modifier
                // Cho phép bong bóng phình to tối đa 75% chiều rộng màn hình khả dụng,
                // giải quyết vấn đề bong bóng bị quá nhỏ trên Desktop/Web
                .fillMaxWidth(0.75f)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        // Xử lý "đuôi" bong bóng tùy theo người gửi
                        bottomStart = if (isUser) 20.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 20.dp
                    )
                )
                .background(
                    if (isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = message.text,
                color = if (isUser) {
                    MaterialTheme.colorScheme.onPrimary
                } else if (message.isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // --- Avatar của Người dùng (Bên phải) ---
        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = "User Avatar",
                    modifier = Modifier.padding(6.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Hiệu ứng hiển thị trạng thái đang chờ AI phản hồi (Typing Indicator).
 * Được gọi khi đã gửi tin nhắn nhưng chưa nhận được response từ API Gemini.
 */
@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SmartToy,
                contentDescription = "Bot Avatar",
                modifier = Modifier.padding(6.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "BugScanner đang suy nghĩ...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}