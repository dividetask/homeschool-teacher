# Homeschool Teacher — Lessons

This file is the source of truth for how the app's lessons are organized
and how they behave. Lessons reference the shared **Variables**,
**Screens**, and **Rules** sections below so each lesson definition
stays short.

## Overview

The app teaches by stepping a learner through a list of named **lessons**.
Each lesson belongs to one **category**, uses a defined **screen** to
present problems, reads and writes one or more defined **variables**,
and is considered done when its **pass criteria** are met. Lessons may
be locked until one or more parent lessons are passed.

All stored variables persist across app restarts.

## Categories

- **Game** — game-style activities
- **Math** — math problem activities
- **Reading** — reading / letter-recognition activities

## Difficulty and Subject

Every lesson carries two ordering attributes:

- **Subject** — a within-category grouping (e.g. *Addition* and
  *Subtraction* are both Math subjects; *Tic Tac Toe* and *Chess* are
  both Game subjects). All variant-screens of the same operation
  share a Subject so they can be unlocked or queried as a unit.
- **Difficulty** — an integer per Subject, starting at 0. Lessons
  inside one Subject are ordered by Difficulty.

Unlock conditions reference these in the form

> All **&lt;Subject&gt;** Difficulty **N** passed.

This is shorthand for "every lesson in the named Subject at the named
Difficulty has `lesson_passed == true`." For example, the four
Addition Level 1 variants all sit at Math Subject = *Addition*,
Difficulty = 1; once each of their `lesson_passed` flags is true,
"All Addition Difficulty 1 passed" evaluates to true and the next
tier unlocks.

For simple chains (Tic Tac Toe Level 0 → Level 1 → Level 2, Phonemes
0 → Animals 0, etc.) you can also state the parent lesson by name —
both phrasings describe the same gate.

## Game UIDs

Groups related lessons. Used to index stored variables that span
multiple levels of the same game family (e.g. `win_streak[game][level]`).

| UID | Game family    |
| --- | -------------- |
| 0   | Tic Tac Toe    |
| 1   | Chess          |
| 2   | Addition       |
| 3   | Letter Sounds / Phonemes |
| 4   | Animals        |
| 5   | Sight Words    |
| 6   | Rhyming Words  |
| 7   | Subtraction    |
| 8   | Binary         |
| 9   | Multiplication |

## Variables

Single canonical description of every stored variable. Lesson
definitions reference these by name. All defaults are zero unless noted.

### `win_streak[game][slot]`
The single variable that tracks **every** consecutive-correct / non-loss
streak in the app. Indexed by Game UID and a per-streak slot. A correct
answer / non-loss increments the slot; any wrong answer, loss, or
**Give up** resets it to zero. The slot's meaning depends on the game:

- **Games & math** — slot = difficulty level. The four math screens that
  share a Subject + Difficulty each keep their **own** slot, so they are
  passed independently; read e.g. `win_streak[2][1]` as "that screen's
  Level 1 slot", not one shared counter.
- **Animals & Letter Sounds** — slot = letter (`A → 0 … Z → 25`).
- **Phonemes & Rhyming Words** — slot = word.
- **Sight Words** — slot = `(word, position)`, shared between Levels 0
  and 1.
- **Letter Sounds** additionally keeps `win_streak[3][run]`, the
  across-all-letters run.

All slots default to zero and persist across restarts.

### `binary_grid[level][operator][op1][op2]`
Integer 4D array. `level ∈ 0..1`; `operator ∈ {AND, OR, XOR}` (stored
as 0, 1, 2 in that order); `op1` and `op2` indexed `0..7`. Level 0 only
writes the `0..1` sub-grid. Default zero.

### `multiplication_grid[op1][op2]`
Integer 2D array, `op1` and `op2` indexed `0..9`. Default zero. Cell
tracks correct answers for the matching multiplication problem when
shown via the Counting Multiplication Screen.

### `multiplication_equation_grid[op1][op2]`
Integer 2D array, `op1` and `op2` indexed `0..4`. Product-coverage grid
shared by the Horizontal / Vertical / Number Line multiplication screens
(tap-the-product lessons). Separate from `multiplication_grid` (the
counting/product lesson) and `multiplication_operands_grid`. Default zero.

### `multiplication_operands_grid[op1][op2]`
Integer 2D array, `op1` and `op2` indexed `1..4`. Cell tracks correct
identifications of the two operands in Counting Multiplication Level 1
(separate from `multiplication_grid`, which tracks products in Level 0).
Default zero.

### `addition_grid[op1][op2]`
Integer 2D array, `op1` and `op2` indexed `0..19` (length 20 each). Cell
tracks correct answers for the matching addition problem when shown via
the Vertical, Horizontal, or Number Line equation screens. Shared
across those three screens.

### `subtraction_grid[op1][op2]`
Integer 2D array, `op1` and `op2` indexed `0..19`. Cell tracks correct
answers for the matching subtraction problem when shown via the
Vertical, Horizontal, or Number Line equation screens. Shared across
those three screens.

### `lesson_passed[lesson]`
Boolean, one per lesson. Sticky: once a lesson's pass criteria are met
the flag is set to `true` and stays `true` even if streaks subsequently
drop.

### `lesson_manual_unlock[lesson]`
Boolean, one per lesson. Flipping the Progress screen's Switch sets
this. When `true`, the lesson is considered unlocked regardless of
whether its parents are passed (see Manual unlock). Independent of
`lesson_passed[lesson]` — manually unlocking a lesson does not mark
it as completed.

## Screens

Each screen describes one visual layout. Lessons reference a screen by
name and supply the variables and operand ranges that drive it.

### Counting Equation Screen
Two groups of identical animal emoji separated by an operator and `= ?`.
Example for `op1 = 3`, `op2 = 2`, addition:

