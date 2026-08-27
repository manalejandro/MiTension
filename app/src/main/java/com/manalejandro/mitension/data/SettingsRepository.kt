package com.manalejandro.mitension.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.manalejandro.mitension.data.db.Reminder
import com.manalejandro.mitension.data.db.ReminderDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "mitension_settings")

data class AppSettings(
    val onboardingDone: Boolean = false,
    val activeUserId: Long = -1L,
    val language: String? = null,
    val textScale: String = TEXT_SCALE_NORMAL
)

object SettingsKeys {
    val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    val ACTIVE_USER_ID = longPreferencesKey("active_user_id")
    val LANGUAGE = stringPreferencesKey("language")
    val TEXT_SCALE = stringPreferencesKey("text_scale")
}

const val TEXT_SCALE_NORMAL = "normal"
const val TEXT_SCALE_LARGE = "large"
const val TEXT_SCALE_XLARGE = "xlarge"

class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            onboardingDone = prefs[SettingsKeys.ONBOARDING_DONE] ?: false,
            activeUserId = prefs[SettingsKeys.ACTIVE_USER_ID] ?: -1L,
            language = prefs[SettingsKeys.LANGUAGE],
            textScale = prefs[SettingsKeys.TEXT_SCALE] ?: TEXT_SCALE_NORMAL
        )
    }

    suspend fun markOnboardingDone() {
        context.dataStore.edit { it[SettingsKeys.ONBOARDING_DONE] = true }
    }

    suspend fun setActiveUser(id: Long) {
        context.dataStore.edit { it[SettingsKeys.ACTIVE_USER_ID] = id }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { it[SettingsKeys.LANGUAGE] = language }
    }

    suspend fun setTextScale(scale: String) {
        context.dataStore.edit { it[SettingsKeys.TEXT_SCALE] = scale }
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun ensureDefaultReminders(reminderDao: ReminderDao) {
        val defaults = listOf(
            Reminder(kind = "morning", enabled = false, hour = 9, minute = 0),
            Reminder(kind = "midday", enabled = false, hour = 13, minute = 0),
            Reminder(kind = "evening", enabled = false, hour = 20, minute = 0)
        )
        for (reminder in defaults) {
            if (reminderDao.getByKind(reminder.kind) == null) {
                reminderDao.insert(reminder)
            }
        }
    }
}