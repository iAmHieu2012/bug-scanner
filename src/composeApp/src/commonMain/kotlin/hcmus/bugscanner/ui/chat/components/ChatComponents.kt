package hcmus.bugscanner.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import hcmus.bugscanner.domain.model.ChatMessage
import hcmus.bugscanner.ui.text.MarkdownBlock
import hcmus.bugscanner.ui.text.MarkdownEmphasis
import hcmus.bugscanner.ui.text.MarkdownSpan
import hcmus.bugscanner.ui.text.SimpleMarkdown

/**
 * Component hiển thị một bong bóng tin nhắn (Chat Bubble) đơn lẻ trong giao diện trò chuyện.
 * Thiết kế theo chuẩn UI Chat hiện đại:
 * - Tin nhắn của Bot (AI): Căn trái, màu nền phụ, bo góc vuông ở dưới cùng bên trái.
 * - Tin nhắn của Người dùng: Căn phải, màu nền chính, bo góc vuông ở dưới cùng bên phải.
 * Hỗ trợ hiển thị thêm hình ảnh đính kèm (nếu có) ngay phía trên nội dung văn bản.
 *
 * @param message Đối tượng khối dữ liệu chứa nội dung văn bản, hình ảnh đính kèm (Byte), trạng thái người gửi và trạng thái lỗi.
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

        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (message.imageBytes != null) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    AsyncImage(
                        model = message.imageBytes,
                        contentDescription = "Ảnh đính kèm",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .widthIn(max = 520.dp)
                            .heightIn(max = 260.dp)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }

            if (message.text.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp,
                                bottomStart = if (isUser) 20.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 20.dp
                            )
                        )
                        .background(
                            if (message.isError) MaterialTheme.colorScheme.errorContainer
                            else if (isUser) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    SelectionContainer {
                        if (isUser || message.isError) {
                            Text(
                                text = message.text,
                                color = if (message.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            MarkdownText(
                                text = message.text,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

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

@Composable
private fun MarkdownText(
    text: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SimpleMarkdown.parse(text).forEach { block ->
            Row {
                if (block is MarkdownBlock.Bullet) {
                    Text(
                        text = "•",
                        modifier = Modifier.padding(end = 8.dp, top = 1.dp),
                        color = color,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Text(
                    text = block.spans.toAnnotatedString(),
                    modifier = Modifier.weight(1f, fill = false),
                    color = color,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

private fun List<MarkdownSpan>.toAnnotatedString() = buildAnnotatedString {
    forEach { span ->
        val style = when (span.emphasis) {
            MarkdownEmphasis.NORMAL -> SpanStyle()
            MarkdownEmphasis.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
            MarkdownEmphasis.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
            MarkdownEmphasis.BOLD_ITALIC -> SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
        }
        withStyle(style) {
            append(span.text)
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
