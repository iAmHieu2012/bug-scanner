package hcmus.bugscanner.domain.model

/**
 * Lớp chứa thông tin giải thích chi tiết về mức độ tin cậy của kết quả nhận diện.
 *
 * @property label Nhãn hiển thị mức độ tin cậy đầy đủ.
 * @property shortLabel Nhãn rút gọn cho UI không gian hẹp.
 * @property percentText Chuỗi phần trăm hiển thị (VD: "85%").
 * @property guidance Lời khuyên hành động tương ứng với độ tin cậy.
 */
data class ConfidenceExplanation(
    val label: String,
    val shortLabel: String,
    val percentText: String,
    val guidance: String
)

/**
 * Policy xử lý logic phân loại và tạo lời giải thích dựa trên điểm số tin cậy (Confidence).
 */
object ConfidencePolicy {
    /**
     * Chuyển đổi điểm số tin cậy (0.0 - 1.0) thành đối tượng giải thích chi tiết.
     *
     * @param confidence Điểm số nhận diện trả về từ mô hình AI.
     * @return Đối tượng [ConfidenceExplanation] chứa thông tin hiển thị.
     */
    fun explain(confidence: Float): ConfidenceExplanation {
        val normalized = confidence.coerceIn(0f, 1f)
        val percent = "${(normalized * 100).toInt()}%"
        return when {
            normalized >= 0.75f -> ConfidenceExplanation(
                label = "Độ tin cậy cao",
                shortLabel = "Cao",
                percentText = percent,
                guidance = "Kết quả khá rõ. Bạn vẫn nên đối chiếu ảnh và đặc điểm nhận dạng."
            )
            normalized >= 0.5f -> ConfidenceExplanation(
                label = "Độ tin cậy trung bình",
                shortLabel = "Trung bình",
                percentText = percent,
                guidance = "Có thể đúng, nhưng nên kiểm tra thêm đặc điểm nhận dạng trước khi kết luận."
            )
            else -> ConfidenceExplanation(
                label = "Độ tin cậy thấp",
                shortLabel = "Thấp",
                percentText = percent,
                guidance = "Kết quả chưa chắc chắn. Hãy chụp lại gần hơn, rõ hơn và đủ ánh sáng."
            )
        }
    }
}
