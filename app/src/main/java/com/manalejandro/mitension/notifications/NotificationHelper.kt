package com.manalejandro.mitension.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.manalejandro.mitension.MainActivity
import com.manalejandro.mitension.R
import com.manalejandro.mitension.data.db.Reminder
import com.manalejandro.mitension.util.AppLocale

object NotificationHelper {

    private const val CHANNEL_PREFIX = "reminder_channel_"
    const val SILENT_SOUND = "__silent__"

    fun channelId(kind: String): String = "$CHANNEL_PREFIX$kind"

    fun createChannel(context: Context, reminder: Reminder, label: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val silent = reminder.soundUri == SILENT_SOUND
        val sound: Uri? = if (silent) null else reminder.soundUri
            ?.takeIf { it.isNotBlank() }
            ?.let { Uri.parse(it) }
        val resolvedSound = sound ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val channel = NotificationChannel(
            channelId(reminder.kind),
            label,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            if (silent) {
                setSound(null, null)
            } else {
                setSound(
                    resolvedSound,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            enableVibration(!silent)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
        }
        manager.createNotificationChannel(channel)
    }

    fun buildNotification(
        context: Context,
        reminder: Reminder,
        language: String,
        label: String
    ): android.app.Notification {
        val title = label
        val text = AppLocale.getString(context, language, R.string.notification_text)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            ReminderScheduler.requestCode(reminder.kind),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, channelId(reminder.kind))
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    fun notificationsEnabled(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        return manager.areNotificationsEnabled()
    }
}