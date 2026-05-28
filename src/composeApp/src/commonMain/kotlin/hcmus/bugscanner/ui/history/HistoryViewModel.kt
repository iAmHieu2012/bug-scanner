package hcmus.bugscanner.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import hcmus.bugscanner.core.utils.getCurrentTimeMillis
import hcmus.bugscanner.data.repository.HistoryRepositoryImpl
import hcmus.bugscanner.domain.model.ScanHistory
import hcmus.bugscanner.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel chịu trách nhiệm quản lý trạng thái hiển thị lịch sử và kết nối với Firebase.
 * Quản lý luồng dữ liệu hai chiều: Kéo danh sách về (Fetch) và Đẩy dữ liệu + hình ảnh lên (Save/Upload).
 */
class HistoryViewModel : ViewModel() {
    private val repository: HistoryRepository = HistoryRepositoryImpl()
    private val _historyList = MutableStateFlow<List<ScanHistory>>(emptyList())

    /**
     * Dòng chảy trạng thái chứa danh sách lịch sử quét.
     * Giao diện (UI) sẽ Collect biến này để tự động Recomposition khi có thay đổi.
     */
    val historyList: StateFlow<List<ScanHistory>> = _historyList.asStateFlow()

    /**
     * Lưu một kết quả nhận diện mới vào lịch sử người dùng.
     * Tiến trình bao gồm: Kiểm tra đăng nhập -> Upload ảnh lên Storage (nếu có) -> Lấy URL -> Ghép với Tên sinh vật -> Lưu Firestore.
     *
     * @param bugName Tên của loài côn trùng được mô hình AI phân loại thành công.
     * @param imageBytes Dữ liệu thô của bức ảnh bị đóng khung nhận diện. Mặc định là `null` nếu quá trình quét không sinh ra ảnh.
     */
    fun addHistory(bugName: String, imageBytes: ByteArray? = null) {
        val currentUser = Firebase.auth.currentUser
        // Khách (Anonymous) không được phép lưu lịch sử vào Database
        if (currentUser != null && !currentUser.isAnonymous) {
            viewModelScope.launch {
                var uploadedUrl = ""

                // 1. Tải ảnh lên Storage trước tiên nếu ByteArray tồn tại
                if (imageBytes != null) {
                    val url = repository.uploadImage(currentUser.uid, imageBytes)
                    if (url != null) {
                        uploadedUrl = url
                    }
                }

                // 2. Tạo Entity lịch sử hoàn chỉnh với URL ảnh tương ứng
                val newHistory = ScanHistory(
                    userId = currentUser.uid,
                    bugName = bugName,
                    timestamp = getCurrentTimeMillis(), // Đã được đồng bộ sang Double cho Web JS
                    imageUrl = uploadedUrl
                )

                // 3. Đẩy lên Firestore
                repository.saveHistory(newHistory)
            }
        }
    }

    /**
     * Truy xuất toàn bộ lịch sử quét của người dùng đang đăng nhập từ Firestore.
     * Tự động bỏ qua yêu cầu nếu phát hiện người dùng đang dùng phiên khách (Guest Mode).
     */
    fun fetchHistory() {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null && !currentUser.isAnonymous) {
            viewModelScope.launch {
                _historyList.value = repository.getUserHistory(currentUser.uid)
            }
        }
    }
}