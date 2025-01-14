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
import studio1a23.altTextAi.R

val JsonLoose = Json { ignoreUnknownKeys = true }

@Serializable
private data class ChatMessage(val role: String, val content: List<MessageContent>)

@Serializable
sealed class MessageContent {
    @Serializable
    @SerialName("text")
    data class Text(val text: String) : MessageContent()

    @Serializable
    @SerialName("image_url")
    data class ImageUrl(@SerialName("image_url") val imageUrl: ImageUrlContent) :
        MessageContent()
}

@Serializable
data class ImageUrlContent(val url: String, val detail: String)

@Serializable
private data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false
)

@Serializable
private data class ChatCompletionResponse(val choices: List<Choice>) {
    @Serializable
    data class Choice(val message: Message) {
        @Serializable
        data class Message(val content: String)
    }
}

@Serializable
private data class ChatCompletionChunk(
    val choices: List<StreamChoice>
) {
    @Serializable
    data class StreamChoice(
        val delta: Delta,
        @SerialName("finish_reason") val finishReason: String? = null
    ) {
        @Serializable
        data class Delta(
            val content: String? = null
        )
    }
}

private fun buildChatRequest(
    model: String,
    base64Image: String,
    presetPrompt: String,
    enableStreaming: Boolean
): ChatCompletionRequest {
    return ChatCompletionRequest(
        model = model,
        messages = listOf(
            ChatMessage(
                role = "user",
                content = listOf(
                    MessageContent.Text(presetPrompt),
                    MessageContent.ImageUrl(
                        ImageUrlContent(
                            url = "data:image/png;base64,$base64Image",
                            detail = "high"
                        )
                    )
                )
            )
        ),
        stream = enableStreaming
    )
}

private suspend fun handleStreamingResponse(
    client: HttpClient,
    url: String,
    headers: Map<String, String>,
    queryParameters: Map<String, String>?,
    request: ChatCompletionRequest,
    onChunk: (String) -> Unit
): Result<String> {
    val fullResponse = StringBuilder()
    return try {
        client.preparePost(url) {
            contentType(ContentType.Application.Json)
            headers { headers.forEach { (key, value) -> append(key, value) } }
            url { queryParameters?.forEach { (key, value) -> parameters.append(key, value) } }
            setBody(request)
        }.execute { response ->
            response.bodyAsChannel().apply {
                while (!isClosedForRead) {
                    val line = readUTF8Line(Int.MAX_VALUE) ?: continue
                    if (line.startsWith("data: ")) {
                        val jsonData = line.substring(6)
                        if (jsonData == "[DONE]") break
                        try {
                            val chunk = JsonLoose.decodeFromString<ChatCompletionChunk>(jsonData)
                            val firstChoice = chunk.choices.firstOrNull() ?: continue
                            firstChoice.delta.content?.let { content ->
                                fullResponse.append(content)
                                onChunk(content)
                            }
                            firstChoice.finishReason?.let { finishReason ->
                                if (finishReason != "stop") {
                                    onChunk(" [finishReason: $finishReason]")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("OpenAIKtor", "Failed to parse chunk: $jsonData", e)
                            // Skip malformed chunks
                        }
                    } else {
                        Log.d("OpenAIKtor", "Non-data line: $line")
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
    queryParameters: Map<String, String>?,
    request: ChatCompletionRequest,
    context: Context
): Result<String> {
    val response = client.post(url) {
        contentType(ContentType.Application.Json)
        headers { headers.forEach { (key, value) -> append(key, value) } }
        url { queryParameters?.forEach { (key, value) -> parameters.append(key, value) } }
        setBody(request)
    }

    return when (response.status) {
        HttpStatusCode.OK -> {
            try {
                val completionResponse = response.body<ChatCompletionResponse>()
                completionResponse.choices.firstOrNull()?.message?.content?.let {
                    Result.success(it)
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

suspend fun openAiComplete(
    baseUrl: String,
    headers: Map<String, String>,
    queryParameters: Map<String, String>? = null,
    model: String,
    base64Image: String,
    presetPrompt: String,
    context: Context,
    enableStreaming: Boolean = false,
    onChunk: (String) -> Unit = {}
): Result<String> {
    // Validate inputs
    if (baseUrl.isEmpty() ||
        model.isEmpty() ||
        base64Image.isEmpty() ||
        presetPrompt.isEmpty() ||
        headers.isEmpty()
    ) {
        return Result.failure(
            IllegalArgumentException(context.getString(R.string.incomplete_configuration))
        )
    }

    val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }
            )
        }
        install(HttpTimeout) { requestTimeoutMillis = 60000 }
    }

    try {
        val url = baseUrl.trimEnd('/') + "/chat/completions"
        val request = buildChatRequest(model, base64Image, presetPrompt, enableStreaming)
        
        return if (enableStreaming) {
            handleStreamingResponse(client, url, headers, queryParameters, request, onChunk)
        } else {
            handleNonStreamingResponse(client, url, headers, queryParameters, request, context)
        }
    } catch (e: Exception) {
        return Result.failure(e)
    } finally {
        client.close()
    }
}
