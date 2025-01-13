package studio1a23.altTextAi

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import studio1a23.altTextAi.SettingsDataStore.getSettings
import studio1a23.altTextAi.ui.components.MarkdownText
import studio1a23.altTextAi.ui.components.StepItem
import studio1a23.altTextAi.ui.components.StepState
import studio1a23.altTextAi.ui.components.VerticalStepper
import studio1a23.altTextAi.ui.theme.AltTextHelperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val pickImage =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                if (uri != null) {
                    val intent =
                        Intent(this, ShareReceiverActivity::class.java).apply {
                            putExtra(Intent.EXTRA_STREAM, uri)
                        }
                    startActivity(intent)
                }
            }

        setContent {
            AltTextHelperTheme { MainScreen(onPickImage = { pickImage.launch("image/*") }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onPickImage: () -> Unit) {
    val navController = rememberNavController()
    val baseTitle = "" // stringResource(R.string.app_name)
    val settingsTitle = stringResource(R.string.title_settings)
    val (title, setTitle) = remember { mutableStateOf(baseTitle) }
    val (canPop, setCanPop) = remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity
        val navigateTo = activity?.intent?.getStringExtra("navigate_to")
        if (navigateTo == "settings") {
            navController.navigate("settings")
        }
    }

    navController.addOnDestinationChangedListener { controller, _, _ ->
        setCanPop(controller.previousBackStackEntry != null)
        setTitle(
            when (controller.currentBackStackEntry?.destination?.route) {
                "home" -> baseTitle
                "settings" -> settingsTitle
                else -> baseTitle
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (canPop) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription =
                                stringResource(R.string.button_back)
                            )
                        }
                    }
                },
                actions = {
                    if (!canPop) {
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(Icons.Default.Settings, contentDescription = settingsTitle)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                HomeContent(
                    onSettings = { navController.navigate("settings") },
                    onPickImage = onPickImage
                )
            }
            composable("settings") { SettingsScreen(snackbarHostState = snackbarHostState) }
        }
    }
}

@Composable
fun HomeContent(onSettings: () -> Unit, onPickImage: () -> Unit) {
    val context = LocalContext.current
    val settings by getSettings(context).collectAsState(null)
    val invalidConfig = settings?.activeConfig?.isFilled != true

    Column(
        Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(Modifier.height(64.dp))
        Text(
            stringResource(R.string.welcome_to, stringResource(R.string.app_name)),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.headlineLarge.copy(
                lineBreak = LineBreak.Heading
            )
        )
        VerticalStepper(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            StepItem(
                title = {
                    Text(stringResource(R.string.guide_set_up, stringResource(R.string.app_name)))
                },
                state = if (!invalidConfig) StepState.Success else StepState.Active,
                stepNumber = 1,
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MarkdownText(
                            stringResource(
                                R.string.guide_set_up_body_text,
                                stringResource(R.string.app_name)
                            )
                        )
                        FilledTonalButton(onClick = onSettings) {
                            Text(stringResource(R.string.title_settings))
                        }
                    }
                }
            )

            StepItem(
                title = { Text(stringResource(R.string.guide_generate)) },
                state = if (!invalidConfig) StepState.Active else StepState.Upcoming,
                stepNumber = 2,
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(context.getString(R.string.guide_generate_description))
                        FilledTonalButton(onClick = onPickImage, enabled = !invalidConfig) {
                            Text(stringResource(R.string.button_pick_a_picture))
                        }
                    }
                }
            )
        }
    }
}

@Preview
@Composable
fun MainScreenPreview() {
    AltTextHelperTheme { MainScreen(onPickImage = {}) }
}
