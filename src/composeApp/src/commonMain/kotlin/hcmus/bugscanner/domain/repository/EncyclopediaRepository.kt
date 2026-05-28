package hcmus.bugscanner.domain.repository

import hcmus.bugscanner.domain.model.BugInfo

/**
 * Interface định nghĩa các thao tác giao tiếp với cơ sở dữ liệu Bách khoa toàn thư.
 */
interface EncyclopediaRepository {
    /**
     * Lấy danh sách các loài côn trùng để hiển thị trên màn hình Khám phá.
     *
     * @param searchQuery Từ khóa tìm kiếm. Để trống nếu muốn lấy danh sách mặc định.
     * @param limit Số lượng bản ghi tối đa trả về trong một lần gọi (Pagination).
     * @return Danh sách các đối tượng [BugInfo].
     */
    suspend fun getExploreInsects(searchQuery: String = "", limit: Int = 20): List<BugInfo>

    /**
     * Truy vấn thông tin chi tiết của một loài côn trùng dựa vào tên phổ thông.
     *
     * @param name Tên phổ thông (tên thường gọi) của côn trùng.
     * @return Đối tượng [BugInfo] nếu tìm thấy, ngược lại trả về null.
     */
    suspend fun getBugByName(name: String): BugInfo?

    /**
     * Truy vấn thông tin chi tiết của một loài côn trùng dựa vào tên khoa học.
     *
     * @param scientificName Tên khoa học của loài côn trùng.
     * @return Đối tượng [BugInfo] nếu tìm thấy, ngược lại trả về null.
     */
    suspend fun getBugByScientificName(scientificName: String): BugInfo?
}