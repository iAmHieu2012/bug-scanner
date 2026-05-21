package hcmus.bugscanner.data.remote

import hcmus.bugscanner.domain.model.WikiResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Service gọi API Wikipedia sử dụng Ktor (hỗ trợ Đa nền tảng).
 */
class WikiApiService {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    suspend fun searchInsects(query: String): WikiResponse {
        return client.get("https://vi.wikipedia.org/w/api.php") {
            url {
                parameters.append("action", "query")
                parameters.append("format", "json")
                parameters.append("prop", "extracts|pageimages")
                parameters.append("exintro", "true")
                parameters.append("explaintext", "true")
                parameters.append("piprop", "thumbnail")
                parameters.append("pithumbsize", "300")
                parameters.append("generator", "search")
                parameters.append("gsrlimit", "10")
                parameters.append("gsrsearch", query)
                parameters.append("origin", "*")
            }
            headers {
                append("User-Agent", "BugScannerApp/1.0 (hcmus.bugscanner)")
            }
        }.body()
    }
}