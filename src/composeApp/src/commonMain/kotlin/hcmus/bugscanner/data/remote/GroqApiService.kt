package hcmus.bugscanner.data.remote

import hcmus.bugscanner.BuildConfig
import hcmus.bugscanner.core.config.AppConfigProvider
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
 * Dịch vụ giao tiếp với hệ thống Groq AI (Llama).
 * Đảm nhận nhiệm vụ dịch thuật tự nhiên và phát sinh dữ liệu chuyên ngành nông nghiệp.
 * Tên mô hình và lệnh hệ thống (System Prompt) được đọc động từ cấu hình Firestore `app_config`.
 *
 * @property client HTTP Client cấu hình sẵn của Ktor.
 * @property appConfigProvider Bộ cung cấp cấu hình ứng dụng để đọc tên mô hình và prompt.
 */
class GroqApiService(
    private val client: HttpClient,
    private val appConfigProvider: AppConfigProvider
) {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    /**
     * Tiền xử lý kết quả trả về từ các mô hình Reasoning (vd: DeepSeek-R1, Qwen-Reasoning)
     * bằng cách loại bỏ toàn bộ phần nội dung nằm trong cặp thẻ <think>...</think>.
     *
     * @param text Chuỗi kết quả thô từ AI.
     * @return Chuỗi văn bản đã được làm sạch thẻ suy luận.
     */
    private fun cleanReasoningOutput(text: String): String {
        var cleanText = text
        if (cleanText.contains("<think>")) {
            cleanText = cleanText.replace(Regex("<think>[\\s\\S]*?</think>"), "")
        }
        return cleanText.trim()
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
        val config = appConfigProvider.getConfig()

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
            model = config.groqModel,
            messages = listOf(
                GroqMessage(role = "system", content = config.groqSystemPrompt),
                GroqMessage(role = "user", content = prompt)
            ),
            responseFormat = GroqResponseFormat(type = "json_object"),
            temperature = 0.2
        )

        return try {
            val response: GroqResponse = client.post("https://api.groq.com/openai/v1/chat/completions") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${BuildConfig.GROQ_API_KEY}")
                    append(HttpHeaders.ContentType, "application/json")
                }
                setBody(payload)
            }.body()

            var jsonString = response.choices.firstOrNull()?.message?.content ?: "{}"
            jsonString = cleanReasoningOutput(jsonString)
            val startIndex = jsonString.indexOf('{')
            val endIndex = jsonString.lastIndexOf('}')
            if (startIndex != -1 && endIndex != -1 && endIndex >= startIndex) {
                jsonString = jsonString.substring(startIndex, endIndex + 1)
            }
            
            println("[GROQ] Hoàn thành phân tích JSON cho '$scientificName'!")
            jsonParser.decodeFromString<AiBugData>(jsonString)
        } catch (e: Exception) {
            println("❌ [GROQ] Lỗi trích xuất dữ liệu: ${e.message}")
            AiBugData(nameVi = scientificName, description = "Lỗi trích xuất dữ liệu từ AI.")
        }
    }

    /**
     * Dịch tên côn trùng từ Tiếng Việt sang Tên Tiếng Anh thông dụng (English common name) sử dụng Groq AI.
     * Tránh việc AI trả về tên khoa học quá chi tiết làm hẹp phạm vi tìm kiếm.
     *
     * @param vietnameseName Tên gọi bằng tiếng Việt.
     * @return Tên gọi bằng tiếng Anh tương ứng.
     */
    suspend fun translateToEnglishName(vietnameseName: String): String {
        val config = appConfigProvider.getConfig()

        val payload = GroqRequest(
            model = config.groqModel,
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
            val response: GroqResponse = client.post("https://api.groq.com/openai/v1/chat/completions") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${BuildConfig.GROQ_API_KEY}")
                    append(HttpHeaders.ContentType, "application/json")
                }
                setBody(payload)
            }.body()

            val rawResult = response.choices.firstOrNull()?.message?.content ?: ""
            val result = cleanReasoningOutput(rawResult).removeSurrounding("\"")
            val finalTranslated = if (result.contains("không biết", ignoreCase = true) || result.contains("sorry", ignoreCase = true)) "" else result
            
            println("[GROQ] Đã dịch '$vietnameseName' -> '$finalTranslated' bằng model ${config.groqModel}")
            finalTranslated
        } catch (e: Exception) {
            println("[GROQ] Lỗi dịch tên khoa học: ${e.message}")
            ""
        }
    }
}