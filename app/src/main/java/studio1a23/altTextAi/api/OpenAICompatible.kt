package studio1a23.altTextAi.api

import android.content.Context
import studio1a23.altTextAi.OpenAICompatibleConfig
import studio1a23.altTextAi.R

suspend fun openApiCompatibleComplete(
    config: OpenAICompatibleConfig,
    base64Image: String,
    presetPrompt: String,
    enableStreaming: Boolean = false,
    onChunk: (String) -> Unit = {},
    context: Context
): Result<String> {
    if (config.apiKey.isEmpty() || config.model.isEmpty() || config.baseUrl.isEmpty()) {
        return Result.failure(IllegalArgumentException(context.getString(R.string.incomplete_configuration)))
    }

    val headers = mapOf(
        "Authorization" to "Bearer ${config.apiKey}",
        "Content-Type" to "application/json"
    )

    return openAiComplete(
        baseUrl = config.baseUrl,
        headers = headers,
        model = config.model,
        base64Image = base64Image,
        presetPrompt = presetPrompt,
        enableStreaming = enableStreaming,
        onChunk = onChunk,
        context = context
    )
}
