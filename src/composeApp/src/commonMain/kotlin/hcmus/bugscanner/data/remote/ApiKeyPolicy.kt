package hcmus.bugscanner.data.remote

object ApiKeyPolicy {
    fun isConfigured(value: String): Boolean {
        val key = value.trim()
        return key.isNotEmpty() &&
            !key.endsWith("...") &&
            !key.startsWith("your_", ignoreCase = true) &&
            !key.contains("_here", ignoreCase = true)
    }

    fun requireConfigured(serviceName: String, value: String): String {
        val key = value.trim()
        require(isConfigured(key)) { "$serviceName API key is not configured" }
        return key
    }
}
