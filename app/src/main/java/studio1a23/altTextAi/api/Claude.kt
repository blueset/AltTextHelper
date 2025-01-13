package studio1a23.altTextAi.api

import android.content.Context
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import studio1a23.altTextAi.ClaudeConfig
import studio1a23.altTextAi.MAX_TOKENS
import studio1a23.altTextAi.R

private const val CLAUDE_API_URL = "https://api.anthropic.com/v1/"

@Serializable
private data class ClaudeMessage(
    val role: String,
    val content: List<ClaudeContent>
)

@Serializable
sealed class ClaudeContent {
    @Serializable
    @SerialName("text")
    data class Text(
        val type: String = "text",
        val text: String
    ) : ClaudeContent()

    @Serializable
    @SerialName("image")
    data class Image(
        val type: String = "image",
        val source: ImageSource
    ) : ClaudeContent()
}

@Serializable
data class ImageSource(
    val type: String = "base64",
    @SerialName("media_type")
    val mediaType: String = "image/png",
    val data: String
)

@Serializable
private data class ClaudeRequest(
    val model: String = "claude-3-5-sonnet-latest",
    @SerialName("max_tokens")
    val maxTokens: Int = MAX_TOKENS,
    val messages: List<ClaudeMessage>
)

@Serializable
private data class ClaudeResponse(
    val content: List<ClaudeContent>
)

suspend fun claudeComplete(
    config: ClaudeConfig,
    base64Image: String,
    presetPrompt: String,
    context: Context
): Result<String> {
    if (config.apiKey.isEmpty() || config.model.isEmpty()) {
        return Result.failure(IllegalArgumentException(context.getString(R.string.incomplete_configuration)))
    }

    val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 60000
            socketTimeoutMillis = 60000
        }
    }

    try {
        val url = CLAUDE_API_URL + "messages"

        val request = ClaudeRequest(
            model = config.model,
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

        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            headers {
                append("x-api-key", config.apiKey)
                append("anthropic-version", "2023-06-01")
            }
            setBody(request)
        }

        return when (response.status) {
            HttpStatusCode.OK -> {
                try {
                    val claudeResponse = response.body<ClaudeResponse>()
                    claudeResponse.content.filterIsInstance<ClaudeContent.Text>().firstOrNull()?.let {
                        Result.success(it.text)
                    } ?: Result.failure(Exception(context.getString(R.string.error_no_response)))
                } catch (e: Exception) {
                    val responseContent = response.body<String>()
                    Result.failure(Exception("HTTP ${response.status}: $responseContent"))
                }
            }
            else -> {
                val responseContent = response.body<String>()
                Result.failure(Exception("HTTP ${response.status}: $responseContent"))
            }
        }
    } catch (e: Exception) {
        return Result.failure(e)
    } finally {
        client.close()
    }
}
