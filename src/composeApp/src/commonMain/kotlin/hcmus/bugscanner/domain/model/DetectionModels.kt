package hcmus.bugscanner.domain.model

/**
 * Lưu trữ kết quả phân tích nhận diện vật thể của mô hình AI (YOLO) đối với một cá thể.
 *
 * @property x1 Tọa độ góc trên bên trái (trục X) của Bounding Box.
 * @property y1 Tọa độ góc trên bên trái (trục Y) của Bounding Box.
 * @property x2 Tọa độ góc dưới bên phải (trục X) của Bounding Box.
 * @property y2 Tọa độ góc dưới bên phải (trục Y) của Bounding Box.
 * @property score Điểm tin cậy (Confidence Score) của mô hình từ 0.0 đến 1.0.
 * @property className Tên nhãn (Label) của đối tượng được nhận diện.
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
 * Kết quả nhận diện tổng thể cho một khung hình (frame) hoặc một bức ảnh.
 *
 * @property boxes Danh sách các đối tượng [DetectionResult] phát hiện được trong khung hình.
 * @property sourceWidth Chiều rộng nguyên bản của khung hình truyền vào mô hình.
 * @property sourceHeight Chiều cao nguyên bản của khung hình truyền vào mô hình.
 */
data class FrameResult(
    val boxes: List<DetectionResult>,
    val sourceWidth: Int,
    val sourceHeight: Int
)