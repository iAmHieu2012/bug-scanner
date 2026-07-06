package hcmus.bugscanner.ui.chat

import hcmus.bugscanner.domain.model.GeminiContent

/**
 * Centralizes limits for payloads sent to Gemini so chat cost and request size stay bounded.
 */
object ChatPayloadPolicy {
    const val MAX_CONTEXT_MESSAGES = 8
    const val MAX_INLINE_IMAGE_BYTES = 4 * 1024 * 1024

    fun trimHistory(history: List<GeminiContent>): List<GeminiContent> {
        return history.takeLast(MAX_CONTEXT_MESSAGES)
    }

    fun acceptsInlineImage(imageBytes: ByteArray): Boolean {
        return imageBytes.size <= MAX_INLINE_IMAGE_BYTES
    }
}
