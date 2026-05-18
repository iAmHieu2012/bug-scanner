package hcmus.bugscanner.domain.model

/**
 * Model dữ liệu đại diện cho một tin nhắn.
 */
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false
)