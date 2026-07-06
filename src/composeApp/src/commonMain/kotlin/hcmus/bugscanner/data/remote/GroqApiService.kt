package hcmus.bugscanner.data.remote

import hcmus.bugscanner.BuildConfig
import hcmus.bugscanner.domain.model.AiBugData
import hcmus.bugscanner.domain.model.GroqRequest
import hcmus.bugscanner.domain.model.GroqMessage
import hcmus.bugscanner.domain.model.GroqResponse
import hcmus.bugscanner.domain.model.GroqResponseFormat
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

/**
 * Dịch vụ giao tiếp với hệ thống Groq AI (Llama 3).
 * Đảm nhận nhiệm vụ dịch thuật tự nhiên và phát sinh dữ liệu chuyên ngành nông nghiệp.
 *
 * @property client HTTP Client cấu hình sẵn của Ktor.
 */
class GroqApiService(private val client: HttpClient) {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    private suspend fun postChatCompletion(payload: GroqRequest): GroqResponse {
        val configuredApiKey = ApiKeyPolicy.requireConfigured("Groq", BuildConfig.GROQ_API_KEY)
        return client.post("https://api.groq.com/openai/v1/chat/completions") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $configuredApiKey")
                append(HttpHeaders.ContentType, "application/json")
            }
            setBody(payload)
        }.body()
    }

    /**
     * Sinh nội dung chi tiết bằng tiếng Việt dựa trên danh pháp khoa học.
     * Ép buộc AI trả về định dạng JSON nghiêm ngặt để Parse an toàn vào hệ thống.
     *
     * @param scientificName Tên khoa học của sinh vật.
     * @param englishName Tên tiếng Anh (để AI hiểu rõ ngữ cảnh hơn).
     * @return [AiBugData] Dữ liệu JSON đã được giải mã.
     */
    suspend fun generateBugInfo(scientificName: String, englishName: String): AiBugData {
        val prompt = """
            Cung cấp thông tin sinh học và nông nghiệp bằng tiếng Việt cho loài côn trùng có tên khoa học là "$scientificName" (Tên tiếng Anh: "$englishName").
            
            BẮT BUỘC TRẢ VỀ CHUẨN JSON VỚI 5 KEY NÀY, KHÔNG ĐƯỢC CHỨA BẤT KỲ TEXT NÀO KHÁC:
            {
                "nameVi": "Tên gọi tiếng Việt phổ biến nhất (hoặc dịch chuẩn sang tiếng Việt).",
                "description": "Mô tả sinh học, tập tính, vòng đời (3-4 câu).",
                "identification": "Đặc điểm nhận dạng hình thái.",
                "danger": "Chọn đúng 1 từ: Nguy hiểm, An toàn, hoặc Theo dõi.",
                "treatment": "Biện pháp xử lý hoặc phòng trừ."
            }
        """.trimIndent()

        val payload = GroqRequest(
            model = "llama-3.3-70b-versatile",
            messages = listOf(
                GroqMessage(role = "system", content = "You are a professional agricultural assistant. You must output ONLY valid JSON without Markdown."),
                GroqMessage(role = "user", content = prompt)
            ),
            responseFormat = GroqResponseFormat(type = "json_object"),
            temperature = 0.2
        )

        return try {
            val response = postChatCompletion(payload)
            val jsonString = response.choices.firstOrNull()?.message?.content ?: "{}"
            jsonParser.decodeFromString<AiBugData>(jsonString)
        } catch (e: Exception) {
            println("Lỗi trích xuất dữ liệu từ Groq AI: ${e.message}")
            AiBugData(nameVi = scientificName, description = "Lỗi trích xuất dữ liệu từ AI.")
        }
    }

    /**
     * Dịch tên côn trùng từ Tiếng Việt sang Tên Tiếng Anh thông dụng (English common name) sử dụng model Llama 3.1 8B Instant.
     * Tránh việc AI trả về tên khoa học quá chi tiết làm hẹp phạm vi tìm kiếm.
     */
    suspend fun translateToEnglishName(vietnameseName: String): String {
        val payload = GroqRequest(
            model = "llama-3.3-70b-versatile",
            messages = listOf(
                GroqMessage(role = "system", content = "You are a specialized biology translator. You translate Vietnamese insect/animal names to their English common name. ONLY output the English name. No explanation, no quotes, no original text."),
                GroqMessage(role = "user", content = "Ong bắp cày"),
                GroqMessage(role = "assistant", content = "Hornet"),
                GroqMessage(role = "user", content = "Bọ xít"),
                GroqMessage(role = "assistant", content = "Stink bug"),
                GroqMessage(role = "user", content = vietnameseName)
            ),
            responseFormat = null,
            temperature = 0.1
        )

        return try {
            val response = postChatCompletion(payload)
            val result = response.choices.firstOrNull()?.message?.content?.trim()?.removeSurrounding("\"") ?: ""
            if (result.contains("không biết", ignoreCase = true) || result.contains("sorry", ignoreCase = true)) "" else result
        } catch (e: Exception) {
            println("Lỗi dịch tên khoa học bằng Groq: ${e.message}")
            ""
        }
    }
}