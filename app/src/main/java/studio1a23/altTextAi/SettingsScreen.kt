package studio1a23.altTextAi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import studio1a23.altTextAi.ui.settings.AzureConfigFields
import studio1a23.altTextAi.ui.settings.OpenAIConfigFields

val ApiTypeNames = mapOf(
    ApiType.AzureOpenAi to R.string.azure_openai_name,
    ApiType.OpenAi to R.string.openai_name
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = settings.presetPrompt,
            onValueChange = { viewModel.updatePresetPrompt(it) },
            label = { Text(stringResource(R.string.settings_preset_prompt)) },
            modifier = Modifier.fillMaxWidth()
        )

        // API Type Selection
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = stringResource(ApiTypeNames.getValue(settings.apiType)),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.settings_api_type)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ApiTypeNames.forEach { (type, name) ->
                    DropdownMenuItem(
                        text = { Text(stringResource(name)) },
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
            is AzureOpenAIConfig -> AzureConfigFields(
                config = config,
                onValueChange = viewModel::updateApiConfig
            )
            is OpenAIConfig -> OpenAIConfigFields(
                config = config,
                onValueChange = viewModel::updateApiConfig
            )
        }

        Button(
            onClick = { viewModel.saveSettings() },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(stringResource(R.string.button_save))
        }
    }
}
