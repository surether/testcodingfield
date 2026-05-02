# Test Coding Field

Android tablet note-taking app prototype inspired by Samsung Notes.

## What Is Included

- Native Android app scaffolded with Kotlin and Jetpack Compose.
- Pen, highlighter, eraser, note list, stroke persistence, and undo.
- Handwritten stroke recognition pass that turns common math shapes into editable expressions.
- Function parser and graph renderer for expressions such as `y = x^2`, `sin(x)`, `abs(x)`, and `1 / x`.
- Harness documentation for six agent architecture patterns: Pipeline, Fan-out/Fan-in, Expert Pool, Producer-Reviewer, Supervisor, and Hierarchical.
- GitHub Actions workflow for Android unit tests and debug assembly.

## Build

Install Android Studio with JDK 17 and Android SDK API 36, then run:

```powershell
gradle testDebugUnitTest assembleDebug
```

This local Codex workspace does not currently have Java, Gradle, or the Android SDK installed, so full APK verification is expected to run in Android Studio or GitHub Actions.

## App Flow

1. Create or select a note.
2. Write with a pen or highlighter on the canvas.
3. Tap the recognition control to produce a math expression candidate.
4. Edit the expression if needed.
5. Pin it to the note and inspect the rendered function graph.

## Recognition Scope

The current recognizer is deterministic and on-device. It recognizes common stroke geometries such as linear, quadratic, absolute-value, sine-like, and constant shapes. The code is structured around `HandwritingMathRecognizer`, so a future ML Kit or custom math OCR adapter can replace the heuristic engine without changing the graphing surface.
