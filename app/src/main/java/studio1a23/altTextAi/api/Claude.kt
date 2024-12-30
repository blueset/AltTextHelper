package studio1a23.altTextAi.api

import android.content.Context
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import studio1a23.altTextAi.ClaudeConfig
import studio1a23.altTextAi.MAX_TOKENS
import studio1a23.altTextAi.R
import java.util.concurrent.TimeUnit

private const val CLAUDE_API_URL = "https://api.anthropic.com/v1/"

data class ClaudeMessage(
    val role: String,
    val content: List<ClaudeContent>
)

sealed class ClaudeContent {
    data class Text(
        val type: String = "text",
        val text: String
    ) : ClaudeContent()

    data class Image(
        val type: String = "image",
        val source: ImageSource
    ) : ClaudeContent()
}

data class ImageSource(
    val type: String = "base64",
    @SerializedName("media_type")
    val mediaType: String = "image/png",
    val data: String
)

data class ClaudeRequest(
    val model: String = "claude-3-5-sonnet-latest",
    @SerializedName("max_tokens")
    val maxTokens: Int = MAX_TOKENS,
    val messages: List<ClaudeMessage>
)

data class ClaudeResponse(
    val content: List<ClaudeContent>
)

interface ClaudeApi {
    @POST("messages")
    suspend fun complete(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Body request: ClaudeRequest
    ): ClaudeResponse
}

suspend fun claudeComplete(
    config: ClaudeConfig,
    base64Image: String,
    presetPrompt: String,
    context: Context
): Result<String> {
    if (config.apiKey.isEmpty() || config.model.isEmpty()) {
        return Result.failure(IllegalArgumentException(context.getString(R.string.incomplete_configuration)))
    }

    try {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(CLAUDE_API_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val claudeApi = retrofit.create(ClaudeApi::class.java)

        val request = ClaudeRequest(
            messages = listOf(
                ClaudeMessage(
                    role = "user",
                    content = listOf(
                        ClaudeContent.Image(
                            source = ImageSource(
                                data = base64Image
                            )
                        ),
                        ClaudeContent.Text(
                            text = presetPrompt
                        )
                    )
                )
            )
        )

        val response = claudeApi.complete(
            apiKey = config.apiKey,
            request = request
        )

        return response.content.filterIsInstance<ClaudeContent.Text>().firstOrNull()?.let {
            Result.success(it.text)
        } ?: Result.failure(Exception(context.getString(R.string.error_no_response)))
    } catch (e: Exception) {
        return Result.failure(e)
    }
}
