package com.yannickpulver.gridline.ui.feed

import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnResume
import com.yannickpulver.gridline.data.api.InstaApi
import com.yannickpulver.gridline.data.api.InstaLoginExpiredException
import com.yannickpulver.gridline.data.api.SupabaseApi
import com.yannickpulver.gridline.data.dto.InstaAccountDto
import com.yannickpulver.gridline.data.grid.GridImage
import com.yannickpulver.gridline.data.grid.GridStore
import com.yannickpulver.gridline.data.observeSharedImages
import com.yannickpulver.gridline.data.prefs.AppPrefs
import com.yannickpulver.gridline.ui.feed.model.DisplayItem
import com.yannickpulver.gridline.ui.snackbar.SnackbarStateHolder
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FeedComponent(componentContext: ComponentContext, private val onReset: () -> Unit) :
    KoinComponent,
    ComponentContext by componentContext {
    private val instaApi: InstaApi by inject()
    private val supabaseApi: SupabaseApi by inject()
    private val gridStore: GridStore by inject()
    private val prefs: AppPrefs by inject()

    private val _showBorders = MutableStateFlow(prefs.showBorders())
    private val _instaPinnedFeed = MutableStateFlow(emptyList<DisplayItem.InstaItem>())
    private val _instaFeed = MutableStateFlow(emptyList<DisplayItem.InstaItem>())
    private val _pinnedLinks = MutableStateFlow<List<String>?>(null)
    private val _account = MutableStateFlow<InstaAccountDto?>(null)

    /** Links of the pinned posts while their dialog is open, null when it's closed. */
    val pinnedLinks: StateFlow<List<String>?> = _pinnedLinks

    val state: StateFlow<FeedViewState> =
        combine(
            _instaFeed,
            gridStore.images.map { it.asItems() },
            _instaPinnedFeed,
            supabaseApi.observeStoredImages(),
            _showBorders,
        ) { insta, supabaseFeed, instaPinned, storedImages, showBorders ->
            FeedViewState(
                data = instaPinned + supabaseFeed + insta,
                storedImages = storedImages,
                showBorders = showBorders
            )
        }.combine(_account) { state, account -> state.copy(account = account) }.stateIn(
            scope = CoroutineScope(Dispatchers.Default),
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FeedViewState(),
        )

    // A failed request (e.g. offline) shows a message instead of crashing the app or stopping later requests
    private val ioScope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, error ->
            Logger.e(error) { "Feed request failed" }
            CoroutineScope(Dispatchers.Default).launch {
                SnackbarStateHolder.error("Couldn't reach Gridline, please check your connection")
            }
        }
    )

    private var refreshJob: Job? = null

    init {
        ioScope.launch {
            observeSharedImages().collect { images ->
                addImages(images)
            }
        }

        // Fires on first start and every time the app comes back to the foreground
        lifecycle.doOnResume { refresh() }
    }

    private fun refresh() {
        refreshJob?.cancel()
        refreshJob = ioScope.launch {
            launch { instaApi.getAccount().collect { _account.value = it } }
            gridStore.requestSync()

            instaApi.getFeed()
                .catch { if (it is InstaLoginExpiredException) logOutExpired() else throw it }
                .collect { posts ->
                    if (posts.isEmpty()) return@collect
                    val (pinned, latest) = posts.partition { it.pinned }
                    _instaPinnedFeed.update { pinned }
                    _instaFeed.update { latest }
                }
        }
    }

    fun editPinnedPosts() {
        _pinnedLinks.value = prefs.getPinnedPosts().map { "https://www.instagram.com/p/${it.shortcode}/" }
    }

    fun dismissPinnedPosts() {
        _pinnedLinks.value = null
    }

    fun savePinnedPosts(links: List<String>) {
        _pinnedLinks.value = null
        val entered = links.filter { it.isNotBlank() }
        val shortcodes = entered.mapNotNull(::shortcodeOf).distinct().take(MAX_PINNED_POSTS)
        ioScope.launch {
            if (shortcodes.isNotEmpty()) SnackbarStateHolder.success("Looking up pinned posts...")
            val posts = instaApi.findPosts(shortcodes)
            if (posts.size < entered.size) {
                SnackbarStateHolder.error("Couldn't find ${entered.size - posts.size} of the posts on your account")
            }
            prefs.setPinnedPosts(posts)
            refresh()
        }
    }

    private suspend fun logOutExpired() = withContext(Dispatchers.Main) {
        SnackbarStateHolder.error("Instagram login expired, please log in again")
        reset()
    }

    fun fetchPossibleImages() {
        ioScope.launch {
            supabaseApi.fetchStoredImages()
        }
    }

    /** [previousItem] and [nextItem] are the items above and below [displayItem] after the move. */
    fun onMove(displayItem: DisplayItem, previousItem: DisplayItem?, nextItem: DisplayItem?) {
        if (displayItem !is DisplayItem.SupabaseItem) return
        gridStore.move(
            key = displayItem.key,
            above = (previousItem as? DisplayItem.SupabaseItem)?.key,
            below = (nextItem as? DisplayItem.SupabaseItem)?.key,
        )
    }

    fun addPlaceholder() = gridStore.addPlaceholder()

    fun onDelete(item: DisplayItem) {
        if (item is DisplayItem.SupabaseItem) gridStore.delete(item.key)
    }

    fun hideImage(item: DisplayItem.InstaItem) {
        _instaFeed.update { it.minus(item) }
    }

    fun storeImages(bytes: List<Pair<ByteArray, String>>) {
        bytes.forEach { (image, extension) ->
            ioScope.launch {
                SnackbarStateHolder.success("Uploading...")
                supabaseApi.storeImage(image, extension)
                SnackbarStateHolder.success("Success!")
            }
        }
    }

    // Shows up right away, also offline, and uploads in the background
    fun addImages(bytes: List<Pair<ByteArray, String>>) {
        ioScope.launch {
            bytes.forEach { (image, extension) -> gridStore.add(image, extension) }
        }
    }

    fun exchangeImage(value: DisplayItem.SupabaseItem?, url: String) {
        value?.let { gridStore.exchangeUrl(it.key, url) }
    }

    fun toggleBorders() {
        val newValue = !_showBorders.value
        prefs.toggleBorders(newValue)
        _showBorders.update { newValue }
    }

    fun removeImage(path: String) {
        ioScope.launch {
            supabaseApi.deleteFromStorage(path)
        }
    }

    fun reset() {
        prefs.clearUserInfo()
        gridStore.clear()
        onReset()
    }
}

private fun List<GridImage>.asItems(): List<DisplayItem.SupabaseItem> = map {
    DisplayItem.SupabaseItem(key = it.key, url = it.url, order = it.order, file = it.file, pending = it.pending)
}

const val MAX_PINNED_POSTS = 3

/** "https://www.instagram.com/p/ABC123/?igsh=…" or just "ABC123" → "ABC123" */
internal fun shortcodeOf(link: String): String? {
    val trimmed = link.trim()
    Regex("""/(?:p|reels?|tv)/([\w-]+)""").find(trimmed)?.let { return it.groupValues[1] }
    return trimmed.takeIf { it.matches(Regex("""[\w-]+""")) }
}
