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
 */
class ScanFallbackViewModel(
    private val apiService: INaturalistApiService
) : ViewModel() {

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

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
