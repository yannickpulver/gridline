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
import kotlin.uuid.Uuid

@OptIn(ExperimentalSerializationApi::class, ExperimentalSettingsApi::class)

class AppPrefs(private val settings: Settings) {
    private val InstaItemListSerializer = ListSerializer(DisplayItem.InstaItem.serializer())

    fun setUserName(userName: String, uuid: String? = null) {
        settings.putString(KEY_USERNAME, userName)
        settings.putString(KEY_UUID, uuid ?: Uuid.random().toString())
    }

    fun hasUserInfo() = getUserName() != null

    fun getUserName() = settings.getStringOrNull(KEY_USERNAME)

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
        settings.remove(KEY_UUID)
        settings.removeValue(InstaItemListSerializer, KEY_FEED)
    }

    companion object {
        const val KEY_USERNAME = "userName"
        const val KEY_UUID = "key-uuid"
        const val KEY_FEED = "key-feed"
        const val KEY_SHOWBORDERS = "key-showBorders"
    }
}
