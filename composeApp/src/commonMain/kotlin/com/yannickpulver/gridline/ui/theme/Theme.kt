package com.yannickpulver.gridline.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
val blueContainer = Color(0xFF7292b0)
val blue = Color(0xFF2e75f0)

val LightColorScheme = lightColorScheme(
    primary = blue,
    onPrimary = Color.White,
    primaryContainer = blueContainer,
    onPrimaryContainer = Color.White,
    secondary = Color.Black,
    onSecondary = Color.Gray,
    background = Color.White,
    onBackground = Color.DarkGray,
    surface = Color.White,
    onSurface = Color.Black,
)

val DarkColorScheme = darkColorScheme(
    primary = blue,
    onPrimary = Color.White,
    primaryContainer = blueContainer,
    onPrimaryContainer = Color.White,
    secondary = Color.DarkGray,
    onSecondary = Color.White,
    background = Color.Black,
    onBackground = Color.Gray,
    surface = Color.Black,
    onSurface = Color.White,
)


