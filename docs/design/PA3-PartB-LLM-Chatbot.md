# PA3 Phần B - Huấn luyện, triển khai và đánh giá mô hình LLM

Phần này mô tả thành phần LLM của BugScanner. Trong hệ thống, mô hình thị giác máy tính được dùng để phát hiện và phân loại côn trùng từ ảnh/camera, còn phần này chỉ tập trung vào mô hình chatbot dùng để trả lời câu hỏi của người dùng về côn trùng, thiên nhiên và gợi ý xử lý.

## 1. Mô hình được chọn

Chatbot sử dụng Google Gemini 2.5 Flash thông qua Google Generative Language API. Trong ứng dụng hiện tại, mô hình được gọi bởi `GeminiApiService`, gửi request đến endpoint:

```text
https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent
```

Gemini 2.5 Flash được chọn vì có tốc độ phản hồi nhanh, hỗ trợ tốt hội thoại tiếng Việt và phù hợp với vai trò trợ lý trong ứng dụng di động/web. Chatbot không phải là mô hình được nhóm tự huấn luyện cục bộ. Đây là mô hình nền tảng đã được huấn luyện sẵn và được nhà cung cấp triển khai trên cloud; BugScanner điều khiển hành vi của mô hình thông qua system prompt, cấu trúc request và logic trong ứng dụng.

## 2. Triển khai và vận hành

LLM được triển khai như một dịch vụ cloud bên ngoài, không phải là file model được đóng gói trực tiếp trong ứng dụng. BugScanner triển khai phần tích hợp chatbot một cách tĩnh trong ứng dụng Kotlin Multiplatform, còn mô hình Gemini thật sự được vận hành động trên hạ tầng API của Google.

Luồng vận hành khi người dùng sử dụng chatbot:

1. Người dùng mở tab Chatbot hoặc nhấn nút "Hỏi BugScanner AI" từ màn hình chi tiết côn trùng.
2. `ChatScreen` nhận nội dung người dùng nhập và gọi `ChatViewModel.sendMessage(...)`.
3. `ChatViewModel` thêm tin nhắn của người dùng vào lịch sử hội thoại lưu trong bộ nhớ.
4. ViewModel tạo một `GeminiRequest` gồm:
   - system instruction định nghĩa BugScanner AI là trợ lý chuyên nghiệp về sinh học và côn trùng học,
   - lịch sử hội thoại dưới dạng các message có vai trò `user` và `model`.
5. `GeminiApiService` gửi request bằng Ktor `HttpClient` dùng chung của ứng dụng.
6. Ứng dụng lấy câu trả lời đầu tiên từ danh sách candidate của Gemini và hiển thị dưới dạng tin nhắn của trợ lý.
7. Nếu request mạng hoặc API thất bại, UI hiển thị một bong bóng tin nhắn lỗi thay vì làm ứng dụng bị crash.

API key của Gemini được cấu hình thông qua Gradle BuildConfig, lấy từ biến môi trường hoặc file `local.properties`. Cách này phù hợp cho prototype ở PA3, nhưng khi triển khai production, API key nên được bảo vệ bằng backend proxy để tránh nhúng trực tiếp key vào bản build client.

## 3. Triển khai tĩnh hay động

Cách triển khai chatbot là dạng kết hợp:

- Phần tĩnh trong ứng dụng: API client, endpoint mô hình, DTO request/response, system instruction, xử lý trạng thái UI và xử lý lỗi đều được biên dịch vào ứng dụng.
- Phần động khi vận hành: trọng số mô hình LLM và dịch vụ inference được Google host, nên khả năng phục vụ, cập nhật runtime và hạ tầng chạy mô hình do Google quản lý.

Nhóm không đóng gói trọng số Gemini vào ứng dụng Android hoặc Web. Điều này giúp ứng dụng nhẹ hơn và không yêu cầu thiết bị người dùng có GPU hoặc phần cứng tăng tốc cho chatbot.

## 4. Huấn luyện lại, fine-tune, chỉnh sửa và tái triển khai

