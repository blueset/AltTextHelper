package studio1a23.altTextAi.api

import android.content.Context
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.ImagePart
import com.aallam.openai.api.chat.ListContent
import com.aallam.openai.api.chat.TextPart
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import studio1a23.altTextAi.MAX_TOKENS
import studio1a23.altTextAi.OpenAICompatibleConfig
import studio1a23.altTextAi.OpenAIConfig
import studio1a23.altTextAi.R
import kotlin.time.Duration.Companion.seconds

suspend fun openApiCompatibleComplete(
    config: OpenAICompatibleConfig,
    base64Image: String,
    presetPrompt: String,
    context: Context
): Result<String> {
    if (config.apiKey.isEmpty() || config.model.isEmpty() || config.baseUrl.isEmpty()) {
        return Result.failure(IllegalArgumentException(context.getString(R.string.incomplete_configuration)))
    }

    try {
        val openai = OpenAI(
            token = config.apiKey,
            timeout = Timeout(socket = 60.seconds),
            host = OpenAIHost(baseUrl = config.baseUrlWithSlash)
        )
        val completion = openai.chatCompletion(
            ChatCompletionRequest(
                model = ModelId(config.model),
                maxTokens = MAX_TOKENS,
                messages = listOf(
                    ChatMessage(
                        role = ChatRole.User,
                        messageContent = ListContent(
                            listOf(
                                TextPart(presetPrompt),
                                ImagePart("data:image/png;base64,$base64Image", "high")
                            )
                        )
                    )
                )
            )
        )

        return completion.choices.firstOrNull()?.message?.content?.let { Result.success(it) }
            ?: Result.failure(Exception(context.getString(R.string.error_no_response)))
    } catch (e: Exception) {
        return Result.failure(e)
    }
}