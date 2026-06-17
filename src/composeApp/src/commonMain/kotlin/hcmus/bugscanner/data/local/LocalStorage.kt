package hcmus.bugscanner.data.local

/**
 * Lớp đa nền tảng (expect/actual) cung cấp cơ chế lưu trữ dữ liệu dạng khóa-giá trị (key-value)
 * trực tiếp trên bộ nhớ cục bộ của thiết bị. Hỗ trợ tính năng hoạt động ngoại tuyến.
 */
expect class LocalStorage() {
    
    /**
     * Lưu trữ một cặp khóa - giá trị kiểu chuỗi vào bộ nhớ máy.
     *
     * @param key Khóa định danh (không được trùng lặp).
     * @param value Giá trị chuỗi cần lưu.
     */
    fun saveString(key: String, value: String)
    
    /**
     * Truy xuất giá trị chuỗi đã lưu dựa vào khóa.
     *
     * @param key Khóa định danh cần tìm.
     * @return Chuỗi giá trị nếu tồn tại, ngược lại trả về `null`.
     */
    fun getString(key: String): String?
    
    /**
     * Xóa vĩnh viễn một bản ghi khỏi bộ nhớ dựa trên khóa.
     *
     * @param key Khóa định danh của bản ghi cần xóa.
     */
    fun remove(key: String)
    
    /**
     * Lấy toàn bộ danh sách các khóa hiện đang được lưu trong bộ nhớ.
     * Hữu ích cho việc duyệt qua tất cả các bản ghi (ví dụ: đồng bộ hàng loạt).
     *
     * @return Danh sách chuỗi chứa tất cả các khóa.
     */
    fun getAllKeys(): List<String>
}
