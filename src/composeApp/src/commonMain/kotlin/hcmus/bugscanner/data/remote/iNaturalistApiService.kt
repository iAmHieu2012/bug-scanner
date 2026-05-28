package hcmus.bugscanner.data.remote

import hcmus.bugscanner.domain.model.INaturalistResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Service gọi API iNaturalist sử dụng Ktor.
 */
class INaturalistApiService {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

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