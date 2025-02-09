package com.yannickpulver.gridline.ui.snackbar

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * SnackbarController makes sure that always the latest snackbar is shown.
 * Could be done without it, but then we would not get instant feedback when a new snackbar is published.
 */
class SnackbarController(private val scope: CoroutineScope, private val snackbarHostState: SnackbarHostState) {
    private var snackbarJob: Job? = null

    init {
        cancelJob()
    }

    fun showSnackbar(
        message: String,
        snackbarDuration: SnackbarDuration = SnackbarDuration.Short,
    ) {
        if (snackbarJob == null) {
            snackbarJob = scope.launch {
                snackbarHostState.showSnackbar(message = message, duration = snackbarDuration)
                cancelJob()
            }
        } else {
            cancelJob()
            snackbarJob = scope.launch {
                snackbarHostState.showSnackbar(message = message, duration = snackbarDuration)
                cancelJob()
            }
        }
    }

    private fun cancelJob() {
        snackbarJob?.let { job ->
            job.cancel()
            snackbarJob = Job()
        }
    }
}
