package com.yannickpulver.gridline.ui.feed

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun PhotoPickerWrapper(
    addImages: (List<Pair<ByteArray, String>>) -> Unit,
    content: @Composable (onClick: (() -> Unit)) -> Unit
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            val byteArrays = uris.mapNotNull { uri ->
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?.let { it to uri.path?.substringAfterLast(".").orEmpty().ifEmpty { "jpg" } }
            }
            addImages(byteArrays)
        }
    )

    content { launcher.launch("image/*") }
}
