# BugScanner 🐛 - Hệ sinh thái Quét AI & Bách khoa toàn thư Côn trùng Đa nền tảng

BugScanner là một ứng dụng Kotlin Multiplatform (KMP) toàn diện dành cho Android và Web. Không chỉ dừng lại ở một công cụ nhận diện thông thường, BugScanner là một hệ sinh thái khép kín: kết hợp giữa nhận diện Edge AI (LiteRT) siêu tốc, hạ tầng Cloud linh hoạt, trợ lý ảo LLM và một cơ sở dữ liệu có khả năng tự động mở rộng và học hỏi không ngừng từ chính các truy vấn của cộng đồng.

## Kiến trúc Hệ thống & Tính năng Cốt lõi

### 1. Động cơ Nhận diện Lai (YOLO11s & iNaturalist API)
Hệ thống nhận diện được thiết kế tối ưu, kết hợp sức mạnh của mô hình AI tại thiết bị và mạng lưới API chuyên sâu trên đám mây.
* **Suy luận tại biên (Edge AI):** Sử dụng mô hình quantized YOLO11s chạy trực tiếp cục bộ. Trên Android, hệ thống dùng TensorFlow Lite. Trên Web, hệ thống sử dụng kiến trúc **LiteRT Web (WASM)** thông qua thư viện `@litertjs/core`, loại bỏ hoàn toàn nhu cầu convert sang định dạng TFJS cũ. Engine này cung cấp khả năng phát hiện vật thể realtime với độ trễ cực thấp và hoạt động hoàn hảo 100% offline.
* **Cơ chế Fallback Đám mây:** Ứng dụng triển khai luồng graceful degradation. Khi mô hình AI cục bộ cho ra điểm tin cậy thấp, hệ thống sẽ tự động chuyển tiếp luồng ảnh gọi API của iNaturalist để phân tích chuyên sâu.
* **Xử lý Luồng dữ liệu Đa định dạng:** Hỗ trợ xử lý ảnh realtime qua luồng camera trực tiếp (tích hợp cơ chế freeze-frame) và hỗ trợ quét ảnh tĩnh tải lên từ thư viện.

### 2. Trợ lý ảo AI & RAG (Retrieval-Augmented Generation)
Khác biệt hoàn toàn với các chatbot API truyền thống, BugScanner mang đến trải nghiệm trợ lý ảo phản hồi tức thì và thấu hiểu ngữ cảnh.
* **Động cơ Suy luận Kép:** Ứng dụng tích hợp sức mạnh của Google Gemini (`gemini-2.5-flash`) và Groq (`gpt-oss-120b`). Nhờ kiến trúc LPU của Groq và cơ chế RAG, hệ thống đọc trực tiếp dữ liệu từ Bách khoa toàn thư để trả lời câu hỏi của người dùng một cách chính xác, loại bỏ các thuật ngữ kỹ thuật khô khan.
* **Hội thoại Kế thừa Ngữ cảnh:** Luồng UI được thiết kế để tự động luân chuyển data prompt mượt mà từ màn hình Quét hoặc Bách khoa thẳng vào tab Chatbot. 
* **Tương tác Web tinh tế:** Hỗ trợ tính năng dán ảnh trực tiếp bằng phím tắt `Ctrl+V` (Clipboard Paste) trên giao diện Web, mang lại trải nghiệm chuẩn mực như ChatGPT.

