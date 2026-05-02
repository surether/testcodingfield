# Skill: Android Tablet Math Note App

Use this skill when changing the Android note-taking app.

## Goal

Build toward a Samsung Notes-style tablet app with a dense writing-first interface, pen input, math recognition, and graph rendering.

## Workflow

1. Preserve a tablet-first layout: note rail, writing canvas, graph panel.
2. Keep ink capture local and responsive.
3. Route handwriting-to-expression work through `HandwritingMathRecognizer`.
4. Route graph evaluation through `FunctionExpression`.
5. Persist user-visible note data through `NoteRepository`.
6. Add focused tests for parser or recognizer behavior when behavior changes.

## Review Checklist

- Strokes render without resizing the canvas.
- Eraser does not delete unrelated state.
- Expression parser handles implicit multiplication and common functions.
- Graph renderer skips invalid samples instead of crashing.
- Notes survive activity recreation through repository persistence.
