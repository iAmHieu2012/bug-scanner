package hcmus.bugscanner.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hcmus.bugscanner.data.remote.*
import hcmus.bugscanner.domain.model.ChatMessage
import hcmus.bugscanner.domain.model.GeminiContent
import hcmus.bugscanner.domain.model.GeminiInlineData
import hcmus.bugscanner.domain.model.GeminiPart
import hcmus.bugscanner.domain.model.GeminiRequest
import hcmus.bugscanner.domain.model.Instruction
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * ViewModel quản lý logic luồng tin nhắn và giao tiếp trực tiếp với API Google Gemini.
 * Chịu trách nhiệm duy trì ngữ cảnh trò chuyện (Context History) để AI có thể hiểu các câu hỏi nối tiếp.
 * Tự động chuyển đổi URL hình ảnh thành mảng Byte (Base64) để xử lý đa phương thức đồng nhất, giữ cho Model luôn sạch.
 *
 * @property messages Trạng thái danh sách tin nhắn hiện tại hiển thị trên UI.
 * @property isTyping Trạng thái chờ phản hồi từ AI (dùng để hiển thị Typing Indicator).
 * @param geminiApi Dịch vụ gọi mạng hỗ trợ giao tiếp với Google Gemini được cung cấp bởi DI (Koin).
 */
@OptIn(ExperimentalEncodingApi::class)
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
     * Gửi tin nhắn và/hoặc hình ảnh của người dùng lên API của Gemini và chờ phản hồi.
     * Tự động tải ảnh từ URL về mảng byte nếu dữ liệu đầu vào là đường dẫn mạng.
     *
     * @param text Nội dung tin nhắn người dùng nhập vào.
     * @param imageBytes Dữ liệu mảng byte của hình ảnh đính kèm (nếu upload từ máy).
     * @param imageUrl Đường dẫn URL của hình ảnh đính kèm (nếu chọn từ Lịch sử/Wiki).
     */
    fun sendMessage(text: String, imageBytes: ByteArray? = null, imageUrl: String? = null) {
        if (text.isBlank() && imageBytes == null && imageUrl == null) return

        _isTyping.value = true

        viewModelScope.launch {
            try {
                var finalBytes = imageBytes

                if (finalBytes == null && imageUrl != null) {
                    val client = HttpClient()
                    finalBytes = client.get(imageUrl).readRawBytes()
                    client.close()
                }

                _messages.update { it + ChatMessage(text = text, isUser = true, imageBytes = finalBytes) }

                val userParts = mutableListOf<GeminiPart>()

                if (text.isNotBlank()) {
                    userParts.add(GeminiPart(text = text))
                }

                if (finalBytes != null) {
                    val base64String = Base64.encode(finalBytes)
                    userParts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64String)))
                }

                chatHistory.add(GeminiContent(role = "user", parts = userParts))

                val requestBody = GeminiRequest(
                    systemInstruction = Instruction(parts = GeminiPart(text = "Bạn là BugScanner AI, một trợ lý ảo chuyên nghiệp về sinh học và côn trùng học. Hãy trả lời ngắn gọn, thân thiện và chính xác các câu hỏi về thiên nhiên, côn trùng, thực vật. Khi nhận được ảnh, hãy phân tích kỹ các đặc điểm sinh học trên ảnh để tư vấn. Yêu cầu định dạng: Tuyệt đối không sử dụng định dạng Markdown (như in đậm **, tiêu đề #). Nếu cần liệt kê hoặc chia ý, chỉ sử dụng một dấu gạch ngang (-) ở đầu dòng.")),
                    contents = chatHistory
                )

                val response = geminiApi.generateContent(requestBody)
                val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Xin lỗi, mình không có phản hồi."

                chatHistory.add(GeminiContent(role = "model", parts = listOf(GeminiPart(text = replyText))))
                _messages.update { it + ChatMessage(text = replyText, isUser = false) }

            } catch (e: Exception) {
                _messages.update { it + ChatMessage(text = "Lỗi kết nối hoặc không tải được ảnh: ${e.message}", isUser = false, isError = true) }
            } finally {
                _isTyping.value = false
            }
        }
    }
}