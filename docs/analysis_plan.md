# KẾ HOẠCH & YÊU CẦU PHÂN TÍCH KIẾN TRÚC MÃ NGUỒN BUGSCANNER

Tài liệu này được lập ra để đóng đinh **Tiêu chuẩn chất lượng** và **Lộ trình băm nhỏ** nhằm phân tích toàn bộ 94 file mã nguồn (Kotlin/JS) của dự án BugScanner theo đúng những yêu cầu khắt khe nhất.

---

## PHẦN I: CÁC NGUYÊN TẮC TỐI THƯỢNG (TUYỆT ĐỐI TUÂN THỦ)

1. **Tuyệt đối KHÔNG ĐOÁN MÒ (No Hallucination)**: 
   - Bắt buộc phải sử dụng công cụ (`view_file`) để đọc TỪNG FILE MỘT trực tiếp từ hệ thống trước khi đưa ra bất kỳ kết luận nào.

2. **Phân tích RÃ XÁC, Mổ Xẻ Kỹ Thuật (Teardown Deep-Dive)**: 
   - Báo cáo phân tích (`analysis.md`) cấm viết theo kiểu "văn vẻ, lướt sóng, cho trẻ con đọc". 
   - Yêu cầu liệt kê ĐẦY ĐỦ: Tên `class`, `data class`, `object`, `enum`. Liệt kê chính xác tên hàm (`fun`), các tham số đầu vào (`parameters`), kiểu trả về (`return type`), và **logic thuật toán** cốt lõi nằm bên trong chúng. KHÔNG ĐƯỢC BỎ SÓT.

3. **Phân tách Đa Nền Tảng Rõ Ràng (KMP Architecture)**:
   - Đi từ gốc lên ngọn: Luôn bắt đầu đọc từ `commonMain` trước.
   - Nhận diện rạch ròi sự khác biệt giữa `androidMain` (Android native) và `webMain` / `wasmJsMain` / `jsMain` (Web ecosystem). Tập trung vào 2 nền tảng này, bỏ qua `iosMain` và `jvmMain`.

4. **Kỷ luật Cập nhật File**:
   - Mọi tiến độ phải được đánh dấu `[x]` ngay lập tức vào file `checklist.md` (nằm ở gốc dự án).
   - Mọi kết quả phân tích phải được ghi thẳng vào file `analysis.md` (nằm ở gốc dự án, tuyệt đối không tạo file ảo trong artifact).

5. **Rà soát KDoc**:
   - Nếu phát hiện KDoc (Comment) ghi không khớp với logic code thực tế, phải ghi chú lại vào file `kdoc.md` với format: Tên file - Phần cần chỉnh KDoc.

6. **Phản biện & Đề xuất (Suggestions)**:
   - Nếu có điểm nào trong mã nguồn chưa tối ưu hoặc cần cải thiện, phải ghi chép lại vào file `suggestions.md`.

---

## PHẦN II: LỘ TRÌNH "BĂM NHỎ" 94 FILES (CHUNKING PLAN)

Để đảm bảo bộ nhớ không bị quá tải và việc đọc file đạt độ chi tiết cao nhất, 94 files sẽ được chia thành 11 Giai đoạn như sau:

### Giai đoạn 1: Core, DI & Nền móng (5 files)
Đọc các file khởi tạo và quản lý Dependency Injection:
- `App.kt`, `Greeting.kt`, `Platform.kt`, `core/di/AppModule.kt`, `core/utils/TimeUtils.kt`.

### Giai đoạn 2: Tầng Domain Models (9 files)
Đọc các định dạng cấu trúc dữ liệu cốt lõi:
- `BugInfo.kt`, `ScanHistory.kt`, `OfflineScanHistory.kt`, `DetectionModels.kt`, `ChatMessage.kt`.
- Các DTO của API: `GeminiModels.kt`, `GroqModels.kt`, `INaturalistModels.kt`, `WikiModels.kt`.

