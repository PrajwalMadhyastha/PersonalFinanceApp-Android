// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/components/DragDrop.kt
// REASON: FIX - The drag-and-drop logic has been completely rewritten to be
// stable and smooth. The dragged item's visual offset is now calculated
// independently of the LazyColumn's recomposition, breaking the feedback loop
// that caused items to jump to the top or bottom of the list. The item now
// sticks to the user's finger correctly while other items animate smoothly.
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

    // The total accumulated vertical movement of the user's finger.
    private var draggingItemOffset by mutableFloatStateOf(0f)

    // A helper to get the current index of the item being dragged by its key.
    val draggingItemIndex: Int?
        get() = draggingItemKey?.let { key ->
            lazyListState.layoutInfo.visibleItemsInfo.find { it.key == key }?.index
        }

    // A reference to the LazyListItemInfo of the item currently being dragged.
    private val currentDraggingItem: LazyListItemInfo?
        get() = draggingItemIndex?.let {
            lazyListState.layoutInfo.visibleItemsInfo.find { item -> item.index == it }
        }

    // The final visual translation for the dragged item. This is now simply the
    // accumulated finger movement, completely decoupled from the list's own layout changes.
    val draggingItemTranslationY: Float
        get() = draggingItemOffset

    /**
     * Called when a drag gesture starts.
     * Finds the item under the pointer and initializes the drag state.
     */
    fun onDragStart(offset: Offset) {
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
            ?.also {
                // Prevent dragging the hero card at index 0
                if (it.index == 0) return
                draggingItemKey = it.key
            }
    }

    /**
     * Called for each movement during a drag gesture.
     * This function accumulates the finger's movement and detects when a swap should occur.
     */
    fun onDrag(offset: Offset) {
        draggingItemOffset += offset.y

        val draggingIndex = draggingItemIndex ?: return
        val draggingItem = currentDraggingItem ?: return

        // Calculate the current visual center of the dragged item.
        val draggedItemCenter = draggingItem.offset + draggingItemOffset + (draggingItem.size / 2f)

        // Find the item that the dragging item is hovering over.
        val targetItem = lazyListState.layoutInfo.visibleItemsInfo.find {
            it.key != draggingItemKey && it.index != 0 &&
                    draggedItemCenter in (it.offset.toFloat()..(it.offset + it.size).toFloat())
        }

        if (targetItem != null) {
            val from = draggingIndex
            val to = targetItem.index
            onMove(from, to)
        }
    }

    /**
     * Called when the drag gesture ends. Resets the state.
     */
    fun onDragEnd() {
        draggingItemKey = null
        draggingItemOffset = 0f
    }

    /**
     * Calculates the amount to scroll when the dragged item reaches the edge of the viewport.
     */
    fun checkForOverScroll(): Float {
        val draggingItem = currentDraggingItem ?: return 0f
        val viewportStartOffset = lazyListState.layoutInfo.viewportStartOffset
        val viewportEndOffset = lazyListState.layoutInfo.viewportEndOffset

        // The item's visual top position, including the drag offset.
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
