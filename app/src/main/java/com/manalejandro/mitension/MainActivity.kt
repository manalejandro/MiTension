package com.manalejandro.mitension

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.manalejandro.mitension.ui.LocalAppLanguage
import com.manalejandro.mitension.ui.MainScreen
import com.manalejandro.mitension.ui.MainViewModel
import com.manalejandro.mitension.ui.screens.onboarding.OnboardingScreen
import com.manalejandro.mitension.ui.theme.MiTensionTheme
import com.manalejandro.mitension.ui.theme.TextScale
import com.manalejandro.mitension.util.AppLocale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()
            val language = settings.language ?: remember { AppLocale.systemLanguage() }
            val darkTheme = isSystemInDarkTheme()

            CompositionLocalProvider(LocalAppLanguage provides language) {
                MiTensionTheme(
                    darkTheme = darkTheme,
                    textScale = TextScale.fromName(settings.textScale)
                ) {
                    if (!settings.onboardingDone) {
                        OnboardingScreen(onComplete = vm::completeOnboarding)
                    } else {
                        MainScreen(vm = vm)
                    }
                }
            }
        }
    }
}