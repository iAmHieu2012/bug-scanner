package hcmus.bugscanner.core.config

import hcmus.bugscanner.domain.model.AppConfig
import hcmus.bugscanner.domain.repository.AdminRepository

/**
 * Bộ cung cấp cấu hình ứng dụng với cơ chế cache trên RAM.
 * Tránh gọi Firestore nhiều lần trong cùng một phiên sử dụng, giúp giảm chi phí đọc và tăng tốc độ phản hồi.
 *
 * @property adminRepository Repository dùng để tải cấu hình từ Firestore khi cache trống.
 */
class AppConfigProvider(
    private val adminRepository: AdminRepository
) {
    private var cachedConfig: AppConfig? = null

    /**
     * Trả về cấu hình ứng dụng hiện tại.
     * Nếu đã có bản sao trong RAM thì trả về ngay, ngược lại sẽ tải từ Firestore và lưu vào cache.
     *
     * @return Đối tượng [AppConfig] chứa cấu hình hiện tại.
     */
    suspend fun getConfig(): AppConfig {
        cachedConfig?.let { return it }
        val config = adminRepository.getAppConfig()
        cachedConfig = config
        return config
    }

    /**
     * Xóa bản sao cache trong RAM, buộc lần gọi [getConfig] tiếp theo phải tải lại từ Firestore.
     * Thường được gọi sau khi Admin cập nhật cấu hình.
     */
    fun invalidate() {
        cachedConfig = null
    }
}
