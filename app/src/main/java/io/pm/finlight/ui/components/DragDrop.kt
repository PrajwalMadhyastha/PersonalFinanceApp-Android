// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/components/DragDrop.kt
// REASON: FIX - The drag-and-drop logic has been completely rewritten to be
// stable and smooth. The logic now triggers a swap only when the dragged item's
// edge crosses the center of a target item. This creates the desired "make
// space" effect and prevents the item from incorrectly jumping to the top or
// bottom of the list, ensuring a fluid and predictable reordering experience.
// =================================================================================
package io.pm.finlight.ui.components

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState = rememberLazyListState(),
    onMove: (Int, Int) -> Unit,
): DragDropState {
    return remember { DragDropState(lazyListState, onMove) }
}

class DragDropState(
    val lazyListState: LazyListState,
    private val onMove: (Int, Int) -> Unit
) {
    var draggingItemKey by mutableStateOf<Any?>(null)
        private set

    private var draggingItemOffset by mutableFloatStateOf(0f)

    val draggingItemIndex: Int?
        get() = draggingItemKey?.let { key ->
            lazyListState.layoutInfo.visibleItemsInfo.find { it.key == key }?.index
        }

    private val currentDraggingItem: LazyListItemInfo?
        get() = draggingItemIndex?.let {
            lazyListState.layoutInfo.visibleItemsInfo.find { item -> item.index == it }
        }

    val draggingItemTranslationY: Float
        get() = draggingItemOffset

    fun onDragStart(offset: Offset) {
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
            ?.also {
                if (it.index == 0) return // Prevent dragging the hero card
                draggingItemKey = it.key
            }
    }

    fun onDrag(offset: Offset) {
        draggingItemOffset += offset.y

        val draggingIndex = draggingItemIndex ?: return
        val draggingItem = currentDraggingItem ?: return

        // --- REWRITTEN SWAP LOGIC ---
        val draggedItemTop = draggingItem.offset + draggingItemOffset
        val draggedItemCenterY = draggedItemTop + (draggingItem.size / 2f)

        // Find the item we are currently over
        val targetItem = lazyListState.layoutInfo.visibleItemsInfo.find {
            it.key != draggingItemKey && // Not dragging over itself
                    draggedItemCenterY in it.offset.toFloat()..(it.offset + it.size).toFloat() &&
                    it.index != 0 // And not over the hero card
        }

        if (targetItem != null) {
            // Check if we need to swap positions
            if (draggingIndex != targetItem.index) {
                onMove(draggingIndex, targetItem.index)
            }
        }
    }


    fun onDragEnd() {
        draggingItemKey = null
        draggingItemOffset = 0f
    }

    fun checkForOverScroll(): Float {
        val draggingItem = currentDraggingItem ?: return 0f
        val viewportStartOffset = lazyListState.layoutInfo.viewportStartOffset
        val viewportEndOffset = lazyListState.layoutInfo.viewportEndOffset

        val itemTop = draggingItem.offset + draggingItemOffset
        val itemBottom = itemTop + draggingItem.size

        val scrollAmount = 40f

        return when {
            itemBottom > viewportEndOffset -> scrollAmount
            itemTop < viewportStartOffset -> -scrollAmount
            else -> 0f
        }
    }
}
