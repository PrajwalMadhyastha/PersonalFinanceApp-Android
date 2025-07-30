// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/components/DragDrop.kt
// REASON: REFACTOR - The entire drag-and-drop state logic has been rewritten to
// provide a smooth, jank-free reordering experience on the dashboard. The new
// implementation uses a stable key to identify the dragged item, preventing it
// from being "lost" during recomposition. The translation logic is also updated
// to correctly calculate the offset from the item's new position after a swap,
// resulting in a smooth drag that sticks to the user's finger.
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
    // --- UPDATED: Store the stable key of the item, not its transient index ---
    var draggingItemKey by mutableStateOf<Any?>(null)
        private set

    // The total offset of the finger from the start of the drag.
    private var draggingItemOffset by mutableFloatStateOf(0f)

    // The initial offset of the item when the drag started.
    private var initialDraggingItemOffset by mutableStateOf<Int?>(null)

    // A helper to get the current index of the item being dragged by its key.
    private val draggingItemIndex: Int?
        get() = draggingItemKey?.let { key ->
            lazyListState.layoutInfo.visibleItemsInfo.find { it.key == key }?.index
        }

    // A reference to the LazyListItemInfo of the item currently being dragged.
    private val currentDraggingItem: LazyListItemInfo?
        get() = draggingItemIndex?.let {
            lazyListState.layoutInfo.visibleItemsInfo.find { item -> item.index == it }
        }

    // The calculated translationY to be applied in the graphicsLayer.
    // This calculation is the key to smoothness: it's the total finger
    // movement minus the distance the list has already scrolled to place the item.
    val draggingItemTranslationY: Float
        get() = currentDraggingItem?.let {
            (initialDraggingItemOffset ?: 0) + draggingItemOffset - it.offset
        } ?: 0f

    /**
     * Called when a drag gesture starts.
     * Finds the item under the pointer and initializes the drag state.
     * Prevents dragging the first item (the Hero Card).
     */
    fun onDragStart(offset: Offset) {
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
            ?.also {
                // Prevent dragging the hero card at index 0
                if (it.index == 0) return

                // --- UPDATED: Store the key and initial offset ---
                draggingItemKey = it.key
                initialDraggingItemOffset = it.offset
            }
    }

    /**
     * Called for each movement during a drag gesture.
     * This function accumulates the finger's movement and detects when a swap should occur.
     */
    fun onDrag(offset: Offset) {
        draggingItemOffset += offset.y

        val currentDraggingIndex = draggingItemIndex ?: return
        val initialOffset = initialDraggingItemOffset ?: return
        val draggingItem = currentDraggingItem ?: return

        // Calculate the absolute visual position of the item's center.
        val itemCenter = initialOffset + draggingItemOffset + (draggingItem.size / 2f)

        // Find the item that the dragging item is hovering over.
        val targetItem = lazyListState.layoutInfo.visibleItemsInfo.find {
            // Exclude the item being dragged and the non-movable hero card at index 0.
            it.key != draggingItemKey && it.index != 0 &&
                    // Check if the center of the dragging item is within the bounds of the target item.
                    itemCenter.toInt() in it.offset..(it.offset + it.size)
        }

        if (targetItem != null) {
            val from = currentDraggingIndex
            val to = targetItem.index
            onMove(from, to)
            // After moving, the initial offset needs to be updated to the target's
            // offset to prevent a jump, as the item's natural position has changed.
            initialDraggingItemOffset = targetItem.offset
        }
    }

    /**
     * Called when the drag gesture ends. Resets the state.
     */
    fun onDragEnd() {
        draggingItemKey = null
        draggingItemOffset = 0f
        initialDraggingItemOffset = null
    }

    /**
     * Calculates the amount to scroll when the dragged item reaches the edge of the viewport.
     */
    fun checkForOverScroll(): Float {
        val draggingItem = currentDraggingItem ?: return 0f
        val viewportStartOffset = lazyListState.layoutInfo.viewportStartOffset
        val viewportEndOffset = lazyListState.layoutInfo.viewportEndOffset

        val itemTop = draggingItem.offset + draggingItemTranslationY
        val itemBottom = itemTop + draggingItem.size

        val scrollAmount = 40f

        return when {
            itemBottom > viewportEndOffset -> scrollAmount
            itemTop < viewportStartOffset -> -scrollAmount
            else -> 0f
        }
    }
}
