package hcmus.bugscanner.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.repository.EncyclopediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel quản lý logic tải dữ liệu chi tiết của côn trùng.
 * Cơ sở dữ liệu BugScanner là nguồn sự thật; AI không tự sinh hoặc lưu hồ sơ mới.
 *
 * @param repository Đối tượng giao tiếp với Firebase Database.
 */
class BugDetailViewModel(
    private val repository: EncyclopediaRepository
) : ViewModel() {

    private val _detailedBug = MutableStateFlow<BugInfo?>(null)
    val detailedBug: StateFlow<BugInfo?> = _detailedBug.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Khởi chạy tiến trình kiểm tra và tải dữ liệu chi tiết.
     *
     * @param initialBug Dữ liệu cơ bản truyền từ luồng Scan/Fallback.
     */
    fun loadBugDetails(initialBug: BugInfo) {
        _detailedBug.value = initialBug
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val realBug = repository.getBugByScientificName(initialBug.scientificName)

                _detailedBug.value = if (realBug != null) {
                    realBug.copy(
                        imageUrl = initialBug.imageUrl.takeIf { it.isNotBlank() } ?: realBug.imageUrl,
                        imageUrls = (initialBug.displayImageUrls() + realBug.displayImageUrls()).distinct()
                    )
                } else {
                    initialBug.copy(
                        description = initialBug.description.ifBlank {
                            "Chưa có thông tin chi tiết trong cơ sở dữ liệu BugScanner."
                        }
                    )
                }
            } catch (e: Exception) {
                _detailedBug.value = _detailedBug.value?.copy(
                    description = _detailedBug.value?.description?.ifBlank {
                        "Chưa tải được dữ liệu chi tiết từ cơ sở dữ liệu BugScanner."
                    } ?: "Chưa tải được dữ liệu chi tiết từ cơ sở dữ liệu BugScanner."
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
}
