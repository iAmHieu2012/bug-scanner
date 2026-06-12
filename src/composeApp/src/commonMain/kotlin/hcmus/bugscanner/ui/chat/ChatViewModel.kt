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

    private val greetingMessage = ChatMessage(
        "Xin chào! Mình là BugScanner AI. Mình có thể giúp bạn đọc kết quả nhận diện, tìm hiểu côn trùng và gợi ý cách xử lý an toàn.",
        isUser = false
    )

    private val _messages = MutableStateFlow(listOf(greetingMessage))
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val chatHistory = mutableListOf<GeminiContent>()

    fun clearConversation() {
        chatHistory.clear()
        _messages.value = listOf(greetingMessage)
        _isTyping.value = false
    }

    /**
     * Gửi tin nhắn của người dùng lên API của Gemini và chờ phản hồi.
     * Tự động lưu trữ lịch sử để gửi kèm trong các request tiếp theo.
     *
     * @param text Nội dung tin nhắn người dùng nhập vào.
     */
    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val cleanText = text.trim()
        _messages.update { it + ChatMessage(cleanText, isUser = true) }
        _isTyping.value = true

        chatHistory.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = cleanText))))

        viewModelScope.launch {
            try {
                val requestBody = GeminiRequest(
                    systemInstruction = Instruction(parts = GeminiPart("Bạn là BugScanner AI, một trợ lý ảo chuyên nghiệp về sinh học và côn trùng học. Hãy trả lời ngắn gọn, thân thiện và chính xác các câu hỏi về thiên nhiên, côn trùng, thực vật. Yêu cầu định dạng: Tuyệt đối không sử dụng định dạng Markdown (như in đậm **, tiêu đề #). Nếu cần liệt kê hoặc chia ý, chỉ sử dụng một dấu gạch ngang (-) ở đầu dòng.")),
                    contents = chatHistory
                )

                val response = geminiApi.generateContent(requestBody)
                val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Mình chưa nhận được phản hồi từ AI. Bạn thử hỏi lại ngắn gọn hơn nhé."

                chatHistory.add(GeminiContent(role = "model", parts = listOf(GeminiPart(text = replyText))))
                _messages.update { it + ChatMessage(replyText, isUser = false) }

            } catch (e: Exception) {
                _messages.update {
                    it + ChatMessage(
                        "Mình chưa kết nối được với AI. Vui lòng kiểm tra mạng hoặc API key rồi thử lại.",
                        isUser = false,
                        isError = true
                    )
                }
            } finally {
                _isTyping.value = false
            }
        }
    }
}
