package hcmus.bugscanner.data.remote

import hcmus.bugscanner.BuildConfig
import hcmus.bugscanner.domain.model.INaturalistResponse
import hcmus.bugscanner.domain.model.INaturalistTaxon
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

/**
 * Lớp bao bọc (Wrapper) danh sách kết quả trả về từ API Computer Vision của iNaturalist.
 * Cấu trúc này cần thiết để hứng đúng định dạng JSON đặc thù của luồng nhận diện ảnh.
 *
 * @property results Danh sách các kết quả nhận diện hình ảnh.
 */
@Serializable
data class INaturalistCVResponse(
    val results: List<INaturalistCVResult>
)

/**
 * Lớp đại diện cho một kết quả nhận diện đơn lẻ từ API Computer Vision.
 *
 * @property taxon Dữ liệu chi tiết về phân loại học của sinh vật được nhận diện.
 */
@Serializable
data class INaturalistCVResult(
    val taxon: INaturalistTaxon
)

/**
 * Dịch vụ (Service) chịu trách nhiệm giao tiếp với hệ thống API của iNaturalist thông qua Ktor Client.
 * Cung cấp các phương thức để tra cứu thông tin sinh học bằng văn bản và nhận diện bằng hình ảnh.
 *
 * @property client Đối tượng [HttpClient] được cung cấp bởi hệ thống Dependency Injection (Koin).
 */
class INaturalistApiService(private val client: HttpClient) {

    /**
     * Tìm kiếm một loài côn trùng trên hệ thống cơ sở dữ liệu của iNaturalist bằng từ khóa.
     * API đã được tinh chỉnh bộ lọc để ưu tiên trả về các kết quả thuộc lớp Côn Trùng (Insecta) và ngôn ngữ Tiếng Việt.
     *
     * @param query Từ khóa tìm kiếm (Tên khoa học hoặc tên phổ thông).
     * @return [INaturalistResponse] Đối tượng chứa danh sách các sinh vật khớp với từ khóa tìm kiếm.
     * @throws Exception Nếu có lỗi xảy ra trong quá trình gọi mạng hoặc phân tích cú pháp JSON.
     */
    suspend fun searchInsects(query: String): INaturalistResponse {
        return client.get("https://api.inaturalist.org/v1/taxa") {
            url {
                parameters.append("q", query)
                parameters.append("taxon_id", "47158") // Bộ lọc: ID 47158 tương ứng với lớp Côn Trùng (Insecta)
                parameters.append("locale", "vi")      // Yêu cầu máy chủ ưu tiên trả về ngôn ngữ Tiếng Việt
                parameters.append("per_page", "15")    // Giới hạn số lượng kết quả trả về là 15
            }
            headers {
                append("User-Agent", "BugScannerApp/1.0 (hcmus.bugscanner)")
            }
        }.body()
    }

    /**
     * Phân tích và định danh loài sinh vật dựa trên hình ảnh sử dụng mô hình Computer Vision của iNaturalist.
     * Dữ liệu hình ảnh được đóng gói và gửi đi dưới định dạng Multipart Form-Data.
     *
     * @param imageBytes Mảng byte (ByteArray) của bức ảnh cần nhận diện (thường được nén từ thiết bị chụp).
     * @return [INaturalistResponse] Danh sách các sinh vật có khả năng khớp cao nhất với hình ảnh.
     * Dữ liệu đã được bóc tách và định dạng lại để đồng nhất với cấu trúc của hàm [searchInsects].
     * @throws Exception Nếu hình ảnh không hợp lệ, dung lượng quá lớn, hoặc máy chủ từ chối yêu cầu.
     */
    suspend fun identifyImageByVision(imageBytes: ByteArray): INaturalistResponse {
        val cvResponse: INaturalistCVResponse = client.submitFormWithBinaryData(
            url = "https://api.inaturalist.org/v1/computervision/score_image",
            formData = formData {
                append("image", imageBytes, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    // Chỉ định filename giả lập để máy chủ nhận diện đây là một tập tin vật lý
                    append(HttpHeaders.ContentDisposition, "filename=\"fallback_scan.jpg\"")
                })
            }
        ) {
            headers {
                append("User-Agent", "BugScannerApp/1.0 (hcmus.bugscanner)")
                val myToken = BuildConfig.INATURALIST_API_TOKEN
                if (myToken.isNotEmpty()) {
                    append(HttpHeaders.Authorization, "Bearer $myToken")
                }
            }
        }.body()

        // Trích xuất thuộc tính taxon từ kết quả Computer Vision và đóng gói lại thành INaturalistResponse.
        // Giúp tầng Logic (ViewModel/UseCase) không cần viết thêm mã xử lý cho một kiểu dữ liệu mới.
        return INaturalistResponse(
            results = cvResponse.results.map { it.taxon }
        )
    }
}