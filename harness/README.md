# Harness

This repository uses a harness-oriented collaboration structure for future coding work. The harness is represented as agents, generated skills, and architecture patterns that can be run manually or wired into automation later.

## Patterns

- Pipeline: product intent -> architecture -> implementation -> test -> review.
- Fan-out/Fan-in: several specialists inspect independent concerns, then one integrator merges the answer.
- Expert Pool: route a task to the best specialist skill for Android, ink, math, storage, or QA.
- Producer-Reviewer: one worker implements; another reviews behavior, risk, and tests.
- Supervisor: a coordinator owns scope, sequencing, and acceptance criteria.
- Hierarchical: split large work into feature epics, then smaller implementation tickets.

## Current Agent Set

- Supervisor: keeps the Samsung Notes-style target and Android tablet constraints aligned.
- Android Compose Engineer: builds native UI and input flows.
- Ink Math Specialist: owns stroke capture, recognition, expression parsing, and graphing.
- Storage Engineer: owns on-device note persistence.
- Reviewer: checks regression risk, build health, and missing tests.

## Generated Skill

See `harness/skills/android-note-app.skill.md`.