### 3. Bách khoa toàn thư Tự động sinh (AI-Generated Encyclopedia)
Đây là trái tim của ứng dụng. Database không chỉ là nơi đọc dữ liệu tĩnh, mà là một thực thể sống liên tục phát triển (Crowdsourcing).
* **Tự động Mở rộng bằng AI:** Khi người dùng quét một loài côn trùng hoàn toàn mới chưa tồn tại trong Firestore, ứng dụng sẽ gọi mô hình `gpt-oss-120b` (qua Groq) để tự động soạn thảo một bài viết bách khoa chi tiết (Cách xử lý, Cây ký chủ, Mức độ độc hại). Bài viết này được ép xuất ra định dạng JSON chuẩn mực và tự động ghi ngược (write-back) lên Firestore cho cộng đồng.
* **Tìm kiếm Tiền tố & Dịch thuật AI:** Hỗ trợ tìm kiếm siêu tốc bằng kỹ thuật prefix-search (`\uf8ff`) trên Firestore mà không cần ElasticSearch. Tích hợp AI dịch thuật để tự động dịch Tên Tiếng Việt sang Tiếng Anh làm mồi (fallback) khi tra cứu các API quốc tế.
* **Offline-First Persistence:** Các lượt quét và tìm kiếm khi offline sẽ được đưa vào hàng đợi LocalStorage/SharedPreferences và tự động đồng bộ ngầm lên server (Auto-sync) khi có mạng trở lại.

### 4. Quản trị viên & Bảng Điều Khiển (Admin Dashboard)
* **Bảo mật Phân quyền:** Sử dụng hai Collection Firestore riêng biệt (`users` và `admins`) để ngăn chặn triệt để rủi ro leo thang đặc quyền (Privilege Escalation).
* **Biểu đồ Zero-Dependency:** Các thống kê hệ thống được vẽ thủ công 100% bằng thư viện `Canvas` native của Compose. Việc loại bỏ các thư viện biểu đồ bên thứ 3 giúp tối ưu triệt để dung lượng và đảm bảo độ mượt mà trên cả Android lẫn Web.
* **Cấu hình Động (Dynamic Config):** Cho phép Admin thay đổi System Prompt và tên mô hình AI (như đổi sang bản Gemini mới) ngay trên bảng điều khiển, lập tức có tác dụng trên toàn bộ máy người dùng mà không cần tung bản cập nhật App.

### 5. Trạm trung chuyển Hình ảnh & Trải nghiệm Đa nền tảng
* **Bypass Dung lượng (IMGBB):** Hình ảnh được upload trực tiếp lên máy chủ IMGBB để lấy Public URL. URL này đóng vai trò làm khóa chính siêu nhẹ cho luồng LLM Vision, giảm tải tối đa băng thông Firebase.
* **Động cơ Chia sẻ Native:** Sử dụng native Intents (Android) kết hợp với FileProvider, và tích hợp Web Share API (Web) an toàn tuyệt đối.

## Giới hạn Hệ thống & Lỗi đã biết (Known Issues)

* **Rào cản Phần cứng (Thermal Throttling):** Chạy ImageAnalysis liên tục bằng YOLO11s trên thiết bị di động có thể làm máy nóng lên, buộc hệ điều hành kích hoạt thermal throttling và làm giảm FPS.
* **App Bundle Size:** Việc gói kèm file model `.tflite` vào thư mục assets để chạy 100% offline làm tăng đáng kể dung lượng file APK/AAB ban đầu.
* **Cold Start Offline:** Trải nghiệm offline phụ thuộc vào cơ chế cache của Firestore. Nếu thiết bị vừa cài đặt App và đi ngay vào vùng mất sóng (chưa từng tải data), bách khoa toàn thư sẽ trống rỗng.
* **Tainted Canvas (Web):** Hình ảnh từ domain bên ngoài (như iNaturalist) nếu không đính kèm header CORS khi vẽ lên Canvas sẽ bị trình duyệt đánh dấu là Tainted, gây lỗi SecurityError nếu cố tình đọc byte ảnh. Giải pháp hiện tại là dùng RemoteImagePolicy để cô lập.
* **Lỗi Share Intent (Facebook Messenger trên Android):** Messenger thường có hành vi vứt bỏ phần văn bản khi chia sẻ kèm ảnh. Khắc phục: Ép sao chép văn bản vào Clipboard và hiện Toast nhắc người dùng dán vào khung chat.
* **Lỗi Web Share API trên Desktop:** Hàm chia sẻ bị vô hiệu hóa khi share file cục bộ trên Chrome/Firefox PC. Khắc phục: Tự động copy text và tải file ảnh trực tiếp xuống thư mục Downloads.

---

