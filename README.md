# Homeschool Teacher

An Android app that teaches basic concepts to young children through
short, rotating lessons: tic-tac-toe and chess puzzles, addition with
pictures / number lines / vertical and horizontal equations, binary
logic (AND / OR / XOR), counting multiplication, phonics, animal
letter-recognition, sight words, and rhyming words.

Progress is tracked per lesson with streak-based mastery criteria;
passing a lesson unlocks the next one in its chain. A "Random / Mixed"
mode rotates between unlocked lessons, weighting unfinished ones
higher. All progress persists on the device.

## Build

```
./gradlew :app:assembleDebug
```

Requires the Android SDK (set `ANDROID_HOME` or `sdk.dir` in
`local.properties`). The APK lands in `app/build/outputs/apk/debug/`.

Run the unit tests for the lesson-selection logic with:

```
./gradlew :shared:test
```

## Project layout

- `app/` — the Android application (Jetpack Compose UI, ViewModels,
  persistence, text-to-speech).
- `shared/` — pure-Kotlin logic (lesson registry, random lesson
  selection, chess engine, tic-tac-toe logic) kept free of Android
  dependencies so it can move to Kotlin Multiplatform for an iOS port.
- `docs/lessons.md` — the source of truth for how lessons behave:
  variables, screens, rules, and per-lesson definitions.
- `docs/code.md` — how the code is organized and how to add a lesson.
- `app/src/main/assets/config.yaml` — runtime-tunable word lists and
  session settings.
