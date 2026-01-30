package com.yannickpulver.gridline.ui.feed

import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import com.yannickpulver.gridline.data.api.InstaApi
import com.yannickpulver.gridline.data.api.SupabaseApi
import com.yannickpulver.gridline.data.dto.ImageDto
import com.yannickpulver.gridline.data.observeSharedImages
import com.yannickpulver.gridline.data.dto.ImageOrderUpdateDto
import com.yannickpulver.gridline.data.prefs.AppPrefs
import com.yannickpulver.gridline.ui.feed.model.DisplayItem
import com.yannickpulver.gridline.ui.snackbar.SnackbarStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FeedComponent(componentContext: ComponentContext, private val onReset: () -> Unit) :
    KoinComponent,
    ComponentContext by componentContext {
    private val instaApi: InstaApi by inject()
    private val supabaseApi: SupabaseApi by inject()
    private val prefs: AppPrefs by inject()

    private val _showBorders = MutableStateFlow(prefs.showBorders())
    private val _instaPinnedFeed = MutableStateFlow(emptyList<DisplayItem.InstaItem>())
    private val _instaFeed = MutableStateFlow(emptyList<DisplayItem.InstaItem>())
    private val _supaFeed = MutableStateFlow(emptyList<DisplayItem.SupabaseItem>())

    val state: StateFlow<FeedViewState> =
        combine(
            _instaFeed,
            _supaFeed,
            _instaPinnedFeed,
            supabaseApi.observeStoredImages(),
            _showBorders,
        ) { insta, supabaseFeed, instaPinned, storedImages, showBorders ->
            FeedViewState(
                data = instaPinned + supabaseFeed + insta,
                storedImages = storedImages,
                uuid = prefs.getUuid(),
                showBorders = showBorders
            )
        }.stateIn(
            scope = CoroutineScope(Dispatchers.Default),
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FeedViewState(),
        )

    private val ioScope = CoroutineScope(Dispatchers.IO)

    init {

        ioScope.launch {
            _supaFeed.update { supabaseApi.fetchImages().asItems() }
        }

        ioScope.launch {
            supabaseApi.observeImages(ioScope).map { it.asItems() }.collect { images ->
                _supaFeed.update { images }
            }
        }

        ioScope.launch {
            instaApi.getFeed().collect { posts ->
                if (posts.isEmpty()) return@collect
                val latestIndex = posts.withIndex().maxBy { it.value.publishedAt ?: 0 }.index

                posts.withIndex().partition { it.index < latestIndex }.let { (pinned, posts) ->
                    _instaPinnedFeed.update { pinned.map { it.value } }
                    _instaFeed.update { posts.map { it.value } }
                }
            }
        }

        ioScope.launch {
            observeSharedImages().collect { images ->
                addImages(images)
            }
        }
    }

    fun fetchPossibleImages() {
        ioScope.launch {
            supabaseApi.fetchStoredImages()
        }
    }

    // Convert to event (so that it gets properly handled)
    fun onMove(displayItem: DisplayItem, previousItem: DisplayItem?, nextItem: DisplayItem?) {
        val nextRank = (previousItem as? DisplayItem.SupabaseItem) // flipped on purpose
        val previousRank = (nextItem as? DisplayItem.SupabaseItem)


        if (displayItem is DisplayItem.SupabaseItem) {
            val rank = runCatching {
                Rank.calculateRank(previous = previousRank?.order, next = nextRank?.order)
            }

            rank.fold(
                onSuccess = {
                    Logger.i {  "Ranking single item: ${displayItem.supaId} to $it" }
                    ioScope.launch {
                        supabaseApi.updateImageOrder(ImageOrderUpdateDto(displayItem.supaId, it))
                    }
                },
                onFailure = {
                    Logger.i { "Ranking single item failed: ${it.message}, retrying with full list" }
                    ioScope.launch {
                        val images = supabaseApi.fetchImages()

                        val updatedImages = images.toMutableList()
                        val currentIndex =
                            updatedImages.indexOfFirst { it.id == displayItem.supaId }
                        if (currentIndex != -1) {
                            updatedImages.removeAt(currentIndex)
                            val targetIndex = when {
                                previousItem is DisplayItem.SupabaseItem -> {
                                    val index =
                                        updatedImages.indexOfFirst { it.id == previousItem.supaId }
                                    if (index == -1) updatedImages.size else index + 1
                                }

                                nextItem is DisplayItem.SupabaseItem -> {
                                    val index =
                                        updatedImages.indexOfFirst { it.id == nextItem.supaId }
                                    if (index == -1) 0 else index
                                }

                                else -> updatedImages.size
                            }
                            updatedImages.add(targetIndex, images[currentIndex])
                        }


                        rerank(updatedImages)
                            .map { ImageOrderUpdateDto(id = it.id, order = it.order) }
                            .let { supabaseApi.updateImageOrders(it) }
                    }
                }
            )

        }
    }

    private fun rerank(images: List<ImageDto>): List<ImageDto> {
        return images.reversed()
            .mapIndexed { index, image ->
                image.copy(order = (index + 1) * Rank.GAP)
            }
    }

    fun addPlaceholder() {
        ioScope.launch {
            SnackbarStateHolder.success("Adding placeholder...")
            supabaseApi.putPlaceholder()
            SnackbarStateHolder.success("Success!")
        }
    }

    fun onDelete(item: DisplayItem) {
        when (item) {
            is DisplayItem.SupabaseItem -> {
                ioScope.launch {
                    supabaseApi.deleteImage(item.supaId)
                    supabaseApi.fetchImages()
                }
            }

            else -> Unit
        }
    }

    fun hideImage(item: DisplayItem.InstaItem) {
        _instaFeed.update { it.minus(item) }
    }

    fun storeImages(bytes: List<Pair<ByteArray, String>>) {
        bytes.forEach { addImage(it.first, it.second, storageOnly = true) }
    }

    fun addImages(bytes: List<Pair<ByteArray, String>>) {
        bytes.forEach { addImage(it.first, it.second, storageOnly = false) }
    }

    fun addImage(bytes: ByteArray, extension: String, storageOnly: Boolean) {
        ioScope.launch {
            SnackbarStateHolder.success("Uploading...")
            supabaseApi.putImage(bytes = bytes, storageOnly = storageOnly, extension = extension)
            SnackbarStateHolder.success("Success!")
        }
    }

    fun exchangeImage(value: DisplayItem.SupabaseItem?, url: String) {
        ioScope.launch {
            value?.let {
                supabaseApi.exchangeImageUrl(it.supaId, url)
            }
        }
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
        onReset()
    }
}

private fun List<ImageDto>.asItems(): List<DisplayItem.SupabaseItem> {
    return map {
        DisplayItem.SupabaseItem(
            url = it.url,
            supaId = it.id,
            order = it.order
        )
    }
}
