// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/components/DragDrop.kt
// REASON: REFACTOR - The entire drag-and-drop state logic has been rewritten to
// provide a smooth, jank-free reordering experience on the dashboard. The new
// implementation decouples the dragged item's visual translation from the
// LazyColumn's recomposition, making it stick to the user's finger while other
// items animate smoothly into place.
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
    // Index of the item being dragged.
    private var _draggingItemIndex by mutableStateOf<Int?>(null)
    val draggingItemIndex: Int? get() = _draggingItemIndex

    // The total offset of the finger from the start of the drag.
    private var draggingItemOffset by mutableFloatStateOf(0f)

    // The initial offset of the item when the drag started. This is now adjusted
    // during swaps to ensure smoothness.
    private var initialDraggingItemOffset by mutableFloatStateOf(0f)

    // A reference to the LazyListItemInfo of the item currently being dragged.
    private val currentDraggingItem: LazyListItemInfo?
        get() = draggingItemIndex?.let {
            lazyListState.layoutInfo.visibleItemsInfo.find { item -> item.index == it }
        }

    // The calculated translationY to be applied in the graphicsLayer.
    // This is now purely based on the user's finger movement.
    val draggingItemTranslationY: Float
        get() = draggingItemOffset

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

                _draggingItemIndex = it.index
                initialDraggingItemOffset = it.offset.toFloat()
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
        val draggedItemCenter = initialDraggingItemOffset + draggingItemOffset + (draggingItem.size / 2f)

        // Find the item that the dragging item is hovering over.
        val targetItem = lazyListState.layoutInfo.visibleItemsInfo.find {
            it.index != draggingIndex && it.index != 0 && // Not itself, not the hero card
                    draggedItemCenter in (it.offset.toFloat()..(it.offset + it.size).toFloat())
        }

        if (targetItem != null) {
            val from = draggingIndex
            val to = targetItem.index

            // When a swap occurs, we adjust the initial offset. This prevents a visual jump
            // because the item's "natural" position in the list is about to change.
            val diff = lazyListState.layoutInfo.visibleItemsInfo.find { it.index == from }!!.offset -
                    lazyListState.layoutInfo.visibleItemsInfo.find { it.index == to }!!.offset
            initialDraggingItemOffset -= diff

            // Update the index and notify the caller of the move.
            _draggingItemIndex = to
            onMove(from, to)
        }
    }

    /**
     * Called when the drag gesture ends. Resets the state.
     */
    fun onDragEnd() {
        _draggingItemIndex = null
        draggingItemOffset = 0f
        initialDraggingItemOffset = 0f
    }

    /**
     * Calculates the amount to scroll when the dragged item reaches the edge of the viewport.
     */
    fun checkForOverScroll(): Float {
        val draggingItem = currentDraggingItem ?: return 0f
        val viewportStartOffset = lazyListState.layoutInfo.viewportStartOffset
        val viewportEndOffset = lazyListState.layoutInfo.viewportEndOffset

        // The item's visual top position, including the drag offset.
        val itemTop = initialDraggingItemOffset + draggingItemOffset
        val itemBottom = itemTop + draggingItem.size

        val scrollAmount = 40f // Increased for a slightly faster scroll

        return when {
            itemBottom > viewportEndOffset -> scrollAmount
            itemTop < viewportStartOffset -> -scrollAmount
            else -> 0f
        }
    }
}
