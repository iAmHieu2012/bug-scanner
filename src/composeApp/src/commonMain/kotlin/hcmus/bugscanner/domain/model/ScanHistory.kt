package hcmus.bugscanner.domain.model

import kotlinx.serialization.Serializable

/**
 * Lớp thực thể (Entity) đại diện cho một bản ghi lịch sử nhận diện của người dùng.
 *
 * @property id Mã định danh duy nhất của bản ghi (tự động sinh bởi Firestore).
 * @property userId Mã định danh Firebase UID của người dùng sở hữu bản ghi.
 * @property bugName Tên của loài côn trùng được nhận diện.
 * @property timestamp Mốc thời gian thực hiện quét tính bằng milliseconds.
 * @property imageUrl Đường dẫn URL tĩnh của bức ảnh gốc được tải lên tại thời điểm quét.
 */
@Serializable
data class ScanHistory(
    val id: String = "",
    val userId: String = "",
    val bugName: String = "",
    val timestamp: Double = 0.0,
    val imageUrl: String = ""
)