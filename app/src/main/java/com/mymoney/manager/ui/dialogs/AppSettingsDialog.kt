package com.mymoney.manager.ui.dialogs

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mymoney.manager.ui.MainViewModel
import com.mymoney.manager.ui.theme.IncomeGreen
import com.mymoney.manager.ui.theme.Primary
import com.mymoney.manager.voice.JarvisVoiceManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsDialog(
    viewModel: MainViewModel,
    voiceManager: JarvisVoiceManager?,
    onDismiss: () -> Unit
) {
    var selectedPreset by remember { mutableIntStateOf(voiceManager?.getSavedPreset() ?: 0) }
    var pitch by remember { mutableFloatStateOf(voiceManager?.getSavedPitch() ?: 0.78f) }
    var speed by remember { mutableFloatStateOf(voiceManager?.getSavedSpeed() ?: 0.95f) }
    var selectedCustomVoiceName by remember { mutableStateOf(voiceManager?.getSavedCustomVoiceName()) }

    var presetDropdownExpanded by remember { mutableStateOf(false) }
    var voiceListDropdownExpanded by remember { mutableStateOf(false) }

    val availableVoices = remember { voiceManager?.getAvailableVoices() ?: emptyList() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Settings & Voice",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // SECTION 1: JARVIS VOICE ASSISTANT
                Text(
                    text = "JARVIS VOICE & TONE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Voice Persona & Tone",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        ExposedDropdownMenuBox(
                            expanded = presetDropdownExpanded,
                            onExpandedChange = { presetDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = JarvisVoiceManager.PRESET_NAMES.getOrElse(selectedPreset) { "Jarvis Deep Tech (Male)" },
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = presetDropdownExpanded,
                                onDismissRequest = { presetDropdownExpanded = false }
                            ) {
                                JarvisVoiceManager.PRESET_NAMES.forEachIndexed { index, name ->
                                    DropdownMenuItem(
                                        text = { Text(name, fontWeight = if (index == selectedPreset) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            selectedPreset = index
                                            presetDropdownExpanded = false
                                            when (index) {
                                                0 -> { pitch = 0.75f; speed = 0.95f; selectedCustomVoiceName = null }
                                                1 -> { pitch = 0.90f; speed = 1.0f; selectedCustomVoiceName = null }
                                                2 -> { pitch = 1.15f; speed = 1.05f; selectedCustomVoiceName = null }
                                                3 -> { pitch = 0.85f; speed = 0.95f; selectedCustomVoiceName = null }
                                                4 -> { pitch = 1.0f; speed = 1.0f; selectedCustomVoiceName = null }
                                                else -> { pitch = 1.0f; speed = 1.0f; selectedCustomVoiceName = null }
                                            }
                                            voiceManager?.applyVoiceSettings(selectedPreset, pitch, speed, selectedCustomVoiceName)
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Pitch Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Voice Pitch", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Text(String.format("%.2fx", pitch), style = MaterialTheme.typography.labelSmall, color = Primary, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = pitch,
                            onValueChange = {
                                pitch = it
                                voiceManager?.applyVoiceSettings(selectedPreset, pitch, speed, selectedCustomVoiceName)
                            },
                            valueRange = 0.6f..1.6f,
                            colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary)
                        )

                        // Speed Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Speech Rate (Speed)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Text(String.format("%.2fx", speed), style = MaterialTheme.typography.labelSmall, color = Primary, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = speed,
                            onValueChange = {
                                speed = it
                                voiceManager?.applyVoiceSettings(selectedPreset, pitch, speed, selectedCustomVoiceName)
                            },
                            valueRange = 0.7f..1.5f,
                            colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary)
                        )

                        // Available voices on phone
                        if (availableVoices.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Device Voice Engine", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            ExposedDropdownMenuBox(
                                expanded = voiceListDropdownExpanded,
                                onExpandedChange = { voiceListDropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedCustomVoiceName ?: "Preset Default Engine",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = voiceListDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = voiceListDropdownExpanded,
                                    onDismissRequest = { voiceListDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Use Preset Selected Voice") },
                                        onClick = {
                                            selectedCustomVoiceName = null
                                            voiceListDropdownExpanded = false
                                            voiceManager?.applyVoiceSettings(selectedPreset, pitch, speed, null)
                                        }
                                    )
                                    availableVoices.forEach { v ->
                                        DropdownMenuItem(
                                            text = { Text(v.name, fontSize = 12.sp) },
                                            onClick = {
                                                selectedCustomVoiceName = v.name
                                                voiceListDropdownExpanded = false
                                                voiceManager?.applyVoiceSettings(selectedPreset, pitch, speed, v.name)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Test Voice Button
                        OutlinedButton(
                            onClick = {
                                voiceManager?.applyVoiceSettings(selectedPreset, pitch, speed, selectedCustomVoiceName)
                                voiceManager?.testVoice("Hello! I am Jarvis, your personal money manager.")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Test Voice", tint = Primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Voice Audio", color = Primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 2: NOTIFICATIONS & DAILY REMINDERS
                Text(
                    text = "NOTIFICATIONS & REMINDERS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Daily Review Reminder", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Scheduled daily at 9:00 PM for pending drafts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(IncomeGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Active (9 PM)", style = MaterialTheme.typography.labelSmall, color = IncomeGreen, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { viewModel.triggerTestNotification() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = "Test Notification", tint = Primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send Test Notification Now", color = Primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 3: DATA & PRIVACY
                Text(
                    text = "DATA HEALTH & OFFLINE PRIVACY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = "Privacy", tint = IncomeGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("100% Offline SQLite Room Database", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Zero cloud tracking, zero external servers. All bank SMS and transaction entries remain strictly on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { viewModel.cleanupDuplicates() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = "Clean Duplicates", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verify & Clean Duplicate Categories", fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
