package com.yannickpulver.gridline.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import gridline.composeapp.generated.resources.Res
import gridline.composeapp.generated.resources.inter_bold
import gridline.composeapp.generated.resources.inter_medium
import gridline.composeapp.generated.resources.inter_regular
import gridline.composeapp.generated.resources.inter_semibold
import org.jetbrains.compose.resources.Font

@Composable
fun InterFontFamily() = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
    Font(Res.font.inter_bold, FontWeight.Bold),
)

@Composable
fun AppTypography(): Typography {
    val inter = InterFontFamily()
    val defaults = Typography()
    return defaults.copy(
        displayLarge = defaults.displayLarge.copy(fontFamily = inter),
        displayMedium = defaults.displayMedium.copy(fontFamily = inter),
        displaySmall = defaults.displaySmall.copy(fontFamily = inter),
        headlineLarge = defaults.headlineLarge.copy(fontFamily = inter),
        headlineMedium = defaults.headlineMedium.copy(fontFamily = inter),
        headlineSmall = defaults.headlineSmall.copy(fontFamily = inter),
        titleLarge = defaults.titleLarge.copy(fontFamily = inter),
        titleMedium = defaults.titleMedium.copy(fontFamily = inter),
        titleSmall = defaults.titleSmall.copy(fontFamily = inter),
        bodyLarge = defaults.bodyLarge.copy(fontFamily = inter),
        bodyMedium = defaults.bodyMedium.copy(fontFamily = inter),
        bodySmall = defaults.bodySmall.copy(fontFamily = inter),
        labelLarge = defaults.labelLarge.copy(fontFamily = inter),
        labelMedium = defaults.labelMedium.copy(fontFamily = inter),
        labelSmall = defaults.labelSmall.copy(fontFamily = inter),
    )
}
