package hcmus.bugscanner.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window
import org.khronos.webgl.Uint8Array

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
     * @param appLink Đường dẫn tải app hoặc trang web.
     */
    override fun shareBugInfo(
        bugName: String,
        scientificName: String,
        imageBytes: ByteArray?,
        appLink: String
    ) {
        val shareText = "Tôi vừa phát hiện ra loài: $bugName trên ứng dụng BugScanner. Tên khoa học: $scientificName."

        try {
            // Ép kiểu navigator về dynamic để vượt qua rào cản kiểu dữ liệu của Wasm
            val navigator: dynamic = window.navigator

            // Kiểm tra xem trình duyệt có hỗ trợ Web Share API hay không
            if (navigator.share != undefined) {

                // Khởi tạo Object cấu hình an toàn cho Wasm (Thay thế cho js("{}"))
                val shareData = createJsObject().unsafeCast<ShareData>()
                shareData.title = "Nhận diện côn trùng qua BugScanner"
                shareData.text = shareText
                shareData.url = appLink // Đính kèm Link tải app

                // NẾU CÓ ẢNH -> KIỂM TRA QUYỀN ĐÍNH KÈM FILE (Web Share API Level 2)
                if (imageBytes != null && navigator.canShare != undefined) {

                    // Chuyển ByteArray của Kotlin sang Uint8Array của JS để trình duyệt có thể đọc
                    val uint8Array = Uint8Array(imageBytes.toTypedArray())

                    // Khởi tạo đối tượng File của HTML5 thông qua JS thuần
                    val file = js("new window.File([uint8Array], 'bug_scanned.jpg', {type: 'image/jpeg'})")

                    // Gói file vào một mảng Javascript Array
                    val filesArray = arrayOf(file)

                    // Tạo dữ liệu test để hỏi hệ điều hành xem nó có duyệt cho share file này không
                    val testData = createJsObject()
                    testData.files = filesArray

                    if (navigator.canShare(testData) as Boolean) {
                        shareData.files = filesArray
                    }
                }

                // BẮT BUỘC PHẢI CÓ .catch() ĐỂ CHẶN LỖI DOMException CỦA TRÌNH DUYỆT
                // Xảy ra khi người dùng hủy chia sẻ hoặc trình duyệt chặn (ví dụ: iframe, thiếu HTTPS).
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