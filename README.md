# BugScanner

![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-blue?logo=kotlin)
![Compose](https://img.shields.io/badge/Compose-Multiplatform-4285F4?logo=jetpackcompose)
![TensorFlow Lite](https://img.shields.io/badge/TensorFlow-Lite-FF6F00?logo=tensorflow)
![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS-lightgray)

BugScanner là một ứng dụng di động được xây dựng bằng Compose Multiplatform, hỗ trợ đa nền tảng (Android, iOS, Web, Desktop). Chức năng chính của ứng dụng là phát hiện và phân loại côn trùng thông qua hình ảnh sử dụng Machine Learning.

## Features

* **Authentication:** Hỗ trợ đăng nhập và quản lý tài khoản người dùng.
* **Image Detection & Scanning:**
  * Real-time camera scanning sử dụng mô hình YOLO để nhận diện trực tiếp.
  * Static image detection để phân tích hình ảnh có sẵn từ gallery.
* **Encyclopedia:** Tra cứu thông tin chi tiết về các loài côn trùng thông qua WikiApiService.
* **Chat:** Tích hợp tính năng chat hỗ trợ người dùng.
* **Scan History:** Lưu trữ và quản lý lịch sử các lần nhận diện trước đó.

## Tech Stack

* **Language:** Kotlin
* **Framework:** Compose Multiplatform
* **Architecture:** MVVM (Model-View-ViewModel)
* **Machine Learning:** TensorFlow Lite, YOLO object detection
* **Network:** REST API

## Project Structure

Dự án được tổ chức theo cấu trúc chuẩn của một Compose Multiplatform project:

* `composeApp/`: Module chính chứa toàn bộ logic và UI của ứng dụng.
  * `src/commonMain/`: Chứa shared code hoạt động trên mọi nền tảng (Domain models như `BugInfo`, `ScanHistory`; Repositories như `HistoryRepository`, `EncyclopediaRepository`).
  * `src/androidMain/`: Platform-specific code cho Android, bao gồm UI screens (`HomeScreen`, `ScanScreen`, `AuthScreen`, v.v.), ViewModels, cấu hình Camera, và implementation cho TensorFlow Lite (`YoloDetector.kt`).
  * `src/iosMain/`: Platform-specific code cho iOS (`MainViewController.kt`).
  * `src/jvmMain/` & `src/webMain/`: Cấu hình entry point cho Desktop và Web platforms.
* `iosApp/`: Project Xcode native để build ứng dụng iOS.

## Prerequisites

* Android Studio (phiên bản mới nhất có hỗ trợ Kotlin Multiplatform) hoặc IntelliJ IDEA.
* JDK 17 trở lên.
* Xcode (nếu cần build và chạy target iOS).

## Getting Started

### 1. Configuration

* **Google Services:** Đảm bảo file `composeApp/google-services.json` đã có sẵn và chứa cấu hình hợp lệ để các dịch vụ authentication hoặc cloud có thể hoạt động trên Android.
* **Machine Learning Model:** Dự án yêu cầu mô hình học máy đã được convert sang định dạng TFLite. File này phải được đặt đúng vị trí tại đường dẫn `composeApp/src/androidMain/assets/model.tflite`.

### 2. Build & Run

* **Android:**
    Mở project bằng Android Studio, đợi Gradle sync hoàn tất. Chọn run configuration là `composeApp` và chạy trên Emulator hoặc physical device.
* **iOS:** (Incoming)
    Mở file `iosApp/iosApp.xcworkspace` bằng Xcode, cấu hình signing target và nhấn Run.
* **Desktop (JVM):** (Incoming)
    Thực thi Gradle task sau trong terminal để chạy app trên desktop:
    `./gradlew :composeApp:run`

## License

Dự án được phát triển nội bộ (HCMUS). Mọi source code và models tuân theo quy định của repository này.
