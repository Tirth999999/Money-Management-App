package com.mymoney.manager

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class MyMoneyApp : Application() {

    companion object {
        const val CHANNEL_DRAFTS = "drafts_channel"
        const val CHANNEL_REMINDERS = "reminders_channel"
        lateinit var instance: MyMoneyApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val draftsChannel = NotificationChannel(
                CHANNEL_DRAFTS,
                "Transaction Drafts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for newly detected transaction drafts"
                enableVibration(true)
            }

            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Pending Drafts Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily 9 PM reminders for unreviewed drafts"
            }

            notificationManager.createNotificationChannel(draftsChannel)
            notificationManager.createNotificationChannel(remindersChannel)
        }
    }
}
