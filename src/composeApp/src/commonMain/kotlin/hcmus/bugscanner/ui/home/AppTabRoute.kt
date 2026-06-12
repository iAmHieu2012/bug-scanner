package hcmus.bugscanner.ui.home

fun AppTab.toHashRoute(): String = when (this) {
    AppTab.SCAN -> "#/scan"
    AppTab.HISTORY -> "#/history"
    AppTab.WIKI -> "#/encyclopedia"
    AppTab.CHATBOT -> "#/chat"
}

fun appTabFromHash(hash: String): AppTab = when (hash.trim().lowercase()) {
    "#/history" -> AppTab.HISTORY
    "#/encyclopedia" -> AppTab.WIKI
    "#/chat" -> AppTab.CHATBOT
    else -> AppTab.SCAN
}
