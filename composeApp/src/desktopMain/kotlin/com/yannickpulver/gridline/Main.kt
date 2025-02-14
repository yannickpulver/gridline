package com.yannickpulver.gridline

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.yannickpulver.gridline.data.api.SupabaseApi
import com.yannickpulver.gridline.di.initKoin
import com.yannickpulver.gridline.ui.feed.Menu
import com.yannickpulver.gridline.ui.navigation.RootComponent
import com.yannickpulver.gridline.ui.snackbar.SnackbarStateHolder
import com.yannickpulver.gridline.utils.runOnUiThread
import io.kanro.compose.jetbrains.expui.window.JBWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File

val koin = initKoin().koin

fun main() {
    val lifecycle = LifecycleRegistry()

    // Always create the root component outside Compose on the UI thread
    val rootComponent =
        runOnUiThread {
            RootComponent(
                componentContext = DefaultComponentContext(lifecycle = lifecycle),
            )
        }

    application {
        JBWindow(
            onCloseRequest = ::exitApplication,
            state = WindowState(size = DpSize(400.dp, 800.dp)),
            title = "Gridline",
            mainToolBar = {
                val childStack by rootComponent.childStack.subscribeAsState()
                Row(
                    Modifier.mainToolBarItem(Alignment.End, true),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val instance = childStack.active.instance
                    if (instance is RootComponent.Child.Feed) {
                        Menu(
                            uuid = instance.component.state.value.uuid,
                            addPlaceholder = { instance.component.addPlaceholder() },
                            reset = { instance.component.reset() },
                            toggleBorders = { instance.component.toggleBorders() },
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.MoreHoriz,
                                    contentDescription = "More",
                                    tint = Color.White
                                )
                            })
                    }
                }
            }
        ) {
            MainApp(rootComponent)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun MainApp(rootComponent: RootComponent, supabaseApi: SupabaseApi = koinInject()) {
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
                                SnackbarStateHolder.success("Uploading...")
                                supabaseApi.putImage(file.readBytes(), file.extension)
                                SnackbarStateHolder.success("Success!")
                                supabaseApi.fetchImages()
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
