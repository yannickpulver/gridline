package com.yannickpulver.gridline.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin

fun initKoin(extras: KoinApplication.() -> Unit): KoinApplication {
    return startKoin(extras = extras)
}

fun initKoin(): KoinApplication {
    return startKoin(extras = {})
}

private fun startKoin(extras: KoinApplication.() -> Unit) = startKoin {
    extras()
    modules(
        platformModule(),
        commonModule
    )
}
