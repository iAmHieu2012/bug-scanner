# Đề Xuất Cải Thiện & Tối Ưu (Suggestions & Improvements)

File này ghi chú những điểm hạn chế trong kiến trúc, thuật toán hoặc UI/UX của dự án và đề xuất giải pháp khắc phục.

### 1. Dọn dẹp Code Thừa & Tối ưu Kiểu Dữ liệu
- **File thừa**: `Greeting.kt` là file sinh ra từ template mặc định của KMP wizard, không hề phục vụ cho business logic của ứng dụng. **Đề xuất**: Xóa bỏ hoàn toàn file này.
- **`TimeUtils.kt`**: Hàm `getCurrentTimeMillis()` trả về kiểu `Double`. Mặc dù trong JS số luôn là Float/Double, nhưng API `toEpochMilliseconds()` gốc của Kotlin trả về kiểu `Long`. Việc ép kiểu sang `Double` rồi sau đó lại ép về `toLong()` ở hàm `formatTimestamp` là thừa thãi và không an toàn (Double bị mất độ chính xác với các số nguyên lớn). **Đề xuất**: Đổi kiểu trả về của timestamp thành `Long`.

### 2. Thiết kế Cơ sở Dữ liệu (Database Schema)
- **Flatten Schema trong `ScanHistory.kt`**: Thay vì chứa một thuộc tính `bugInfo: BugInfo`, lớp `ScanHistory` lại khai báo lại toàn bộ các trường của BugInfo (bugId, bugName, scientificName, description, v.v.). Việc "đập phẳng" (Flatten) này có lợi cho việc query index trên Firestore (Ví dụ tìm lịch sử theo Tên loài), nhưng lại khiến code sinh ra nhiều hàm chuyển đổi (mapper) rườm rà như `toBugInfo()` và `toHistory()`. **Đề xuất**: Nên cân nhắc bọc hẳn `bugInfo` thành một Map object lưu trên Firestore nếu như ứng dụng không có nhu cầu query trực tiếp vào các field description/danger của lịch sử.

### 3. Rủi ro về Tích hợp API (Remote API)
- **Google Translate GTX (`WikiApiService.kt`)**: Hàm `translateToVietnamese` đang sử dụng endpoint `translate.googleapis.com/translate_a/single?client=gtx`. Đây là một endpoint không chính thức, không có tài liệu của Google, thường được dùng nội bộ bởi extension Chrome. Nếu Google phát hiện lượng request tăng đột biến, họ có thể khóa IP hoặc thay đổi cấu trúc mảng JSON trả về bất cứ lúc nào, gây crash toàn bộ tính năng đọc Wiki tiếng nước ngoài. **Đề xuất**: Nên sử dụng Groq API (vốn đã được tích hợp sẵn Llama 3) để dịch thuật nội dung Wiki, hoặc dùng Cloud Translation API trả phí chính thống.

### 4. Rủi ro Lưu trữ Ảnh (Image Storage)
- **Sử dụng ImgBB (`HistoryRepositoryImpl.kt`)**: Ứng dụng đang đẩy ảnh history lên `api.imgbb.com` thông qua một API Key cố định. Nếu API Key này bị hết hạn, quá giới hạn lượt tải, hoặc ImgBB sập, toàn bộ tính năng lưu Lịch sử (kể cả đồng bộ Offline) sẽ bị tê liệt hoàn toàn do không lấy được URL tĩnh. **Đề xuất**: Đã tích hợp sẵn hệ sinh thái Firebase (Firestore) thì nên cấu hình và sử dụng **Firebase Storage** luôn để lưu trữ ảnh, giúp tăng tính đồng bộ, bảo mật và dễ quản lý tập trung.

### 5. UI / UX State Management
- **Mất State khi xoay màn hình ở `HomeScreen.kt`**: Cơ chế mở màn hình chi tiết (BugDetail) đang dựa vào state cục bộ `var selectedSnapshot by remember { mutableStateOf<DetectedBugSnapshot?>(null) }`. Việc dùng `remember` thông thường sẽ khiến màn hình bị reset (tắt màn hình chi tiết, văng ra ngoài tab) khi người dùng đổi cấu hình thiết bị (xoay ngang/dọc màn hình Android). Hơn nữa cơ chế này không dính liền với `BackHandler`, dẫn tới việc người dùng ấn nút Back của Android sẽ thoát luôn ứng dụng thay vì đóng màn chi tiết. **Đề xuất**: Nên gắn thêm `BackHandler` khi `selectedSnapshot != null`.