```
🐱🐱🐱 + 🐱🐱 = ?
```

Each group is sometimes arranged on **two lines** instead of one —
always when the operand is greater than 4 (a long single row is hard
to fit and hard to count), and roughly 30% of the time for groups of
2–4, so the learner sees that the count doesn't depend on the
arrangement. The top line gets the larger half (3 → 2 over 1; 7 → 4
over 3). Example for `op1 = 3`, `op2 = 2` with the left group split:

```
🐱🐱
🐱    + 🐱🐱 = ?
```

The arrangement is chosen once per problem and stays fixed until the
next problem. For subtraction the operator becomes `-`. The single
animal species used is chosen at random per problem.

**Answer surface:** single-tap grid covering every possible answer
within the lesson's operand range.

### Horizontal Equation Screen
The two operands and operator displayed on a single horizontal line:

```
X + Y = ?
```

**Answer surface:** single-tap grid covering every possible answer.

### Vertical Equation Screen
The two operands stacked, with the operator on the second line:

```
 X
+Y
──
```

**Answer surface:** single-tap grid covering every possible answer.

### Number Line Equation Screen
A horizontal number line drawn above the equation, with the equation
below (`X op Y = ?`). The number line:

- **always starts at 0** and runs to `next_multiple_of_ten(answer + 10)`
  — so the answer sits comfortably inside the range, never at the edge;
- **labels every integer** with a tick;
- **scrolls horizontally** — the learner drags it left/right with a
  finger (it is wider than the screen);
- **is markable** — tapping a number toggles a mark (a filled dot) on it,
  as a skip-counting aid. Marks are visual only (they don't affect
  correctness) and clear when the next problem appears.

The answer itself is still entered on the equation's answer surface (the
number line is a counting aid, not the answer input).

**Answer surface:** the single-tap grid (Level 0) or the Number Pad
(Level 1 multiplication), as specified per lesson.

### Tic Tac Toe Board Screen
Standard 3×3 board. The learner taps an empty cell to place their mark;
the CPU then plays. Game ends on a win, draw, or loss. The CPU rule
varies by lesson.

### Tic Tac Toe Puzzle Screen
Standard 3×3 board, but pre-filled with a single-move puzzle position and
no CPU turn. The learner taps one empty cell; that resolves the puzzle
(correct = the win or block, anything else = a loss), the correct/ wrong
cells are outlined, and the next puzzle is dealt. Used by "Win or Block".

### Chess Board Screen
8×8 board with one player piece, a number of capturable pawns, a
number of non-capturable pawns, and optionally one or more friendly
pieces. The learner taps the player piece, then taps a target. A
correct capture is landing on one of the capturable pawns; any other
tap counts as an incorrect move. On a correct capture, the player
piece animates to the captured square before advancing.

Pawns never appear on rank 1 or rank 8 — those are starting / promotion
squares in real chess and the puzzles follow the same convention. The
player piece itself may stand on any square.

### Animal Picture Screen
A single emoji shown large in the centre of the screen; the animal's
English name is spoken via TTS (see Rules § TTS playback). Tapping the
emoji replays the audio.

**Answer surface:** full A–Z keypad.

### Word Display Screen
A word shown with one letter replaced by an underscore; the whole word
is spoken aloud via TTS. Tapping the word replays the audio.

**Answer surface:** full A–Z keypad.

### Phoneme Trio Screen
Three words played in sequence via TTS at the three rates. The words
are masked on screen as `🔊 1`, `🔊 2`, `🔊 3` until the learner
answers, then they are revealed alongside the green/red feedback. A
🔊 Repeat button replays the whole sequence.

**Answer surface:** A–Z Keypad.

### Counting Multiplication Screen
The equation displayed on its own line followed by `op2` groups, each
containing `op1` copies of a randomly-picked animal emoji. **Each group
is drawn inside its own rounded box, with a wide gap between boxes**, so
the "this many groups of this many" structure is clear. Groups flow
left-to-right and wrap to additional lines as needed; a single group is
never split across a line. Example for `op1 = 2`, `op2 = 4`:

```
2 × 4 = ?
[🐱🐱]  [🐱🐱]  [🐱🐱]  [🐱🐱]
```

For products that don't fit on one line (e.g. `4 × 4 = 16`), the
boxed groups wrap onto two or three lines while staying visually
grouped. When either operand is 0 the area shows "(no 🐱)" instead of
empty space.

**Answer surface:** Numeric Grid (0..max).

### Binary Vertical Equation Screen
Two binary operands stacked, with a bitwise operator on the second line
and a subscript `₂` after each operand and the answer line to indicate
base 2:

```
    0₂                110₂
AND 1₂      or    AND 011₂
  ──                ─────
    ?₂                ???₂
```

Each operand is zero-padded to `bits` digits. The operator is spelled
out as the word `AND`, `OR`, or `XOR`. The answer slots fill
left-to-right as the learner taps digits; while empty they render as `_`.

A **cheat sheet** sits at the top of the screen showing the single-bit
truth table for **the current operator only** — the four `a OP b = r`
rows for `a, b ∈ {0, 1}`. When the problem's operator is AND it shows the
AND rows; OR shows the OR rows; XOR shows the XOR rows. For Level 0 these
four rows are every possible question; for Level 1 they are the per-column
rule to apply to each of the three bits.

**Answer surface:** Binary Keypad with `bits` slots.

## Answer Surfaces

The keyboard / button group used to capture the learner's answer.
Lessons cite a surface by name from this list rather than describing
it inline.

### Numeric Grid (0..max)
Grid of single-tap buttons, each labelled with one candidate answer
from `0` to `max`. Used by every math equation screen. Column count
scales with `max` so the grid stays comfortable on a phone (5 columns
through `max = 18`, 7 columns above that). Tapping a button submits
that value as the answer.

