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

import hcmus.bugscanner.core.config.AppConfigProvider
import dev.gitlive.firebase.auth.auth

/**
 * ViewModel quản lý logic luồng tin nhắn và giao tiếp trực tiếp với API Google Gemini.
 * Chịu trách nhiệm duy trì ngữ cảnh trò chuyện (Context History) để AI có thể hiểu các câu hỏi nối tiếp.
 * Tự động chuyển đổi URL hình ảnh thành mảng Byte (Base64) để xử lý đa phương thức đồng nhất, giữ cho Model luôn sạch.
 *
 * @property messages Trạng thái danh sách tin nhắn hiện tại hiển thị trên UI.
 * @property isTyping Trạng thái chờ phản hồi từ AI (dùng để hiển thị Typing Indicator).
 * @param geminiApi Dịch vụ gọi mạng hỗ trợ giao tiếp với Google Gemini được cung cấp bởi DI (Koin).
 * @param httpClient Ktor Client được inject từ Koin để tải dữ liệu hình ảnh.
 * @param appConfigProvider Cung cấp cấu hình prompt động từ Firestore.
 */
@OptIn(ExperimentalEncodingApi::class)
class ChatViewModel(
    private val geminiApi: GeminiApiService,
    private val httpClient: HttpClient,
    private val appConfigProvider: AppConfigProvider
) : ViewModel() {

    private val greetingMessage = ChatMessage(
        "Xin chào! Mình là BugScanner AI. Mình có thể giúp bạn đọc kết quả nhận diện, tìm hiểu côn trùng và gợi ý cách xử lý an toàn.",
        isUser = false
    )

    init {
        viewModelScope.launch {
            try {
                dev.gitlive.firebase.Firebase.auth.authStateChanged.collect { user ->
                    if (user == null) {
                        clearConversation()
                    }
                }
            } catch (e: Exception) {
                // Ignore if auth is not available
            }
        }
    }

    private val _messages = MutableStateFlow(listOf(greetingMessage))
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val chatHistory = mutableListOf<GeminiContent>()

    /**
     * Xóa sạch lịch sử cuộc trò chuyện hiện tại và thiết lập lại tin nhắn chào mừng.
     */
    fun clearConversation() {
        chatHistory.clear()
        _messages.value = listOf(greetingMessage)
        _isTyping.value = false
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

        val cleanText = text.trim()
        val initialMessage = ChatMessage(cleanText, isUser = true, imageBytes = imageBytes)
        _messages.update { it + initialMessage }
        _isTyping.value = true

        viewModelScope.launch {
            try {
                var finalBytes = imageBytes

                if (finalBytes == null && !imageUrl.isNullOrBlank()) {
                    finalBytes = httpClient.get(imageUrl).readRawBytes()
                    _messages.update { list ->
                        list.map { msg ->
                            if (msg === initialMessage) {
                                msg.copy(imageBytes = finalBytes)
                            } else {
                                msg
                            }
                        }
                    }
                }

                val userParts = mutableListOf<GeminiPart>()

                if (cleanText.isNotBlank()) {
                    userParts.add(GeminiPart(text = cleanText))
                }

                if (finalBytes != null) {
                    val isPng = finalBytes.size > 3 && finalBytes[0] == 0x89.toByte() && finalBytes[1] == 0x50.toByte()
                    val mimeType = if (isPng) "image/png" else "image/jpeg"

                    val base64String = Base64.encode(finalBytes)
                    userParts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = mimeType, data = base64String)))
                }

                chatHistory.add(GeminiContent(role = "user", parts = userParts))

                val config = appConfigProvider.getConfig()

                val requestBody = GeminiRequest(
                    systemInstruction = Instruction(parts = GeminiPart(text = config.geminiSystemPrompt)),
                    contents = chatHistory
                )

                val response = geminiApi.generateContent(requestBody)
                val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Mình chưa nhận được phản hồi từ AI. Bạn thử hỏi lại ngắn gọn hơn nhé."

                chatHistory.add(GeminiContent(role = "model", parts = listOf(GeminiPart(text = replyText))))
                _messages.update { it + ChatMessage(text = replyText, isUser = false) }

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
