package hcmus.bugscanner.domain.repository

import hcmus.bugscanner.domain.model.ScanHistory

/**
 * Interface định nghĩa các thao tác trừu tượng liên quan đến việc quản lý Lịch sử quét.
 */
interface HistoryRepository {
    /**
     * Lưu một bản ghi lịch sử nhận diện mới vào hệ thống.
     *
     * @param history Đối tượng chứa thông tin lịch sử cần lưu.
     * @return `true` nếu lưu thành công, ngược lại `false`.
     */
    suspend fun saveHistory(history: ScanHistory): Boolean

    /**
     * Truy xuất toàn bộ danh sách lịch sử quét của một người dùng cụ thể.
     *
     * @param userId Mã định danh của người dùng (Firebase UID).
     * @return Danh sách các bản ghi [ScanHistory] đã được sắp xếp theo thời gian.
     */
    suspend fun getUserHistory(userId: String): List<ScanHistory>

    /**
     * Tải mảng byte của hình ảnh lên hệ thống lưu trữ Cloud.
     *
     * @param userId Mã định danh người dùng thực hiện tải ảnh.
     * @param imageBytes Dữ liệu thô của bức ảnh cần tải lên.
     * @return Đường dẫn URL tĩnh của ảnh sau khi tải lên thành công, hoặc `null` nếu thất bại.
     */
    suspend fun uploadImage(userId: String, imageBytes: ByteArray): String?
}