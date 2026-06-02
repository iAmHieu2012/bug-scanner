package hcmus.bugscanner.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import hcmus.bugscanner.ui.chat.components.ChatBubble
import hcmus.bugscanner.ui.chat.components.TypingIndicator
import org.koin.compose.viewmodel.koinViewModel

/**
 * Màn hình giao diện nhắn tin với AI Chatbot.
 * Tích hợp Responsive Layout: Giới hạn độ rộng tối đa trên màn hình Web/Desktop để nâng cao trải nghiệm đọc và tương tác.
 *
 * @param initialPrompt Câu hỏi mẫu được truyền vào từ các màn hình khác. Nếu có, AI sẽ tự động xử lý khi mở màn hình.
 * @param viewModel ViewModel quản lý logic gọi API Gemini và duy trì trạng thái lịch sử đoạn chat.
 */
@Composable
fun ChatScreen(
    initialPrompt: String? = null,
    viewModel: ChatViewModel = koinViewModel()
) {
    var prompt by remember { mutableStateOf(initialPrompt ?: "") }
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()

    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrBlank()) {
            prompt = initialPrompt
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 800.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(message = msg)
                }

                if (isTyping) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text("Hỏi BugScanner điều gì đó...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    IconButton(
                        onClick = {
                            val cleanPrompt = prompt.trim()
                            if (cleanPrompt.isNotEmpty()) {
                                viewModel.sendMessage(cleanPrompt)
                                prompt = ""
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                color = if (prompt.isNotBlank() && !isTyping) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = CircleShape
                            ),
                        enabled = prompt.isNotBlank() && !isTyping
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Gửi tin nhắn",
                            tint = if (prompt.isNotBlank() && !isTyping) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}