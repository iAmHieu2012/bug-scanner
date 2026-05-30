package hcmus.bugscanner.domain.model

import kotlinx.serialization.Serializable

/**
 * DTO đại diện cho yêu cầu (Request) gửi lên API của Google Gemini.
 *
 * @property systemInstruction Lời nhắc hệ thống (System Prompt) để định hình tính cách/hành vi của AI (tuỳ chọn).
 * @property contents Danh sách các đoạn hội thoại hoặc nội dung (ảnh, text) cần gửi.
 */
@Serializable
data class GeminiRequest(
    val systemInstruction: Instruction? = null,
    val contents: List<GeminiContent>
)

/**
 * DTO chứa chỉ thị hệ thống dành cho mô hình AI.
 *
 * @property parts Chứa nội dung text quy định tính cách của AI.
 */
@Serializable
data class Instruction(
    val parts: GeminiPart
)

/**
 * DTO đại diện cho một thông điệp (Message) trong luồng chat.
 *
 * @property role Vai trò của người gửi (VD: "user" cho người dùng, "model" cho AI).
 * @property parts Danh sách các thành phần của thông điệp (text, ảnh).
 */
@Serializable
data class GeminiContent(
    val role: String,
    val parts: List<GeminiPart>
)

/**
 * DTO chứa thành phần nội dung chi tiết.
 *
 * @property text Chuỗi văn bản truyền đi hoặc nhận về.
 */
@Serializable
data class GeminiPart(
    val text: String
)

/**
 * DTO mapping cấu trúc dữ liệu phản hồi (Response) trả về từ Gemini.
 *
 * @property candidates Danh sách các câu trả lời mà AI sinh ra (thường chỉ lấy phần tử đầu tiên).
 */
@Serializable
data class GeminiResponse(
    val candidates: List<Candidate>? = null
)

/**
 * DTO đại diện cho một ứng viên (kết quả) trong danh sách phản hồi.
 *
 * @property content Khối dữ liệu chứa câu trả lời thực tế của AI.
 */
@Serializable
data class Candidate(
    val content: GeminiContent
)