### Giai đoạn 3: Tầng Remote API (4 files)
Đọc các Service gọi API Đám mây (Sử dụng Ktor):
- `GeminiApiService.kt`, `GroqApiService.kt`, `INaturalistApiService.kt`, `WikiApiService.kt`.

### Giai đoạn 4: Tầng Local Storage & Repositories (6 files)
Đọc logic lưu trữ ngoại tuyến và đồng bộ Firebase:
- `data/local/LocalStorage.kt` (và các file expect/actual của Android/Web).
- `data/model/BugInfoEntity.kt`.
- `EncyclopediaRepositoryImpl.kt`, `HistoryRepositoryImpl.kt` và các file interface tương ứng.

### Giai đoạn 5: Tầng Điều hướng & Layout Tổng (5 files)
Đọc luồng điều hướng màn hình chính:
- `ui/navigation/AppNavigation.kt`, `ui/home/HomeScreen.kt`, `ui/home/AppTabRoute.kt`.
- `ui/layout/AdaptiveLayout.kt`.

### Giai đoạn 6: Tầng UI Scan (Giao kèo CommonMain) (8 files)
Đọc logic quản lý nhận diện dùng chung:
- `ui/scan/ScanScreen.kt`, `ui/scan/components/DetectionPanel.kt`, `ui/scan/components/ScannerOverlay.kt`.
- `ui/scan/PlatformScanComponents.kt`, `ui/scan/utils/DrawUtils.kt`.
- `ui/scan/CameraPermissionScreen.kt`, `ui/scan/ScanFallbackViewModel.kt`, `ml/YoloConstants.kt`.

### Giai đoạn 7: Tầng Nhận diện Native Android (6 files)
Đọc cách Android dùng CameraX và TFLite:
- `androidMain/.../YoloDetector.kt`, `ScanViewModel.kt`.
- `AndroidCameraScreen.kt`, `AndroidStaticDetectionScreen.kt`, `AndroidScanProvider.kt`, `AndroidImagePickerHelper.kt`.

### Giai đoạn 8: Tầng Nhận diện Native Web (6 files)
Đọc cách Web dùng WebRTC và TensorFlow.js:
- `webMain/.../WebYoloDetector.kt`, `webMain/resources/yolo_helper.js`.
- `WebCameraScreen.kt`, `WebStaticDetectionScreen.kt`, `WebScanProvider.kt`, `WebImagePickerHelper.kt`.

### Giai đoạn 9: Tầng UI Chatbot Trợ lý Ảo (4 files)
Đọc giao diện và logic gọi LLM:
- `ui/chat/ChatScreen.kt`, `ChatViewModel.kt`, `ChatComponents.kt`, `ChatPromptSuggestions.kt`.

### Giai đoạn 10: Tầng UI Chi tiết & Từ điển (4 files)
Đọc màn hình hiển thị sâu bệnh chi tiết:
- `ui/detail/BugDetailScreen.kt`, `BugDetailViewModel.kt`.
- `ui/encyclopedia/EncyclopediaScreen.kt`, `EncyclopediaViewModel.kt`.

### Giai đoạn 11: Tầng UI Lịch sử, Xác thực & Components lẻ (Các file còn lại)
Đoạn cuối cùng vét sạch các file UI phụ trợ:
- Các file Auth (`AuthScreen`, `AuthViewModel`, `AuthValidation`).
- Màn hình Splash và History (`HistoryScreen`, `HistoryViewModel`).
- Components dùng chung (`BugImage`, `BugItemCard`, `RequireAuthScreen`...).
- Theme & Utils nhỏ lặt vặt.

---

**CƠ CHẾ THỰC THI**: Tôi sẽ tuần tự đọc từng Giai đoạn, đánh dấu `checklist.md`, ghi cực kỳ chi tiết vào `analysis.md` và xin phép bạn trước khi nhảy sang Giai đoạn tiếp theo.
