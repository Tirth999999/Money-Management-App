package com.mymoney.manager.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

object VectorIconHelper {

    fun getIconByName(iconName: String): ImageVector {
        return when (iconName.lowercase()) {
            "account_balance", "bank", "hdfc" -> Icons.Default.AccountBalance
            "payments", "cash", "offline" -> Icons.Default.Payments
            "savings" -> Icons.Default.Savings
            "wallet", "account_balance_wallet" -> Icons.Default.AccountBalanceWallet
            "work", "salary" -> Icons.Default.Work
            "computer", "freelance" -> Icons.Default.Computer
            "assignment", "project" -> Icons.Default.Assignment
            "restaurant", "food" -> Icons.Default.Restaurant
            "shopping_bag", "shopping", "shopping_cart" -> Icons.Default.ShoppingBag
            "directions_car", "travel", "transport" -> Icons.Default.DirectionsCar
            "receipt_long", "bills", "bill" -> Icons.Default.ReceiptLong
            "movie", "entertainment" -> Icons.Default.Movie
            "medical_services", "health" -> Icons.Default.MedicalServices
            "school", "education" -> Icons.Default.School
            "person", "personal" -> Icons.Default.Person
            "credit_card" -> Icons.Default.CreditCard
            "sync_alt", "transfer" -> Icons.Default.SyncAlt
            "trending_up", "income" -> Icons.Default.TrendingUp
            "trending_down", "expense" -> Icons.Default.TrendingDown
            "assessment", "analysis" -> Icons.Default.Assessment
            else -> Icons.Default.Category
        }
    }
}
