package studio1a23.altTextAi.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import studio1a23.altTextAi.OpenAIConfig

@Composable
fun OpenAIConfigFields(
    config: OpenAIConfig,
    onValueChange: (OpenAIConfig) -> Unit
) {
    OutlinedTextField(
        value = config.apiKey,
        onValueChange = { onValueChange(config.copy(apiKey = it)) },
        label = { Text("API Key") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = config.organization,
        onValueChange = { onValueChange(config.copy(organization = it)) },
        label = { Text("Organization (Optional)") },
        modifier = Modifier.fillMaxWidth()
    )
}