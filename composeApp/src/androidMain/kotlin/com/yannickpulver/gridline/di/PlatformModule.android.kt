package com.yannickpulver.gridline.di

import android.content.Context
import android.content.SharedPreferences
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { provideSettingsPreferences(get()) }

    single<Settings> { SharedPreferencesSettings(get()) }
}

private const val PREFERENCES_FILE_KEY = "com.yannickpulver.gridline.preferences"
private fun provideSettingsPreferences(context: Context): SharedPreferences =
    context.getSharedPreferences(PREFERENCES_FILE_KEY, Context.MODE_PRIVATE)
