package hcmus.bugscanner.data.repository

import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FirebaseFirestore
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
import hcmus.bugscanner.data.local.LocalStorage
import hcmus.bugscanner.domain.model.OfflineScanHistory
import kotlinx.serialization.encodeToString

/**
 * Lớp thực thi (Implementation) quản lý luồng dữ liệu lịch sử quét của người dùng.
 * - Đồng bộ dữ liệu văn bản (tên, thời gian) lên Firebase Firestore.
 * - Upload hình ảnh độc lập lên dịch vụ ImgBB thông qua Ktor HTTP Client.
 */
class HistoryRepositoryImpl(
    db: FirebaseFirestore,
    private val httpClient: HttpClient
) : HistoryRepository {
    private val historyCollection = db.collection("scan_history")
    private val localStorage = LocalStorage()

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
     * Xóa một bản ghi lịch sử dựa trên ID của nó.
     *
     * @param historyId Mã định danh duy nhất của bản ghi lịch sử.
     * @return `true` nếu xóa thành công, ngược lại `false`.
     */
    override suspend fun deleteHistory(historyId: String): Boolean {
        return try {
            historyCollection.document(historyId).delete()
            true
        } catch (e: Exception) {
            println("Lỗi deleteHistory: ${e.message}")
            false
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
            val base64Image = Base64.encode(imageBytes)

            val response = httpClient.submitForm(
                url = "https://api.imgbb.com/1/upload",
                formParameters = Parameters.build {
                    append("key", imgbbApiKey)
                    append("image", base64Image)
                }
            )

            if (response.status.isSuccess()) {
                val responseBody = response.bodyAsText()
                val json = Json.parseToJsonElement(responseBody).jsonObject
                val data = json["data"]?.jsonObject
                val url = data?.get("url")?.jsonPrimitive?.content

                url
            } else {
                println("Lỗi từ chối từ ImgBB. HTTP Status: ${response.status}")
                null
            }
        } catch (e: Exception) {
            println("Lỗi upload ảnh lên ImgBB: ${e.message}")
            null
        }
    }

    /**
     * Lưu lịch sử ngoại tuyến khi thiết bị mất kết nối mạng.
     *
     * @param userId Mã định danh Firebase UID của người dùng.
     * @param history Khối dữ liệu lịch sử cần lưu trữ.
     * @param imageBytes Dữ liệu hình ảnh dạng byte array.
     */
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun saveOfflineHistory(userId: String, history: ScanHistory, imageBytes: ByteArray) {
        try {
            val base64Image = Base64.encode(imageBytes)
            val offlineId = "offline_${hcmus.bugscanner.core.utils.TimeUtils.getCurrentTimeMillis()}"
            val offlineData = OfflineScanHistory(
                id = offlineId,
                userId = userId,
                history = history,
                imageBase64 = base64Image
            )
            val jsonString = Json.encodeToString(offlineData)
            localStorage.saveString(offlineId, jsonString)
            println("Đã lưu lịch sử ngoại tuyến: $offlineId")
        } catch (e: Exception) {
            println("Lỗi lưu lịch sử ngoại tuyến: ${e.message}")
        }
    }

    /**
     * Kiểm tra số lượng bản ghi đang kẹt offline.
     */
    override fun getOfflineHistoryCount(): Int {
        return localStorage.getAllKeys().count { it.startsWith("offline_") }
    }

    /**
     * Đồng bộ lịch sử ngoại tuyến lên Firebase khi có mạng.
     * @return Số lượng bản ghi đồng bộ thành công
     */
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun syncOfflineHistory(): Int {
        val keys = localStorage.getAllKeys().filter { it.startsWith("offline_") }
        var successCount = 0
        for (key in keys) {
            try {
                val jsonString = localStorage.getString(key) ?: continue
                val offlineData = Json.decodeFromString<OfflineScanHistory>(jsonString)
                val imageBytes = Base64.decode(offlineData.imageBase64)
                
                val url = uploadImage(offlineData.userId, imageBytes)
                if (url != null) {
                    val updatedHistory = offlineData.history.copy(imageUrl = url)
                    val success = saveHistory(updatedHistory)
                    if (success) {
                        localStorage.remove(key)
                        successCount++
                        println("Đã đồng bộ thành công: $key")
                    }
                }
            } catch (e: Exception) {
                println("Lỗi đồng bộ lịch sử $key: ${e.message}")
            }
        }
        return successCount
    }
}