### A–Z Keypad
Full alphabet keypad of 26 single-tap buttons arranged in rows of 7
(letters A–Z, last row padded with spacers). Used by every reading
screen. Tapping a letter submits it as the answer.

### Number Pad
Calculator-style pad — digits `1..9`, then a **⌫ (Back) / 0 / Enter**
row — with a readout above showing the digits entered so far. The learner
types the answer and presses **Enter** to submit; **⌫** deletes the last
digit. Used where the answer range is too large for a comfortable tap grid
(multiplication products up to 81, so answers are at most two digits).

### Operand Picker
Row of single-tap buttons `1..4` used by Counting Multiplication Level 1.
The displayed equation has two blanks (`▢ × ▢`); the first tap fills the
left blank, the second fills the right blank and submits. The answer is
order-independent. A **Clear** button resets the picks before the second
tap.

### Binary Keypad (N slots)
Two answer buttons (`0` and `1`) plus a `Back` button. The current
input is shown above the keypad as `N` slots filled left-to-right;
empty slots render as `_`. Tapping `0` or `1` fills the next empty
slot; `Back` clears the right-most filled slot. The answer
auto-submits the moment all `N` slots are filled. For `N = 1` the
keypad effectively behaves as a single-tap surface (one digit fills
the only slot and submits immediately).

## Rules

Shared rule sets referenced by multiple lessons.

### Random problem selection (math grid)

For lessons that use a 2D operand grid:

1. Roll `wildcard ∈ 1..10`.
2. If `wildcard == 1`, choose any `(op1, op2)` uniformly at random from
   the lesson's declared operand range.
3. Otherwise, choose `(op1, op2)` where `grid[op1][op2]` is at the
   **minimum** value within the lesson's range. Break ties uniformly at
   random.

The next-problem selection avoids repeating the previous problem when
the candidate pool has more than one entry.

### Random problem selection (per-word list)

For lessons that use a list of words / letters / animals each with its
own streak:

1. Roll `wildcard ∈ 1..10`.
2. If `wildcard == 1`, choose any entry uniformly at random.
3. Otherwise, choose any entry whose streak equals the minimum streak in
   the pool. Break ties uniformly at random.

Avoid repeating the previous entry when an alternative exists.

### Chess piece movement

Movement in our chess puzzles is **single-move only**. There is no
castling, en passant, promotion, or pawn movement.

- **Pawn** — never moves in our puzzles; pawns are only ever capture
  targets.
- **Rook** — any number of squares along its rank or file. The first
  piece encountered blocks further movement (the rook may capture it
  if it is opposing colour).
- **Bishop** — any number of squares along a diagonal. Same blocking
  rule.
- **Queen** — any number of squares along rank, file, or diagonal.
  Same blocking rule.
- **Knight** — L-shape (two squares one direction then one square
  perpendicular). Cannot be blocked.

### Chess capture rule

For the lesson's player piece **P**:

