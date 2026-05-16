package hcmus.bugscanner.data.remote

import hcmus.bugscanner.domain.model.WikiResponse
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interface định nghĩa các endpoint giao tiếp với Wikipedia API.
 */
interface WikiApiService {
    @GET("api.php")
    suspend fun searchInsects(
        @Query("action") action: String = "query",
        @Query("format") format: String = "json",
        @Query("prop") prop: String = "extracts|pageimages",
        @Query("exintro") exintro: Boolean = true,
        @Query("explaintext") explaintext: Boolean = true,
        @Query("piprop") piprop: String = "thumbnail",
        @Query("pithumbsize") pithumbsize: Int = 300,
        @Query("generator") generator: String = "search",
        @Query("gsrlimit") gsrlimit: Int = 10,
        @Query("gsrsearch") query: String
    ): WikiResponse
}

/**
 * Singleton khởi tạo và cấu hình Retrofit client.
 */
object RetrofitClient {
    private const val BASE_URL = "https://vi.wikipedia.org/w/"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "BugScannerApp/1.0 (hcmus.bugscanner)")
                .build()
            chain.proceed(request)
        }
        .build()

    val apiService: WikiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WikiApiService::class.java)
    }
}