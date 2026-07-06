package hcmus.bugscanner.ui.chat

import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.model.ScanSource

/**
 * Đối tượng sinh câu hỏi gợi ý và xây dựng cấu trúc Prompt cho AI Chatbot.
 */
object ChatPromptSuggestions {
    /**
     * Danh sách các câu hỏi gợi ý mặc định khi mở màn hình chatbot.
     */
    val defaultPrompts = listOf(
        "Tôi nên chụp ảnh thế nào để nhận diện rõ hơn?",
        "Dấu hiệu nào cho thấy côn trùng đang gây hại cây trồng?",
        "Làm sao phân biệt côn trùng có ích và gây hại?",
        "Khi chưa chắc loài côn trùng, tôi nên làm gì?"
    )

    fun promptsForBug(bug: BugInfo?): List<String> {
        val name = bug?.name?.takeIf { it.isNotBlank() } ?: return defaultPrompts
        return listOf(
            "$name thường gây hại cây nào?",
            "Dấu hiệu nhận biết $name là gì?",
            "Khi thấy $name trên cây, tôi nên kiểm tra gì trước?",
            "Tóm tắt thông tin quan trọng nhất về $name."
        )
    }

    /**
     * Sinh câu lệnh (prompt) hỏi thông tin đơn giản theo tên loài.
     *
     * @param name Tên loài côn trùng.
     * @return Câu prompt hoàn chỉnh dạng chuỗi.
     */
    fun detailPrompt(name: String): String =
        "Cung cấp cho tôi thông tin chi tiết và cách xử lý ${name.trim()}?"

    /**
     * Sinh câu lệnh (prompt) chứa đầy đủ thông tin chi tiết về đối tượng nhận dạng
     * để làm ngữ cảnh đầu vào (Context) cho AI trả lời chính xác nhất.
     *
     * @param bug Đối tượng chứa thông tin sinh học của sinh vật.
     * @param confidence Độ tin cậy (%) của kết quả nhận diện (nếu có).
     * @param source Nguồn nhận dạng (YOLO hoặc iNaturalist).
     * @return Câu prompt phức hợp hoàn chỉnh.
     */
    fun detailPrompt(
        bug: BugInfo,
        confidence: Float = 0f,
        source: ScanSource = ScanSource.UNKNOWN
    ): String {
        val confidenceText = if (confidence > 0f) "${(confidence * 100).toInt()}%" else "chưa có"
        val scientificName = bug.scientificName.ifBlank { "chưa rõ" }
        val englishName = bug.englishName.ifBlank { "chưa cập nhật" }
        val contextParts = listOf(
            "Tên hiển thị: ${bug.name.ifBlank { scientificName }}",
            "Tên khoa học: $scientificName",
            "Tên tiếng Anh: $englishName",
            "Nguồn nhận diện: ${source.userFacingName}",
            "Độ tin cậy: $confidenceText",
            "Mô tả: ${bug.description.ifBlank { "chưa có" }}",
            "Đặc điểm nhận dạng: ${bug.identification.ifBlank { "chưa có" }}",
            "Mức độ nguy hại: ${bug.danger.ifBlank { "chưa có" }}",
            "Cách xử lý: ${bug.treatment.ifBlank { "chưa có" }}"
        )

        return buildString {
            appendLine("Dựa trên kết quả nhận diện sau, hãy giải thích thông tin chi tiết và cách xử lý an toàn cho người dùng.")
            contextParts.forEach { appendLine(it) }
            append("Không dùng Markdown. Trả lời ngắn gọn, chính xác, bằng tiếng Việt.")
        }
    }
}
