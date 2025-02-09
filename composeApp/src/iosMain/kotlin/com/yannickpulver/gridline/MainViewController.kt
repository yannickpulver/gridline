package com.yannickpulver.gridline

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.lifecycle.ApplicationLifecycle
import com.yannickpulver.gridline.ui.navigation.RootComponent
import platform.UIKit.UIViewController

@OptIn(ExperimentalDecomposeApi::class)
fun MainViewController(): UIViewController {
    val rootComponent =
        RootComponent(
            componentContext = DefaultComponentContext(lifecycle = ApplicationLifecycle()),
        )

    return ComposeUIViewController {
        App(rootComponent = rootComponent)
    }
}
