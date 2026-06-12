package hcmus.bugscanner.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import hcmus.bugscanner.core.utils.getCurrentTimeMillis
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
 */
class HistoryViewModel(
    private val repository: HistoryRepository
) : ViewModel() {

    private val _historyList = MutableStateFlow<List<ScanHistory>>(emptyList())
    val historyList: StateFlow<List<ScanHistory>> = _historyList.asStateFlow()

    private val _isSavingHistory = MutableStateFlow(false)
    val isSavingHistory: StateFlow<Boolean> = _isSavingHistory.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

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

                val saved = repository.saveHistory(newHistory)
                if (saved) {
                    _historyList.value = listOf(newHistory) + _historyList.value
                    _saveMessage.value = if (uploadFailed) {
                        "Đã lưu lịch sử nhưng chưa tải được ảnh."
                    } else {
                        "Đã lưu kết quả vào lịch sử."
                    }
                } else {
                    _saveMessage.value = "Chưa lưu được lịch sử. Vui lòng thử lại."
                }
            } catch (e: Exception) {
                _saveMessage.value = "Chưa lưu được lịch sử. Vui lòng kiểm tra kết nối."
            } finally {
                _isSavingHistory.value = false
            }
        }
    }

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

    fun fetchHistory() {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null && !currentUser.isAnonymous) {
            viewModelScope.launch {
                _historyList.value = repository.getUserHistory(currentUser.uid)
            }
        }
    }
}
