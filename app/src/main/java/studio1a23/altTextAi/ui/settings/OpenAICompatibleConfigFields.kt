package studio1a23.altTextAi.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import studio1a23.altTextAi.OpenAICompatibleConfig
import studio1a23.altTextAi.R
import studio1a23.altTextAi.ui.components.MarkdownText

@Composable
fun OpenAICompatibleConfigFields(
    config: OpenAICompatibleConfig,
    prompt: String,
    onValueChange: (OpenAICompatibleConfig) -> Unit
) {
    OutlinedTextField(
        value = config.apiKey,
        onValueChange = { onValueChange(config.copy(apiKey = it)) },
        label = { Text(stringResource(R.string.settings_api_key)) },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = config.baseUrl,
        onValueChange = { onValueChange(config.copy(baseUrl = it)) },
        label = { Text(stringResource(R.string.openai_compatible_base_url)) },
        placeholder = { Text("https://api.example.com/v1/") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = config.model,
        onValueChange = { onValueChange(config.copy(model = it)) },
        label = { Text(stringResource(R.string.settings_model)) },
        modifier = Modifier.fillMaxWidth()
    )
    MarkdownText(
        markdown = """
The “OpenAI-compatible” config assumes an API endpoint that behaves similar to that of OpenAI. Under this config, it will send an HTTP POST request as follows.

```json
POST ${config.baseUrl.ifBlank { "<Base URL>" }}/chat/completions/
Content-Type: application/json
Authorization: Bearer ${config.apiKey.ifBlank { "<API Key>" }}

{
 "model": "${config.model.ifBlank { "<Model>" }}",
 "messages": [
  {
   "role": "user",
   "content": [
    {
     "type": "text",
     "text": "${prompt.ifBlank { "<Preset Prompt>" }}"
    },
    {
     "type": "image_url",
     "image_url": {
      "url": "data:image/png;base64,<Base64 Image>",
      "detail": "high"
     }
    }
   ]
  }
 ]
}
```

And expect a successful response in JSON with at least the following fields:

```json
HTTP 200 OK
Content-Type: application/json

{
 "id": "<ID>",
 "created": <Timestamp>,
 "model": "${config.model.ifBlank { "<Model>" }}",
 "choices": [
  {
   "index": 0,
   "message": {
    "content": "<Alt Text>",
    "role": "assistant"
   }
  }
 ]
}
```

You can use this option if the above API endpoint matches the API specification of your service provider, and the provider is not otherwise already supported by the app.
    """.trimIndent(),
        modifier = Modifier.fillMaxWidth().wrapContentHeight()
    )
}

@Preview(showBackground = true)
@Composable
fun OpenAICompatibleConfigFieldsPreview() {
    MaterialTheme {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OpenAICompatibleConfigFields(
                OpenAICompatibleConfig("", "", ""),
                prompt = "",
                onValueChange = {}
            )
        }
    }
}
