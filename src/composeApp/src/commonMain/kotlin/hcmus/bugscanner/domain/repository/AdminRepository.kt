package hcmus.bugscanner.domain.repository

import hcmus.bugscanner.domain.model.AppConfig
import hcmus.bugscanner.domain.model.ScanHistory
import hcmus.bugscanner.domain.model.UserProfile

/**
 * Interface định nghĩa các thao tác quản trị hệ thống dành cho Admin.
 */
interface AdminRepository {
    /**
     * Kiểm tra xem người dùng có quyền Admin hay không.
     * Tra cứu trong Firestore collection `admins`.
     *
     * @param uid Mã định danh Firebase Auth UID của người dùng cần kiểm tra.
     * @return `true` nếu người dùng là Admin, ngược lại `false`.
     */
    suspend fun isAdmin(uid: String): Boolean

    /**
     * Kiểm tra xem người dùng có bị khóa tài khoản hay không.
     * Tra cứu trong Firestore collection `users`.
     *
     * @param uid Mã định danh Firebase Auth UID của người dùng.
     * @return `true` nếu người dùng bị khóa, ngược lại `false`.
     */
    suspend fun isBanned(uid: String): Boolean

    /**
     * Lưu hoặc cập nhật hồ sơ người dùng vào Firestore collection `users`.
     *
     * @param profile Đối tượng [UserProfile] chứa thông tin cần lưu.
     */
    suspend fun saveUserProfile(profile: UserProfile)

    /**
     * Khóa hoặc mở khóa tài khoản người dùng (Soft Ban).
     *
     * @param uid Mã định danh Firebase Auth UID của người dùng.
     * @param isBanned `true` để khóa, `false` để mở khóa.
     * @return `true` nếu cập nhật thành công, ngược lại `false`.
     */
    suspend fun banUser(uid: String, isBanned: Boolean): Boolean

    /**
     * Lấy danh sách tất cả người dùng đã đăng ký trong hệ thống.
     *
     * @return Danh sách các đối tượng [UserProfile].
     */
    suspend fun getAllUsers(): List<UserProfile>

    /**
     * Lấy toàn bộ lịch sử quét, sắp xếp theo thời gian giảm dần, giới hạn 200 bản ghi.
     *
     * @return Danh sách các đối tượng [ScanHistory].
     */
    suspend fun getAllHistory(): List<ScanHistory>

    /**
     * Xóa một bản ghi lịch sử quét khỏi Firestore.
     *
     * @param id Mã định danh Document ID của bản ghi cần xóa.
     * @return `true` nếu xóa thành công, ngược lại `false`.
     */
    suspend fun deleteHistory(id: String): Boolean

    /**
     * Đọc cấu hình ứng dụng từ Firestore. Trả về giá trị mặc định nếu chưa tồn tại.
     *
     * @return Đối tượng [AppConfig] chứa cấu hình hiện tại.
     */
    suspend fun getAppConfig(): AppConfig

    /**
     * Ghi cấu hình ứng dụng lên Firestore.
     *
     * @param config Đối tượng [AppConfig] chứa cấu hình cần cập nhật.
     * @return `true` nếu ghi thành công, ngược lại `false`.
     */
    suspend fun updateAppConfig(config: AppConfig): Boolean
}
