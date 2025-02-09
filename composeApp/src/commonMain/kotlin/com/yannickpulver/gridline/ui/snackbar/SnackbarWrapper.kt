package com.yannickpulver.gridline.ui.snackbar

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SnackbarWrapper(content: @Composable () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarController = remember { SnackbarController(coroutineScope, snackbarHostState) }

    LaunchedEffect(Unit) {
        SnackbarStateHolder.snackbarState.collect { state ->
            snackbarController.showSnackbar(state.message)
        }
    }

    Scaffold(snackbarHost = {
        SnackbarHost(
            hostState = snackbarHostState,
            snackbar = { data -> TinySnackbar(data.visuals.message) },
            modifier = Modifier.navigationBarsPadding().padding(bottom = 24.dp)
        )
    }) {
        content()
    }
}
