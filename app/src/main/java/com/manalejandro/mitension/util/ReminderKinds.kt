package com.manalejandro.mitension.util

import com.manalejandro.mitension.R

object ReminderKinds {

    const val MORNING = "morning"
    const val MIDDAY = "midday"
    const val EVENING = "evening"

    val ALL: List<String> = listOf(MORNING, MIDDAY, EVENING)

    fun labelRes(kind: String): Int = when (kind) {
        MORNING -> R.string.reminder_morning
        MIDDAY -> R.string.reminder_midday
        else -> R.string.reminder_evening
    }
}