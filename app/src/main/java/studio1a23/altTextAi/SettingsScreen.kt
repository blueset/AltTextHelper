package studio1a23.altTextAi

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsState(initial = Settings("", "", ""))
    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = settings.endpoint,
            onValueChange = { viewModel.updateEndpoint(it) },
            label = { Text("API Endpoint") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = settings.apiKey,
            onValueChange = { viewModel.updateApiKey(it) },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = settings.presetPrompt,
            onValueChange = { viewModel.updatePresetPrompt(it) },
            label = { Text("Preset Prompt") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.saveSettings() }, modifier = Modifier.align(
                Alignment.End
            )
        ) {
            Text("Save")
        }
    }
}
