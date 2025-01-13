package studio1a23.altTextAi.api

import android.content.Context
import studio1a23.altTextAi.AzureOpenAIConfig
import studio1a23.altTextAi.R

suspend fun azureOpenApiComplete(
    config: AzureOpenAIConfig,
    base64Image: String,
    presetPrompt: String,
    context: Context
): Result<String> {
    if (config.apiKey.isEmpty() || config.deploymentId.isEmpty() || config.resourceName.isEmpty() || config.model.isEmpty()) {
        return Result.failure(IllegalArgumentException(context.getString(R.string.incomplete_configuration)))
    }

    val headers = mapOf(
        "api-key" to config.apiKey,
        "Content-Type" to "application/json"
    )

    val baseUrl = "https://${config.resourceName}.openai.azure.com/openai/deployments/${config.deploymentId}"

    return openAiComplete(
        baseUrl = baseUrl,
        headers = headers,
        queryParameters = mapOf("api-version" to "2024-08-01-preview"),
        model = config.model,
        base64Image = base64Image,
        presetPrompt = presetPrompt,
        context = context
    )
}
