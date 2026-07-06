package hcmus.bugscanner.domain.model

data class ConfidenceExplanation(
    val label: String,
    val shortLabel: String,
    val percentText: String,
    val guidance: String
)

object ConfidencePolicy {
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