- **Capturable pawn** — opposite-colour pawn that P can reach in one
  legal move (on P's movement axes and not blocked by another piece).
- **Non-capturable pawn** — pawn that P cannot reach in one move
  (off-axis or blocked).
- **Friendly piece** — same colour as P; serves as a distractor and
  may also block P's movement.

A correct answer is tapping any capturable pawn. Any other tap (a
non-capturable pawn, a friendly piece, an empty square, or P itself)
is an incorrect move.

### Show answer time

After the learner answers (or gives up) the correct answer is shown
highlighted, then the app advances. Default hold times:

- **Correct answer:** 0.9 seconds.
- **Wrong answer:** 2 seconds.
- **Give up / reveal:** 1.6 seconds.

A lesson definition may carry a **Show answer time** row; that single
value then replaces all three defaults for that lesson. The row is
present only when the lesson overrides the defaults — its absence
means the defaults above apply. (Tic Tac Toe is a game rather than a
single-answer problem; it instead waits the configurable
`tictactoe.auto_restart_seconds` after a game ends.)

### Runs per round

In Random / Mixed mode, each time a lesson is drawn it runs this many
problems / games in a row before another lesson is drawn. Every lesson
definition carries a **Runs per round** row. The values come from
`app/src/main/assets/config.yaml` — one default per category
(Game 1, Math 4, Reading 2) plus per-lesson overrides such as
`phonemes_runs_per_round`.

### TTS playback

When a word is spoken (single word or each word inside a trio):

1. Play at `1.0×` speed.
2. Pause 1 second.
3. Play at `0.5×` speed.
4. Pause 1 second.
5. Play at `0.125×` speed.

For phoneme trios, within a single speed each word is followed by a
0.5-second pause before the next word. A 🔊 Repeat button on the
screen replays the whole sequence.

When a lesson instance finishes — after its show-answer hold elapses,
right before the next problem or lesson appears — **all speech is
stopped immediately**, so audio from the finished problem never plays
over the next one.

## Lesson catalog

| Game UID | Lesson                              | Category | Unlock                            |
| -------- | ----------------------------------- | -------- | --------------------------------- |
| 0        | Tic Tac Toe — Level 0               | Game     | —                                 |
| 0        | Tic Tac Toe — Win or Block          | Game     | Tic Tac Toe 0                     |
| 0        | Tic Tac Toe — Level 1               | Game     | Tic Tac Toe — Win or Block        |
| 0        | Tic Tac Toe — Level 2               | Game     | Tic Tac Toe 1                     |
| 1        | Chess — Level 0                     | Game     | Tic Tac Toe 0                     |
| 1        | Chess — Level 1                     | Game     | Chess 0                           |
| 1        | Chess — Level 2                     | Game     | Chess 1                           |
| 1        | Chess — Level 3                     | Game     | Chess 2                           |
| 2        | Counting Addition — Level 0          | Math     | —                                 |
| 2        | Horizontal Addition — Level 0       | Math     | —                                 |
| 2        | Vertical Addition — Level 0         | Math     | —                                 |
| 2        | Number Line Addition — Level 0      | Math     | —                                 |
| 2        | Number Line Addition — Level 1      | Math     | Number Line Addition 0 passed     |
| 2        | Counting Addition — Level 1          | Math     | All Addition Diff 0 + Number Line Addition 1 |
| 2        | Horizontal Addition — Level 1       | Math     | All Addition Diff 0 + Number Line Addition 1 |
| 2        | Vertical Addition — Level 1         | Math     | All Addition Diff 0 + Number Line Addition 1 |
| 8        | Binary — Level 0                    | Math     | All Addition Difficulty 0 passed  |
| 8        | Binary — Level 1                    | Math     | All Addition Diff 0 + Binary 0    |
| 7        | Counting Subtraction — Level 0      | Math     | All Addition Difficulty 1 passed  |
| 7        | Horizontal Subtraction — Level 0    | Math     | All Addition Difficulty 1 passed  |
| 7        | Vertical Subtraction — Level 0      | Math     | All Addition Difficulty 1 passed  |
| 7        | Number Line Subtraction — Level 0   | Math     | All Addition Difficulty 1 passed  |
| 9        | Counting Multiplication — Level 0   | Math     | All Subtraction Difficulty 0 passed |
| 9        | Counting Multiplication — Level 1   | Math     | Counting Multiplication 0 passed  |
| 9        | Number Line Multiplication — Level 0| Math     | Counting Multiplication 0 passed  |
| 9        | Horizontal Multiplication — Level 0 | Math     | Number Line Multiplication 0 passed |
| 9        | Vertical Multiplication — Level 0   | Math     | Number Line Multiplication 0 passed |
| 9        | Horizontal Multiplication — Level 1 | Math     | All symbolic Multiplication Diff 0 passed |
| 9        | Vertical Multiplication — Level 1   | Math     | All symbolic Multiplication Diff 0 passed |
| 9        | Number Line Multiplication — Level 1| Math     | All symbolic Multiplication Diff 0 passed |
| 3        | Letter Sounds — Level 0             | Reading  | —                                 |
| 3        | Phonemes — Level 0                  | Reading  | Letter Sounds 0                   |
| 4        | Animals — Level 0                   | Reading  | Phonemes 0                        |
| 5        | Sight Words — Level 0               | Reading  | Animals 0                         |
| 5        | Sight Words — Level 1               | Reading  | Sight Words 0                     |
| 6        | Rhyming Words — Level 0             | Reading  | Sight Words 1                     |

## Lesson definitions

### Tic Tac Toe — Level 0
- **Game UID:** 0
- **Subject:** Tic Tac Toe
- **Difficulty:** 0
- **Category:** Game
- **Runs per round:** 1
- **Unlock conditions:** always.
- **Screen:** Tic Tac Toe Board Screen
- **CPU rule:** uniformly random legal move every turn.
- **Variables:** `win_streak[0][0]`
- **Pass criteria:** `win_streak[0][0] >= 8`

### Tic Tac Toe — Level 1
- **Game UID:** 0
- **Subject:** Tic Tac Toe
- **Difficulty:** 1
- **Category:** Game
- **Runs per round:** 1
- **Unlock conditions:** Tic Tac Toe — Win or Block passed.
- **Screen:** Tic Tac Toe Board Screen
- **CPU rule:** take a winning move if one exists; else uniformly random
  legal move.
- **Variables:** `win_streak[0][1]`
- **Pass criteria:** `win_streak[0][1] >= 8`

### Tic Tac Toe — Level 2
- **Game UID:** 0
- **Subject:** Tic Tac Toe
- **Difficulty:** 2
- **Category:** Game
- **Runs per round:** 1
- **Unlock conditions:** Tic Tac Toe — Level 1 passed.
- **Screen:** Tic Tac Toe Board Screen
- **CPU rule:** take a winning move if one exists; else block the
  opponent's winning move if one exists; else uniformly random legal
  move.
- **Variables:** `win_streak[0][2]`
- **Pass criteria:** `win_streak[0][2] >= 8`

### Tic Tac Toe — Win or Block
- **Game UID:** 0
- **Subject:** Tic Tac Toe
- **Category:** Game
- **Runs per round:** 1
- **Unlock conditions:** Tic Tac Toe — Level 0 passed.
- **Screen:** Tic Tac Toe Puzzle Screen — a single pre-set board; no CPU
  turn, no "New Game" button.
- **Setup:** the board is generated so it is the learner's move (they are
  X, X/O counts equal) and **exactly one side has a winning move**: either
  X can complete three-in-a-row, or O threatens to and X must block. The
  other side has no winning move. There is exactly one correct cell (the
  win or the block).
- **Rules:** tapping the correct cell is a **Correct**; **any other tap is
  a loss** (Wrong). After answering, the correct cell is outlined (green if
  taken, yellow if missed) and a wrong tap is outlined red, then the next
  puzzle appears.
- **Variables:** `win_streak[0][WinOrBlock]`
- **Pass criteria:** `win_streak >= 8` (eight correct in a row).

### Chess — Level 0
- **Game UID:** 1
- **Subject:** Chess
- **Difficulty:** 0
- **Category:** Game
- **Runs per round:** 1
- **Unlock conditions:** Tic Tac Toe — Level 0 passed.
- **Screen:** Chess Board Screen
- **Pieces:** queen (random colour); capturable pawn ≥ 1;
  non-capturable pawn ≥ 1
- **Movement:** queen (see Rules § Chess piece movement)
- **Variables:** `win_streak[1][0]`
- **Pass criteria:** `win_streak[1][0] >= 8`

