package com.yannickpulver.gridline

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.core.net.toUri
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
            CompositionLocalProvider(LocalUriHandler provides CustomTabUriHandler(this)) {
                App(rootComponent = root)
            }
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

// Links (like the Instagram login) open in a Custom Tab, so they stay in a browser instead of jumping to the Instagram app
private class CustomTabUriHandler(private val context: Context) : UriHandler {
    override fun openUri(uri: String) {
        CustomTabsIntent.Builder().build().launchUrl(context, uri.toUri())
    }
}
