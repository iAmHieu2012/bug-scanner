package hcmus.bugscanner.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hcmus.bugscanner.data.remote.*
import hcmus.bugscanner.domain.model.ChatMessage
import hcmus.bugscanner.domain.model.GeminiContent
import hcmus.bugscanner.domain.model.GeminiPart
import hcmus.bugscanner.domain.model.GeminiRequest
import hcmus.bugscanner.domain.model.Instruction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel quản lý logic luồng tin nhắn và giao tiếp trực tiếp với API Google Gemini.
 * Chịu trách nhiệm duy trì ngữ cảnh trò chuyện (Context History) để AI có thể hiểu các câu hỏi nối tiếp.
 *
 * @param geminiApi Dịch vụ gọi mạng hỗ trợ giao tiếp với Google Gemini được cung cấp bởi DI (Koin).
 */
class ChatViewModel(private val geminiApi: GeminiApiService) : ViewModel() {

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
            ChatMessage("Xin chào! Mình là BugScanner AI. Mình có thể giúp gì cho bạn trong việc tìm hiểu về côn trùng?", isUser = false)
        )
    }

    /**
     * Gửi tin nhắn của người dùng lên API của Gemini và chờ phản hồi.
     * Tự động lưu trữ lịch sử để gửi kèm trong các request tiếp theo.
     *
     * @param text Nội dung tin nhắn người dùng nhập vào.
     */
    fun sendMessage(text: String) {
        if (text.isBlank()) return

        // 1. Thêm tin nhắn của người dùng vào UI và cập nhật trạng thái
        _messages.update { it + ChatMessage(text, isUser = true) }
        _isTyping.value = true

        // Thêm vào ngữ cảnh (Context) để gửi cho AI
        chatHistory.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = text))))

        viewModelScope.launch {
            try {
                // 2. Gói dữ liệu Request kèm theo System Instruction để định khuôn tính cách của Bot
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
                _messages.update { it + ChatMessage(replyText, isUser = false) }

            } catch (e: Exception) {
                // Xử lý lỗi mạng / hết quota API
                _messages.update { it + ChatMessage("Lỗi kết nối: ${e.message}", isUser = false, isError = true) }
            } finally {
                _isTyping.value = false
            }
        }
    }
}