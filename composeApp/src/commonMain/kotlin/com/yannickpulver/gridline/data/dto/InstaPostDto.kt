package com.yannickpulver.gridline.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InstaFeedDto(
    val items: List<InstaFeedItem> = emptyList(),
    val status: String,
)

@Serializable
data class InstaFeedItem(
    @SerialName("taken_at")
    val takenAt: Long,
    @SerialName("image_versions2")
    val imageVersions: ImageVersions? = null,
)

@Serializable
data class ImageVersions(
    val candidates: List<ImageCandidate> = emptyList(),
)

@Serializable
data class ImageCandidate(
    val url: String,
    val width: Long,
    val height: Long,
)
