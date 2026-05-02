package com.surether.testcodingfield.data

import android.content.Context
import com.surether.testcodingfield.model.InkPoint
import com.surether.testcodingfield.model.InkStroke
import com.surether.testcodingfield.model.MathCard
import com.surether.testcodingfield.model.Note
import com.surether.testcodingfield.model.newBlankNote
import org.json.JSONArray
import org.json.JSONObject

class NoteRepository(context: Context) {
    private val preferences = context.getSharedPreferences("testcodingfield_notes", Context.MODE_PRIVATE)

    fun loadNotes(): List<Note> {
        val raw = preferences.getString(KEY_NOTES, null) ?: return listOf(newBlankNote(1))
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index -> array.getJSONObject(index).toNote() }
        }.getOrElse {
            listOf(newBlankNote(1))
        }
    }

    fun saveNotes(notes: List<Note>) {
        val payload = JSONArray()
        notes.forEach { note -> payload.put(note.toJson()) }
        preferences.edit().putString(KEY_NOTES, payload.toString()).apply()
    }

    private fun JSONObject.toNote(): Note =
        Note(
            id = getString("id"),
            title = optString("title", "노트"),
            updatedAt = optLong("updatedAt", System.currentTimeMillis()),
            strokes = optJSONArray("strokes").toStrokeList(),
            mathCards = optJSONArray("mathCards").toMathCardList(),
        )

    private fun JSONArray?.toStrokeList(): List<InkStroke> {
        if (this == null) return emptyList()
        return List(length()) { strokeIndex ->
            val stroke = getJSONObject(strokeIndex)
            InkStroke(
                id = stroke.optString("id"),
                points = stroke.optJSONArray("points").toPointList(),
                colorArgb = stroke.optLong("colorArgb", DEFAULT_INK),
                width = stroke.optDouble("width", 5.5).toFloat(),
                isHighlighter = stroke.optBoolean("isHighlighter", false),
            )
        }
    }

    private fun JSONArray?.toPointList(): List<InkPoint> {
        if (this == null) return emptyList()
        return List(length()) { pointIndex ->
            val point = getJSONObject(pointIndex)
            InkPoint(
                x = point.optDouble("x").toFloat(),
                y = point.optDouble("y").toFloat(),
                timeMillis = point.optLong("timeMillis"),
            )
        }
    }

    private fun JSONArray?.toMathCardList(): List<MathCard> {
        if (this == null) return emptyList()
        return List(length()) { cardIndex ->
            val card = getJSONObject(cardIndex)
            MathCard(
                id = card.optString("id"),
                expression = card.optString("expression"),
                source = card.optString("source"),
                confidence = card.optDouble("confidence", 0.0).toFloat(),
                createdAt = card.optLong("createdAt", System.currentTimeMillis()),
            )
        }
    }

    private fun Note.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("title", title)
            .put("updatedAt", updatedAt)
            .put("strokes", JSONArray().also { array -> strokes.forEach { array.put(it.toJson()) } })
            .put("mathCards", JSONArray().also { array -> mathCards.forEach { array.put(it.toJson()) } })

    private fun InkStroke.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("colorArgb", colorArgb)
            .put("width", width)
            .put("isHighlighter", isHighlighter)
            .put("points", JSONArray().also { array -> points.forEach { array.put(it.toJson()) } })

    private fun InkPoint.toJson(): JSONObject =
        JSONObject()
            .put("x", x)
            .put("y", y)
            .put("timeMillis", timeMillis)

    private fun MathCard.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("expression", expression)
            .put("source", source)
            .put("confidence", confidence)
            .put("createdAt", createdAt)

    private companion object {
        const val KEY_NOTES = "notes"
        const val DEFAULT_INK = 0xFF111827L
    }
}
