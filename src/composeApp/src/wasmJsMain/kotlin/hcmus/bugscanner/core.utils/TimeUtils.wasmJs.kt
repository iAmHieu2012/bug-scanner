package hcmus.bugscanner.core.utils

/**
 * Hàm ngoại vi (external function) gọi trực tiếp API `Date.now()` của JavaScript.
 * Trả về thời gian hiện tại dưới dạng Double để tương thích với KMP và tránh lỗi của kiểu Long trong Wasm/JS.
 */
@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

/**
 * Hàm ngoại vi (external function) xử lý logic định dạng ngày tháng bằng JavaScript thuần.
 * Nhận vào timestamp và trả về chuỗi theo định dạng "dd/MM/yyyy HH:mm".
 * Các hàm `padStart(2, '0')` đảm bảo các con số nhỏ hơn 10 luôn có số 0 ở đầu (VD: 09 thay vì 9).
 *
 * @param timestamp Thời gian dạng milliseconds cần định dạng.
 * @return Chuỗi thời gian đã được định dạng.
 */
@JsFun("""
(timestamp) => { 
    const d = new Date(timestamp); 
    const day = d.getDate().toString().padStart(2, '0');
    const month = (d.getMonth() + 1).toString().padStart(2, '0');
    const year = d.getFullYear();
    const hours = d.getHours().toString().padStart(2, '0');
    const minutes = d.getMinutes().toString().padStart(2, '0');
    return day + '/' + month + '/' + year + ' ' + hours + ':' + minutes; 
}
""")
private external fun jsFormatDate(timestamp: Double): String

/**
 * Triển khai hàm lấy thời gian hiện hành (actual function) cho nền tảng WasmJS.
 *
 * @return Mốc thời gian hiện tại tính bằng milliseconds.
 */
actual fun getCurrentTimeMillis(): Double = jsDateNow()

/**
 * Triển khai hàm định dạng chuỗi thời gian (actual function) cho nền tảng WasmJS.
 *
 * @param timestamp Giá trị thời gian cần định dạng.
 * @return Chuỗi thời gian đã được định dạng chuẩn "dd/MM/yyyy HH:mm".
 */
actual fun formatTimestamp(timestamp: Double): String = jsFormatDate(timestamp)