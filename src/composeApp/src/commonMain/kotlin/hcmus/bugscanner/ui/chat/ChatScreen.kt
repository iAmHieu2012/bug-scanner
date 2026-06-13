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
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hcmus.bugscanner.ui.chat.components.ChatBubble
import hcmus.bugscanner.ui.chat.components.TypingIndicator
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
    var lastAutoSentPrompt by remember { mutableStateOf<String?>(null) }
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val listState = rememberLazyListState()

    fun submitPrompt(text: String = prompt) {
        val cleanPrompt = text.trim()
        if (cleanPrompt.isNotEmpty() && !isTyping) {
            viewModel.sendMessage(cleanPrompt)
            prompt = ""
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 900.dp)
        ) {
            ChatHeader(
                canClear = messages.size > 1 || isTyping,
                onClearClick = { viewModel.clearConversation() }
            )

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
                onPromptChange = { prompt = it },
                onSubmit = ::submitPrompt
            )
        }
    }
}

/**
 * Component hiển thị phần đầu (header) của màn hình Chat.
 *
 * @param canClear Trạng thái cho biết cuộc trò chuyện có thể xóa được hay không.
 * @param onClearClick Callback kích hoạt khi người dùng nhấn nút xóa cuộc trò chuyện.
 */
@Composable
private fun ChatHeader(
    canClear: Boolean,
    onClearClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SmartToy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Trợ lý BugScanner",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Hỏi về nhận diện, đặc điểm và cách xử lý côn trùng.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onClearClick, enabled = canClear) {
            Icon(
                imageVector = Icons.Rounded.DeleteSweep,
                contentDescription = "Xóa cuộc trò chuyện",
                tint = if (canClear) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outlineVariant
            )
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
 * @param onPromptChange Callback khi nội dung trong ô nhập liệu thay đổi.
 * @param onSubmit Callback khi gửi tin nhắn đi.
 */
@Composable
private fun ChatInput(
    prompt: String,
    isTyping: Boolean,
    onPromptChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
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

            IconButton(
                onClick = onSubmit,
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = if (prompt.isNotBlank() && !isTyping) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
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
