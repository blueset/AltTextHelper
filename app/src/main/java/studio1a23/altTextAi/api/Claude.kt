package studio1a23.altTextAi.api

import android.content.Context
import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsChannel
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
    val messages: List<ClaudeMessage>,
    val stream: Boolean = false
)

@Serializable
private data class ClaudeResponse(
    val content: List<ClaudeContent>
)

@Serializable
private data class ClaudeStreamEvent(
    val type: String,
    val message: ClaudeStreamMessage? = null,
    val delta: ClaudeStreamDelta? = null,
    val index: Int? = null
)

@Serializable
private data class ClaudeStreamMessage(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ClaudeContent>,
    @SerialName("stop_reason")
    val stopReason: String? = null
)

@Serializable
private data class ClaudeStreamDelta(
    @SerialName("text_delta")
    val textDelta: TextDelta? = null,
    @SerialName("stop_reason")
    val stopReason: String? = null
)

@Serializable
private data class TextDelta(
    val text: String
)

private suspend fun handleStreamingResponse(
    client: HttpClient,
    url: String,
    headers: Map<String, String>,
    request: ClaudeRequest,
    onChunk: (String) -> Unit
): Result<String> {
    val fullResponse = StringBuilder()
    return try {
        client.preparePost(url) {
            contentType(ContentType.Application.Json)
            headers { headers.forEach { (key, value) -> append(key, value) } }
            setBody(request)
        }.execute { response ->
            response.bodyAsChannel().apply {
                while (!isClosedForRead) {
                    val line = readUTF8Line(Int.MAX_VALUE) ?: continue
                    if (line.startsWith("data: ")) {
                        val jsonData = line.substring(6)
                        try {
                            val event = JsonLoose.decodeFromString<ClaudeStreamEvent>(jsonData)
                            when (event.type) {
                                "content_block_delta" -> {
                                    event.delta?.textDelta?.text?.let { text ->
                                        fullResponse.append(text)
                                        onChunk(text)
                                    }
                                }
                                "message_delta" -> {
                                    event.delta?.stopReason?.let { stopReason ->
                                        if (stopReason != "end_turn") {
                                            onChunk(" [stopReason: $stopReason]")
                                        }
                                    }
                                }
                                "message_stop" -> {
                                    break
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("Claude", "Failed to parse chunk: $jsonData", e)
                        }
                    }
                }
            }
        }
        Result.success(fullResponse.toString())
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private suspend fun handleNonStreamingResponse(
    client: HttpClient,
    url: String,
    headers: Map<String, String>,
    request: ClaudeRequest,
    context: Context
): Result<String> {
    val response = client.post(url) {
        contentType(ContentType.Application.Json)
        headers { headers.forEach { (key, value) -> append(key, value) } }
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
}

suspend fun claudeComplete(
    config: ClaudeConfig,
    base64Image: String,
    presetPrompt: String,
    enableStreaming: Boolean = false,
    onChunk: (String) -> Unit = {},
    context: Context,
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
        val headers = mapOf(
            "x-api-key" to config.apiKey,
            "anthropic-version" to "2023-06-01"
        )

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
            ),
            stream = enableStreaming
        )

        return if (enableStreaming) {
            handleStreamingResponse(client, url, headers, request, onChunk)
        } else {
            handleNonStreamingResponse(client, url, headers, request, context)
        }
    } catch (e: Exception) {
        return Result.failure(e)
    } finally {
        client.close()
    }
}
