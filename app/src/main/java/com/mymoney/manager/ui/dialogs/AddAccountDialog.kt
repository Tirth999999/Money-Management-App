package com.mymoney.manager.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mymoney.manager.data.model.AccountType
import com.mymoney.manager.data.model.CurrencyUtils

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, openingBalancePaise: Long, type: AccountType, icon: String, colorHex: Long) -> Unit
) {
    var nameText by remember { mutableStateOf("") }
    var openingBalanceText by remember { mutableStateOf("0") }
    var selectedType by remember { mutableStateOf(AccountType.CUSTOM_FUND) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Add Account / Fund",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Account Name (e.g. Savings, Emergency Fund)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = openingBalanceText,
                    onValueChange = { openingBalanceText = it },
                    label = { Text("Opening Balance (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Account Type", style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == AccountType.PRIMARY,
                        onClick = { selectedType = AccountType.PRIMARY },
                        label = { Text("Primary Bank") }
                    )
                    FilterChip(
                        selected = selectedType == AccountType.CUSTOM_FUND,
                        onClick = { selectedType = AccountType.CUSTOM_FUND },
                        label = { Text("Custom Fund / Savings") }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (nameText.isNotBlank()) {
                                val paise = CurrencyUtils.parseRupeesToPaise(openingBalanceText)
                                val icon = if (selectedType == AccountType.CUSTOM_FUND) "savings" else "account_balance"
                                val color = if (selectedType == AccountType.CUSTOM_FUND) 0xFF0288D1 else 0xFF006C50
                                onSave(nameText.trim(), paise, selectedType, icon, color)
                                onDismiss()
                            }
                        }
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}
