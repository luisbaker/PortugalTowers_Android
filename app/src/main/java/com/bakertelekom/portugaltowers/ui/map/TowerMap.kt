package com.bakertelekom.portugaltowers.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.bakertelekom.portugaltowers.domain.Tower
import com.bakertelekom.portugaltowers.ui.components.composeColor
import kotlin.math.max

@Composable
fun TowerMap(
    towers: List<Tower>,
    onTowerSelected: (Tower) -> Unit,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val surface = MaterialTheme.colorScheme.surface
    val land = MaterialTheme.colorScheme.secondaryContainer
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.75f, 8f)
                        offset += pan
                        val maxPan = max(size.width, size.height).toFloat() * scale
                        offset = Offset(
                            x = offset.x.coerceIn(-maxPan, maxPan),
                            y = offset.y.coerceIn(-maxPan, maxPan),
                        )
                    }
                }
                .pointerInput(towers, scale, offset, canvasSize) {
                    detectTapGestures { tap ->
                        nearestTower(tap, towers, canvasSize, scale, offset)?.let(onTowerSelected)
                    }
                },
        ) {
            drawRect(surface)
            drawRoundRect(
                color = land,
                topLeft = Offset(28.dp.toPx(), 36.dp.toPx()),
                size = Size(size.width - 56.dp.toPx(), size.height - 88.dp.toPx()),
                cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx()),
            )
            drawRoundRect(
                color = outline,
                topLeft = Offset(28.dp.toPx(), 36.dp.toPx()),
                size = Size(size.width - 56.dp.toPx(), size.height - 88.dp.toPx()),
                cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx()),
            )

            towers.forEach { tower ->
                val point = tower.toScreenPoint(size, scale, offset)
                drawCircle(
                    color = tower.primaryOperator.composeColor(),
                    radius = max(2.5.dp.toPx(), 4.6.dp.toPx() / scale),
                    center = point,
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            tonalElevation = 2.dp,
        ) {
            Text(
                text = "Arrasta para mover. Junta os dedos para zoom. Toca numa torre.",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

private fun nearestTower(
    tap: Offset,
    towers: List<Tower>,
    canvasSize: IntSize,
    scale: Float,
    offset: Offset,
): Tower? {
    if (canvasSize.width <= 0 || canvasSize.height <= 0) return null
    val size = Size(canvasSize.width.toFloat(), canvasSize.height.toFloat())
    var best: Tower? = null
    var bestDistance = Float.MAX_VALUE
    towers.forEach { tower ->
        val point = tower.toScreenPoint(size, scale, offset)
        val distance = (point - tap).getDistanceSquared()
        if (distance < bestDistance) {
            bestDistance = distance
            best = tower
        }
    }
    return best.takeIf { bestDistance < 28f * 28f }
}

private fun Tower.toScreenPoint(
    size: Size,
    scale: Float,
    offset: Offset,
): Offset {
    val base = Offset(
        x = longitudeToX(longitude, size.width),
        y = latitudeToY(latitude, size.height),
    )
    val center = Offset(size.width / 2f, size.height / 2f)
    return Offset(
        x = ((base.x - center.x) * scale) + center.x + offset.x,
        y = ((base.y - center.y) * scale) + center.y + offset.y,
    )
}

private fun longitudeToX(longitude: Double, width: Float): Float =
    (((longitude + 32.5) / 26.5) * width).toFloat().coerceIn(0f, width)

private fun latitudeToY(latitude: Double, height: Float): Float =
    (height - ((latitude - 32.0) / 10.5) * height).toFloat().coerceIn(0f, height)
