package hcmus.bugscanner.ui.chat.components

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
 * Component hiển thị một bong bóng tin nhắn (Chat Bubble) đơn lẻ trong giao diện trò chuyện.
 * Thiết kế theo chuẩn UI Chat hiện đại:
 * - Tin nhắn của Bot (AI): Căn trái, màu nền phụ, bo góc vuông ở dưới cùng bên trái.
 * - Tin nhắn của Người dùng: Căn phải, màu nền chính, bo góc vuông ở dưới cùng bên phải.
 *
 * @param message Đối tượng khối dữ liệu chứa nội dung văn bản, trạng thái người gửi và trạng thái lỗi.
 */
@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 8.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        // Avatar AI (Hiển thị bên trái)
        if (!isUser) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(32.dp).align(Alignment.Bottom)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SmartToy,
                    contentDescription = "AI",
                    modifier = Modifier.padding(6.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Bong bóng tin nhắn
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 4.dp, // Vuông góc bot
                        bottomEnd = if (isUser) 4.dp else 20.dp    // Vuông góc user
                    )
                )
                .background(
                    if (message.isError) MaterialTheme.colorScheme.errorContainer
                    else if (isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = message.text,
                color = if (message.isError) MaterialTheme.colorScheme.onErrorContainer
                else if (isUser) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // Avatar User (Hiển thị bên phải)
        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp).align(Alignment.Bottom)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = "User",
                    modifier = Modifier.padding(6.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Hiệu ứng hiển thị trạng thái đang chờ AI xử lý (Typing Indicator).
 * Nên được đưa vào cây UI (Composition) khi người dùng vừa gửi tin nhắn và đang chờ luồng phản hồi từ API.
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