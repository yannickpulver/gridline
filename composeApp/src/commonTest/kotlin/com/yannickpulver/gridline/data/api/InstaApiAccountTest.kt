package com.yannickpulver.gridline.data.api

import com.russhwolf.settings.MapSettings
import com.yannickpulver.gridline.data.dto.InstaAccountDto
import com.yannickpulver.gridline.data.prefs.AppPrefs
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class InstaApiAccountTest {

    private var instagramUp = true

    private val engine = MockEngine { request ->
        assertEquals("/me", request.url.encodedPath)
        assertEquals("username,profile_picture_url", request.url.parameters["fields"])
        if (!instagramUp) return@MockEngine respondError(HttpStatusCode.InternalServerError)
        respond(
            """{"username":"gridline.app","profile_picture_url":"https://cdn/me.jpg","id":"1"}""",
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }

    private val prefs = AppPrefs(MapSettings()).apply { setInstaToken("tok") }
    private val api = InstaApi(
        appPrefs = prefs,
        client = HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } },
    )

    private val current = InstaAccountDto("gridline.app", "https://cdn/me.jpg")

    @Test
    fun accountShowsTheCachedOneThenTheCurrentOne() = runTest {
        prefs.setAccount(InstaAccountDto("old.name", null))

        assertEquals(listOf(InstaAccountDto("old.name", null), current), api.getAccount().toList())
        assertEquals(current, prefs.getAccount())
    }

    @Test
    fun accountKeepsTheCachedOneWhenInstagramFails() = runTest {
        prefs.setAccount(current)
        instagramUp = false

        assertEquals(listOf(current), api.getAccount().toList())
    }

    @Test
    fun loggingOutForgetsTheAccount() = runTest {
        api.getAccount().toList()
        prefs.clearUserInfo()

        assertEquals(null, prefs.getAccount())
    }
}
