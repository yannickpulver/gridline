package com.yannickpulver.gridline.data.dto.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccessTokenRequestDto(
    @SerialName("client_id") val client_id: String,
    @SerialName("client_secret") val client_secret: String,
    @SerialName("grant_type") val grant_type: String,
    @SerialName("redirect_uri") val redirect_uri: String,
    @SerialName("code") val code: String
)
