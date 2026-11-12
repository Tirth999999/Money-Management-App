package com.mymoney.manager.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mymoney.manager.data.local.entity.AccountEntity
import com.mymoney.manager.data.model.TransactionType
import com.mymoney.manager.parser.VoiceCommandResult
import com.mymoney.manager.ui.theme.ExpenseRed
import com.mymoney.manager.ui.theme.IncomeGreen
import com.mymoney.manager.ui.theme.Primary
import com.mymoney.manager.voice.JarvisVoiceManager

@Composable
fun VoiceAssistantDialog(
    isListening: Boolean,
    recognizedText: String,
    jarvisPrompt: String,
    needsClarification: VoiceCommandResult.NeedsClarification?,
    successMessage: String?,
    onDismiss: () -> Unit,
    onMicClick: () -> Unit,
    onClarifyMode: (isOnline: Boolean) -> Unit,
    onClarifyType: (TransactionType) -> Unit,
    voiceManager: JarvisVoiceManager? = null,
    accounts: List<AccountEntity> = emptyList(),
    onSelectAccount: ((AccountEntity) -> Unit)? = null
) {
    var showVoiceSettings by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Voice Settings & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Jarvis AI Assistant",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (voiceManager != null) {
                            IconButton(
                                onClick = { showVoiceSettings = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Voice Settings",
                                    tint = Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Microphone Circle with pulsing appearance
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                successMessage != null -> IncomeGreen
                                isListening -> Primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onMicClick) {
                        Icon(
                            imageVector = when {
                                successMessage != null -> Icons.Default.CheckCircle
                                isListening -> Icons.Default.Mic
                                else -> Icons.Default.MicOff
                            },
                            contentDescription = "Voice",
                            tint = if (isListening || successMessage != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Spoken Prompt / Assistant Message
                Text(
                    text = jarvisPrompt,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // User speech transcription
                if (recognizedText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"$recognizedText\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Clarification Prompts with compact responsive buttons
                AnimatedVisibility(visible = needsClarification != null) {
                    needsClarification?.let { clarification ->
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Tap or speak your choice:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                when (clarification.missingField) {
                                    VoiceCommandResult.NeedsClarification.MissingField.ACCOUNT_MODE -> {
                                        if (accounts.isNotEmpty()) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                accounts.chunked(2).forEach { rowAccounts ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        rowAccounts.forEach { acc ->
                                                            Button(
                                                                onClick = {
                                                                    if (onSelectAccount != null) {
                                                                        onSelectAccount(acc)
                                                                    } else {
                                                                        onClarifyMode(!acc.name.contains("cash", ignoreCase = true))
                                                                    }
                                                                },
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color(acc.colorHex)),
                                                                shape = RoundedCornerShape(8.dp),
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                Text(
                                                                    text = acc.name,
                                                                    fontSize = 12.sp,
                                                                    maxLines = 1,
                                                                    softWrap = false,
                                                                    color = Color.White
                                                                )
                                                            }
                                                        }
                                                        if (rowAccounts.size == 1) {
                                                            Spacer(modifier = Modifier.weight(1f))
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = { onClarifyMode(true) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(text = "Online (HDFC)", fontSize = 12.sp, maxLines = 1, color = Color.White)
                                                }
                                                Button(
                                                    onClick = { onClarifyMode(false) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(text = "Cash (Offline)", fontSize = 12.sp, maxLines = 1, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                    VoiceCommandResult.NeedsClarification.MissingField.TRANSACTION_TYPE -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { onClarifyType(TransactionType.INCOME) },
                                                colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = "Income",
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    color = Color.White
                                                )
                                            }
                                            Button(
                                                onClick = { onClarifyType(TransactionType.EXPENSE) },
                                                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = "Expense",
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                    VoiceCommandResult.NeedsClarification.MissingField.AMOUNT -> {
                                        Text(
                                            text = "Speak amount (e.g. '100 rupees' or 'ek hajar')",
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Done or Close Button
                if (successMessage != null) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showVoiceSettings) {
        VoiceSettingsDialog(
            voiceManager = voiceManager,
            onDismiss = { showVoiceSettings = false }
        )
    }
}
