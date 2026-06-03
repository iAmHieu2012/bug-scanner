# BugScanner 🐛 - Hệ sinh thái Quét AI & Bách khoa toàn thư Côn trùng Đa nền tảng

BugScanner là một ứng dụng Kotlin Multiplatform toàn diện dành cho Android và Web. Không chỉ dừng lại ở một công cụ nhận diện thông thường, BugScanner là một hệ sinh thái khép kín: kết hợp giữa nhận diện Edge AI siêu tốc, hạ tầng Cloud linh hoạt, trợ lý ảo LLM và một cơ sở dữ liệu có khả năng tự động mở rộng và học hỏi không ngừng từ chính các truy vấn của người dùng.

## Kiến trúc Hệ thống & Tính năng Cốt lõi

### 1. Động cơ Nhận diện Lai (YOLO11s & iNaturalist API)

Hệ thống nhận diện được thiết kế tối ưu, kết hợp sức mạnh của mô hình AI tại thiết bị và mạng lưới API chuyên sâu để đảm bảo không một loài côn trùng nào bị bỏ sót.

* **Suy luận tại biên với YOLO11s:** Sử dụng mô hình quantized YOLO11s chạy trực tiếp trên CPU/GPU của thiết bị. Engine này cung cấp khả năng phát hiện vật thể realtime, vẽ bounding box và phân loại loài với độ trễ cực thấp, hoàn toàn không tiêu tốn băng thông mạng và hoạt động hoàn hảo ở vùng sâu vùng xa.
* **Cơ chế Fallback Đám mây:** Ứng dụng triển khai luồng graceful degradation. Khi mô hình YOLO11s tại thiết bị cho ra điểm tin cậy thấp hoặc đụng độ một loài dị biệt chưa có trong tập dữ liệu huấn luyện cục bộ, hệ thống sẽ tự động chuyển tiếp luồng ảnh, gọi thẳng API của iNaturalist để phân tích thị giác máy tính chuyên sâu.
* **Xử lý Luồng dữ liệu Đa định dạng:** Hỗ trợ xử lý ảnh realtime qua luồng camera trực tiếp tích hợp cơ chế freeze-frame để bắt dính các khung hình có độ nét cao nhất, đồng thời hỗ trợ suy luận ảnh tĩnh tải lên từ local gallery.

### 2. Trợ lý ảo AI Thời gian thực

Khác biệt hoàn toàn với các chatbot API truyền thống bị giới hạn tốc độ, BugScanner mang đến trải nghiệm trợ lý ảo phản hồi tức thì.

* **Động cơ Suy luận Groq siêu tốc:** Ứng dụng tích hợp sức mạnh của các mô hình ngôn ngữ lớn hàng đầu như LLaMA và Gemini. Thay vì gọi API trực tiếp từ nhà phát hành, BugScanner sử dụng backend suy luận của Groq. Nhờ kiến trúc phần cứng LPU của Groq, hệ thống xử lý token chớp nhoáng, loại bỏ hoàn toàn độ trễ sinh text.
* **Hội thoại Kế thừa Ngữ cảnh:** Luồng UI được thiết kế để tự động luân chuyển data prompt mượt mà từ màn hình bách khoa toàn thư hoặc kết quả nhận diện thẳng vào tab Chatbot. Hệ thống tự động tiêm các thông số sinh học vào prompt hệ thống, giúp người dùng có thể truy vấn sâu hơn về loài côn trùng vừa quét mà không cần thiết lập lại ngữ cảnh đầu vào.

### 3. Trạm trung chuyển Hình ảnh & Bypass Dung lượng (IMGBB)

Hình ảnh là dữ liệu đắt đỏ nhất trong toàn bộ chu trình xử lý. Thay vì truyền mảng byte thô làm nghẽn bộ nhớ, ứng dụng dùng kiến trúc trạm trung chuyển.

* **Tối ưu hóa Payload:** Hình ảnh độ phân giải cao thu được từ camera sẽ được upload trực tiếp lên máy chủ IMGBB.
* **Public URL làm Chìa khóa:** Link ảnh public trả về từ IMGBB đóng vai trò then chốt: nó được dùng làm tham số đầu vào siêu nhẹ cho các Cloud API phân tích, làm ngữ cảnh hình ảnh cho các luồng LLM Vision, và giải quyết triệt để bài toán chia sẻ nội dung đa nền tảng.

### 4. Bách khoa toàn thư Mở rộng Động (Dynamic Firestore Encyclopedia)

Đây là trái tim của ứng dụng. Database không chỉ là nơi đọc dữ liệu tĩnh, mà là một thực thể sống liên tục phát triển nhờ các lượt quét của cộng đồng người dùng.

* **Khởi tạo Dữ liệu Nền tảng:** Hệ thống sở hữu sẵn một Pre-built Encyclopedia đồ sộ lưu trữ trên Cloud Firestore chứa thông tin sinh học chi tiết của hàng ngàn loài.
* **Cơ chế Tự động Mở rộng (Crowdsourcing):** Khi người dùng quét hoặc tìm kiếm một loài côn trùng hoàn toàn mới chưa tồn tại trong Firestore, cơ chế Fallback (iNaturalist/AI) sẽ phân tích định danh loài đó. Ngay lập tức, ứng dụng sẽ thu thập các thông tin sinh học liên quan, định dạng cấu trúc chuẩn và tự động ghi ngược (write-back) một document mới vào Firestore. Nhờ đó, database của ứng dụng liên tục được mở rộng và làm giàu từ chính hành vi khám phá của cộng đồng.
* **Pre-fetch Engine & Offline Persistence:** Để phục vụ nhu cầu đi rừng, ứng dụng ép tiến trình tải trước toàn bộ collection từ Firestore vào bộ nhớ đệm cục bộ ở lần khởi chạy đầu tiên. Mọi thao tác tìm kiếm, đọc dữ liệu hoạt động mượt mà 100% trong chế độ máy bay. Các lượt quét khi offline sẽ vào hàng đợi và tự động đồng bộ ngầm lên server khi có mạng.

