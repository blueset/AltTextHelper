package studio1a23.altTextAi.api

import android.content.Context
import android.util.Log
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
import studio1a23.altTextAi.R

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
private data class ChatCompletionRequest(val model: String, val messages: List<ChatMessage>)

@Serializable
private data class ChatCompletionResponse(val choices: List<Choice>) {
    @Serializable
    data class Choice(val message: Message) {
        @Serializable
        data class Message(val content: String)
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

    val client =
        HttpClient(Android) {
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

        val request =
            ChatCompletionRequest(
                model = model,
                messages =
                listOf(
                    ChatMessage(
                        role = "user",
                        content =
                        listOf(
                            MessageContent.Text(
                                presetPrompt
                            ),
                            MessageContent.ImageUrl(
                                ImageUrlContent(
                                    url = "data:image/png;base64,$base64Image",
                                    detail = "high"
                                )
                            )
                        )
                    )
                )
            )

        val response =
            client.post(url) {
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
                    }
                        ?: Result.failure(Exception(context.getString(R.string.error_no_response)))
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
