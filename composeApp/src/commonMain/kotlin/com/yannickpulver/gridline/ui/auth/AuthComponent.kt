package com.yannickpulver.gridline.ui.auth

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.yannickpulver.gridline.data.api.InstaApi
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

    /** Gridline ID from before, so existing grids survive the switch to Instagram Login. */
    val existingUuid: String = prefs.getUuid().orEmpty()

    private val _waitingForLogin = MutableStateFlow(false)
    val waitingForLogin: StateFlow<Boolean> = _waitingForLogin

    init {
        lifecycle.doOnDestroy { scope.cancel() }
    }

    /** Waits for the login in the browser to finish and returns the URL to open. */
    fun startLogin(uuid: String): String {
        val state = Uuid.random().toString()
        loginJob?.cancel()
        loginJob = scope.launch {
            _waitingForLogin.value = true
            val token = withTimeoutOrNull(10.minutes) {
                var token: String? = null
                while (token == null) {
                    delay(2.seconds)
                    token = runCatching { instaApi.fetchLoginToken(state) }.getOrNull()
                }
                token
            }
            _waitingForLogin.value = false

            if (token == null) {
                SnackbarStateHolder.error("Instagram login timed out")
                return@launch
            }
            prefs.setInstaLogin(token, uuid.trim().ifEmpty { null })
            navigateToFeed()
        }
        return instaApi.loginUrl(state)
    }
}
