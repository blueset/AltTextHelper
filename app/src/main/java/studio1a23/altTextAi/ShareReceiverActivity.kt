package studio1a23.altTextAi

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.ImageRequest.*
import coil.request.SuccessResult
import studio1a23.altTextAi.ui.theme.AltTextHelperTheme
import java.io.ByteArrayOutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

class ShareReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val imageUri = intent.parcelable<Uri>(Intent.EXTRA_STREAM)
        if (imageUri != null) {
            setContent {
                AltTextHelperTheme {
                    ShareReceiverScreen(imageUri)
                }
            }
        } else {
            finish()
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareReceiverScreen(imageUri: Uri) {
    val context = LocalContext.current
    val viewModel: ShareReceiverViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val prompt by viewModel.prompt.collectAsState()

    LaunchedEffect(imageUri) {
        viewModel.processImage(context, imageUri)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Generate Alt Text") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                AsyncImage(
                    model = Builder(context).data(imageUri).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .aspectRatio(16f / 9f)
                )
            }
            Row {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (uiState) {
                        is UiState.Loading -> {
                            LoadingDialog()
                        }

                        is UiState.Success -> {
                            ResultDialog(
                                resultText = (uiState as UiState.Success).data,
                                onCopy = { viewModel.copyToClipboard(context) },
                            )
                        }

                        is UiState.Error -> {
                            ErrorDialog(
                                errorMessage = (uiState as UiState.Error).exception.message
                                    ?: "Unknown error",
                                onRetry = { viewModel.retry(context, imageUri) },
                            )
                        }
                    }
                }
            }
            Row {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = {
                            viewModel.updatePrompt(it)
                        },
                        label = { Text("Prompt") },
                        enabled = uiState !== UiState.Loading
                    )
                    FilledTonalButton(
                        onClick = { viewModel.processImage(context, imageUri) },
                        enabled = uiState !== UiState.Loading
                    ) {
                        Text("Update")
                    }
                }
            }
        }
    }
}


@Composable
fun ResultDialog(resultText: String, onCopy: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        Text("Result", style = MaterialTheme.typography.titleLarge)
        SelectionContainer {
            Text(resultText, style = MaterialTheme.typography.bodyLarge)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            FilledTonalButton(onClick = onCopy) {
                Text("Copy to Clipboard")
            }
        }
    }
}

@Composable
fun ErrorDialog(errorMessage: String, onRetry: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        Text("Error", style = MaterialTheme.typography.titleLarge)
        SelectionContainer {
            Text(errorMessage, style = MaterialTheme.typography.bodyLarge)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            FilledTonalButton(onClick = onRetry) {
                Text("Retry")
            }
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
    ) {
        Text("Loading…", style = MaterialTheme.typography.titleLarge)
    }
}