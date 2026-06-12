package hcmus.bugscanner.ui.components

actual fun canLoadRemoteImage(imageUrl: String): Boolean = !isKnownCanvasUnsafeImageUrl(imageUrl)
