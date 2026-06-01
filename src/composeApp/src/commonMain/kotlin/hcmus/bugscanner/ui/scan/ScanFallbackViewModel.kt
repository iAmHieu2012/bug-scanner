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
 * @property apiService Dịch vụ mạng iNaturalist được tiêm tự động thông qua Dependency Injection (Koin).
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
     * Gửi dữ liệu hình ảnh lên máy chủ iNaturalist để định danh loài côn trùng.
     * Tự động trích xuất và đóng gói kết quả sơ bộ thành đối tượng [BugInfo].
     *
     * @param imageBytes Mảng byte (ByteArray) của bức ảnh cần phân tích.
     * @param onResult Callback trả về đối tượng [BugInfo] chứa thông tin sinh vật nếu nhận diện thành công, trả về null nếu thất bại hoặc có lỗi mạng.
     */
    fun analyzeFallbackImage(imageBytes: ByteArray, onResult: (BugInfo?) -> Unit) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val response = apiService.identifyImageByVision(imageBytes)
                val taxon = response.results.firstOrNull()

                if (taxon != null) {
                    val rankVN = when(taxon.rank) {
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
                    val bioStats = "• Tên khoa học chuẩn: $scientificName\n" +
                            "• Tên quốc tế (Tiếng Anh): $englishName\n" +
                            "• Cấp bậc sinh học: $rankVN"

                    val bugInfo = BugInfo(
                        id = taxon.id.toString(),
                        name = scientificName,
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
                    onResult(null)
                }
            } catch (e: Exception) {
                println("Lỗi nhận diện ảnh qua API iNaturalist: ${e.message}")
                onResult(null)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }
}