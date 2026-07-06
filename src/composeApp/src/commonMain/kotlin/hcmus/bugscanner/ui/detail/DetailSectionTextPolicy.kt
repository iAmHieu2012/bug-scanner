package hcmus.bugscanner.ui.detail

/**
 * Lớp chứa dữ liệu đại diện cho một mục chi tiết trên giao diện (ví dụ: mô tả, đặc điểm).
 *
 * @property label Nhãn tiêu đề của mục (ví dụ: "Đặc điểm").
 * @property body Nội dung chi tiết của mục.
 */
data class DetailSectionItem(
    val label: String,
    val body: String
) {
    val fullText: String
        get() = if (label.isBlank()) body else "$label: $body"
}

/**
 * Policy xử lý văn bản cho các phần chi tiết của sinh vật.
 * Cắt nhỏ chuỗi văn bản dài thành các đoạn nhỏ dựa trên dấu chấm phẩy và gạch đầu dòng.
 */
object DetailSectionTextPolicy {
    /**
     * Phân tách chuỗi văn bản thành danh sách các [DetailSectionItem].
     *
     * @param content Nội dung chuỗi văn bản thô cần phân tách.
     * @return Danh sách các mục đã được phân tách và làm sạch.
     */
    fun sectionItems(content: String): List<DetailSectionItem> {
        return content
            .lines()
            .flatMap { line -> line.split(";") }
            .map { it.trim().trimStart('-', '•').trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .map { item ->
                val label = item.substringBefore(": ", missingDelimiterValue = "")
                val body = item.substringAfter(": ", missingDelimiterValue = "")
                if (label.isNotBlank() && body.isNotBlank()) {
                    DetailSectionItem(label = label, body = body)
                } else {
                    DetailSectionItem(label = "", body = item)
                }
            }
    }
}
