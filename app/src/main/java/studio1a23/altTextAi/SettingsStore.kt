package studio1a23.altTextAi

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object SettingsDataStore {
    private val Context.dataStore by preferencesDataStore(name = "settings")

    val ENDPOINT = stringPreferencesKey("endpoint")
    val API_KEY = stringPreferencesKey("api_key")
    val PRESET_PROMPT = stringPreferencesKey("preset_prompt")

    suspend fun saveSettings(context: Context, endpoint: String, apiKey: String, presetPrompt: String) {
        context.dataStore.edit { settings ->
            settings[ENDPOINT] = endpoint
            settings[API_KEY] = apiKey
            settings[PRESET_PROMPT] = presetPrompt
        }
    }

    fun getSettings(context: Context): Flow<Settings> {
        return context.dataStore.data.map { preferences ->
            Settings(
                endpoint = preferences[ENDPOINT] ?: "",
                apiKey = preferences[API_KEY] ?: "",
                presetPrompt = preferences[PRESET_PROMPT] ?: ""
            )
        }
    }
}

data class Settings(
    val endpoint: String,
    val apiKey: String,
    val presetPrompt: String
)
