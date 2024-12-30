package studio1a23.altTextAi

import android.content.ClipData.*
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import studio1a23.altTextAi.SettingsDataStore.getSettings
import studio1a23.altTextAi.api.azureOpenAIFetchCompletion
import studio1a23.altTextAi.api.provideAzureOpenAIApiService
import java.io.ByteArrayOutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

sealed class UiState {
    data object Loading : UiState()
    data class Success(val data: String) : UiState()
    data class Error(val exception: Throwable) : UiState()
}

class ShareReceiverViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private var resultText: String = ""

    private val _prompt = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt

    fun processImage(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val base64Image = loadImageAsBase64(context, imageUri)
            if (base64Image != null) {
                // Fetch settings from preferences
                getSettings(context).collect { settings ->
                    try {
                        if (_prompt.value.isBlank()) {
                            _prompt.value = settings.presetPrompt
                        }
                        val presetPrompt = _prompt.value
                        val result = when (val config = settings.activeConfig) {
                            is AzureOpenAIConfig -> {
                                val apiService = provideAzureOpenAIApiService(config)
                                azureOpenAIFetchCompletion(apiService, base64Image, presetPrompt)
                            }
                            is OpenAIConfig -> TODO("OpenAI API support is not implemented yet")
                        }
                        when {
                            result.isSuccess -> {
                                resultText = result.getOrThrow()
                                _uiState.value = UiState.Success(result.getOrThrow())
                            }

                            result.isFailure -> {
                                _uiState.value = UiState.Error(
                                    result.exceptionOrNull() ?: Exception("Unknown exception")
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

    fun retry(context: Context, imageUri: Uri) {
        processImage(context, imageUri)
    }
}

@OptIn(ExperimentalEncodingApi::class)
suspend fun loadImageAsBase64(context: Context, imageUri: Uri): String? {
    val loader = context.imageLoader
    val request = Builder(context)
        .data(imageUri)
        .allowHardware(false)
        .build()
    val result = (loader.execute(request) as? SuccessResult)?.drawable
    return result?.let { drawable ->
        val bitmap = (drawable as BitmapDrawable).bitmap
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        Base64.encode(byteArray)
    }
}
