package com.manalejandro.mitension.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object AppLocale {

    fun systemLanguage(): String {
        val locale = Locale.getDefault().language
        return if (locale == "en") "en" else "es"
    }

    fun contextForLanguage(context: Context, language: String): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale(language))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(android.os.LocaleList(Locale(language)))
        }
        return context.createConfigurationContext(config)
    }

    fun getString(context: Context, language: String, resId: Int): String =
        contextForLanguage(context, language).getString(resId)

    fun getString(context: Context, language: String, resId: Int, vararg args: Any): String =
        contextForLanguage(context, language).getString(resId, *args)
}