# Homeschool Teacher — Code architecture

This document explains how the Android app is organized so that adding
new lessons or tweaking existing ones is a small, well-scoped change.

## Module layout

The codebase has two Gradle modules:

- **`:shared`** — pure-Kotlin module. Holds all logic that doesn't depend
  on Android, Compose, or persistence. The intent is that this is the
  set of files we can move into a Kotlin Multiplatform `commonMain`
  source set when an iOS target is added: `lesson/Lesson.kt`,
  `lesson/LessonRoulette.kt`, `chess/ChessEngine.kt`,
  `tictactoe/GameLogic.kt`, `reading/Animal.kt`. The roulette unit
  test (`LessonRouletteTest`) also lives here.
- **`:app`** — Android application module. Compose UI, `Storage` (built
  on SharedPreferences), `Tts` (Android `TextToSpeech`), `AppConfig`
  (parses `config.yaml`), `LessonSelector` (orchestrator wiring the
  shared `LessonRoulette` to Android ViewModels), and all per-lesson
  ViewModels / screens.

`:app` `implementation(project(":shared"))` is the only wire between the
two. Adding the iOS app later means adding another module (e.g.
`:iosApp`) that consumes `:shared` and provides its own
`Storage` / `Tts` / UI in Swift.

Inside `app/src/main/java/com/dividetask/homeschoolteacher/` the source
is still split by feature:

```
homeschoolteacher/
├── MainActivity.kt          Activity entry point. Wires up the Compose
│                            tree and initializes the singletons.
├── AppConfig.kt             Loads config.yaml into typed values.
├── Storage.kt               Thin wrapper over SharedPreferences. All
│                            persistence flows through here.
├── Tts.kt                   Text-to-speech singleton (Android TTS).
├── ClipPlayer.kt            Bundled-audio-clip player (MediaPlayer),
│                            used by Letter Sounds to play word clips.
├── lesson/
│   └── LessonSelector.kt    The orchestrator. Knows which lesson is
│                            active, decides what's next. (The lesson
│                            registry itself lives in :shared.)
├── ui/
│   ├── HomeschoolTeacherApp.kt   Top-level composable. Drawer, top
│   │                             bar, dispatch to the right screen.
│   ├── ProficiencyBar.kt    Passed-count-per-category header. No longer
│   │                        rendered (kept for reference).
│   ├── ProgressScreen.kt    Debug/inspection screen + manual
│   │                        lock/unlock switches.
│   └── theme/Theme.kt       Material3 color scheme.
├── tictactoe/               Lessons: TicTacToe0, TicTacToe1
│   ├── GameViewModel.kt     Runs whichever TTT lesson is active.
│   └── GameScreen.kt        Compose UI for both TTT lessons.
├── chess/                   Lessons: Chess0..Chess3
│   ├── ChessViewModel.kt    Runs whichever chess lesson is active.
│   └── ChessScreen.kt
├── math/                    Lessons: addition variants — L0:
│   ├── MathViewModel.kt     MathPictures (counting), Math0
│   └── MathScreen.kt        (vertical), HorizontalAddition0,
│                            NumberLineAddition0; L1: Math1
│                            (vertical), HorizontalAddition1,
│                            MathNumberLine.
├── binary/                  Lessons: BinaryOps0, BinaryOps1
│   ├── BinaryOperationsViewModel.kt
│   └── BinaryOperationsScreen.kt
├── multiplication/          Lessons: CountingMultiplication0/1
│   ├── CountingMultiplicationViewModel.kt / CountingMultiplicationScreen.kt
│   └── MultiplicationOperandsViewModel.kt / MultiplicationOperandsScreen.kt
│                            (Level 1: pick the two operands)
└── reading/                 Lessons: LetterSounds0, Phonemes0,
    ├── LetterSounds.kt      Reading0, SightWords0/1, RhymingWords0
    ├── LetterSoundsViewModel.kt / LetterSoundsScreen.kt
    ├── Phonemes.kt
    ├── PhonemesViewModel.kt / PhonemesScreen.kt
    ├── ReadingViewModel.kt / ReadingScreen.kt
    ├── SightWords.kt / SightWordsViewModel.kt / SightWordsScreen.kt
    └── RhymingWords.kt / RhymingWordsViewModel.kt / RhymingWordsScreen.kt
```

