package com.yannickpulver.gridline.data.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccessTokenResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("user_id") val userId: Long
)

@Serializable
data class LongLivedAccessTokenResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Long
)
