package hcmus.bugscanner.ui.chat

import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.model.HarmfulnessPolicy

/**
 * Builds the grounded encyclopedia context sent to Gemini.
 */
object ChatRagContextPolicy {
    private const val BASE_SYSTEM_INSTRUCTION =
        "Bạn là BugScanner AI, một trợ lý ảo chuyên nghiệp về sinh học và côn trùng học. " +
            "Luôn trả lời bằng tiếng Việt tự nhiên. Hãy trả lời ngắn gọn, thân thiện và chính xác. " +
            "Đối tượng chính là nông dân và người dùng phổ thông, nên hãy dùng từ dễ hiểu. " +
            "Khi nhận được ảnh, hãy phân tích kỹ các đặc điểm sinh học trên ảnh để tư vấn. " +
            "Không nhắc đến tên mô hình, bộ dữ liệu, mã lớp, nhãn mô hình, nguồn API phân loại hoặc chi tiết kỹ thuật nội bộ trừ khi người dùng hỏi trực tiếp về cách hệ thống hoạt động. " +
            "Tuyệt đối không sử dụng định dạng Markdown như in đậm ** hoặc tiêu đề #. " +
            "Nếu cần liệt kê hoặc chia ý, chỉ sử dụng một dấu gạch ngang (-) ở đầu dòng."

    fun systemInstruction(contextBug: BugInfo?): String {
        val context = contextBug?.takeIf { it.hasUsefulContext() }?.toGeminiContext()
        return if (context == null) {
            BASE_SYSTEM_INSTRUCTION
        } else {
            buildString {
                appendLine(BASE_SYSTEM_INSTRUCTION)
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
            "Nhóm trong ứng dụng" to HarmfulnessPolicy.fromValue(harmfulnessLevel).label,
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
