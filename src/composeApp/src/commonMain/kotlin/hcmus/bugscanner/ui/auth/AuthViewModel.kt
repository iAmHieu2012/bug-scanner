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
 * Đại diện cho các trạng thái của chu trình xác thực (Authentication State).
 * Được sử dụng để cập nhật giao diện (UI) tương ứng với từng giai đoạn xử lý.
 */
sealed class AuthState {
    /** Trạng thái tĩnh (chờ), chưa có hành động nào diễn ra. */
    object Idle : AuthState()

    /** Đang thực hiện gọi API mạng, UI nên hiển thị vòng xoay (Loading). */
    object Loading : AuthState()

    /**
     * Xác thực thành công.
     * @property uid Mã định danh người dùng do Firebase cấp.
     * @property isGuest Đánh dấu `true` nếu là phiên đăng nhập ẩn danh (không email).
     */
    data class Success(val uid: String, val isGuest: Boolean) : AuthState()

    /**
     * Xảy ra lỗi trong quá trình xác thực.
     * @property message Chuỗi thông báo mô tả lỗi để hiển thị lên màn hình.
     */
    data class Error(val message: String) : AuthState()
}

/**
 * ViewModel chịu trách nhiệm quản lý toàn bộ nghiệp vụ (Business Logic) Đăng nhập/Đăng ký.
 * Giao tiếp trực tiếp với Firebase Authentication thông qua thư viện hỗ trợ KMP (GitLive).
 */
class AuthViewModel : ViewModel() {
    // Khởi tạo Firebase Auth instance
    private val auth = Firebase.auth

    // Trạng thái nội bộ
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // KIỂM TRA PHIÊN ĐĂNG NHẬP (AUTO-LOGIN) KHI MỞ APP
        checkCurrentUser()
    }

    /**
     * Kiểm tra xem thiết bị đã có user nào đăng nhập từ trước chưa.
     */
    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Đã có tài khoản lưu sẵn -> Vào thẳng App
            _authState.value = AuthState.Success(
                uid = currentUser.uid,
                isGuest = currentUser.isAnonymous
            )
        } else {
            // Chưa có tài khoản -> Ở lại trạng thái Idle để hiện màn hình Đăng nhập
            _authState.value = AuthState.Idle
        }
    }

    /**
     * Đăng ký một tài khoản mới bằng Email và Mật khẩu.
     *
     * @param email Địa chỉ email người dùng nhập.
     * @param pass Mật khẩu người dùng nhập (Nên >= 6 ký tự).
     */
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

    /**
     * Xác thực một tài khoản đã tồn tại bằng Email và Mật khẩu.
     *
     * @param email Địa chỉ email đã đăng ký.
     * @param pass Mật khẩu ứng với email.
     */
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
                _authState.value = AuthState.Error("Sai email hoặc mật khẩu. Vui lòng thử lại.")
            }
        }
    }

    /**
     * Đăng nhập dưới quyền Khách (Anonymous) mà không cần tạo tài khoản.
     */
    fun signInAnonymously() {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = auth.signInAnonymously()
                val user = result.user
                _authState.value = AuthState.Success(user?.uid ?: "", isGuest = true)
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Lỗi đăng nhập ẩn danh: ${e.message}")
            }
        }
    }

    /**
     * Chấm dứt phiên đăng nhập hiện tại, xóa Token và đẩy trạng thái UI về `Idle`.
     */
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