### 6. Rủi ro Phình to Bộ nhớ (OOM) khi lưu Offline
- **Lưu ảnh Base64 vào LocalStorage (`HistoryRepositoryImpl.kt`)**: Khi người dùng mất mạng, hàm `saveOfflineHistory` đang encode toàn bộ mảng byte của ảnh sang chuỗi `Base64` và nén chung vào một cục JSON để lưu xuống `LocalStorage` (K/V store). Điều này là cực kỳ nguy hiểm vì Base64 làm tăng 33% dung lượng ảnh. Nếu ảnh nặng 5MB, chuỗi String sẽ lên tới ~7MB. Lưu nhiều bản ghi sẽ làm phình to bộ nhớ K/V, gây tốn RAM khi `Json.decodeFromString` và dễ dẫn đến Crash (Out of Memory). 
- **Đề xuất**: Tuyệt đối không lưu Base64 của ảnh tĩnh chất lượng cao vào K/V Database. Hãy ghi mảng byte ảnh thành một file vật lý (vd: `offline_img_123.jpg`) vào thư mục `Cache` hoặc `Files` của hệ điều hành thông qua `expect/actual`, và trong Database chỉ lưu lại chuỗi đường dẫn (Local Path) đến file đó. Mạng có lại thì đọc file lên và upload.

### 7. Tính năng Dùng không cần Mạng / Bỏ qua Đăng nhập (Lazy Authentication)
- **Hạn chế của `signInAnonymously()`**: Hiện tại tính năng "Đăng nhập ẩn danh" bắt buộc phải có Internet ở lần chạy đầu tiên để Firebase cấp phát UID. Nếu người dùng tải App, tắt WiFi và mở App thì sẽ không thể đăng nhập ẩn danh được, dẫn đến việc không cho phép trải nghiệm App.
- **Đề xuất (Lazy Authentication)**: Có thể thiết kế lại logic để không cần bắt buộc phụ thuộc Auth. Nếu không có mạng hoặc người dùng chọn Bỏ qua, hãy sinh ra một UUID nội bộ (ví dụ: `local_device_abc123`) và cho phép người dùng vào thẳng `HomeScreen`. Toàn bộ lịch sử quét sẽ được gắn với `userId = local_device_abc123` và lưu tại `LocalStorage`. Sau này, khi người dùng thực sự muốn tạo tài khoản Email/Google để đồng bộ, App chỉ việc đổi tên các bản ghi từ `local_device_abc123` thành Firebase UID thật sự rồi đẩy lên Cloud. Đây là pattern rất thịnh hành trên các ứng dụng như TikTok, E-Commerce.

### 8. Nâng cấp: Xây dựng Admin Master Dashboard (Dành cho Production)
- **Thiếu công cụ Quản trị (CMS)**: Hiện tại dữ liệu Bách khoa toàn thư (Wikipedia) đang phụ thuộc vào Groq AI sinh ra và lưu trên Firestore. Nếu AI sinh ra nội dung sai lệch (Hallucination), Admin không có cách nào sửa lại ngoài việc chui vào giao diện thô cứng của Firebase Console để sửa từng file JSON. Hơn nữa, model YOLO (`model.tflite`) đang bị nhúng cứng (hardcode) vào App, mỗi lần model có bản mới lại phải bắt người dùng update App.
- **Đề xuất**: Xây dựng thêm một Master Dashboard (Web CMS) dành riêng cho Admin có phân quyền (Role-Based Access Control). Chức năng chính:
  1. Kiểm duyệt và sửa tay các bài viết Bách khoa do AI tạo ra.
  2. Bảng điều khiển (Analytics) thống kê loài côn trùng nào đang được quét nhiều nhất để thu thập làm Training Data.
  3. Quản lý OTA Model: Cho phép Admin upload `model.tflite` mới lên Dashboard, sau đó App ở dưới Client sẽ tự động fetch model mới về thay thế model cũ mà không cần update App.
