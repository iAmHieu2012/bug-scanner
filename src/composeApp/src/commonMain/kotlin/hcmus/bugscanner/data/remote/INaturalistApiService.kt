package hcmus.bugscanner.data.remote

import hcmus.bugscanner.domain.model.INaturalistResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

/**
 * Service gọi API iNaturalist sử dụng Ktor.
 * Dùng để tra cứu chéo hoặc tìm kiếm thông tin khoa học chuẩn xác của côn trùng.
 *
 * @param client Đối tượng [HttpClient] được cung cấp bởi hệ thống Dependency Injection.
 */
class INaturalistApiService(private val client: HttpClient) {

    /**
     * Tìm kiếm một loài côn trùng trên hệ thống iNaturalist.
     * API đã được tinh chỉnh để ưu tiên kết quả thuộc lớp Côn Trùng và ngôn ngữ Tiếng Việt.
     *
     * @param query Từ khóa tìm kiếm (tên khoa học hoặc tên phổ thông).
     * @return Danh sách các kết quả phân loại học khớp với từ khóa.
     */
    suspend fun searchInsects(query: String): INaturalistResponse {
        return client.get("https://api.inaturalist.org/v1/taxa") {
            url {
                parameters.append("q", query)
                parameters.append("taxon_id", "47158") // BỘ LỌC QUAN TRỌNG: 47158 là ID của lớp Côn Trùng (Insecta)
                parameters.append("locale", "vi")      // Ưu tiên trả về tên/mô tả bằng Tiếng Việt
                parameters.append("per_page", "15")    // Lấy 15 kết quả
            }
            headers {
                append("User-Agent", "BugScannerApp/1.0 (hcmus.bugscanner)")
            }
        }.body()
    }
}