package com.yannickpulver.gridline.data.api

import com.russhwolf.settings.MapSettings
import com.yannickpulver.gridline.data.prefs.AppPrefs
import com.yannickpulver.gridline.data.prefs.PinnedPost
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class InstaApiPinnedTest {

    private val requests = mutableListOf<String>()

    private fun media(id: String, shortcode: String) =
        """{"id":"$id","shortcode":"$shortcode","media_type":"IMAGE","media_url":"https://cdn/$id.jpg","timestamp":"2026-11-01T08:00:01+0000"}"""

    private val engine = MockEngine { request ->
        requests += request.url.toString()
        val params = request.url.parameters
        val body = when {
            request.url.encodedPath == "/me/media" && params["after"] == "page2" ->
                """{"data":[${media("old", "OLD")}]}"""

            request.url.encodedPath == "/me/media" -> {
                val next = "https://graph.instagram.com/me/media?after=page2&access_token=tok"
                """{"data":[${media("new", "NEW")},${media("recent", "RECENT")}],"paging":{"next":"$next"}}"""
            }

            request.url.encodedPath == "/old" -> media("old", "OLD")
            request.url.encodedPath == "/recent" -> media("recent", "RECENT")
            else -> error("Unexpected request ${request.url}")
        }
        respond(body, headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
    }

    private val prefs = AppPrefs(MapSettings()).apply { setInstaToken("tok") }
    private val api = InstaApi(
        appPrefs = prefs,
        client = HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } },
    )

    @Test
    fun findPostsPagesUntilAllAreFound() = runTest {
        val posts = api.findPosts(listOf("OLD", "RECENT"))

        assertEquals(listOf(PinnedPost("OLD", "old"), PinnedPost("RECENT", "recent")), posts)
        assertEquals(2, requests.size)
    }

    @Test
    fun findPostsStopsOnFirstPageWhenAllAreThere() = runTest {
        api.findPosts(listOf("RECENT"))

        assertEquals(1, requests.size)
    }

    @Test
    fun findPostsSkipsUnknownPosts() = runTest {
        assertEquals(listOf(PinnedPost("OLD", "old")), api.findPosts(listOf("MISSING", "OLD")))
    }

    @Test
    fun feedShowsPinnedPostsFirstAndOnlyOnce() = runTest {
        prefs.setPinnedPosts(listOf(PinnedPost("OLD", "old"), PinnedPost("RECENT", "recent")))

        val feed = api.getFeed().last()

        assertEquals(listOf("old", "recent", "new"), feed.map { it.mediaId })
        assertEquals(listOf(true, true, false), feed.map { it.pinned })
        assertEquals(feed, prefs.getFeed())
    }
}
