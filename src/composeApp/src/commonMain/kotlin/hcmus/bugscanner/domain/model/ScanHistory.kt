package hcmus.bugscanner.domain.model

import kotlinx.serialization.Serializable

/**
 * Nguồn nhận diện tạo ra record scan/history.
 */
enum class ScanSource(val value: String, val displayName: String) {
    YOLO("yolo", "YOLO"),
    INATURALIST("inaturalist", "iNaturalist"),
    UNKNOWN("unknown", "Không rõ");

    val userFacingName: String
        get() = when (this) {
            YOLO -> "Nhận diện trong ứng dụng"
            INATURALIST -> "Nhận diện tham khảo"
            UNKNOWN -> "Không rõ"
        }

    companion object {
        fun fromValue(value: String): ScanSource = entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}

/**
 * Snapshot kết quả nhận diện dùng để truyền xuyên suốt scan -> detail -> chat -> history.
 */
data class DetectedBugSnapshot(
    val bug: BugInfo,
    val imageBytes: ByteArray? = null,
    val confidence: Float = 0f,
    val source: ScanSource = ScanSource.UNKNOWN
)

/**
 * Lớp thực thể (Entity) đại diện cho một bản ghi lịch sử nhận diện của người dùng.
 * Các field mới đều có default để record Firestore cũ vẫn deserialize được.
 *
 * @property id Mã định danh duy nhất của bản ghi (tự động sinh bởi Firestore).
 * @property userId Mã định danh Firebase UID của người dùng sở hữu bản ghi.
 * @property bugName Tên của loài côn trùng được nhận diện.
 * @property timestamp Mốc thời gian thực hiện quét tính bằng milliseconds.
 * @property imageUrl Đường dẫn URL tĩnh của bức ảnh gốc được tải lên tại thời điểm quét.
 * @property bugId Mã định danh loài côn trùng.
 * @property scientificName Tên khoa học của côn trùng.
 * @property englishName Tên tiếng Anh của côn trùng.
 * @property confidence Độ tin cậy của kết quả nhận diện.
 * @property source Nguồn gốc nhận diện (YOLO hoặc iNaturalist).
 * @property description Mô tả chi tiết về côn trùng.
 * @property identification Cách nhận dạng côn trùng.
 * @property danger Mức độ nguy hiểm của côn trùng.
 * @property harmfulnessLevel Mức gây hại chuẩn hóa.
 * @property treatment Cách xử lý/điều trị khi tiếp xúc.
 * @property wikiUrl Đường dẫn bách khoa toàn thư Wikipedia.
 */
@Serializable
data class ScanHistory(
    val id: String = "",
    val userId: String = "",
    val bugName: String = "",
    val timestamp: Double = 0.0,
    val imageUrl: String = "",
    val bugId: String = "",
    val scientificName: String = "",
    val englishName: String = "",
    val confidence: Float = 0f,
    val source: String = ScanSource.UNKNOWN.value,
    val description: String = "",
    val identification: String = "",
    val danger: String = "",
    val harmfulnessLevel: String = HarmfulnessLevel.UNKNOWN.value,
    val treatment: String = "",
    val wikiUrl: String = ""
)

/**
 * Chuyển đổi từ thực thể lịch sử quét [ScanHistory] sang thông tin chi tiết côn trùng [BugInfo].
 */
fun ScanHistory.toBugInfo(): BugInfo {
    val fallbackName = bugName.ifBlank { scientificName.ifBlank { bugId } }
    val resolvedScientificName = scientificName.ifBlank { fallbackName }

    return BugInfo(
        id = bugId.ifBlank { fallbackName },
        name = bugName.ifBlank { resolvedScientificName },
        englishName = englishName,
        scientificName = resolvedScientificName,
        description = description,
        imageUrl = imageUrl,
        identification = identification,
        danger = danger,
        harmfulnessLevel = harmfulnessLevel,
        treatment = treatment,
        wikiUrl = wikiUrl
    )
}

/**
 * Chuyển đổi kết quả nhận diện thời gian thực [DetectedBugSnapshot] sang bản ghi lịch sử [ScanHistory] để chuẩn bị lưu trữ.
 *
 * @param userId Mã người dùng Firebase UID.
 * @param timestamp Mốc thời gian lưu trữ.
 * @param uploadedImageUrl URL của hình ảnh sau khi đã được tải lên Storage.
 */
fun DetectedBugSnapshot.toHistory(
    userId: String,
    timestamp: Double,
    uploadedImageUrl: String = ""
): ScanHistory = ScanHistory(
    userId = userId,
    bugId = bug.id,
    bugName = bug.name,
    englishName = bug.englishName,
    scientificName = bug.scientificName,
    timestamp = timestamp,
    imageUrl = uploadedImageUrl.ifBlank { bug.imageUrl },
    confidence = confidence,
    source = source.value,
    description = bug.description,
    identification = bug.identification,
    danger = bug.danger,
    harmfulnessLevel = bug.harmfulnessLevel,
    treatment = bug.treatment,
    wikiUrl = bug.wikiUrl
)
