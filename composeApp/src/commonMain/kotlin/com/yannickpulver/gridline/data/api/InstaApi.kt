package com.yannickpulver.gridline.data.api

import com.yannickpulver.gridline.BuildKonfig
import com.yannickpulver.gridline.data.dto.InstaAccountDto
import com.yannickpulver.gridline.data.dto.InstaErrorResponseDto
import com.yannickpulver.gridline.data.dto.InstaLoginDto
import com.yannickpulver.gridline.data.dto.InstaMediaDto
import com.yannickpulver.gridline.data.dto.InstaMediaResponseDto
import com.yannickpulver.gridline.data.dto.InstaTokenDto
import com.yannickpulver.gridline.data.dto.LinkRequestDto
import com.yannickpulver.gridline.data.dto.LinkResponseDto
import com.yannickpulver.gridline.data.prefs.AppPrefs
import com.yannickpulver.gridline.data.prefs.PinnedPost
import com.yannickpulver.gridline.ui.feed.model.DisplayItem
import com.yannickpulver.gridline.ui.snackbar.SnackbarStateHolder
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
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

    /**
     * Instagram's login page; the token is picked up afterwards with [fetchLogin]. It opens Instagram
     * directly instead of redirecting through instagram-auth/login: on Android that redirect made
     * the browser hand the login to the Instagram app. [platform] tells the callback where to return.
     */
    fun loginUrl(state: String, platform: String? = loginPlatform) = URLBuilder("https://www.instagram.com/oauth/authorize").apply {
        parameters.append("client_id", INSTAGRAM_APP_ID)
        parameters.append("redirect_uri", "$authUrl/callback")
        parameters.append("response_type", "code")
        parameters.append("scope", "instagram_business_basic")
        parameters.append("enable_fb_login", "false")
        parameters.append("state", listOfNotNull(state, platform).joinToString("."))
    }.buildString()

    /**
     * Returns the login once the user finished it in the browser, null while pending.
     * [gridlineId] is this device's ID, which becomes the account's ID on its first login.
     */
    suspend fun fetchLogin(state: String, gridlineId: String?): InstaLoginDto? {
        val response = client.get("$authUrl/token") {
            parameter("state", state)
            // Sent even when empty, the server only links accounts for clients that send it
            parameter("gridline_id", gridlineId.orEmpty())
        }
        if (!response.status.isSuccess()) return null
        return response.body<InstaLoginDto>()
    }

    /**
     * Links the Supabase sign-in behind [supabaseToken] to the grid of the Instagram account behind
     * [instaToken]. Returns the account's Gridline ID, null when it didn't work.
     */
    suspend fun link(instaToken: String, supabaseToken: String): String? {
        val response = client.post("$authUrl/link") {
            bearerAuth(supabaseToken)
            contentType(ContentType.Application.Json)
            setBody(LinkRequestDto(instaToken))
        }
        if (!response.status.isSuccess()) return null
        return response.body<LinkResponseDto>().gridlineId
    }

    fun getFeed(): Flow<List<DisplayItem.InstaItem>> = flow {
        val token = appPrefs.getInstaToken() ?: return@flow
        emit(appPrefs.getFeed())

        // Keep the cached feed when Instagram fails, so the grid never goes empty
        val result = runCatching { fetchFeed(refreshedToken(token)) }.getOrElse {
            if (it is InstaLoginExpiredException) throw it
            SnackbarStateHolder.error("Couldn't fetch Instagram feed")
            return@flow
        }
        emit(result)
        appPrefs.setFeed(result)
    }

    /** The cached account first, then the current one; a failed request keeps the cached one. */
    fun getAccount(): Flow<InstaAccountDto> = flow {
        val token = appPrefs.getInstaToken() ?: return@flow
        appPrefs.getAccount()?.let { emit(it) }

        val account = runCatching {
            graphGet<InstaAccountDto>("$GRAPH_URL/me", token) {
                parameter("fields", "username,profile_picture_url")
            }
        }.getOrNull() ?: return@flow
        emit(account)
        appPrefs.setAccount(account)
    }

    /** Pinned posts first, like on the profile, then the latest posts. */
    private suspend fun fetchFeed(token: String): List<DisplayItem.InstaItem> {
        val latest = graphGet<InstaMediaResponseDto>("$GRAPH_URL/me/media", token) {
            parameter("fields", MEDIA_FIELDS)
            parameter("limit", 12)
        }.data.mapNotNull { it.toItem() }

        // A pinned post that was deleted on Instagram just drops out
        val pinned = appPrefs.getPinnedPosts().mapNotNull { post ->
            runCatching {
                graphGet<InstaMediaDto>("$GRAPH_URL/${post.mediaId}", token) {
                    parameter("fields", MEDIA_FIELDS)
                }.toItem()?.copy(pinned = true)
            }.getOrNull()
        }

        return pinned + latest.filter { item -> pinned.none { it.mediaId == item.mediaId } }
    }

    /**
     * Finds the posts with these [shortcodes] among the user's posts.
     * The API can't look up a post by its link, so this pages through all posts until it found them.
     */
    suspend fun findPosts(shortcodes: List<String>): List<PinnedPost> {
        val token = appPrefs.getInstaToken() ?: return emptyList()
        val found = mutableMapOf<String, String>()
        var page = graphGet<InstaMediaResponseDto>("$GRAPH_URL/me/media", token) {
            parameter("fields", "id,media_type,shortcode")
            parameter("limit", 100)
        }
        while (true) {
            page.data.forEach { media -> media.shortcode?.takeIf { it in shortcodes }?.let { found[it] = media.id } }
            val next = page.paging?.next
            if (next == null || found.keys.containsAll(shortcodes)) break
            // The next page's url already carries the fields and the token
            page = graphGet(next, token = null)
        }
        return shortcodes.mapNotNull { code -> found[code]?.let { PinnedPost(code, it) } }
    }

    private suspend inline fun <reified T> graphGet(
        url: String,
        token: String?,
        crossinline block: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response = client.get(url) {
            token?.let { parameter("access_token", it) }
            block()
        }
        if (!response.status.isSuccess()) {
            val code = runCatching { response.body<InstaErrorResponseDto>().error.code }.getOrNull()
            if (code == INVALID_TOKEN) throw InstaLoginExpiredException()
            error("Instagram responded ${response.status}")
        }
        return response.body()
    }

    private fun InstaMediaDto.toItem() = imageUrl?.let { url ->
        DisplayItem.InstaItem(
            url = url,
            publishedAt = timestamp?.let(::parseTimestamp),
            mediaId = id,
        )
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

    private companion object {
        // Expired or revoked, e.g. after a password change or removing the app in Instagram
        const val INVALID_TOKEN = 190
        const val GRAPH_URL = "https://graph.instagram.com"
        // Instagram app ID of the Meta app "Gridline" (public, also in the login URL)
        const val INSTAGRAM_APP_ID = "1797906374484097"
        const val MEDIA_FIELDS = "id,media_type,media_url,thumbnail_url,timestamp"
    }
}

/** The Instagram token can't be used anymore, the user has to log in again. */
class InstaLoginExpiredException : Exception("Instagram login expired")
