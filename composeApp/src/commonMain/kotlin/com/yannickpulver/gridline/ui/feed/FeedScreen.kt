package com.yannickpulver.gridline.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.yannickpulver.gridline.ui.feed.model.DisplayItem
import com.yannickpulver.gridline.ui.snackbar.SnackbarStateHolder
import kotlinx.coroutines.launch

expect val hasHeader: Boolean

@Composable
fun FeedScreen(component: FeedComponent, modifier: Modifier = Modifier) {
    val state = component.state.collectAsState()

    val selectedItem = remember { mutableStateOf<DisplayItem.SupabaseItem?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (hasHeader) {
                TopBar(state.value, component)
            }
        }
    ) {
        Column(modifier = modifier.fillMaxSize()) {
            Box(modifier = Modifier.padding(top = it.calculateTopPadding()).weight(1f)) {
                Feed(
                    state = state.value,
                    onMove = component::onMove,
                    onDelete = component::onDelete,
                    onItemClick = {
                        selectedItem.value =
                            if (selectedItem.value?.url == it.url) (if (selectedItem.value != null) null else it) else it
                    },
                    showBorders = state.value.showBorders,
                    onHide = component::hideImage
                )
                Column(
                    modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PhotoPickerWrapper(component::addImages) { onClick ->
                        FloatingActionButton(
                            onClick = onClick,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
            BottomContainer(selectedItem = selectedItem, component = component, state = state.value)
        }
    }
}

@Composable
private fun TopBar(
    state: FeedViewState,
    component: FeedComponent
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding()
                .padding(start = 24.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(32.dp))
            Text(
                "Gridline",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Menu(
                uuid = state.uuid,
                addPlaceholder = component::addPlaceholder,
                reset = component::reset,
                toggleBorders = { component.toggleBorders() },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.MoreHoriz,
                        contentDescription = "Menu"
                    )
                }
            )
        }
        Divider()
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun BottomContainer(
    selectedItem: MutableState<DisplayItem.SupabaseItem?>,
    component: FeedComponent,
    state: FeedViewState
) {
    AnimatedVisibility(
        visible = selectedItem.value != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        LaunchedEffect(Unit) {
            component.fetchPossibleImages()
        }

        val lazyListState = rememberLazyGridState()

        LaunchedEffect(state.storedImages, selectedItem.value?.url) {
            val index =
                state.storedImages.map { it.url }.indexOf(selectedItem.value?.url)
            if (index != -1) {
                lazyListState.animateScrollToItem((index - 1).coerceAtLeast(0))
            }
        }

        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .navigationBarsPadding()
        ) {
            Row(modifier = Modifier.align(Alignment.End)) {
                PhotoPickerWrapper(addImages = component::storeImages) { onClick ->
                    IconButton(onClick = onClick) {
                        Icon(
                            imageVector = Icons.Rounded.FileUpload,
                            contentDescription = "Upload"
                        )
                    }
                }

                IconButton(onClick = { selectedItem.value = null }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close"
                    )
                }
            }

            val rows = remember { derivedStateOf { if (state.storedImages.size > 10) 2 else 1 } }

            LazyHorizontalGrid(
                rows = GridCells.Fixed(rows.value),
                state = lazyListState,
                modifier = Modifier.fillMaxWidth().height(72.dp * rows.value + 8.dp * 2 + 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                itemsIndexed(state.storedImages) { i, (path, url) ->
                    SubcomposeAsyncImage(
                        model = url,
                        loading = { ImageLoader() },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .combinedClickable(
                                onClick = {
                                    component.exchangeImage(selectedItem.value, url)
                                    selectedItem.value = selectedItem.value?.copy(url = url)
                                },
                                onLongClick = {
                                    component.removeImage(path)
                                }
                            )
                            .then(
                                if (selectedItem.value?.url == url)
                                    Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary
                                    ) else Modifier
                            )

                    )
                }
            }
        }
    }
}

@Composable
fun ImageLoader() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
    }
}

@Composable
fun Menu(
    uuid: String?,
    addPlaceholder: () -> Unit,
    reset: () -> Unit,
    toggleBorders: () -> Unit,
    icon: @Composable () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.wrapContentSize(Alignment.TopEnd)
    ) {
        IconButton(onClick = { expanded = !expanded }) {
            icon()
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            uuid?.let { uuid ->
                DropdownMenuItem(
                    text = { Text("Copy Id") },
                    onClick = {
                        clipboard.setText(AnnotatedString(uuid))
                        scope.launch {
                            SnackbarStateHolder.success("Copied to clipboard!")
                        }
                        expanded = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Add Placeholder") },
                onClick = {
                    addPlaceholder()
                    expanded = false
                }
            )
//            DropdownMenuItem(
//                text = { Text("Toggle Borders") },
//                onClick = {
//                    toggleBorders()
//                    expanded = false
//                }
//            )
            DropdownMenuItem(
                text = { Text("Reset", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    reset()
                    expanded = false
                }
            )
        }
    }
}
