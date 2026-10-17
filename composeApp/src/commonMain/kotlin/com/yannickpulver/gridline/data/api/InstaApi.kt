package com.yannickpulver.gridline.data.api

import com.yannickpulver.gridline.BuildKonfig
import com.yannickpulver.gridline.data.dto.InstaMediaResponseDto
import com.yannickpulver.gridline.data.dto.InstaTokenDto
import com.yannickpulver.gridline.data.prefs.AppPrefs
import com.yannickpulver.gridline.ui.feed.model.DisplayItem
import com.yannickpulver.gridline.ui.snackbar.SnackbarStateHolder
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class InstaApi(
    private val appPrefs: AppPrefs,
    private val client: HttpClient,
    private val clock: Clock = Clock.System,
) {

    private val authUrl = "${BuildKonfig.SUPABASE_URL.trimEnd('/')}/functions/v1/instagram-auth"

    /** Opens Instagram's login page; the token is picked up afterwards with [fetchLoginToken]. */
    fun loginUrl(state: String) = "$authUrl/login?state=$state"

    /** Returns the token once the user finished the login in the browser, null while pending. */
    suspend fun fetchLoginToken(state: String): String? {
        val response = client.get("$authUrl/token") { parameter("state", state) }
        if (!response.status.isSuccess()) return null
        return response.body<InstaTokenDto>().accessToken
    }

    fun getFeed(): Flow<List<DisplayItem.InstaItem>> = flow {
        val token = appPrefs.getInstaToken() ?: return@flow
        emit(appPrefs.getFeed())

        // Keep the cached feed when Instagram fails, so the grid never goes empty
        val result = runCatching { fetchMedia(refreshedToken(token)) }
            .onFailure { SnackbarStateHolder.error("Couldn't fetch Instagram feed") }
            .getOrNull() ?: return@flow
        emit(result)
        appPrefs.setFeed(result)
    }

    private suspend fun fetchMedia(token: String): List<DisplayItem.InstaItem> {
        val response = client.get("https://graph.instagram.com/me/media") {
            parameter("fields", "media_type,media_url,thumbnail_url,timestamp")
            parameter("limit", 12)
            parameter("access_token", token)
        }
        check(response.status.isSuccess()) { "Instagram responded ${response.status}" }

        return response.body<InstaMediaResponseDto>().data.mapNotNull { media ->
            DisplayItem.InstaItem(
                url = media.imageUrl ?: return@mapNotNull null,
                publishedAt = media.timestamp?.let(::parseTimestamp),
            )
        }
    }

    /** Long-lived tokens last 60 days; refreshing daily keeps them alive while the app is used. */
    private suspend fun refreshedToken(token: String): String {
        val refreshedAt = appPrefs.getInstaTokenRefreshedAt()
        if (clock.now() - refreshedAt < 1.days) return token

        val refreshed = runCatching {
            client.get("https://graph.instagram.com/refresh_access_token") {
                parameter("grant_type", "ig_refresh_token")
                parameter("access_token", token)
            }.body<InstaTokenDto>().accessToken
        }.getOrNull() ?: return token

        appPrefs.setInstaToken(refreshed)
        return refreshed
    }

    // Instagram sends "2024-05-01T10:00:00+0000", Instant.parse wants "+00:00"
    private fun parseTimestamp(value: String): Long? = runCatching {
        Instant.parse(value.replace(Regex("([+-]\\d{2})(\\d{2})$"), "$1:$2")).epochSeconds
    }.getOrNull()
}
