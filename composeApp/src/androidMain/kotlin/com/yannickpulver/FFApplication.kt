package com.yannickpulver

import android.app.Application
import com.yannickpulver.gridline.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class FFApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidLogger()
            androidContext(this@FFApplication)
        }
    }
}
