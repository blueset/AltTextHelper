package studio1a23.altTextAi

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val application: Application) : AndroidViewModel(application) {
    private val _settings = MutableStateFlow(Settings("", "", ""))
    val settings: StateFlow<Settings> = _settings

    init {
        viewModelScope.launch {
            SettingsDataStore.getSettings(application.applicationContext).collect {
                _settings.value = it
            }
        }
    }

    fun updateEndpoint(endpoint: String) {
        _settings.value = _settings.value.copy(endpoint = endpoint)
    }

    fun updateApiKey(apiKey: String) {
        _settings.value = _settings.value.copy(apiKey = apiKey)
    }

    fun updatePresetPrompt(presetPrompt: String) {
        _settings.value = _settings.value.copy(presetPrompt = presetPrompt)
    }

    fun saveSettings() {
        viewModelScope.launch {
            if (!_settings.value.endpoint.endsWith("/")) {
                _settings.value = _settings.value.copy(endpoint = _settings.value.endpoint + "/")
            }
            SettingsDataStore.saveSettings(
                application.applicationContext,
                _settings.value.endpoint,
                _settings.value.apiKey,
                _settings.value.presetPrompt
            )
        }
    }
}
