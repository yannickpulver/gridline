package com.yannickpulver.gridline.ui.feed

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import coil3.compose.SubcomposeAsyncImage
import com.yannickpulver.gridline.ui.feed.model.DisplayItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyGridState

@Composable
fun Feed(
    state: FeedViewState,
    onMove: (DisplayItem, DisplayItem?, DisplayItem?) -> Unit,
    onDelete: (DisplayItem) -> Unit,
    onHide: (DisplayItem.InstaItem) -> Unit,
    onItemClick: (DisplayItem.SupabaseItem) -> Unit,
    showBorders: Boolean
) {
    val hapticFeedback = LocalHapticFeedback.current
    val items = remember(state.data) { mutableStateOf(state.data) }


    val gridState = rememberLazyGridState()
    var fromIndexState by remember { mutableStateOf<Int?>(null) }
    var toIndexState by remember { mutableStateOf<Int?>(null) }

    val reorderableState = rememberReorderableLazyGridState(gridState) { from, to ->
        val localFrom = from.index
        val localTo = to.index

        Logger.d { "ReorderableLazyListState: ${from.index}, ${to.index} --> local: $localFrom, $localTo" }

        if (fromIndexState == null) fromIndexState = localFrom
        if (localFrom == localTo) return@rememberReorderableLazyGridState
        toIndexState = localTo
        items.value = items.value.toMutableList().apply {
            add(localTo, removeAt(localFrom))
        }
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            val finalFrom = fromIndexState ?: return@LaunchedEffect
            val finalTo = toIndexState ?: return@LaunchedEffect
            fromIndexState = null
            toIndexState = null
            Logger.d { "ReorderableLazyListState: final: $finalFrom, $finalTo" }
            if (finalFrom == finalTo) return@LaunchedEffect
            val current = items.value[finalTo]
            val before = items.value.getOrNull(finalTo - 1)
            val after = items.value.getOrNull(finalTo + 1)
            onMove(current, before, after)
        }
    }

    // todo: call onMove when the item is moved


    Box {
        if (state.data.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "You can add images with the button down below.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            itemsIndexed(items = items.value, key = { i, item -> item.id }) { i, item ->
                when (item) {
                    is DisplayItem.InstaItem -> {
                        InstaGridItem(item, onHide)
                    }

                    is DisplayItem.SupabaseItem -> {
                        ReorderableGridItem(
                            reorderState = reorderableState,
                            item = item,
                            onDelete = onDelete,
                            onClick = { onItemClick(item) },
                            showBorders = showBorders
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstaGridItem(
    item: DisplayItem.InstaItem,
    onHide: (DisplayItem.InstaItem) -> Unit,
    borders: BorderType = BorderType.None
) {
    Box(
        modifier = Modifier
            .aspectRatio(3 / 4f)
            .background(MaterialTheme.colorScheme.surface)
            .padding(
                horizontal = if (borders == BorderType.Horizontal) 8.dp else 0.dp,
                vertical = if (borders == BorderType.Vertical) 8.dp else 0.dp
            )
    ) {
        SubcomposeAsyncImage(
            model = item.url,
            contentDescription = null,
            loading = { ImageLoader() },
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Icon(
            imageVector = Icons.Rounded.VisibilityOff,
            contentDescription = null,
            modifier = Modifier
                .clickable { onHide(item) }
                .align(Alignment.BottomEnd)
                .size(20.dp)
                .padding(2.dp)
                .background(Color.Black.copy(0.5f), RoundedCornerShape(4.dp))
                .padding(2.dp),
            tint = Color.White
        )
    }
}

enum class BorderType {
    Horizontal, Vertical, None
}

@Composable
private fun LazyGridItemScope.ReorderableGridItem(
    reorderState: ReorderableLazyGridState,
    item: DisplayItem.SupabaseItem,
    onDelete: (DisplayItem) -> Unit,
    onClick: () -> Unit = {},
    showBorders: Boolean = false,
) {
    ReorderableItem(
        state = reorderState,
        key = item.id,
        modifier = Modifier.clickable(onClick = onClick)
    ) { isDragging ->
        val elevation = animateDpAsState(if (isDragging) 8.dp else 0.dp)

        val hapticFeedback = LocalHapticFeedback.current

        var borderType by remember { mutableStateOf(BorderType.None) }
        val borderFraction = 1f / 36


        Box(
            modifier = Modifier
                .aspectRatio(3 / 4f)
                .shadow(elevation.value)
                .longPressDraggableHandle(onDragStarted = { hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) })
        ) {
            if (item.url != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .align(Alignment.Center)
                ) {
                    SubcomposeAsyncImage(
                        model = item.url,
                        loading = { ImageLoader() },
                        onSuccess = {
                            borderType =
                                if (it.result.image.height < it.result.image.width) BorderType.Vertical else BorderType.Horizontal
                        },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxHeight(fraction = if (borderType == BorderType.Vertical && showBorders) 1f - borderFraction * 2 else 1f)
                            .fillMaxWidth(fraction = if (borderType == BorderType.Horizontal && showBorders) 1f - borderFraction * 2 else 1f)
                            .align(Alignment.Center)
                    )
                }
            } else {
                Box(
                    modifier = Modifier.aspectRatio(3 / 4f)
                        .background(Color.LightGray)
                )
            }

            DeleteItem(
                onDelete = { onDelete(item) },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun DeleteItem(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = Icons.Rounded.Delete,
        contentDescription = null,
        modifier = modifier
            .size(20.dp)
            .padding(2.dp)
            .background(Color.Black.copy(0.5f), RoundedCornerShape(4.dp))
            .padding(2.dp)
            .clickable(onClick = onDelete),
        tint = Color.White
    )
}
