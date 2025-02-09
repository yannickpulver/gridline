package com.yannickpulver.gridline.ui.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.yannickpulver.gridline.ui.auth.AuthComponent
import com.yannickpulver.gridline.ui.feed.FeedComponent
import com.yannickpulver.gridline.ui.route.RouteComponent
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent

class RootComponent(componentContext: ComponentContext) :
    KoinComponent,
    ComponentContext by componentContext {

    private val navigation = StackNavigation<Configuration>()

    val childStack = childStack(
        source = navigation,
        serializer = Configuration.serializer(),
        initialConfiguration = Configuration.Route,
        handleBackButton = true,
        childFactory = ::createChild
    )

    @OptIn(ExperimentalDecomposeApi::class)
    private fun createChild(
        configuration: Configuration,
        componentContext: ComponentContext
    ): Child =
        when (configuration) {
            is Configuration.Feed -> Child.Feed(
                FeedComponent(
                    componentContext = componentContext,
                    onReset = { navigation.replaceCurrent(Configuration.Route) }
                )
            )
            is Configuration.Auth -> Child.Auth(
                AuthComponent(
                    componentContext = componentContext,
                    navigateToFeed = { navigation.replaceCurrent(Configuration.Feed) }
                )
            )

            is Configuration.Route -> Child.Route(
                RouteComponent(
                    componentContext = componentContext,
                    navigateToFeed = { navigation.replaceCurrent(Configuration.Feed) },
                    navigateToAuth = { navigation.replaceCurrent(Configuration.Auth) }
                )
            )
        }

    sealed class Child {
        data class Route(val component: RouteComponent) : Child()
        data class Feed(val component: FeedComponent) : Child()
        data class Auth(val component: AuthComponent) : Child()
    }

    @Serializable
    sealed class Configuration {
        @Serializable
        data object Feed : Configuration()

        @Serializable
        data object Auth : Configuration()

        @Serializable
        data object Route : Configuration()
    }
}
