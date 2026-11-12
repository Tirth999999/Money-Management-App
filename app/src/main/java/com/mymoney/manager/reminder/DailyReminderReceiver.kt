package com.mymoney.manager.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mymoney.manager.MainActivity
import com.mymoney.manager.MyMoneyApp
import com.mymoney.manager.R
import com.mymoney.manager.data.local.database.AppDatabase
import com.mymoney.manager.data.repository.FinanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val repository = FinanceRepository(db)
                val pendingCount = repository.getPendingDraftsCountSync()

                // Only send notification if pending drafts exist
                if (pendingCount > 0) {
                    showReminderNotification(context, pendingCount)
                }

                // Reschedule for next day 9 PM
                DailyReminderManager.scheduleDailyReminder(context, 21, 0)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun showReminderNotification(context: Context, pendingCount: Int) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_NAV_TAB", "DRAFTS")
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                9002,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val message = if (pendingCount == 1) {
                "You have 1 pending transaction to review."
            } else {
                "You have $pendingCount pending transactions to review."
            }

            val notification = NotificationCompat.Builder(context, MyMoneyApp.CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(0xFF2563EB.toInt())
                .setContentTitle("Pending Drafts Reminder")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$message\nTap to approve or reject them."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(9002, notification)
        }
    }
}
