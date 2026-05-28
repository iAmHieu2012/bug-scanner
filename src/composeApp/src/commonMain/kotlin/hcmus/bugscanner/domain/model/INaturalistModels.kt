package hcmus.bugscanner.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO mapping cấu trúc dữ liệu trả về từ API tìm kiếm (v1/taxa) của iNaturalist.
 *
 * @property totalResults Tổng số kết quả tìm thấy trên hệ thống.
 * @property results Danh sách các đối tượng phân loại học khớp với từ khóa tìm kiếm.
 */
@Serializable
data class INaturalistResponse(
    @SerialName("total_results")
    val totalResults: Int = 0,
    val results: List<INaturalistTaxon> = emptyList()
)

/**
 * DTO đại diện cho một đơn vị phân loại học (Taxon) của iNaturalist.
 *
 * @property id Mã định danh sinh học trên hệ thống iNaturalist.
 * @property name Tên khoa học của sinh vật.
 * @property preferredCommonName Tên thường gọi được ưu tiên theo khu vực (locale) đã truyền vào API.
 * @property englishCommonName Tên thường gọi bằng tiếng Anh.
 * @property defaultPhoto Hình ảnh đại diện mặc định của loài này trên iNaturalist.
 * @property rank Cấp bậc phân loại (Ví dụ: "species" - loài, "genus" - chi, "family" - họ).
 * @property observationsCount Tổng số lượt quan sát ngoài đời thực được cộng đồng ghi nhận.
 * @property wikipediaUrl Link bài viết Wikipedia gắn liền với sinh vật này (thường chứa tiền tố ngôn ngữ locale).
 */
@Serializable
data class INaturalistTaxon(
    val id: Long,
    val name: String = "",

    @SerialName("preferred_common_name")
    val preferredCommonName: String? = null,

    @SerialName("english_common_name")
    val englishCommonName: String? = null,

    @SerialName("default_photo")
    val defaultPhoto: INaturalistPhoto? = null,

    val rank: String? = null,

    @SerialName("observations_count")
    val observationsCount: Int = 0,

    @SerialName("wikipedia_url")
    val wikipediaUrl: String? = null
)

/**
 * DTO chứa thông tin về hình ảnh của iNaturalist.
 *
 * @property mediumUrl Đường dẫn ảnh kích thước trung bình.
 * @property squareUrl Đường dẫn ảnh cắt vuông (thumbnail).
 */
@Serializable
data class INaturalistPhoto(
    @SerialName("medium_url")
    val mediumUrl: String? = null,

    @SerialName("square_url")
    val squareUrl: String? = null
)