package hcmus.bugscanner.data.remote

import hcmus.bugscanner.BuildConfig
import hcmus.bugscanner.domain.model.GeminiRequest
import hcmus.bugscanner.domain.model.GeminiResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Service chuyên biệt để giao tiếp với Google Gemini AI (Mô hình Gemini 2.5 Flash).
 * Đóng vai trò là cầu nối mạng để gửi câu hỏi và nhận câu trả lời từ AI.
 * Có thể được tái sử dụng ở bất kỳ ViewModel hoặc tính năng AI nào khác trong ứng dụng.
 *
 * @property client Đối tượng [HttpClient] được cung cấp bởi hệ thống Dependency Injection (Koin).
 */
class GeminiApiService(private val client: HttpClient) {

    // Lấy API Key từ biến môi trường/file properties để bảo mật
    private val apiKey = BuildConfig.GEMINI_API_KEY

    /**
     * Gửi một yêu cầu sinh văn bản (Generate Content) đến mô hình Gemini.
     *
     * @param request Khối dữ liệu [GeminiRequest] chứa câu lệnh hệ thống và lịch sử trò chuyện.
     * @return Đối tượng [GeminiResponse] chứa câu trả lời từ AI đã được parse tự động từ JSON.
     */
    suspend fun generateContent(request: GeminiRequest): GeminiResponse {
        return client.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent") {
            url { parameters.append("key", apiKey) }
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}