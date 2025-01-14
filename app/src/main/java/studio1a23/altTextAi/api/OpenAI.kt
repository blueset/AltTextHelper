package studio1a23.altTextAi.api

import android.content.Context
import studio1a23.altTextAi.OpenAIConfig
import studio1a23.altTextAi.R

suspend fun openApiComplete(
    config: OpenAIConfig,
    base64Image: String,
    presetPrompt: String,
    enableStreaming: Boolean = false,
    onChunk: (String) -> Unit = {},
    context: Context
): Result<String> {
    if (config.apiKey.isEmpty() || config.model.isEmpty()) {
        return Result.failure(IllegalArgumentException(context.getString(R.string.incomplete_configuration)))
    }

    val headers = buildMap {
        put("Authorization", "Bearer ${config.apiKey}")
        if (config.organization.isNotEmpty()) {
            put("OpenAI-Organization", config.organization)
        }
    }

    return openAiComplete(
        baseUrl = "https://api.openai.com/v1",
        headers = headers,
        model = config.model,
        base64Image = base64Image,
        presetPrompt = presetPrompt,
        enableStreaming = enableStreaming,
        onChunk = onChunk,
        context = context
    )
}
