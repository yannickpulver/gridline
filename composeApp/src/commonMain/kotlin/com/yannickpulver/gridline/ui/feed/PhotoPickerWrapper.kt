package com.yannickpulver.gridline.ui.feed

import androidx.compose.runtime.Composable

@Composable
expect fun PhotoPickerWrapper(addImages: (List<Pair<ByteArray, String>>) -> Unit, content: @Composable (onClick: (() -> Unit)) -> Unit)
