package studio1a23.altTextAi.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import studio1a23.altTextAi.AzureOpenAIConfig
import studio1a23.altTextAi.R
import studio1a23.altTextAi.ui.components.AutocompleteTextField

private val modelOptions = listOf(
    "gpt-4o",
    "gpt-4o-mini",
    "o1",
)

@Composable
fun AzureConfigFields(
    config: AzureOpenAIConfig,
    onValueChange: (AzureOpenAIConfig) -> Unit
) {
    Text(
        AnnotatedString.fromHtml(
            stringResource(R.string.azure_openai_guide),
            linkStyles = TextLinkStyles(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.tertiary,
                    textDecoration = TextDecoration.Underline
                )
            )
        ),
        style = MaterialTheme.typography.bodyMedium,
    )
    OutlinedTextField(
        value = config.apiKey,
        onValueChange = { onValueChange(config.copy(apiKey = it)) },
        label = { Text(stringResource(R.string.settings_api_key)) },
        supportingText = { Text(stringResource(R.string.azure_openai_api_key_help)) },
        placeholder = { Text("0123456789abcdef0123456789abcdef") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = config.resourceName,
        onValueChange = { onValueChange(config.copy(resourceName = it)) },
        label = { Text(stringResource(R.string.azure_openai_resource_name)) },
        supportingText = { Text(AnnotatedString.fromHtml(stringResource(R.string.azure_openai_api_resource_name_help))) },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = config.deploymentId,
        onValueChange = { onValueChange(config.copy(deploymentId = it)) },
        label = { Text(stringResource(R.string.azure_openai_deployment_id)) },
        modifier = Modifier.fillMaxWidth()
    )
    AutocompleteTextField(
        candidates = modelOptions,
        value = config.model,
        onValueChange = { onValueChange(config.copy(model = it)) },
        modifier = Modifier.fillMaxWidth(),
        content = { value, setValue, modifier ->
            OutlinedTextField(
                value = value,
                onValueChange = setValue,
                label = { Text(stringResource(R.string.settings_model)) },
                modifier = modifier.fillMaxWidth()
            )
        }
    )
}
