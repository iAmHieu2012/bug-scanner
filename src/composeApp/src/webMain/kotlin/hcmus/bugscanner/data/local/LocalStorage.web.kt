package hcmus.bugscanner.data.local

import kotlinx.browser.window

/**
 * Trình quản lý lưu trữ cục bộ cho nền tảng Web (HTML/JS/WASM).
 * Sử dụng Web Storage API (localStorage) của trình duyệt để lưu giữ dữ liệu vĩnh viễn
 * cho đến khi người dùng xóa bộ nhớ đệm (cache) của trình duyệt.
 */
actual class LocalStorage actual constructor() {

    /**
     * Lưu trữ một cặp khóa - giá trị kiểu chuỗi vào trình duyệt.
     *
     * @param key Khóa định danh.
     * @param value Giá trị chuỗi cần lưu.
     */
    actual fun saveString(key: String, value: String) {
        window.localStorage.setItem(key, value)
    }

    /**
     * Truy xuất giá trị chuỗi dựa vào khóa.
     *
     * @param key Khóa định danh cần tìm.
     * @return Chuỗi giá trị nếu tồn tại, ngược lại trả về `null`.
     */
    actual fun getString(key: String): String? {
        return window.localStorage.getItem(key)
    }

    /**
     * Xóa một bản ghi khỏi bộ nhớ cục bộ.
     *
     * @param key Khóa định danh của bản ghi cần xóa.
     */
    actual fun remove(key: String) {
        window.localStorage.removeItem(key)
    }

    /**
     * Lấy toàn bộ danh sách các khóa hiện đang được lưu trong LocalStorage.
     *
     * @return Danh sách chuỗi chứa tất cả các khóa.
     */
    actual fun getAllKeys(): List<String> {
        val keys = mutableListOf<String>()
        for (i in 0 until window.localStorage.length) {
            window.localStorage.key(i)?.let { keys.add(it) }
        }
        return keys
    }
}
