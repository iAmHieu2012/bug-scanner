package hcmus.bugscanner.domain.model

import kotlinx.serialization.Serializable

/**
 * Hồ sơ người dùng được lưu trữ trong Firestore collection `users`.
 * Tất cả các trường đều có giá trị mặc định để tương thích với cơ chế deserialization của Firestore.
 *
 * @property uid Mã định danh duy nhất của người dùng (Firebase Auth UID).
 * @property email Địa chỉ email đăng nhập của người dùng.
 * @property isAnonymous Đánh dấu người dùng ẩn danh (đăng nhập không cần tài khoản).
 * @property lastLoginAt Thời điểm đăng nhập gần nhất (Unix timestamp dạng milliseconds).
 * @property isBanned Đánh dấu tài khoản người dùng đã bị khóa bởi Admin (Soft Ban).
 */
@Serializable
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val isAnonymous: Boolean = false,
    val lastLoginAt: Double = 0.0,
    val isBanned: Boolean = false
)
