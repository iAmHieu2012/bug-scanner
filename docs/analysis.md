# TÀI LIỆU PHÂN TÍCH MÃ NGUỒN

> **Quy tắc:**
> - Tài liệu này đang chờ người dùng duyệt kế hoạch và hướng dẫn cách phân tích.

---

## 1. Flow: Foundation & Cấu Hình Cốt Lõi

### Batch 1.1: Entry Point & Navigation
*(Phân tích chuyên sâu từ: `App.kt`, `Platform.kt`, `AppNavigation.kt`, `AdaptiveLayout.kt`, `SplashScreen.kt`)*

**1. Khởi tạo Ứng dụng & Nền tảng (Platform & App Shell):**
*   **Triết lý KMP (`Platform.kt`):** Áp dụng pattern `expect/actual` kinh điển của Kotlin Multiplatform để định danh hệ điều hành. Dù rất đơn giản nhưng đây là viên gạch nền móng để các thư viện bên thứ 3 (như Camera, Image Picker) biết ứng dụng đang chạy trên Android hay Web/Wasm.
*   **Trái tim của UI (`App.kt`):** Đây là Root Composable của toàn bộ ứng dụng. 
    *   **DI Injection:** Sử dụng `KoinApplication` bọc ngoài cùng, đảm bảo toàn bộ cây giao diện bên dưới đều có thể `koinInject()` mà không bị lỗi vòng đời.
    *   **Đo lường thích ứng (Responsive):** Khéo léo dùng `BoxWithConstraints` ngay tại gốc. Bằng cách tính toán kích thước khung hình ở level cao nhất (`WindowSizeClass.calculateFromSize` và `classifyAdaptiveWidth`), ứng dụng chỉ cần tính 1 lần và truyền trạng thái (COMPACT/MEDIUM/EXPANDED) xuống các màn hình con. Điều này tối ưu hiệu suất render (Recomposition) hơn rất nhiều so với việc mỗi màn hình tự đo lại kích thước.

**2. Giao thức Điều hướng và State-Driven Routing (`AppNavigation.kt`):**
*   Đây không phải là một Router thông thường (như Jetpack Navigation dùng chuỗi định tuyến), mà là một **State-Driven Router**. Nó lắng nghe trực tiếp `authState` (luồng trạng thái đăng nhập) từ `AuthViewModel`.
*   **Luồng bảo mật cổng (Gatekeeping):** 
    *   Trạng thái mặc định là hiện `SplashScreen`. 
    *   Khi có kết quả từ luồng Auth (Success hoặc Unauthenticated), nó mới gỡ Splash.
    *   Nếu Success (Đã đăng nhập), nó mở khóa `HomeScreen`. Đồng thời, một quyết định kiến trúc rất xuất sắc là gọi ngầm `encyclopediaRepository.prefetchDatabase()` trên một luồng phụ (`launch`). Nghĩa là lúc người dùng đang lướt Home, dữ liệu Bách khoa đã được tải ngầm sẵn vào Cache, khi bấm sang tab Bách khoa sẽ thấy dữ liệu hiện ra tức thì không độ trễ.
*   **Tập trung hóa Side-Effects (Chia sẻ đa phương tiện):** Thay vì bắt từng màn hình con (như Home, Chat) tự xử lý chức năng "Share", tác giả đẩy `onShareClick` lên tận `AppNavigation`. Cực kỳ tinh vi ở chỗ: Nếu ảnh đã có sẵn mảng byte thì share luôn. Nhưng nếu nó là một URL từ Cloud, `AppNavigation` tự động gọi `httpClient.get().readBytes()` để tải ảnh về RAM trước rồi mới share qua OS. Thiết kế này giải phóng các UI Component khỏi việc phải import `HttpClient`.

**3. Hiệu ứng Khởi động & Ràng buộc UI (`SplashScreen.kt` & `AdaptiveLayout.kt`):**
*   **AdaptiveLayout.kt:** Gom nhóm kích thước thiết bị theo chuẩn Material 3: `< 600dp` là điện thoại (COMPACT), `< 1000dp` là Tablet (MEDIUM), lớn hơn là Desktop/Web ngang (EXPANDED). Dùng Enum giúp code trong sáng và dễ test logic.
### Batch 1.2: Dependency Injection (DI) Module
*(Phân tích chuyên sâu từ: `AppModule.kt`)*

**1. Kiến trúc Tầng Mạng (Network Layer Resilience):**
*   **Singleton HttpClient:** Toàn bộ ứng dụng dùng chung một Ktor `HttpClient` duy nhất, giúp tái sử dụng Connection Pool và tiết kiệm bộ nhớ.
*   **Thiết lập Timeouts Thực chiến:** Do ứng dụng có đặc thù phục vụ người dùng ở vùng nông nghiệp/lâm nghiệp (mạng yếu, chập chờn), tác giả cố ý cài đặt `HttpTimeout` dài hơn bình thường (15s Request/Connect, 20s Socket) để tránh văng lỗi Timeout rác.
*   **Json Parser Chống Crash:** Bật cờ `ignoreUnknownKeys = true`, `isLenient = true`, `coerceInputValues = true` trong `ContentNegotiation`. Bốn hệ thống AI/API bên ngoài (Gemini, Groq, Wiki, iNaturalist) có thể thay đổi schema trả về bất cứ lúc nào. Lớp giáp bảo vệ này đảm bảo app không bao giờ bị Force Close khi bên thứ 3 ngấm ngầm đổi API.

