package hcmus.bugscanner.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import hcmus.bugscanner.core.utils.TimeUtils.getCurrentTimeMillis
import hcmus.bugscanner.domain.model.BugInfo
import hcmus.bugscanner.domain.model.DetectedBugSnapshot
import hcmus.bugscanner.domain.model.ScanHistory
import hcmus.bugscanner.domain.model.ScanSource
import hcmus.bugscanner.domain.model.toHistory
import hcmus.bugscanner.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel chịu trách nhiệm quản lý trạng thái hiển thị lịch sử và kết nối với Firebase.
 * Quản lý luồng dữ liệu hai chiều: Kéo danh sách về (Fetch) và Đẩy dữ liệu + hình ảnh lên (Save/Upload).
 * Người dùng dưới quyền Khách (Anonymous) sẽ bị từ chối quyền lưu trữ.
 *
 * @param repository Repository chịu trách nhiệm thao tác dữ liệu lịch sử (lưu trữ và tải hình ảnh).
 */
class HistoryViewModel(
    private val repository: HistoryRepository
) : ViewModel() {

    private val _historyList = MutableStateFlow<List<ScanHistory>>(emptyList())

    /**
     * Dòng chảy trạng thái chứa danh sách lịch sử quét.
     * Giao diện (UI) sẽ Collect biến này để tự động Recomposition khi có thay đổi.
     */
    val historyList: StateFlow<List<ScanHistory>> = _historyList.asStateFlow()

    private val _isSavingHistory = MutableStateFlow(false)

    /**
     * Trạng thái cho biết đang trong quá trình tải ảnh và lưu lịch sử lên server hay không.
     */
    val isSavingHistory: StateFlow<Boolean> = _isSavingHistory.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)

    /**
     * Thông điệp kết quả của việc lưu lịch sử (thành công hoặc thông báo lỗi).
     */
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    /**
     * Thêm bản ghi nhận diện mới vào lịch sử.
     * Thực hiện tải hình ảnh lên Firebase Storage trước khi lưu thông tin vào Firestore.
     *
     * @param snapshot Đối tượng snapshot chứa thông tin côn trùng và mảng byte hình ảnh.
     */
    fun addHistory(snapshot: DetectedBugSnapshot) {
        val currentUser = Firebase.auth.currentUser

        if (currentUser == null || currentUser.isAnonymous) {
            _saveMessage.value = null
            return
        }

        viewModelScope.launch {
            _isSavingHistory.value = true
            var uploadedUrl = snapshot.bug.imageUrl
            var uploadFailed = false

            try {
                if (snapshot.imageBytes != null) {
                    val url = repository.uploadImage(currentUser.uid, snapshot.imageBytes)
                    if (url != null) {
                        uploadedUrl = url
                    } else {
                        uploadFailed = true
                    }
                }

                val newHistory = snapshot.toHistory(
                    userId = currentUser.uid,
                    timestamp = getCurrentTimeMillis(),
                    uploadedImageUrl = uploadedUrl
                )

                if (uploadFailed && snapshot.imageBytes != null) {
                    repository.saveOfflineHistory(currentUser.uid, newHistory, snapshot.imageBytes)
                    _historyList.value = listOf(newHistory.copy(imageUrl = "offline")) + _historyList.value
                    _saveMessage.value = "Đã lưu ngoại tuyến (Chờ có mạng để đồng bộ)."
                } else {
                    val saved = repository.saveHistory(newHistory)
                    if (saved) {
                        _historyList.value = listOf(newHistory) + _historyList.value
                        _saveMessage.value = "Đã lưu kết quả vào lịch sử."
                    } else {
                        if (snapshot.imageBytes != null) {
                            repository.saveOfflineHistory(currentUser.uid, newHistory, snapshot.imageBytes)
                            _saveMessage.value = "Lưu Firestore lỗi. Đã lưu ngoại tuyến."
                        } else {
                            _saveMessage.value = "Chưa lưu được lịch sử. Vui lòng thử lại."
                        }
                    }
                }
            } catch (e: Exception) {
                _saveMessage.value = "Chưa lưu được lịch sử. Vui lòng kiểm tra kết nối."
            } finally {
                _isSavingHistory.value = false
            }
        }
    }

    /**
     * Thêm bản ghi nhận diện mới (dạng legacy/thủ công chỉ có tên và hình ảnh) vào lịch sử.
     *
     * @param bugName Tên côn trùng được nhận diện.
     * @param imageBytes Dữ liệu mảng byte của hình ảnh.
     */
    fun addHistory(bugName: String, imageBytes: ByteArray? = null) {
        val legacyBug = BugInfo.empty().copy(
            id = bugName,
            name = bugName,
            scientificName = bugName
        )
        addHistory(
            DetectedBugSnapshot(
                bug = legacyBug,
                imageBytes = imageBytes,
                source = ScanSource.UNKNOWN
            )
        )
    }

    /**
     * Tải toàn bộ danh sách lịch sử nhận diện của người dùng hiện tại từ cơ sở dữ liệu.
     */
    fun fetchHistory() {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null && !currentUser.isAnonymous) {
            viewModelScope.launch {
                val offlineCount = repository.getOfflineHistoryCount()
                if (offlineCount > 0) {
                    _saveMessage.value = "Phát hiện $offlineCount bản ghi chưa đồng bộ. Đang tiến hành tải lên đám mây..."
                    _isSavingHistory.value = true
                    
                    val syncedCount = repository.syncOfflineHistory()
                    
                    _isSavingHistory.value = false
                    if (syncedCount > 0) {
                        _saveMessage.value = "Tuyệt vời! Đã đồng bộ thành công $syncedCount bản ghi."
                    } else {
                        _saveMessage.value = "Đồng bộ thất bại do mạng yếu. Sẽ thử lại sau."
                    }
                }
                
                _historyList.value = repository.getUserHistory(currentUser.uid)
            }
        }
    }
}
