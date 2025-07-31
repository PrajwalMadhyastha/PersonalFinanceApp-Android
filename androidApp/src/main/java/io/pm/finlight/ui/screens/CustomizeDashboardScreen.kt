// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/screens/CustomizeDashboardScreen.kt
// REASON: FIX - The `graphicsLayer` modifier now correctly applies the translation
// from the rewritten DragDropState. This change, combined with the new state
// logic, ensures a smooth and stable drag-and-drop experience without any
// visual jumping or glitches.
// =================================================================================
package io.pm.finlight.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.pm.finlight.DashboardCardType
import io.pm.finlight.DashboardViewModel
import io.pm.finlight.ui.components.GlassPanel
import io.pm.finlight.ui.components.rememberDragDropState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CustomizeDashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel
) {
    val allCards by viewModel.allCards.collectAsState()
    val visibleCards by viewModel.visibleCards.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var overscrollJob by remember { mutableStateOf<Job?>(null) }
    val dragDropState = rememberDragDropState(onMove = viewModel::updateCardOrder)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customize Dashboard") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        LazyColumn(
            state = dragDropState.lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDrag = { change, offset ->
                            change.consume()
                            dragDropState.onDrag(offset)
                            if (overscrollJob?.isActive == true) return@detectDragGesturesAfterLongPress
                            dragDropState
                                .checkForOverScroll()
                                .takeIf { it != 0f }
                                ?.let { overscrollJob = coroutineScope.launch { dragDropState.lazyListState.scrollBy(it) } }
                                ?: run { overscrollJob?.cancel() }
                        },
                        onDragStart = { offset -> dragDropState.onDragStart(offset) },
                        onDragEnd = { dragDropState.onDragEnd() },
                        onDragCancel = { dragDropState.onDragEnd() }
                    )
                },
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(allCards, key = { _, item -> item.name }) { index, cardType ->
                val isBeingDragged = dragDropState.draggingItemKey == cardType.name
                val elevation by animateFloatAsState(if (isBeingDragged) 8f else 0f, label = "elevation")

                GlassPanel(
                    modifier = Modifier
                        .animateItemPlacement()
                        .graphicsLayer {
                            translationY = if (isBeingDragged) dragDropState.draggingItemTranslationY else 0f
                        }
                        .shadow(elevation.dp, MaterialTheme.shapes.extraLarge)
                ) {
                    // Hero Budget is not movable or hideable
                    if (cardType == DashboardCardType.HERO_BUDGET) {
                        ListItem(
                            headlineContent = { Text(cardType.name.toDisplayString()) },
                            supportingContent = { Text("This card is always visible and at the top.") },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
                        )
                    } else {
                        ListItem(
                            headlineContent = { Text(cardType.name.toDisplayString()) },
                            leadingContent = {
                                Switch(
                                    checked = visibleCards.contains(cardType),
                                    onCheckedChange = { viewModel.toggleCardVisibility(cardType) }
                                )
                            },
                            trailingContent = {
                                Icon(
                                    Icons.Default.DragHandle,
                                    contentDescription = "Drag to reorder"
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
                        )
                    }
                }
            }
        }
    }
}

private fun String.toDisplayString(): String {
    return this.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
}
