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
 * Tích hợp Responsive Layout: Giới hạn độ rộng tối đa trên màn hình Web/Desktop
 * (giống ChatGPT/Gemini) để nâng cao trải nghiệm đọc và tương tác.
 *
 * @param initialPrompt Câu hỏi mẫu được truyền vào từ các màn hình khác (ví dụ: từ BugDetailScreen). Nếu có, AI sẽ tự động xử lý khi mở màn hình.
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

    // Tự động nạp prompt ban đầu (nếu có) khi màn hình vừa mở
    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrBlank()) {
            prompt = initialPrompt
        }
    }

    // Box ngoài cùng: Phủ full màn hình màu nền, và CĂN GIỮA nội dung.
    // Việc căn giữa này cực kỳ quan trọng cho giao diện Web/Desktop.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        // Cột trung tâm: Chứa danh sách tin nhắn và thanh nhập liệu.
        // Bị khóa chiều rộng tối đa (800dp) để không bị kéo giãn quá mức trên màn hình to.
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 800.dp) // <--- Điểm mấu chốt của Responsive Chat Layout
        ) {

            // --- VÙNG 1: Danh sách hiển thị lịch sử trò chuyện ---
            LazyColumn(
                modifier = Modifier
                    .weight(1f) // Đẩy thanh nhập liệu xuống đáy, chiếm phần còn lại
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Render từng bong bóng chat
                items(messages) { msg ->
                    ChatBubble(message = msg)
                }

                // Hiển thị hiệu ứng "Đang gõ..." nếu AI đang xử lý
                if (isTyping) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            // --- VÙNG 2: Thanh nhập liệu (Input Bar) ---
            Surface(
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp), // Thêm padding bottom cho cân đối
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
                            // Ẩn hoàn toàn viền để tạo cảm giác UI hiện đại, nguyên khối
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            // Đổ màu nền nhạt tạo hiệu ứng hình viên thuốc (pill shape)
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        ),
                        maxLines = 4 // Hỗ trợ gõ nhiều dòng nhưng không che lấp hết màn hình
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Nút Gửi (Send Button)
                    IconButton(
                        onClick = {
                            // Trim khoảng trắng thừa trước khi gửi để tránh lỗi API
                            val cleanPrompt = prompt.trim()
                            if (cleanPrompt.isNotEmpty()) {
                                viewModel.sendMessage(cleanPrompt)
                                prompt = ""
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                // Nút chỉ sáng lên màu Primary khi có chữ và AI không bận
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