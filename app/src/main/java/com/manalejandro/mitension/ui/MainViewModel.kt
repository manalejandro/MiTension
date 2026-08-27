package com.manalejandro.mitension.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.manalejandro.mitension.MiTensionApplication
import com.manalejandro.mitension.data.AppSettings
import com.manalejandro.mitension.data.SettingsRepository
import com.manalejandro.mitension.data.db.Reading
import com.manalejandro.mitension.data.db.Reminder
import com.manalejandro.mitension.data.db.User
import com.manalejandro.mitension.notifications.NotificationHelper
import com.manalejandro.mitension.notifications.ReminderScheduler
import com.manalejandro.mitension.util.AppLocale
import com.manalejandro.mitension.util.ReminderKinds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MiTensionApplication
    private val userDao = app.database.userDao()
    private val readingDao = app.database.readingDao()
    private val reminderDao = app.database.reminderDao()
    private val settingsRepository: SettingsRepository = app.settingsRepository

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val users: StateFlow<List<User>> = userDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeUser: StateFlow<User?> = combine(settings, users) { s, us ->
        us.firstOrNull { it.id == s.activeUserId }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val readings: StateFlow<List<Reading>> = activeUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else readingDao.observeByUser(user.id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val reminders: StateFlow<List<Reminder>> = reminderDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            settingsRepository.ensureDefaultReminders(reminderDao)
        }
    }

    // ---- Onboarding / users ----

    fun completeOnboarding(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val id = userDao.insert(User(name = trimmed, isPrimary = true))
            settingsRepository.setActiveUser(id)
            settingsRepository.markOnboardingDone()
        }
    }

    fun createUser(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            userDao.insert(User(name = trimmed))
        }
    }

    fun renameUser(user: User, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            userDao.update(user.copy(name = trimmed))
        }
    }

    fun setActiveUser(id: Long) {
        viewModelScope.launch { settingsRepository.setActiveUser(id) }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            if (user.isPrimary) return@launch
            val count = userDao.count()
            if (count <= 1) return@launch
            userDao.delete(user)
        }
    }

    // ---- Readings ----

    fun addReading(systolic: Double, diastolic: Double, pulse: Double, note: String) {
        val user = activeUser.value ?: return
        viewModelScope.launch {
            readingDao.insert(
                Reading(
                    userId = user.id,
                    systolic = systolic,
                    diastolic = diastolic,
                    pulse = pulse,
                    note = note.trim()
                )
            )
        }
    }

    fun updateReading(reading: Reading) {
        viewModelScope.launch { readingDao.update(reading) }
    }

    fun deleteReading(reading: Reading) {
        viewModelScope.launch { readingDao.delete(reading) }
    }

    // ---- Reminders ----

    fun setReminderEnabled(kind: String, enabled: Boolean) {
        updateReminder(kind) { it.copy(enabled = enabled) }
    }

    fun setReminderTime(kind: String, hour: Int, minute: Int) {
        updateReminder(kind) { it.copy(hour = hour, minute = minute) }
    }

    fun setReminderSound(kind: String, soundUri: String?) {
        updateReminder(kind) { it.copy(soundUri = soundUri) }
    }

    private fun updateReminder(kind: String, transform: (Reminder) -> Reminder) {
        viewModelScope.launch {
            val current = reminderDao.getByKind(kind) ?: return@launch
            val updated = transform(current)
            reminderDao.update(updated)
            applyReminder(updated)
        }
    }

    private suspend fun applyReminder(reminder: Reminder) {
        val settings = settingsRepository.current()
        val language = settings.language ?: AppLocale.systemLanguage()
        val label = AppLocale.getString(app, language, ReminderKinds.labelRes(reminder.kind))
        NotificationHelper.createChannel(app, reminder, label)
        if (reminder.enabled) {
            ReminderScheduler.schedule(app, reminder)
        } else {
            ReminderScheduler.cancel(app, reminder.kind)
        }
    }

    // ---- Settings ----

    fun setLanguage(language: String) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
            refreshChannels()
        }
    }

    fun setTextScale(scale: String) {
        viewModelScope.launch { settingsRepository.setTextScale(scale) }
    }

    private suspend fun refreshChannels() {
        val settings = settingsRepository.current()
        val language = settings.language ?: AppLocale.systemLanguage()
        reminderDao.getAll().forEach { reminder ->
            val label = AppLocale.getString(app, language, ReminderKinds.labelRes(reminder.kind))
            NotificationHelper.createChannel(app, reminder, label)
        }
    }
}