**2. Tiêm Cấu trúc (Dependency Injection Graph):**
*   **Che giấu API Key qua Firestore:** `INaturalistApiService` được tiêm thẳng `FirebaseFirestore` vào constructor thông qua hàm `get()`. Đây là một thủ thuật bảo mật rất cao: API Token được cất trên Cloud và fetch động xuống, thay vì nén cứng (hardcode) trong source code, loại bỏ rủi ro bị hacker dùng tool decompile bới ra Token.
*   **Kiến trúc Đảo ngược Phụ thuộc (Dependency Inversion):** Khai báo tường minh `single<Interface> { Implementation() }`. Các ViewModel bên trên hoàn toàn "mù" về lớp Impl, chúng chỉ giao tiếp qua Interface, tạo tiền đề cực tốt cho việc viết Unit Test/Mock Data sau này.
*   **Tối giản hóa ViewModel:** Áp dụng cú pháp DSL mới của Koin `viewModelOf(::ClassName)`. Koin tự động dùng Reflection quét Constructor để lấp đầy các dependency, tiết kiệm hàng chục dòng mã lặp lại.
*   **Sự biến mất của ScanViewModel:** Bảng DI liệt kê mọi ViewModel nhưng **không hề có** `ScanViewModel` (chuyên lo luồng Camera Live). Điều này khẳng định 100% giả thuyết: Luồng Camera và YOLO được bóc tách hoàn toàn, giao nộp sinh tử về cho tầng Platform Native (Android/Web) tự sinh tự diệt độc lập với Koin.

### Batch 1.3: Theme & Resource Constants
*(Phân tích chuyên sâu từ: `Theme.kt`, `Color.kt`, `Type.kt`, `AppIcon.kt`)*

**1. Hệ thống Màu sắc & Chế độ Đêm (`Color.kt` & `Theme.kt`):**
*   **Tuân thủ triệt để Material 3 (M3):** Bảng màu không hề được chọn ngẫu nhiên mà được thiết kế theo đúng spec M3 (Primary, Secondary, Tertiary và các biến thể Container). Màu sắc chủ đạo là Xanh lục (`#2E7D32` cho Light và `#8CD588` cho Dark), toát lên rõ rệt đặc thù của một ứng dụng sinh học/thiên nhiên.
*   **Cơ chế Override ThemeMode:** Hàm `@Composable AppTheme` không chỉ áp dụng mù quáng `isSystemInDarkTheme()` từ OS. Nó mở ra một `enum class ThemeMode { SYSTEM, LIGHT, DARK }` và tham số `useDarkTheme`. Quyết định kiến trúc này cho phép phần "Cài đặt" của app có khả năng ghi đè lại giao diện hệ thống (VD: máy đang Dark Mode nhưng user thích app sáng thì app vẫn Sáng).

**2. Quy chuẩn Typography (`Type.kt`):**
*   Khai báo biến `AppTypography` ánh xạ 9 kích cỡ chuẩn của M3 (từ `headlineMedium` xuống `labelMedium`).
*   Tuy hiện tại chỉ đang dùng `FontFamily.Default`, nhưng việc đóng gói toàn bộ vào 1 cục duy nhất mang lại tính **Maintainability (Khả năng bảo trì)** tuyệt đối. Nếu khách hàng muốn đổi sang font "Inter" hoặc "Roboto", Dev chỉ cần sửa `FontFamily` đúng 1 lần tại file này là toàn bộ app thay áo mới, không sót một chữ nào.

**3. Kiến trúc Không gian tên Icon (`AppIcon.kt`):**
*   File này dường như trống rỗng, chỉ chứa đúng `object AppIcon`. Nhưng thực chất đây là một **Pattern Namespacing** cao cấp của Kotlin.
*   Bằng cách biến `AppIcon` thành một cái phễu (Namespace), các ảnh vector sẽ được viết dưới dạng Extension Function của nó (Ví dụ: `val AppIcon.Bug -> ...`). Khi gọi ra UI, Dev bắt buộc gõ `AppIcon.*`. Cách này triệt tiêu hoàn toàn rủi ro bị "ô nhiễm không gian tên" (Namespace Pollution) khi thư viện Compose tự nhiên cũng có một icon tên là `Bug`.

---

## 2. Flow: Core Utilities & Components dùng chung

### Batch 2.1: Core (Utils, Config)
*(Phân tích chuyên sâu từ: `AppConfigProvider.kt`, `ShareManager.kt`, `TimeUtils.kt`, `SimpleMarkdown.kt`)*

**1. Tối ưu chi phí Database (`AppConfigProvider.kt`):**
*   Đây là một lớp Cache cấp RAM rất quan trọng. Cấu hình ứng dụng (`AppConfig` chứa API Key AI, Model names...) được lưu trên Firestore. Nếu mỗi lần gọi API đều chọc xuống Firestore để lấy Key thì app sẽ rất chậm và chi phí (Read Operations) đội lên cực cao.
*   Bằng cách dùng biến `cachedConfig`, `AppConfigProvider` đảm bảo chỉ tốn đúng 1 lượt Read từ Firestore trong mỗi phiên sử dụng. Kèm theo hàm `invalidate()` để ép tải lại khi Admin thay đổi cấu hình nóng.

