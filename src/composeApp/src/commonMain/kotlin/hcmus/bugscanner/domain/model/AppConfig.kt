package hcmus.bugscanner.domain.model

import kotlinx.serialization.Serializable

/**
 * Cấu hình động của ứng dụng, được lưu trữ trên Firestore collection `app_config`.
 * Admin có thể chỉnh sửa các giá trị này từ xa mà không cần cập nhật phiên bản ứng dụng.
 *
 * @property geminiModel Tên model Gemini AI sử dụng cho nhận diện.
 * @property geminiSystemPrompt System prompt gửi kèm cho Gemini AI.
 * @property groqModel Tên model Groq AI sử dụng cho phân tích.
 * @property groqSystemPrompt System prompt gửi kèm cho Groq AI.
 */
@Serializable
data class AppConfig(
    val geminiModel: String = DEFAULT_GEMINI_MODEL,
    val geminiSystemPrompt: String = DEFAULT_GEMINI_SYSTEM_PROMPT,
    val groqModel: String = DEFAULT_GROQ_MODEL,
    val groqSystemPrompt: String = DEFAULT_GROQ_SYSTEM_PROMPT
) {
    companion object {
        const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"
        const val DEFAULT_GEMINI_SYSTEM_PROMPT =
            "Bạn là BugScanner AI, một trợ lý ảo chuyên nghiệp về sinh học và côn trùng học. " +
            "Hãy trả lời ngắn gọn, thân thiện và chính xác các câu hỏi về thiên nhiên, côn trùng, thực vật. " +
            "Khi nhận được ảnh, hãy phân tích kỹ các đặc điểm sinh học trên ảnh để tư vấn. " +
            "Yêu cầu định dạng: Tuyệt đối không sử dụng định dạng Markdown (như in đậm **, tiêu đề #). " +
            "Nếu cần liệt kê hoặc chia ý, chỉ sử dụng một dấu gạch ngang (-) ở đầu dòng."
        const val DEFAULT_GROQ_MODEL = "llama-3.3-70b-versatile"
        const val DEFAULT_GROQ_SYSTEM_PROMPT =
            "You are a professional agricultural assistant. You must output ONLY valid JSON without Markdown."
    }
}
