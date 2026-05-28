package hcmus.bugscanner.domain.model

import kotlinx.serialization.Serializable

/**
 * DTO mapping cấu trúc dữ liệu phản hồi (Response) cấp cao nhất từ API của Wikipedia.
 *
 * @property query Đối tượng chứa kết quả của các thao tác truy vấn (action=query).
 */
@Serializable
data class WikiResponse(
    val query: WikiQuery? = null
)

/**
 * DTO chứa danh sách các trang (Pages) được trả về từ truy vấn.
 *
 * @property pages Một Map với Key là Page ID dạng chuỗi và Value là dữ liệu chi tiết của trang đó.
 */
@Serializable
data class WikiQuery(
    val pages: Map<String, WikiPage>? = null
)

/**
 * DTO đại diện cho một trang nội dung trên Wikipedia.
 *
 * @property title Tiêu đề chính thức của trang.
 * @property extract Đoạn văn bản trích xuất (tóm tắt) nội dung của trang (yêu cầu prop=extracts).
 * @property thumbnail Đối tượng chứa hình ảnh thu nhỏ đại diện của trang.
 * @property index Vị trí thứ tự của trang trong danh sách kết quả.
 */
@Serializable
data class WikiPage(
    val title: String,
    val extract: String? = null,
    val thumbnail: WikiThumbnail? = null,
    val index: Int = 0
)

/**
 * DTO chứa thông tin hình ảnh thu nhỏ của trang Wiki.
 *
 * @property source Đường dẫn URL trực tiếp đến file hình ảnh.
 */
@Serializable
data class WikiThumbnail(
    val source: String
)