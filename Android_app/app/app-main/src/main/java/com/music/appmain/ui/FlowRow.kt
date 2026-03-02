package com.music.appmain.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 自定义 FlowRow 布局组件
 * 用于实现感知芯片的流式布局
 */
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: androidx.compose.foundation.layout.Arrangement.Horizontal = androidx.compose.foundation.layout.Arrangement.Start,
    verticalArrangement: androidx.compose.foundation.layout.Arrangement.Vertical = androidx.compose.foundation.layout.Arrangement.Top,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val hGapPx = 12.dp.roundToPx()
        val vGapPx = 16.dp.roundToPx()
        
        val rows = mutableListOf<List<Placeable>>()
        val rowWidths = mutableListOf<Int>()
        val rowHeights = mutableListOf<Int>()
        
        var currentRow = mutableListOf<Placeable>()
        var currentRowWidth = 0
        var currentRowHeight = 0
        
        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints)
            
            if (currentRow.isNotEmpty() && currentRowWidth + hGapPx + placeable.width > constraints.maxWidth) {
                rows.add(currentRow)
                rowWidths.add(currentRowWidth)
                rowHeights.add(currentRowHeight)
                
                currentRow = mutableListOf()
                currentRowWidth = 0
                currentRowHeight = 0
            }
            
            currentRow.add(placeable)
            currentRowWidth += if (currentRow.size == 1) placeable.width else hGapPx + placeable.width
            currentRowHeight = max(currentRowHeight, placeable.height)
        }
        
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
            rowWidths.add(currentRowWidth)
            rowHeights.add(currentRowHeight)
        }
        
        val width = if (constraints.hasFixedWidth) constraints.maxWidth else rowWidths.maxOrNull() ?: 0
        val height = rowHeights.sum() + (rows.size - 1).coerceAtLeast(0) * vGapPx
        
        layout(width, height) {
            var y = 0
            
            rows.forEachIndexed { rowIndex, row ->
                var x = when (horizontalArrangement) {
                    androidx.compose.foundation.layout.Arrangement.End -> width - rowWidths[rowIndex]
                    androidx.compose.foundation.layout.Arrangement.Center -> (width - rowWidths[rowIndex]) / 2
                    else -> 0
                }
                
                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + hGapPx
                }
                
                y += rowHeights[rowIndex] + vGapPx
            }
        }
    }
}
