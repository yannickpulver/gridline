package com.yannickpulver.gridline.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImageCreateDto(
    @SerialName("url") val url: String?,
    @SerialName("order") val order: Long,
    @SerialName("user_id") val userId: String
)

@Serializable
data class ImageDto(
    @SerialName("url") val url: String?,
    @SerialName("order") val order: Long,
    @SerialName("id") val id: Int,
    @SerialName("user_id") val userId: String,
)

@Serializable
data class ImageUpdateDto(
    @SerialName("order") val order: Long,
    @SerialName("url") val url: String?
)
