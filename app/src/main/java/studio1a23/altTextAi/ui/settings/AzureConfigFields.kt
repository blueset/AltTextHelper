package studio1a23.altTextAi.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import studio1a23.altTextAi.AzureOpenAIConfig

@Composable
fun AzureConfigFields(
    config: AzureOpenAIConfig,
    onValueChange: (AzureOpenAIConfig) -> Unit
) {
    OutlinedTextField(
        value = config.apiKey,
        onValueChange = { onValueChange(config.copy(apiKey = it)) },
        label = { Text("API Key") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = config.endpoint,
        onValueChange = { onValueChange(config.copy(endpoint = it)) },
        label = { Text("API Endpoint") },
        modifier = Modifier.fillMaxWidth()
    )
}