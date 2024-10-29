package studio1a23.altTextAi

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface OpenAIApiService {
    @POST("completions?api-version=2024-08-01-preview")
    suspend fun getCompletion(
        @Body request: CompletionRequest
    ): CompletionResponse
}

interface CompletionContent {
    val type: String
}

data class CompletionContentText(
    val text: String
): CompletionContent {
    override var type: String = "text"
}

data class CompletionContentImageInfo(
    val url: String,
    val detail: String = "high"
)

data class CompletionContentImageUrl(
    val image_url: CompletionContentImageInfo
): CompletionContent {
    override var type: String = "image_url"
}

data class CompletionMessage(
    val role: String,
    val content: List<CompletionContent>
)

data class CompletionRequest(
    val model: String,
    val messages: List<CompletionMessage>,
    val max_tokens: Int,
    // Include other parameters as needed
)

data class CompletionResponse(
    val choices: List<CompletionResponseChoice>
)

data class CompletionResponseChoice(
    val message: CompletionResponseMessage
)

data class CompletionResponseMessage (
    val content: String,
    val role: String
)

fun provideOpenAIApiService(endpoint: String, apiKey: String): OpenAIApiService {
    val logging = HttpLoggingInterceptor()
    logging.setLevel(HttpLoggingInterceptor.Level.BODY)
    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
                .newBuilder()
                .addHeader("api-key", apiKey)
                .build()
            chain.proceed(request)
        }
        .addInterceptor(logging)
        .build()

    return Retrofit.Builder()
        .baseUrl(endpoint)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenAIApiService::class.java)
}

suspend fun fetchCompletion(
    apiService: OpenAIApiService,
    base64Image: String,
    presetPrompt: String
): Result<String> {
    return try {
        val request = CompletionRequest(
            messages = listOf(
                CompletionMessage(
                    role = "user",
                    content = listOf(
                        CompletionContentText(presetPrompt),
                        CompletionContentImageUrl(CompletionContentImageInfo("data:image/png;base64,$base64Image"))
                    )
                )
            ),
            model = "gpt-4o",
            max_tokens = 300 // Adjust as needed
        )
        val response = apiService.getCompletion(request)
        val text = response.choices.firstOrNull()?.message?.content ?: "No response"
        Result.success(text)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
