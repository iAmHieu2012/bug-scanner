package hcmus.bugscanner

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform