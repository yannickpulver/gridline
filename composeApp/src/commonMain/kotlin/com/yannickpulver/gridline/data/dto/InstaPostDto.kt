package com.yannickpulver.gridline.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InstaPostsDto(
    val data: Data,
    val status: String,
)

@Serializable
data class Data(
    val user: User,
)

@Serializable
data class User(
    @SerialName("edge_owner_to_timeline_media") val edgeOwnerToTimelineMedia: EdgeOwnerToTimelineMedia,
)

@Serializable
data class EdgeOwnerToTimelineMedia(
    val edges: List<Post>,
)

@Serializable
data class Post(
    val node: PostContent,
)

@Serializable
data class PostContent(
    val id: String,
    @SerialName("taken_at_timestamp")
    val takenAtTimestamp: Long,
    @SerialName("is_video")
    val isVideo: Boolean,
    @SerialName("thumbnail_resources")
    val thumbnailResources: List<Thumbnail>? = null,
    @SerialName("display_url")
    val displayUrl: String? = null,
)

@Serializable
data class Thumbnail(
    val src: String,
    @SerialName("config_width")
    val width: Long,
    @SerialName("config_height")
    val height: Long,
)