### Chess — Level 1
- **Game UID:** 1
- **Subject:** Chess
- **Difficulty:** 1
- **Category:** Game
- **Runs per round:** 1
- **Unlock conditions:** Chess — Level 0 passed.
- **Screen:** Chess Board Screen
- **Pieces:** queen (random colour); capturable opposite-colour pawn
  ≥ 1; non-capturable opposite-colour pawn ≥ 1; friendly pawn ≥ 1
- **Movement:** queen
- **Variables:** `win_streak[1][1]`
- **Pass criteria:** `win_streak[1][1] >= 8`

### Chess — Level 2
- **Game UID:** 1
- **Subject:** Chess
- **Difficulty:** 2
- **Category:** Game
- **Runs per round:** 1
- **Unlock conditions:** Chess — Level 1 passed.
- **Screen:** Chess Board Screen
- **Pieces:** rook (random colour); capturable opposite-colour pawn
  ≥ 1; non-capturable opposite-colour pawn ≥ 1; friendly pawn ≥ 1
- **Movement:** rook
- **Variables:** `win_streak[1][2]`
- **Pass criteria:** `win_streak[1][2] >= 8`

### Chess — Level 3
- **Game UID:** 1
- **Subject:** Chess
- **Difficulty:** 3
- **Category:** Game
- **Runs per round:** 1
- **Unlock conditions:** Chess — Level 2 passed.
- **Screen:** Chess Board Screen
- **Pieces:** bishop (random colour); capturable opposite-colour pawn
  ≥ 1; non-capturable opposite-colour pawn ≥ 1; friendly pawn ≥ 1
- **Movement:** bishop
- **Variables:** `win_streak[1][3]`
- **Pass criteria:** `win_streak[1][3] >= 8`

### Counting Addition — Level 0
- **Game UID:** 2
- **Subject:** Addition
- **Difficulty:** 0
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** always.
- **Screen:** Counting Equation Screen (addition operator)
- **Variables:** `addition_grid`, `win_streak[2][0]`
- **Random variables:**
  - `op1, op2 ∈ 0..4`
- **Problem selection:** standard math-grid selection
  (see Rules § Random problem selection (math grid))
- **Pass criteria:** `addition_grid[op1][op2] >= 2` for
  every `op1, op2 ∈ 0..4` **AND** `win_streak[2][0] >= 4`

### Horizontal Addition — Level 0
- **Game UID:** 2
- **Subject:** Addition
- **Difficulty:** 0
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** always.
- **Screen:** Horizontal Equation Screen (addition operator)
- **Variables:** `addition_grid`, `win_streak[2][0]`
- **Random variables:**
  - `op1, op2 ∈ 0..4`
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `addition_grid[op1][op2] >= 2` for every
  `op1, op2 ∈ 0..4` **AND** `win_streak[2][0] >= 4`

### Vertical Addition — Level 0
- **Game UID:** 2
- **Subject:** Addition
- **Difficulty:** 0
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** always.
- **Screen:** Vertical Equation Screen (addition operator)
- **Variables:** `addition_grid`, `win_streak[2][0]`
- **Random variables:**
  - `op1, op2 ∈ 0..4`
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `addition_grid[op1][op2] >= 2` for every
  `op1, op2 ∈ 0..4` **AND** `win_streak[2][0] >= 4`

### Number Line Addition — Level 0
- **Game UID:** 2
- **Subject:** Addition
- **Difficulty:** 0
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** always.
- **Screen:** Number Line Equation Screen (addition operator)
- **Variables:** `addition_grid`, `win_streak[2][0]`
- **Random variables:**
  - `op1, op2 ∈ 0..4`
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `addition_grid[op1][op2] >= 2` for every
  `op1, op2 ∈ 0..4` **AND** `win_streak[2][0] >= 4`

### Counting Addition — Level 1
- **Game UID:** 2
- **Subject:** Addition
- **Difficulty:** 1
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** All Addition Difficulty 0 passed and Number
  Line Addition 1 passed.
- **Screen:** Counting Equation Screen (addition operator)
- **Variables:** `addition_grid`, `win_streak[Counting Addition 1]`
- **Random variables:**
  - `op1, op2 ∈ 0..8`
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `addition_grid[op1][op2] >= 2` for every
  `op1, op2 ∈ 0..8` **AND** `win_streak[2][1] >= 4`

### Horizontal Addition — Level 1
- **Game UID:** 2
- **Subject:** Addition
- **Difficulty:** 1
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** All Addition Difficulty 0 passed and Number Line Addition 1 passed.
- **Screen:** Horizontal Equation Screen (addition operator)
- **Variables:** `addition_grid`, `win_streak[2][1]`
- **Random variables:**
  - `op1, op2 ∈ 0..8`
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `addition_grid[op1][op2] >= 2` for every
  `op1, op2 ∈ 0..8` **AND** `win_streak[2][1] >= 4`

### Vertical Addition — Level 1
- **Game UID:** 2
- **Subject:** Addition
- **Difficulty:** 1
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** All Addition Difficulty 0 passed and Number Line Addition 1 passed.
- **Screen:** Vertical Equation Screen (addition operator)
- **Variables:** `addition_grid`, `win_streak[2][1]`
- **Random variables:**
  - `op1, op2 ∈ 0..8`
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `addition_grid[op1][op2] >= 2` for every
  `op1, op2 ∈ 0..8` **AND** `win_streak[2][1] >= 4`

### Number Line Addition — Level 1
- **Game UID:** 2
- **Subject:** Addition
- **Difficulty:** 1
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** Number Line Addition 0 passed.
- **Screen:** Number Line Equation Screen (addition operator)
- **Variables:** `addition_grid`, `win_streak[2][1]`
- **Random variables:**
  - `op1, op2 ∈ 0..8`
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `addition_grid[op1][op2] >= 2` for every
  `op1, op2 ∈ 0..8` **AND** `win_streak[2][1] >= 4`

