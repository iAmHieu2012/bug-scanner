package hcmus.bugscanner.ui.auth

/**
 * Lớp lưu trữ thông báo lỗi xác thực dữ liệu đầu vào.
 *
 * @property message Nội dung thông báo lỗi chi tiết.
 */
data class AuthValidationError(val message: String)

/**
 * Đối tượng tiện ích hỗ trợ kiểm tra tính hợp lệ của thông tin tài khoản đăng nhập/đăng ký tại thiết bị cục bộ.
 */
object AuthValidation {
    private val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    /**
     * Kiểm tra định dạng Email và độ dài Mật khẩu.
     *
     * @param email Chuỗi ký tự email người dùng cung cấp.
     * @param password Mật khẩu người dùng cung cấp.
     * @return [AuthValidationError] chứa thông báo lỗi cụ thể nếu dữ liệu không hợp lệ, hoặc null nếu hợp lệ.
     */
    fun validate(email: String, password: String): AuthValidationError? {
        val cleanEmail = email.trim()

        return when {
            cleanEmail.isBlank() -> AuthValidationError("Vui lòng nhập email.")
            !emailPattern.matches(cleanEmail) -> AuthValidationError("Email không hợp lệ.")
            password.length < 6 -> AuthValidationError("Mật khẩu cần ít nhất 6 ký tự.")
            else -> null
        }
    }
}
