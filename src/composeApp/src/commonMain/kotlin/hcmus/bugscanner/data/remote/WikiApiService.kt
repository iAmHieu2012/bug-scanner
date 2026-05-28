package hcmus.bugscanner.data.remote

import hcmus.bugscanner.domain.model.WikiResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Service gọi API Wikipedia sử dụng Ktor (hỗ trợ Đa nền tảng).
 * Đã lược bỏ các hàm không còn sử dụng.
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

    suspend fun getSummaryByTitle(title: String, lang: String = "en"): String? {
        return try {
            val response: WikiResponse = client.get("https://$lang.wikipedia.org/w/api.php") {
                url {
                    parameters.append("action", "query")
                    parameters.append("format", "json")
                    parameters.append("prop", "extracts")
                    parameters.append("exintro", "true")
                    parameters.append("explaintext", "true")
                    parameters.append("redirects", "1")
                    parameters.append("titles", title)
                    parameters.append("origin", "*")
                }
            }.body()

            // Trích xuất đoạn văn bản đầu tiên tìm được
            val extractText = response.query?.pages?.values?.firstOrNull()?.extract

            // Nếu có chữ và ngôn ngữ gốc không phải Tiếng Việt thì tiến hành dịch
            if (!extractText.isNullOrBlank() && lang != "vi") {
                translateToVietnamese(extractText, sourceLang = lang)
            } else {
                extractText
            }
        } catch (e: Exception) {
            println("Lỗi kéo Wiki chi tiết: ${e.message}")
            null
        }
    }

    /**
     * Dịch văn bản thông qua endpoint gtx của Google Translate.
     * Miễn phí, tốc độ cao và không bị giới hạn RPM khắt khe như Gemini.
     */
    private suspend fun translateToVietnamese(text: String, sourceLang: String = "en"): String {
        return try {
            val responseText: String = client.get("https://translate.googleapis.com/translate_a/single") {
                url {
                    parameters.append("client", "gtx")
                    parameters.append("sl", sourceLang)
                    parameters.append("tl", "vi")
                    parameters.append("dt", "t")
                    parameters.append("q", text)
                }
            }.bodyAsText()

            val jsonArray = Json.parseToJsonElement(responseText).jsonArray
            val translatedSegments = jsonArray[0].jsonArray

            var fullTranslation = ""
            for (i in 0 until translatedSegments.size) {
                fullTranslation += translatedSegments[i].jsonArray[0].jsonPrimitive.content
            }

            fullTranslation.trim()
        } catch (e: Exception) {
            println("Lỗi khi dịch văn bản: ${e.message}")
            // Trả về text gốc nếu quá trình dịch gặp lỗi mạng
            text
        }
    }
}