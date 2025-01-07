package studio1a23.altTextAi

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed interface ApiConfig {
    companion object {
        fun fromApiType(type: ApiType, configs: ApiConfigs): ApiConfig = when (type) {
            ApiType.AzureOpenAi -> configs.azure
            ApiType.OpenAi -> configs.openai
            ApiType.Claude -> configs.claude
            ApiType.Gemini -> configs.gemini
            ApiType.OpenAiCompatible -> configs.openaiCompatible
        }
    }

    val isFilled: Boolean
}

@Serializable
data class AzureOpenAIConfig(
    val apiKey: String,
    val resourceName: String,
    val deploymentId: String,
    val model: String = "gpt-4o",
) : ApiConfig {
    override val isFilled: Boolean
        get() = apiKey.isNotEmpty() || resourceName.isNotEmpty() || deploymentId.isNotEmpty()
}

@Serializable
data class OpenAIConfig(
    val apiKey: String,
    val organization: String = "", // Optional
    val model: String = "gpt-4o",
) : ApiConfig {
    override val isFilled: Boolean
        get() = apiKey.isNotEmpty() || model.isNotEmpty()
}

@Serializable
data class OpenAICompatibleConfig(
    val apiKey: String,
    val baseUrl: String = "",
    val model: String = "",
) : ApiConfig {
    val baseUrlWithSlash: String
        get() = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
    override val isFilled: Boolean
        get() = apiKey.isNotEmpty() || baseUrl.isNotEmpty() || model.isNotEmpty()
}

@Serializable
data class ClaudeConfig(
    val apiKey: String,
    val model: String = "claude-3-5-sonnet-latest",
) : ApiConfig {
    override val isFilled: Boolean
        get() = apiKey.isNotEmpty() || model.isNotEmpty()
}

@Serializable
data class GeminiConfig(
    val apiKey: String,
    val model: String = "gemini-2.0-flash-exp",
) : ApiConfig {
    override val isFilled: Boolean
        get() = apiKey.isNotEmpty() || model.isNotEmpty()
}

@Serializable
data class ApiConfigs(
    val azure: AzureOpenAIConfig = AzureOpenAIConfig("", "", ""),
    val openai: OpenAIConfig = OpenAIConfig(""),
    val claude: ClaudeConfig = ClaudeConfig(""),
    val gemini: GeminiConfig = GeminiConfig(""),
    val openaiCompatible: OpenAICompatibleConfig = OpenAICompatibleConfig(""),
)

enum class ApiType {
    AzureOpenAi,
    OpenAi,
    Claude,
    Gemini,
    OpenAiCompatible;

    companion object {
        fun fromString(value: String?): ApiType {
            return try {
                value?.let { valueOf(it) } ?: AzureOpenAi
            } catch (e: IllegalArgumentException) {
                AzureOpenAi
            }
        }
    }

    override fun toString(): String = name
}

val DEFAULT_API_TYPE = ApiType.AzureOpenAi

object SettingsDataStore {
    private val Context.dataStore by preferencesDataStore(name = "settings")

    private val API_CONFIG = stringPreferencesKey("api_config")
    private val API_TYPE = stringPreferencesKey("api_type")
    private val PRESET_PROMPT = stringPreferencesKey("preset_prompt")

    suspend fun saveSettings(context: Context, newSettings: Settings) {
        context.dataStore.edit { settings ->
            settings[API_CONFIG] = Json.encodeToString(newSettings.configs)
            settings[API_TYPE] = newSettings.apiType.toString()
            settings[PRESET_PROMPT] = newSettings.presetPrompt
        }
    }

    fun getSettings(context: Context): Flow<Settings> {
        return context.dataStore.data.map { preferences ->
            val apiType = ApiType.fromString(preferences[API_TYPE])
            val apiConfigJson = preferences[API_CONFIG]

            val configs = try {
                apiConfigJson?.let { Json.decodeFromString<ApiConfigs>(apiConfigJson) }
            } catch (e: Exception) {
                null
            }

            Settings.build(
                configs = configs,
                apiType = apiType,
                presetPrompt = preferences[PRESET_PROMPT]
            )
        }
    }
}

data class Settings(
    val configs: ApiConfigs,
    val apiType: ApiType,
    val presetPrompt: String
) {
    companion object {
        fun build(
            configs: ApiConfigs? = null,
            apiType: ApiType? = null,
            presetPrompt: String? = null
        ) = Settings(
            configs ?: ApiConfigs(),
            apiType ?: DEFAULT_API_TYPE,
            presetPrompt ?: ""
        )
    }

    val activeConfig: ApiConfig
        get() = ApiConfig.fromApiType(apiType, configs)
}
