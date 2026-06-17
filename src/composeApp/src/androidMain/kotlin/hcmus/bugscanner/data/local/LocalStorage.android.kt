package hcmus.bugscanner.data.local

import android.content.Context
import hcmus.bugscanner.MainActivity

/**
 * Trình quản lý lưu trữ cục bộ cho nền tảng Android.
 * Sử dụng [SharedPreferences] để ghi dữ liệu xuống đĩa vật lý của thiết bị.
 */
actual class LocalStorage actual constructor() {
    private val prefs = MainActivity.appContext.getSharedPreferences("offline_history", Context.MODE_PRIVATE)

    /**
     * Lưu trữ một cặp khóa - giá trị kiểu chuỗi vào [SharedPreferences].
     * Sử dụng phương thức `apply()` để ghi bất đồng bộ (Asynchronous) nhằm tối ưu hiệu năng.
     *
     * @param key Khóa định danh.
     * @param value Giá trị chuỗi cần lưu.
     */
    actual fun saveString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    /**
     * Truy xuất giá trị chuỗi đã lưu dựa vào khóa.
     *
     * @param key Khóa định danh cần tìm.
     * @return Chuỗi giá trị nếu tồn tại, ngược lại trả về `null`.
     */
    actual fun getString(key: String): String? {
        return prefs.getString(key, null)
    }

    /**
     * Xóa vĩnh viễn một bản ghi khỏi hệ thống bằng khóa định danh.
     *
     * @param key Khóa định danh của bản ghi cần xóa.
     */
    actual fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    /**
     * Lấy toàn bộ danh sách các khóa hiện đang được lưu.
     *
     * @return Danh sách chuỗi chứa tất cả các khóa của [SharedPreferences].
     */
    actual fun getAllKeys(): List<String> {
        return prefs.all.keys.toList()
    }
}
