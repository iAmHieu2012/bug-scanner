package hcmus.bugscanner.data.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import hcmus.bugscanner.domain.model.ScanHistory
import hcmus.bugscanner.domain.repository.HistoryRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Lớp thực thi (Implementation) quản lý luồng dữ liệu lịch sử quét của người dùng.
 * - Đồng bộ dữ liệu văn bản (tên, thời gian) lên Firebase Firestore.
 * - Upload hình ảnh độc lập lên dịch vụ ImgBB thông qua Ktor HTTP Client.
 */
class HistoryRepositoryImpl : HistoryRepository {
    private val db = Firebase.firestore
    private val historyCollection = db.collection("scan_history")

    // Khởi tạo Ktor Client đã có sẵn trong build.gradle.kts
    private val httpClient = HttpClient()

    /**
     * Ghi một bản ghi lịch sử mới vào Firestore.
     * Tự động sinh Document ID thông qua thuộc tính `.document`.
     *
     * @param history Khối dữ liệu lịch sử cần lưu.
     * @return `true` nếu ghi thành công, ngược lại `false`.
     */
    override suspend fun saveHistory(history: ScanHistory): Boolean {
        return try {
            val docRef = historyCollection.document
            val historyWithId = history.copy(id = docRef.id)
            docRef.set(historyWithId)
            true
        } catch (e: Exception) {
            println("Lỗi saveHistory: ${e.message}")
            false
        }
    }

    /**
     * Lấy danh sách lịch sử theo User ID, sắp xếp giảm dần theo thời gian (mới nhất xếp trước).
     *
     * @param userId Mã định danh Firebase UID của người dùng.
     * @return Danh sách [ScanHistory]. Trả về mảng rỗng nếu có lỗi.
     */
    override suspend fun getUserHistory(userId: String): List<ScanHistory> {
        return try {
            val snapshot = historyCollection
                .where { "userId" equalTo userId }
                .orderBy("timestamp", Direction.DESCENDING)
                .get()

            snapshot.documents.map { it.data<ScanHistory>() }
        } catch (e: Exception) {
            println("Lỗi getUserHistory: ${e.message}")
            emptyList()
        }
    }

    /**
     * Tải mảng byte ảnh trực tiếp lên máy chủ ImgBB bằng giao thức Multipart Form.
     *
     * @param userId Mã định danh người dùng.
     * @param imageBytes Dữ liệu hình ảnh thô.
     * @return Chuỗi URL trực tiếp của tấm ảnh (`.jpg`/`.png`), hoặc `null` nếu bị từ chối.
     */
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun uploadImage(userId: String, imageBytes: ByteArray): String? {
        return try {
            val imgbbApiKey = hcmus.bugscanner.BuildConfig.IMGBB_API_KEY

            // Encode mảng byte thành Base64 để truyền qua HTTP Form
            val base64Image = Base64.encode(imageBytes)

            // Bắn Request POST lên API của ImgBB
            val response = httpClient.submitForm(
                url = "https://api.imgbb.com/1/upload",
                formParameters = Parameters.build {
                    append("key", imgbbApiKey)
                    append("image", base64Image)
                }
            )

            // Bóc tách JSON response để lấy URL của tấm ảnh
            if (response.status.isSuccess()) {
                val responseBody = response.bodyAsText()
                val json = Json.parseToJsonElement(responseBody).jsonObject
                val data = json["data"]?.jsonObject
                val url = data?.get("url")?.jsonPrimitive?.content

                url // Trả về link ảnh (.jpg/.png)
            } else {
                println("Lỗi từ chối từ ImgBB. HTTP Status: ${response.status}")
                null
            }
        } catch (e: Exception) {
            println("Lỗi upload ảnh lên ImgBB: ${e.message}")
            null
        }
    }
}