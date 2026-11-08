package com.yannickpulver.gridline.data.api

import co.touchlab.kermit.Logger
import com.yannickpulver.gridline.data.prefs.AppPrefs
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Supabase only shows a grid to installs linked to it: this signs the install in anonymously and lets
 * instagram-auth link it, using the Instagram token as proof that the account is ours.
 * Without sign-in (older backend) everything keeps working with the public key.
 */
class GridAccess(
    private val supabase: SupabaseClient,
    private val instaApi: InstaApi,
    private val prefs: AppPrefs,
) {
    private val mutex = Mutex()

    suspend fun ensureLinked() = mutex.withLock {
        try {
            link()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(e) { "Linking this install to its grid failed, retrying on the next refresh" }
        }
    }

    private suspend fun link() {
        val instaToken = prefs.getInstaToken() ?: return
        val auth = supabase.auth
        auth.awaitInitialization()
        if (auth.currentSessionOrNull() == null) auth.signInAnonymously()
        val session = auth.currentSessionOrNull() ?: return
        val authUserId = session.user?.id ?: auth.currentUserOrNull()?.id ?: return
        if (prefs.getLinkedSignIn() == authUserId) return

        val gridlineId = instaApi.link(instaToken, session.accessToken) ?: return
        prefs.setLinkedSignIn(authUserId, gridlineId)
        Logger.i { "Linked this install to its grid" }
    }
}
