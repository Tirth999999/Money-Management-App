package com.mymoney.manager.ui.analysis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mymoney.manager.data.local.entity.LoanEntity
import com.mymoney.manager.data.model.CurrencyUtils
import com.mymoney.manager.data.model.LoanType
import com.mymoney.manager.data.model.TransactionType
import com.mymoney.manager.ui.MainViewModel
import com.mymoney.manager.ui.TimePeriod
import com.mymoney.manager.ui.components.VectorIconHelper
import com.mymoney.manager.ui.dialogs.SettleLoanDialog
import com.mymoney.manager.ui.theme.BorderLight
import com.mymoney.manager.ui.theme.ExpenseContainer
import com.mymoney.manager.ui.theme.ExpenseRed
import com.mymoney.manager.ui.theme.IncomeContainer
import com.mymoney.manager.ui.theme.IncomeGreen
import com.mymoney.manager.ui.theme.Primary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalysisScreen(viewModel: MainViewModel) {
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val transactions by viewModel.filteredTransactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val incomeTotal by viewModel.periodIncomePaise.collectAsState()
    val expenseTotal by viewModel.periodExpensePaise.collectAsState()

    val lentLoans by viewModel.lentLoans.collectAsState()
    val borrowedLoans by viewModel.borrowedLoans.collectAsState()
    val totalReceivables by viewModel.totalReceivablesPaise.collectAsState()
    val totalLiabilities by viewModel.totalLiabilitiesPaise.collectAsState()
    val persons by viewModel.persons.collectAsState()

    var loanToSettle by remember { mutableStateOf<LoanEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Period Tabs
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

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Income vs Expense Overview Card
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Cash Flow Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Income", tint = IncomeGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Income", style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(
                                    text = CurrencyUtils.formatPaise(incomeTotal),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Expense", tint = ExpenseRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Expense", style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(
                                    text = CurrencyUtils.formatPaise(expenseTotal),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Visual Proportional Bar
                        val totalFlow = (incomeTotal + expenseTotal).coerceAtLeast(1L)
                        val incomeRatio = incomeTotal.toFloat() / totalFlow.toFloat()
                        LinearProgressIndicator(
                            progress = { incomeRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = IncomeGreen,
                            trackColor = ExpenseRed
                        )
                    }
                }
            }

            // 2. Spending Trend Chart (Canvas Bar Chart)
            item {
                val expenseTxs = transactions.filter { it.type == TransactionType.EXPENSE }
                val dayFormat = remember { SimpleDateFormat("dd", Locale.getDefault()) }
                val dailySpending = remember(expenseTxs) {
                    expenseTxs.groupBy { dayFormat.format(Date(it.transactionDate)) }
                        .mapValues { entry -> entry.value.sumOf { it.amountPaise } }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Daily Spending Trend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (dailySpending.isEmpty()) {
                            Text(
                                text = "No expense records in this period",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val maxDaily = (dailySpending.values.maxOrNull() ?: 1L).coerceAtLeast(1L)
                            val daysList = dailySpending.toList().takeLast(10) // show up to last 10 active days

                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                            ) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height
                                val barWidth = (canvasWidth / (daysList.size * 2f)).coerceAtMost(30.dp.toPx())
                                val spacing = canvasWidth / daysList.size

                                daysList.forEachIndexed { index, (dayStr, amount) ->
                                    val barHeight = (amount.toFloat() / maxDaily.toFloat()) * (canvasHeight - 30f)
                                    val x = index * spacing + (spacing - barWidth) / 2f
                                    val y = canvasHeight - barHeight

                                    drawRoundRect(
                                        color = ExpenseRed,
                                        topLeft = Offset(x, y),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                }
                            }

                            // Day labels
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                daysList.forEach { (dayStr, _) ->
                                    Text(
                                        text = dayStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Category-wise Expenses Breakdown
            item {
                val expenseTxs = transactions.filter { it.type == TransactionType.EXPENSE }
                val catExpenses = remember(expenseTxs, categories) {
                    expenseTxs.groupBy { it.categoryId }
                        .map { (catId, txList) ->
                            val cat = categories.find { it.id == catId }
                            val total = txList.sumOf { it.amountPaise }
                            Triple(cat?.name ?: "Other", cat?.iconName ?: "category", total)
                        }
                        .sortedByDescending { it.third }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Category Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (catExpenses.isEmpty()) {
                            Text(
                                text = "No category data available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val totalExp = expenseTotal.coerceAtLeast(1L)
                            catExpenses.forEach { (name, icon, amount) ->
                                val pct = (amount.toFloat() / totalExp.toFloat())
                                val pctInt = (pct * 100).toInt()

                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = VectorIconHelper.getIconByName(icon),
                                                contentDescription = name,
                                                tint = ExpenseRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        }
                                        Text(
                                            text = "${CurrencyUtils.formatPaise(amount)} ($pctInt%)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { pct },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = ExpenseRed,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Lending & Borrowing Section
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Lending & Borrowing Overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Receivables
                            Column {
                                Text("Money Owed to Me", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = CurrencyUtils.formatPaise(totalReceivables),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                            }

                            // Liabilities
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Money I Owe Others", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = CurrencyUtils.formatPaise(totalLiabilities),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                        }

                        if (lentLoans.isNotEmpty() || borrowedLoans.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Active Records", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Lent loans
                            lentLoans.forEach { loan ->
                                val person = persons.find { it.id == loan.personId }
                                ActiveLoanRow(
                                    loan = loan,
                                    personName = person?.name ?: "Contact",
                                    onSettleClick = { loanToSettle = loan }
                                )
                            }

                            // Borrowed loans
                            borrowedLoans.forEach { loan ->
                                val person = persons.find { it.id == loan.personId }
                                ActiveLoanRow(
                                    loan = loan,
                                    personName = person?.name ?: "Contact",
                                    onSettleClick = { loanToSettle = loan }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Settle Loan Dialog
    loanToSettle?.let { loan ->
        val person = persons.find { it.id == loan.personId }
        SettleLoanDialog(
            loan = loan,
            personName = person?.name ?: "Contact",
            accounts = accounts,
            onDismiss = { loanToSettle = null },
            onSettle = { amount, accId, notes ->
                viewModel.recordLoanRepayment(loan.id, amount, accId, notes)
                loanToSettle = null
            }
        )
    }
}

@Composable
fun ActiveLoanRow(
    loan: LoanEntity,
    personName: String,
    onSettleClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = if (loan.type == LoanType.LENT) "Lent to $personName" else "Borrowed from $personName",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Outstanding: ${CurrencyUtils.formatPaise(loan.outstandingAmountPaise)}",
                style = MaterialTheme.typography.labelSmall,
                color = if (loan.type == LoanType.LENT) IncomeGreen else ExpenseRed
            )
        }

        Button(
            onClick = onSettleClick,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text("Repay", style = MaterialTheme.typography.labelSmall)
        }
    }
}
