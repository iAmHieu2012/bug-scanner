package hcmus.bugscanner.data.remote

import hcmus.bugscanner.BuildConfig
import hcmus.bugscanner.core.config.AppConfigProvider
import hcmus.bugscanner.domain.model.GeminiRequest
import hcmus.bugscanner.domain.model.GeminiResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Service chuyên biệt để giao tiếp với Google Gemini AI.
 * Đóng vai trò là cầu nối mạng để gửi câu hỏi và nhận câu trả lời từ AI.
 * Tên mô hình được đọc động từ cấu hình hệ thống (Firestore `app_config`) thay vì cố định trong mã nguồn.
 *
 * @property client Đối tượng [HttpClient] được cung cấp bởi hệ thống Dependency Injection (Koin).
 * @property appConfigProvider Bộ cung cấp cấu hình ứng dụng để đọc tên mô hình AI.
 */
class GeminiApiService(
    private val client: HttpClient,
    private val appConfigProvider: AppConfigProvider
) {

    private val apiKey = BuildConfig.GEMINI_API_KEY

    /**
     * Gửi một yêu cầu sinh văn bản (Generate Content) đến mô hình Gemini.
     *
     * @param request Khối dữ liệu [GeminiRequest] chứa câu lệnh hệ thống và lịch sử trò chuyện.
     * @return Đối tượng [GeminiResponse] chứa câu trả lời từ AI đã được parse tự động từ JSON.
     */
    suspend fun generateContent(request: GeminiRequest): GeminiResponse {
        val configuredApiKey = ApiKeyPolicy.requireConfigured("Gemini", apiKey)
        val config = appConfigProvider.getConfig()
        return client.post("https://generativelanguage.googleapis.com/v1beta/models/${config.geminiModel}:generateContent") {
            url { parameters.append("key", configuredApiKey) }
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}