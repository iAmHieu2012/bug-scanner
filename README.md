# 🐛 BugScanner

![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-blue?logo=kotlin)
![Compose](https://img.shields.io/badge/Compose-Multiplatform-4285F4?logo=jetpackcompose)
![TensorFlow Lite](https://img.shields.io/badge/TensorFlow-Lite-FF6F00?logo=tensorflow)
![Platforms](https://img.shields.io/badge/Platforms-Android%20%7C%20Web-lightgray)
![License](https://img.shields.io/badge/License-Apache%202.0-blue)

An intelligent multiplatform application for real-time insect detection and classification using machine learning. Built with **Kotlin Multiplatform** and **Compose**, BugScanner leverages **YOLO object detection** and **TensorFlow Lite** to provide accurate insect identification across **Android** and **Web** platforms.

## ✨ Features

- **🔐 User Authentication** – Secure login and account management via Firebase
- **📷 Real-time Camera Detection** – Live camera scanning using YOLO models
- **📁 Gallery Image Scanning** – Analyze existing images from device storage
- **📚 Insect Encyclopedia** – Comprehensive database with detailed species information
- **💬 AI Chat Support** – Integrated Gemini AI chat for user assistance
- **📊 Scan History** – Track and manage all detection records
- **🔄 Multi-Platform** – Seamless experience on Android and Web

## 🛠️ Tech Stack

| Component | Technology |
|-----------|------------|
| **Language** | Kotlin 2.x |
| **UI Framework** | Jetpack Compose Multiplatform |
| **Architecture** | MVVM (Model-View-ViewModel) |
| **ML Framework** | TensorFlow Lite, YOLO object detection |
| **AI Integration** | Google Gemini API |
| **Camera** | AndroidX Camera for Android |
| **Backend** | REST API + Firebase |
| **Authentication** | Firebase Authentication |
| **Build System** | Gradle with Kotlin DSL |

## 📁 Project Structure

```
bug-scanner/
├── src/
│   ├── composeApp/                    # Main Compose Multiplatform module
│   │   ├── src/
│   │   │   ├── commonMain/            # Shared code for all platforms
│   │   │   │   └── kotlin/hcmus/bugscanner/
│   │   │   │       ├── App.kt         # Main app entry point
│   │   │   │       ├── Platform.kt    # Platform utilities
│   │   │   │       ├── domain/        # Business logic & entities
│   │   │   │       ├── data/          # Repositories & API clients
│   │   │   │       ├── ml/            # ML-related utilities
│   │   │   │       ├── core/          # Core utilities & constants
│   │   │   │       └── ui/            # Shared Compose components
│   │   │   │
│   │   │   ├── androidMain/           # Android-specific implementation
│   │   │   │   ├── kotlin/hcmus/bugscanner/
│   │   │   │   ├── assets/            # ML models (model.tflite)
│   │   │   │   ├── res/               # Android resources
│   │   │   │   └── AndroidManifest.xml
│   │   │   │
│   │   │   ├── webMain/               # Web platform implementation
│   │   │   │   ├── kotlin/
│   │   │   │   └── resources/
│   │   │   │
│   │   │   ├── commonTest/            # Shared unit tests
│   │   │   │
│   │   │   └── (iosMain/jvmMain/etc)  # Other platform folders (not active)
│   │   │
│   │   ├── build.gradle.kts
│   │   ├── google-services.json       # Firebase config (Android)
│   │   └── webpack.config.d/          # Webpack configuration (Web)
│   │
│   ├── iosApp/                        # Native iOS Xcode project (planned)
│   ├── gradle/                        # Gradle wrapper & version catalog
│   └── settings.gradle.kts
│
├── docs/                              # Project documentation
├── README.md                          # This file
└── LICENSE                            # Apache 2.0 License
```

## 📋 Prerequisites

### For Android Development
- Android Studio (latest with Kotlin Multiplatform support)
- Android SDK API level 24+
- JDK 17 or higher

### For Web Development
- JDK 17 or higher
- Modern web browser (Chrome, Firefox, Safari, Edge)

## 🚀 Getting Started

### Clone & Setup

```bash
git clone <repository-url>
cd bug-scanner/src
```

### Configure Required Files

Before building, ensure these files are in place:

```
composeApp/google-services.json
composeApp/src/androidMain/assets/model.tflite    # YOLO model for Android
composeApp/src/webMain/resources/firebase-config.json
```

### Build & Run

#### ▶️ Android

```bash
# Build debug APK
./gradlew :composeApp:assembleDebug

# Or run directly (requires emulator/device)
./gradlew :composeApp:installDebug

# Run from Android Studio
# Select "composeApp" run configuration and click Run
```

#### 🌐 Web

```bash
# Run development server
./gradlew :composeApp:jsBrowserDevelopmentRun

# Then open browser at: http://localhost:8080
```

## 💻 Development Guide

### Architecture Overview

BugScanner follows the **MVVM (Model-View-ViewModel)** architectural pattern:

```
┌─────────────────────────────────────┐
│  UI Layer (Compose Components)      │
├─────────────────────────────────────┤
│  ViewModel (Business Logic State)   │
├─────────────────────────────────────┤
│  Repository (Data Abstraction)      │
├─────────────────────────────────────┤
│  Data Sources (API, Database, etc)  │
└─────────────────────────────────────┘
```

### Code Organization

**Common Code** (`commonMain`)
- Domain models and entities
- Repository interfaces and implementations
- ViewModels
- Shared UI components and screens

**Platform-Specific Code**
- **Android** – Camera integration, Android sensors, platform utilities
- **Web** – Browser-specific implementations, WASM optimizations

### Adding a New Feature

1. **Define the domain model** in `commonMain/domain/model/`
2. **Create a repository** in `commonMain/data/repository/`
3. **Build a ViewModel** in `commonMain/ui/viewmodel/`
4. **Design UI components** in `commonMain/ui/screens/` or `commonMain/ui/components/`
5. **Add platform-specific code** as needed (e.g., camera access for Android)

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| **Gradle sync fails** | Run `./gradlew clean` and sync again. Clear IDE caches if needed. |
| **Model file not found** | Ensure `model.tflite` exists in `composeApp/src/androidMain/assets/` |
| **Firebase auth errors** | Verify `google-services.json` is in `composeApp/` directory |
| **Web build fails** | Clear `node_modules` and `.gradle`, then rebuild |
| **Camera permission denied** | Check `AndroidManifest.xml` permissions on Android |

## 📚 Resources

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)
- [TensorFlow Lite Documentation](https://www.tensorflow.org/lite)
- [YOLO Object Detection](https://docs.ultralytics.com/)
- [Firebase Documentation](https://firebase.google.com/docs)

## 📄 License

Licensed under the [Apache License 2.0](LICENSE).

## 🎓 Credits

Developed at **HCMUS** (Ho Chi Minh City University of Science)

---

For questions or contributions, please open an issue or submit a pull request.
