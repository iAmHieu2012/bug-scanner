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
 * @property imageUrls Danh sách ảnh minh họa bổ sung.
 * @property identification Đặc điểm nhận dạng ngoại hình.
 * @property danger Mức độ nguy hiểm hoặc tác hại đối với con người/nông nghiệp.
 * @property harmfulnessLevel Mức gây hại chuẩn hóa để UI hiển thị badge nhất quán.
 * @property treatment Biện pháp xử lý, phòng ngừa hoặc sơ cứu y tế khi tiếp xúc.
 * @property affectedCrops Cây trồng thường gặp hoặc dễ bị ảnh hưởng.
 * @property hostPlants Cây ký chủ được nguồn tham khảo ghi nhận.
 * @property damageSymptoms Dấu hiệu gây hại người dùng có thể quan sát.
 * @property identificationTips Đặc điểm nhận biết bằng ngôn ngữ đơn giản.
 * @property whereToFind Vị trí thường thấy trên cây hoặc ngoài ruộng/vườn.
 * @property season Thời điểm/điều kiện thường xuất hiện nếu có dữ liệu.
 * @property safeActions Việc nên làm an toàn, không thay thế tư vấn chuyên gia.
 * @property ipmNotes Ghi chú quản lý dịch hại tổng hợp từ nguồn đáng tin.
 * @property sourceRefs Danh sách nguồn tham khảo dùng để xây dựng hồ sơ.
 * @property searchTokens Từ khóa tìm kiếm bổ sung đã chuẩn hóa/curate.
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
    val imageUrls: List<String>? = null,
    val identification: String = "",
    val danger: String = "",
    val harmfulnessLevel: String = "crop_pest",
    val treatment: String = "",
    val affectedCrops: List<String>? = null,
    val hostPlants: List<String>? = null,
    val damageSymptoms: List<String>? = null,
    val identificationTips: List<String>? = null,
    val whereToFind: List<String>? = null,
    val season: String = "",
    val safeActions: List<String>? = null,
    val ipmNotes: List<String>? = null,
    val sourceRefs: List<String>? = null,
    val searchTokens: List<String>? = null,
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
        imageUrls = this.imageUrls ?: emptyList(),
        identification = this.identification,
        danger = this.danger,
        harmfulnessLevel = this.harmfulnessLevel,
        treatment = this.treatment,
        affectedCrops = this.affectedCrops ?: emptyList(),
        hostPlants = this.hostPlants ?: emptyList(),
        damageSymptoms = this.damageSymptoms ?: emptyList(),
        identificationTips = this.identificationTips ?: emptyList(),
        whereToFind = this.whereToFind ?: emptyList(),
        season = this.season,
        safeActions = this.safeActions ?: emptyList(),
        ipmNotes = this.ipmNotes ?: emptyList(),
        sourceRefs = this.sourceRefs ?: emptyList(),
        searchTokens = this.searchTokens ?: emptyList(),
        wikiUrl = this.wikiUrl
    )
}
