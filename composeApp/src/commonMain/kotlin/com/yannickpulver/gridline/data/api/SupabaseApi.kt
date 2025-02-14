package com.yannickpulver.gridline.data.api

import co.touchlab.kermit.Logger
import com.yannickpulver.gridline.data.dto.ImageCreateDto
import com.yannickpulver.gridline.data.dto.ImageDto
import com.yannickpulver.gridline.data.dto.ImageOrderUpdateDto
import com.yannickpulver.gridline.data.dto.ImageUrlUpdateDto
import com.yannickpulver.gridline.data.prefs.AppPrefs
import com.yannickpulver.gridline.data.resizeImage
import com.yannickpulver.gridline.domain.model.StoredImage
import com.yannickpulver.gridline.ui.feed.Rank
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
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
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid

class SupabaseApi(
    private val appPrefs: AppPrefs,
    private val client: SupabaseClient,
    private val json: Json,
    private val clock: Clock.System = Clock.System
) {

    private val realtimeChannel = client.realtime.channel("channel-${Uuid.random()}")
    private val imageFlow = MutableStateFlow<List<ImageDto>>(emptyList())
    private val storedImages = MutableStateFlow<List<StoredImage>>(emptyList())

    suspend fun putImage(bytes: ByteArray, extension: String, storageOnly: Boolean = false) {
        val userId = appPrefs.getUserId() ?: error("No user id")

        val highestRank = getHighestRank()

        var resizedBytes = resizeImage(bytes, 500, extension)

        if (resizedBytes.isEmpty()) {
            resizedBytes = bytes
        }
        Logger.i { "Resized image size: ${resizedBytes.size}, old size: ${bytes.size}" }

        val name = "${Uuid.random()}.jpg" // TODO fix?
        val bucketUrl = putToStorage("$userId/$name", resizedBytes)
        if (storageOnly.not()) {
            client.postgrest[ENTITY_IMAGES_KEY].insert(
                ImageCreateDto(
                    url = bucketUrl,
                    order = Rank.calculateRank(previous = highestRank),
                    userId = userId
                )
            )
        }
        fetchStoredImages()
    }

    suspend fun putPlaceholder() {
        val highestRank = getHighestRank()
        val userId = appPrefs.getUserId() ?: error("No user id")
        client.postgrest[ENTITY_IMAGES_KEY].insert(
            ImageCreateDto(
                url = null,
                order = Rank.calculateRank(previous = highestRank),
                userId = userId
            )
        )
    }

    suspend fun updateImageOrders(images: List<ImageOrderUpdateDto>) {
        client.postgrest[ENTITY_IMAGES_KEY].upsert(values = images)
    }

    suspend fun updateImageOrder(image: ImageOrderUpdateDto) {
        client.postgrest[ENTITY_IMAGES_KEY].upsert(value = image)
    }

    suspend fun deleteImage(id: Int) {
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
        val buckets = client.storage.retrieveBuckets()
        if (buckets.any { it.id != BUCKET_IMAGES_KEY }) {
            client.storage.createBucket(id = BUCKET_IMAGES_KEY) {
                public = true
                fileSizeLimit = 20.megabytes
                allowedMimeTypes(ContentType.Image.JPEG, ContentType.Image.PNG)
            }
        }

        val bucket = client.storage[BUCKET_IMAGES_KEY]
        bucket.upload(name, bytes)
        return bucket.publicUrl(name)
    }

    suspend fun fetchStoredImages() {
        val userId = appPrefs.getUserId() ?: error("No user id")
        val bucket = client.storage[BUCKET_IMAGES_KEY]
        storedImages.value = bucket.list(userId).map {
            StoredImage(
                path = "$userId/${it.name}",
                url = bucket.publicUrl("$userId/${it.name}")
            )
        }
    }

    suspend fun exchangeImageUrl(id: Int, url: String) {
        client.postgrest[ENTITY_IMAGES_KEY].upsert(
            value = ImageUrlUpdateDto(id, url)
        )
    }

    suspend fun fetchImages(): List<ImageDto> {
        val userId = appPrefs.getUserId() ?: return emptyList()

        val result =
            client.postgrest[ENTITY_IMAGES_KEY].select {
                filter {
                    eq("user_id", userId)
                }
            }.data
        val items = json.decodeFromString<List<ImageDto>>(result)
        val sortedImages = items.sortedByDescending { it.order }
        imageFlow.emit(sortedImages)
        return sortedImages
    }


    suspend fun getHighestRank(): Long {
        val isConnected = client.realtime.status.value == Realtime.Status.CONNECTED

        val result = if (isConnected) {
            val images = imageFlow.value
            images.ifEmpty { fetchImages() }
        } else {
            fetchImages()
        }

        val highestRank = result.maxOfOrNull { it.order } ?: Rank.INITIAL_VALUE
        return highestRank
    }

    fun observeStoredImages(): Flow<List<StoredImage>> {
        return storedImages
    }

    fun observeImages(scope: CoroutineScope): Flow<List<ImageDto>> {
        scope.launch {
            runCatching {
                client.realtime.connect()

                val changeFlow: Flow<PostgresAction> =
                    realtimeChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                        table = ENTITY_IMAGES_KEY
                    }

                changeFlow
                    .onEach {
                        this@SupabaseApi.fetchImages()
                    }.launchIn(scope)

                runCatching {
                    realtimeChannel.subscribe()
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
        return imageFlow
    }

    companion object {
        private const val ENTITY_IMAGES_KEY = "images"
        private const val BUCKET_IMAGES_KEY = "images"
    }
}
