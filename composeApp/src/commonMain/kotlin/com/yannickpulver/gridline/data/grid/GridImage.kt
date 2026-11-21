package com.yannickpulver.gridline.data.grid

import com.yannickpulver.gridline.data.dto.ImageDto
import kotlinx.serialization.Serializable

/**
 * One planned image as this device knows it. Changes land here first and reach Supabase on the next sync,
 * so the grid works offline.
 */
@Serializable
data class GridImage(
    /** Stays the same for the item's whole life, also after the upload gives it an [id]. */
    val key: String,
    val order: Long,
    /** Row id in Supabase, null until the image is uploaded. */
    val id: Int? = null,
    val url: String? = null,
    /** Image on this device that still has to be uploaded. */
    val file: String? = null,
    /** Order or url changed on this device and isn't in Supabase yet. */
    val dirty: Boolean = false,
    /** Deleted on this device, removed from Supabase on the next sync. */
    val deleted: Boolean = false,
) {
    val pending: Boolean get() = id == null || dirty || deleted
}

/** What the grid shows: newest (highest order) first. */
fun List<GridImage>.visible(): List<GridImage> = filterNot { it.deleted }.sortedByDescending { it.order }

/**
 * Supabase is right for everything this device hasn't changed. Rows missing in Supabase were deleted elsewhere,
 * images that were never uploaded stay.
 */
fun List<GridImage>.mergeWith(server: List<ImageDto>): List<GridImage> {
    val byId = filter { it.id != null }.associateBy { it.id }
    val fromServer = server.map { row ->
        val mine = byId[row.id]
        when {
            mine == null -> GridImage(key = row.id.toString(), order = row.order, id = row.id, url = row.url)
            mine.pending -> mine
            else -> mine.copy(order = row.order, url = row.url)
        }
    }
    return fromServer + filter { it.id == null }
}
