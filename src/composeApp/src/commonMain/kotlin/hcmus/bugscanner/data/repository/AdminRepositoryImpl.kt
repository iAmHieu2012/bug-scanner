package hcmus.bugscanner.data.repository

import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FirebaseFirestore
import hcmus.bugscanner.domain.model.AppConfig
import hcmus.bugscanner.domain.model.ScanHistory
import hcmus.bugscanner.domain.model.UserProfile
import hcmus.bugscanner.domain.repository.AdminRepository

/**
 * Lớp thực thi (Implementation) quản lý các thao tác quản trị hệ thống.
 * Sử dụng Firebase Firestore kết hợp thư viện KMP GitLive để đồng bộ đa nền tảng.
 *
 * @param db Đối tượng Firestore dùng để kết nối và truy vấn dữ liệu.
 */
class AdminRepositoryImpl(
    private val db: FirebaseFirestore
) : AdminRepository {
    private val adminsCollection = db.collection("admins")
    private val usersCollection = db.collection("users")
    private val historyCollection = db.collection("scan_history")
    private val appConfigCollection = db.collection("app_config")

    /**
     * Kiểm tra quyền Admin bằng cách tra cứu Document ID trong collection `admins`.
     *
     * @param uid Mã định danh Firebase Auth UID.
     * @return `true` nếu document tồn tại (người dùng là Admin), ngược lại `false`.
     */
    override suspend fun isAdmin(uid: String): Boolean {
        return try {
            val doc = adminsCollection.document(uid).get()
            doc.exists
        } catch (e: Exception) {
            println("Lỗi kiểm tra quyền Admin: ${e.message}")
            false
        }
    }

    /**
     * Kiểm tra xem người dùng có bị khóa tài khoản hay không.
     * Tra cứu trạng thái từ Firestore collection `users`.
     *
     * @param uid Mã định danh Firebase Auth UID.
     * @return `true` nếu người dùng bị khóa, ngược lại `false`.
     */
    override suspend fun isBanned(uid: String): Boolean {
        return try {
            val doc = usersCollection.document(uid).get()
            if (doc.exists) {
                val profile = doc.data<UserProfile>()
                profile.isBanned
            } else {
                false
            }
        } catch (e: Exception) {
            println("Lỗi kiểm tra trạng thái khóa: ${e.message}")
            false
        }
    }

    /**
     * Lấy hồ sơ của một người dùng cụ thể từ Firestore.
     *
     * @param uid Mã định danh Firebase Auth UID của người dùng.
     * @return Đối tượng [UserProfile] nếu tìm thấy, ngược lại trả về null.
     */
    override suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            val doc = usersCollection.document(uid).get()
            if (doc.exists) doc.data<UserProfile>() else null
        } catch (e: Exception) {
            println("Lỗi lấy thông tin user profile: ${e.message}")
            null
        }
    }

    /**
     * Lưu hoặc cập nhật hồ sơ người dùng lên Firestore.
     * Sử dụng UID làm Document ID để đảm bảo tính duy nhất.
     *
     * @param profile Đối tượng [UserProfile] chứa thông tin cần lưu.
     */
    override suspend fun saveUserProfile(profile: UserProfile) {
        try {
            val data = mutableMapOf<String, Any>(
                "uid" to profile.uid,
                "email" to profile.email,
                "isAnonymous" to profile.isAnonymous,
                "lastLoginAt" to profile.lastLoginAt,
                "isBanned" to profile.isBanned
            )
            if (profile.displayName.isNotBlank()) {
                data["displayName"] = profile.displayName
            }
            usersCollection.document(profile.uid).set(data, merge = true)
        } catch (e: Exception) {
            println("Lỗi lưu hồ sơ người dùng: ${e.message}")
        }
    }

    /**
     * Khóa hoặc mở khóa tài khoản người dùng trên Firestore.
     * Cập nhật trường `isBanned` trong collection `users`.
     *
     * @param uid Mã định danh Firebase Auth UID.
     * @param isBanned `true` để khóa tài khoản, `false` để mở khóa.
     * @return `true` nếu cập nhật thành công, ngược lại `false`.
     */
    @Suppress("DEPRECATION")
    override suspend fun banUser(uid: String, isBanned: Boolean): Boolean {
        return try {
            usersCollection.document(uid).update("isBanned" to isBanned)
            true
        } catch (e: Exception) {
            println("Lỗi khóa/mở khóa người dùng: ${e.message}")
            false
        }
    }

    /**
     * Truy vấn toàn bộ danh sách người dùng từ Firestore collection `users`.
     *
     * @return Danh sách [UserProfile]. Trả về mảng rỗng nếu lỗi.
     */
    override suspend fun getAllUsers(): List<UserProfile> {
        return try {
            val snapshot = usersCollection.get()
            snapshot.documents.map { it.data<UserProfile>() }
        } catch (e: Exception) {
            println("Lỗi tải danh sách người dùng: ${e.message}")
            emptyList()
        }
    }

    /**
     * Truy vấn toàn bộ lịch sử quét, sắp xếp theo trường `timestamp` giảm dần, giới hạn 200 bản ghi.
     *
     * @return Danh sách [ScanHistory]. Trả về mảng rỗng nếu lỗi.
     */
    override suspend fun getAllHistory(): List<ScanHistory> {
        return try {
            val snapshot = historyCollection
                .orderBy("timestamp", Direction.DESCENDING)
                .limit(200)
                .get()
            snapshot.documents.map { it.data<ScanHistory>() }
        } catch (e: Exception) {
            println("Lỗi tải lịch sử quét: ${e.message}")
            emptyList()
        }
    }

    /**
     * Xóa một bản ghi lịch sử quét dựa trên Document ID.
     *
     * @param id Document ID của bản ghi cần xóa.
     * @return `true` nếu xóa thành công, ngược lại `false`.
     */
    override suspend fun deleteHistory(id: String): Boolean {
        return try {
            historyCollection.document(id).delete()
            true
        } catch (e: Exception) {
            println("Lỗi xóa lịch sử quét: ${e.message}")
            false
        }
    }

    /**
     * Đọc cấu hình ứng dụng từ document `current` trong collection `app_config`.
     * Trả về giá trị mặc định [AppConfig] nếu document chưa tồn tại.
     *
     * @return Đối tượng [AppConfig] chứa cấu hình hiện tại.
     */
    override suspend fun getAppConfig(): AppConfig {
        return try {
            val doc = appConfigCollection.document("current").get()
            if (doc.exists) {
                doc.data<AppConfig>()
            } else {
                AppConfig()
            }
        } catch (e: Exception) {
            println("Lỗi đọc cấu hình ứng dụng: ${e.message}")
            AppConfig()
        }
    }

    /**
     * Ghi cấu hình ứng dụng lên document `current` trong collection `app_config`.
     *
     * @param config Đối tượng [AppConfig] chứa cấu hình cần cập nhật.
     * @return `true` nếu ghi thành công, ngược lại `false`.
     */
    override suspend fun updateAppConfig(config: AppConfig): Boolean {
        return try {
            val data = mapOf(
                "geminiModel" to config.geminiModel,
                "geminiSystemPrompt" to config.geminiSystemPrompt,
                "groqModel" to config.groqModel,
                "groqSystemPrompt" to config.groqSystemPrompt
            )
            appConfigCollection.document("current").set(data)
            true
        } catch (e: Exception) {
            println("Lỗi cập nhật cấu hình ứng dụng: ${e.message}")
            false
        }
    }
}