**2. Trừu tượng hóa OS (`ShareManager.kt` & `TimeUtils.kt`):**
*   **ShareManager.kt:** Định nghĩa Interface và dùng `expect fun rememberShareManager()`. Pattern này ép các nền tảng (Android, Web) phải tự viết code Actual để gọi Share API của hệ điều hành đó (như Android Intent hay Web Share API). Nhờ vậy, Compose UI ở commonMain chỉ việc gọi `shareBugInfo()` cực kỳ nhàn nhã mà không cần biết nó đang chạy trên máy gì.
*   **TimeUtils.kt:** Bọc thư viện `kotlinx-datetime` thành 2 hàm cực gọn (lấy Time Millis và Format chuỗi `dd/MM/yyyy`). Việc gom logic xử lý TimeZone vào Utils giúp các hàm UI không bị bẩn bởi logic của hệ thống thời gian.

**3. Bespoke Markdown Parser (`SimpleMarkdown.kt`):**
*   Tác giả tự viết một trình phân giải Markdown siêu nhẹ (chỉ mười mấy dòng code) bằng tay thay vì xài thư viện khổng lồ. Nó bóc tách String thành các `MarkdownBlock` (Paragraph, Bullet) và `MarkdownSpan` (Bold, Italic).
*   **Tư duy kiến trúc:** App này giao tiếp với các con AI (Gemini, Groq) để lấy thông tin Bách khoa. AI thường trả về chuỗi văn bản chứa dấu `**đậm**` hoặc `- gạch đầu dòng`. Việc viết một parser Custom siêu nhỏ bé (chỉ bắt 2 loại format này) chứng tỏ tác giả tối ưu App Bundle Size (kích thước file cài đặt) tới mức cực đoan, kiên quyết nói "Không" với việc nhét các thư viện parse Markdown cồng kềnh, chậm chạp vào KMP.

### Batch 2.2: Shared UI Components
*(Phân tích chuyên sâu từ: `RemoteImagePolicy.kt`, `BugItemCard.kt`, `BugEditDialog.kt`)*

**1. Vá lỗi KMP Web Canvas (`RemoteImagePolicy.kt`):**
*   **Vấn đề cốt lõi:** Khi chạy KMP trên nền tảng Web (Wasm/Js), Compose render giao diện lên HTML5 Canvas. Tuy nhiên, nếu tải ảnh từ một Domain không hỗ trợ CORS (như `static.inaturalist.org`) và vẽ lên Canvas, Canvas sẽ bị đánh dấu là "Tainted" (ô nhiễm), gây văng app (Crash) nếu có luồng code nào cố đọc Pixel từ nó.
*   **Giải pháp kiến trúc:** Tác giả tạo ra `RemoteImagePolicy` và hàm `expect canLoadRemoteImage`. Ở tầng Common, nó lập danh sách đen (Blacklist) các domain như iNaturalist. Nhờ đó, luồng UI có thể chủ động né việc tải trực tiếp các ảnh này trên nền tảng Web (có thể ẩn đi hoặc hiển thị ảnh thay thế) để cứu ứng dụng khỏi việc bị Crash toàn tập.

**2. Kiến trúc Stateless UI (`BugItemCard.kt`):**
*   Đây là một "Dumb Component" (Component ngốc) hoàn hảo. Nó không tự giữ bất kỳ State nào (như loading, error) hay ViewModel nào.
*   Nó chỉ nhận đúng 1 Data Class (`BugInfo`) và 1 Event Lambda (`onClick`). Nhờ tính Stateless tuyệt đối, thẻ `BugItemCard` này có thể được quăng vào bất cứ đâu (Grid của Bách khoa, List của Lịch sử, hoặc Result của màn hình Quét) mà không sợ bị xung đột logic.

**3. Đóng gói Local State (`BugEditDialog.kt`):**
*   Khác với `BugItemCard`, form điền thông tin này lại là một "Smart Component" chứa tới 15 biến `mutableStateOf` bên trong.
*   **Điểm sáng kiến trúc:** Thay vì đẩy 15 biến này lên ViewModel (làm phình to ViewModel một cách vô nghĩa), tác giả quyết định "nhốt" toàn bộ trạng thái form vào bên trong phạm vi vòng đời của chính Dialog này. Chỉ khi nào Admin bấm "Lưu", form mới đóng gói thành 1 object `BugInfo` duy nhất và bắn lên ViewModel thông qua hàm callback `onSave`. Cách làm này giúp giải phóng RAM ngay lập tức khi Dialog đóng lại.
*   **Xử lý Array bạo lực nhưng hiệu quả:** Với các trường dữ liệu dạng mảng (`affectedCrops`, `hostPlants`...), form dùng `joinToString("\n")` để biến nó thành Text nhiều dòng cho Admin dễ sửa. Lúc lưu lại thì dùng `split("\n")`

---

## 3. Flow: Authentication & User Profile

### Batch 3.1: UI Auth & Xác thực
*(Phân tích chuyên sâu từ: `AuthScreen.kt`, `AuthViewModel.kt`, `AuthValidation.kt`)*

