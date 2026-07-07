package hcmus.bugscanner.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hcmus.bugscanner.data.remote.INaturalistApiService
import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.model.DetectedBugSnapshot
import hcmus.bugscanner.domain.model.ScanSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel dùng chung (commonMain) hỗ trợ luồng dự phòng (Fallback) và điều phối kết quả Quét.
 * Tách biệt hoàn toàn logic ra khỏi giao diện (MVVM).
 *
 * @property apiService Dịch vụ gọi API iNaturalist để phân tích ảnh dự phòng.
 */
class ScanFallbackViewModel(
    private val apiService: INaturalistApiService
) : ViewModel() {

    /**
     * Trạng thái cho biết quá trình gọi mạng API có đang diễn ra hay không.
     */
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    /**
     * Thông báo lỗi hiển thị nếu xảy ra sự cố mạng hoặc API.
     */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Trạng thái sự kiện mang dữ liệu sinh học [DetectedBugSnapshot] để kích hoạt chuyển hướng màn hình sau khi quét.
     */
    private val _scanEvent = MutableStateFlow<DetectedBugSnapshot?>(null)
    val scanEvent: StateFlow<DetectedBugSnapshot?> = _scanEvent.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

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
            identification = "Nguồn nhận diện: YOLO offline\nĐộ tin cậy: ${(confidence * 100).toInt()}%"
        )
        _scanEvent.value = DetectedBugSnapshot(
            bug = bugInfo,
            imageBytes = imageBytes,
            confidence = confidence,
            source = ScanSource.YOLO
        )
    }

    /**
     * Gửi dữ liệu hình ảnh lên máy chủ iNaturalist để định danh chuyên sâu bằng AI mạng.
     * Tự động khởi tạo Snapshot [ScanSource.INATURALIST] nếu thành công.
     *
     * @param imageBytes Mảng byte hình ảnh cần phân tích.
     */
    fun analyzeFallbackImage(imageBytes: ByteArray) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _errorMessage.value = null
            try {
                val response = apiService.identifyImageByVision(imageBytes)
                val taxon = response.results.firstOrNull()

                if (taxon != null) {
                    val rankVN = when (taxon.rank) {
                        "species" -> "Loài"
                        "subspecies" -> "Phân loài"
                        "genus" -> "Chi"
                        "family" -> "Họ"
                        "order" -> "Bộ"
                        "class" -> "Lớp"
                        "phylum" -> "Ngành"
                        else -> taxon.rank?.replaceFirstChar { it.uppercase() } ?: "Không rõ"
                    }

                    val scientificName = taxon.name
                    val englishName = taxon.englishCommonName ?: "Chưa cập nhật"
                    val bioStats = listOf(
                        "- Tên khoa học chuẩn: $scientificName",
                        "- Tên quốc tế: $englishName",
                        "- Cấp bậc sinh học: $rankVN"
                    ).joinToString("\n")

                    val photos = taxon.taxonPhotos?.mapNotNull { 
                        it.photo?.mediumUrl ?: it.photo?.squareUrl 
                    } ?: emptyList()

                    val bugInfo = BugInfo(
                        id = scientificName.lowercase().replace(" ", "_"),
                        name = taxon.preferredCommonName ?: scientificName,
                        englishName = englishName,
                        scientificName = scientificName,
                        description = "",
                        imageUrl = taxon.defaultPhoto?.mediumUrl ?: taxon.defaultPhoto?.squareUrl ?: "",
                        imageUrls = photos.take(5), // Lấy tối đa 5 ảnh
                        identification = bioStats,
                        danger = "",
                        treatment = "",
                        wikiUrl = taxon.wikipediaUrl ?: ""
                    )
                    
                    _scanEvent.value = DetectedBugSnapshot(
                        bug = bugInfo,
                        imageBytes = imageBytes,
                        confidence = 0f,
                        source = ScanSource.INATURALIST
                    )
                } else {
                    _errorMessage.value = "iNaturalist chưa tìm thấy loài phù hợp cho ảnh này."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Không kết nối được iNaturalist. Vui lòng kiểm tra mạng hoặc API token."
            } finally {
                _isAnalyzing.value = false
            }
        }
    }
}
