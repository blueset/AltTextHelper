package studio1a23.altTextAi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val application: Application) : AndroidViewModel(application) {
    private val _settings = MutableStateFlow(Settings.build())
    val settings: StateFlow<Settings> = _settings

    init {
        viewModelScope.launch {
            SettingsDataStore.getSettings(application.applicationContext).collect {
                _settings.value = it
            }
        }
    }

    fun updateApiType(type: ApiType) {
        _settings.value = _settings.value.copy(apiType = type)
    }

    fun updateApiConfig(config: ApiConfig) {
        val newConfigs = when (config) {
            is AzureOpenAIConfig -> _settings.value.configs.copy(azure = config)
            is OpenAIConfig -> _settings.value.configs.copy(openai = config)
        }
        _settings.value = _settings.value.copy(configs = newConfigs)
    }

    fun updatePresetPrompt(presetPrompt: String) {
        _settings.value = _settings.value.copy(presetPrompt = presetPrompt)
    }

    fun saveSettings() {
        viewModelScope.launch {
            // Ensure Azure endpoint ends with "/"
//            if (_settings.value.activeConfig is AzureOpenAIConfig) {
//                val config = _settings.value.activeConfig as AzureOpenAIConfig
//                if (!config.endpoint.endsWith("/")) {
//                    updateApiConfig(config.copy(endpoint = config.endpoint + "/"))
//                }
//            }
            
            SettingsDataStore.saveSettings(
                application.applicationContext,
                _settings.value
            )
        }
    }
}
