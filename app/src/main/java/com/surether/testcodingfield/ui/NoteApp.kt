package com.surether.testcodingfield.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.surether.testcodingfield.data.NoteRepository
import com.surether.testcodingfield.ink.HandwritingMathRecognizer
import com.surether.testcodingfield.model.CanvasTool
import com.surether.testcodingfield.model.InkStroke
import com.surether.testcodingfield.model.MathCard
import com.surether.testcodingfield.model.Note
import com.surether.testcodingfield.model.newBlankNote
import com.surether.testcodingfield.ui.components.GraphPanel
import com.surether.testcodingfield.ui.components.InkCanvas
import com.surether.testcodingfield.ui.components.eraseIntersectingStroke
import com.surether.testcodingfield.ui.theme.AppPalette
import com.surether.testcodingfield.ui.theme.colorFromArgb

@Composable
fun NoteApp(repository: NoteRepository) {
    val recognizer = remember { HandwritingMathRecognizer() }
    var notes by remember { mutableStateOf(repository.loadNotes().ifEmpty { listOf(newBlankNote(1)) }) }
    var selectedNoteId by remember { mutableStateOf(notes.first().id) }
    var selectedTool by remember { mutableStateOf(CanvasTool.Pen) }
    var selectedColor by remember { mutableStateOf(AppPalette.Ink) }
    var strokeWidth by remember { mutableStateOf(5.5f) }
    var expression by remember { mutableStateOf("y = x^2") }
    var recognitionDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(notes) {
        repository.saveNotes(notes)
    }

    val selectedNote = notes.firstOrNull { it.id == selectedNoteId } ?: notes.first()

    fun updateSelected(transform: (Note) -> Note) {
        notes = notes.map { note ->
            if (note.id == selectedNote.id) {
                transform(note).copy(updatedAt = System.currentTimeMillis())
            } else {
                note
            }
        }
    }

    fun addNote() {
        val note = newBlankNote(notes.size + 1)
        notes = listOf(note) + notes
        selectedNoteId = note.id
    }

    fun deleteSelected() {
        if (notes.size == 1) {
            notes = listOf(newBlankNote(1))
            selectedNoteId = notes.first().id
            return
        }
        val remaining = notes.filterNot { it.id == selectedNote.id }
        notes = remaining
        selectedNoteId = remaining.first().id
    }

    fun recognizeMath() {
        val result = recognizer.recognize(selectedNote.strokes)
        if (result == null) {
            recognitionDialog = "인식할 스트로크가 부족합니다."
        } else {
            expression = result.expression
            val card = MathCard(
                expression = result.expression,
                source = result.source,
                confidence = result.confidence,
            )
            updateSelected { note -> note.copy(mathCards = listOf(card) + note.mathCards) }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (maxWidth >= 920.dp) {
                Row(modifier = Modifier.fillMaxSize()) {
                    NoteRail(
                        notes = notes,
                        selectedNoteId = selectedNote.id,
                        onSelect = { selectedNoteId = it },
                        onAdd = ::addNote,
                        onDelete = ::deleteSelected,
                        modifier = Modifier.width(240.dp).fillMaxHeight(),
                    )
                    Workspace(
                        note = selectedNote,
                        selectedTool = selectedTool,
                        selectedColor = selectedColor,
                        strokeWidth = strokeWidth,
                        expression = expression,
                        onToolChange = { selectedTool = it },
                        onColorChange = { selectedColor = it },
                        onStrokeWidthChange = { strokeWidth = it },
                        onExpressionChange = { expression = it },
                        onTitleChange = { title -> updateSelected { it.copy(title = title) } },
                        onStrokeFinished = { stroke -> updateSelected { it.copy(strokes = it.strokes + stroke) } },
                        onEraseAt = { offset -> updateSelected { it.copy(strokes = eraseIntersectingStroke(it.strokes, offset)) } },
                        onUndo = { updateSelected { it.copy(strokes = it.strokes.dropLast(1)) } },
                        onRecognize = ::recognizeMath,
                        onPinExpression = {
                            updateSelected {
                                it.copy(
                                    mathCards = listOf(
                                        MathCard(
                                            expression = expression,
                                            source = "manual-confirm",
                                            confidence = 1f,
                                        ),
                                    ) + it.mathCards,
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    NoteRail(
                        notes = notes,
                        selectedNoteId = selectedNote.id,
                        onSelect = { selectedNoteId = it },
                        onAdd = ::addNote,
                        onDelete = ::deleteSelected,
                        modifier = Modifier.fillMaxWidth().height(148.dp),
                    )
                    Workspace(
                        note = selectedNote,
                        selectedTool = selectedTool,
                        selectedColor = selectedColor,
                        strokeWidth = strokeWidth,
                        expression = expression,
                        onToolChange = { selectedTool = it },
                        onColorChange = { selectedColor = it },
                        onStrokeWidthChange = { strokeWidth = it },
                        onExpressionChange = { expression = it },
                        onTitleChange = { title -> updateSelected { it.copy(title = title) } },
                        onStrokeFinished = { stroke -> updateSelected { it.copy(strokes = it.strokes + stroke) } },
                        onEraseAt = { offset -> updateSelected { it.copy(strokes = eraseIntersectingStroke(it.strokes, offset)) } },
                        onUndo = { updateSelected { it.copy(strokes = it.strokes.dropLast(1)) } },
                        onRecognize = ::recognizeMath,
                        onPinExpression = {
                            updateSelected {
                                it.copy(
                                    mathCards = listOf(
                                        MathCard(
                                            expression = expression,
                                            source = "manual-confirm",
                                            confidence = 1f,
                                        ),
                                    ) + it.mathCards,
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    recognitionDialog?.let { message ->
        AlertDialog(
            onDismissRequest = { recognitionDialog = null },
            confirmButton = {
                TextButton(onClick = { recognitionDialog = null }) {
                    Text("확인")
                }
            },
            title = { Text("수식 인식") },
            text = { Text(message) },
        )
    }
}

@Composable
private fun NoteRail(
    notes: List<Note>,
    selectedNoteId: String,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(colorFromArgb(0xFFFFFFFFL))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("노트", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row {
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "새 노트")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "삭제")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(notes, key = { it.id }) { note ->
                val selected = note.id == selectedNoteId
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) colorFromArgb(0xFFE0F2F1L) else colorFromArgb(0xFFF8FAFCL))
                        .border(
                            width = 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else colorFromArgb(0xFFE5E7EBL),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .clickable { onSelect(note.id) }
                        .padding(12.dp),
                ) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${note.strokes.size} strokes · ${note.mathCards.size} graphs",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorFromArgb(0xFF64748BL),
                    )
                }
            }
        }
    }
}

@Composable
private fun Workspace(
    note: Note,
    selectedTool: CanvasTool,
    selectedColor: Long,
    strokeWidth: Float,
    expression: String,
    onToolChange: (CanvasTool) -> Unit,
    onColorChange: (Long) -> Unit,
    onStrokeWidthChange: (Float) -> Unit,
    onExpressionChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onStrokeFinished: (InkStroke) -> Unit,
    onEraseAt: (Offset) -> Unit,
    onUndo: () -> Unit,
    onRecognize: () -> Unit,
    onPinExpression: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth >= 760.dp) {
            Row(modifier = Modifier.fillMaxSize()) {
                WritingSurface(
                    note = note,
                    selectedTool = selectedTool,
                    selectedColor = selectedColor,
                    strokeWidth = strokeWidth,
                    onToolChange = onToolChange,
                    onColorChange = onColorChange,
                    onStrokeWidthChange = onStrokeWidthChange,
                    onTitleChange = onTitleChange,
                    onStrokeFinished = onStrokeFinished,
                    onEraseAt = onEraseAt,
                    onUndo = onUndo,
                    onRecognize = onRecognize,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                GraphPanel(
                    expression = expression,
                    onExpressionChange = onExpressionChange,
                    onPinExpression = onPinExpression,
                    modifier = Modifier.width(360.dp).fillMaxHeight(),
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                WritingSurface(
                    note = note,
                    selectedTool = selectedTool,
                    selectedColor = selectedColor,
                    strokeWidth = strokeWidth,
                    onToolChange = onToolChange,
                    onColorChange = onColorChange,
                    onStrokeWidthChange = onStrokeWidthChange,
                    onTitleChange = onTitleChange,
                    onStrokeFinished = onStrokeFinished,
                    onEraseAt = onEraseAt,
                    onUndo = onUndo,
                    onRecognize = onRecognize,
                    modifier = Modifier.weight(1f),
                )
                GraphPanel(
                    expression = expression,
                    onExpressionChange = onExpressionChange,
                    onPinExpression = onPinExpression,
                    modifier = Modifier.fillMaxWidth().height(340.dp),
                )
            }
        }
    }
}

@Composable
private fun WritingSurface(
    note: Note,
    selectedTool: CanvasTool,
    selectedColor: Long,
    strokeWidth: Float,
    onToolChange: (CanvasTool) -> Unit,
    onColorChange: (Long) -> Unit,
    onStrokeWidthChange: (Float) -> Unit,
    onTitleChange: (String) -> Unit,
    onStrokeFinished: (InkStroke) -> Unit,
    onEraseAt: (Offset) -> Unit,
    onUndo: () -> Unit,
    onRecognize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = note.title,
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("제목") },
        )

        ToolBar(
            selectedTool = selectedTool,
            selectedColor = selectedColor,
            strokeWidth = strokeWidth,
            onToolChange = onToolChange,
            onColorChange = onColorChange,
            onStrokeWidthChange = onStrokeWidthChange,
            onUndo = onUndo,
            onRecognize = onRecognize,
        )

        InkCanvas(
            strokes = note.strokes,
            selectedTool = selectedTool,
            penColorArgb = selectedColor,
            strokeWidth = if (selectedTool == CanvasTool.Highlighter) strokeWidth * 2.4f else strokeWidth,
            onStrokeFinished = onStrokeFinished,
            onEraseAt = onEraseAt,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@Composable
private fun ToolBar(
    selectedTool: CanvasTool,
    selectedColor: Long,
    strokeWidth: Float,
    onToolChange: (CanvasTool) -> Unit,
    onColorChange: (Long) -> Unit,
    onStrokeWidthChange: (Float) -> Unit,
    onUndo: () -> Unit,
    onRecognize: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconToggleButton(
            checked = selectedTool == CanvasTool.Pen,
            onCheckedChange = { onToolChange(CanvasTool.Pen) },
        ) {
            Icon(Icons.Default.Edit, contentDescription = "펜")
        }
        IconToggleButton(
            checked = selectedTool == CanvasTool.Highlighter,
            onCheckedChange = { onToolChange(CanvasTool.Highlighter) },
        ) {
            Icon(Icons.Default.Check, contentDescription = "형광펜")
        }
        IconToggleButton(
            checked = selectedTool == CanvasTool.Eraser,
            onCheckedChange = { onToolChange(CanvasTool.Eraser) },
        ) {
            Icon(Icons.Default.Clear, contentDescription = "지우개")
        }

        Spacer(Modifier.width(2.dp))

        listOf(AppPalette.Ink, AppPalette.Ocean, AppPalette.Coral, AppPalette.Violet, AppPalette.Amber).forEach { argb ->
            ColorSwatch(
                argb = argb,
                selected = selectedColor == argb,
                onClick = { onColorChange(argb) },
            )
        }

        Slider(
            value = strokeWidth,
            onValueChange = onStrokeWidthChange,
            valueRange = 2f..14f,
            modifier = Modifier.width(140.dp),
        )

        Spacer(modifier = Modifier.width(12.dp))

        FilledTonalIconButton(onClick = onUndo) {
            Icon(Icons.Default.Delete, contentDescription = "되돌리기")
        }
        Button(onClick = onRecognize) {
            Icon(Icons.Default.Check, contentDescription = "수식 인식")
            Spacer(Modifier.width(6.dp))
            Text("인식")
        }
    }
}

@Composable
private fun ColorSwatch(
    argb: Long,
    selected: Boolean,
    onClick: () -> Unit,
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(colorFromArgb(argb))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else colorFromArgb(0xFFE5E7EBL),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    )
}
