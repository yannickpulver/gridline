package com.yannickpulver.gridline.data.prefs

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.serialization.containsValue
import com.russhwolf.settings.serialization.decodeValueOrNull
import com.russhwolf.settings.serialization.encodeValue
import com.russhwolf.settings.serialization.removeValue
import com.yannickpulver.gridline.ui.feed.model.DisplayItem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalSerializationApi::class, ExperimentalSettingsApi::class)

class AppPrefs(private val settings: Settings, private val clock: Clock = Clock.System) {
    private val InstaItemListSerializer = ListSerializer(DisplayItem.InstaItem.serializer())
    private val PinnedPostListSerializer = ListSerializer(PinnedPost.serializer())

    /** The Gridline ID belongs to the Instagram account, so every device shows the same grid. */
    fun setInstaLogin(token: String, gridlineId: String) {
        setInstaToken(token)
        settings.putString(KEY_UUID, gridlineId)
    }

    fun hasUserInfo() = getInstaToken() != null

    fun getInstaToken() = settings.getStringOrNull(KEY_INSTA_TOKEN)

    fun setInstaToken(token: String) {
        settings.putString(KEY_INSTA_TOKEN, token)
        settings.putLong(KEY_INSTA_TOKEN_REFRESHED_AT, clock.now().epochSeconds)
    }

    fun getInstaTokenRefreshedAt(): Instant =
        Instant.fromEpochSeconds(settings.getLong(KEY_INSTA_TOKEN_REFRESHED_AT, 0))

    fun getFeed(): List<DisplayItem.InstaItem> {
        return if (settings.containsValue(InstaItemListSerializer, KEY_FEED)) {
            settings.decodeValueOrNull(InstaItemListSerializer, KEY_FEED).orEmpty()
        } else {
            emptyList()
        }
    }

    fun setFeed(items: List<DisplayItem.InstaItem>) {
        settings.encodeValue(InstaItemListSerializer, KEY_FEED, items)
    }

    /** Instagram's API doesn't say which posts are pinned, so the user tells us. */
    fun getPinnedPosts(): List<PinnedPost> =
        settings.decodeValueOrNull(PinnedPostListSerializer, KEY_PINNED_POSTS).orEmpty()

    fun setPinnedPosts(posts: List<PinnedPost>) {
        settings.encodeValue(PinnedPostListSerializer, KEY_PINNED_POSTS, posts)
    }

    fun toggleBorders(borders: Boolean) {
        settings.putBoolean(KEY_SHOWBORDERS, borders)
    }

    fun getUserId() = settings.getStringOrNull(KEY_UUID)

    fun getUuid() = settings.getStringOrNull(KEY_UUID)

    /** The Supabase sign-in that instagram-auth linked to this account's grid. */
    fun getLinkedSignIn() = settings.getStringOrNull(KEY_LINKED_SIGN_IN)

    fun setLinkedSignIn(authUserId: String, gridlineId: String) {
        settings.putString(KEY_LINKED_SIGN_IN, authUserId)
        settings.putString(KEY_UUID, gridlineId)
    }

    fun showBorders() = settings.getBoolean(KEY_SHOWBORDERS, false)

    fun clearUserInfo() {
        settings.remove(KEY_USERNAME)
        settings.remove(KEY_INSTA_TOKEN)
        settings.remove(KEY_INSTA_TOKEN_REFRESHED_AT)
        settings.remove(KEY_LINKED_SIGN_IN)
        // The Gridline ID stays: the next login links it to the account if the account has none yet
        settings.removeValue(InstaItemListSerializer, KEY_FEED)
        settings.removeValue(PinnedPostListSerializer, KEY_PINNED_POSTS)
    }

    companion object {
        // Username from the old public Instagram API, only kept to clean it up
        const val KEY_USERNAME = "userName"
        const val KEY_INSTA_TOKEN = "key-instaToken"
        const val KEY_INSTA_TOKEN_REFRESHED_AT = "key-instaTokenRefreshedAt"
        const val KEY_UUID = "key-uuid"
        const val KEY_FEED = "key-feed"
        const val KEY_SHOWBORDERS = "key-showBorders"
        const val KEY_PINNED_POSTS = "key-pinnedPosts"
        const val KEY_LINKED_SIGN_IN = "key-linkedSignIn"
    }
}

/** A post pinned on the Instagram profile; [shortcode] is the part of its link after /p/. */
@Serializable
data class PinnedPost(val shortcode: String, val mediaId: String)
