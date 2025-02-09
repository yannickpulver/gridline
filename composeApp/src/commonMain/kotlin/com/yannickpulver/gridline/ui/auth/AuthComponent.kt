package com.yannickpulver.gridline.ui.auth

import com.arkivanov.decompose.ComponentContext
import com.yannickpulver.gridline.data.api.InstaApi
import com.yannickpulver.gridline.data.prefs.AppPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AuthComponent(
    componentContext: ComponentContext,
    private val navigateToFeed: () -> Unit
) : KoinComponent, ComponentContext by componentContext {

    private val prefs: AppPrefs by inject()

    fun setUsername(userName: String, uuid: String) {
        prefs.setUserName(userName, uuid.ifEmpty { null })
        navigateToFeed()
    }
}
