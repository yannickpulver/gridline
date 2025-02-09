package com.yannickpulver.gridline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.memory.MemoryCache
import coil3.util.DebugLogger
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yannickpulver.gridline.ui.auth.AuthScreen
import com.yannickpulver.gridline.ui.feed.FeedScreen
import com.yannickpulver.gridline.ui.navigation.RootComponent
import com.yannickpulver.gridline.ui.route.RouteScreen
import com.yannickpulver.gridline.ui.snackbar.SnackbarWrapper
import com.yannickpulver.gridline.ui.theme.AppTheme
import org.koin.compose.KoinContext

@Composable
fun App(rootComponent: RootComponent, modifier: Modifier = Modifier, darkTheme: Boolean = false) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .logger(DebugLogger())
            .build()
    }

    AppTheme(useDarkTheme = darkTheme, dynamicColor = true) {
        KoinContext {
            SnackbarWrapper {
                val childStack by rootComponent.childStack.subscribeAsState()
                Children(
                    stack = childStack,
                ) { child ->
                    when (val instance = child.instance) {
                        is RootComponent.Child.Auth -> AuthScreen(instance.component)
                        is RootComponent.Child.Feed -> FeedScreen(
                            component = instance.component,
                            modifier = modifier
                        )

                        is RootComponent.Child.Route -> RouteScreen(instance.component)
                    }
                }
            }
        }
    }
}
