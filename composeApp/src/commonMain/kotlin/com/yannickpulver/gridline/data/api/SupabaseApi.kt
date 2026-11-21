package com.yannickpulver.gridline.data.api

import co.touchlab.kermit.Logger
import com.yannickpulver.gridline.data.dto.ImageCreateDto
import com.yannickpulver.gridline.data.dto.ImageDto
import com.yannickpulver.gridline.data.dto.ImageUpdateDto
import com.yannickpulver.gridline.data.grid.GridRemote
import com.yannickpulver.gridline.data.prefs.AppPrefs
import com.yannickpulver.gridline.data.resizeImage
import com.yannickpulver.gridline.domain.model.StoredImage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid

class SupabaseApi(
    private val appPrefs: AppPrefs,
    private val client: SupabaseClient,
    private val json: Json,
    private val clock: Clock = Clock.System
) : GridRemote {

    private val realtimeChannel = client.realtime.channel("channel-${Uuid.random()}")
    private val storedImages = MutableStateFlow<List<StoredImage>>(emptyList())
    private var listening = false

    /** Uploads to the stored images only, without adding it to the grid. */
    suspend fun storeImage(bytes: ByteArray, extension: String) {
        val userId = appPrefs.getUserId() ?: error("No user id")
        val resized = resizeImage(bytes, 500, extension).takeIf { it.isNotEmpty() } ?: bytes
        putToStorage("$userId/${Uuid.random()}.jpg", resized)
        fetchStoredImages()
    }

    // Named after the grid item, so a retried upload replaces the file instead of adding another one
    override suspend fun uploadImage(userId: String, name: String, bytes: ByteArray): String =
        putToStorage("$userId/$name.jpg", bytes)

    override suspend fun insertImage(userId: String, url: String?, order: Long): ImageDto =
        client.postgrest[ENTITY_IMAGES_KEY]
            .insert(ImageCreateDto(url = url, order = order, userId = userId)) { select() }
            .decodeSingle()

    override suspend fun updateImage(id: Int, order: Long, url: String?) {
        client.postgrest[ENTITY_IMAGES_KEY].update(ImageUpdateDto(order = order, url = url)) {
            filter { eq("id", id) }
        }
    }

    override suspend fun deleteImage(id: Int) {
        // todo remove from storage aswell
        client.postgrest[ENTITY_IMAGES_KEY].delete {
            filter { eq("id", id) }
        }
    }

    suspend fun deleteFromStorage(path: String) {
        val bucket = client.storage[BUCKET_IMAGES_KEY]
        bucket.delete(path)

        storedImages.update { it.filter { it.path != path } }
    }

    private suspend fun putToStorage(name: String, bytes: ByteArray): String {
        val buckets = client.storage.listBuckets()
        if (buckets.any { it.id != BUCKET_IMAGES_KEY }) {
            client.storage.createBucket(id = BUCKET_IMAGES_KEY) {
                public = true
                fileSizeLimit = 20.megabytes
                allowedMimeTypes(ContentType.Image.JPEG, ContentType.Image.PNG)
            }
        }

        val bucket = client.storage[BUCKET_IMAGES_KEY]
        bucket.upload(name, bytes) { upsert = true }
        return bucket.publicUrl(name)
    }

    suspend fun fetchStoredImages() {
        val userId = appPrefs.getUserId() ?: error("No user id")
        val bucket = client.storage[BUCKET_IMAGES_KEY]
        storedImages.value = bucket.list(userId)
            .sortedByDescending { it.createdAt ?: kotlin.time.Instant.DISTANT_PAST }
            .map {
                StoredImage(
                    path = "$userId/${it.name}",
                    url = bucket.publicUrl("$userId/${it.name}")
                )
            }
    }

    override suspend fun fetchImages(userId: String): List<ImageDto> {
        val result = client.postgrest[ENTITY_IMAGES_KEY].select {
            filter { eq("user_id", userId) }
        }.data
        return json.decodeFromString<List<ImageDto>>(result)
    }

    fun observeStoredImages(): Flow<List<StoredImage>> {
        return storedImages
    }

    override fun listenForChanges(scope: CoroutineScope, onChange: () -> Unit) {
        if (listening) return
        listening = true
        scope.launch {
            runCatching {
                client.realtime.connect()
                realtimeChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = ENTITY_IMAGES_KEY
                }.onEach { onChange() }.launchIn(scope)
                realtimeChannel.subscribe()
            }.onFailure {
                Logger.w(it) { "Listening for grid changes failed, retrying on the next sync" }
                listening = false
            }
        }
    }

    companion object {
        private const val ENTITY_IMAGES_KEY = "images"
        private const val BUCKET_IMAGES_KEY = "images"
    }
}