### 5. Trải nghiệm Native Đa nền tảng (KMP)

* **Adaptive UI:** Giao diện xây dựng hoàn toàn bằng Jetpack Compose Multiplatform. Hệ thống theo dõi biến số WindowSizeClass để tự động tính toán hình học layout, chuyển đổi mượt mà giữa Bottom Navigation Bar trên thiết bị di động và Navigation Rail trên màn hình lớn.
* **Động cơ Chia sẻ Native:**
* *Android:* Sử dụng native Intents kết hợp với FileProvider, ghi ảnh vật lý vào externalCacheDir và gán quyền truy cập URI động qua ClipData.
* *Web:* Tích hợp Web Share API, kết nối vùng nhớ Kotlin và JavaScript an toàn tuyệt đối thông qua hàm asDynamic.

## Giới hạn Hệ thống & Nút thắt Hiệu năng

### 1. Rào cản Phần cứng & Suy luận tại Biên

* **Thermal Throttling & FPS Drop:** Chạy luồng ImageAnalysis liên tục qua mô hình YOLO11s trên thiết bị Android di động sẽ vắt kiệt năng lực CPU/GPU. Máy sẽ nóng lên nhanh chóng, buộc hệ điều hành kích hoạt thermal throttling. Điều này làm bóp nghẹt tốc độ suy luận, khiến FPS camera tụt thê thảm và gây hao pin nghiêm trọng.
* **Nguy cơ Tràn bộ nhớ (OutOfMemory):** Quá trình trích xuất bitmap từ camera và chuyển đổi thành Tensor đa chiều đòi hỏi chu trình cấp phát bộ nhớ rác liên tục. Trên các thiết bị RAM thấp, dung lượng heap tăng vọt không kiểm soát sẽ đánh gục garbage collector, gây ra lỗi crash OutOfMemory.
* **App Bundle Size:** Nhúng cứng các file mô hình YOLO11s định dạng TFLite vào thư mục assets để đảm bảo khả năng chạy offline 100% sẽ đánh đổi bằng việc làm phình to đáng kể dung lượng file cài đặt ban đầu.

### 2. Giới hạn Kiến trúc API & Nền tảng Web

* **Nút thắt Cổ chai IMGBB:** Mặc dù động cơ LLM Groq xử lý text siêu tốc, toàn bộ luồng hoạt động bắt buộc phải chờ ảnh được upload thành công lên IMGBB để lấy public URL. Ở các khu vực mạng di động yếu kém, thời gian chờ upload này sẽ tạo ra nút thắt cổ chai, kéo lùi tốc độ phản hồi tổng thể của hệ thống.
* **Bảo mật Tainted Canvas trên Web:** Hình ảnh fetch về từ các domain bên ngoài không đính kèm header CORS. Khi kết xuất bề mặt lên thẻ Canvas, trình duyệt đánh dấu bảo mật mức Tainted Canvas. Việc dùng code trích xuất trực tiếp byte ảnh từ bề mặt này sẽ lập tức bị trình duyệt ném ra lỗi SecurityError.
* **Gánh nặng Single-thread (Web):** Phiên bản Web hoạt động hoàn toàn trên một luồng chính. Việc xử lý các mảng byte đồ họa độ phân giải cao sẽ block luồng xử lý giao diện, gây hiện tượng jank UI chớp nhoáng.
* **Cold Start Offline:** Trải nghiệm offline phụ thuộc hoàn toàn vào cơ chế cache của Firestore. Nếu thiết bị cài đặt app và đi vào vùng mất sóng trước khi kịp khởi chạy app để nạp cache, toàn bộ bách khoa toàn thư sẽ hoàn toàn trống rỗng.

---

## Lỗi đã biết & Cơ chế Khắc phục

* **Bị lược bỏ văn bản trên hệ sinh thái Meta (Android):**
* *Lỗi:* Khi gọi Intent chia sẻ đính kèm cả mảng byte ảnh và đoạn văn bản, ứng dụng Facebook Messenger có hành vi cố tình vứt bỏ phần văn bản, chỉ hiển thị ảnh tĩnh.
* *Khắc phục:* Bằng cách tận dụng IMGBB, ứng dụng lách luật bằng cách chia sẻ một đoạn văn bản thô chứa public URL của bức ảnh. Đối với nhu cầu chia sẻ file vật lý thuần túy, ứng dụng ép sao chép văn bản vào Clipboard và bắn Toast hướng dẫn người dùng tự dán nội dung vào khung chat.

* **Độ tương thích của Web Share API trên Desktop:**
* *Lỗi:* Hàm chia sẻ native bị trình duyệt vô hiệu hóa hoàn toàn đối với tác vụ chia sẻ file cục bộ khi chạy trên Chrome hoặc Firefox bản PC.
* *Khắc phục:* Ứng dụng triển khai cơ chế bắt lỗi catch từ luồng Promise thất bại và ngay lập tức thực thi fallback: đẩy văn bản vào Clipboard đồng thời chèn một thẻ neo ảo vào cấu trúc DOM để ép trình duyệt tải trực tiếp file ảnh JPG xuống thư mục Downloads của hệ điều hành.