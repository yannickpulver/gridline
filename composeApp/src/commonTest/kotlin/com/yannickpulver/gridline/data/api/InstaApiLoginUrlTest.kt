package com.yannickpulver.gridline.data.api

import com.russhwolf.settings.MapSettings
import com.yannickpulver.gridline.BuildKonfig
import com.yannickpulver.gridline.data.prefs.AppPrefs
import io.ktor.client.HttpClient
import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals

class InstaApiLoginUrlTest {

    private val api = InstaApi(appPrefs = AppPrefs(MapSettings()), client = HttpClient())

    // Opening Instagram directly (no redirect through Supabase) keeps the Android Custom Tab from
    // handing the login to the Instagram app
    @Test
    fun loginUrlOpensInstagramDirectly() {
        val url = Url(api.loginUrl("abc"))

        assertEquals("https://www.instagram.com/oauth/authorize", "${url.protocol.name}://${url.host}${url.encodedPath}")
        assertEquals("1797906374484097", url.parameters["client_id"])
        assertEquals(
            "${BuildKonfig.SUPABASE_URL.trimEnd('/')}/functions/v1/instagram-auth/callback",
            url.parameters["redirect_uri"],
        )
        assertEquals("code", url.parameters["response_type"])
        assertEquals("instagram_business_basic", url.parameters["scope"])
        assertEquals("false", url.parameters["enable_fb_login"])
        assertEquals("abc", url.parameters["state"])
    }

    // The callback sends Android and iOS back into the app, desktop keeps the browser page
    @Test
    fun loginUrlAddsThePlatformToTheState() {
        assertEquals("abc.ios", Url(api.loginUrl("abc", platform = "ios")).parameters["state"])
    }
}
