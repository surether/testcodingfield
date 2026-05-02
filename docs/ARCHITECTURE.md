# Architecture

## Android App

The app uses a small, native Android architecture:

- `MainActivity` hosts Compose without an extra activity dependency.
- `NoteApp` owns screen state and writes it through `NoteRepository`.
- `InkCanvas` captures strokes, renders pen/highlighter lines, and supports stroke-level erasing.
- `HandwritingMathRecognizer` converts recent strokes into graphable math expression candidates.
- `FunctionExpression` parses and evaluates math expressions for `GraphPanel`.

## Data

Notes are stored on-device in `SharedPreferences` as JSON:

- `Note`: title, stroke list, math card list.
- `InkStroke`: ordered points, color, width, highlighter flag.
- `MathCard`: expression, source, confidence, created timestamp.

This keeps the first version offline and easy to migrate later to Room or cloud sync.

## Math

The parser supports:

- Variables: `x`
- Constants: `pi`, `e`
- Operators: `+`, `-`, `*`, `/`, `^`
- Implicit multiplication: `2x`, `(x+1)(x-1)`
- Functions: `sin`, `cos`, `tan`, `sqrt`, `abs`, `exp`, `ln`, `log`

## Future Recognition Adapter

`HandwritingMathRecognizer` is intentionally isolated. A later ML adapter can implement the same input/output shape:

```kotlin
fun recognize(strokes: List<InkStroke>): RecognitionResult?
```

That adapter can use ML Kit Digital Ink Recognition, a custom math OCR model, or a server-side recognizer while leaving the note canvas and graph panel unchanged.
