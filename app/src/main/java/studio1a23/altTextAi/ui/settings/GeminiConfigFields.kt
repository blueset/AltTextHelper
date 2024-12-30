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
import studio1a23.altTextAi.GeminiConfig
import studio1a23.altTextAi.R
import studio1a23.altTextAi.ui.components.AutocompleteTextField

private val modelOptions = listOf(
    "gemini-2.0-flash-exp",
    "gemini-2.0-flash-thinking-exp",
    "gemini-1.5-flash",
    "gemini-1.5-flash-8b",
    "gemini-1.5-pro",
)

@Composable
fun GeminiConfigFields(
    config: GeminiConfig,
    onValueChange: (GeminiConfig) -> Unit
) {
    Text(
        AnnotatedString.fromHtml(
            stringResource(R.string.gemini_guide),
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