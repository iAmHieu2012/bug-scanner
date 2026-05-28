package hcmus.bugscanner.domain.model

/**
 * Lớp dữ liệu đại diện cho một tin nhắn đơn lẻ trong giao diện Chatbot.
 *
 * @property text Nội dung văn bản của tin nhắn.
 * @property isUser Cờ xác định người gửi. Nếu `true`, đây là tin nhắn của người dùng. Nếu `false`, đây là phản hồi của AI.
 * @property isError Cờ đánh dấu tin nhắn này có phải là thông báo lỗi hệ thống/mạng hay không.
 */
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false
)