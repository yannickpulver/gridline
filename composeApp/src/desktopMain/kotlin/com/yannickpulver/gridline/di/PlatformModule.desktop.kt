package com.yannickpulver.gridline.di

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import com.yannickpulver.gridline.data.grid.LocalImageFiles
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.prefs.Preferences

actual fun platformModule(): Module = module {
    single<Settings> { PreferencesSettings(Preferences.userRoot()) }

    // In the sandbox (App Store build) the home folder is the app's container
    single { LocalImageFiles(System.getProperty("user.home").toPath() / "Library/Application Support/Gridline/grid") }
}