Chatbot hiện tại không thực hiện huấn luyện lại hoặc fine-tune mô hình. Thay vào đó, việc cải thiện được thực hiện bằng cách chỉnh sửa prompt, đánh giá lại và tái triển khai ứng dụng.

Quy trình chỉnh sửa đề xuất:

1. Thu thập các đoạn hội thoại mà chatbot trả lời sai, thiếu chính xác hoặc chất lượng thấp từ quá trình test của nhóm và phản hồi người dùng.
2. Phân loại lỗi thành các nhóm như sai kiến thức, lời khuyên chưa an toàn, trả lời không đúng trọng tâm, diễn đạt tiếng Việt chưa rõ, giữ ngữ cảnh kém hoặc lỗi API.
3. Cập nhật system instruction và quy tắc tạo prompt trong `ChatViewModel`.
4. Chạy lại bộ dữ liệu đánh giá chatbot được mô tả ở phần dưới.
5. Nếu các ngưỡng chấp nhận được đáp ứng, build và triển khai lại ứng dụng.

Fine-tuning có thể được xem xét trong tương lai nếu nhóm thu thập đủ dữ liệu hỏi-đáp chất lượng cao trong miền côn trùng học. Với phạm vi PA3, điều khiển mô hình bằng prompt là phương án phù hợp hơn vì đơn giản, ít tốn chi phí và sát với mức độ hoàn thiện hiện tại của dự án.

## 5. Bộ dữ liệu đánh giá

Bộ dữ liệu đánh giá chatbot nên chứa các câu hỏi đại diện cho những tình huống sử dụng chính của BugScanner. Bộ đánh giá đề xuất cho PA3 gồm 60 prompt, chia thành các nhóm sau:

| Nhóm câu hỏi | Số lượng prompt | Mục đích |
|---|---:|---|
| Kiến thức chung về côn trùng | 15 | Kiểm tra khả năng giải thích kiến thức sinh học về côn trùng. |
| Câu hỏi tiếp nối sau khi phát hiện côn trùng | 15 | Kiểm tra câu trả lời khi người dùng hỏi thêm về một loài vừa được phát hiện. |
| Xử lý và phòng ngừa | 10 | Kiểm tra lời khuyên về kiểm soát côn trùng có hữu ích và an toàn không. |
| Chất lượng hội thoại tiếng Việt | 10 | Kiểm tra câu trả lời có tự nhiên, ngắn gọn và dễ hiểu bằng tiếng Việt không. |
| Ngữ cảnh nhiều lượt hỏi | 5 | Kiểm tra chatbot có sử dụng đúng thông tin từ các tin nhắn trước không. |
| Câu hỏi ngoài miền hoặc không an toàn | 5 | Kiểm tra chatbot có tránh trả lời lan man hoặc đưa ra hướng dẫn không phù hợp không. |

Nguồn dữ liệu:

- Câu hỏi được xây dựng từ các use case chính của dự án: quét côn trùng, xem chi tiết côn trùng và hỏi chatbot.
- Chủ đề về loài côn trùng và cách xử lý lấy từ luồng encyclopedia của BugScanner, thông tin côn trùng dựa trên iNaturalist và các tài liệu tham khảo công khai.
- Các tình huống biên do nhóm tự viết, ví dụ câu hỏi mơ hồ, câu hỏi nối tiếp nhiều lượt và câu hỏi không liên quan đến miền ứng dụng.

Mỗi prompt nên có ghi chú câu trả lời mong đợi hoặc rubric chấm điểm, không nhất thiết phải có một đáp án cố định duy nhất, vì câu trả lời của LLM có thể thay đổi nhưng vẫn chấp nhận được.

## 6. Metrics đánh giá và ngưỡng chấp nhận

