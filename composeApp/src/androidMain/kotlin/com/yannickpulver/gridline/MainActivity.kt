package com.yannickpulver.gridline

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.retainedComponent
import com.yannickpulver.gridline.data.PendingShareHolder
import com.yannickpulver.gridline.ui.navigation.RootComponent

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalDecomposeApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val root = retainedComponent { RootComponent(it) }

        handleShareIntent(intent)

        setContent {
            App(rootComponent = root)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uri ->
                    extractImage(uri)?.let { image ->
                        PendingShareHolder.send(listOf(image))
                    }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris ->
                    val images = uris.mapNotNull { extractImage(it) }
                    if (images.isNotEmpty()) {
                        PendingShareHolder.send(images)
                    }
                }
            }
        }
    }

    private fun extractImage(uri: Uri): Pair<ByteArray, String>? {
        return try {
            val mimeType = contentResolver.getType(uri)
            val extension = when {
                mimeType?.contains("png") == true -> "png"
                mimeType?.contains("webp") == true -> "webp"
                mimeType?.contains("gif") == true -> "gif"
                else -> "jpg"
            }
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            bytes?.let { it to extension }
        } catch (e: Exception) {
            null
        }
    }
}
