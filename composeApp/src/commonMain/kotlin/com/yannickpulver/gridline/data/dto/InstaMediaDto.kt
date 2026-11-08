package com.yannickpulver.gridline.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InstaMediaResponseDto(
    val data: List<InstaMediaDto> = emptyList(),
    val paging: InstaPagingDto? = null,
)

@Serializable
data class InstaPagingDto(
    val next: String? = null,
)

@Serializable
data class InstaMediaDto(
    val id: String,
    @SerialName("media_type") val mediaType: String,
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    val timestamp: String? = null,
    val shortcode: String? = null,
) {
    val imageUrl: String? get() = if (mediaType == "VIDEO") thumbnailUrl else mediaUrl
}

@Serializable
data class InstaLoginDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("gridline_id") val gridlineId: String,
)

@Serializable
data class LinkRequestDto(
    @SerialName("access_token") val accessToken: String,
)

@Serializable
data class LinkResponseDto(
    @SerialName("gridline_id") val gridlineId: String,
)

@Serializable
data class InstaErrorResponseDto(
    val error: InstaErrorDto,
)

@Serializable
data class InstaErrorDto(
    val code: Int,
)

@Serializable
data class InstaTokenDto(
    @SerialName("access_token") val accessToken: String,
)
