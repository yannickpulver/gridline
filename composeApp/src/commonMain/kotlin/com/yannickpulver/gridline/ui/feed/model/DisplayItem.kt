package com.yannickpulver.gridline.ui.feed.model

import kotlinx.serialization.Serializable

@Serializable
sealed class DisplayItem(val id: String) {

    /** Instagram signs [url] anew on every fetch, [mediaId] stays the same (falls back to the url for older caches). */
    @Serializable
    data class InstaItem(val url: String, val publishedAt: Long?, val mediaId: String = url) : DisplayItem(mediaId)

    @Serializable
    data class SupabaseItem(val url: String?, val supaId: Int, val order: Long) : DisplayItem(supaId.toString()) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SupabaseItem) return false

            if (id != other.id) return false
            if (url != other.url) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + url.hashCode()
            return result
        }
    }
}
