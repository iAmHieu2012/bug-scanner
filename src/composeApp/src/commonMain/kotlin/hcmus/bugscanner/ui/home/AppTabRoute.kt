package hcmus.bugscanner.ui.home

/**
 * Chuyển đổi tab ứng dụng [AppTab] sang định dạng hash route dạng chuỗi (dùng cho routing trên nền tảng Web).
 *
 * @return Chuỗi định tuyến (VD: "#/scan", "#/chat").
 */
fun AppTab.toHashRoute(): String = when (this) {
    AppTab.SCAN -> "#/scan"
    AppTab.HISTORY -> "#/history"
    AppTab.WIKI -> "#/encyclopedia"
    AppTab.CHATBOT -> "#/chat"
    AppTab.ADMIN -> "#/admin"
    AppTab.PROFILE -> "#/profile"
}

/**
 * Phân tích chuỗi hash route từ URL để chuyển đổi ngược lại thành đối tượng [AppTab] tương ứng.
 *
 * @param hash Chuỗi hash route lấy từ browser window.
 * @return Đối tượng [AppTab] được khớp, mặc định là [AppTab.SCAN].
 */
fun appTabFromHash(hash: String): AppTab = when (hash.trim().lowercase()) {
    "#/history" -> AppTab.HISTORY
    "#/encyclopedia" -> AppTab.WIKI
    "#/chat" -> AppTab.CHATBOT
    "#/admin" -> AppTab.ADMIN
    "#/profile" -> AppTab.PROFILE
    else -> AppTab.SCAN
}
