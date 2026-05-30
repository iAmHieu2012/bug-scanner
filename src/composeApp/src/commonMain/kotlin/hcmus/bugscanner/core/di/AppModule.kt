package hcmus.bugscanner.core.di

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import hcmus.bugscanner.data.remote.GeminiApiService
import hcmus.bugscanner.data.remote.INaturalistApiService
import hcmus.bugscanner.data.remote.WikiApiService
import hcmus.bugscanner.data.repository.EncyclopediaRepositoryImpl
import hcmus.bugscanner.data.repository.HistoryRepositoryImpl
import hcmus.bugscanner.domain.repository.EncyclopediaRepository
import hcmus.bugscanner.domain.repository.HistoryRepository
import hcmus.bugscanner.ui.auth.AuthViewModel
import hcmus.bugscanner.ui.chat.ChatViewModel
import hcmus.bugscanner.ui.history.HistoryViewModel
import hcmus.bugscanner.ui.encyclopedia.EncyclopediaViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Module cấu hình Dependency Injection (DI) trung tâm của ứng dụng.
 * Quản lý vòng đời của các HTTP Client, API Services, Repositories và ViewModels.
 */
val appModule = module {

    // 1. Tầng Mạng: Khởi tạo HttpClient dùng chung (Singleton)
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                    isLenient = true
                    explicitNulls = false
                })
            }
        }
    }

    // 2. Tầng Database: Cung cấp instance của Firebase Firestore
    single { Firebase.firestore }

    // 3. Tầng API Services: Koin tự động tìm HttpClient ở trên (qua hàm get()) để tiêm vào
    single { GeminiApiService(client = get()) }
    single { INaturalistApiService(client = get()) }
    single { WikiApiService(client = get()) }

    // 4. Tầng Repositories: Tự động tiêm Firebase db và HttpClient vào phần Impl
    single<EncyclopediaRepository> { EncyclopediaRepositoryImpl(db = get()) }
    single<HistoryRepository> { HistoryRepositoryImpl(db = get(), httpClient = get()) }

    // 5. Tầng ViewModels: Koin tự động đọc Constructor để tiêm Repository và API tương ứng
    viewModelOf(::AuthViewModel)
    viewModelOf(::ChatViewModel)
    viewModelOf(::HistoryViewModel)
    viewModelOf(::EncyclopediaViewModel)
}