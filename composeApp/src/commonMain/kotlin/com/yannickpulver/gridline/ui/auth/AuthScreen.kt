package com.yannickpulver.gridline.ui.auth

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import gridline.composeapp.generated.resources.Res
import gridline.composeapp.generated.resources.gridline
import gridline.composeapp.generated.resources.gridline_dark
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@Composable
fun AuthScreen(component: AuthComponent) {
    AuthContent(addUserName = component::setUsername)
}

@Composable
@OptIn(ExperimentalResourceApi::class)
fun AuthContent(addUserName: (String, String) -> Unit) {
    val userName = remember { mutableStateOf("") }
    val uuid = remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).imePadding().padding(vertical = 24.dp, horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Spacer(Modifier.weight(1f))

        // Creates an [InfiniteTransition] instance for managing child animations.
        val infiniteTransition = rememberInfiniteTransition()

        // Creates a child animation of float type as a part of the [InfiniteTransition].
        val rotate by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 90f,
            animationSpec = infiniteRepeatable(
                // Infinitely repeating a 1000ms tween animation using default easing curve.
                animation = tween(durationMillis = 1000, delayMillis = 5000),
                initialStartOffset = StartOffset(5000),
                // After each iteration of the animation (i.e. every 1000ms), the animation will
                // start again from the [initialValue] defined above.
                // This is the default [RepeatMode]. See [RepeatMode.Reverse] below for an
                // alternative.
                repeatMode = RepeatMode.Restart
            )
        )



        Image(
            painter = painterResource(if (isSystemInDarkTheme()) Res.drawable.gridline else Res.drawable.gridline_dark),
            contentDescription = "Gridline Logo",
            modifier = Modifier.size(124.dp).clip(RoundedCornerShape(20.dp)).rotate(rotate),
        )

        Text(
            "Welcome to Gridline 👋",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "Simply enter your Instagram username\nand we are good to go.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(0.5f))

        OutlinedTextField(
            value = userName.value,
            onValueChange = { userName.value = it },
            label = { Text("Instagram Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "If you already have a Gridline ID, enter it here. If you're starting fresh, you can skip this.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            OutlinedTextField(
                value = uuid.value,
                onValueChange = { uuid.value = it },
                label = { Text("Optional: Gridline ID") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.weight(1f))

        Button(
            onClick = { addUserName(userName.value, uuid.value) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = userName.value.isNotBlank()
        ) {
            Text("Open Grid")
        }
    }
}
