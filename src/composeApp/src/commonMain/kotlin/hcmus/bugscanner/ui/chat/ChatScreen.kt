package hcmus.bugscanner.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import hcmus.bugscanner.domain.model.AppConfig
import hcmus.bugscanner.ui.components.ScreenHeader
import kotlinx.coroutines.launch
import hcmus.bugscanner.ui.chat.components.ChatBubble
import hcmus.bugscanner.ui.chat.components.TypingIndicator
import hcmus.bugscanner.ui.scan.LocalPlatformScanProvider
import org.koin.compose.viewmodel.koinViewModel

/**
 * Màn hình giao diện nhắn tin với AI Chatbot.
 * Tích hợp Responsive Layout: Giới hạn độ rộng tối đa trên màn hình Web/Desktop để nâng cao trải nghiệm đọc và tương tác.
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
    var lastAutoSentPrompt by remember { mutableStateOf<String?>(null) }
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val listState = rememberLazyListState()

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

    fun submitPrompt(text: String = prompt) {
        val cleanPrompt = text.trim()
        if ((cleanPrompt.isNotEmpty() || imageToSend != null || urlToSend != null) && !isTyping) {
            viewModel.sendMessage(cleanPrompt, imageToSend, urlToSend)
            prompt = ""
            imageToSend = null
            urlToSend = null
        }
    }

    LaunchedEffect(initialPrompt, initialImageBytes, initialImageUrl) {
        val cleanPrompt = initialPrompt?.trim()
        if ((!cleanPrompt.isNullOrEmpty() && cleanPrompt != lastAutoSentPrompt) || initialImageBytes != null || initialImageUrl != null) {
            lastAutoSentPrompt = cleanPrompt
            viewModel.sendMessage(cleanPrompt.orEmpty(), initialImageBytes, initialImageUrl)
        }
    }

    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            val targetIndex = if (isTyping) messages.size else messages.lastIndex
            listState.animateScrollToItem(targetIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScreenHeader(
                title = "Trợ lý BugScanner",
                subtitle = "Hỏi về nhận diện, đặc điểm và cách xử lý côn trùng.",
                leadingIcon = Icons.Rounded.SmartToy,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp)
            ) {
                IconButton(
                    onClick = { viewModel.clearConversation() },
                    enabled = messages.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = "Xóa cuộc trò chuyện",
                        tint = if (messages.isNotEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 900.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(message = msg)
                }

                if (messages.size <= 1 && !isTyping) {
                    item {
                        PromptSuggestions(onPromptClick = ::submitPrompt)
                    }
                }

                if (isTyping) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            ChatInput(
                prompt = prompt,
                isTyping = isTyping,
                imageBytes = imageToSend,
                imageUrl = urlToSend,
                onPromptChange = { prompt = it },
                onPickImageClick = { imagePicker.launchGallery() },
                onRemoveImageClick = {
                    imageToSend = null
                    urlToSend = null
                },
                onSubmit = ::submitPrompt
            )
        }
    }
}
}



/**
 * Hiển thị danh sách các gợi ý câu hỏi nhanh dưới dạng chip để người dùng lựa chọn.
 *
 * @param onPromptClick Callback kích hoạt khi người dùng nhấn chọn một câu gợi ý.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PromptSuggestions(onPromptClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 12.dp)
    ) {
        Text(
            text = "Gợi ý nhanh",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChatPromptSuggestions.defaultPrompts.forEach { suggestion ->
                SuggestionChip(
                    onClick = { onPromptClick(suggestion) },
                    label = { Text(suggestion) },
                    shape = RoundedCornerShape(18.dp)
                )
            }
        }
    }
}

/**
 * Thanh công cụ nhập liệu tin nhắn trò chuyện của người dùng.
 *
 * @param prompt Nội dung văn bản hiện tại trong ô nhập liệu.
 * @param isTyping Trạng thái AI đang phản hồi (để vô hiệu hóa nút gửi).
 * @param imageBytes Hình ảnh dạng mảng bytes đang được đính kèm.
 * @param imageUrl Link ảnh dạng URL đang được đính kèm.
 * @param onPromptChange Callback khi nội dung trong ô nhập liệu thay đổi.
 * @param onPickImageClick Callback khi người dùng nhấn chọn hình ảnh từ thư viện.
 * @param onRemoveImageClick Callback khi người dùng xóa hình ảnh đã chọn.
 * @param onSubmit Callback khi gửi tin nhắn đi.
 */
@Composable
private fun ChatInput(
    prompt: String,
    isTyping: Boolean,
    imageBytes: ByteArray?,
    imageUrl: String?,
    onPromptChange: (String) -> Unit,
    onPickImageClick: () -> Unit,
    onRemoveImageClick: () -> Unit,
    onSubmit: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
        ) {
            if (imageBytes != null || imageUrl != null) {
                Box(
                    modifier = Modifier.padding(top = 4.dp, end = 12.dp, bottom = 8.dp, start = 8.dp)
                ) {
                    AsyncImage(
                        model = imageBytes ?: imageUrl,
                        contentDescription = "Ảnh chuẩn bị gửi",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .sizeIn(maxWidth = 120.dp, maxHeight = 120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    )

                    IconButton(
                        onClick = onRemoveImageClick,
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
                    onClick = onPickImageClick,
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
                    onValueChange = onPromptChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Hỏi BugScanner điều gì đó...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(12.dp))

                val canSend = (prompt.isNotBlank() || imageBytes != null || imageUrl != null) && !isTyping

                IconButton(
                    onClick = onSubmit,
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            color = if (canSend) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            shape = CircleShape
                        ),
                    enabled = canSend
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Gửi tin nhắn",
                        tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