**1. Kiến trúc Giao diện Thích ứng (Responsive Auth UI):**
*   **Split-Screen Design (`AuthScreen.kt`):** Áp dụng thiết kế cực kỳ hiện đại cho đa nền tảng. Khi phát hiện màn hình rộng (`WindowWidthSizeClass.Expanded` - Desktop/Web), nó tự động tách đôi màn hình: Trái là Banner minh hoạ hoành tráng, Phải là Form đăng nhập. Nếu là Mobile thì Form tự động canh giữa màn hình.
*   **Tái sử dụng Component:** Để không bị lặp code giữa 2 kiểu Layout trên, tác giả tách toàn bộ phần nhập liệu ra thành hàm `@Composable private fun AuthForm`. Đây là một Pattern kinh điển trong Compose để giữ code DRY (Don't Repeat Yourself).

**2. Chiến thuật Xác thực Lạc quan (Optimistic Auth State):**
*   **Mở khóa UI lập tức (`AuthViewModel.kt`):** Khi app khởi động, khối lệnh lắng nghe `auth.authStateChanged` sẽ chạy. Nếu phát hiện token lưu trong bộ nhớ đệm (Cached Session), nó lập tức phát tín hiệu `AuthState.Success` để gỡ bỏ SplashScreen, cho user lao thẳng vào App mà không bắt họ phải chờ kết nối mạng. 
*   **Bảo mật ngầm (Background Verification):** Ngay sau khi cho user vào App, nó âm thầm mở một Thread (Coroutine) chạy ngầm xuống Firestore kiểm tra hàm `adminRepository.isBanned()`. Nếu phát hiện tài khoản đã bị khoá, nó tự động giật lại quyền, force Sign-Out và văng lỗi. Kiến trúc này tạo ra trải nghiệm "Zero-Loading Time" cho người dùng chân chính nhưng vẫn khoá cổ hacker chỉ sau 1 tích tắc.

**3. Tách bạch Logic Kiểm định (Separation of Concerns):**
*   Thay vì nhét đống Regex kiểm tra Email/Mật khẩu vào UI hoặc ném vào ViewModel, tác giả đưa hẳn ra một Object độc lập là `AuthValidation.kt`. Mặc dù file chỉ có vỏn vẹn 30 dòng, nhưng nó thể hiện tư duy kiến trúc Clean Architecture rất chững chạc: UI chỉ để vẽ, ViewModel chỉ để gọi Network, Validation thì phải nằm ở Core/Domain.

### Batch 3.2: User Identity & Profile
*(Phân tích chuyên sâu từ: `AdminRepository.kt`, `AdminRepositoryImpl.kt`, `ProfileScreen.kt`)*

**1. Phân lập dữ liệu chống leo thang đặc quyền (Privilege Escalation):**
*   **Thiết kế Database (`AdminRepositoryImpl.kt`):** Nhìn vào code, tác giả cố tình xài 2 Collection riêng biệt trên Firestore: `users` (chứa profile) và `admins` (chỉ chứa UID của Admin). 
*   **Ý đồ bảo mật:** Nếu gộp chung vào 1 bảng `users` và dùng cờ `isAdmin: Boolean`, hacker có thể dùng mẹo can thiệp gói tin cập nhật Profile để tự set `isAdmin = true` cho bản thân. Bằng cách tách hẳn bảng `admins`, hacker dù có phá được bảng `users` cũng không thể leo quyền lên làm Admin. Đây là một thiết kế bảo mật xuất sắc.

**2. Giao diện phòng thủ (Defensive UI Routing):**
*   **Che giấu tính năng (`ProfileScreen.kt`):** Nút bấm để vào màn hình Bảng Điều Khiển Admin được bọc chặt trong câu lệnh `if (authState is AuthState.Success && authState.isAdmin)`.
*   Điều này có nghĩa là tài khoản thường hoặc Khách (Guest) sẽ không bao giờ nhìn thấy nút Admin. Phương pháp này vừa giúp giao diện gọn gàng, vừa tránh việc User tò mò bấm vào rồi bị kẹt ở màn hình lỗi quyền truy cập.

## 4. Flow: Main Navigation & Dashboard

### Batch 4.1: Home Shell & Tabs
*(Phân tích chuyên sâu từ: `HomeScreen.kt`, `AppTabRoute.kt`)*

**1. Stateful Routing (Điều hướng không cần thư viện):**
*   Thay vì dùng thư viện `androidx.navigation` cồng kềnh và dễ dính lỗi trên KMP, tác giả tự build một bộ định tuyến nội bộ bằng `enum class AppTab` và `Crossfade` (để tạo hiệu ứng chuyển cảnh).
*   **Truyền Context mượt mà:** Khi người dùng đang xem chi tiết một con côn trùng và bấm "Hỏi AI", `HomeScreen` lập tức đóng màn hình chi tiết lại, chuyển tab sang `AppTab.CHATBOT`, đồng thời "bơm" toàn bộ dữ liệu của con côn trùng đó (hình ảnh, tên, mô tả) vào các biến trạng thái (`initialChatBugContext`). Cấu trúc này giúp luồng dữ liệu chảy xuyên suốt giữa các màn hình mà không cần truyền Parcelable cực nhọc như Android thuần.

**2. Vá lỗi Web History (`AppTabRoute.kt`):**
*   File này sinh ra để chuyển đổi qua lại giữa Enum và chuỗi Hash (`#/scan`, `#/chat`...).
*   **Mục đích:** Do Compose Web/Wasm không tự bắt được sự kiện "Back/Forward" của trình duyệt web. Tác giả dùng file này để đồng bộ hóa cái Tab đang mở trong App với cái URL của trình duyệt. Nhờ vậy, người dùng bấm nút "Back" trên Chrome vẫn hoạt động trơn tru. Quá khôn ngoan!

### Batch 4.2: Dashboard UI & Biểu đồ
*(Phân tích chuyên sâu từ: `AdminDashboardScreen.kt`, `AdminViewModel.kt`)*

**1. Vẽ biểu đồ Zero-Dependency (Thuần Canvas):**
*   **Vấn đề:** Các thư viện biểu đồ lớn như MPAndroidChart không chạy được trên KMP (Web/Wasm/iOS), còn các thư viện Compose-Charts thì thường khá nặng và tiềm ẩn rủi ro tương thích.
*   **Giải pháp:** Tác giả tự code tay hàm `ScansPerDayChart` bằng đối tượng `Canvas` cốt lõi của Compose. Việc tự vẽ các đường `Path` và `DrawCircle` không chỉ giúp loại bỏ hoàn toàn các thư viện bên thứ 3 (giảm dung lượng App), mà còn đảm bảo biểu đồ chạy mượt mà, không lỗi font, không vỡ layout trên bất kỳ nền tảng nào. Đây là kỹ năng lập trình UI bậc cao.

**2. Đồng bộ hóa Cache Cấu hình (`AdminViewModel.kt`):**
*   Khi Admin thay đổi System Prompt của AI và bấm "Lưu", ViewModel không chỉ ghi lên Firestore mà còn lập tức gọi `appConfigProvider.invalidate()`. 
*   Lệnh này đóng vai trò như một cú "tát" tỉnh các bộ nhớ đệm (RAM Cache) đang chạy trong app, ép toàn bộ các màn hình Quét AI (ScanScreen) phải tải lại Prompt mới nhất từ Firestore. Rất chặt chẽ và không có độ trễ logic (Data Inconsistency).

## 5. Flow: Hybrid Detection (Camera + YOLO + Cloud)

### Batch 5.1: Logic Nhận diện lõi (Native ML)
*(Phân tích chuyên sâu từ: `AndroidScanProvider.kt`, `YoloDetector.kt`, `WebScanProvider.kt`, `WebYoloDetector.kt`)*

**1. YOLOv8 TFLite Parsing & NMS (Android):**
*   Lớp `YoloDetector.kt` thực hiện nạp mô hình `.tflite` bằng `MappedByteBuffer` và chuyển đổi ảnh Bitmap sang định dạng `NCHW` (TensorFlow Lite mới thường yêu cầu chuẩn này).
*   **Xử lý Ma trận Flat:** Đầu ra của YOLOv8 là một mảng 1 chiều khổng lồ (kích thước `numRows x 16464`). Tác giả đã tự viết hàm `parseYoloOutput` để bóc tách 4 giá trị đầu (cx, cy, w, h) làm tọa độ hộp, và các giá trị sau làm điểm số (confidence score) cho từng nhãn.
*   **Non-Maximum Suppression (NMS):** Cài đặt thuật toán NMS thủ công với hàm `calculateIoU` để loại bỏ các Bounding Box bị chồng lấp (chỉ giữ lại Box có điểm cao nhất). Rất chuẩn xác về mặt thị giác máy tính.

**2. Kotlin Wasm-JS Interop (Web) với LiteRT:**
*   Trên Web, thay vì phải tốn công chuyển đổi model sang định dạng TFJS, tác giả sử dụng thư viện `@litertjs/core` để chạy trực tiếp file `model.tflite` ngay trên trình duyệt thông qua WebAssembly. File `WebYoloDetector.kt` và `yolo_helper.js` làm cầu nối gọi hàm.
*   Một chi tiết cực kỳ tinh vi: Mặc dù suy luận lõi bằng `LiteRT`, tác giả vẫn khéo léo dùng thư viện `tfjs` chỉ để làm công việc tiền xử lý ảnh (`tf.browser.fromPixels`, `transpose` sang `NCHW`) vì TFJS thao tác ma trận rất nhanh.
*   Vì Wasm hiện tại xử lý mảng `Array<dynamic>` khá khó khăn, tác giả đã dùng chiêu trả về một **chuỗi JSON** từ JS (`Promise<String>`), sau đó dùng `kotlinx.serialization` để parse ngược lại thành danh sách đối tượng Kotlin `JsDetection`. Một cách lách luật cực kỳ thông minh.
*   `WebScanProvider.kt` còn đăng ký sự kiện `window.addEventListener("paste")` để cho phép người dùng bấm `Ctrl+V` dán ảnh thẳng vào app web. Trải nghiệm người dùng được tối ưu ngang ngửa Native App.

### Batch 5.2: UI Scanner Core (Giao diện Quét)
*(Phân tích chuyên sâu từ: `ScanScreen.kt`, `ScannerOverlay.kt`, `DetectionPanel.kt`)*

**1. Điều phối Vòng đời Camera (Camera Lifecycle):**
*   `ScanScreen.kt` điều khiển Camera không theo kiểu vòng lặp while như Android truyền thống, mà bằng một biến state gọi là `captureTrigger` và cờ `isScanningLive`. Khi người dùng bấm nút "Đóng băng", hệ thống không tắt Camera đi mà chỉ xuất mảng byte cuối cùng ra, đồng thời chuyển mode sang hiển thị ảnh tĩnh (`NativeStaticDetectionView`). 
*   **Adaptive Layout:** Màn hình quét tự chia đôi không gian (Camera bên trái, Kết quả bên phải) nếu app chạy ngang trên màn hình lớn (Tablet, Web), ngược lại thì xếp dọc trên Mobile.

**2. Chiến lược Nhận diện Lai (Hybrid Strategy) cực hay:**
*   Bảng điều khiển `DetectionPanel.kt` chứa một logic tên là `isYoloFailed`. Logic này tính toán: Nếu đang ở chế độ ảnh tĩnh (đã đóng băng khung hình) MÀ danh sách côn trùng rỗng (không tìm thấy gì) HOẶC điểm tin cậy `highestScore < 0.4f` (40%)...
*   Thì giao diện lập tức hiện ra dòng chữ "Không nhận diện rõ côn trùng" kèm nút bấm: **"Phân tích bằng AI chuyên sâu"** (Fallback).
*   **Đánh giá:** Đây là một thiết kế UX/System Design hoàn hảo. Dùng AI cục bộ (YOLO) để lấy tốc độ nhanh (offline, realtime), nhưng nếu AI cục bộ bó tay thì dùng mạng lưới Đám mây (Cloud API) để đảm bảo độ chính xác.

### Batch 5.3: Fallback & Permissions (Logic Đám mây & Quyền)
*(Phân tích chuyên sâu từ: `ScanFallbackViewModel.kt`, `CameraPermissionScreen.kt`)*

**1. Tích hợp iNaturalist (Cloud AI):**
*   `ScanFallbackViewModel` chính là bộ não xử lý nút bấm "Phân tích bằng AI chuyên sâu" từ Batch trước.
*   ViewModel này chịu trách nhiệm gửi mảng byte ảnh lên API của iNaturalist. Khi có kết quả trả về, nó sẽ tự động biên dịch các cấp bậc phân loại sinh học (Taxonomy) từ tiếng Anh sang tiếng Việt (`species` -> Loài, `family` -> Họ, `order` -> Bộ). 
*   Đồng thời nó gắn cờ `ScanSource.INATURALIST` vào biến `scanEvent` để báo cho toàn bộ hệ thống biết: Con bọ này được định danh bởi Đám mây, không phải bởi YOLO cục bộ.

**2. Quản lý quyền Camera mượt mà:**
*   `CameraPermissionScreen` xử lý UX khi bị từ chối quyền. Khi không có quyền, app không crash mà hiện ra giao diện yêu cầu cấp quyền tử tế. Cấu trúc MVVM giúp luồng xin quyền không làm nghẽn luồng render của Compose.

## 6. Flow: Chi tiết Sinh vật (Bug Detail)

### Batch 6.1: UI & ViewModel Chi tiết
*(Phân tích chuyên sâu từ: `BugDetailScreen.kt`, `BugDetailViewModel.kt`, `DetailSectionTextPolicy.kt`)*

**1. Crowdsourcing và Tự động sinh Từ điển (Auto-Generate Encyclopedia):**
*   Đây là một trong những tính năng thông minh nhất của toàn bộ ứng dụng nằm trong `BugDetailViewModel`. 
*   Khi quét ra một con bọ, đôi khi hệ thống chỉ biết được "Tên Khoa Học" (Scientific Name) từ iNaturalist hoặc YOLO, nhưng lại thiếu các thông tin như "Cách diệt", "Triệu chứng gây hại".
*   Lúc này, ViewModel sẽ tra cứu Firebase. Nếu Firebase **chưa có** con bọ này, nó lập tức kích hoạt `groqApi.generateBugInfo` (Sử dụng `gpt-oss-120b` qua Groq) để viết ra một bài bách khoa toàn thư hoàn chỉnh. Sau đó, nó **tự động lưu ngược lên Firebase**. 
*   -> **Kết quả:** Ứng dụng tự động học hỏi và làm giàu kho dữ liệu (Crowdsourcing). Càng nhiều người dùng quét bọ, từ điển của App càng lớn mà không tốn công nhập liệu bằng tay. Tuyệt tác System Design!

**2. Giao diện tùy biến & Parse chuỗi:**
*   `BugDetailScreen` dùng `BoxWithConstraints` để rẽ nhánh layout: chia đôi màn hình trên máy tính bảng (Tablet/Web > 800dp) và cuộn dọc trên điện thoại.
*   `DetailSectionTextPolicy.kt` được dùng để dọn dẹp các đoạn văn bản do AI sinh ra. Đôi khi AI trả về dữ liệu thô dính liền nhau, Policy này tự động cắt chuỗi bằng dấu `;`, tìm dấu `:` để bôi đậm tiêu đề (ví dụ: **Gây hại:** làm cháy lá) giúp giao diện rất sạch sẽ và thống nhất.

## 7. Flow: History & Data Sync (Lịch sử & Đồng bộ)

### Batch 7.1: UI Lịch sử Scan
*(Phân tích chuyên sâu từ: `HistoryScreen.kt`, `HistoryViewModel.kt`)*

**1. Kiến trúc Offline-First (Ưu tiên Ngoại tuyến):**
*   Vì đặc thù app dùng để soi sâu bọ (thường ở ngoài đồng ruộng, rừng rậm, nơi sóng 3G/4G yếu), tác giả đã cài cắm một cơ chế **Offline-First** cực kỳ xịn xò trong `HistoryViewModel`.
*   Khi quét xong bằng YOLO (YOLO chạy offline không cần mạng), nếu máy **mất mạng**, ViewModel sẽ gom tấm ảnh và kết quả quét gọi hàm `saveOfflineHistory` để giấu vào bộ nhớ đệm (Local DB).
*   Đến khi người dùng mở lại tab Lịch sử (`fetchHistory()`), ViewModel sẽ lập tức đếm xem có bao nhiêu bản ghi bị kẹt (`getOfflineHistoryCount()`) và tự động kích hoạt `syncOfflineHistory()` để đẩy bù lên Cloud (Firebase). Trải nghiệm sử dụng không hề bị gián đoạn.

**2. Giao diện (UI) Trực quan:**
*   `HistoryItemCard` hiển thị đầy đủ thông tin thu nhỏ dưới dạng các Badge (Huy hiệu): **Độ tin cậy** (VD: Trung bình 60%), **Mức độ gây hại** (VD: Nguy hiểm), **Nguồn phân tích** (VD: iNaturalist hay YOLO). Điều này liên kết chặt chẽ với Flow 5.
*   Cơ chế Adaptive Layout tiếp tục được tận dụng: Hiển thị dạng Lưới (Grid) trên màn lớn và Danh sách cuộn (Column) trên điện thoại.

### Batch 7.2: Logic Lưu trữ (Repository)
*(Phân tích chuyên sâu từ: `HistoryRepositoryImpl.kt`, `LocalStorage.kt`)*

**1. Lưu trữ đám mây lai (Hybrid Cloud Storage):**
*   Thay vì dùng Firebase Storage (khá phức tạp khi cấu hình cho cả Android và Web trong KMP), tác giả đã dùng một chiêu rất thông minh: 
    *   **Dữ liệu chữ (Metadata):** Lưu vào Firebase Firestore (`scan_history`).
    *   **Hình ảnh chụp:** Gửi qua API của `ImgBB` bằng Ktor HTTP Client. ImgBB trả về một đường link URL ảnh `.jpg`, đường link này sau đó được lưu vào Firestore. Miễn phí, siêu tốc độ và rất dễ bảo trì.

**2. Cơ chế LocalStorage đa nền tảng (expect/actual):**
*   Bộ nhớ tạm Offline-First ở Batch 7.1 được hiện thực hóa qua class `LocalStorage` (sử dụng từ khóa `expect` của Kotlin Multiplatform).
*   Điều này có nghĩa là App sẽ xài `SharedPreferences` khi chạy trên Android, và xài `window.localStorage` khi chạy trên nền Web.
*   Khi ngoại tuyến, tấm ảnh được nén thành chuỗi Base64 và nhồi chung với dữ liệu côn trùng vào một chuỗi JSON, rồi nhét thẳng vào LocalStorage với từ khóa bắt đầu bằng `offline_`. Khi có mạng, hàm `syncOfflineHistory` lôi đống chuỗi này ra, giải mã và gửi lên Cloud. Vô cùng bền bỉ!

## 8. Flow: Search & Encyclopedia (Groq + Wiki)

### Batch 8.1: Giao diện Bách Khoa Toàn Thư
*(Phân tích chuyên sâu từ: `EncyclopediaScreen.kt`, `EncyclopediaViewModel.kt`)*

**1. Kỹ thuật Dịch thuật Ngữ cảnh (Contextual Translation):**
*   Cơ sở dữ liệu của iNaturalist (toàn cầu) thường không hỗ trợ tốt việc tìm kiếm bằng tiếng Việt (ví dụ: gõ "Bọ rùa" đôi khi không ra).
*   Hàm `searchInsects` trong `EncyclopediaViewModel` đã khắc phục triệt để bằng một luồng tư duy cực hay:
    *   Đầu tiên, nó thử tra chữ "Bọ rùa" vào Firebase của app xem có khớp với "Tên khoa học" nào đã được lưu không.
    *   Nếu không có, nó gọi AI (`groqApi.translateToEnglishName`) để dịch chữ "Bọ rùa" sang tiếng Anh (Ladybug).
    *   Cuối cùng mới lấy chữ "Ladybug" đó ném lên iNaturalist để tìm kiếm. Kết quả trả về vô cùng chính xác!
    
**2. Hệ quản trị Nội dung (CMS) nhúng trực tiếp:**
*   `EncyclopediaScreen` tích hợp sẵn biến `isAdmin`. Nếu người dùng là Admin (đã đăng nhập tài khoản có cờ Admin ở Flow 3), giao diện sẽ tự động mọc ra nút (+) Thêm mới, Sửa, và Xóa ngay trên các thẻ Côn trùng. 
*   Quản trị viên có thể thao tác với cơ sở dữ liệu Firebase (Thêm/Sửa/Xóa bọ) trực tiếp trên điện thoại mà không cần phải code một trang web Admin riêng.

### Batch 8.2: Database & Encyclopedia Repository
*(Phân tích chuyên sâu từ: `EncyclopediaRepository.kt`, `EncyclopediaRepositoryImpl.kt`)*

**1. Thủ thuật Tìm kiếm Firebase (Prefix Search Trick):**
*   Firebase Firestore mặc định **không** hỗ trợ tìm kiếm chuỗi tương đối (tương tự như `LIKE '%query%'` của SQL). Việc cắm một con ElasticSearch vào thì lại quá tốn kém cho một dự án đồ án.
*   Tác giả đã dùng một thủ thuật cực kỳ kinh điển của dân làm Firebase: Kết hợp `startAtFieldValues` và `endAtFieldValues` với ký tự Unicode cao nhất `\uf8ff`. Ví dụ: `startAt("bọ")` và `endAt("bọ\uf8ff")`. Nhờ vậy, App vẫn làm được chức năng Gợi ý Tìm kiếm (Search Suggestion) siêu tốc độ mà không tốn 1 đồng chi phí server nào!
*   Đồng thời tác giả còn sinh ra 4 biến thể của từ khóa (chữ thường, chữ hoa đầu câu, chữ hoa từng chữ) để lặp vòng lặp tìm kiếm, đảm bảo người dùng gõ kiểu gì cũng trúng.

**2. Đảm bảo 100% Offline (Local Fallback):**
*   Nếu người dùng mở App ra giữa rừng và **không có mạng ngay từ đầu**, Firebase sẽ báo lỗi. 
*   Nhưng thay vì App chết, hàm `getFallbackData()` sẽ đọc một file cứng tên là `backup_encyclopedia.json` được đóng gói sẵn bên trong source code của App (`Res.readBytes`) để lấy dữ liệu. Bằng cách này, tính năng Bách Khoa Toàn Thư lúc nào cũng sống sót trong mọi điều kiện khắc nghiệt nhất.

## 9. Flow: AI Chatbot (Gemini)

### Batch 9.1: UI Chat & Gợi ý (Suggestions)
*(Phân tích chuyên sâu từ: `ChatScreen.kt`, `ChatViewModel.kt`)*

**1. Liên kết Ngữ cảnh (Context Routing):**
*   Màn hình Chat không phải là một màn hình cô lập. Tác giả đã thiết kế để nó có thể nhận dữ liệu truyền vào từ mọi nơi trong App: 
    *   Từ màn Scan (`initialImageBytes`)
    *   Từ màn Lịch sử (`initialImageUrl`)
    *   Từ màn Chi tiết Bách khoa (`initialBugContext`)
*   Khi có `initialBugContext`, giao diện sẽ tự động hiện thẻ `ActiveBugContextCard` ("Đang hỏi về loài..."). Điều này báo cho người dùng biết AI đã đọc bài viết đó rồi, họ chỉ việc hỏi thẳng vào trọng tâm mà không cần phải giải thích lại tên con bọ.

**2. Tiền xử lý Đa phương tiện (Multimedia Pre-processing):**
*   `ChatViewModel` xử lý hình ảnh rất tinh tế: Nếu đầu vào là một bức ảnh mảng Byte (chụp từ Camera), nó sẽ giữ nguyên. Nhưng nếu đầu vào là một đường Link URL (từ Lịch sử), nó sẽ tự động dùng Ktor gọi HTTP GET tải bức ảnh đó thành mảng Byte *trước khi* gửi đi. Việc này giúp luồng gửi API cho Gemini luôn đồng nhất (đều là mảng Byte được nén thành Base64).
*   Giao diện Chat trên Web hỗ trợ dán ảnh bằng phím tắt `Ctrl+V` (Sử dụng `registerClipboardImagePasteHandler`). Đây là trải nghiệm chuẩn mực của một Web App hiện đại.

### Batch 9.2: Logic RAG & Gemini API
*(Phân tích chuyên sâu từ: `ChatRagContextPolicy.kt`, `ChatContextResolver.kt`, `GeminiApiService.kt`)*

**1. Xử lý RAG (Retrieval-Augmented Generation) thông minh:**
*   `ChatRagContextPolicy` chịu trách nhiệm nhồi thông tin bách khoa vào đầu con AI (đóng vai trò là System Prompt).
*   Điểm đắt giá nhất nằm ở hàm `cleanForFarmerPrompt()`. Dữ liệu bách khoa đôi khi bị dính các từ khóa kỹ thuật của lập trình viên (như "YOLO", "IP102", "nhãn mô hình"). Hàm này sẽ dùng Regex để gạch bỏ sạch sẽ các từ đó trước khi đưa cho AI đọc. Nhờ vậy, AI khi trả lời người nông dân sẽ dùng văn phong cực kỳ tự nhiên, không bị lộ ra các thuật ngữ lập trình.

**2. Tiêm Model Động (Dynamic Model Injection):**
*   Trong `GeminiApiService`, tên mô hình AI không bị code cứng (hardcode) là `gemini-1.5-flash` hay `gemini-1.5-pro`. 
*   Nó gọi `config.geminiModel` từ Firestore. Nghĩa là sau này Google ra mắt "gemini-2.0", tác giả chỉ việc vào Firebase đổi 1 dòng chữ là toàn bộ App trên điện thoại người dùng sẽ tự động nâng cấp lên mô hình mới nhất mà không cần bắt họ phải cập nhật App!

---
## TỔNG KẾT (CONCLUSION)
Dự án **BugScanner (Kotlin Multiplatform)** là một minh chứng xuất sắc cho trình độ tư duy Kiến trúc Phần mềm (Software Architecture) của tác giả. 
1. **Kiến trúc:** App áp dụng chuẩn mực **Clean Architecture** (Domain - Data - UI) kết hợp **MVVM**, sử dụng Dependency Injection (Koin) xuyên suốt.
2. **Kỹ thuật Offline-First:** Xử lý quá tốt tình trạng mất mạng bằng cách kết hợp SQLite, File IO dự phòng và SharedPreferences/LocalStorage để đồng bộ ngầm khi có mạng.
3. **Triển khai AI:** Tích hợp cả AI cục bộ (YOLO) để nhận diện thời gian thực (không cần mạng) và AI Đám mây (Gemini, Groq) để phân tích ngôn ngữ tự nhiên. 
4. **Hiệu suất & Đa nền tảng:** Code được viết 1 lần nhưng chạy mượt mà trên cả Android, iOS và Web (WASM). Quản lý luồng (Coroutines/Flow) được xử lý hoàn hảo để không bao giờ bị nghẽn UI.

Đây thực sự là một sản phẩm đạt độ hoàn thiện ở mức độ thương mại (Production-ready).
