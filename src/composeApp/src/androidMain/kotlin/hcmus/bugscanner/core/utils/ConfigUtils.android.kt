package hcmus.bugscanner.core.utils

// Android sẽ lấy từ BuildConfig do Gradle sinh ra
actual fun getGeminiApiKey(): String = hcmus.bugscanner.BuildConfig.GEMINI_API_KEY