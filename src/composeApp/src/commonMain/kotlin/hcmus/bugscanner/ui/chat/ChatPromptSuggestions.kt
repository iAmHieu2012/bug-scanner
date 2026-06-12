package hcmus.bugscanner.ui.chat

import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.model.ScanSource

object ChatPromptSuggestions {
    val defaultPrompts = listOf(
        "Côn trùng này có nguy hiểm với người không?",
        "Làm sao phân biệt côn trùng có ích và gây hại?",
        "Nên xử lý côn trùng trong nhà như thế nào?",
        "Giải thích kết quả nhận diện theo cách dễ hiểu."
    )

    fun detailPrompt(name: String): String =
        "Cung cấp cho tôi thông tin chi tiết và cách xử lý ${name.trim()}?"

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
            "Nguồn nhận diện: ${source.displayName}",
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
