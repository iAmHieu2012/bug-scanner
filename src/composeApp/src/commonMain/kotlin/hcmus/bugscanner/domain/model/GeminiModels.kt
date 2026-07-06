package hcmus.bugscanner.domain.model

import kotlinx.serialization.SerialName
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
 * @property parts Danh sách các thành phần của thông điệp (bao gồm cả văn bản và hình ảnh đính kèm).
 */
@Serializable
data class GeminiContent(
    val role: String,
    val parts: List<GeminiPart>
)

/**
 * DTO chứa thành phần nội dung chi tiết (Văn bản hoặc Hình ảnh).
 * Một Part có thể chứa chữ, chứa ảnh, hoặc chứa cả hai tuỳ vào ngữ cảnh.
 *
 * @property text Chuỗi văn bản truyền đi hoặc nhận về (có thể null nếu tin nhắn chỉ có ảnh).
 * @property inlineData Khối dữ liệu hình ảnh đính kèm (có thể null nếu tin nhắn chỉ có chữ).
 */
@Serializable
data class GeminiPart(
    val text: String? = null,
    @SerialName("inline_data")
    val inlineData: GeminiInlineData? = null
)

/**
 * DTO đại diện cho dữ liệu hình ảnh nội tuyến (Inline Data) gửi lên Gemini để AI "nhìn".
 *
 * @property mimeType Định dạng của tệp hình ảnh (VD: "image/jpeg", "image/png").
 * @property data Chuỗi dữ liệu hình ảnh thô đã được mã hóa sang chuẩn Base64.
 */
@Serializable
data class GeminiInlineData(
    @SerialName("mime_type")
    val mimeType: String,
    val data: String
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
