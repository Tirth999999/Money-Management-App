package com.mymoney.manager.ui.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.mymoney.manager.data.local.database.AppDatabase
import com.mymoney.manager.data.local.entity.AccountEntity
import com.mymoney.manager.data.local.entity.TransactionDraftEntity
import com.mymoney.manager.data.model.CurrencyUtils
import com.mymoney.manager.data.model.DetectionSource
import com.mymoney.manager.data.model.DraftStatus
import com.mymoney.manager.data.model.TransactionType
import com.mymoney.manager.data.repository.FinanceRepository
import com.mymoney.manager.parser.VoiceCommandParser
import com.mymoney.manager.parser.VoiceCommandResult
import com.mymoney.manager.ui.dialogs.VoiceAssistantDialog
import com.mymoney.manager.ui.theme.MyMoneyTheme
import com.mymoney.manager.voice.JarvisVoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class QuickVoiceActivity : ComponentActivity() {

    private lateinit var voiceManager: JarvisVoiceManager
    private lateinit var repository: FinanceRepository

    private val isListening = MutableStateFlow(false)
    private val recognizedText = MutableStateFlow("")
    private val jarvisPrompt = MutableStateFlow("Listening... Speak in English, Hindi or Gujarati")
    private val needsClarification = MutableStateFlow<VoiceCommandResult.NeedsClarification?>(null)
    private val successMessage = MutableStateFlow<String?>(null)
    private val accountsState = MutableStateFlow<List<AccountEntity>>(emptyList())

    private val requestAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening()
        } else {
            jarvisPrompt.value = "Microphone permission required."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(this)
        repository = FinanceRepository(db)

        lifecycleScope.launch {
            accountsState.value = repository.getActiveAccountsList()
            repository.getAllAccounts().collect { accList ->
                accountsState.value = accList
            }
        }

        voiceManager = JarvisVoiceManager(
            context = this,
            onSpeechResult = { text -> handleSpeechInput(text) },
            onSpeechPartial = { partial -> recognizedText.value = partial },
            onError = { error ->
                isListening.value = false
                jarvisPrompt.value = error
            },
            onListeningStateChanged = { listening -> isListening.value = listening }
        )

        setContent {
            MyMoneyTheme {
                val listeningState by isListening.collectAsState()
                val textState by recognizedText.collectAsState()
                val promptState by jarvisPrompt.collectAsState()
                val clarificationState by needsClarification.collectAsState()
                val successState by successMessage.collectAsState()
                val accounts by accountsState.collectAsState()

                Box(modifier = Modifier.fillMaxSize()) {
                    VoiceAssistantDialog(
                        isListening = listeningState,
                        recognizedText = textState,
                        jarvisPrompt = promptState,
                        needsClarification = clarificationState,
                        successMessage = successState,
                        onDismiss = { finish() },
                        onMicClick = { startListening() },
                        onClarifyMode = { isOnline -> handleSpeechInput(if (isOnline) "online" else "cash") },
                        onClarifyType = { type -> handleSpeechInput(if (type == TransactionType.INCOME) "income" else "expense") },
                        voiceManager = voiceManager,
                        accounts = accounts,
                        onSelectAccount = { acc -> handleSpeechInput(acc.name) }
                    )
                }
            }
        }

        checkPermissionAndListen()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        needsClarification.value = null
        successMessage.value = null
        recognizedText.value = ""
        checkPermissionAndListen()
    }

    private fun checkPermissionAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startListening() {
        successMessage.value = null
        recognizedText.value = ""
        jarvisPrompt.value = "Listening... Speak in English, Hindi or Gujarati"
        voiceManager.startListening()
    }

    private fun handleSpeechInput(text: String) {
        recognizedText.value = text
        val partial = needsClarification.value

        val result = VoiceCommandParser.parse(
            input = text,
            partialAmountPaise = partial?.partialAmountPaise,
            partialIsOnline = partial?.partialIsOnline,
            partialType = partial?.partialType,
            availableAccounts = accountsState.value,
            partialAccountId = partial?.partialAccountId
        )

        when (result) {
            is VoiceCommandResult.DraftReady -> {
                needsClarification.value = null
                saveVoiceDraftAndFinish(result)
            }
            is VoiceCommandResult.NeedsClarification -> {
                needsClarification.value = result
                jarvisPrompt.value = result.spokenPrompt
                // Hands-free continuous voice: automatically re-start listening after asking the question!
                voiceManager.speak(result.spokenPrompt) {
                    runOnUiThread {
                        startListening()
                    }
                }
            }
            else -> {
                val msg = "Please state the amount, account name or cash, and income or expense."
                jarvisPrompt.value = msg
                voiceManager.speak(msg) {
                    runOnUiThread {
                        startListening()
                    }
                }
            }
        }
    }

    private fun saveVoiceDraftAndFinish(result: VoiceCommandResult.DraftReady) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@QuickVoiceActivity)
            val currentAccounts = if (accountsState.value.isNotEmpty()) {
                accountsState.value
            } else {
                repository.getActiveAccountsList()
            }
            val account = if (result.targetAccountId != null) {
                currentAccounts.find { it.id == result.targetAccountId }
                    ?: repository.getOrCreateDefaultAccount(result.isOnline)
            } else {
                repository.getOrCreateDefaultAccount(result.isOnline)
            }
            var catId: Long? = null
            if (result.suggestedCategory != null) {
                catId = db.categoryDao().findCategoryByName(result.suggestedCategory)?.id
            }

            val draft = TransactionDraftEntity(
                type = result.type,
                amountPaise = result.amountPaise,
                accountId = account.id,
                categoryId = catId,
                description = result.description,
                transactionDate = System.currentTimeMillis(),
                source = DetectionSource.VOICE,
                confidence = 1.0f,
                status = DraftStatus.PENDING
            )
            repository.createDraft(draft)

            val amountStr = CurrencyUtils.formatPaise(result.amountPaise)
            val typeStr = if (result.type == TransactionType.INCOME) "income" else "expense"
            val accountName = account.name
            val confirm = "Created $amountStr $typeStr draft for $accountName."

            successMessage.value = confirm
            jarvisPrompt.value = confirm
            voiceManager.speak(confirm) {
                // Auto finish activity after speech confirmation
                window.decorView.postDelayed({ finish() }, 1800)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.destroy()
    }
}
