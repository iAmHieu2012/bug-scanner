package hcmus.bugscanner.data.model

import hcmus.bugscanner.domain.model.BugInfo
import kotlinx.serialization.Serializable

/**
 * Lớp thực thể (Entity) hứng thông tin trực tiếp từ Firebase hoặc API.
 * Sử dụng `@Serializable` và gán giá trị mặc định rỗng cho mọi thuộc tính để hỗ trợ parse JSON an toàn.
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
 * @property wikiUrl Đường dẫn đến bài viết Wikipedia (nếu có).
 */
@Serializable
data class BugInfoEntity(
    val id: String = "",
    val name: String = "",
    val englishName: String = "",
    val scientificName: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val identification: String = "",
    val danger: String = "",
    val treatment: String = "",
    val wikiUrl: String = ""
)

/**
 * Hàm mở rộng (Extension function) để chuyển đổi từ Dữ liệu thô (Entity) sang Dữ liệu chuẩn (Domain).
 * Đảm bảo giao diện người dùng luôn nhận được một đối tượng đầy đủ thông tin.
 *
 * @return Đối tượng [BugInfo] đã được chuẩn hóa để truyền lên UI.
 */
fun BugInfoEntity.toDomain(): BugInfo {
    return BugInfo(
        id = this.id,
        name = this.name,
        englishName = this.englishName,
        scientificName = this.scientificName,
        description = this.description,
        imageUrl = this.imageUrl,
        identification = this.identification,
        danger = this.danger,
        treatment = this.treatment,
        wikiUrl = this.wikiUrl
    )
}