package com.manalejandro.mitension.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.manalejandro.mitension.MiTensionApplication
import com.manalejandro.mitension.R
import com.manalejandro.mitension.util.AppLocale
import com.manalejandro.mitension.util.ReminderKinds
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra("kind") ?: return
        val app = context.applicationContext as MiTensionApplication

        app.applicationScope.launch {
            val reminder = app.database.reminderDao().getByKind(kind) ?: return@launch
            if (!reminder.enabled) return@launch

            val settings = app.settingsRepository.current()
            val language = settings.language ?: AppLocale.systemLanguage()
            val label = AppLocale.getString(context, language, ReminderKinds.labelRes(kind))

            NotificationHelper.createChannel(context, reminder, label)

            val manager = NotificationManagerCompat.from(context)
            val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

            if (canNotify && manager.areNotificationsEnabled()) {
                val notification = NotificationHelper.buildNotification(context, reminder, language, label)
                manager.notify(ReminderScheduler.requestCode(kind), notification)
            }

            ReminderScheduler.schedule(context, reminder)
        }
    }
}