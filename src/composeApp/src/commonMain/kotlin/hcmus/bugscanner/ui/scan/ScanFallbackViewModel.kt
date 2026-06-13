package hcmus.bugscanner.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hcmus.bugscanner.data.remote.INaturalistApiService
import hcmus.bugscanner.domain.model.BugInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel dùng chung (commonMain) hỗ trợ luồng dự phòng (Fallback) cho chức năng Quét.
 * Đảm nhận trách nhiệm gọi API iNaturalist để phân tích hình ảnh chuyên sâu khi mô hình AI Offline (YOLO) không thể nhận diện được.
 * Tách biệt hoàn toàn với logic Native (CameraX/TFLite) để đảm bảo kiến trúc đa nền tảng (KMP) không bị phá vỡ.
 *
 * @param apiService Dịch vụ mạng iNaturalist được tiêm tự động thông qua Dependency Injection (Koin).
 */
class ScanFallbackViewModel(
    private val apiService: INaturalistApiService
) : ViewModel() {

    /**
     * Trạng thái cho biết quá trình phân tích ảnh qua mạng đang diễn ra hay không.
     * Giao diện (UI) sử dụng trạng thái này để hiển thị hiệu ứng tải (Loading/Spinner) và khóa nút bấm.
     */
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    /**
     * Trạng thái lưu trữ thông báo lỗi mới nhất nếu cuộc gọi API gặp sự cố.
     */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Xóa thông báo lỗi hiện tại.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Gửi dữ liệu hình ảnh lên máy chủ iNaturalist để định danh loài côn trùng.
     * Tự động trích xuất và đóng gói kết quả sơ bộ thành đối tượng [BugInfo].
     *
     * @param imageBytes Mảng byte (ByteArray) của bức ảnh cần phân tích.
     * @param onResult Callback trả về đối tượng [BugInfo] chứa thông tin sinh vật nếu nhận diện thành công, trả về null nếu thất bại hoặc có lỗi mạng.
     */
    fun analyzeFallbackImage(imageBytes: ByteArray, onResult: (BugInfo?) -> Unit) {
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

                    val bugInfo = BugInfo(
                        id = taxon.id.toString(),
                        name = taxon.preferredCommonName ?: scientificName,
                        englishName = englishName,
                        scientificName = scientificName,
                        description = "",
                        imageUrl = taxon.defaultPhoto?.mediumUrl ?: taxon.defaultPhoto?.squareUrl ?: "",
                        identification = bioStats,
                        danger = "",
                        treatment = "",
                        wikiUrl = taxon.wikipediaUrl ?: ""
                    )
                    onResult(bugInfo)
                } else {
                    _errorMessage.value = "iNaturalist chưa tìm thấy loài phù hợp cho ảnh này."
                    onResult(null)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Không kết nối được iNaturalist. Vui lòng kiểm tra mạng hoặc API token."
                onResult(null)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }
}
