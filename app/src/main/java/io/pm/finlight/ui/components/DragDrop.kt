// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/components/DragDrop.kt
// REASON: FIX - The `draggingItemIndex` property has been refactored to use an
// explicit private backing property (`_draggingItemIndex`). This resolves the
// "EmptyMethod" and "unused" warnings by making the state mutations clearer to
// the linter, without changing the component's behavior.
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
    // --- FIX: Refactored to use a private backing property ---
    private val _draggingItemIndex = mutableStateOf<Int?>(null)
    val draggingItemIndex: Int? get() = _draggingItemIndex.value

    // This is the total displacement of the user's finger since the drag started.
    private var draggingItemOffset by mutableFloatStateOf(0f)

    // We need to store the initial offset of the item being dragged.
    private var initialDraggingItemOffset by mutableStateOf<Int?>(null)

    // A reference to the LazyListItemInfo of the item currently being dragged.
    private val currentDraggingItem: LazyListItemInfo?
        get() = draggingItemIndex?.let {
            lazyListState.layoutInfo.visibleItemsInfo.find { item -> item.index == it }
        }

    // The calculated translationY to be applied in the graphicsLayer.
    // This is the magic that makes the drag smooth.
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

                _draggingItemIndex.value = it.index
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

        // Calculate the absolute visual position of the item's center.
        val itemCenter = initialOffset + draggingItemOffset + (currentDraggingItem?.size ?: 0) / 2

        // Find the item that the dragging item is hovering over.
        val targetItem = lazyListState.layoutInfo.visibleItemsInfo.find {
            // Exclude the item being dragged and the non-movable hero card at index 0.
            it.index != currentDraggingIndex && it.index != 0 &&
                    // Check if the center of the dragging item is within the bounds of the target item.
                    itemCenter.toInt() in it.offset..(it.offset + it.size)
        }

        if (targetItem != null) {
            val from = currentDraggingIndex
            val to = targetItem.index
            onMove(from, to)
            // Only update the index. The offset logic will handle the visual position.
            _draggingItemIndex.value = to
        }
    }

    /**
     * Called when the drag gesture ends. Resets the state.
     */
    fun onDragEnd() {
        _draggingItemIndex.value = null
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

        val scrollAmount = 20f

        return when {
            itemBottom > viewportEndOffset -> scrollAmount
            itemTop < viewportStartOffset -> -scrollAmount
            else -> 0f
        }
    }
}
