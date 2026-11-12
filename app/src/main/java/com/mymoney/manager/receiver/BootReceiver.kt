package com.mymoney.manager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mymoney.manager.reminder.DailyReminderManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            DailyReminderManager.scheduleDailyReminder(context, 21, 0)
        }
    }
}
