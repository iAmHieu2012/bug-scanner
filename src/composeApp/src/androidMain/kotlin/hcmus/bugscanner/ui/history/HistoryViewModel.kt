package hcmus.bugscanner.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import hcmus.bugscanner.domain.model.ScanHistory
import hcmus.bugscanner.data.remote.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel xử lý thao tác kéo và lưu lịch sử nhận diện từ Firestore.
 */
class HistoryViewModel : ViewModel() {
    private val repository = HistoryRepository()

    private val _historyList = MutableStateFlow<List<ScanHistory>>(emptyList())
    val historyList: StateFlow<List<ScanHistory>> = _historyList.asStateFlow()

    fun addHistory(bugName: String) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null && !currentUser.isAnonymous) {
            viewModelScope.launch {
                val newHistory = ScanHistory(
                    userId = currentUser.uid,
                    bugName = bugName
                )
                repository.saveHistory(newHistory)
            }
        }
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