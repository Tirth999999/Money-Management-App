package com.mymoney.manager.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mymoney.manager.data.local.entity.AccountEntity
import com.mymoney.manager.data.local.entity.CategoryEntity
import com.mymoney.manager.data.model.CurrencyUtils
import com.mymoney.manager.data.model.TransactionType
import com.mymoney.manager.ui.theme.ExpenseRed
import com.mymoney.manager.ui.theme.IncomeGreen
import com.mymoney.manager.ui.theme.Primary
import com.mymoney.manager.ui.theme.TransferBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (
        type: TransactionType,
        amountPaise: Long,
        accountId: Long,
        destinationAccountId: Long?,
        categoryId: Long?,
        description: String,
        personName: String?,
        notes: String?
    ) -> Unit
) {
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var personNameText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    var selectedAccountId by remember {
        mutableStateOf(accounts.firstOrNull { it.name.contains("HDFC") }?.id ?: accounts.firstOrNull()?.id ?: 1L)
    }
    var destinationAccountId by remember {
        mutableStateOf(accounts.firstOrNull { it.name.contains("Cash") }?.id ?: accounts.getOrNull(1)?.id ?: 2L)
    }
    var selectedCategoryId by remember {
        mutableStateOf(categories.firstOrNull { it.type == TransactionType.EXPENSE }?.id)
    }

    var accountExpanded by remember { mutableStateOf(false) }
    var destAccountExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Add Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Type Tabs
                val types = listOf(
                    TransactionType.EXPENSE,
                    TransactionType.INCOME,
                    TransactionType.TRANSFER,
                    TransactionType.LEND,
                    TransactionType.BORROW
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    types.take(3).forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                if (type == TransactionType.INCOME) {
                                    selectedCategoryId = categories.firstOrNull { it.type == TransactionType.INCOME }?.id
                                } else if (type == TransactionType.EXPENSE) {
                                    selectedCategoryId = categories.firstOrNull { it.type == TransactionType.EXPENSE }?.id
                                }
                            },
                            label = { Text(type.displayName, fontSize = 12.sp, maxLines = 1, softWrap = false) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    types.drop(3).forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.displayName, fontSize = 12.sp, maxLines = 1, softWrap = false) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Account Selector
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = it }
                ) {
                    val currentAccountName = accounts.find { it.id == selectedAccountId }?.name ?: "Select Account"
                    OutlinedTextField(
                        value = currentAccountName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (selectedType == TransactionType.TRANSFER) "From Account" else "Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text("${account.name} (${CurrencyUtils.formatPaise(account.currentBalancePaise)})") },
                                onClick = {
                                    selectedAccountId = account.id
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }

                // If Transfer, show destination account
                if (selectedType == TransactionType.TRANSFER) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ExposedDropdownMenuBox(
                        expanded = destAccountExpanded,
                        onExpandedChange = { destAccountExpanded = it }
                    ) {
                        val currentDestName = accounts.find { it.id == destinationAccountId }?.name ?: "Select Destination"
                        OutlinedTextField(
                            value = currentDestName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("To Account") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = destAccountExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = destAccountExpanded,
                            onDismissRequest = { destAccountExpanded = false }
                        ) {
                            accounts.filter { it.id != selectedAccountId }.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text("${account.name} (${CurrencyUtils.formatPaise(account.currentBalancePaise)})") },
                                    onClick = {
                                        destinationAccountId = account.id
                                        destAccountExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // If Lending / Borrowing, show Person Name
                if (selectedType == TransactionType.LEND || selectedType == TransactionType.BORROW) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = personNameText,
                        onValueChange = { personNameText = it },
                        label = { Text(if (selectedType == TransactionType.LEND) "Lent to (Person Name)" else "Borrowed from (Person Name)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Category Selector (for Income & Expense)
                if (selectedType == TransactionType.INCOME || selectedType == TransactionType.EXPENSE) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val relevantCats = categories.filter { it.type == selectedType }
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it }
                    ) {
                        val currentCatName = relevantCats.find { it.id == selectedCategoryId }?.name ?: "Select Category"
                        OutlinedTextField(
                            value = currentCatName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            relevantCats.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        selectedCategoryId = category.id
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Description
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text("Description (e.g. Lunch, Client Payment)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Notes
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
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
                                onSave(
                                    selectedType,
                                    paise,
                                    selectedAccountId,
                                    if (selectedType == TransactionType.TRANSFER) destinationAccountId else null,
                                    selectedCategoryId,
                                    descriptionText.trim(),
                                    personNameText.trim().ifEmpty { null },
                                    notesText.trim().ifEmpty { null }
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (selectedType) {
                                TransactionType.INCOME -> IncomeGreen
                                TransactionType.EXPENSE -> ExpenseRed
                                TransactionType.TRANSFER -> TransferBlue
                                else -> Primary
                            }
                        )
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
