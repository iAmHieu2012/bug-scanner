package hcmus.bugscanner.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window
import org.khronos.webgl.Int8Array

/**
 * Khai báo Interface an toàn để Kotlin có thể giao tiếp với Object Share của JavaScript.
 */
external interface ShareData {
    var title: String?
    var text: String?
    var url: String?
    var files: Array<dynamic>?
}

/**
 * Hàm hỗ trợ (Helper) tạo một Object JavaScript rỗng an toàn.
 */
internal fun createJsObject(): dynamic = js("({})")

/**
 * Triển khai cụ thể (Implementation) của [ShareManager] dành riêng cho nền tảng Web (Wasm/JS).
 * Sử dụng Web Share API của trình duyệt (Hỗ trợ chia sẻ chữ, link và đính kèm file ảnh).
 */
class WebShareManager : ShareManager {

    /**
     * Mở hộp thoại chia sẻ gốc của hệ điều hành/trình duyệt.
     * Nếu trình duyệt không hỗ trợ hoặc chặn API do lý do bảo mật, hệ thống sẽ tự động chuyển sang chế độ copy văn bản.
     *
     * @param bugName Tên phổ thông của côn trùng.
     * @param scientificName Tên khoa học của côn trùng.
     * @param imageBytes Mảng byte của hình ảnh (nếu có).
     * @param confidenceLabel Độ tin cậy của kết quả nhận diện.
     * @param harmfulnessLabel Mức độ gây hại của côn trùng.
     * @param appLink Đường dẫn tải app hoặc trang web.
     */
    override fun shareBugInfo(
        bugName: String,
        scientificName: String,
        imageBytes: ByteArray?,
        confidenceLabel: String,
        harmfulnessLabel: String,
        appLink: String
    ) {
        val shareText = buildString {
            append("Tôi vừa phát hiện ra loài: $bugName trên ứng dụng BugScanner. ")
            append("Tên khoa học: $scientificName. ")
            if (confidenceLabel.isNotBlank()) append("Độ tin cậy: $confidenceLabel. ")
            if (harmfulnessLabel.isNotBlank()) append("Mức gây hại: $harmfulnessLabel.")
        }.trim()

        try {
            val navigator: dynamic = window.navigator

            if (navigator.share != undefined) {
                val shareData = createJsObject().unsafeCast<ShareData>()
                shareData.title = "Nhận diện côn trùng qua BugScanner"
                shareData.text = shareText
                shareData.url = appLink

                if (imageBytes != null && navigator.canShare != undefined) {
                    // Ép kiểu ByteArray sang Int8Array trực tiếp không qua trung gian (0-copy)
                    val int8Array = imageBytes.unsafeCast<Int8Array>()

                    // Dùng constructor chuẩn của Kotlin JS (tự động có 'new' ngầm định trong JS)
                    val fileOpts = js("{ type: 'image/jpeg' }").unsafeCast<org.w3c.files.FilePropertyBag>()
                    val file = org.w3c.files.File(arrayOf(int8Array), "bug_scanned.jpg", fileOpts)

                    val filesArray = arrayOf(file)

                    val testData = createJsObject()
                    testData.files = filesArray

                    if (navigator.canShare(testData) as Boolean) {
                        shareData.files = filesArray
                    }
                }

                val promise = navigator.share(shareData)
                promise.catch {
                    fallbackToClipboard(shareText, appLink)
                }
            } else {
                fallbackToClipboard(shareText, appLink)
            }
        } catch (e: Exception) {
            fallbackToClipboard(shareText, appLink)
        }
    }

    /**
     * Hàm dự phòng: Tự động sao chép văn bản vào khay nhớ tạm (Clipboard) nếu tính năng Share bị lỗi.
     * Hiển thị thông báo (Alert) cho người dùng biết trạng thái.
     *
     * @param text Nội dung văn bản cần sao chép.
     * @param link Đường dẫn đính kèm thêm vào văn bản.
     */
    private fun fallbackToClipboard(text: String, link: String) {
        val fullText = "$text\nKhám phá ngay tại: $link"
        try {
            window.navigator.clipboard.writeText(fullText)
            window.alert("Trình duyệt đã chặn chia sẻ trực tiếp (Do bảo mật của Canvas). Đã tự động copy thông tin vào khay nhớ tạm!")
        } catch (e: Exception) {
            window.alert("Thông tin côn trùng:\n$fullText")
        }
    }
}

/**
 * Hàm actual khởi tạo và ghi nhớ [WebShareManager] trên nền tảng Web.
 *
 * @return Phiên bản [ShareManager] hoạt động trên trình duyệt.
 */
@Composable
actual fun rememberShareManager(): ShareManager {
    return remember { WebShareManager() }
}
