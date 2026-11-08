package com.yannickpulver.gridline.data.prefs

import com.russhwolf.settings.Settings
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.exception.NoSessionFoundException
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json

/**
 * Keeps the Supabase sign-in under Gridline's own key. On the Mac all Java apps share one settings
 * store, and the library's default key "session" would read (and delete) other apps' sign-ins.
 */
class GridlineSessionManager(private val settings: Settings) : SessionManager {
    // Without encodeDefaults the restored session misses fields
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    override suspend fun saveSession(session: UserSession) {
        settings.putString(KEY, json.encodeToString(UserSession.serializer(), session))
    }

    override suspend fun loadSession(): UserSession {
        val session = settings.getStringOrNull(KEY) ?: throw NoSessionFoundException()
        return json.decodeFromString(UserSession.serializer(), session)
    }

    override suspend fun deleteSession() {
        settings.remove(KEY)
    }

    private companion object {
        const val KEY = "key-supabaseSession"
    }
}
