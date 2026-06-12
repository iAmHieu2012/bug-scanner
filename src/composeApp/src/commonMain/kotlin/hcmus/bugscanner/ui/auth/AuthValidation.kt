package hcmus.bugscanner.ui.auth

data class AuthValidationError(val message: String)

object AuthValidation {
    private val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

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
