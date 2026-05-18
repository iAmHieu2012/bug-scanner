package hcmus.bugscanner.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Trạng thái của quá trình xác thực.
 */
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val uid: String, val isGuest: Boolean) : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * ViewModel quản lý logic đăng nhập, đăng ký với Firebase Auth Đa nền tảng (GitLive).
 */
class AuthViewModel : ViewModel() {
    private val auth = Firebase.auth

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _authState.value = AuthState.Success(
                uid = currentUser.uid,
                isGuest = currentUser.isAnonymous
            )
        }
    }

    fun signInAnonymously() {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = auth.signInAnonymously()
                val user = result.user
                _authState.value = AuthState.Success(user?.uid ?: "", isGuest = true)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Lỗi đăng nhập ẩn danh")
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Vui lòng điền đầy đủ thông tin")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, pass)
                val user = result.user
                _authState.value = AuthState.Success(user?.uid ?: "", isGuest = false)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Lỗi đăng ký")
            }
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Vui lòng điền đầy đủ thông tin")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, pass)
                val user = result.user
                _authState.value = AuthState.Success(user?.uid ?: "", isGuest = false)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sai email hoặc mật khẩu")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
                _authState.value = AuthState.Idle
            } catch (e: Exception) {
                println("Lỗi đăng xuất: ${e.message}")
            }
        }
    }
}