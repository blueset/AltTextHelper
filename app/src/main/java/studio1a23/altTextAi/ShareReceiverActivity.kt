package studio1a23.altTextAi

import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest.Builder
import kotlinx.coroutines.launch
import studio1a23.altTextAi.SettingsDataStore.getSettings
import studio1a23.altTextAi.ui.theme.AltTextHelperTheme

inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? =
    when {
        SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
    }

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? =
    when {
        SDK_INT >= 33 -> getParcelable(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelable(key) as? T
    }

class ShareReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val imageUri = intent.parcelable<Uri>(Intent.EXTRA_STREAM)
        if (imageUri != null) {
            setContent { AltTextHelperTheme { ShareReceiverScreen(imageUri) } }
        } else {
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareReceiverScreen(imageUri: Uri) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: ShareReceiverViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val prompt by viewModel.prompt.collectAsState()
    val settings by getSettings(context).collectAsState(null)
    val invalidConfig = settings?.activeConfig?.isFilled == false
    val noAutoStart = invalidConfig || settings?.presetPrompt?.isBlank() == true
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarCopiedToClipboard = stringResource(R.string.snackbar_copied_to_clipboard)
    val buttonDismiss = stringResource(R.string.button_dismiss)

    LaunchedEffect(imageUri, noAutoStart) {
        if (!noAutoStart) {
            viewModel.processImage(context, imageUri)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.title_generate_alt_text)) })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                AsyncImage(
                    model = Builder(context)
                        .data(imageUri)
                        .crossfade(true)
                        .placeholder(R.drawable.baseline_loop_24)
                        .error(R.drawable.baseline_error_outline_24)
                        .fallback(R.drawable.outline_scan_delete_24)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .aspectRatio(16f / 9f)
                )
            }
            Row {
                Card(
                    colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (settings == null) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.generating),
                            )
                        }
                    } else if (invalidConfig || noAutoStart) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text =
                                stringResource(
                                    if (invalidConfig)
                                        R.string.incomplete_configuration_prompt
                                    else R.string.no_preset_prompt
                                ),
                            )
                            FilledTonalButton(
                                modifier = Modifier.align(Alignment.End),
                                onClick = {
                                    context.startActivity(
                                        Intent(context, MainActivity::class.java).apply {
                                            putExtra("navigate_to", "settings")
                                        }
                                    )
                                }
                            ) { Text(stringResource(R.string.title_settings)) }
                        }
                    } else {
                        val onCopy: () -> Unit = {
                            viewModel.copyToClipboard(context)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = snackbarCopiedToClipboard,
                                    actionLabel = buttonDismiss
                                )
                            }
                        }
                        when (uiState) {
                            is UiState.Loading -> {
                                LoadingDialog()
                            }

                            is UiState.Streaming -> {
                                ResultDialog(
                                    resultText = (uiState as UiState.Streaming).currentData,
                                    onCopy = onCopy,
                                    isStreaming = true
                                )
                            }

                            is UiState.Success -> {
                                ResultDialog(
                                    resultText = (uiState as UiState.Success).data,
                                    onCopy = onCopy,
                                    isStreaming = false
                                )
                            }

                            is UiState.Error -> {
                                ErrorDialog(
                                    error = uiState as UiState.Error,
                                    onRetry = { viewModel.processImage(context, imageUri) },
                                )
                            }
                        }
                    }
                }
            }
            Row {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = prompt,
                        onValueChange = { viewModel.updatePrompt(it) },
                        label = { Text(stringResource(R.string.input_prompt)) },
                        enabled = uiState !is UiState.Loading && !invalidConfig
                    )
                    if (uiState is UiState.Loading) {
                        FilledTonalButton(
                            modifier = Modifier.align(Alignment.End),
                            onClick = { viewModel.cancelProcessing(context) },
                        ) { Text(stringResource(R.string.button_cancel)) }
                    } else {
                        FilledTonalButton(
                            modifier = Modifier.align(Alignment.End),
                            onClick = { viewModel.processImage(context, imageUri) },
                            enabled =
                            uiState !== UiState.Loading &&
                                    !invalidConfig &&
                                    prompt.isNotBlank()
                        ) { Text(stringResource(R.string.button_update)) }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultDialog(resultText: String, onCopy: () -> Unit, isStreaming: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (isStreaming) stringResource(R.string.generating) else stringResource(R.string.title_result),
                style = MaterialTheme.typography.titleLarge
            )
        }
        SelectionContainer {
            Text(
                resultText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.animateContentSize()
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            FilledTonalButton(
                onClick = onCopy,
                enabled = !isStreaming && resultText.isNotEmpty()
            ) {
                Text(stringResource(R.string.button_copy_to_clipboard))
            }
        }
    }
}

@Composable
fun ErrorDialog(error: UiState.Error, onRetry: () -> Unit) {
    val errorMessage = error.exception.message ?: stringResource(R.string.unknown_error)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
        if (error.data?.isEmpty() == false) {
            SelectionContainer { Text(error.data, style = MaterialTheme.typography.bodyLarge) }
            HorizontalDivider()
        }
        Text("Error", style = MaterialTheme.typography.titleLarge)
        SelectionContainer { Text(errorMessage, style = MaterialTheme.typography.bodyLarge) }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            FilledTonalButton(onClick = onRetry) { Text(stringResource(R.string.button_retry)) }
        }
    }
}

@Composable
fun LoadingDialog() {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) { Text(stringResource(R.string.generating), style = MaterialTheme.typography.titleLarge) }
}
