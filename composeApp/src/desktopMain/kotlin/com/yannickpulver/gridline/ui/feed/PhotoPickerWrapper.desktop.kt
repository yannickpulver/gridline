package com.yannickpulver.gridline.ui.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.darkrockstudios.libraries.mpfilepicker.MultipleFilePicker
import kotlinx.coroutines.launch

@Composable
actual fun PhotoPickerWrapper(
    addImages: (List<Pair<ByteArray, String>>) -> Unit,
    content: @Composable (onClick: (() -> Unit)) -> Unit
) {
    var showFilePicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val fileType = listOf("jpg", "png")
    MultipleFilePicker(show = showFilePicker, fileExtensions = fileType) { file ->
        showFilePicker = false
        scope.launch {
            val files = file.orEmpty().map {
                it.getFileByteArray() to it.path.substringAfterLast(".").ifEmpty { "jpg" }
            }
            addImages(files)
        }
        // do something with the file
    }

    content { showFilePicker = true }
}
