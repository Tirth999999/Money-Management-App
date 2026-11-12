package com.mymoney.manager.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.mymoney.manager.MainActivity
import com.mymoney.manager.MyMoneyApp
import com.mymoney.manager.R
import com.mymoney.manager.data.local.database.AppDatabase
import com.mymoney.manager.data.local.entity.TransactionDraftEntity
import com.mymoney.manager.data.model.CurrencyUtils
import com.mymoney.manager.data.model.DetectionSource
import com.mymoney.manager.data.model.DraftStatus
import com.mymoney.manager.data.model.TransactionType
import com.mymoney.manager.data.repository.FinanceRepository
import com.mymoney.manager.parser.HdfcSmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Concatenate multi-part SMS messages
        val fullBodyBuilder = StringBuilder()
        var timestamp = System.currentTimeMillis()

        for (sms in messages) {
            fullBodyBuilder.append(sms.displayMessageBody)
            timestamp = sms.timestampMillis
        }

        val fullBody = fullBodyBuilder.toString()
        if (fullBody.isBlank()) return

        // Run SMS parsing and draft insertion in background CoroutineScope
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val parsed = HdfcSmsParser.parse(fullBody, timestamp)
                if (parsed != null) {
                    val db = AppDatabase.getDatabase(context)
                    val repository = FinanceRepository(db)

                    // Prevent duplicate drafts
                    val isDuplicate = repository.isDraftDuplicate(parsed.referenceId)
                    if (!isDuplicate) {
                        val hdfcAccount = repository.getOrCreateDefaultAccount(isOnline = true)

                        // Try finding matching category
                        var categoryId: Long? = null
                        if (parsed.suggestedCategoryName != null) {
                            val cat = db.categoryDao().findCategoryByName(parsed.suggestedCategoryName)
                            categoryId = cat?.id
                        }

                        val draft = TransactionDraftEntity(
                            type = parsed.type,
                            amountPaise = parsed.amountPaise,
                            accountId = hdfcAccount.id,
                            categoryId = categoryId,
                            description = parsed.description,
                            transactionDate = parsed.dateMillis,
                            source = DetectionSource.SMS,
                            originalSms = fullBody,
                            transactionReference = parsed.referenceId,
                            confidence = parsed.confidence,
                            status = DraftStatus.PENDING
                        )

                        val draftId = repository.createDraft(draft)

                        // Send confirmation notification
                        showDraftCreatedNotification(context, draft.copy(id = draftId))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showDraftCreatedNotification(context: Context, draft: TransactionDraftEntity) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_TAB", "DRAFTS")
            putExtra("EXTRA_DRAFT_ID", draft.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            draft.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val amountStr = CurrencyUtils.formatPaise(draft.amountPaise)
        val typeStr = if (draft.type == TransactionType.INCOME) "income" else "expense"
        val message = "$amountStr $typeStr draft created from HDFC."

        val notification = NotificationCompat.Builder(context, MyMoneyApp.CHANNEL_DRAFTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF2563EB.toInt())
            .setContentTitle("New transaction draft created")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$message\nTap to review and approve."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(draft.id.toInt(), notification)
    }
}
