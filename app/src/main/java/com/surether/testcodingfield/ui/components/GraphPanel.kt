package com.surether.testcodingfield.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.surether.testcodingfield.math.FunctionExpression
import com.surether.testcodingfield.ui.theme.AppPalette
import com.surether.testcodingfield.ui.theme.colorFromArgb
import kotlin.math.abs

@Composable
fun GraphPanel(
    expression: String,
    onExpressionChange: (String) -> Unit,
    onPinExpression: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val parsed = remember(expression) { runCatching { FunctionExpression.parse(expression) } }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = expression,
                onValueChange = onExpressionChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("수식") },
            )
            Button(onClick = onPinExpression, enabled = parsed.isSuccess) {
                Icon(Icons.Default.Check, contentDescription = "저장")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawGraphGrid()
                parsed.getOrNull()?.let { expression ->
                    val path = Path()
                    var active = false
                    val samples = 360
                    repeat(samples + 1) { index ->
                        val x = -GRAPH_RANGE + (GRAPH_RANGE * 2.0 * index / samples)
                        val y = expression.evaluate(x)
                        if (y.isFinite() && abs(y) <= GRAPH_RANGE * 8) {
                            val pointX = ((x + GRAPH_RANGE) / (GRAPH_RANGE * 2.0) * size.width).toFloat()
                            val pointY = ((GRAPH_RANGE - y) / (GRAPH_RANGE * 2.0) * size.height).toFloat()
                            if (active) path.lineTo(pointX, pointY) else path.moveTo(pointX, pointY)
                            active = true
                        } else {
                            active = false
                        }
                    }
                    drawPath(
                        path = path,
                        color = colorFromArgb(AppPalette.Graph),
                        style = Stroke(width = 4f),
                    )
                }
            }

            if (parsed.isFailure) {
                Text(
                    text = "그래프를 그릴 수 없는 수식",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Text(
            text = parsed.getOrNull()?.normalizedSource ?: "수식 확인 필요",
            color = if (parsed.isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGraphGrid() {
    val gridColor = colorFromArgb(0xFFE5E7EBL)
    val axisColor = colorFromArgb(0xFF64748BL)

    repeat(9) { index ->
        val fraction = (index + 1) / 10f
        drawLine(
            color = gridColor,
            start = androidx.compose.ui.geometry.Offset(size.width * fraction, 0f),
            end = androidx.compose.ui.geometry.Offset(size.width * fraction, size.height),
            strokeWidth = 1f,
        )
        drawLine(
            color = gridColor,
            start = androidx.compose.ui.geometry.Offset(0f, size.height * fraction),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height * fraction),
            strokeWidth = 1f,
        )
    }

    drawLine(
        color = axisColor,
        start = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
        end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
        strokeWidth = 2f,
    )
    drawLine(
        color = axisColor,
        start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
        end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
        strokeWidth = 2f,
    )
}

private const val GRAPH_RANGE = 10.0
