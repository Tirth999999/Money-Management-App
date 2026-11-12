package com.mymoney.manager.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mymoney.manager.ui.theme.Primary
import com.mymoney.manager.ui.theme.PrimaryDark
import com.mymoney.manager.voice.JarvisVoiceManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsDialog(
    voiceManager: JarvisVoiceManager?,
    onDismiss: () -> Unit
) {
    if (voiceManager == null) {
        onDismiss()
        return
    }

    var selectedPreset by remember { mutableIntStateOf(voiceManager.getSavedPreset()) }
    var pitch by remember { mutableFloatStateOf(voiceManager.getSavedPitch()) }
    var speed by remember { mutableFloatStateOf(voiceManager.getSavedSpeed()) }
    var selectedCustomVoiceName by remember { mutableStateOf(voiceManager.getSavedCustomVoiceName()) }

    var presetDropdownExpanded by remember { mutableStateOf(false) }
    var voiceListDropdownExpanded by remember { mutableStateOf(false) }

    val availableVoices = remember { voiceManager.getAvailableVoices() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
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
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Voice Settings",
                            tint = Primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Jarvis Voice Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Voice Presets
                Text(
                    text = "Voice Persona & Tone",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    // Set optimal defaults for preset
                                    when (index) {
                                        0 -> { pitch = 0.75f; speed = 0.95f; selectedCustomVoiceName = null }
                                        1 -> { pitch = 0.90f; speed = 1.0f; selectedCustomVoiceName = null }
                                        2 -> { pitch = 1.15f; speed = 1.05f; selectedCustomVoiceName = null }
                                        3 -> { pitch = 0.85f; speed = 0.95f; selectedCustomVoiceName = null }
                                        4 -> { pitch = 1.0f; speed = 1.0f; selectedCustomVoiceName = null }
                                        else -> { pitch = 1.0f; speed = 1.0f; selectedCustomVoiceName = null }
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Pitch Slider (0.6x to 1.6x)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pitch (Tone Depth)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = String.format("%.2fx", pitch),
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = pitch,
                    onValueChange = { pitch = it },
                    valueRange = 0.6f..1.6f,
                    colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary)
                )

                // Speed Slider (0.7x to 1.5x)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Speech Rate (Speed)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = String.format("%.2fx", speed),
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = speed,
                    onValueChange = { speed = it },
                    valueRange = 0.7f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary)
                )

                // Optional: Device System Voices Picker
                if (availableVoices.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Device Installed Voice",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    ExposedDropdownMenuBox(
                        expanded = voiceListDropdownExpanded,
                        onExpandedChange = { voiceListDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCustomVoiceName ?: "Default Engine Voice",
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
                                }
                            )
                            availableVoices.forEach { voice ->
                                DropdownMenuItem(
                                    text = { Text(voice.name, fontSize = 13.sp) },
                                    onClick = {
                                        selectedCustomVoiceName = voice.name
                                        voiceListDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Test Voice Preview Button
                OutlinedButton(
                    onClick = {
                        voiceManager.applyVoiceSettings(selectedPreset, pitch, speed, selectedCustomVoiceName)
                        voiceManager.testVoice("Hello! I am Jarvis, your personal money manager.")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play Test", tint = PrimaryDark, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Jarvis Voice", color = PrimaryDark, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Save & Apply Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            voiceManager.applyVoiceSettings(selectedPreset, pitch, speed, selectedCustomVoiceName)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("Save Settings", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
