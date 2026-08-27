package com.manalejandro.mitension.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.manalejandro.mitension.util.AppLocale

val LocalAppLanguage = staticCompositionLocalOf { "es" }

@Composable
fun str(@StringRes resId: Int): String {
    val language = LocalAppLanguage.current
    val context = LocalContext.current
    return AppLocale.getString(context, language, resId)
}

@Composable
fun str(@StringRes resId: Int, vararg args: Any): String {
    val language = LocalAppLanguage.current
    val context = LocalContext.current
    return AppLocale.getString(context, language, resId, *args)
}