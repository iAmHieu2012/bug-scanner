package hcmus.bugscanner.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Model dữ liệu đại diện cho một tin nhắn.
 */
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false
)

/**
 * ViewModel quản lý trạng thái tin nhắn và giao tiếp với Google Gemini AI.
 */
class ChatViewModel : ViewModel() {

    private val apiKey = hcmus.bugscanner.BuildConfig.GEMINI_API_KEY

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey,
        systemInstruction = content {
            text("Bạn là BugScanner AI, một trợ lý ảo chuyên nghiệp về sinh học và côn trùng học. Hãy trả lời ngắn gọn, thân thiện và chính xác các câu hỏi về thiên nhiên, côn trùng, thực vật.")
        }
    )

    private val chat = generativeModel.startChat()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    init {
        _messages.value = listOf(
            ChatMessage("Xin chào! Mình là BugScanner AI \uD83C\uDF43. Mình có thể giúp bạn giải đáp các thắc mắc về côn trùng và thế giới tự nhiên. Bạn muốn hỏi gì nào?", isUser = false)
        )
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        val currentList = _messages.value.toMutableList()
        currentList.add(ChatMessage(userMessage, isUser = true))
        _messages.value = currentList

        _isTyping.value = true

        viewModelScope.launch {
            try {
                val response = chat.sendMessage(userMessage)
                response.text?.let { reply ->
                    val updatedList = _messages.value.toMutableList()
                    updatedList.add(ChatMessage(reply, isUser = false))
                    _messages.value = updatedList
                }
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