package studio1a23.altTextAi

import android.content.ClipData.newPlainText
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest.Builder
import coil.request.SuccessResult
import java.io.ByteArrayOutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import studio1a23.altTextAi.SettingsDataStore.getSettings
import studio1a23.altTextAi.api.azureOpenApiComplete
import studio1a23.altTextAi.api.claudeComplete
import studio1a23.altTextAi.api.geminiComplete
import studio1a23.altTextAi.api.openApiCompatibleComplete
import studio1a23.altTextAi.api.openApiComplete

sealed class UiState {
    data object Loading : UiState()
    data class Success(val data: String) : UiState()
    data class Error(val exception: Throwable) : UiState()
}

class ShareReceiverViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private var resultText: String = ""
    private var currentJob: kotlinx.coroutines.Job? = null

    private val _prompt = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt

    fun cancelProcessing() {
        currentJob?.cancel()
        _uiState.value = UiState.Error(Exception("Request cancelled"))
    }

    fun processImage(context: Context, imageUri: Uri) {
        currentJob?.cancel()
        currentJob =
            viewModelScope.launch {
                _uiState.value = UiState.Loading
                val bitmapImage = loadImageAsBitmap(context, imageUri)
                if (bitmapImage != null) {
                    val base64Image by lazy { bitmapImage.toPngBase64() }
                    // Fetch settings from preferences
                    getSettings(context).collect { settings ->
                        try {
                            if (_prompt.value.isBlank()) {
                                _prompt.value = settings.presetPrompt
                            }
                            val presetPrompt = _prompt.value
                            val result =
                                when (val config = settings.activeConfig) {
                                    is AzureOpenAIConfig ->
                                        azureOpenApiComplete(
                                            config,
                                            base64Image,
                                            presetPrompt,
                                            context
                                        )

                                    is OpenAIConfig ->
                                        openApiComplete(
                                            config,
                                            base64Image,
                                            presetPrompt,
                                            context
                                        )

                                    is ClaudeConfig ->
                                        claudeComplete(
                                            config,
                                            base64Image,
                                            presetPrompt,
                                            context
                                        )

                                    is GeminiConfig ->
                                        geminiComplete(
                                            config,
                                            bitmapImage,
                                            presetPrompt,
                                            context
                                        )

                                    is OpenAICompatibleConfig ->
                                        openApiCompatibleComplete(
                                            config,
                                            base64Image,
                                            presetPrompt,
                                            context
                                        )
                                }
                            when {
                                result.isSuccess -> {
                                    resultText = result.getOrThrow()
                                    _uiState.value = UiState.Success(result.getOrThrow())
                                }

                                result.isFailure -> {
                                    _uiState.value =
                                        UiState.Error(
                                            result.exceptionOrNull()
                                                ?: Exception("Unknown exception")
                                        )
                                }
                            }
                        } catch (e: NotImplementedError) {
                            _uiState.value = UiState.Error(e)
                        }
                    }
                } else {
                    _uiState.value = UiState.Error(Exception("Failed to load image"))
                }
            }
    }

    fun updatePrompt(newPrompt: String) {
        _prompt.value = newPrompt
    }

    fun copyToClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = newPlainText("Result", resultText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}

suspend fun loadImageAsBitmap(context: Context, imageUri: Uri): Bitmap? {
    val loader = context.imageLoader
    val request = Builder(context).data(imageUri).allowHardware(false).build()
    val result = (loader.execute(request) as? SuccessResult)?.drawable
    return result?.let { drawable -> (drawable as BitmapDrawable).bitmap }
}

@OptIn(ExperimentalEncodingApi::class)
fun Bitmap.toPngBase64(): String {
    val outputStream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    val byteArray = outputStream.toByteArray()
    return Base64.encode(byteArray)
}
