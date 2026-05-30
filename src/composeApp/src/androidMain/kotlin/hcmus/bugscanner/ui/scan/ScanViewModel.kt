package hcmus.bugscanner.ui.scan

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hcmus.bugscanner.domain.model.FrameResult
import hcmus.bugscanner.ml.YoloDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * ViewModel xử lý nghiệp vụ quét và nhận diện côn trùng trên nền tảng Android.
 * Đóng vai trò cầu nối giữa giao diện (UI) và lõi mô hình AI (YoloDetector).
 * Tự động quản lý vòng đời và tiến trình khởi tạo của mô hình AI một cách an toàn.
 *
 * @param context Context của ứng dụng dùng để khởi tạo [YoloDetector] (đọc file mô hình AI).
 */
class ScanViewModel(private val context: Context) : ViewModel() {

    /**
     * Đối tượng quản lý mô hình TensorFlow Lite.
     * Được khởi tạo ngầm bên trong [viewModelScope] để không gây đơ UI (Main Thread).
     */
    private var yoloDetector: YoloDetector? = null

    /**
     * Thread Pool (luồng chạy nền) chuyên dụng để phân tích hình ảnh (đặc biệt từ CameraX),
     * tránh gây giật lag luồng chính (Main Thread).
     */
    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /**
     * Dòng chảy trạng thái báo hiệu mô hình AI đã tải xong và sẵn sàng nhận diện hay chưa.
     * UI sẽ collect luồng này để hiển thị hoặc ẩn vòng xoay Loading.
     */
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    /**
     * Dòng chảy trạng thái chứa kết quả nhận diện (Bounding boxes, kích thước ảnh).
     * UI sẽ collect luồng này để tự động cập nhật Canvas.
     */
    private val _frameResult = MutableStateFlow(FrameResult(emptyList(), 0, 0))
    val frameResult: StateFlow<FrameResult> = _frameResult.asStateFlow()

    init {
        // Tự động load Model dưới background (I/O thread) ngay khi ViewModel ra đời
        viewModelScope.launch(Dispatchers.IO) {
            yoloDetector = YoloDetector(context)

            // Lắng nghe liên tục kết quả từ Yolo và đẩy lên luồng UI (Main thread)
            viewModelScope.launch(Dispatchers.Main) {
                yoloDetector?.frameResult?.collect { result ->
                    _frameResult.value = result
                }
            }

            // Báo hiệu cho UI biết AI đã sẵn sàng
            _isReady.value = true
        }
    }

    /**
     * Đẩy một khung hình (Bitmap) vào mô hình YOLO để phân tích.
     *
     * @param bitmap Khung hình hiện tại từ Camera hoặc ảnh tĩnh.
     * @param rotation Góc xoay của ảnh để mô hình chuẩn hóa chiều dữ liệu trước khi quét.
     */
    fun analyzeImage(bitmap: Bitmap, rotation: Int = 0) {
        yoloDetector?.analyze(bitmap, rotation)
    }

    /**
     * Hàm vòng đời được gọi khi ViewModel bị hủy.
     * Có nhiệm vụ dọn dẹp bộ nhớ của mô hình AI và tắt các luồng chạy nền để tránh rò rỉ bộ nhớ (Memory Leak).
     */
    override fun onCleared() {
        super.onCleared()
        yoloDetector?.close()
        cameraExecutor.shutdown()
    }
}