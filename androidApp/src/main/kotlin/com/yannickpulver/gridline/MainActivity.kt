package com.yannickpulver.gridline

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.retainedComponent
import com.yannickpulver.gridline.data.PendingShareHolder
import com.yannickpulver.gridline.ui.navigation.RootComponent
import com.yannickpulver.gridline.update.InAppUpdate

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalDecomposeApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val root = retainedComponent { RootComponent(it) }

        handleShareIntent(intent)
        InAppUpdate(this)
        val uriHandler = CustomTabUriHandler(this)

        setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
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

// Links (like the Instagram login) open in a Custom Tab. With a session, Chrome keeps instagram.com in the tab
// instead of handing it to the Instagram app.
private class CustomTabUriHandler(private val activity: ComponentActivity) : UriHandler, DefaultLifecycleObserver {
    private var session: CustomTabsSession? = null
    private val connection = object : CustomTabsServiceConnection() {
        override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
            client.warmup(0)
            session = client.newSession(null)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            session = null
        }
    }
    private val bound = CustomTabsClient.getPackageName(activity, null)
        ?.let { CustomTabsClient.bindCustomTabsService(activity, it, connection) } == true

    init {
        activity.lifecycle.addObserver(this)
    }

    override fun openUri(uri: String) {
        val builder = session?.let { CustomTabsIntent.Builder(it) } ?: CustomTabsIntent.Builder()
        builder.build().launchUrl(activity, uri.toUri())
    }

    override fun onDestroy(owner: LifecycleOwner) {
        if (bound) activity.unbindService(connection)
    }
}
