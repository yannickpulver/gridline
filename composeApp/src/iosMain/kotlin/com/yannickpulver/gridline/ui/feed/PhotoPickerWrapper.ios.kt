package com.yannickpulver.gridline.ui.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.preat.peekaboo.image.picker.ResizeOptions
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher

@Composable
actual fun PhotoPickerWrapper(addImages: (List<ByteArray>) -> Unit, content: @Composable (onClick: (() -> Unit)) -> Unit) {
    val scope = rememberCoroutineScope()

    val multipleImagePicker = rememberImagePickerLauncher(
        // Optional: Set a maximum selection limit, e.g., SelectionMode.Multiple(maxSelection = 5).
        // Default: No limit, depends on system's maximum capacity.
        selectionMode = SelectionMode.Multiple(),
        scope = scope,
        onResult = addImages,
        resizeOptions = ResizeOptions(500, 500)
    )

    content { multipleImagePicker.launch() }
}
