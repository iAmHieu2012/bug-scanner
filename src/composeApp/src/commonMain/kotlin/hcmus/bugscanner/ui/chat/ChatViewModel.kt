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
 * ViewModel quản lý logic luồng tin nhắn và giao tiếp trực tiếp với API Google Gemini.
 * Chịu trách nhiệm duy trì ngữ cảnh trò chuyện (Context History) để AI có thể hiểu các câu hỏi nối tiếp.
 */
class ChatViewModel : ViewModel() {
    // Khởi tạo Service gọi mạng
    private val geminiApi = GeminiApiService()

    // Trạng thái danh sách tin nhắn hiện tại hiển thị trên UI
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // Trạng thái chờ phản hồi từ AI (dùng để hiển thị Typing Indicator)
    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    // Cấu trúc dữ liệu nội bộ lưu lại lịch sử hội thoại chuẩn form của Gemini API (vai trò: user / model)
    private val chatHistory = mutableListOf<GeminiContent>()

    init {
        // Lời chào mặc định khi khởi tạo màn hình Chat
        _messages.value = listOf(
            ChatMessage("Xin chào! Mình là BugScanner AI \uD83C\uDF43. Mình có thể giúp bạn giải đáp các thắc mắc về côn trùng và thế giới tự nhiên. Bạn muốn hỏi gì nào?", isUser = false)
        )
    }

    /**
     * Gửi một tin nhắn mới từ người dùng đến hệ thống AI và xử lý phản hồi.
     *
     * @param userMessage Nội dung câu hỏi/tin nhắn của người dùng.
     */
    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        // 1. Cập nhật UI ngay lập tức với tin nhắn của user
        val currentList = _messages.value.toMutableList()
        currentList.add(ChatMessage(userMessage, isUser = true))
        _messages.value = currentList

        // 2. Thêm tin nhắn vào lịch sử hội thoại để gửi lên AI kèm theo ngữ cảnh cũ
        chatHistory.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = userMessage))))

        _isTyping.value = true

        viewModelScope.launch {
            try {
                // Xây dựng khối request với System Instruction để ép khuôn tính cách của Bot
                val requestBody = GeminiRequest(
                    systemInstruction = Instruction(parts = GeminiPart("Bạn là BugScanner AI, một trợ lý ảo chuyên nghiệp về sinh học và côn trùng học. Hãy trả lời ngắn gọn, thân thiện và chính xác các câu hỏi về thiên nhiên, côn trùng, thực vật.")),
                    contents = chatHistory
                )

                // Gọi qua tầng Data Service thay vì tự fetch
                val response = geminiApi.generateContent(requestBody)

                val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Xin lỗi, mình không có phản hồi."

                // 3. Lưu phản hồi của AI vào lịch sử để dành cho các câu hỏi tiếp theo
                chatHistory.add(GeminiContent(role = "model", parts = listOf(GeminiPart(text = replyText))))

                // 4. Cập nhật UI với câu trả lời của AI
                val updatedList = _messages.value.toMutableList()
                updatedList.add(ChatMessage(replyText, isUser = false))
                _messages.value = updatedList

            } catch (e: Exception) {
                // Xử lý lỗi mạng / hết quota API
                val updatedList = _messages.value.toMutableList()
                updatedList.add(ChatMessage("Lỗi kết nối: ${e.message}", isUser = false, isError = true))
                _messages.value = updatedList
            } finally {
                _isTyping.value = false
            }
        }
    }
}