### Binary — Level 0
- **Game UID:** 8
- **Subject:** Binary
- **Difficulty:** 0
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** All Addition Difficulty 0 passed.
- **Screen:** Binary Vertical Equation Screen (`bits = 1`)
- **Variables:** `binary_grid`
- **Random variables:**
  - `operator ∈ {AND, OR, XOR}` chosen uniformly at random
  - `op1, op2 ∈ 0..1`
- **Answer surface:** Binary Keypad (1 slot)
- **Problem selection:** standard math-grid selection over the 3-D
  `(operator, op1, op2)` cell space.
- **Pass criteria:** `binary_grid[0][operator][op1][op2] >= 2` for
  every operator and every `op1, op2 ∈ 0..1`.

### Binary — Level 1
- **Game UID:** 8
- **Subject:** Binary
- **Difficulty:** 1
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** All Addition Difficulty 0 passed and Binary 0 passed.
- **Screen:** Binary Vertical Equation Screen (`bits = 3`)
- **Variables:** `binary_grid`
- **Random variables:**
  - `operator ∈ {AND, OR, XOR}` chosen uniformly at random
  - `op1, op2 ∈ 0..7` (rendered as zero-padded 3-bit binary)
- **Answer surface:** Binary Keypad (3 slots)
- **Problem selection:** standard math-grid selection over the 3-D
  `(operator, op1, op2)` cell space.
- **Pass criteria:** `binary_grid[1][operator][op1][op2] >= 2` for
  every operator and every `op1, op2 ∈ 0..7`.

### Counting Multiplication — Level 0
- **Game UID:** 9
- **Subject:** Multiplication
- **Difficulty:** 0
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** All Subtraction Difficulty 0 passed.
- **Screen:** Counting Multiplication Screen
- **Variables:** `multiplication_grid`
- **Random variables:**
  - `op1, op2 ∈ 0..4` (max product 16)
  - A random animal emoji per problem (independent of streak)
- **Answer surface:** Numeric Grid (0..16)
- **Problem selection:** standard math-grid selection over the
  `(op1, op2)` cell space.
- **Pass criteria:** `multiplication_grid[op1][op2] >= 2` for every
  `op1, op2 ∈ 0..4`.

### Counting Multiplication — Level 1
- **Game UID:** 9
- **Subject:** Multiplication
- **Difficulty:** 1
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** Counting Multiplication — Level 0 passed.
- **Screen:** Counting Multiplication Screen (same boxed animal groups),
  but the operands are hidden and the answer surface is the Operand
  Picker (below) instead of the numeric grid. The product is never shown.
- **Variables:** `multiplication_operands_grid` — a separate coverage
  grid from Level 0, cells indexed `(op1, op2) ∈ 1..4`.
- **Random variables:**
  - `op1, op2 ∈ 1..4` (**never 0**)
  - A random animal emoji per problem
- **Answer surface:** Operand Picker — buttons `1..4`; the first tap fills
  the first operand blank, the second tap fills the second and submits.
  The answer is **order-independent** (a × b ≡ b × a). A **Clear** button
  undoes the first pick before submitting.
- **Problem selection:** standard math-grid selection over the
  `(op1, op2) ∈ 1..4` cell space.
- **Pass criteria:** `multiplication_operands_grid[op1][op2] >= 2` for
  every `op1, op2 ∈ 1..4`.

### Horizontal Multiplication — Level 0
- **Game UID:** 9
- **Subject:** Multiplication
- **Difficulty:** 0
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** Number Line Multiplication — Level 0 passed.
- **Screen:** Horizontal Equation Screen (`×` operator)
- **Variables:** `multiplication_equation_grid`, `win_streak[9][Horizontal]`
- **Random variables:**
  - `op1, op2 ∈ 0..4` (max product 16)
- **Answer surface:** Numeric Grid (0..16)
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `multiplication_equation_grid[op1][op2] >= 2` for
  every `op1, op2 ∈ 0..4` **AND** `win_streak >= 4`

### Vertical Multiplication — Level 0
- **Game UID:** 9
- **Subject:** Multiplication
- **Difficulty:** 0
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** Number Line Multiplication — Level 0 passed.
- **Screen:** Vertical Equation Screen (`×` operator)
- **Variables:** `multiplication_equation_grid`, `win_streak[9][Vertical]`
- **Random variables:**
  - `op1, op2 ∈ 0..4`
- **Answer surface:** Numeric Grid (0..16)
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `multiplication_equation_grid[op1][op2] >= 2` for
  every `op1, op2 ∈ 0..4` **AND** `win_streak >= 4`

### Number Line Multiplication — Level 0
- **Game UID:** 9
- **Subject:** Multiplication
- **Difficulty:** 0
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** Counting Multiplication — Level 0 passed.
- **Screen:** Number Line Equation Screen (`×` operator) — the scrollable,
  markable line from 0; the learner still taps the product.
- **Variables:** `multiplication_equation_grid`, `win_streak[9][NumberLine]`
- **Random variables:**
  - `op1, op2 ∈ 0..4`
- **Answer surface:** Numeric Grid (0..16)
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `multiplication_equation_grid[op1][op2] >= 2` for
  every `op1, op2 ∈ 0..4` **AND** `win_streak >= 4`

The three share one `multiplication_equation_grid` (product coverage) but
each keeps its own streak, so they pass independently — mirroring the
Addition / Subtraction Level 0 groups. This grid is separate from the
counting-multiplication grids.

### Horizontal / Vertical / Number Line Multiplication — Level 1
- **Game UID:** 9
- **Subject:** Multiplication
- **Difficulty:** 1
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** all three symbolic Multiplication Level 0 lessons
  passed (Horizontal, Vertical, and Number Line Multiplication 0).
