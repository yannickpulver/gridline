package com.yannickpulver.gridline.data.grid

import co.touchlab.kermit.Logger
import com.yannickpulver.gridline.data.dto.ImageDto
import com.yannickpulver.gridline.data.prefs.AppPrefs
import com.yannickpulver.gridline.data.resizeImage
import com.yannickpulver.gridline.ui.feed.Rank
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

/** The grid in Supabase, as far as this device needs it. */
interface GridRemote {
    suspend fun uploadImage(userId: String, name: String, bytes: ByteArray): String
    suspend fun insertImage(userId: String, url: String?, order: Long): ImageDto
    suspend fun updateImage(id: Int, order: Long, url: String?)
    suspend fun deleteImage(id: Int)
    suspend fun fetchImages(userId: String): List<ImageDto>

    /** Calls [onChange] whenever the grid changes in Supabase, e.g. from another device. */
    fun listenForChanges(scope: CoroutineScope, onChange: () -> Unit)
}

/**
 * The grid lives on the device: every change shows right away and is kept across restarts,
 * then a sync sends it to Supabase. Without a connection, the sync retries every [retryDelay].
 */
class GridStore(
    private val remote: GridRemote,
    private val prefs: AppPrefs,
    private val files: LocalImageFiles,
    private val beforeSync: suspend () -> Unit = {},
    private val resize: (ByteArray, String) -> ByteArray = { bytes, extension ->
        resizeImage(bytes, 500, extension).takeIf { it.isNotEmpty() } ?: bytes
    },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val retryDelay: Duration = 30.seconds,
) {
    private val all = MutableStateFlow(prefs.getGrid())
    private val syncRequests = Channel<Unit>(Channel.CONFLATED)
    private var retryJob: Job? = null

    val images: Flow<List<GridImage>> = all.map { it.visible() }

    init {
        scope.launch { all.drop(1).collect { prefs.setGrid(it) } }
        scope.launch { for (request in syncRequests) sync() }
    }

    fun requestSync() {
        syncRequests.trySend(Unit)
    }

    suspend fun add(bytes: ByteArray, extension: String) {
        val key = Uuid.random().toString()
        val file = withContext(Dispatchers.IO) { files.save("$key.jpg", resize(bytes, extension)) }
        change { it + GridImage(key = key, order = nextOrder(it), file = file) }
    }

    fun addPlaceholder() = change { it + GridImage(key = Uuid.random().toString(), order = nextOrder(it)) }

    fun delete(key: String) = change { list -> list.map { if (it.key == key) it.copy(deleted = true) else it } }

    fun exchangeUrl(key: String, url: String) {
        val replacedFile = all.value.find { it.key == key }?.file
        change { list -> list.map { if (it.key == key) it.copy(url = url, file = null, dirty = true) else it } }
        replacedFile?.let(files::delete)
    }

    /** Moves the image between [above] and [below] (keys of the images next to it, null at the edges). */
    fun move(key: String, above: String?, below: String?) = change { list ->
        val visible = list.visible()
        val rank = runCatching {
            Rank.calculateRank(
                previous = visible.find { it.key == below }?.order,
                next = visible.find { it.key == above }?.order,
            )
        }.getOrNull()

        val newOrders = if (rank != null) mapOf(key to rank) else rerank(visible, key, above, below)
        list.map { image ->
            val order = newOrders[image.key]
            if (order == null || order == image.order) image else image.copy(order = order, dirty = true)
        }
    }

    /** Forgets the grid of the account that logged out. */
    fun clear() {
        all.value = emptyList()
        files.deleteAll()
    }

    private suspend fun sync() {
        retryJob?.cancel()
        var failed = false
        try {
            beforeSync()
            val userId = prefs.getUserId() ?: return
            remote.listenForChanges(scope) { requestSync() }
            for (image in all.value.filter { it.pending }) {
                try {
                    push(userId, image)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // One image that can't be sent doesn't hold back the others
                    Logger.w(e) { "Syncing grid image ${image.key} failed" }
                    failed = true
                }
            }
            val server = remote.fetchImages(userId)
            all.update { it.mergeWith(server) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(e) { "Grid sync failed, probably offline" }
            failed = true
        }
        if (failed) {
            retryJob = scope.launch {
                delay(retryDelay)
                requestSync()
            }
        }
    }

    private suspend fun push(userId: String, image: GridImage) {
        when {
            image.deleted -> {
                image.id?.let { remote.deleteImage(it) }
                image.file?.let(files::delete)
                all.update { list -> list.filterNot { it.key == image.key } }
            }

            image.id == null -> {
                val bytes = image.file?.let(files::read)
                val url = bytes?.let { remote.uploadImage(userId, image.key, it) } ?: image.url
                val row = remote.insertImage(userId, url, image.order)
                all.update { list -> list.map { if (it.key == image.key) it.uploaded(image, row.id, url) else it } }
                image.file?.let(files::delete)
            }

            else -> {
                remote.updateImage(image.id, image.order, image.url)
                all.update { list ->
                    list.map { if (it.key == image.key && it.order == image.order && it.url == image.url) it.copy(dirty = false) else it }
                }
            }
        }
    }

    private fun change(edit: (List<GridImage>) -> List<GridImage>) {
        all.update(edit)
        requestSync()
    }

    private fun nextOrder(list: List<GridImage>): Long =
        Rank.calculateRank(previous = list.filterNot { it.deleted }.maxOfOrNull { it.order } ?: Rank.INITIAL_VALUE)

    /** No room left between two ranks: spread all images out again, with [key] at its new place. */
    private fun rerank(visible: List<GridImage>, key: String, above: String?, below: String?): Map<String, Long> {
        val moving = visible.find { it.key == key } ?: return emptyMap()
        val others = visible.filterNot { it.key == key }.toMutableList()
        val index = above?.let { a -> others.indexOfFirst { it.key == a }.takeIf { it >= 0 }?.plus(1) }
            ?: below?.let { b -> others.indexOfFirst { it.key == b }.takeIf { it >= 0 } }
            ?: others.size
        others.add(index, moving)
        return others.reversed().mapIndexed { i, image -> image.key to (i + 1) * Rank.GAP }.toMap()
    }
}

/** The image was uploaded as [sent]: changes made meanwhile stay to be sent on the next sync. */
private fun GridImage.uploaded(sent: GridImage, id: Int, url: String?): GridImage {
    val sameImage = file == sent.file
    return copy(
        id = id,
        url = if (sameImage) url else this.url,
        file = if (sameImage) null else file,
        dirty = order != sent.order || !sameImage || this.url != sent.url,
    )
}
