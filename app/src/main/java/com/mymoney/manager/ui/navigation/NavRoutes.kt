package com.mymoney.manager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Records : Screen("records", "Records", Icons.Default.ListAlt)
    object Analysis : Screen("analysis", "Analysis", Icons.Default.Assessment)
    object Accounts : Screen("accounts", "Accounts", Icons.Default.AccountBalance)
    object Categories : Screen("categories", "Categories", Icons.Default.Category)
    object Drafts : Screen("drafts", "Drafts", Icons.Default.PendingActions)
}

val BottomNavItems = listOf(
    Screen.Records,
    Screen.Analysis,
    Screen.Accounts,
    Screen.Categories,
    Screen.Drafts
)