## Thách thức Kỹ thuật & Giải pháp (Technical Challenges & Solutions)

Quá trình xây dựng và hoàn thiện dự án đã ghi nhận và xử lý triệt để các bài toán kỹ thuật cốt lõi sau:

### 1. Quản lý Bộ nhớ & Vòng đời Camera (Memory & State Leakage)
* **Vấn đề:** Màn hình quét (Scan screen) từng bị rò rỉ trạng thái bất đồng bộ và tràn bộ nhớ (Memory Leak) khi xử lý luồng ảnh liên tục từ Camera. Giao diện Web cũng gặp tình trạng giật lag và không hiển thị luồng Camera.
* **Giải quyết:** Cấu trúc lại toàn bộ kiến trúc Scan Screen, tách bạch luồng lifecycle của giao diện khỏi luồng tiền xử lý Tensor. Đóng gói lại (bundle) model v3 riêng biệt cho Android và Web để tối ưu hóa việc giải phóng RAM (`tf.dispose` và `litert.delete()`).

### 2. Sự cố Phiên Đăng nhập Web (Session Loss) & UX
* **Vấn đề:** Người dùng Web bị văng đăng xuất (mất session) mỗi khi ấn tải lại trang (F5). Màn hình Splash cũng bị nháy sáng (flashing) khi hệ thống kiểm tra auto-login.
* **Giải quyết:** Tái cấu trúc cơ chế quản lý trạng thái, chuyển sang lắng nghe luồng sự kiện `authStateChanged` trực tiếp từ API của Firebase. Cấu trúc lại luồng khởi tạo Splash Screen để loại bỏ tình trạng độ trễ hình ảnh.

### 3. Lỗ hổng Quyền riêng tư (Privacy Breach)
* **Vấn đề:** Lịch sử trò chuyện với Chatbot không bị xóa đi khi người dùng bấm Đăng xuất (Logout), dẫn đến người dùng sau có thể đọc được.
* **Giải quyết:** Bổ sung cơ chế tự động dọn dẹp (Auto clear) toàn bộ `ViewModel` và Local Database ngay lập tức khi phát hiện tín hiệu đăng xuất.

### 4. Tìm kiếm Tiếng Việt & Dịch thuật AI
* **Vấn đề:** Cơ chế tìm kiếm mặc định gặp hạn chế với các từ khóa tiếng Việt có dấu hoặc các tên gọi địa phương đa dạng.
* **Giải quyết:** Đồng bộ khóa tìm kiếm theo tên tiếng Anh thông dụng (English Common Name). Tích hợp Groq AI để tự động dịch thuật và chuẩn hóa ngữ cảnh từ khóa tiếng Việt sang tiếng Anh trước khi thực thi truy vấn.

### 5. LLM Phá vỡ Cấu trúc Parse JSON (Markdown Breakdown)
* **Vấn đề:** Động cơ AI thi thoảng sinh ra văn bản chứa Markdown (in đậm, tiêu đề) làm hỏng giao diện chat, nghiêm trọng hơn là phá vỡ cấu trúc JSON khi sinh Bách khoa toàn thư. Ngoài ra, tính năng RAG hay bị truyền nhầm ngữ cảnh.
* **Giải quyết:** Sửa đổi sâu vào System Prompt, ép AI trả về dữ liệu nguyên thủy (Raw) và đổi sang dùng model `gpt-oss-120b` xử lý định dạng JSON cấu trúc tốt hơn. Vá lại luồng luân chuyển dữ liệu RAG giữa màn hình BugDetail và Chatbot.

### 6. Bất đồng bộ Lưu trữ Lịch sử (Offline Sync)
* **Vấn đề:** Cơ chế lưu lịch sử offline đôi lúc ghi nhận sai trạng thái lưu, dẫn đến mất dữ liệu khi thiết bị có lại kết nối mạng.
* **Giải quyết:** Rút gọn lại luồng cập nhật StateFlow của lịch sử quét, đảm bảo mọi dữ liệu đẩy vào hàng đợi SQLite/LocalStorage phải được xác nhận hoàn tất trước khi cập nhật UI.