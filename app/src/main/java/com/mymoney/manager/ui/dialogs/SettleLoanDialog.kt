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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.mymoney.manager.data.local.entity.AccountEntity
import com.mymoney.manager.data.local.entity.LoanEntity
import com.mymoney.manager.data.model.CurrencyUtils
import com.mymoney.manager.data.model.LoanType
import com.mymoney.manager.ui.theme.IncomeGreen
import com.mymoney.manager.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleLoanDialog(
    loan: LoanEntity,
    personName: String,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSettle: (amountPaise: Long, accountId: Long, notes: String?) -> Unit
) {
    var amountText by remember {
        mutableStateOf((loan.outstandingAmountPaise / 100.0).toString())
    }
    var selectedAccountId by remember {
        mutableStateOf(accounts.firstOrNull { it.id == loan.accountId }?.id ?: accounts.firstOrNull()?.id ?: 1L)
    }
    var notesText by remember { mutableStateOf("") }
    var accountExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (loan.type == LoanType.LENT) "Repayment from $personName" else "Repay to $personName",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Outstanding: ${CurrencyUtils.formatPaise(loan.outstandingAmountPaise)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Repayment Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Account
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = it }
                ) {
                    val currentName = accounts.find { it.id == selectedAccountId }?.name ?: "Select Account"
                    OutlinedTextField(
                        value = currentName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (loan.type == LoanType.LENT) "Receive Into Account" else "Pay From Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (${CurrencyUtils.formatPaise(acc.currentBalancePaise)})") },
                                onClick = {
                                    selectedAccountId = acc.id
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

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
                            val paise = CurrencyUtils.parseRupeesToPaise(amountText)
                            if (paise > 0) {
                                onSettle(paise, selectedAccountId, notesText.trim().ifEmpty { null })
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (loan.type == LoanType.LENT) IncomeGreen else Primary)
                    ) {
                        Text("Record Repayment")
                    }
                }
            }
        }
    }
}
