package studio1a23.altTextAi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import studio1a23.altTextAi.ui.settings.AzureConfigFields
import studio1a23.altTextAi.ui.settings.ClaudeConfigFields
import studio1a23.altTextAi.ui.settings.GeminiConfigFields
import studio1a23.altTextAi.ui.settings.OpenAICompatibleConfigFields
import studio1a23.altTextAi.ui.settings.OpenAIConfigFields

private fun getApiTypeName(type: ApiType) =
    when (type) {
        ApiType.AzureOpenAi -> R.string.azure_openai_name
        ApiType.OpenAi -> R.string.openai_name
        ApiType.Claude -> R.string.claude_name
        ApiType.Gemini -> R.string.gemini_name
        ApiType.OpenAiCompatible -> R.string.openai_compatible_name
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            OutlinedTextField(
                value = settings.presetPrompt,
                onValueChange = { viewModel.updatePresetPrompt(it) },
                label = { Text(stringResource(R.string.settings_preset_prompt)) },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text(stringResource(R.string.settings_preset_prompt_help)) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_enable_streaming),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = settings.enableStreaming,
                    onCheckedChange = { viewModel.updateEnableStreaming(it) }
                )
            }

            // API Type Selection
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = stringResource(getApiTypeName(settings.apiType)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.settings_api_type)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    ApiType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(stringResource(getApiTypeName(type))) },
                            onClick = {
                                viewModel.updateApiType(type)
                                expanded = false
                            }
                        )
                    }
                }
            }

            // API-specific fields
            when (val config = settings.activeConfig) {
                is AzureOpenAIConfig ->
                    AzureConfigFields(
                        config = config,
                        onValueChange = viewModel::updateApiConfig
                    )

                is OpenAIConfig ->
                    OpenAIConfigFields(
                        config = config,
                        onValueChange = viewModel::updateApiConfig
                    )

                is ClaudeConfig ->
                    ClaudeConfigFields(
                        config = config,
                        onValueChange = viewModel::updateApiConfig
                    )

                is GeminiConfig ->
                    GeminiConfigFields(
                        config = config,
                        onValueChange = viewModel::updateApiConfig
                    )

                is OpenAICompatibleConfig ->
                    OpenAICompatibleConfigFields(
                        config = config,
                        prompt = settings.presetPrompt,
                        enableStreaming = settings.enableStreaming,
                        onValueChange = viewModel::updateApiConfig
                    )
            }
    }
}
