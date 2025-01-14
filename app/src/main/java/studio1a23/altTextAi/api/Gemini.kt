package studio1a23.altTextAi.api

import android.content.Context
import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.FinishReason
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.flow.collect
import studio1a23.altTextAi.GeminiConfig
import studio1a23.altTextAi.MAX_TOKENS
import studio1a23.altTextAi.R

suspend fun geminiComplete(
    config: GeminiConfig,
    imageBitmap: Bitmap,
    presetPrompt: String,
    enableStreaming: Boolean = false,
    onChunk: (String) -> Unit = {},
    context: Context
): Result<String> {
    if (config.apiKey.isEmpty() || config.model.isEmpty()) {
        return Result.failure(IllegalArgumentException(context.getString(R.string.incomplete_configuration)))
    }

    try {
        val model = GenerativeModel(
            modelName = config.model,
            apiKey = config.apiKey,
            generationConfig = generationConfig {
                maxOutputTokens = MAX_TOKENS
            }
        )

        if (enableStreaming) {
            val responseBuilder = StringBuilder()
            model.generateContentStream(content {
                image(imageBitmap)
                text(presetPrompt)
            }).collect { response ->
                response.text?.let { chunk ->
                    onChunk(chunk)
                    responseBuilder.append(chunk)
                }
                response.candidates.first().finishReason?.let {
                    if (it != FinishReason.STOP) {
                        onChunk(" [finishReason=$it]")
                    }
                }
            }
            val finalResponse = responseBuilder.toString()
            return if (finalResponse.isBlank()) {
                Result.failure(Exception(context.getString(R.string.error_no_response)))
            } else {
                Result.success(finalResponse)
            }
        } else {
            val result = model.generateContent(content {
                image(imageBitmap)
                text(presetPrompt)
            })

            return if (result.text == null || result.text!!.isBlank()) {
                Result.failure(Exception(context.getString(R.string.error_no_response)))
            } else {
                Result.success(result.text!!)
            }
        }
    } catch (e: Exception) {
        return Result.failure(e)
    }
}