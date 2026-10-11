package com.justnpcx.mirage.ui.compose

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.justnpcx.mirage.R

/**
 * Node list — the second Compose vertical slice.
 *
 * Shows the nodes of the current group with a latency chip; tapping one selects it.
 */
@Composable
fun NodeListScreen(
    state: NodeListState,
    onSelect: (Long) -> Unit,
    onMove: (Int, Int) -> Unit,
    onMoveFinished: () -> Unit,
    onEdit: (Long) -> Unit,
    onShare: (Long) -> Unit,
    onPing: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var menuNode by remember { mutableStateOf<NodeItem?>(null) }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    Surface(modifier = modifier.fillMaxSize()) {
        if (state.nodes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.node_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Surface
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(state.nodes, key = { _, node -> node.id }) { index, node ->
                val isDragging = index == draggingIndex
                NodeCard(
                    node = node,
                    onClick = { onSelect(node.id) },
                    onMenu = { menuNode = node },
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            if (isDragging) {
                                translationY = dragOffset
                                scaleX = 1.02f
                                scaleY = 1.02f
                                alpha = 0.94f
                            }
                        }
                        .pointerInput(node.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingIndex = index
                                    dragOffset = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount.y
                                    val from = draggingIndex
                                    if (from < 0) return@detectDragGesturesAfterLongPress
                                    val target = targetIndex(listState, from, dragOffset)
                                    if (target != from) {
                                        onMove(from, target)
                                        draggingIndex = target
                                        dragOffset = 0f
                                    }
                                },
                                onDragEnd = {
                                    draggingIndex = -1
                                    dragOffset = 0f
                                    onMoveFinished()
                                },
                                onDragCancel = {
                                    draggingIndex = -1
                                    dragOffset = 0f
                                    onMoveFinished()
                                },
                            )
                        },
                )
            }
        }
    }
}

@Composable
private fun NodeCard(
    node: NodeItem,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (node.selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = node.name.ifBlank { node.address },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = node.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            LatencyChip(ping = node.ping)
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onMenu) {
                Text(text = "\u22ee", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

/** Which slot the dragged card currently sits over, based on the visible layout. */
private fun targetIndex(listState: LazyListState, currentIndex: Int, offset: Float): Int {
    val layoutInfo = listState.layoutInfo
    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentIndex }
        ?: return currentIndex
    val center = itemInfo.offset + itemInfo.size / 2f + offset
    return layoutInfo.visibleItemsInfo
        .firstOrNull { center >= it.offset && center <= it.offset + it.size }
        ?.index
        ?: currentIndex
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodeActionSheet(
    node: NodeItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onPing: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = node.name.ifBlank { node.address },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Text(
                text = node.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(12.dp))
            SheetAction(stringResource(R.string.node_action_edit), onEdit)
            SheetAction(stringResource(R.string.node_action_share), onShare)
            SheetAction(stringResource(R.string.node_action_ping), onPing)
            SheetAction(stringResource(R.string.node_action_delete), onDelete)
        }
    }
}

@Composable
private fun SheetAction(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun LatencyChip(ping: Int) {
    val (text, color) = when {
        ping <= 0 -> stringResource(R.string.node_untested) to MaterialTheme.colorScheme.outline
        ping < 200 -> "$ping ms" to MaterialTheme.colorScheme.tertiary
        ping < 500 -> "$ping ms" to MaterialTheme.colorScheme.secondary
        else -> "$ping ms" to MaterialTheme.colorScheme.error
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = (color as? Color ?: MaterialTheme.colorScheme.outline).copy(alpha = 0.14f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
