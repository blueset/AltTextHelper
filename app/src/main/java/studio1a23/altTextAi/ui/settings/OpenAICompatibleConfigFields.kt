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
    enableStreaming: Boolean,
    onValueChange: (OpenAICompatibleConfig) -> Unit
) {
    val helperText = if (enableStreaming) {
"""
${stringResource(R.string.openai_compat_guide_stream_1)}

```json
POST ${config.baseUrl.ifBlank { "<${stringResource(R.string.openai_compatible_base_url)}>" }}/chat/completions/
Content-Type: application/json
Authorization: Bearer ${config.apiKey.ifBlank { "<${stringResource(R.string.settings_api_key)}>" }}

{
 "model": "${config.model.ifBlank { "<${stringResource(R.string.settings_model)}>" }}",
 "stream": true,
 "messages": [
  {
   "role": "user",
   "content": [
    {
     "type": "text",
     "text": "${prompt.ifBlank { "<${stringResource(R.string.settings_preset_prompt)}>" }}"
    },
    {
     "type": "image_url",
     "image_url": {
      "url": "data:image/png;base64,<${stringResource(R.string.settings_base64_image)}>",
      "detail": "high"
     }
    }
   ]
  }
 ]
}
```

${stringResource(R.string.openai_compat_guide_stream_2)}

```json
HTTP 200 OK
Content-Type: text/event-stream

data: {"choices":[{"index":0,"delta":{"content":"Partial "},"finish_reason":null}]}

data: {"choices":[{"index":0,"delta":{"content":"response"},"finish_reason":null}]}

data: {"choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

data: [DONE]
```

${stringResource(R.string.openai_compat_guide_stream_3)}
""".trimIndent()
    } else {
"""
${stringResource(R.string.openai_compat_guide_1)}

```json
POST ${config.baseUrl.ifBlank { "<${stringResource(R.string.openai_compatible_base_url)}>" }}/chat/completions/
Content-Type: application/json
Authorization: Bearer ${config.apiKey.ifBlank { "<${stringResource(R.string.settings_api_key)}>" }}

{
 "model": "${config.model.ifBlank { "<${stringResource(R.string.settings_model)}>" }}",
 "messages": [
  {
   "role": "user",
   "content": [
    {
     "type": "text",
     "text": "${prompt.ifBlank { "<${stringResource(R.string.settings_preset_prompt)}>" }}"
    },
    {
     "type": "image_url",
     "image_url": {
      "url": "data:image/png;base64,<${stringResource(R.string.settings_base64_image)}>",
      "detail": "high"
     }
    }
   ]
  }
 ]
}
```

${stringResource(R.string.openai_compat_guide_2)}

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

${stringResource(R.string.openai_compat_guide_3)}
""".trimIndent()
    }
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
        markdown = helperText,
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
                enableStreaming = false,
                onValueChange = {}
            )
        }
    }
}
