package com.surether.testcodingfield.ink

import com.surether.testcodingfield.model.InkPoint
import com.surether.testcodingfield.model.InkStroke
import kotlin.math.abs
import kotlin.math.hypot

data class RecognitionResult(
    val expression: String,
    val source: String,
    val confidence: Float,
)

class HandwritingMathRecognizer {
    fun recognize(strokes: List<InkStroke>): RecognitionResult? {
        val recentStrokes = strokes.takeLast(MAX_STROKES)
        val points = recentStrokes.flatMap { it.points }
        if (points.size < MIN_POINTS) return null

        val bounds = Bounds.from(points) ?: return null
        if (bounds.width < MIN_SIZE || bounds.height < MIN_SIZE) return null

        val ordered = points.sortedBy { it.timeMillis }
        val first = ordered.first()
        val last = ordered.last()
        val dx = last.x - first.x
        val dy = last.y - first.y
        val width = bounds.width.coerceAtLeast(1f)
        val height = bounds.height.coerceAtLeast(1f)
        val aspect = width / height
        val signChanges = verticalDirectionChanges(ordered)
        val centerY = averageY(ordered.filter { it.x in bounds.centerX - width * 0.18f..bounds.centerX + width * 0.18f })
        val endY = averageY(
            ordered.filter {
                it.x <= bounds.left + width * 0.18f || it.x >= bounds.right - width * 0.18f
            },
        )

        return when {
            aspect > 2.8f && height / width < 0.18f ->
                RecognitionResult("y = 0", "horizontal-stroke", 0.70f)

            signChanges >= 4 && aspect > 1.3f ->
                RecognitionResult("y = sin(x)", "wave-stroke", 0.74f)

            centerY != null && endY != null && centerY > endY + height * 0.20f && recentStrokes.size <= 2 ->
                RecognitionResult("y = abs(x)", "v-stroke", 0.73f)

            centerY != null && endY != null && centerY > endY + height * 0.16f ->
                RecognitionResult("y = x^2", "u-stroke", 0.76f)

            abs(dx) > width * 0.60f && abs(dy) > height * 0.55f && dx.sign() != dy.sign() ->
                RecognitionResult("y = x", "ascending-line", 0.72f)

            abs(dx) > width * 0.60f && abs(dy) > height * 0.55f && dx.sign() == dy.sign() ->
                RecognitionResult("y = -x", "descending-line", 0.72f)

            recentStrokes.size >= 3 && strokeLength(ordered).toDouble() / hypot(width.toDouble(), height.toDouble()) > 2.4 ->
                RecognitionResult("y = x^3", "compound-curve", 0.58f)

            else ->
                RecognitionResult("y = x", "fallback-line", 0.42f)
        }
    }

    private fun verticalDirectionChanges(points: List<InkPoint>): Int {
        var changes = 0
        var previousSign = 0
        points.zipWithNext().forEach { (a, b) ->
            val sign = (b.y - a.y).sign()
            if (sign != 0 && previousSign != 0 && sign != previousSign) changes += 1
            if (sign != 0) previousSign = sign
        }
        return changes
    }

    private fun averageY(points: List<InkPoint>): Float? =
        if (points.isEmpty()) null else points.map { it.y }.average().toFloat()

    private fun strokeLength(points: List<InkPoint>): Float =
        points.zipWithNext().sumOf { (a, b) -> hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()) }.toFloat()

    private fun Float.sign(): Int =
        when {
            this > 0f -> 1
            this < 0f -> -1
            else -> 0
        }

    private data class Bounds(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
    ) {
        val width: Float = right - left
        val height: Float = bottom - top
        val centerX: Float = left + width / 2f

        companion object {
            fun from(points: List<InkPoint>): Bounds? {
                if (points.isEmpty()) return null
                return Bounds(
                    left = points.minOf { it.x },
                    top = points.minOf { it.y },
                    right = points.maxOf { it.x },
                    bottom = points.maxOf { it.y },
                )
            }
        }
    }

    private companion object {
        const val MAX_STROKES = 12
        const val MIN_POINTS = 8
        const val MIN_SIZE = 18f
    }
}
