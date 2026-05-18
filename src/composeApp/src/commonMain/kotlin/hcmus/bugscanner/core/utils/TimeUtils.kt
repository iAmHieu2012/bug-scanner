package hcmus.bugscanner.core.utils

// Khai báo hàm để commonMain yêu cầu nền tảng cung cấp cách lấy và format thời gian
expect fun getCurrentTimeMillis(): Long
expect fun formatTimestamp(timestamp: Long): String