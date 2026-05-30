package hcmus.bugscanner.domain.model

/**
 * Lớp dữ liệu chứa thông tin chi tiết của một loài côn trùng để hiển thị trên giao diện người dùng (UI).
 * Domain Model này yêu cầu truyền đầy đủ dữ liệu (không có giá trị mặc định ngoại trừ wikiUrl)
 * để đảm bảo an toàn, tránh lỗi hiển thị khi thiếu dữ liệu.
 *
 * @property id Mã định danh duy nhất của bản ghi.
 * @property name Tên phổ thông (tên thường gọi bằng tiếng Việt) của côn trùng.
 * @property englishName Tên phổ thông bằng tiếng Anh.
 * @property scientificName Tên khoa học (Danh pháp hai phần).
 * @property description Đoạn văn bản mô tả tổng quan về loài côn trùng.
 * @property imageUrl Đường dẫn URL tĩnh chứa hình ảnh minh họa.
 * @property identification Đặc điểm nhận dạng ngoại hình.
 * @property danger Mức độ nguy hiểm hoặc tác hại đối với con người/nông nghiệp.
 * @property treatment Biện pháp xử lý, phòng ngừa hoặc sơ cứu y tế khi tiếp xúc.
 * @property wikiUrl Đường dẫn đến bài viết Wikipedia (nếu có, mặc định là chuỗi rỗng).
 */
data class BugInfo(
    val id: String,
    val name: String,
    val englishName: String,
    val scientificName: String,
    val description: String,
    val imageUrl: String,
    val identification: String,
    val danger: String,
    val treatment: String,
    val wikiUrl: String = ""
) {
    /**
     * Hàm tiện ích giúp UI khởi tạo một đối tượng rỗng (Empty State)
     * trong lúc chờ dữ liệu tải về từ API/Firebase.
     */
    companion object {
        fun empty() = BugInfo(
            id = "",
            name = "",
            englishName = "",
            scientificName = "",
            description = "",
            imageUrl = "",
            identification = "",
            danger = "",
            treatment = "",
            wikiUrl = ""
        )
    }
}