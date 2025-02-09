package com.yannickpulver.gridline.ui.feed

import androidx.compose.runtime.Stable
import com.yannickpulver.gridline.domain.model.StoredImage
import com.yannickpulver.gridline.ui.feed.model.DisplayItem

@Stable
data class FeedViewState(
    val data: List<DisplayItem> = emptyList(),
    val storedImages: List<StoredImage> = emptyList(),
    val uuid: String? = null,
    val showBorders: Boolean = false,
)
