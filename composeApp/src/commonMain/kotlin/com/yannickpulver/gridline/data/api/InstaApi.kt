package com.yannickpulver.gridline.data.api

import com.yannickpulver.gridline.data.dto.InstaPostsDto
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
        val url = "https://i.instagram.com/api/v1/users/web_profile_info/?username=$userName"
        val result = client.get(url) {
            header("x-ig-app-id", "567067343352427")
        }
        val response = runCatching {
            if (result.status.value != 200) {
                null
            } else {
                json.decodeFromString<InstaPostsDto>(result.body())
            }
        }

        return response
            .onFailure { SnackbarStateHolder.error("Couldn't fetch Instagram API") }
            .getOrNull()?.let { root ->
                root.data.user.edgeOwnerToTimelineMedia.edges.map { edge ->
                    val node = edge.node
                    println(node)
                    DisplayItem.InstaItem(
                        url = node.displayUrl
                            ?: node.thumbnailResources?.lastOrNull()?.src.orEmpty(),
                        publishedAt = node.takenAtTimestamp,
                    )
                }
            }.orEmpty()
    }
}
