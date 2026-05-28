package hcmus.bugscanner.data.remote

import hcmus.bugscanner.BuildConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// --- DTOs ĐƯỢC CHUYỂN TỪ VIEWMODEL SANG ĐÂY ---
@Serializable
data class GeminiRequest(val systemInstruction: Instruction? = null, val contents: List<GeminiContent>)
@Serializable
data class Instruction(val parts: GeminiPart)
@Serializable
data class GeminiContent(val role: String, val parts: List<GeminiPart>)
@Serializable
data class GeminiPart(val text: String)
@Serializable
data class GeminiResponse(val candidates: List<Candidate>? = null)
@Serializable
data class Candidate(val content: GeminiContent)

/**
 * Service chuyên biệt để giao tiếp với Google Gemini AI.
 * Đóng vai trò là cầu nối mạng để gửi câu hỏi và nhận câu trả lời từ AI.
 * Có thể được tái sử dụng ở bất kỳ ViewModel hoặc Service nào khác.
 */
class GeminiApiService {
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    /**
     * Gửi một yêu cầu sinh văn bản (Generate Content) đến mô hình Gemini.
     *
     * @param request Khối dữ liệu chứa câu lệnh hệ thống và nội dung trò chuyện của người dùng.
     * @return Khối dữ liệu JSON đã được parse thành Object chứa câu trả lời từ AI.
     */
    suspend fun generateContent(request: GeminiRequest): GeminiResponse {
        return client.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent") {
            url { parameters.append("key", apiKey) }
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}