Pure-Kotlin logic lives in `shared/src/main/java/...`: `lesson/Lesson.kt`
(LessonId + registry), `lesson/LessonRoulette.kt` (random selection),
`chess/ChessEngine.kt`, `tictactoe/GameLogic.kt`, `reading/Animal.kt`.

`app/src/main/assets/config.yaml` is the runtime config; it ships
inside the APK and is read on app start.

## Lessons are first-class

Every learning unit in the app is a **lesson**. Lessons are listed in
`lesson/Lesson.kt`:

```kotlin
enum class LessonId {
    TicTacToe0, TicTacToe1, TicTacToe2,
    Chess0, Chess1, Chess2, Chess3,
    MathPictures, Math0, HorizontalAddition0, NumberLineAddition0,
    CountingAddition1, Math1, HorizontalAddition1, MathNumberLine,
    BinaryOps0, BinaryOps1,
    CountingSubtraction0, HorizontalSubtraction0,
    VerticalSubtraction0, NumberLineSubtraction0,
    CountingMultiplication0,
    LetterSounds0, Phonemes0, Reading0, SightWords0, SightWords1, RhymingWords0,
}

data class LessonDefinition(
    val id: LessonId,
    val title: String,
    val category: Category,
    /** Multiple parents express the docs' "All <Subject> Difficulty N
     * passed" gate — every parent must be passed for the lesson to
     * unlock. Empty list = entry-level. */
    val parents: List<LessonId> = emptyList(),
)

data class LessonDefinition(
    val id: LessonId,
    val title: String,
    val category: Category,
    val parent: LessonId?,
)
```

`Lessons.all` is the master list. Lookup by id with `Lessons.get(id)`.
`Lessons.unlocked(passedMap)` returns the lessons currently available
(no parent, or parent already passed).

`docs/lessons.md` is the human-facing spec for what each lesson does;
this file is for how the code is shaped.

## The orchestrator: `LessonSelector`

`lesson/LessonSelector` is a `ViewModel` and the single source of truth
for "what is the learner doing right now". It owns three pieces of
state:

- `mode: StateFlow<SelectionMode>` — `Random`, `SingleLesson`, or
  `Progress`.
- `currentLesson: StateFlow<LessonId>` — the lesson presently on
  screen (irrelevant when `mode == Progress`).
- `passedMap: StateFlow<Map<LessonId, Boolean>>` — combined from every
  lesson VM's `passed` flow, so it stays live.

The selector exposes four entry points:

- `selectRandom()` — switch to Random rotation; immediately rolls a
  lesson.
- `selectLesson(id)` — open a specific lesson and stay there
  indefinitely.
- `selectProgress()` — show the debug screen; activity VMs untouched.
- `onLessonInstanceCompleted()` — called by each lesson screen when
  one instance of the lesson finishes (e.g. one math problem solved,
  one TTT game ended). The selector decrements the per-round counter
  and either runs another instance of the same lesson or re-rolls.

### Random-rotation rules

When `mode == Random` and `onLessonInstanceCompleted()` empties the
counter, the selector picks the next lesson with these rules:

1. **Pool = unlocked lessons.** A lesson with no parent is always in
   the pool; a lesson with a parent is in the pool only when its parent
   is in `passedMap` with `true`.
2. **Exclude the just-completed category** when at least one lesson of
   a different category is in the pool. (E.g. you never get Math 0
   right after Math 1.) If filtering would empty the pool, the filter
   is dropped.
3. **Weight unpassed lessons 2× over passed lessons.** So lessons you
   haven't mastered come up about twice as often as ones you have, but
   passed lessons remain in regular rotation.
4. **Pick weighted-uniformly.** Once a lesson is chosen, the per-round
   counter is set to `AppConfig.runsPerRound(def)` (per-lesson override
   if one exists, else the category default) and the lesson is started.

When `mode == SingleLesson` the per-round counter is `Int.MAX_VALUE`
and the same lesson runs over and over until the menu picks something
else.

### Per-lesson dispatch

Each lesson belongs to a "runner" VM in its feature package. The
selector calls one of:

