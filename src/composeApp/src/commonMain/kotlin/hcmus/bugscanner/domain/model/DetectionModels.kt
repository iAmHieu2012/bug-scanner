package hcmus.bugscanner.domain.model

/**
 * Lưu trữ kết quả bounding box và confidence score từ model YOLO.
 */
data class DetectionResult(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val score: Float,
    val className: String
)

/**
 * Kết quả nhận diện tổng thể cho một camera frame.
 */
data class FrameResult(
    val boxes: List<DetectionResult>,
    val sourceWidth: Int,
    val sourceHeight: Int
)