package com.yannickpulver.gridline.ui.feed.model

import kotlinx.serialization.Serializable

@Serializable
sealed class DisplayItem(val id: String) {

    /** Instagram signs [url] anew on every fetch, [mediaId] stays the same (falls back to the url for older caches). */
    @Serializable
    data class InstaItem(
        val url: String,
        val publishedAt: Long?,
        val mediaId: String = url,
        val pinned: Boolean = false,
    ) : DisplayItem(mediaId)

    /** A planned image; [file] is set while it only exists on this device, [pending] until Supabase has it. */
    @Serializable
    data class SupabaseItem(
        val key: String,
        val url: String?,
        val order: Long,
        val file: String? = null,
        val pending: Boolean = false,
    ) : DisplayItem(key) {
        // The order is left out: it changes on every move, the item stays the same
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SupabaseItem) return false
            return key == other.key && url == other.url && file == other.file && pending == other.pending
        }

        override fun hashCode(): Int {
            var result = key.hashCode()
            result = 31 * result + url.hashCode()
            result = 31 * result + file.hashCode()
            result = 31 * result + pending.hashCode()
            return result
        }
    }
}
