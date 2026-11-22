package com.yannickpulver.gridline

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.yannickpulver.gridline.data.grid.GridStore
import com.yannickpulver.gridline.di.initKoin
import com.yannickpulver.gridline.ui.theme.AppTheme
import com.yannickpulver.gridline.ui.navigation.RootComponent
import com.yannickpulver.gridline.utils.SparkleUpdater
import com.yannickpulver.gridline.utils.runOnUiThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File

val koin = initKoin().koin

@OptIn(ExperimentalDecomposeApi::class)
fun main() {
    val lifecycle = LifecycleRegistry()

    // Always create the root component outside Compose on the UI thread
    val rootComponent =
        runOnUiThread {
            RootComponent(
                componentContext = DefaultComponentContext(lifecycle = lifecycle),
            )
        }

    SparkleUpdater.start()

    application {
        val windowState = rememberWindowState(width = 400.dp, height = 800.dp)
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Gridline",
        ) {
            if (SparkleUpdater.isAvailable) {
                MenuBar {
                    Menu("Help") {
                        Item("Check for Updates…", onClick = SparkleUpdater::checkForUpdates)
                    }
                }
            }
            // Resumes the lifecycle when the window gains focus, pauses it when focus is lost
            LifecycleController(lifecycle, windowState, LocalWindowInfo.current)
            AppTheme(useDarkTheme = false, dynamicColor = false) {
                MainApp(rootComponent)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun MainApp(rootComponent: RootComponent, gridStore: GridStore = koinInject()) {
    val callback = remember {
        object : DragAndDropTarget {

            // Highlights the border of a potential drop target
            override fun onStarted(event: DragAndDropEvent) {
            }

            override fun onEnded(event: DragAndDropEvent) {
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                println("YY onDrag ${event.dragData()}")

                when (val dragData = event.dragData()) {
                    is DragData.FilesList -> {
                        dragData.readFiles().map {
                            val path = if (it.startsWith("file")) {
                                it.drop(5).replace("%20", " ")
                            } else {
                                it
                            }

                            CoroutineScope(Dispatchers.IO).launch {
                                val file = File(path)
                                gridStore.add(file.readBytes(), file.extension)
                            }
                        }
                        return true
                    }

                    else -> {
                        return false
                    }
                }
            }
        }
    }

    App(
        rootComponent = rootComponent,
        darkTheme = false,
        modifier = Modifier.fillMaxSize()
            .dragAndDropTarget(
                shouldStartDragAndDrop = {
                    println("YY shouldStartDragAndDrop $it")
                    true
                },
                target = callback
            )
    )
}

@Preview
@Composable
fun AppDesktopPreview() {
    App(
        rootComponent = RootComponent(DefaultComponentContext(LifecycleRegistry())),
    )
}
