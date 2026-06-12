package hcmus.bugscanner.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import hcmus.bugscanner.ui.chat.components.ChatBubble
import hcmus.bugscanner.ui.chat.components.TypingIndicator
import hcmus.bugscanner.ui.scan.LocalPlatformScanProvider
import org.koin.compose.viewmodel.koinViewModel

/**
 * Màn hình giao diện nhắn tin với AI Chatbot.
 * Tích hợp Responsive Layout: Giới hạn độ rộng tối đa trên màn hình Web/Desktop để nâng cao trải nghiệm.
 * Cung cấp nút tải ảnh trực tiếp và hiển thị trước hình ảnh đính kèm (cả dạng Byte lẫn URL) theo tỷ lệ chuẩn.
 *
 * @param initialPrompt Câu hỏi mẫu truyền vào từ màn hình khác (tuỳ chọn).
 * @param initialImageBytes Hình ảnh đính kèm dạng mảng Byte truyền vào từ màn hình Scan (tuỳ chọn).
 * @param initialImageUrl Hình ảnh đính kèm dạng URL truyền vào từ màn hình Lịch sử / Bách khoa (tuỳ chọn).
 * @param viewModel ViewModel quản lý logic gọi API Gemini và duy trì trạng thái lịch sử đoạn chat.
 */
@Composable
fun ChatScreen(
    initialPrompt: String? = null,
    initialImageBytes: ByteArray? = null,
    initialImageUrl: String? = null,
    viewModel: ChatViewModel = koinViewModel()
) {
    var prompt by remember { mutableStateOf("") }
    var imageToSend by remember { mutableStateOf<ByteArray?>(null) }
    var urlToSend by remember { mutableStateOf<String?>(null) }

    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()

    val scanProvider = LocalPlatformScanProvider.current
    val imagePicker = scanProvider.rememberImagePickerHelper(
        onModeChange = {},
        onResult = {},
        onImageIdCaptured = {},
        onImageBytesCaptured = { bytes ->
            if (bytes != null) {
                imageToSend = bytes
                urlToSend = null
            }
        }
    )

    LaunchedEffect(initialPrompt, initialImageBytes, initialImageUrl) {
        if (!initialPrompt.isNullOrBlank()) prompt = initialPrompt
        if (initialImageBytes != null) imageToSend = initialImageBytes
        if (!initialImageUrl.isNullOrBlank()) urlToSend = initialImageUrl
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                ) {
                    if (imageToSend != null || urlToSend != null) {
                        Box(
                            modifier = Modifier.padding(top = 12.dp, end = 12.dp, bottom = 8.dp, start = 8.dp)
                        ) {
                            AsyncImage(
                                model = imageToSend ?: urlToSend,
                                contentDescription = "Ảnh chuẩn bị gửi",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .sizeIn(maxWidth = 120.dp, maxHeight = 120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            )

                            IconButton(
                                onClick = {
                                    imageToSend = null
                                    urlToSend = null
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-6).dp)
                                    .size(20.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Xóa ảnh",
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        IconButton(
                            onClick = { imagePicker.launchGallery() },
                            modifier = Modifier
                                .padding(end = 8.dp, bottom = 4.dp)
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Image,
                                contentDescription = "Chọn ảnh từ thư viện",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedTextField(
                            value = prompt,
                            onValueChange = { prompt = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text("Hỏi BugScanner...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
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

                        val canSend = (prompt.isNotBlank() || imageToSend != null || urlToSend != null) && !isTyping

                        IconButton(
                            onClick = {
                                val cleanPrompt = prompt.trim()
                                if (cleanPrompt.isNotEmpty() || imageToSend != null || urlToSend != null) {
                                    viewModel.sendMessage(text = cleanPrompt, imageBytes = imageToSend, imageUrl = urlToSend)
                                    prompt = ""
                                    imageToSend = null
                                    urlToSend = null
                                }
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    color = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = CircleShape
                                ),
                            enabled = canSend
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Gửi",
                                tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}