package com.surether.testcodingfield.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.surether.testcodingfield.model.CanvasTool
import com.surether.testcodingfield.model.InkPoint
import com.surether.testcodingfield.model.InkStroke
import com.surether.testcodingfield.ui.theme.colorFromArgb
import kotlin.math.hypot
import kotlin.math.max

@Composable
fun InkCanvas(
    strokes: List<InkStroke>,
    selectedTool: CanvasTool,
    penColorArgb: Long,
    strokeWidth: Float,
    onStrokeFinished: (InkStroke) -> Unit,
    onEraseAt: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    var activePoints by remember { mutableStateOf<List<InkPoint>>(emptyList()) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colorFromArgb(0xFFFFFEF7L))
            .pointerInput(selectedTool, penColorArgb, strokeWidth) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (selectedTool == CanvasTool.Eraser) {
                            onEraseAt(offset)
                        } else {
                            activePoints = listOf(offset.toInkPoint())
                        }
                    },
                    onDrag = { change, _ ->
                        if (selectedTool == CanvasTool.Eraser) {
                            onEraseAt(change.position)
                        } else {
                            activePoints = activePoints + change.position.toInkPoint()
                        }
                        change.consume()
                    },
                    onDragEnd = {
                        if (selectedTool != CanvasTool.Eraser && activePoints.size > 1) {
                            onStrokeFinished(
                                InkStroke(
                                    points = activePoints,
                                    colorArgb = penColorArgb,
                                    width = strokeWidth,
                                    isHighlighter = selectedTool == CanvasTool.Highlighter,
                                ),
                            )
                        }
                        activePoints = emptyList()
                    },
                    onDragCancel = {
                        activePoints = emptyList()
                    },
                )
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            strokes.forEach { stroke ->
                drawStroke(stroke)
            }
            if (activePoints.isNotEmpty()) {
                drawStroke(
                    InkStroke(
                        points = activePoints,
                        colorArgb = penColorArgb,
                        width = strokeWidth,
                        isHighlighter = selectedTool == CanvasTool.Highlighter,
                    ),
                )
            }
        }
    }
}

fun eraseIntersectingStroke(strokes: List<InkStroke>, at: Offset): List<InkStroke> =
    strokes.filterNot { stroke ->
        val radius = max(24f, stroke.width * 2.8f)
        stroke.points.any { point ->
            hypot((point.x - at.x).toDouble(), (point.y - at.y).toDouble()) <= radius
        }
    }

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStroke(stroke: InkStroke) {
    val color = colorFromArgb(stroke.colorArgb).copy(alpha = if (stroke.isHighlighter) 0.34f else 1f)
    val points = stroke.points
    if (points.isEmpty()) return
    if (points.size == 1) {
        drawCircle(color = color, radius = stroke.width / 2f, center = Offset(points.first().x, points.first().y))
        return
    }

    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { point -> lineTo(point.x, point.y) }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = stroke.width,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
}

private fun Offset.toInkPoint(): InkPoint =
    InkPoint(x = x, y = y, timeMillis = System.currentTimeMillis())
