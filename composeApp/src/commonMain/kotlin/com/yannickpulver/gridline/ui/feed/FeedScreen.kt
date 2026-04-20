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
import compose.icons.TablerIcons
import compose.icons.tablericons.CircleCheck
import compose.icons.tablericons.Dots
import compose.icons.tablericons.Plus
import compose.icons.tablericons.Trash
import compose.icons.tablericons.Upload
import compose.icons.tablericons.X
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
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
import androidx.compose.runtime.CompositionLocalProvider
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
                Box(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .navigationBarsPadding().padding(16.dp)
                ) {
                    PhotoPickerWrapper(component::addImages) { onClick ->
                        FloatingActionButton(
                            onClick = onClick,
                            containerColor = Color.Black,
                            contentColor = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Icon(
                                imageVector = TablerIcons.Plus,
                                contentDescription = null
                            )
                        }
                    }
                    if (hasHeader) {
                        Menu(
                            uuid = state.value.uuid,
                            addPlaceholder = component::addPlaceholder,
                            reset = component::reset,
                            toggleBorders = { component.toggleBorders() },
                            icon = {
                                Icon(
                                    imageVector = TablerIcons.Dots,
                                    contentDescription = "Menu",
                                    tint = Color.White
                                )
                            },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
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
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Gridline",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
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

        var selectionMode by remember { mutableStateOf(false) }
        val selectedPaths = remember { mutableStateOf(setOf<String>()) }

        // Reset selection when panel is closed
        LaunchedEffect(selectedItem.value) {
            if (selectedItem.value == null) {
                selectionMode = false
                selectedPaths.value = emptySet()
            }
        }

        LaunchedEffect(state.storedImages, selectedItem.value?.url) {
            val index =
                state.storedImages.map { it.url }.indexOf(selectedItem.value?.url)
            if (index != -1) {
                lazyListState.animateScrollToItem((index - 1).coerceAtLeast(0))
            }
        }

        Column(
            Modifier
                .background(Color.White)
                .navigationBarsPadding()
        ) {
            Row(modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                if (selectionMode) {
                    // Delete selected button
                    IconButton(
                        onClick = {
                            selectedPaths.value.forEach { path -> component.removeImage(path) }
                            selectedPaths.value = emptySet()
                            selectionMode = false
                        }
                    ) {
                        Icon(
                            imageVector = TablerIcons.Trash,
                            contentDescription = "Delete selected",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // Cancel selection
                    IconButton(onClick = {
                        selectionMode = false
                        selectedPaths.value = emptySet()
                    }) {
                        Icon(
                            imageVector = TablerIcons.X,
                            contentDescription = "Cancel selection",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    PhotoPickerWrapper(addImages = component::storeImages) { onClick ->
                        IconButton(onClick = onClick) {
                            Icon(
                                imageVector = TablerIcons.Upload,
                                contentDescription = "Upload",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(onClick = { selectedItem.value = null }) {
                        Icon(
                            imageVector = TablerIcons.X,
                            contentDescription = "Close",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            val rows = if (state.storedImages.size > 10) 2 else 1

            LazyHorizontalGrid(
                rows = GridCells.Fixed(rows),
                state = lazyListState,
                modifier = Modifier.fillMaxWidth().height(72.dp * rows + 8.dp * 2 + 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                itemsIndexed(state.storedImages) { i, (path, url) ->
                    val isSelected = selectedPaths.value.contains(path)
                    val isActive = !selectionMode && selectedItem.value?.url == url

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .combinedClickable(
                                onClick = {
                                    if (selectionMode) {
                                        selectedPaths.value = if (isSelected) {
                                            selectedPaths.value - path
                                        } else {
                                            selectedPaths.value + path
                                        }
                                        // Exit selection mode if nothing selected
                                        if (selectedPaths.value.isEmpty()) selectionMode = false
                                    } else {
                                        component.exchangeImage(selectedItem.value, url)
                                        selectedItem.value = selectedItem.value?.copy(url = url)
                                    }
                                },
                                onLongClick = {
                                    selectionMode = true
                                    selectedPaths.value = selectedPaths.value + path
                                }
                            )
                            .then(
                                when {
                                    isSelected -> Modifier.border(2.dp, MaterialTheme.colorScheme.error)
                                    isActive -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
                                    else -> Modifier
                                }
                            )
                    ) {
                        SubcomposeAsyncImage(
                            model = url,
                            loading = { ImageLoader() },
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
                            )
                        }
                    }
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
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 1.dp) {
        Box(
            modifier = modifier.wrapContentSize(Alignment.TopEnd)
        ) {
            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            ) {
                icon()
            }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(8.dp)
        ) {
            uuid?.let { uuid ->
                DropdownMenuItem(
                    text = { Text("Copy Id") },
                    contentPadding = PaddingValues(horizontal = 16.dp),
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
                contentPadding = PaddingValues(horizontal = 16.dp),
                onClick = {
                    addPlaceholder()
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Logout", color = MaterialTheme.colorScheme.error) },
                contentPadding = PaddingValues(horizontal = 16.dp),
                onClick = {
                    reset()
                    expanded = false
                }
            )
        }
        }
    }
}
