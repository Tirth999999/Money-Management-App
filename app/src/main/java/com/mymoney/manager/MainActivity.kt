package com.mymoney.manager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.mymoney.manager.reminder.DailyReminderManager
import com.mymoney.manager.ui.MainViewModel
import com.mymoney.manager.ui.accounts.AccountsScreen
import com.mymoney.manager.ui.analysis.AnalysisScreen
import com.mymoney.manager.ui.categories.CategoriesScreen
import com.mymoney.manager.ui.dialogs.VoiceAssistantDialog
import com.mymoney.manager.ui.drafts.DraftsScreen
import com.mymoney.manager.ui.navigation.BottomNavItems
import com.mymoney.manager.ui.navigation.Screen
import com.mymoney.manager.ui.records.RecordsScreen
import com.mymoney.manager.ui.theme.ExpenseRed
import com.mymoney.manager.ui.theme.MyMoneyTheme
import com.mymoney.manager.ui.theme.Primary
import com.mymoney.manager.voice.JarvisVoiceManager

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var voiceManager: JarvisVoiceManager? = null

    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled gracefully
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Schedule daily 9 PM reminder
        DailyReminderManager.scheduleDailyReminder(this, 21, 0)

        // Request runtime permissions on modern Android (Vivo Y33T Android 14+)
        checkAndRequestPermissions()

        // Initialize voice manager for in-app assistant
        voiceManager = JarvisVoiceManager(
            context = this,
            onSpeechResult = { text -> viewModel.handleVoiceResult(text) },
            onSpeechPartial = {},
            onError = { error -> }
        )
        viewModel.voiceManager = voiceManager

        setContent {
            MyMoneyTheme {
                MainAppContent(
                    viewModel = viewModel,
                    initialTab = intent.getStringExtra("EXTRA_NAV_TAB")
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECORD_AUDIO
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val neededPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (neededPermissions.isNotEmpty()) {
            requestMultiplePermissions.launch(neededPermissions.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager?.destroy()
    }
}

@Composable
fun MainAppContent(
    viewModel: MainViewModel,
    initialTab: String?
) {
    var selectedScreen by remember {
        mutableStateOf<Screen>(
            if (initialTab == "DRAFTS") Screen.Drafts else Screen.Records
        )
    }

    val pendingCount by viewModel.pendingDraftsCount.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val isVoiceVisible by viewModel.voiceDialogVisible.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val recognizedText by viewModel.voiceRecognizedText.collectAsState()
    val jarvisPrompt by viewModel.jarvisSpokenPrompt.collectAsState()
    val needsClarification by viewModel.needsClarification.collectAsState()
    val successMessage by viewModel.voiceSuccessMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(initialTab) {
        if (initialTab == "DRAFTS") {
            selectedScreen = Screen.Drafts
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Primary
            ) {
                BottomNavItems.forEach { screen ->
                    val isSelected = selectedScreen.route == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedScreen = screen },
                        icon = {
                            if (screen == Screen.Drafts && pendingCount > 0) {
                                BadgedBox(badge = {
                                    Badge(containerColor = ExpenseRed) {
                                        Text("$pendingCount")
                                    }
                                }) {
                                    Icon(screen.icon, contentDescription = screen.title)
                                }
                            } else {
                                Icon(screen.icon, contentDescription = screen.title)
                            }
                        },
                        label = { Text(screen.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            indicatorColor = Primary.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedScreen) {
                Screen.Records -> RecordsScreen(
                    viewModel = viewModel,
                    onOpenVoice = { viewModel.openVoiceAssistant() }
                )
                Screen.Analysis -> AnalysisScreen(viewModel = viewModel)
                Screen.Accounts -> AccountsScreen(viewModel = viewModel)
                Screen.Categories -> CategoriesScreen(viewModel = viewModel)
                Screen.Drafts -> DraftsScreen(viewModel = viewModel)
            }

            // In-App Jarvis Voice Assistant Dialog
            if (isVoiceVisible) {
                VoiceAssistantDialog(
                    isListening = isListening,
                    recognizedText = recognizedText,
                    jarvisPrompt = jarvisPrompt,
                    needsClarification = needsClarification,
                    successMessage = successMessage,
                    onDismiss = { viewModel.closeVoiceAssistant() },
                    onMicClick = { viewModel.openVoiceAssistant() },
                    onClarifyMode = { isOnline -> viewModel.clarifyAccountMode(isOnline) },
                    onClarifyType = { type -> viewModel.clarifyType(type) },
                    voiceManager = viewModel.voiceManager,
                    accounts = accounts,
                    onSelectAccount = { acc -> viewModel.handleVoiceResult(acc.name) }
                )
            }
        }
    }
}
