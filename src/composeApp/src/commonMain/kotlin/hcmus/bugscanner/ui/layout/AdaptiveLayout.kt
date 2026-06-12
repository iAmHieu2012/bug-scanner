package hcmus.bugscanner.ui.layout

enum class AdaptiveLayoutSize { COMPACT, MEDIUM, EXPANDED }

fun classifyAdaptiveWidth(widthDp: Float): AdaptiveLayoutSize = when {
    widthDp < 600f -> AdaptiveLayoutSize.COMPACT
    widthDp < 1000f -> AdaptiveLayoutSize.MEDIUM
    else -> AdaptiveLayoutSize.EXPANDED
}
