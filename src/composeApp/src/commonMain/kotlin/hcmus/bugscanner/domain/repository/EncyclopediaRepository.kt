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
     * @param harmfulnessLevel Lọc theo độ nguy hiểm (Tất cả, Có hại, Ít hại, Có lợi)
     * @param yoloOnly Chỉ lấy các loài có thể nhận diện qua YOLO
     * @return Danh sách các đối tượng [BugInfo].
     */
    suspend fun getExploreInsects(searchQuery: String = "", limit: Int = 30, harmfulnessLevel: String? = null, yoloOnly: Boolean = false): List<BugInfo>

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

    /**
     * Tải trước toàn bộ Database về Local Cache để dùng Offline.
     */
    suspend fun prefetchDatabase()

    /**
     * Lưu trữ thông tin chi tiết của một loài côn trùng mới lên cơ sở dữ liệu.
     * Hỗ trợ cơ chế Crowdsourcing: Dữ liệu do AI sinh ra sẽ được lưu lại để tối ưu hóa truy vấn cho các lần sau.
     *
     * @param bug Đối tượng [BugInfo] chứa thông tin sinh vật cần lưu.
     * @return `true` nếu lưu thành công, ngược lại `false`.
     */
    suspend fun saveBugToFirebase(bug: BugInfo): Boolean

    /**
     * Xóa một bài viết côn trùng khỏi Bách khoa toàn thư.
     *
     * @param docId Document ID (Tên khoa học đã chuẩn hóa) của mục cần xóa.
     * @return `true` nếu xóa thành công, ngược lại `false`.
     */
    suspend fun deleteBugEntry(docId: String): Boolean
}