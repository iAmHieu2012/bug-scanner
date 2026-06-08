package hcmus.bugscanner.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO đại diện cho yêu cầu (Request) gửi lên API của Groq (Llama 3).
 *
 * @property model Tên mô hình AI được sử dụng (VD: "llama-3.3-70b-versatile").
 * @property messages Danh sách các đoạn hội thoại hoặc nội dung cần gửi.
 * @property responseFormat Cấu hình định dạng trả về (VD: ép buộc trả về JSON).
 * @property temperature Độ sáng tạo của câu trả lời (từ 0.0 đến 1.0, số nhỏ giúp văn bản nhất quán và chính xác hơn).
 */
@Serializable
data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>,
    @SerialName("response_format")
    val responseFormat: GroqResponseFormat,
    val temperature: Double
)

/**
 * DTO đại diện cho một thông điệp (Message) trong luồng chat của Groq.
 *
 * @property role Vai trò của người gửi (VD: "system" cho hệ thống thiết lập hành vi, "user" cho người dùng, "assistant" cho AI).
 * @property content Chuỗi văn bản truyền đi hoặc nhận về.
 */
@Serializable
data class GroqMessage(
    val role: String,
    val content: String
)

/**
 * DTO cấu hình định dạng trả về từ Groq API.
 *
 * @property type Kiểu định dạng mong muốn (VD: "json_object" để ép AI trả về chuẩn JSON).
 */
@Serializable
data class GroqResponseFormat(
    val type: String
)

/**
 * DTO mapping cấu trúc dữ liệu phản hồi (Response) trả về từ Groq.
 *
 * @property choices Danh sách các lựa chọn (câu trả lời) mà AI sinh ra (thường chỉ lấy phần tử đầu tiên).
 */
@Serializable
data class GroqResponse(
    val choices: List<GroqChoice>
)

/**
 * DTO đại diện cho một ứng viên (kết quả) trong danh sách phản hồi của Groq.
 *
 * @property message Khối dữ liệu chứa câu trả lời thực tế của AI.
 */
@Serializable
data class GroqChoice(
    val message: GroqMessage
)

/**
 * Lớp dữ liệu ánh xạ cấu trúc JSON tĩnh chứa nội dung chuyên ngành sinh học/nông nghiệp từ AI trả về.
 * Các trường dữ liệu đã được gán giá trị mặc định để tránh crash ứng dụng khi có lỗi parse dữ liệu.
 *
 * @property nameVi Tên gọi tiếng Việt phổ biến nhất (hoặc dịch chuẩn sang tiếng Việt).
 * @property description Mô tả sinh học, tập tính, vòng đời.
 * @property identification Đặc điểm nhận dạng hình thái.
 * @property danger Mức độ nguy hại đối với mùa màng hoặc con người (Nguy hiểm, An toàn, Theo dõi).
 * @property treatment Biện pháp xử lý, phòng ngừa hoặc sơ cứu khuyên dùng.
 */
@Serializable
data class AiBugData(
    val nameVi: String = "",
    val description: String = "Đang cập nhật...",
    val identification: String = "Đang cập nhật...",
    val danger: String = "Theo dõi",
    val treatment: String = "Đang cập nhật..."
)