package com.yannickpulver.gridline.di

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.prefs.Preferences

actual fun platformModule(): Module = module {
    single<Settings> { PreferencesSettings(Preferences.userRoot()) }
}
