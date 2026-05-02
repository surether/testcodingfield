package com.surether.testcodingfield.model

import java.util.UUID

enum class CanvasTool {
    Pen,
    Highlighter,
    Eraser,
}

data class InkPoint(
    val x: Float,
    val y: Float,
    val timeMillis: Long,
)

data class InkStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<InkPoint>,
    val colorArgb: Long,
    val width: Float,
    val isHighlighter: Boolean = false,
)

data class MathCard(
    val id: String = UUID.randomUUID().toString(),
    val expression: String,
    val source: String,
    val confidence: Float,
    val createdAt: Long = System.currentTimeMillis(),
)

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val strokes: List<InkStroke> = emptyList(),
    val mathCards: List<MathCard> = emptyList(),
)

fun newBlankNote(index: Int): Note =
    Note(title = "노트 $index")
