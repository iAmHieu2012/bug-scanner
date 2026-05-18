package hcmus.bugscanner.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hcmus.bugscanner.core.utils.getGeminiApiKey
import hcmus.bugscanner.domain.model.ChatMessage
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// --- Các Data Class để map JSON của Gemini API ---
@Serializable
data class GeminiRequest(val system_instruction: Instruction? = null, val contents: List<GeminiContent>)
@Serializable
data class Instruction(val parts: GeminiPart)
@Serializable
data class GeminiContent(val role: String, val parts: List<GeminiPart>)
@Serializable
data class GeminiPart(val text: String)
@Serializable
data class GeminiResponse(val candidates: List<Candidate>? = null)
@Serializable
data class Candidate(val content: GeminiContent)

/**
 * ViewModel quản lý tin nhắn và gọi API Google Gemini bằng Ktor.
 */
class ChatViewModel : ViewModel() {
    private val apiKey = getGeminiApiKey()
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

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

                val response: GeminiResponse = client.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent") {
                    url { parameters.append("key", apiKey) }
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }.body()

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