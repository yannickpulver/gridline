package com.yannickpulver.gridline.data.grid

import com.russhwolf.settings.MapSettings
import com.yannickpulver.gridline.data.dto.ImageDto
import com.yannickpulver.gridline.data.prefs.AppPrefs
import com.yannickpulver.gridline.ui.feed.Rank
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class GridStoreTest {

    private val remote = FakeRemote()
    private val prefs = AppPrefs(MapSettings(AppPrefs.KEY_UUID to "grid-1"))
    private val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "gridline-test-${Uuid.random()}"
    private val files = LocalImageFiles(dir)

    private fun TestScope.store() = GridStore(remote, prefs, files, resize = { bytes, _ -> bytes }, scope = backgroundScope)

    private suspend fun GridStore.shown() = images.first()

    @Test
    fun addedOfflineShowsRightAwayAndStaysAfterRestart() = runTest {
        remote.online = false
        val store = store()

        store.add(byteArrayOf(1, 2, 3), "jpg")
        runCurrent()

        val image = store.shown().single()
        assertTrue(image.pending)
        assertTrue(FileSystem.SYSTEM.exists(assertNotNull(image.file).toPath()))
        assertEquals(listOf(image), store().shown())
    }

    @Test
    fun addedOfflineIsUploadedOnceOnline() = runTest {
        remote.online = false
        val store = store()
        store.add(byteArrayOf(1, 2, 3), "jpg")
        runCurrent()
        val offline = store.shown().single()

        remote.online = true
        advanceTimeBy(31.seconds)
        runCurrent()

        val synced = store.shown().single()
        assertEquals(offline.key, synced.key)
        assertFalse(synced.pending)
        assertNull(synced.file)
        assertEquals("https://storage/grid-1/${offline.key}.jpg", synced.url)
        assertEquals(listOf(byteArrayOf(1, 2, 3).toList()), remote.uploads.map { it.toList() })
        assertEquals(offline.order, remote.rows.single().order)
        assertFalse(FileSystem.SYSTEM.exists(offline.file!!.toPath()))
    }

    @Test
    fun movedOfflineIsSentOnceOnline() = runTest {
        remote.rows += listOf(row(1, order = 3 * Rank.GAP), row(2, order = 2 * Rank.GAP), row(3, order = Rank.GAP))
        val store = store()
        store.requestSync()
        runCurrent()
        assertEquals(listOf("1", "2", "3"), store.shown().map { it.key })

        remote.online = false
        // Drag the top image to the bottom
        store.move("1", above = "3", below = null)
        runCurrent()
        assertEquals(listOf("2", "3", "1"), store.shown().map { it.key })

        remote.online = true
        store.requestSync()
        runCurrent()

        assertEquals(listOf("2", "3", "1"), store.shown().map { it.key })
        assertFalse(store.shown().any { it.pending })
        assertEquals(listOf(2, 3, 1), remote.rows.sortedByDescending { it.order }.map { it.id })
    }

    @Test
    fun deletedOfflineIsRemovedOnceOnline() = runTest {
        remote.rows += row(1)
        val store = store()
        store.requestSync()
        runCurrent()

        remote.online = false
        store.delete("1")
        runCurrent()
        assertEquals(emptyList(), store.shown())

        remote.online = true
        store.requestSync()
        runCurrent()
        assertEquals(emptyList(), remote.rows)
    }

    @Test
    fun movesWithoutRoomBetweenRanksSpreadTheGridOut() = runTest {
        remote.rows += listOf(row(1, order = 11), row(2, order = 10), row(3, order = 1))
        val store = store()
        store.requestSync()
        runCurrent()

        store.move("3", above = "1", below = "2")
        runCurrent()

        assertEquals(listOf("1", "3", "2"), store.shown().map { it.key })
    }

    @Test
    fun mergeKeepsLocalChangesAndDropsImagesDeletedElsewhere() {
        val local = listOf(
            GridImage(key = "a", order = 5, id = 1, url = "a"),
            GridImage(key = "b", order = 9, id = 2, url = "b", dirty = true),
            GridImage(key = "c", order = 1, id = 3, url = "c"),
            GridImage(key = "new", order = 7, file = "/new.jpg"),
        )
        val server = listOf(row(1, order = 6), row(2, order = 2), row(4, order = 3))

        val merged = local.mergeWith(server).associateBy { it.key }

        assertEquals(6, merged.getValue("a").order) // changed elsewhere
        assertEquals(9, merged.getValue("b").order) // changed here, not sent yet
        assertNull(merged["c"]) // deleted elsewhere
        assertEquals(3, merged.getValue("4").order) // added elsewhere
        assertNotNull(merged["new"])
    }

    private fun row(id: Int, order: Long = id * Rank.GAP) = ImageDto(url = "url-$id", order = order, id = id, userId = "grid-1")

    private class FakeRemote : GridRemote {
        var online = true
        val rows = mutableListOf<ImageDto>()
        val uploads = mutableListOf<ByteArray>()

        private fun checkOnline() = check(online) { "offline" }

        override suspend fun uploadImage(userId: String, name: String, bytes: ByteArray): String {
            checkOnline()
            uploads += bytes
            return "https://storage/$userId/$name.jpg"
        }

        override suspend fun insertImage(userId: String, url: String?, order: Long): ImageDto {
            checkOnline()
            return ImageDto(url = url, order = order, id = (rows.maxOfOrNull { it.id } ?: 0) + 1, userId = userId)
                .also { rows += it }
        }

        override suspend fun updateImage(id: Int, order: Long, url: String?) {
            checkOnline()
            rows.replaceAll { if (it.id == id) it.copy(order = order, url = url) else it }
        }

        override suspend fun deleteImage(id: Int) {
            checkOnline()
            rows.removeAll { it.id == id }
        }

        override suspend fun fetchImages(userId: String): List<ImageDto> {
            checkOnline()
            return rows.toList()
        }

        override fun listenForChanges(scope: CoroutineScope, onChange: () -> Unit) = Unit
    }
}