| Metric | Cách đo | Ngưỡng chấp nhận |
|---|---|---:|
| Độ đúng kiến thức | Người đánh giá chấm theo thang 1-5 dựa trên tài liệu tham khảo đáng tin cậy. | Trung bình >= 4.0/5 |
| Mức độ liên quan | Tỷ lệ câu trả lời giải quyết đúng câu hỏi của người dùng. | >= 85% |
| An toàn của lời khuyên | Tỷ lệ câu trả lời về xử lý/phòng ngừa không đưa hướng dẫn nguy hiểm hoặc quá tự tin. | >= 90% |
| Độ rõ ràng tiếng Việt | Người đánh giá chấm ngữ pháp, giọng văn và độ dễ hiểu. | Trung bình >= 4.0/5 |
| Khả năng giữ ngữ cảnh | Tỷ lệ prompt nhiều lượt mà chatbot sử dụng đúng thông tin trước đó. | >= 80% |
| Độ trễ phản hồi | Thời gian từ lúc người dùng gửi câu hỏi đến khi câu trả lời hiển thị trong điều kiện mạng bình thường. | Trung bình <= 5 giây |
| Khả dụng và xử lý lỗi | API lỗi hoặc mất mạng phải hiển thị lỗi rõ ràng cho người dùng thay vì crash. | 100% ca test lỗi |

## 7. Trạng thái đánh giá ban đầu

Hiện tại phần triển khai đã đáp ứng các yêu cầu chức năng cơ bản của chatbot LLM:

| Yêu cầu | Trạng thái hiện tại |
|---|---|
| Có giao diện chat trong ứng dụng. | Đã triển khai trong `ChatScreen`. |
| Người dùng có thể gửi tin nhắn đến LLM. | Đã triển khai qua `ChatViewModel.sendMessage`. |
| Lịch sử hội thoại được giữ trong vòng đời của ViewModel. | Đã triển khai bằng `chatHistory`. |
| Có system instruction theo miền ứng dụng. | Đã đưa vào request gửi đến Gemini. |
| Câu trả lời của LLM được hiển thị trong UI chat. | Đã triển khai bằng `ChatBubble`. |
| Có trạng thái loading khi chờ phản hồi. | Đã triển khai bằng `TypingIndicator`. |
| Có xử lý lỗi API hoặc lỗi mạng. | Đã triển khai bằng tin nhắn lỗi trong chat. |
| Đánh giá định lượng bằng dataset. | Chưa có kết quả được commit trong repository; cần chạy bộ 60 prompt ở trên trước khi nộp PA3 cuối cùng. |

Dựa trên việc đọc code, chatbot đã sẵn sàng cho bước đánh giá thủ công ở PA3. Phần còn lại là chạy bộ prompt đề xuất, ghi lại điểm số theo từng metric và đưa bảng kết quả cuối cùng vào SAD.

## 8. Hạn chế và rủi ro

- Chatbot phụ thuộc vào kết nối internet và độ khả dụng của Gemini API.
- API key hiện được đưa vào BuildConfig của client; khi triển khai production nên bảo vệ bằng backend service.
- Mô hình có thể sinh thông tin sai hoặc lời khuyên xử lý côn trùng chưa chính xác, nên kết quả cần được kiểm tra với nguồn đáng tin cậy.
- Lịch sử chat chỉ được lưu trong bộ nhớ và không được giữ lại sau khi ứng dụng khởi động lại.
- Hiện tại chatbot chưa truy xuất trực tiếp từ một cơ sở dữ liệu côn trùng đã kiểm chứng trước khi trả lời. Nếu bổ sung retrieval-augmented generation, độ tin cậy kiến thức sẽ tốt hơn.

## 9. Kế hoạch tái triển khai

Khi cần chỉnh sửa prompt hoặc tích hợp API chatbot, nhóm sẽ:

1. Cập nhật prompt hoặc logic tạo request trong shared Kotlin code.
2. Chạy bộ dữ liệu đánh giá chatbot.
3. Xác nhận tất cả ngưỡng chấp nhận đều đạt.
4. Build và triển khai lại ứng dụng Android/Web.
5. Theo dõi phản hồi người dùng và thu thập thêm các ví dụ trả lời lỗi cho chu kỳ cải thiện tiếp theo.

