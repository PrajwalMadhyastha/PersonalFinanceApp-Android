// =================================================================================
// FILE: ./app/src/main/java/io/pm/finlight/ui/components/DragDrop.kt
// REASON: NEW FILE - This file contains the state holder (`DragDropState`) and
// the remember function (`rememberDragDropState`) required to implement
// drag-and-drop functionality in a LazyColumn. It encapsulates the complex
// logic of tracking item positions, offsets, and scroll state during a drag
// operation, resolving the "Unresolved reference" errors in DashboardScreen.
// =================================================================================
package io.pm.finlight.ui.components

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
    var draggingItemIndex by mutableIntStateOf(-1)
    private var draggingItemInitialOffset by mutableIntStateOf(0)
    var draggingItemOffset by mutableFloatStateOf(0f)

    private val currentDraggingItemInfo: LazyListItemInfo?
        get() = lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == draggingItemIndex }

    fun onDragStart(offset: Offset) {
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
            ?.also {
                draggingItemIndex = it.index
                draggingItemInitialOffset = it.offset
            }
    }

    fun onDrag(offset: Offset) {
        draggingItemOffset += offset.y
        val draggingItem = currentDraggingItemInfo ?: return
        val startOffset = draggingItem.offset + draggingItemOffset
        val endOffset = startOffset + draggingItem.size
        val middleOffset = startOffset + (endOffset - startOffset) / 2f

        val targetItem = lazyListState.layoutInfo.visibleItemsInfo.find {
            middleOffset.toInt() in it.offset..it.offsetEnd && draggingItem.index != it.index
        }

        if (targetItem != null) {
            val targetIndex = targetItem.index
            onMove.invoke(draggingItemIndex, targetIndex)
            draggingItemIndex = targetIndex
        }
    }

    fun onDragEnd() {
        draggingItemIndex = -1
        draggingItemOffset = 0f
    }

    fun checkForOverScroll(): Float {
        val draggingItem = currentDraggingItemInfo ?: return 0f
        val startOffset = draggingItem.offset + draggingItemOffset
        val endOffset = startOffset + draggingItem.size

        return when {
            draggingItemOffset > 0 -> (endOffset - lazyListState.layoutInfo.viewportEndOffset + 50f).takeIf { it > 0 }
            draggingItemOffset < 0 -> (startOffset - 50f).takeIf { it < 0 }
            else -> 0f
        } ?: 0f
    }

    private val LazyListItemInfo.offsetEnd: Int
        get() = this.offset + this.size
}
