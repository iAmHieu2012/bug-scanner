package hcmus.bugscanner.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hcmus.bugscanner.data.remote.GroqApiService
import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.repository.EncyclopediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel quản lý logic tải và sinh dữ liệu chi tiết của côn trùng.
 * Ứng dụng cơ chế Lazy Loading & Crowdsourcing: Tra cứu Firebase trước,
 * nếu thiếu dữ liệu sẽ kích hoạt AI sinh nội dung và tự động lưu ngược lên Firebase.
 *
 * @param repository Đối tượng giao tiếp với Firebase Database.
 * @param groqApi Dịch vụ gọi AI sinh nội dung chuyên sâu.
 */
class BugDetailViewModel(
    private val repository: EncyclopediaRepository,
    private val groqApi: GroqApiService
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

        if (initialBug.treatment.isNotBlank() && initialBug.name != initialBug.scientificName) return

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val realBug = repository.getBugByScientificName(initialBug.scientificName)

                if (realBug != null) {
                    _detailedBug.value = realBug.copy(
                        imageUrl = initialBug.imageUrl.takeIf { it.isNotBlank() } ?: realBug.imageUrl
                    )
                } else {
                    val aiData = groqApi.generateBugInfo(initialBug.scientificName, initialBug.englishName)

                    val completeBug = initialBug.copy(
                        name = aiData.nameVi.ifBlank { initialBug.scientificName },
                        description = aiData.description,
                        identification = initialBug.identification + "\n" + aiData.identification,
                        danger = aiData.danger,
                        treatment = aiData.treatment
                    )

                    _detailedBug.value = completeBug

                    launch {
                        repository.saveBugToFirebase(completeBug)
                    }
                }
            } catch (e: Exception) {
                _detailedBug.value = _detailedBug.value?.copy(
                    description = "Đã xảy ra lỗi kết nối khi phân tích dữ liệu chuyên sâu. Vui lòng kiểm tra lại kết nối mạng."
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
}