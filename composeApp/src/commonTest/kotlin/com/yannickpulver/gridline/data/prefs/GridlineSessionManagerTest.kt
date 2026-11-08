package com.yannickpulver.gridline.data.prefs

import com.russhwolf.settings.MapSettings
import io.github.jan.supabase.auth.exception.NoSessionFoundException
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GridlineSessionManagerTest {

    // Another app's sign-in in the settings store all Java apps share on the Mac
    private val settings = MapSettings("session" to "other app's session")
    private val manager = GridlineSessionManager(settings)
    private val session = UserSession(accessToken = "access", refreshToken = "refresh", expiresIn = 3600, tokenType = "bearer")

    @Test
    fun ignoresOtherAppsSession() = runTest {
        assertFailsWith<NoSessionFoundException> { manager.loadSession() }
    }

    @Test
    fun keepsOtherAppsSessionWhenSavingAndDeleting() = runTest {
        manager.saveSession(session)
        assertEquals("access", manager.loadSession().accessToken)

        manager.deleteSession()

        assertFailsWith<NoSessionFoundException> { manager.loadSession() }
        assertEquals("other app's session", settings.getString("session", ""))
    }
}
