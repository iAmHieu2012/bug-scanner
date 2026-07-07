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
 * @property groqCrowdsourcingPrompt Prompt dùng cho Groq để generate JSON khi cào dữ liệu (Crowdsourcing).
 * @property geminiRagPrompt Prompt dặn dò Gemini cách sử dụng RAG context.
 */
@Serializable
data class AppConfig(
    val geminiModel: String = DEFAULT_GEMINI_MODEL,
    val geminiSystemPrompt: String = DEFAULT_GEMINI_SYSTEM_PROMPT,
    val groqModel: String = DEFAULT_GROQ_MODEL,
    val groqSystemPrompt: String = DEFAULT_GROQ_SYSTEM_PROMPT,
    val groqCrowdsourcingPrompt: String = DEFAULT_GROQ_CROWDSOURCING_PROMPT,
    val geminiRagPrompt: String = DEFAULT_GEMINI_RAG_PROMPT
) {
    companion object {
        const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"
        const val DEFAULT_GEMINI_SYSTEM_PROMPT =
            "Bạn là BugScanner AI, một trợ lý ảo chuyên nghiệp về sinh học và côn trùng học. " +
            "Hãy trả lời ngắn gọn, thân thiện và chính xác các câu hỏi về thiên nhiên, côn trùng, thực vật. " +
            "Khi nhận được ảnh, hãy phân tích kỹ các đặc điểm sinh học trên ảnh để tư vấn. " +
            "Yêu cầu định dạng: Tuyệt đối không sử dụng định dạng Markdown (như in đậm **, tiêu đề #). " +
            "Nếu cần liệt kê hoặc chia ý, chỉ sử dụng một dấu gạch ngang (-) ở đầu dòng."
        const val DEFAULT_GROQ_MODEL = "openai/gpt-oss-120b"
        const val DEFAULT_GROQ_SYSTEM_PROMPT =
            "You are a professional agricultural assistant. You must output ONLY valid JSON without Markdown."
        
        const val DEFAULT_GROQ_CROWDSOURCING_PROMPT = 
            "Cung cấp thông tin sinh học và nông nghiệp chi tiết bằng tiếng Việt cho loài côn trùng/sinh vật có tên khoa học là \"{SCIENTIFIC_NAME}\" (Tên tiếng Anh: \"{ENGLISH_NAME}\").\n\n" +
            "QUY TẮC BẮT BUỘC:\n" +
            "1. Trả về đúng định dạng JSON hợp lệ, KHÔNG chứa đoạn text nào nằm ngoài JSON.\n" +
            "2. Tuyệt đối không được bỏ sót key nào.\n" +
            "3. Đối với các mảng (Array), hãy liệt kê MỌI thông tin có thể (từ 3 đến 10 phần tử tùy loại), KHÔNG giới hạn số lượng.\n" +
            "4. Nếu không có dữ liệu thực tế cho một mảng, hãy trả về mảng rỗng [].\n" +
            "5. TUYỆT ĐỐI KHÔNG ghi kèm tên khoa học (tiếng Latin) vào trong mảng `affectedCrops` và `hostPlants`. Chỉ ghi tên gọi thông thường bằng tiếng Việt (Ví dụ: \"Lúa\", \"Ngô\" - KHÔNG ghi \"Lúa (Oryza sativa)\").\n\n" +
            "CẤU TRÚC JSON YÊU CẦU:\n" +
            "{\n" +
            "    \"nameVi\": \"string (Tên tiếng Việt phổ biến nhất)\",\n" +
            "    \"description\": \"string (Mô tả chi tiết sinh học, tập tính, vòng đời)\",\n" +
            "    \"identification\": \"string (Đặc điểm nhận dạng hình thái)\",\n" +
            "    \"danger\": \"string (Chỉ chọn 1: Nguy hiểm, An toàn, hoặc Theo dõi)\",\n" +
            "    \"treatment\": \"string (Biện pháp xử lý hoặc phòng trừ chi tiết)\",\n" +
            "    \"affectedCrops\": [\"string\", \"string\", \"...\"], // Toàn bộ cây trồng bị ảnh hưởng\n" +
            "    \"hostPlants\": [\"string\", \"string\", \"...\"], // Toàn bộ cây ký chủ\n" +
            "    \"damageSymptoms\": [\"string\", \"string\", \"...\"], // Dấu hiệu gây hại có thể quan sát\n" +
            "    \"identificationTips\": [\"string\", \"string\", \"...\"], // Mẹo nhận biết nhanh ngoài thực địa\n" +
            "    \"whereToFind\": [\"string\", \"string\", \"...\"], // Vị trí thường trú ngụ (VD: chồi non, mặt dưới lá)\n" +
            "    \"season\": \"string (Mùa vụ hoặc điều kiện thời tiết xuất hiện)\",\n" +
            "    \"safeActions\": [\"string\", \"string\", \"...\"], // Hành động xử lý an toàn nên làm ngay\n" +
            "    \"ipmNotes\": [\"string\", \"string\", \"...\"], // Lưu ý về Quản lý dịch hại tổng hợp (IPM)\n" +
            "    \"searchTokens\": [\"string\", \"string\", \"...\"], // Rất nhiều từ khóa, từ đồng nghĩa để tìm kiếm\n" +
            "    \"harmfulnessLevel\": \"string\" // Bắt buộc trả về đúng 1 trong 3 chữ sau (tiếng Anh viết thường): \"crop_pest\", \"low_risk\", hoặc \"beneficial\"\n" +
            "}"

        const val DEFAULT_GEMINI_RAG_PROMPT = 
            "Ưu tiên sử dụng ngữ cảnh trên khi trả lời câu hỏi về loài này. Nếu thông tin trong ngữ cảnh chưa đủ, hãy nói rõ phần nào cần kiểm chứng thêm thay vì bịa thêm dữ kiện. Hãy chuyển nội dung nguồn thành lời tư vấn đơn giản, không lặp lại văn bản kỹ thuật."
    }
}
