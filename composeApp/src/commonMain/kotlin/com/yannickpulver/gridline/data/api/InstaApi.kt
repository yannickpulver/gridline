package com.yannickpulver.gridline.data.api

import com.yannickpulver.gridline.data.dto.InstaFeedDto
import com.yannickpulver.gridline.data.prefs.AppPrefs
import com.yannickpulver.gridline.ui.feed.model.DisplayItem
import com.yannickpulver.gridline.ui.snackbar.SnackbarStateHolder
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

class InstaApi(val appPrefs: AppPrefs, val client: HttpClient, val json: Json) {

    fun getFeed(): Flow<List<DisplayItem.InstaItem>> = flow {
        val userName = appPrefs.getUserName() ?: return@flow
        emit(appPrefs.getFeed())
        val result = getFeedByUsername(userName)
        emit(result)
        appPrefs.setFeed(result)
    }

    private suspend fun getFeedByUsername(userName: String): List<DisplayItem.InstaItem> {
        val url = "https://i.instagram.com/api/v1/feed/user/$userName/username/?count=12"
        val result = client.get(url) {
            header("x-ig-app-id", "567067343352427")
        }
        val response = runCatching {
            if (result.status.value != 200) {
                null
            } else {
                json.decodeFromString<InstaFeedDto>(result.body())
            }
        }

        return response
            .onFailure { SnackbarStateHolder.error("Couldn't fetch Instagram API") }
            .getOrNull()?.items
            ?.mapNotNull { item ->
                val imageUrl = item.imageVersions?.candidates?.firstOrNull()?.url
                    ?: return@mapNotNull null
                DisplayItem.InstaItem(
                    url = imageUrl,
                    publishedAt = item.takenAt,
                )
            }.orEmpty()
    }
}
