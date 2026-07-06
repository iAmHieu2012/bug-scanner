package hcmus.bugscanner.ui.auth

object AuthErrorPolicy {
    fun toUserMessage(rawMessage: String?, fallback: String): String {
        val raw = rawMessage.orEmpty()
        return when {
            raw.contains("operation-not-allowed", ignoreCase = true) ->
                "Email/Mật khẩu chưa được bật trong Firebase Authentication. Hãy bật Sign-in method Email/Password rồi thử lại."
            raw.contains("email-already-in-use", ignoreCase = true) ->
                "Email này đã được đăng ký. Hãy đăng nhập hoặc dùng email khác."
            raw.contains("weak-password", ignoreCase = true) ->
                "Mật khẩu quá yếu. Hãy dùng ít nhất 6 ký tự."
            raw.contains("invalid-email", ignoreCase = true) ->
                "Email không hợp lệ."
            raw.isNotBlank() -> raw
            else -> fallback
        }
    }
}