```kotlin
ttt.startLesson(id)            // TicTacToe0 / TicTacToe1
chess.startLesson(id)          // Chess0 / Chess1 / Chess2 / Chess3
math.startLesson(id)           // MathPictures, Math0, HorizontalAddition0,
                               // NumberLineAddition0, CountingAddition1,
                               // Math1, HorizontalAddition1, MathNumberLine,
                               // CountingSubtraction0, HorizontalSubtraction0,
                               // VerticalSubtraction0, NumberLineSubtraction0
binary.startLesson(id)         // BinaryOps0 / BinaryOps1
multiplication.startLesson()   // CountingMultiplication0
multiplicationOperands.startLesson() // CountingMultiplication1
letterSounds.startLesson()     // LetterSounds0
phonemes.startLesson()         // Phonemes0
reading.startLesson()          // Reading0
sightWords.startLesson(id)     // SightWords0 / SightWords1
rhymingWords.startLesson()     // RhymingWords0
```

The runner VM stores which lesson is active (where applicable) and
adjusts its own behaviour (range, AI level, UI variant, etc.). Adding
a new "level" of an existing category is therefore a) a new
`LessonId` enum value, b) a `LessonDefinition` entry, c) a new branch
inside the relevant VM. No glue code changes in the UI layer.

## ViewModel structure

Every lesson runner VM exposes:

| Flow                                      | Purpose                          |
| ----------------------------------------- | -------------------------------- |
| `state: StateFlow<...State>`              | UI state for the active instance |
| `passed: StateFlow<Boolean>` or `passed(id)` | Sticky "lesson passed" flag   |
| Per-lesson streak counter(s)              | For the Progress screen          |

The runner is the only place that mutates per-lesson state and the
only place that writes to `Storage`. Compose screens are pure
view-of-state.

### Lesson "instance completion"

Each lesson has a notion of one finished instance:

- **TTT**: one game ended (win, loss, or draw).
- **Chess**: one correct capture, a wrong tap, or "Give up".
- **All quiz-style lessons (Math, Binary, Multiplication, Phonemes,
  Animals, Sight/Rhyming Words)**: one answered problem. Correct
  auto-advances after 0.9s; wrong reveals the right answer and
  advances after 2s; "Give up" reveals and advances after 1.6s.
  Phonemes overrides all three with a single 4-second hold (the
  answer reveals three words — see docs/lessons.md § Show answer
  time).

The corresponding screen runs a `LaunchedEffect` that, once the hold
elapses, calls `Tts.stopAll()` and then `onCompleted()` (a callback
the selector wires up) — so speech from the finished problem never
bleeds into the next lesson. Input is disabled for 1 second after a
new problem appears so rapid taps from the previous problem can't
leak through.

## Persistence (`Storage.kt`)

All persisted state lives in one `SharedPreferences` file named
`homeschool_teacher`. Keys are namespaced:

- `lesson.<LessonId>.passed`         — sticky boolean per lesson.
- `lesson.<LessonId>.manualOverride` — legacy. Set in older builds when
                                        the Progress switch was flipped;
                                        no longer written but still
                                        loaded if present.
- `lesson.<LessonId>.manualUnlock`   — Progress-screen Switch flips
                                        this. When true, the lesson is
                                        listed as unlocked regardless of
                                        whether its parents are passed.
                                        Does not mark the lesson as
                                        passed.
- `win_streak.<key>`                 — THE single streak store. Every
                                        consecutive-correct / non-loss
                                        streak lives here under one
                                        namespace; `<key>` is the
                                        `LessonId` (games & math), or
                                        `<LessonId>.<letter|word|...>` for
                                        the per-item reading lessons
                                        (`SightWords.<word>.<pos>` is shared
                                        by both Sight Words levels;
                                        `LetterSounds0.run` is the across-
                                        letters run). Set/save through
                                        `Storage.loadWinStreak/saveWinStreak`.
- `ttt.{player|cpu|draw}Score`       — aggregate scoreboard.
- `math.streak.<x>.<y>`              — 16×16 addition cell *grid* (a
                                        coverage map, NOT a win streak),
                                        shared by every addition variant
                                        (Counting, Vertical, Horizontal,
                                        Number Line) at both difficulties.
- `math.{correct|wrong}`             — lifetime counters.
- `subtraction.streak.<x>.<y>`       — 16×16 subtraction cell grid (shared
                                        by every subtraction variant).
- `binary.streak.<lvl>.<op>.<a>.<b>` — binary AND/OR/XOR coverage grid.
- `binary.{correct|wrong}`           — lifetime counters.
- `multiplication.streak.<a>.<b>`    — counting-multiplication (product)
                                        coverage grid (Level 0).
