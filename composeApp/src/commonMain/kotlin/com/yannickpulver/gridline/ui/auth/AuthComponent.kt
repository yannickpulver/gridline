package com.yannickpulver.gridline.ui.auth

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.yannickpulver.gridline.data.api.InstaApi
import com.yannickpulver.gridline.data.dto.InstaLoginDto
import com.yannickpulver.gridline.data.prefs.AppPrefs
import com.yannickpulver.gridline.ui.snackbar.SnackbarStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class AuthComponent(
    componentContext: ComponentContext,
    private val navigateToFeed: () -> Unit
) : KoinComponent, ComponentContext by componentContext {

    private val prefs: AppPrefs by inject()
    private val instaApi: InstaApi by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var loginJob: Job? = null

    private val _waitingForLogin = MutableStateFlow(false)
    val waitingForLogin: StateFlow<Boolean> = _waitingForLogin

    init {
        lifecycle.doOnDestroy { scope.cancel() }
    }

    /** Waits for the login in the browser to finish and returns the URL to open. */
    fun startLogin(): String {
        val state = Uuid.random().toString()
        loginJob?.cancel()
        loginJob = scope.launch {
            _waitingForLogin.value = true
            val login = withTimeoutOrNull(10.minutes) {
                var login: InstaLoginDto? = null
                while (login == null) {
                    delay(2.seconds)
                    login = runCatching { instaApi.fetchLogin(state, prefs.getUuid()) }.getOrNull()
                }
                login
            }
            _waitingForLogin.value = false

            if (login == null) {
                SnackbarStateHolder.error("Instagram login timed out")
                return@launch
            }
            prefs.setInstaLogin(login.accessToken, login.gridlineId)
            navigateToFeed()
        }
        return instaApi.loginUrl(state)
    }
}