- **Screen:** the matching Horizontal / Vertical / Number Line Equation
  Screen (`×`). The Number Line screen scrolls (it runs 0 to the answer +
  10, rounded to the next ten — up to 90 for the largest products).
- **Variables:** `multiplication_equation_grid` (the same grid as Level 0,
  now covering the `0..9` slice), plus each lesson's own `win_streak`.
- **Random variables:** `op1, op2 ∈ 0..9` (max product 81).
- **Answer surface:** **Number Pad** — the learner types the product and
  presses **Enter** (products up to 81 are too many for a tap grid).
- **Problem selection:** standard math-grid selection over `0..9`.
- **Pass criteria:** `multiplication_equation_grid[op1][op2] >= 2` for
  every `op1, op2 ∈ 0..9` **AND** `win_streak >= 4` (per lesson).

Like Level 0, the three presentations share the grid and pass
independently.

### Counting Subtraction — Level 0
- **Game UID:** 7
- **Subject:** Subtraction
- **Difficulty:** 0
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** All Addition Difficulty 1 passed.
- **Screen:** Counting Equation Screen (subtraction operator)
- **Variables:** `subtraction_grid`, `win_streak[7][0]`
- **Random variables:**
  - `op1 ∈ 4..9`, `op2 ∈ 0..4`
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `subtraction_grid[op1][op2] >= 2` for
  every `op1 ∈ 4..9`, `op2 ∈ 0..4` **AND** `win_streak[7][0] >= 4`

### Horizontal Subtraction — Level 0
- **Game UID:** 7
- **Subject:** Subtraction
- **Difficulty:** 0
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** All Addition Difficulty 1 passed.
- **Screen:** Horizontal Equation Screen (subtraction operator)
- **Variables:** `subtraction_grid`, `win_streak[7][0]`
- **Random variables:**
  - `op1 ∈ 4..9`, `op2 ∈ 0..4`
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `subtraction_grid[op1][op2] >= 2` for every
  `op1 ∈ 4..9`, `op2 ∈ 0..4` **AND** `win_streak[7][0] >= 4`

### Vertical Subtraction — Level 0
- **Game UID:** 7
- **Subject:** Subtraction
- **Difficulty:** 0
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** All Addition Difficulty 1 passed.
- **Screen:** Vertical Equation Screen (subtraction operator)
- **Variables:** `subtraction_grid`, `win_streak[7][0]`
- **Random variables:**
  - `op1 ∈ 4..9`, `op2 ∈ 0..4`
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `subtraction_grid[op1][op2] >= 2` for every
  `op1 ∈ 4..9`, `op2 ∈ 0..4` **AND** `win_streak[7][0] >= 4`

### Number Line Subtraction — Level 0
- **Game UID:** 7
- **Subject:** Subtraction
- **Difficulty:** 0
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** All Addition Difficulty 1 passed.
- **Screen:** Number Line Equation Screen (subtraction operator)
- **Variables:** `subtraction_grid`, `win_streak[7][0]`
- **Random variables:**
  - `op1 ∈ 4..9`, `op2 ∈ 0..4`
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `subtraction_grid[op1][op2] >= 2` for every
  `op1 ∈ 4..9`, `op2 ∈ 0..4` **AND** `win_streak[7][0] >= 4`

### Letter Sounds — Level 0
- **Game UID:** 3
- **Subject:** Letter Sounds
- **Difficulty:** 0
- **Category:** Reading
- **Runs per round:** 2 (Reading default)
- **Unlock conditions:** always (entry-level Reading lesson, and the head
  of the whole Reading chain).
- **Screen:** Letter Sound Clip Screen — a large tappable speaker plays a
  pre-recorded word clip; tapping it replays the clip.
- **Variables:** `win_streak[3]` — a per-letter slot plus the `run` slot
  (`win_streak[3][run]`).
- **Audio:** two pre-cut clips per letter, bundled under
  `app/src/main/res/raw`: `<x>3.mp3` (the word, played as the question)
  and `<x>1.mp3` (the letter, played back after the learner answers).
  E.g. `a3.mp3` / `a1.mp3`. The whole alphabet A–Z is present; the letter
  set is the list in `reading/LetterSounds.kt`.
- **Problem selection:** pick a letter that still has
  `win_streak[3][letter] < 2` where possible; avoid immediately
  repeating the previous letter when more than one is available. Play
  that letter's word clip and ask which letter it starts with (A–Z
  keypad).
- **Show answer:** after any answer (correct, wrong, or Give up) the
  letter clip (`<x>1.mp3`) plays as reinforcement. The lesson waits for
  the clip to finish in full (plus a short buffer, and never less than the
  usual feedback hold) before advancing, so it is never cut off.
- **Pass criteria (both required):**
  - `win_streak[3][run] >= 8` (eight correct answers in a row), AND
  - `win_streak[3][letter] >= 2` for every letter that has a clip.

  Any wrong answer (or Give up) resets both the run streak and the
  current letter's streak to 0.

### Phonemes — Level 0
- **Game UID:** 3
- **Subject:** Phonemes
- **Difficulty:** 0
- **Category:** Reading
- **Runs per round:** 3
- **Show answer time:** 4 seconds (the answer reveals all three words,
  which take longer to read than a single answer).
- **Unlock conditions:** Letter Sounds — Level 0 passed.
- **Screen:** Phoneme Trio Screen
- **Variables:** `win_streak[3]` (one slot per word; the Game UID 3 slot
  space is shared with Letter Sounds, keyed separately per lesson)
