package hcmus.bugscanner.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
 * ViewModel quản lý logic đăng nhập, đăng ký và phiên làm việc với Firebase Auth.
 */
class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth

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
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d("AuthViewModel", "Đăng nhập ẩn danh thành công: ${user?.uid}")
                    _authState.value = AuthState.Success(user?.uid ?: "", isGuest = true)
                } else {
                    Log.e("AuthViewModel", "Đăng nhập ẩn danh thất bại", task.exception)
                    _authState.value = AuthState.Error(task.exception?.message ?: "Lỗi không xác định")
                }
            }
    }

    fun signUpWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Vui lòng điền đầy đủ thông tin")
            return
        }
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    _authState.value = AuthState.Success(user?.uid ?: "", isGuest = false)
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Lỗi đăng ký")
                }
            }
    }

    fun signInWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Vui lòng điền đầy đủ thông tin")
            return
        }
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    _authState.value = AuthState.Success(user?.uid ?: "", isGuest = false)
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Sai email hoặc mật khẩu")
                }
            }
    }

    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }
}