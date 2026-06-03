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
 * @property messages Trạng thái danh sách tin nhắn hiện tại hiển thị trên UI.
 * @property isTyping Trạng thái chờ phản hồi từ AI (dùng để hiển thị Typing Indicator).
 * @param geminiApi Dịch vụ gọi mạng hỗ trợ giao tiếp với Google Gemini được cung cấp bởi DI (Koin).
 */
class ChatViewModel(private val geminiApi: GeminiApiService) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val chatHistory = mutableListOf<GeminiContent>()

    init {
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

        _messages.update { it + ChatMessage(text, isUser = true) }
        _isTyping.value = true

        chatHistory.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = text))))

        viewModelScope.launch {
            try {
                val requestBody = GeminiRequest(
                    systemInstruction = Instruction(parts = GeminiPart("Bạn là BugScanner AI, một trợ lý ảo chuyên nghiệp về sinh học và côn trùng học. Hãy trả lời ngắn gọn, thân thiện và chính xác các câu hỏi về thiên nhiên, côn trùng, thực vật. Yêu cầu định dạng: Tuyệt đối không sử dụng định dạng Markdown (như in đậm **, tiêu đề #). Nếu cần liệt kê hoặc chia ý, chỉ sử dụng một dấu gạch ngang (-) ở đầu dòng.")),
                    contents = chatHistory
                )

                val response = geminiApi.generateContent(requestBody)
                val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Xin lỗi, mình không có phản hồi."

                chatHistory.add(GeminiContent(role = "model", parts = listOf(GeminiPart(text = replyText))))
                _messages.update { it + ChatMessage(replyText, isUser = false) }

            } catch (e: Exception) {
                _messages.update { it + ChatMessage("Lỗi kết nối: ${e.message}", isUser = false, isError = true) }
            } finally {
                _isTyping.value = false
            }
        }
    }
}