- `multiplication.{correct|wrong}`   — lifetime counters.
- `multoperands.streak.<a>.<b>`      — Level 1 "identify the operands"
                                        coverage grid (op1, op2 ∈ 1..4).
- `multoperands.{correct|wrong}`     — lifetime counters.
- `lettersounds.{correct|wrong}`     — lifetime counters.
- `phonemes.{correct|wrong}`         — lifetime counters.
- `reading.{correct|wrong}`          — lifetime counters.
- `sightwords.{correct|wrong}`       — lifetime counters.
- `rhymingwords.{correct|wrong}`     — lifetime counters.
- `chess.{correct|wrong}`            — lifetime counters (shared across
                                        all chess levels).
- `migration.v2` … `migration.v5`    — one-shot migration sentinels. `v4`
                                        auto-passes Letter Sounds 0 for
                                        users who already reached Phonemes;
                                        `v5` folds every old per-feature
                                        streak key (`ttt.streak.*`,
                                        `chess.streak.*`,
                                        `math.lessonstreak.*`, the reading
                                        streaks, …) into `win_streak.*`.

When a runner mutates state, it writes to storage inline (no debouncing
or batching — SharedPreferences `apply()` is cheap). When a runner is
constructed it reads everything it needs from storage so a fresh
process resumes exactly where the previous one left off.

`Storage.init(context)` runs the v1→v2 migration on first launch after
upgrade: it derives `lesson.*.passed` flags from any older `ttt.level`
and the math streak grid, and routes the legacy `ttt.nonLossStreak`
into the right per-lesson slot.

## Config (`AppConfig.kt` + `config.yaml`)

`AppConfig.load(context)` parses `app/src/main/assets/config.yaml` on
app start. Currently:

```yaml
session:
  game_runs_per_round: 1
  math_runs_per_round: 4
  reading_runs_per_round: 2
  phonemes_runs_per_round: 3   # per-lesson override
tictactoe:
  auto_restart_seconds: 2
```

These map to typed properties (`AppConfig.gameRunsPerRound`, etc.).
The orchestrator asks `AppConfig.runsPerRound(def)`, which returns the
lesson's override if it has one (currently only Phonemes) and the
category default otherwise. Older keys (`tictactoe_games_per_round`,
`math_problems_per_round`, `reading_puzzles_per_round`) are still
accepted for backwards compatibility.

## UI structure

`HomeschoolTeacherApp` builds:

- A `ModalNavigationDrawer` whose entries are: **Random / Mixed**, one
  row per lesson (with ✓ for passed and 🔒 for locked), and
  **Progress**. The drawer content scrolls vertically — the lesson
  list is taller than most screens.
- A `TopAppBar` whose title is the current lesson's `title` (or
  "Progress" when in Progress mode).
- The active screen dispatched off `currentLesson` — one `when`
  branch per runner (TTT, Chess, Math, Binary, Multiplication,
  Phonemes, Animals, Sight Words, Rhyming Words).
- Opening the drawer requires holding the hamburger icon for half a
  second (a progress ring fills during the hold); drawer swipe-to-open
  is disabled so kids can't open it accidentally.

Each lesson screen takes its VM plus an `onCompleted: () -> Unit`
callback that the selector binds to `onLessonInstanceCompleted`.

The Math screen branches its presentation off
`viewModel.activeLesson`: MathPictures shows emoji groups,
MathNumberLine draws the tick-marked number line, Math0/Math1 show
the vertical stack. All answers are single-tap on a numeric grid
sized to the lesson's max possible sum. The Binary screen uses a
0/1/Back keypad that auto-submits when all bits are entered.

## How to add a new lesson

1. Add a new entry to `LessonId` and to the list in `Lessons`. Set
   `category` and `parent` correctly.
2. Decide which runner owns the lesson (existing VM or a new one).
   - **Existing runner**: add a branch in `startLesson(id)` and in any
     behavior that varies (problem range, AI level, UI mode). Track
     a new `streak` and `passed` flow per lesson where appropriate.
   - **New category / new runner**: create a new feature package with
     `XViewModel`, `XScreen`, and any pure-logic file. Add storage
     keys to `Storage.kt`. Add a `when` branch in
     `LessonSelector.startInstance` and in
     `HomeschoolTeacherApp`'s `when (current)`.
3. If the new lesson should have its own per-round count, add a key
   to `config.yaml` and `AppConfig`.
4. Update `docs/lessons.md` with the new spec.
