package com.yannickpulver.gridline.data.prefs

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.serialization.containsValue
import com.russhwolf.settings.serialization.decodeValueOrNull
import com.russhwolf.settings.serialization.encodeValue
import com.russhwolf.settings.serialization.removeValue
import com.yannickpulver.gridline.ui.feed.model.DisplayItem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalSerializationApi::class, ExperimentalSettingsApi::class)

class AppPrefs(private val settings: Settings, private val clock: Clock = Clock.System) {
    private val InstaItemListSerializer = ListSerializer(DisplayItem.InstaItem.serializer())

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

    fun toggleBorders(borders: Boolean) {
        settings.putBoolean(KEY_SHOWBORDERS, borders)
    }

    fun getUserId() = settings.getStringOrNull(KEY_UUID)

    fun getUuid() = settings.getStringOrNull(KEY_UUID)

    fun showBorders() = settings.getBoolean(KEY_SHOWBORDERS, false)

    fun clearUserInfo() {
        settings.remove(KEY_USERNAME)
        settings.remove(KEY_INSTA_TOKEN)
        settings.remove(KEY_INSTA_TOKEN_REFRESHED_AT)
        // The Gridline ID stays: the next login links it to the account if the account has none yet
        settings.removeValue(InstaItemListSerializer, KEY_FEED)
    }

    companion object {
        // Username from the old public Instagram API, only kept to clean it up
        const val KEY_USERNAME = "userName"
        const val KEY_INSTA_TOKEN = "key-instaToken"
        const val KEY_INSTA_TOKEN_REFRESHED_AT = "key-instaTokenRefreshedAt"
        const val KEY_UUID = "key-uuid"
        const val KEY_FEED = "key-feed"
        const val KEY_SHOWBORDERS = "key-showBorders"
    }
}
