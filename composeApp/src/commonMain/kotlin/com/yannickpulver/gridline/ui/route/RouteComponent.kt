package com.yannickpulver.gridline.ui.route

import com.arkivanov.decompose.ComponentContext
import com.yannickpulver.gridline.data.prefs.AppPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RouteComponent(
    componentContext: ComponentContext,
    navigateToFeed: () -> Unit,
    navigateToAuth: () -> Unit
) : KoinComponent, ComponentContext by componentContext {

    private val prefs: AppPrefs by inject()

    init {
        CoroutineScope(Dispatchers.Main).launch {
            if (prefs.hasUserInfo()) {
                navigateToFeed()
            } else {
                navigateToAuth()
            }
        }
    }
}
