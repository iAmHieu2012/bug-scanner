package hcmus.bugscanner.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hcmus.bugscanner.data.remote.WikiApiService
import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.repository.EncyclopediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel quản lý logic tải dữ liệu chi tiết của côn trùng.
 * Đóng vai trò cầu nối giữa UI và tầng Data (Firebase/Wiki API), đảm bảo tuân thủ Clean Architecture.
 * Giải phóng hoàn toàn gánh nặng gọi mạng và truy vấn DB cho tầng Giao diện (UI).
 *
 * @param repository Đối tượng truy xuất dữ liệu từ Firebase Database.
 * @param wikiApi Dịch vụ gọi mạng lấy tóm tắt từ Wikipedia.
 */
class BugDetailViewModel(
    private val repository: EncyclopediaRepository,
    private val wikiApi: WikiApiService
) : ViewModel() {

    /** * Luồng trạng thái chứa thông tin chi tiết của sinh vật.
     * UI sẽ collect luồng này để tự động cập nhật giao diện khi có dữ liệu mới.
     */
    private val _detailedBug = MutableStateFlow<BugInfo?>(null)
    val detailedBug: StateFlow<BugInfo?> = _detailedBug.asStateFlow()

    /** * Luồng trạng thái hiển thị tiến trình tải (Loading state).
     * Bằng `true` khi đang chờ API/Firebase trả kết quả.
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Hàm tải thông tin chi tiết của côn trùng.
     * Chỉ gọi API/Firebase nếu dữ liệu truyền vào đang bị thiếu (như lấy từ Camera hoặc Lịch sử).
     *
     * @param initialBug Dữ liệu cơ bản truyền từ màn hình trước để làm dữ liệu khởi tạo.
     */
    fun loadBugDetails(initialBug: BugInfo) {
        _detailedBug.value = initialBug

        // Nếu đã có đủ dữ liệu xử lý (treatment) và không có link Wiki thì không cần fetch
        if (initialBug.treatment.isNotBlank() && initialBug.wikiUrl.isBlank()) return

        _isLoading.value = true
        viewModelScope.launch {
            try {
                // 1. Ưu tiên tìm trong Firebase trước (đảm bảo tính năng Quét Camera không bị ảnh hưởng)
                val realBug = repository.getBugByScientificName(initialBug.scientificName)

                if (realBug != null) {
                    // Cập nhật dữ liệu từ DB, giữ lại ảnh gốc từ Lịch sử (nếu có)
                    _detailedBug.value = realBug.copy(
                        imageUrl = initialBug.imageUrl.takeIf { it.isNotBlank() } ?: realBug.imageUrl
                    )
                } else if (initialBug.wikiUrl.isNotBlank()) {
                    // 2. Nếu Firebase KHÔNG CÓ, tiến hành bóc tách Link Wiki để kéo Text tóm tắt
                    val uriParts = initialBug.wikiUrl.split("/wiki/")
                    if (uriParts.size == 2) {
                        val lang = uriParts[0].substringAfter("://").substringBefore(".")
                        val title = uriParts[1]

                        val summary = wikiApi.getSummaryByTitle(title, lang)
                        if (!summary.isNullOrBlank()) {
                            _detailedBug.value = _detailedBug.value?.copy(description = summary)
                        }
                    }
                }
            } catch (e: Exception) {
                println("Lỗi khi tải chi tiết sinh vật: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}