- **Word bank:** `app/src/main/assets/config.yaml` under `phonemes` —
  19 letter groups: b, c, d, f, g, h, j, k, l, m, n, p, r, s, t, v, w,
  y, z. (`/k/` is split into c-words and k-words because the answer is
  a letter, not a phoneme. `/ng/` and `/zh/` are excluded because they
  don't appear word-initially in English.)
- **Problem selection:** group words by first letter; prefer a letter
  whose words still have at least one `win_streak[3][word] < 2`
  (10% wildcard for uniformly random letter); draw 3 random words from
  that letter's list.
- **Pass criteria:** `win_streak[3][word] >= 2` for every word
  in the bank.

### Animals — Level 0
- **Game UID:** 4
- **Subject:** Animals
- **Difficulty:** 0
- **Category:** Reading
- **Runs per round:** 2
- **Unlock conditions:** Phonemes — Level 0 passed.
- **Screen:** Animal Picture Screen
- **Variables:** `win_streak[4]`
- **Problem selection:** per-word list selection
  (see Rules § Random problem selection (per-word list)) over the set
  of letters with a mapped animal emoji.
- **Pass criteria:** `win_streak[4][letter] >= 2` for every mapped
  letter. letters are mapped with A -> 0, B -> 1, etc

### Sight Words — Level 0
- **Game UID:** 5
- **Subject:** Sight Words
- **Difficulty:** 0
- **Category:** Reading
- **Runs per round:** 2
- **Unlock conditions:** Animals — Level 0 passed.
- **Screen:** Word Display Screen
- **Variables:** `win_streak[5]`
- **Word bank:** `app/src/main/assets/config.yaml` under
  `sight_words.words`
- **Random variables:**
  - `position = 0` (only the first letter is ever blanked)
- **Problem selection:** per-word list selection over the word bank.
- **Pass criteria:** `win_streak[5][word][0] >= 2` for every word
  (the first-letter slot of each word).


### Sight Words — Level 1
- **Game UID:** 5
- **Subject:** Sight Words
- **Difficulty:** 1
- **Category:** Reading
- **Runs per round:** 2
- **Unlock conditions:** Sight Words — Level 0 passed.
- **Screen:** Word Display Screen
- **Variables:** `win_streak[5]` (per `(word, position)`; shared with
  Level 0)
- **Word bank:** same as Level 0
- **Random variables:**
  - `position` chosen uniformly at random within the word
- **Problem selection:** per-word list selection over the set of
  `(word, position)` pairs.
- **Pass criteria:** `win_streak[5][word][p] >= 2` for every word
  and every letter position `p` of every word.

### Rhyming Words — Level 0
- **Game UID:** 6
- **Subject:** Rhyming Words
- **Difficulty:** 0
- **Category:** Reading
- **Runs per round:** 2
- **Unlock conditions:** Sight Words — Level 1 passed.
- **Screen:** Word Display Screen
- **Variables:** `win_streak[6]` (one slot per word)
- **Word bank:** `app/src/main/assets/config.yaml` under
  `rhyming_words.groups`
- **Random variables:**
  - `position = 0`
- **Problem selection:** per-word list selection.
- **Pass criteria:** `win_streak[6][word] >= 2` for every word.

## Unlocking

A lesson is **unlocked** if its unlock conditions are met. Conditions
can be:

- **Always** — the lesson is at the head of its chain.
- **Single parent** — the named lesson is passed (`lesson_passed[parent]
  == true`).
- **All-of** — every named lesson is passed. Used for the cross-screen
  Addition / Subtraction tiers; spelled either as "All &lt;Subject&gt;
  Difficulty N passed" (e.g. all four Counting/Horizontal/Vertical/
  Number Line variants of Addition at Difficulty 0) or, equivalently,
  as the lesson-by-lesson list.

A lesson is **passed** the first time its pass criteria are met. Once
passed, the lesson stays passed even if the learner subsequently fails
problems within it. A passed lesson can still be opened and practiced
from the menu.

### Manual unlock

The Progress screen exposes a Switch beside every lesson. Flipping it
on **manually unlocks** the lesson — it appears in the menu and the
Random / Mixed rotation right away, even if its prerequisites haven't
been passed yet. Flipping it off relocks the lesson (it remains
available if its parents have been passed naturally). The switch sets
`lesson_manual_unlock[lesson]`; it does **not** mark the lesson as
passed. The learner still has to play and pass the lesson the normal
way for it to count as done — and so for downstream lessons whose
gate references it to unlock.

Useful for letting a learner jump ahead to try a more advanced lesson
before mastering everything below it, or for skipping past a lesson
the parent doesn't want them to drill right now. Switches on
entry-level lessons (no prerequisites) are disabled — those lessons
are always available regardless.

## Random / Mixed mode

When the **Random / Mixed** menu option is active, after every
completed problem or game the app draws the next activity randomly
from the pool of **all currently unlocked lessons**.

**Weighting:**

- Unlocked but **not yet passed** lessons get weight **2**.
- Unlocked **and already passed** lessons get weight **1**.

So unfinished lessons are about twice as likely to appear as
completed ones, but earlier (already-passed) lessons still come up
regularly. The just-completed lesson is excluded from the next draw
whenever the pool contains more than one entry, so two different
lessons are never drawn back-to-back.

Each draw runs the picked lesson's **Runs per round** (see Rules §
Runs per round) problems / games before re-drawing. Counts live in
`app/src/main/assets/config.yaml`:

```yaml
session:
  math_runs_per_round: 4
  game_runs_per_round: 1
  reading_runs_per_round: 2
  # Per-lesson overrides
  phonemes_runs_per_round: 3
```

## Persistence

All variables described in the Variables section — the single
`win_streak` (every consecutive-correct / non-loss streak in the app),
`addition_grid`, `subtraction_grid`, `multiplication_grid`,
`binary_grid`, plus `lesson_passed` and `lesson_manual_unlock` — are
written to the device's app-local storage and reloaded on launch. They
survive closing the app, killing it from recents, and rebooting the
device.
