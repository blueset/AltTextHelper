package studio1a23.altTextAi

import android.content.ClipData.newPlainText
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest.Builder
import coil.request.SuccessResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import studio1a23.altTextAi.SettingsDataStore.getSettings
import studio1a23.altTextAi.api.azureOpenApiComplete
import studio1a23.altTextAi.api.claudeComplete
import studio1a23.altTextAi.api.geminiComplete
import studio1a23.altTextAi.api.openApiCompatibleComplete
import studio1a23.altTextAi.api.openApiComplete
import java.io.ByteArrayOutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

sealed class UiState {
    data object Loading : UiState()
    data class Success(val data: String) : UiState()
    data class Streaming(val currentData: String) : UiState()
    data class Error(val exception: Throwable, private val previousUiState: UiState? = null) : UiState() {
        val data: String? =
            if (previousUiState is Success) previousUiState.data else if (previousUiState is Streaming) previousUiState.currentData else null
    }
}

class ShareReceiverViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private var resultText: String = ""
    private var currentJob: kotlinx.coroutines.Job? = null
    private var streamingBuffer: StringBuilder = StringBuilder()

    private val _prompt = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt

    // Callback for handling streaming updates
    private val streamingCallback: (String) -> Unit = { chunk ->
        streamingBuffer.append(chunk)
        _uiState.value = UiState.Streaming(streamingBuffer.toString())
    }

    fun cancelProcessing(context: Context) {
        currentJob?.cancel()
        _uiState.value =
            UiState.Error(Exception(context.getString(R.string.request_cancelled)), _uiState.value)
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
                            val onChunk: (String) -> Unit = { chunk ->
                                streamingBuffer.append(chunk)
                                _uiState.value = UiState.Streaming(streamingBuffer.toString())
                            }
                            val result = when (val config = settings.activeConfig) {
                                is OpenAIConfig -> {
                                    openApiComplete(
                                        config = config,
                                        base64Image = base64Image,
                                        presetPrompt = presetPrompt,
                                        enableStreaming = settings.enableStreaming,
                                        onChunk = onChunk,
                                        context = context,
                                    )
                                }

                                is AzureOpenAIConfig ->
                                    azureOpenApiComplete(
                                        config = config,
                                        base64Image = base64Image,
                                        presetPrompt = presetPrompt,
                                        enableStreaming = settings.enableStreaming,
                                        onChunk = onChunk,
                                        context = context,
                                    )

                                is ClaudeConfig ->
                                    claudeComplete(
                                        config = config,
                                        base64Image = base64Image,
                                        presetPrompt = presetPrompt,
                                        enableStreaming = settings.enableStreaming,
                                        onChunk = onChunk,
                                        context = context,
                                    )

                                is GeminiConfig ->
                                    geminiComplete(
                                        config = config,
                                        imageBitmap = bitmapImage,
                                        presetPrompt = presetPrompt,
                                        enableStreaming = settings.enableStreaming,
                                        onChunk = onChunk,
                                        context = context
                                    )

                                is OpenAICompatibleConfig ->
                                    openApiCompatibleComplete(
                                        config = config,
                                        base64Image = base64Image,
                                        presetPrompt = presetPrompt,
                                        enableStreaming = settings.enableStreaming,
                                        onChunk = onChunk,
                                        context = context
                                    )
                            }
                            // Reset streaming buffer before processing result
                            streamingBuffer.clear()
                            when {
                                result.isSuccess -> {
                                    resultText = result.getOrThrow()
                                    _uiState.value = UiState.Success(result.getOrThrow())
                                }

                                result.isFailure -> {
                                    Log.e(
                                        "ShareReceiverViewModel",
                                        "Error: ${result.exceptionOrNull()}",
                                        result.exceptionOrNull()
                                    )
                                    _uiState.value =
                                        UiState.Error(
                                            result.exceptionOrNull()
                                                ?: Exception(context.getString(R.string.unknown_exception)),
                                            _uiState.value
                                        )
                                }
                            }
                        } catch (e: NotImplementedError) {
                            _uiState.value = UiState.Error(e, _uiState.value)
                        }
                    }
                } else {
                    _uiState.value =
                        UiState.Error(Exception(context.getString(R.string.error_failed_to_load_image)), _uiState.value)
                }
            }
    }

    fun updatePrompt(newPrompt: String) {
        _prompt.value = newPrompt
    }

    fun copyToClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = newPlainText(context.getString(R.string.title_result), resultText)
        clipboard.setPrimaryClip(clip)
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
