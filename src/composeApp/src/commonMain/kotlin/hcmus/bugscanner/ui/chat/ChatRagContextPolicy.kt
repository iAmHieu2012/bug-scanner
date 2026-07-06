package hcmus.bugscanner.ui.chat

import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.model.HarmfulnessLevel

/**
 * Lớp chịu trách nhiệm xây dựng ngữ cảnh bách khoa toàn thư (RAG) để gửi làm System Prompt cho AI.
 */
object ChatRagContextPolicy {
    /**
     * Xây dựng nội dung System Prompt cho Gemini bằng cách kết hợp câu lệnh gốc và ngữ cảnh bách khoa.
     *
     * @param basePrompt Câu lệnh hệ thống gốc (thường lấy từ cấu hình động).
     * @param contextBug Thông tin chi tiết của sinh vật để AI dùng làm cơ sở tham chiếu.
     * @return Chuỗi System Prompt đã hoàn thiện để gửi cho Gemini.
     */
    fun systemInstruction(basePrompt: String, contextBug: BugInfo?): String {
        val context = contextBug?.takeIf { it.hasUsefulContext() }?.toGeminiContext()
        return if (context == null) {
            basePrompt
        } else {
            buildString {
                appendLine(basePrompt)
                appendLine()
                appendLine("Ngữ cảnh từ cơ sở dữ liệu BugScanner:")
                appendLine(context)
                appendLine()
                append("Ưu tiên sử dụng ngữ cảnh trên khi trả lời câu hỏi về loài này. Nếu thông tin trong ngữ cảnh chưa đủ, hãy nói rõ phần nào cần kiểm chứng thêm thay vì bịa thêm dữ kiện. Hãy chuyển nội dung nguồn thành lời tư vấn đơn giản, không lặp lại văn bản kỹ thuật.")
            }
        }
    }

    private fun BugInfo.hasUsefulContext(): Boolean =
        listOf(name, englishName, scientificName, description, identification, danger, treatment, season, wikiUrl)
            .any { it.isNotBlank() } ||
            listOf(affectedCrops, hostPlants, damageSymptoms, identificationTips, whereToFind, safeActions, ipmNotes)
                .any { it.isNotEmpty() }

    private fun BugInfo.toGeminiContext(): String {
        val lines = listOfNotNull(
            "Tên tiếng Việt" to name,
            "Tên tiếng Anh" to englishName,
            "Tên khoa học" to scientificName,
            "Tóm tắt" to description,
            "Cây trồng thường gặp" to affectedCrops.joinClean(),
            "Cây ký chủ" to hostPlants.joinClean(),
            "Dấu hiệu gây hại" to damageSymptoms.joinClean(),
            "Dấu hiệu nhận biết" to listOf(identification, identificationTips.joinClean()).joinClean(),
            "Thường thấy ở" to whereToFind.joinClean(),
            "Thời điểm thường gặp" to season,
            "Mức độ gây hại" to danger,
            "Nhóm trong ứng dụng" to HarmfulnessLevel.fromValue(harmfulnessLevel).label,
            "Việc nên làm" to listOf(treatment, safeActions.joinClean(), ipmNotes.joinClean()).joinClean(),
            "Nguồn tham khảo" to sourceRefs.joinClean().ifBlank { wikiUrl }
        )

        return lines
            .mapNotNull { (label, value) ->
                val cleanValue = value.cleanForFarmerPrompt()
                if (cleanValue.isBlank()) null else "$label: $cleanValue"
            }
            .joinToString(separator = "\n")
    }

    private fun List<String>.joinClean(): String =
        map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("; ")

    private fun String.cleanForFarmerPrompt(): String {
        val forbidden = listOf("YOLO", "IP102", "GBIF", "iNaturalist", "dataset", "mã lớp", "nhãn mô hình")
        return lines()
            .map { it.trim() }
            .filter { line -> line.isNotBlank() && forbidden.none { line.contains(it, ignoreCase = true) } }
            .joinToString(" ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
