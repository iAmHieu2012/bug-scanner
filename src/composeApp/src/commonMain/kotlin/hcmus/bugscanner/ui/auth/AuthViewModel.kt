package hcmus.bugscanner.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import hcmus.bugscanner.core.utils.TimeUtils
import hcmus.bugscanner.domain.model.UserProfile
import hcmus.bugscanner.domain.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Đại diện cho các trạng thái của chu trình xác thực (Authentication State).
 * Được sử dụng để cập nhật giao diện (UI) tương ứng với từng giai đoạn xử lý.
 */
sealed class AuthState {
    /** Đang tải trạng thái hệ thống ban đầu. */
    object Initializing : AuthState()

    /** Trạng thái chưa đăng nhập (hoặc đã đăng xuất). */
    object Unauthenticated : AuthState()

    /** Đang thực hiện gọi API mạng, UI nên hiển thị vòng xoay (Loading). */
    object Loading : AuthState()

    /**
     * Xác thực thành công.
     * @property uid Mã định danh người dùng do Firebase cấp.
     * @property isGuest Đánh dấu `true` nếu là phiên đăng nhập ẩn danh (không email).
     * @property isAdmin Đánh dấu `true` nếu UID tồn tại trong Firestore collection `admins`.
     */
    data class Success(val uid: String, val isGuest: Boolean, val isAdmin: Boolean = false) : AuthState()

    /**
     * Xảy ra lỗi trong quá trình xác thực.
     * @property message Chuỗi thông báo mô tả lỗi để hiển thị lên màn hình.
     */
    data class Error(val message: String) : AuthState()
}

/**
 * ViewModel chịu trách nhiệm quản lý toàn bộ nghiệp vụ (Business Logic) Đăng nhập/Đăng ký.
 * Giao tiếp trực tiếp với Firebase Authentication thông qua thư viện hỗ trợ KMP (GitLive).
 * Tự động kiểm tra quyền Admin và lưu hồ sơ người dùng lên Firestore sau mỗi lần xác thực thành công.
 *
 * @param adminRepository Repository cung cấp thao tác kiểm tra quyền Admin và lưu hồ sơ người dùng.
 */
class AuthViewModel(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val auth = Firebase.auth

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        viewModelScope.launch {
            auth.authStateChanged.collect { firebaseUser ->
                if (firebaseUser != null) {
                    _authState.value = AuthState.Loading
                    val isBanned = try { adminRepository.isBanned(firebaseUser.uid) } catch (_: Exception) { false }
                    if (isBanned) {
                        try { auth.signOut() } catch (_: Exception) { }
                        _authState.value = AuthState.Error("Tài khoản của bạn đã bị khóa vi phạm chính sách.")
                        return@collect
                    }

                    val isAdmin = try { adminRepository.isAdmin(firebaseUser.uid) } catch (_: Exception) { false }
                    try {
                        adminRepository.saveUserProfile(
                            UserProfile(
                                uid = firebaseUser.uid,
                                email = firebaseUser.email ?: "",
                                isAnonymous = firebaseUser.isAnonymous,
                                lastLoginAt = TimeUtils.getCurrentTimeMillis(),
                                isBanned = false
                            )
                        )
                    } catch (_: Exception) { }
                    _authState.value = AuthState.Success(
                        uid = firebaseUser.uid,
                        isGuest = firebaseUser.isAnonymous,
                        isAdmin = isAdmin
                    )
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            }
        }
    }

    /**
     * Đăng ký một tài khoản mới bằng Email và Mật khẩu.
     *
     * @param email Địa chỉ email người dùng nhập.
     * @param pass Mật khẩu người dùng nhập.
     */
    fun signUpWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Vui lòng điền đầy đủ thông tin")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email, pass)
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
                auth.signInWithEmailAndPassword(email, pass)
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
                auth.signInAnonymously()
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Lỗi đăng nhập ẩn danh: ${e.message}")
            }
        }
    }

    /**
     * Chấm dứt phiên đăng nhập hiện tại, xóa Token và đẩy trạng thái UI về `Unauthenticated`.
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
                _authState.value = AuthState.Unauthenticated
            } catch (e: Exception) {
                println("Lỗi đăng xuất: ${e.message}")
            }
        }
    }
}