package hcmus.bugscanner.domain.model

/**
 * Lớp dữ liệu đại diện cho một tin nhắn đơn lẻ trong giao diện Chatbot.
 * Hỗ trợ lưu trữ đa phương thức (bao gồm cả văn bản và hình ảnh đính kèm).
 *
 * @property text Nội dung văn bản của tin nhắn (những gì người dùng nhập hoặc AI trả lời).
 * @property isUser Cờ xác định người gửi. Nếu `true`, đây là tin nhắn của người dùng. Nếu `false`, đây là phản hồi của AI.
 * @property isError Cờ đánh dấu tin nhắn này có phải là thông báo lỗi hệ thống/mạng hay không (dùng để đổi màu chữ thành đỏ trên UI).
 * @property imageBytes Dữ liệu mảng byte của bức ảnh được đính kèm vào tin nhắn. Có giá trị `null` nếu tin nhắn chỉ có văn bản.
 */
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false,
    val imageBytes: ByteArray? = null
)