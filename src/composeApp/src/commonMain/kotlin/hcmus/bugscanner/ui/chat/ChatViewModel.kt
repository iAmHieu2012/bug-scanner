package hcmus.bugscanner.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hcmus.bugscanner.data.remote.*
import hcmus.bugscanner.domain.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel quản lý tin nhắn và gọi API Google Gemini thông qua GeminiApiService.
 */
class ChatViewModel : ViewModel() {
    // Khởi tạo Service thay vì tự gọi Ktor
    private val geminiApi = GeminiApiService()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    // Lưu lại lịch sử hội thoại để AI hiểu ngữ cảnh (vai trò: user / model)
    private val chatHistory = mutableListOf<GeminiContent>()

    init {
        _messages.value = listOf(
            ChatMessage("Xin chào! Mình là BugScanner AI \uD83C\uDF43. Mình có thể giúp bạn giải đáp các thắc mắc về côn trùng và thế giới tự nhiên. Bạn muốn hỏi gì nào?", isUser = false)
        )
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        // 1. Cập nhật UI tin nhắn của user
        val currentList = _messages.value.toMutableList()
        currentList.add(ChatMessage(userMessage, isUser = true))
        _messages.value = currentList

        // 2. Thêm vào lịch sử hội thoại gửi lên AI
        chatHistory.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = userMessage))))

        _isTyping.value = true

        viewModelScope.launch {
            try {
                val requestBody = GeminiRequest(
                    system_instruction = Instruction(parts = GeminiPart("Bạn là BugScanner AI, một trợ lý ảo chuyên nghiệp về sinh học và côn trùng học. Hãy trả lời ngắn gọn, thân thiện và chính xác các câu hỏi về thiên nhiên, côn trùng, thực vật.")),
                    contents = chatHistory
                )

                // Gọi qua tầng Data Service thay vì tự fetch
                val response = geminiApi.generateContent(requestBody)

                val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Xin lỗi, mình không có phản hồi."

                // 3. Lưu phản hồi của AI vào lịch sử
                chatHistory.add(GeminiContent(role = "model", parts = listOf(GeminiPart(text = replyText))))

                // 4. Cập nhật UI
                val updatedList = _messages.value.toMutableList()
                updatedList.add(ChatMessage(replyText, isUser = false))
                _messages.value = updatedList

            } catch (e: Exception) {
                val updatedList = _messages.value.toMutableList()
                updatedList.add(ChatMessage("Lỗi kết nối: ${e.message}", isUser = false, isError = true))
                _messages.value = updatedList
            } finally {
                _isTyping.value = false
            }
        }
    }
}