package com.mymoney.manager.ui.records

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mymoney.manager.data.local.entity.AccountEntity
import com.mymoney.manager.data.local.entity.CategoryEntity
import com.mymoney.manager.data.local.entity.TransactionEntity
import com.mymoney.manager.data.model.CurrencyUtils
import com.mymoney.manager.data.model.TransactionType
import com.mymoney.manager.ui.MainViewModel
import com.mymoney.manager.ui.TimePeriod
import com.mymoney.manager.ui.components.VectorIconHelper
import com.mymoney.manager.ui.dialogs.AddTransactionDialog
import com.mymoney.manager.ui.dialogs.AppSettingsDialog
import com.mymoney.manager.ui.theme.BorderLight
import com.mymoney.manager.ui.theme.ExpenseContainer
import com.mymoney.manager.ui.theme.ExpenseRed
import com.mymoney.manager.ui.theme.IncomeContainer
import com.mymoney.manager.ui.theme.IncomeGreen
import com.mymoney.manager.ui.theme.Primary
import com.mymoney.manager.ui.theme.TransferBlue
import com.mymoney.manager.ui.theme.TransferContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    viewModel: MainViewModel,
    onOpenVoice: () -> Unit
) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val incomeTotal by viewModel.periodIncomePaise.collectAsState()
    val expenseTotal by viewModel.periodExpensePaise.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsState()
    val selectedAccountFilter by viewModel.selectedAccountFilter.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var showSearchBox by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // App Header Bar with Jarvis Branding, Search, Mic & Settings
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "My Money Manager",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Jarvis Personal Finance Assistant",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showSearchBox = !showSearchBox },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (showSearchBox) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onOpenVoice,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Jarvis Voice",
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Period Tabs: Day, Month, Year, All
            ScrollableTabRow(
                selectedTabIndex = selectedPeriod.ordinal,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedPeriod.ordinal]),
                        color = Primary
                    )
                }
            ) {
                TimePeriod.values().forEach { period ->
                    Tab(
                        selected = selectedPeriod == period,
                        onClick = { viewModel.setPeriod(period) },
                        text = {
                            Text(
                                text = period.displayName,
                                fontWeight = if (selectedPeriod == period) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Summary Card (Income, Expense, Net)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Income
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = CurrencyUtils.formatPaise(incomeTotal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Expenses
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = CurrencyUtils.formatPaise(expenseTotal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Net Total
                    val net = incomeTotal - expenseTotal
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Net Amount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = CurrencyUtils.formatPaise(net),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (net >= 0) IncomeGreen else ExpenseRed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Search Bar & Filter Chips
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (showSearchBox) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search transactions, merchants, notes...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            IconButton(onClick = {
                                viewModel.setSearchQuery("")
                                showSearchBox = false
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close Search")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        singleLine = true
                    )
                }

                // Filter row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    if (!showSearchBox) {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = { showSearchBox = true },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(16.dp)) },
                                label = { Text("Search") }
                            )
                        }
                    }

                    // Type filters
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == null,
                            onClick = { viewModel.setTypeFilter(null) },
                            label = { Text("All Types") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == TransactionType.EXPENSE,
                            onClick = { viewModel.setTypeFilter(if (selectedTypeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE) },
                            label = { Text("Expenses") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == TransactionType.INCOME,
                            onClick = { viewModel.setTypeFilter(if (selectedTypeFilter == TransactionType.INCOME) null else TransactionType.INCOME) },
                            label = { Text("Income") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == TransactionType.TRANSFER,
                            onClick = { viewModel.setTypeFilter(if (selectedTypeFilter == TransactionType.TRANSFER) null else TransactionType.TRANSFER) },
                            label = { Text("Transfers") }
                        )
                    }

                    // Account filter chips
                    accounts.forEach { acc ->
                        item {
                            FilterChip(
                                selected = selectedAccountFilter == acc.id,
                                onClick = { viewModel.setAccountFilter(if (selectedAccountFilter == acc.id) null else acc.id) },
                                label = { Text(acc.name) }
                            )
                        }
                    }
                }
            }

            // Group transactions by date
            val groupedTransactions = remember(transactions) {
                val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
                transactions.groupBy { dateFormat.format(Date(it.transactionDate)) }
            }

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No transactions found in this period",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // List of grouped transactions
                // Generous bottom padding (120dp) ensures floating action buttons NEVER overlap entries or delete actions
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    groupedTransactions.forEach { (dateStr, txs) ->
                        // Date Header
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val dayIncome = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amountPaise }
                                val dayExpense = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPaise }
                                val dayNet = dayIncome - dayExpense
                                Text(
                                    text = CurrencyUtils.formatPaise(dayNet),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (dayNet >= 0) IncomeGreen else ExpenseRed
                                )
                            }
                        }

                        // Transaction Cards
                        items(txs, key = { it.id }) { tx ->
                            val account = accounts.find { it.id == tx.accountId }
                            val destAccount = if (tx.destinationAccountId != null) accounts.find { it.id == tx.destinationAccountId } else null
                            val category = categories.find { it.id == tx.categoryId }

                            TransactionItem(
                                transaction = tx,
                                account = account,
                                destAccount = destAccount,
                                category = category,
                                onDelete = { transactionToDelete = tx }
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Buttons (Add + and Voice Mic)
        // Arranged horizontally side-by-side on the bottom right so they are unobtrusive and NEVER overlap each other
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mic Voice Button
            FloatingActionButton(
                onClick = onOpenVoice,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Primary,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Voice Assistant", modifier = Modifier.size(24.dp))
            }

            // Add Transaction Button
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(28.dp))
            }
        }
    }

    // Add Transaction Dialog
    if (showAddDialog) {
        AddTransactionDialog(
            accounts = accounts,
            categories = categories,
            onDismiss = { showAddDialog = false },
            onSave = { type, amount, accountId, destAccountId, categoryId, desc, personName, notes ->
                viewModel.addManualTransaction(type, amount, accountId, destAccountId, categoryId, desc, personName, notes)
            }
        )
    }

    // Delete Confirmation Dialog
    transactionToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction of ${CurrencyUtils.formatPaise(tx.amountPaise)}? This will safely reverse the account balance.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        transactionToDelete = null
                    }
                ) {
                    Text("Delete", color = ExpenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSettingsDialog) {
        AppSettingsDialog(
            viewModel = viewModel,
            voiceManager = viewModel.voiceManager,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    account: AccountEntity?,
    destAccount: AccountEntity?,
    category: CategoryEntity?,
    onDelete: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeStr = timeFormat.format(Date(transaction.transactionDate))

    val iconVector = when (transaction.type) {
        TransactionType.INCOME -> VectorIconHelper.getIconByName(category?.iconName ?: "trending_up")
        TransactionType.EXPENSE -> VectorIconHelper.getIconByName(category?.iconName ?: "trending_down")
        TransactionType.TRANSFER -> Icons.Default.SyncAlt
        else -> VectorIconHelper.getIconByName("account_balance_wallet")
    }

    val iconBgColor = when (transaction.type) {
        TransactionType.INCOME -> IncomeContainer
        TransactionType.EXPENSE -> ExpenseContainer
        TransactionType.TRANSFER -> TransferContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val iconTintColor = when (transaction.type) {
        TransactionType.INCOME -> IncomeGreen
        TransactionType.EXPENSE -> ExpenseRed
        TransactionType.TRANSFER -> TransferBlue
        else -> Primary
    }

    val amountColor = when (transaction.type) {
        TransactionType.INCOME, TransactionType.REPAYMENT_RECEIVED -> IncomeGreen
        TransactionType.EXPENSE, TransactionType.LEND, TransactionType.REPAYMENT_PAID -> ExpenseRed
        TransactionType.TRANSFER -> TransferBlue
        else -> Primary
    }

    val prefix = when (transaction.type) {
        TransactionType.INCOME, TransactionType.REPAYMENT_RECEIVED -> "+"
        TransactionType.EXPENSE, TransactionType.LEND, TransactionType.REPAYMENT_PAID -> "-"
        else -> ""
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Category Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = category?.name ?: "Category",
                    tint = iconTintColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Middle Description and Badges
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description.ifEmpty { category?.name ?: transaction.type.displayName },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val accountDesc = if (transaction.type == TransactionType.TRANSFER && destAccount != null) {
                        "${account?.name ?: "Account"} → ${destAccount.name}"
                    } else {
                        account?.name ?: "Account"
                    }
                    Text(
                        text = accountDesc,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right Amount and Delete Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "$prefix${CurrencyUtils.formatPaise(transaction.amountPaise)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
