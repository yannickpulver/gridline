package com.yannickpulver.gridline.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InstaMediaResponseDto(
    val data: List<InstaMediaDto> = emptyList(),
)

@Serializable
data class InstaMediaDto(
    @SerialName("media_type") val mediaType: String,
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    val timestamp: String? = null,
) {
    val imageUrl: String? get() = if (mediaType == "VIDEO") thumbnailUrl else mediaUrl
}

@Serializable
data class InstaLoginDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("gridline_id") val gridlineId: String,
)

@Serializable
data class InstaTokenDto(
    @SerialName("access_token") val accessToken: String,
)
