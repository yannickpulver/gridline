package com.yannickpulver.gridline.ui.snackbar

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Global holder of snackbar state
 */
object SnackbarStateHolder {

    private var _snackbarState = MutableSharedFlow<SnackbarState>(replay = 0)
    val snackbarState: SharedFlow<SnackbarState> = _snackbarState

    suspend fun put(snackbar: SnackbarState) {
        _snackbarState.emit(snackbar)
    }

    suspend fun success() = success(message = null)
    suspend fun success(message: String? = null) =
        put(SnackbarState.Success(message ?: "Success"))

    suspend fun error(message: String? = null) =
        put(SnackbarState.Error(message ?: "Error"))
}

// String messages may be replaced with StringRes, depending on what is more convenient.
sealed interface SnackbarState {
    val message: String

    data class Success(override val message: String) : SnackbarState
    data class Error(override val message: String) : SnackbarState
    data class Info(override val message: String) : SnackbarState

    sealed interface Message {
        class Text(val text: String) : Message
    }
}
