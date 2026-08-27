package com.manalejandro.mitension

import android.app.Application
import com.manalejandro.mitension.data.SettingsRepository
import com.manalejandro.mitension.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MiTensionApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: AppDatabase
        private set
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.get(this)
        settingsRepository = SettingsRepository(this)
    }
}