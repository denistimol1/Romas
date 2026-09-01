package com.example.boxpacker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.boxpacker.packing.PlacedBox

enum class Projection { FRONT, SIDE, TOP }

private val palette = listOf(
    Color(0xFF64B5F6), Color(0xFF81C784), Color(0xFFFFB74D),
    Color(0xFFE57373), Color(0xFFBA68C8), Color(0xFF4DB6AC),
    Color(0xFFF06292), Color(0xFFA1887D)
)

/**
 * Рисует схематичный чертёж одной из проекций ячейки с коробками.
 *
 * FRONT — вид по оси Y (плоскость X-Z, ширина x высота)
 * SIDE  — вид по оси X (плоскость Y-Z, глубина x высота)
 * TOP   — вид по оси Z (плоскость X-Y, ширина x глубина)
 */
@Composable
fun ProjectionCanvas(
    cellW: Int, cellD: Int, cellH: Int,
    boxes: List<PlacedBox>,
    projection: Projection,
    label: String
) {
    val (planeW, planeH) = when (projection) {
        Projection.FRONT -> cellW to cellH
        Projection.SIDE -> cellD to cellH
        Projection.TOP -> cellW to cellD
    }

    Text(label)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .aspectRatio(planeW.toFloat() / planeH.toFloat())
    ) {
        val scale = minOf(size.width / planeW, size.height / planeH)
        val offsetX = (size.width - planeW * scale) / 2f
        val offsetY = (size.height - planeH * scale) / 2f

        fun toCanvas(px: Float, pyFromBottom: Float): Offset {
            // переворачиваем ось Y экрана, чтобы "низ" ячейки был внизу рисунка
            val screenY = (planeH - pyFromBottom) * scale + offsetY
            return Offset(px * scale + offsetX, screenY)
        }

        // контур ячейки
        drawRect(
            color = Color.Black,
            topLeft = Offset(offsetX, offsetY),
            size = Size(planeW * scale, planeH * scale),
            style = Stroke(width = 3f)
        )

        boxes.forEachIndexed { index, b ->
            val (u, v, uu, vv) = when (projection) {
                Projection.FRONT -> listOf(b.x, b.z, b.w, b.h)
                Projection.SIDE -> listOf(b.y, b.z, b.d, b.h)
                Projection.TOP -> listOf(b.x, b.y, b.w, b.d)
            }.map { it.toFloat() }

            val topLeft = toCanvas(u, v + vv)
            val color = palette[index % palette.size]

            drawRect(
                color = color.copy(alpha = 0.55f),
                topLeft = topLeft,
                size = Size(uu * scale, vv * scale)
            )
            drawRect(
                color = Color.Black,
                topLeft = topLeft,
                size = Size(uu * scale, vv * scale),
                style = Stroke(width = 1.5f)
            )
        }
    }
}
