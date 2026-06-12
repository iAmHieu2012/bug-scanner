package hcmus.bugscanner.ui.components

fun isKnownCanvasUnsafeImageUrl(imageUrl: String): Boolean {
    val host = imageUrl.substringAfter("://", missingDelimiterValue = "")
        .substringBefore('/')
        .substringBefore(':')
        .lowercase()
    return host == "static.inaturalist.org" ||
        host.endsWith(".static.inaturalist.org") ||
        host == "via.placeholder.com"
}

expect fun canLoadRemoteImage(imageUrl: String): Boolean
