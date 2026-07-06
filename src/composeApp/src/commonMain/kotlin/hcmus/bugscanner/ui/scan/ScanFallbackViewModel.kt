package hcmus.bugscanner.ui.scan

import androidx.lifecycle.ViewModel
import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.model.DetectedBugSnapshot
import hcmus.bugscanner.domain.model.ScanSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel dùng chung (commonMain) điều phối kết quả quét từ mô hình YOLO offline.
 * Tách biệt hoàn toàn logic ra khỏi giao diện (MVVM).
 */
class ScanFallbackViewModel : ViewModel() {
    /**
     * Trạng thái sự kiện mang dữ liệu sinh học [DetectedBugSnapshot] để kích hoạt chuyển hướng màn hình sau khi quét.
     */
    private val _scanEvent = MutableStateFlow<DetectedBugSnapshot?>(null)
    val scanEvent: StateFlow<DetectedBugSnapshot?> = _scanEvent.asStateFlow()

    fun clearScanEvent() {
        _scanEvent.value = null
    }

    /**
     * Xử lý kết quả nhận diện từ mô hình AI Offline (YOLO) và đóng gói thành đối tượng [DetectedBugSnapshot].
     *
     * @param className Tên lớp côn trùng từ mô hình YOLO.
     * @param displayName Tên hiển thị bằng tiếng Việt.
     * @param confidence Độ tin cậy (từ 0.0 đến 1.0) của kết quả nhận diện.
     * @param imageBytes Dữ liệu hình ảnh đính kèm.
     */
    fun handleYoloDetection(className: String, displayName: String, confidence: Float, imageBytes: ByteArray?) {
        val bugInfo = BugInfo.empty().copy(
            id = className,
            name = displayName,
            scientificName = className,
            identification = "Nguồn nhận diện: ${ScanSource.YOLO.userFacingName}\nĐộ tin cậy: ${(confidence * 100).toInt()}%"
        )
        _scanEvent.value = DetectedBugSnapshot(
            bug = bugInfo,
            imageBytes = imageBytes,
            confidence = confidence,
            source = ScanSource.YOLO
        )
    }
}
