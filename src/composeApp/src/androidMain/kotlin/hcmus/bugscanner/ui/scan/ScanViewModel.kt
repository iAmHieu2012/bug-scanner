package hcmus.bugscanner.ui.scan

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import hcmus.bugscanner.ml.YoloDetector
import java.util.concurrent.ExecutorService

/**
 * ViewModel xử lý nghiệp vụ quét và nhận diện côn trùng trên nền tảng Android.
 * Đóng vai trò cầu nối giữa giao diện (UI) và lõi mô hình AI (YoloDetector).
 *
 * @property yoloDetector Đối tượng khởi tạo và quản lý mô hình TensorFlow Lite.
 * @property cameraExecutor Thread Pool (luồng chạy nền) chuyên dụng để phân tích hình ảnh, tránh gây giật lag luồng chính (Main Thread).
 */
class ScanViewModel(
    private val yoloDetector: YoloDetector,
    val cameraExecutor: ExecutorService
) : ViewModel() {

    /**
     * Dòng chảy trạng thái chứa kết quả nhận diện (Bounding boxes, kích thước ảnh).
     * UI sẽ collect luồng này để tự động cập nhật Canvas.
     */
    val frameResult = yoloDetector.frameResult

    /**
     * Đẩy một khung hình (Bitmap) vào mô hình YOLO để phân tích.
     *
     * @param bitmap Khung hình hiện tại từ Camera hoặc ảnh tĩnh.
     * @param rotation Góc xoay của ảnh để mô hình chuẩn hóa chiều dữ liệu.
     */
    fun analyzeImage(bitmap: Bitmap, rotation: Int = 0) {
        yoloDetector.analyze(bitmap, rotation)
    }

    /**
     * Xóa sạch kết quả nhận diện hiện hành trên màn hình.
     */
    fun clearResult() {
        yoloDetector.clearResult()
    }

    /**
     * Hàm vòng đời được gọi khi ViewModel bị hủy.
     * Có nhiệm vụ dọn dẹp bộ nhớ của mô hình AI và tắt các luồng chạy nền để tránh rò rỉ bộ nhớ (Memory Leak).
     */
    override fun onCleared() {
        super.onCleared()
        yoloDetector.close()
        cameraExecutor.shutdown()
    }
}