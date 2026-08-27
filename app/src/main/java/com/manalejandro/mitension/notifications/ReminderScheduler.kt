package com.manalejandro.mitension.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.manalejandro.mitension.data.db.Reminder
import java.util.Calendar

object ReminderScheduler {

    private const val EXTRA_KIND = "kind"

    fun requestCode(kind: String): Int = kind.hashCode()

    fun pendingIntent(context: Context, kind: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(kind)
            .putExtra(EXTRA_KIND, kind)
        return PendingIntent.getBroadcast(
            context,
            requestCode(kind),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun rescheduleAll(context: Context, reminders: List<Reminder>) {
        reminders.forEach { reminder ->
            if (reminder.enabled) {
                schedule(context, reminder)
            } else {
                cancel(context, reminder.kind)
            }
        }
    }

    fun schedule(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(context, reminder.kind)
        val triggerAt = nextTriggerMillis(reminder.hour, reminder.minute)
        if (canScheduleExact(context, alarmManager)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                WINDOW_WINDOW_MS,
                pi
            )
        }
    }

    fun cancel(context: Context, kind: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(pendingIntent(context, kind))
    }

    fun canScheduleExact(context: Context, alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return alarmManager.canScheduleExactAlarms()
    }

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!trigger.after(now)) {
            trigger.add(Calendar.DAY_OF_YEAR, 1)
        }
        return trigger.timeInMillis
    }

    private const val WINDOW_WINDOW_MS = 10 * 60 * 1000L
}