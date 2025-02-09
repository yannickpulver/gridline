package com.yannickpulver.gridline.data

import kotlinx.serialization.Serializable

@Serializable
data class InstaFeed(
    val data: List<MediaItem>
)

@Serializable
data class MediaItem(
    val id: String,
    val media_type: String,
    val media_url: String,
    val thumbnail_url: String? = null,
    val is_shared_to_feed: Boolean? = null
) {
    val imageUrl = (if (media_type == "VIDEO") thumbnail_url else media_url) ?: media_url
}
