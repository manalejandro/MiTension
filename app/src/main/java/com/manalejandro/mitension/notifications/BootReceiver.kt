package com.manalejandro.mitension.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.manalejandro.mitension.MiTensionApplication
import com.manalejandro.mitension.util.AppLocale
import com.manalejandro.mitension.util.ReminderKinds
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != "android.intent.action.QUICKBOOT_POWERON") {
            return
        }
        val app = context.applicationContext as MiTensionApplication
        app.applicationScope.launch {
            val reminders = app.database.reminderDao().getAll()
            val settings = app.settingsRepository.current()
            val language = settings.language ?: AppLocale.systemLanguage()
            reminders.forEach { reminder ->
                NotificationHelper.createChannel(
                    context,
                    reminder,
                    AppLocale.getString(context, language, ReminderKinds.labelRes(reminder.kind))
                )
            }
            ReminderScheduler.rescheduleAll(context, reminders)
        }
    }
}