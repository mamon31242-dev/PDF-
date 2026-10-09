package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.PdfProjectEntity
import com.example.data.PdfRepository
import com.example.monetization.AdMobManager
import com.example.monetization.BillingManager
import com.example.ui.editor.PdfEditorScreen
import com.example.ui.home.HomeScreen
import com.example.ui.splash.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

sealed class Screen {
    data object Splash : Screen()
    data object Home : Screen()
    data class Editor(val project: PdfProjectEntity) : Screen()
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: PdfRepository
    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize AdMob and In-App Billing
        AdMobManager.initialize(applicationContext)
        billingManager = BillingManager(applicationContext, lifecycleScope)
        billingManager.startConnection()

        val database = AppDatabase.getInstance(applicationContext)
        repository = PdfRepository(database)

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
                val scope = rememberCoroutineScope()

                // Check for incoming PDF intent
                LaunchedEffect(intent) {
                    repository.ensureInitialSampleDocs(applicationContext)
                    handleIncomingIntent(intent) { project ->
                        currentScreen = Screen.Editor(project)
                    }
                }

                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_transition",
                    modifier = Modifier.fillMaxSize()
                ) { screen ->
                    when (screen) {
                        is Screen.Splash -> {
                            SplashScreen(
                                onContinue = {
                                    currentScreen = Screen.Home
                                }
                            )
                        }

                        is Screen.Home -> {
                            HomeScreen(
                                repository = repository,
                                billingManager = billingManager,
                                onOpenProject = { project ->
                                    currentScreen = Screen.Editor(project)
                                }
                            )
                        }

                        is Screen.Editor -> {
                            PdfEditorScreen(
                                project = screen.project,
                                repository = repository,
                                billingManager = billingManager,
                                onBack = {
                                    currentScreen = Screen.Home
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleIncomingIntent(intent: Intent?, onPdfReady: (PdfProjectEntity) -> Unit) {
        if (intent == null) return
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> null
        }
        if (uri != null) {
            val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
            scope.launch {
                val proj = repository.createProjectFromUploadedPdf(applicationContext, uri, "Shared_Document.pdf")
                if (proj != null) {
                    onPdfReady(proj)
                }
            }
        }
    }
}
