package com.yannickpulver.gridline.ui.feed

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.yannickpulver.gridline.ui.auth.AuthContent
import com.yannickpulver.gridline.ui.theme.AppTheme

@Preview
@Composable
private fun AuthScreenPreview() {
    AppTheme(useDarkTheme = false, dynamicColor = true) {
        AuthContent(addUserName = { _, _ -> })
    }
}