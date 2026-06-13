package hcmus.bugscanner.ui.layout

/**
 * Định nghĩa phân nhóm kích thước màn hình để phục vụ thiết kế đáp ứng (Responsive Layout).
 */
enum class AdaptiveLayoutSize { COMPACT, MEDIUM, EXPANDED }

/**
 * Phân loại kích thước giao diện dựa trên chiều rộng tính bằng Dp.
 *
 * @param widthDp Chiều rộng màn hình hiện hành tính bằng Dp.
 * @return [AdaptiveLayoutSize] tương ứng (COMPACT, MEDIUM hoặc EXPANDED).
 */
fun classifyAdaptiveWidth(widthDp: Float): AdaptiveLayoutSize = when {
    widthDp < 600f -> AdaptiveLayoutSize.COMPACT
    widthDp < 1000f -> AdaptiveLayoutSize.MEDIUM
    else -> AdaptiveLayoutSize.EXPANDED
}
