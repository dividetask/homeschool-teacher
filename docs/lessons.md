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

Every lesson also has a universal fast path: a run of eight correct
answers in a row passes it outright, regardless of its listed pass
criteria (see Rules → *Universal win-streak pass*). A lesson can be
marked passed (or un-passed) by hand from the Progress screen too.

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
| 10  | Position Words |
| 11  | Division       |

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
- **Every lesson** additionally keeps a `run[lesson]` slot — a plain
  consecutive-correct run across the whole lesson (increment on correct,
  reset to zero on any wrong answer or **Give up**). It drives the
  universal pass rule (see Rules → *Universal win-streak pass*). Games,
  which are scored per game rather than per answer, use their existing
  non-loss slot as this run.

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
Integer 2D array, `op1` and `op2` indexed `0..15` (Level 0 fills the
`0..4` slice, Level 1 the `0..8` slice). Product-coverage grid
shared by the Horizontal / Vertical / Number Line multiplication screens
(tap-the-product lessons). Separate from `multiplication_grid` (the
counting/product lesson) and `multiplication_operands_grid`. Default zero.

### `multiplication_operands_grid[op1][op2]`
Integer 2D array, `op1` and `op2` indexed `1..4`. Cell tracks correct
identifications of the two operands in Multiplication Operands — Level 0
(separate from `multiplication_grid`, which tracks products in Counting
Multiplication).
Default zero.

### `addition_grid[op1][op2]`
Integer 2D array, `op1` and `op2` indexed `0..19` (length 20 each). The
row index is a first operand, which subtraction takes to 16; no operand
itself passes 8. Cell
tracks correct answers for the matching addition problem when shown via
the Vertical, Horizontal, or Number Line equation screens. Shared
across those three screens.

### `subtraction_grid[op1][op2]`
Integer 2D array, `op1` and `op2` indexed `0..19`. Cell tracks correct
answers for the matching subtraction problem when shown via the
Vertical, Horizontal, or Number Line equation screens. Shared across
those three screens.

### `division_grid[level][dividend][divisor]`
Integer 3D array. `level ∈ 0..1`; `dividend` indexed `0..40`; `divisor`
indexed `0..8`. Cell tracks correct answers for `dividend ÷ divisor` at
that level. Only the cells where the division comes out whole are ever
asked, so the grid is **sparse by design** — `division_grid[0][7][2]`,
for instance, stays at zero forever and is not counted towards mastery.
Each level keeps its own coverage: answering without the pens giving the
answer away is the point of Level 1, so Level 0's coverage must not pass
it. Default zero.

### `lesson_passed[lesson]`
Boolean, one per lesson. Sticky: once a lesson's pass criteria are met
the flag is set to `true` and stays `true` even if streaks subsequently
drop.

### `lesson_manual_unlock[lesson]`
Boolean, one per lesson. Flipping the Progress screen's **Unlocked**
switch sets this. When `true`, the lesson is considered unlocked
regardless of whether its parents are passed (see Manual unlock).
Independent of `lesson_passed[lesson]` — manually unlocking a lesson
does not mark it as completed.

### `lesson_manual_override[lesson]`
Boolean, one per lesson. Tracks the Progress screen's **Passed** switch.
When `true`, the lesson's automatic pass evaluation is suppressed — the
`lesson_passed` value is pinned by hand. Turning **Passed** on sets both
`lesson_passed` and this flag to `true`; turning it off sets both back to
`false`, so the lesson can be earned normally again.

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
next problem. An operand of **0** draws as blank space about one animal
wide, so the equation keeps its shape and the gap reads as "none here".
For subtraction the operator becomes `-`. The single animal species used
is chosen at random per problem.

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

- **always starts at 0** and runs to
  `next_multiple_of_ten(lesson_max_answer + 10)`, where
  `lesson_max_answer` is the largest answer the lesson can ask — so the
  line is the same length for every problem in a lesson (sizing it off
  the current answer instead would draw a stubby line for an easy
  problem, such as one with a zero operand, and a long one for the next),
  and every answer sits comfortably inside the range, never at the edge;
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
Standard 3×3 board. A **large X or O sits above the board, at the left of
the score row**, showing which mark the learner is playing this game. The
learner taps an empty cell to place their mark; the CPU then plays. Game
ends on a win, draw, or loss. On any win a **large line is drawn through
the three winning cells**.

### Tic Tac Toe Puzzle Screen
Standard 3×3 board (large player X above the board, left of the scores),
pre-filled with a
single-move puzzle position and no CPU turn and no instruction text. The
learner taps one empty cell:

- **Correct win** — they complete three-in-a-row; the winning line is
  drawn.
- **Correct block** — they take the opponent's threatened cell; nobody
  wins.
- **Missed win** — the correct cell blinks a red X, then the winning line
  they missed is drawn.
- **Missed block** — the opponent moves into the threatened cell to punish
  the mistake, then the winning line is drawn.

Then the next puzzle is dealt. Used by "Win or Block".

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

### Counting Addition Intro

The worked example that opens the counting addition lessons (see Rules §
Lesson intros). Four steps, on a screen of its own:

1. The two groups of animals with the `+` and `= ?` between them —
   exactly the picture the lesson puts up. The problem is spoken: "3
   plus 2".
2. The groups slide together and the operator and question mark clear
   away, leaving one row of animals.
3. The row is counted off one animal at a time, left to right, each
   number appearing above its animal as it is said. Those numbers are
   the only writing the intro puts on screen.
4. The whole sentence is spoken — "3 plus 2 equals 5 zebras", with the
   animal's own name — over the counted row, whose last number is the
   answer.

The animals shrink to fit as the total grows, so the biggest problem the
Level 1 range allows still counts along a single row, and the count runs
a little quicker above eight so a long one does not drag.

### Letter Sound Clip Screen
A large tappable speaker button in the centre of the screen. On each new
problem it plays a pre-recorded clip of a word (`<x>3.mp3`); tapping the
speaker replays it. The learner taps the letter the word starts with.
After any answer the matching letter clip (`<x>1.mp3`) plays as
reinforcement, and the screen holds until it has finished (see the
lesson's **Show answer** row). A score row above shows correct, current
streak, and wrong counts.

**Answer surface:** full A–Z keypad.

### Word Display Screen
A word shown with one letter replaced by an underscore; the whole word
is spoken aloud via TTS. Tapping the word replays the audio.

**Answer surface:** full A–Z keypad.

### Rhyme Choice Screen
Used by the Rhyming Words lessons. A prompt plus a vertical list of rows,
each a word **answer button** with a separate **🔊 play button to its
left** (tapping the answer button answers; the 🔊 replays just that
word). On each new problem all the words are read aloud in sequence, and
the word currently being spoken is highlighted (yellow outline). Level 0
shows the spoken **target word** at the top ("which rhymes with X?");
Level 1 shows no target ("which does NOT rhyme?"). The correct button
turns green and a wrong pick turns red. A 🔊 Repeat button replays the
whole read-out.

**Answer surface:** the word answer buttons (one per choice).

### Position Scene Screen
Used by the Position Words lessons. A depicted scene (an animal emoji
placed on / in / over / under an object emoji) above the sentence
`The __ is __ the __.` with one blank, and a wrapping row of word-choice
buttons. Tapping a word answers; the blank fills in (green if correct,
yellow on reveal) and the correct button turns green / a wrong pick red.

**Answer surface:** the word-choice buttons.

### Phoneme Trio Screen
Three words played in sequence via TTS at the three rates. The words
are masked on screen as `🔊 1`, `🔊 2`, `🔊 3` until the learner
answers, then they are revealed alongside the green/red feedback. A
🔊 Repeat button replays the whole sequence.

**Answer surface:** A–Z Keypad.

### Counting Multiplication Screen
The equation displayed on its own line followed by `op1` groups, each
containing `op2` copies of a randomly-picked animal emoji — multiplication
is always read as "`op1` groups of `op2`". **Each group is drawn inside
its own rounded box, with a wide gap between boxes**, so the "this many
groups of this many" structure is clear. Groups flow left-to-right and
wrap to additional lines as needed; a single group is never split across
a line. Example for `op1 = 2`, `op2 = 4`:

```
2 × 4 = ?
[🐱🐱🐱🐱]  [🐱🐱🐱🐱]
```

For products that don't fit on one line (e.g. `4 × 4 = 16`), the
boxed groups wrap onto two or three lines while staying visually
grouped. When either operand is 0 the area shows "(no 🐱)" instead of
empty space.

**Answer surface:** Numeric Grid (0..max).

### Animal Division Screen
The problem stated as `X ÷ Y = ?` on its own line, with `X` animals of a
single randomly-picked species loose in the middle of the screen and a
column of empty **pens** down the side:

```
        12 ÷ 3 = ?

  ┌────┐
  │    │     🐰🐰🐰🐰🐰🐰
  ├────┤     🐰🐰🐰🐰🐰🐰
  │    │
  ├────┤
  │    │
  └────┘
```

Tapping a pen selects it (a thicker, tinted border); tapping any animal
in the middle then moves one animal into that pen. Tapping an animal
already inside a pen sends it back to the middle, and a **Start over**
button empties every pen at once. The arrangement resets on each new
problem.

Sharing the animals out is **only an aid** — the answer is graded off
the tapped number, so a learner who already knows it can answer without
moving anything, and a learner who shares them unevenly is not marked
wrong for it.

How many pens appear is what separates the two levels:

- **Level 0** puts out exactly `Y` pens, so filling them evenly makes
  the answer visible — this is the level that *shows* what dividing is.
- **Level 1** always puts out **eight** pens — as many as the widest
  divisor the level can ask — whatever the divisor actually is, so the
  count of pens never gives the answer away and the learner has to work
  out how many of them to use.

**Answer surface:** Numeric Grid (0..8) — the answer is how many
land in each pen, which never passes the level's operand ceiling, not
the dividend.

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

A **Cheat sheet** button reveals a full-screen overlay of the single-bit
truth table for **the current operator only** — the four `a OP b`
combinations for `a, b ∈ {0, 1}`, each drawn in the **same stacked layout
and size as the problem itself** (operands stacked, `₂` subscripts, rule
line, result). AND shows the AND table; OR the OR table; XOR the XOR
table. For Level 0 these four are every possible question; for Level 1
they are the per-column rule for each of the three bits. Pressing the
button again hides it; otherwise it auto-hides after 16 seconds (or on a
tap). It resets to hidden on each new problem.

**Answer surface:** Binary Keypad with `bits` slots.

## Answer Surfaces

The keyboard / button group used to capture the learner's answer.
Lessons cite a surface by name from this list rather than describing
it inline.

### Numeric Grid (0..max)
Grid of single-tap buttons, each labelled with one candidate answer
from `0` to `max`. One shared surface behind every math lesson that taps
its answer — the equation screens, Counting Multiplication and Counting
Division alike — so the button size and colours never differ between
them. Column count scales with `max` so the grid stays comfortable on a
phone (5 columns through `max = 18`, 7 columns above that). Tapping a
button submits that value as the answer.

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
Row of single-tap buttons `1..4` used by Multiplication Operands.
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

### Lesson intros

Some lessons open with a **worked example**: a short animation that
solves one problem end to end, narrated, before the questions start.

An intro is **not a lesson**. Nothing is asked, nothing is scored, and
nothing about it is stored — it has no entry in the catalog, no pass
criteria, and no row on the Progress screen. It is a preamble attached
to a lesson.

- **When it plays.** Once at the start of a round, whichever way the
  round began — picked from the menu, or drawn by Random / Mixed. A
  round of four questions plays it once, before the first of them, not
  before each. Coming back to the same lesson later is a new round, so
  it plays again.
- **What it shows.** Its own problem, rolled at random, independent of
  the questions that follow. Operands are drawn from the lesson's
  standard range but are **never 0 or 1**: a worked example needs
  something to work, and nothing merges into a group of zero or takes
  more than a moment to count when there is one of something. The
  questions still ask those cells.
- **Interaction.** None. No answer surface, no buttons, no skip; it ends
  on its own and the lesson appears.
- **Sound.** One pass of narration at normal speed (not the three speeds
  the reading lessons use). The narration is where the words live —
  **nothing is written on screen**. An intro shows the same picture its
  lesson shows and marks it up as it works (the numbers written over the
  animals as they are counted, say), but it never spells out the
  equation or the sentence. With the sound off, the pictures carry it.

A lesson with no intro written yet simply starts as it always has.

### Standard operand ranges

Every Math lesson outside Binary draws from one of four ranges, chosen by
its **operation family** and its **Difficulty**. A lesson states its
Difficulty and inherits the range; it does not invent its own.

`Z` is the big number in both multiplication families — the product a
multiplication asks for, and the dividend a division starts from — so a
Level 1 sheet of either never runs past 40.

| Family                     | Difficulty 0            | Difficulty 1            |
| -------------------------- | ----------------------- | ----------------------- |
| Addition / Subtraction     | `op1, op2 ∈ 0..4`       | `op1, op2 ∈ 0..8`       |
| Multiplication / Division  | `X, Y ∈ 0..4`, `Z ∈ 0..16` | `X, Y ∈ 0..8`, `Z ∈ 0..40` |

Read each pair of families as one triple seen from either side:
`X + Y = Z` for addition and `Z - X = Y` for subtraction, `X × Y = Z`
for multiplication and `Z ÷ X = Y` for division. In both cases the
forward operation takes `X` and `Y` from the range and lets `Z` fall
out; the backward one takes `X` and the **answer** `Y` from the range
and derives the `Z` it starts from.

Three constraints fall out of the ranges rather than being stated per
lesson:

- **Subtraction is built from the answer, not from two operands.** The
  number being taken away and the answer both come from the family
  range, and the number they are taken from is their sum — so it runs to
  **twice** the family ceiling (8 at Difficulty 0, 16 at Difficulty 1)
  while the answer stays inside the range and can never go negative.
  This is the same construction division uses, and it makes each level
  the exact inverse of the addition level beside it: Addition Level 1
  adds two numbers up to 8 and lands in 0..16, Subtraction Level 1 takes
  a number up to 8 off something in 0..16.
- **Division excludes a zero divisor and a zero dividend.** `X` and `Y`
  each run `1..4` / `1..8`; `Z` is their product. Dividing zero by
  something is degenerate, and dividing by zero is undefined.
- **`Z` is a ceiling, not a guarantee.** Where `X × Y` would exceed it —
  `7 × 8` at Difficulty 1 — that pair is simply not asked. So the
  Difficulty 1 grids are sparse in their top-right corner, the same way
  the division grid is sparse everywhere `X` does not divide `Z`.

Pass criteria are judged over the cells a lesson can actually ask, never
over cells its ceiling rules out.

### Easy cells

Some cells are right on sight and are not worth drilling like the rest:

| Operation      | Easy when                                              |
| -------------- | ------------------------------------------------------ |
| Addition       | `op1 == 0` or `op2 == 0`                               |
| Subtraction    | `op1 == 0` or `op2 == 0`                               |
| Multiplication | either operand is `0` or `1`                           |
| Division       | `divisor <= 1` (or `dividend == 0`, never asked)       |

Two things follow, wherever a lesson uses an operand grid:

- **`cell_target(op1, op2)`** — the count a cell needs before it counts
  as covered — is **1** for an easy cell and **2** for every other cell.
- Easy cells are drawn at **half the weight** of an ordinary cell, so
  they come up about half as often. This holds both while the lesson is
  still being covered and once every cell is covered.

Grids that are not arithmetic (the binary AND/OR/XOR grids, the per-word
reading lists) have no easy cells: every one of their cells needs 2 and
every one is drawn at the same weight. The binary lessons otherwise run
the same selection as the arithmetic ones, over their own
`(operator, op1, op2)` cells.

### Balanced operands

A lesson's cells are not always spread evenly across its operands.
Division is the clear case, because the dividend has to be a whole
number of groups: at Difficulty 1 the divisors `1..5` each own eight
cells, while `÷ 6` owns six and `÷ 7` and `÷ 8` own five apiece — the
bigger the divisor, the fewer quotients fit under the `Z` ceiling. Left
alone, the small divisors crowd out exactly the ones a learner finds
hard.

Where a lesson names a **balance operand**, every distinct value of that
operand comes up equally often, however many cells it owns: a cell's pick
weight is divided by the number of cells in the pool sharing its balance
value. This multiplies with the easy-cell weight rather than replacing
it, so `÷ 1` is damped twice — once for being one divisor of several,
once for being easy.

Only the division lessons name one, and it is the **divisor**. It leaves
`÷ 1` at roughly a fifteenth of Difficulty 1 problems and the other
seven divisors level with each other.

### Random problem selection (math grid)

For lessons that use a 2D operand grid:

1. Roll `wildcard ∈ 1..10`.
2. If `wildcard == 1`, or every cell has reached its `cell_target`,
   draw from the lesson's whole declared operand range.
3. Otherwise, draw from the cells furthest behind their `cell_target`
   (largest `cell_target(op1, op2) - grid[op1][op2]` first), so a cell
   that has never been asked outranks one already answered right once.

Every draw is weighted: an easy cell counts half as much as an ordinary
one (see Rules § Easy cells), and where the lesson names a balance
operand, a cell counts for less the more cells share its value of that
operand (see Rules § Balanced operands). Ties are otherwise broken
uniformly at random.

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

### CPU level slip

Tic Tac Toe above Level 0 gives the learner a way through: when a game
starts, there is a **10% chance** the CPU plays that game at a level
drawn uniformly from the levels below the lesson's own (so Level 2 may
play at Level 1 or Level 0, Level 1 at Level 0). The choice is made once
per game and holds for every move in it. Level 0 has nothing below it
and always plays its own rule.

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
highlighted, then the app advances. Every lesson holds for the same
times, so the pacing does not change from screen to screen:

- **Correct answer:** 0.9 seconds.
- **Wrong answer:** 2 seconds.
- **Give up / reveal:** 1.6 seconds.

Two lessons wait longer because they are still playing audio when the
answer lands, and both say so in their own definition: **Phonemes**
carries a **Show answer time** row (a single value replacing all three),
and **Letter Sounds** holds until its letter clip has finished. A
**Show answer time** row appears only on a lesson that overrides these
values; its absence means the three above apply.

(Tic Tac Toe is a game rather than a single-answer problem; it instead
waits the configurable `tictactoe.auto_restart_seconds` after a game
ends. The Win-or-Block puzzle screen runs its own staged timeline —
blink the missed move, draw the winning line, let the opponent punish a
missed block — rather than a single hold.)

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
| 7        | Counting Subtraction — Level 1      | Math     | All Subtraction Difficulty 0 passed |
| 9        | Counting Multiplication — Level 0   | Math     | All Subtraction Difficulty 0 passed |
| 9        | Multiplication Operands — Level 0   | Math     | Counting Multiplication 0 passed  |
| 9        | Number Line Multiplication — Level 0| Math     | Counting Multiplication 0 passed  |
| 9        | Horizontal Multiplication — Level 0 | Math     | Number Line Multiplication 0 passed |
| 9        | Vertical Multiplication — Level 0   | Math     | Number Line Multiplication 0 passed |
| 9        | Horizontal Multiplication — Level 1 | Math     | All symbolic Multiplication Diff 0 passed |
| 9        | Vertical Multiplication — Level 1   | Math     | All symbolic Multiplication Diff 0 passed |
| 9        | Number Line Multiplication — Level 1| Math     | All symbolic Multiplication Diff 0 passed |
| 11       | Counting Division — Level 0         | Math     | All symbolic Multiplication Diff 1 + Multiplication Operands 0 |
| 11       | Counting Division — Level 1         | Math     | Counting Division 0 passed        |
| 3        | Letter Sounds — Level 0             | Reading  | —                                 |
| 3        | Phonemes — Level 0                  | Reading  | Letter Sounds 0                   |
| 4        | Animals — Level 0                   | Reading  | Phonemes 0                        |
| 5        | Sight Words — Level 0               | Reading  | Animals 0                         |
| 5        | Sight Words — Level 1               | Reading  | Sight Words 0                     |
| 6        | Rhyming Words — Level 0             | Reading  | Sight Words 1                     |
| 6        | Rhyming Words — Level 1             | Reading  | Rhyming Words 0                   |
| 10       | Position Words — Level 0            | Reading  | Rhyming Words 1                   |
| 10       | Position Words — Level 1            | Reading  | Position Words 0                  |
| 10       | Position Words — Level 2            | Reading  | Position Words 1                  |

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
  legal move. In **10% of games** the CPU instead plays at a randomly
  chosen lower level for that whole game (see Rules § CPU level slip).
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
  move. In **10% of games** the CPU instead plays at a randomly chosen
  lower level for that whole game (see Rules § CPU level slip).
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
  a loss** (Wrong). The consequence is then shown (see Tic Tac Toe Puzzle
  Screen: a missed win blinks the correct move red then draws the line; a
  missed block lets the opponent complete their win) before the next
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
- **Random variables:** the standard range for this family and
  Difficulty (see Rules § Standard operand ranges)
- **Problem selection:** standard math-grid selection
  (see Rules § Random problem selection (math grid))
- **Pass criteria:** `addition_grid[op1][op2] >= cell_target(op1, op2)`
  for every cell the lesson can ask **AND** `win_streak[2][0] >= 4`

### Horizontal Addition — Level 0
- **Game UID:** 2
- **Subject:** Addition
- **Difficulty:** 0
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** always.
- **Screen:** Horizontal Equation Screen (addition operator)
- **Variables:** `addition_grid`, `win_streak[2][0]`
- **Random variables:** the standard range for this family and
  Difficulty (see Rules § Standard operand ranges)
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `addition_grid[op1][op2] >= cell_target(op1, op2)`
  for every cell the lesson can ask **AND** `win_streak[2][0] >= 4`

### Vertical Addition — Level 0
- **Game UID:** 2
- **Subject:** Addition
- **Difficulty:** 0
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** always.
- **Screen:** Vertical Equation Screen (addition operator)
- **Variables:** `addition_grid`, `win_streak[2][0]`
- **Random variables:** the standard range for this family and
  Difficulty (see Rules § Standard operand ranges)
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `addition_grid[op1][op2] >= cell_target(op1, op2)`
  for every cell the lesson can ask **AND** `win_streak[2][0] >= 4`

### Number Line Addition — Level 0
- **Game UID:** 2
- **Subject:** Addition
- **Difficulty:** 0
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** always.
- **Screen:** Number Line Equation Screen (addition operator)
- **Variables:** `addition_grid`, `win_streak[2][0]`
- **Random variables:** the standard range for this family and
  Difficulty (see Rules § Standard operand ranges)
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `addition_grid[op1][op2] >= cell_target(op1, op2)`
  for every cell the lesson can ask **AND** `win_streak[2][0] >= 4`

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
- **Random variables:** the standard range for this family and
  Difficulty (see Rules § Standard operand ranges)
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `addition_grid[op1][op2] >= cell_target(op1, op2)`
  for every cell the lesson can ask **AND** `win_streak[2][1] >= 4`

### Horizontal Addition — Level 1
- **Game UID:** 2
- **Subject:** Addition
- **Difficulty:** 1
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** All Addition Difficulty 0 passed and Number Line Addition 1 passed.
- **Screen:** Horizontal Equation Screen (addition operator)
- **Variables:** `addition_grid`, `win_streak[2][1]`
- **Random variables:** the standard range for this family and
  Difficulty (see Rules § Standard operand ranges)
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `addition_grid[op1][op2] >= cell_target(op1, op2)`
  for every cell the lesson can ask **AND** `win_streak[2][1] >= 4`

### Vertical Addition — Level 1
- **Game UID:** 2
- **Subject:** Addition
- **Difficulty:** 1
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** All Addition Difficulty 0 passed and Number Line Addition 1 passed.
- **Screen:** Vertical Equation Screen (addition operator)
- **Variables:** `addition_grid`, `win_streak[2][1]`
- **Random variables:** the standard range for this family and
  Difficulty (see Rules § Standard operand ranges)
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `addition_grid[op1][op2] >= cell_target(op1, op2)`
  for every cell the lesson can ask **AND** `win_streak[2][1] >= 4`

### Number Line Addition — Level 1
- **Game UID:** 2
- **Subject:** Addition
- **Difficulty:** 1
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** Number Line Addition 0 passed.
- **Screen:** Number Line Equation Screen (addition operator)
- **Variables:** `addition_grid`, `win_streak[2][1]`
- **Random variables:** the standard range for this family and
  Difficulty (see Rules § Standard operand ranges)
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `addition_grid[op1][op2] >= cell_target(op1, op2)`
  for every cell the lesson can ask **AND** `win_streak[2][1] >= 4`

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

### Counting Subtraction — Level 1
- **Game UID:** 7
- **Subject:** Subtraction
- **Difficulty:** 1
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** All Subtraction Difficulty 0 passed.
- **Screen:** Counting Equation Screen (subtraction operator)
- **Variables:** `subtraction_grid`, `win_streak[7][1]`
- **Random variables:** the standard range for this family and
  Difficulty (see Rules § Standard operand ranges)
- **Answer surface:** Numeric Grid (0..9) — the largest difference is 8,
  padded to a full row
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `subtraction_grid[op1][op2] >= cell_target(op1, op2)`
  for every cell the lesson can ask **AND** `win_streak[7][1] >= 4`

The number taken away and the answer both come from the same `0..8`
range as Addition Level 1, so the number they come off runs to 16 — the
exact inverse of that lesson's sums. Only the counting presentation
exists at this level; the Horizontal, Vertical and Number Line
subtraction screens stay at Level 0.

### Counting Multiplication — Level 0
- **Game UID:** 9
- **Subject:** Multiplication
- **Difficulty:** 0
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** All Subtraction Difficulty 0 passed.
- **Screen:** Counting Multiplication Screen
- **Variables:** `multiplication_grid`
- **Random variables:** the standard range for this family and
  Difficulty (see Rules § Standard operand ranges)
  - A random animal emoji per problem (independent of streak)
- **Answer surface:** Numeric Grid (0..16)
- **Problem selection:** standard math-grid selection over the
  `(op1, op2)` cell space.
- **Pass criteria:**
  `multiplication_grid[op1][op2] >= cell_target(op1, op2)` for every cell the lesson can ask.

### Multiplication Operands — Level 0
Filling in the operands is its own exercise rather than a harder level of
Counting Multiplication: the picture is the same, but the question runs
backwards — the learner reads the groups and says which two numbers made
them. Each operation will get an Operands lesson at each level as they
are written; multiplication is the first.

- **Game UID:** 9
- **Subject:** Multiplication Operands
- **Difficulty:** 0
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** Counting Multiplication — Level 0 passed.
- **Screen:** Counting Multiplication Screen (same boxed animal groups),
  but the operands are hidden and the answer surface is the Operand
  Picker (below) instead of the numeric grid. The product is never shown.
- **Variables:** `multiplication_operands_grid` — a separate coverage
  grid from Counting Multiplication, cells indexed `(op1, op2) ∈ 1..4`.
- **Random variables:** the standard range for this family and
  Difficulty (see Rules § Standard operand ranges) — the operand
  picker means neither operand is 0
  - A random animal emoji per problem
- **Answer surface:** Operand Picker — buttons `1..4`; the first tap fills
  the first operand blank, the second tap fills the second and submits.
  The answer is **order-independent** (a × b ≡ b × a). A **Clear** button
  undoes the first pick before submitting.
- **Problem selection:** standard math-grid selection over the
  `(op1, op2) ∈ 1..4` cell space.
- **Pass criteria:**
  `multiplication_operands_grid[op1][op2] >= cell_target(op1, op2)` for
  every cell the lesson can ask.

### Horizontal Multiplication — Level 0
- **Game UID:** 9
- **Subject:** Multiplication
- **Difficulty:** 0
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** Number Line Multiplication — Level 0 passed.
- **Screen:** Horizontal Equation Screen (`×` operator)
- **Variables:** `multiplication_equation_grid`, `win_streak[9][Horizontal]`
- **Random variables:** the standard range for this family and
  Difficulty (see Rules § Standard operand ranges)
- **Answer surface:** Numeric Grid (0..16)
- **Problem selection:** standard math-grid selection
- **Pass criteria:**
  `multiplication_equation_grid[op1][op2] >= cell_target(op1, op2)` for
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
- **Random variables:** the standard range for this family and
  Difficulty (see Rules § Standard operand ranges)
- **Answer surface:** Numeric Grid (0..16)
- **Problem selection:** standard math-grid selection
- **Pass criteria:**
  `multiplication_equation_grid[op1][op2] >= cell_target(op1, op2)` for
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
- **Random variables:** the standard range for this family and
  Difficulty (see Rules § Standard operand ranges)
- **Answer surface:** Numeric Grid (0..16)
- **Problem selection:** standard math-grid selection
- **Pass criteria:**
  `multiplication_equation_grid[op1][op2] >= cell_target(op1, op2)` for
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
  Screen (`×`). The Number Line screen scrolls; it runs 0 to 50 — the
  largest product this tier can ask, 40, plus 10, rounded to the next
  ten (see the product ceiling below).
- **Variables:** `multiplication_equation_grid` (the same grid as Level 0,
  now covering the `0..8` slice), plus each lesson's own `win_streak`.
- **Random variables:** the standard range for this family and Difficulty
  (see Rules § Standard operand ranges) — `X, Y ∈ 0..8` with the product
  capped at **40**. All three presentations share the cap: the number
  line has to draw every integer up to the answer, and a line to 90 is an
  unreadable smear of ticks on a phone, so the ceiling that keeps that
  screen countable is the one the whole tier uses.
- **Answer surface:** **Number Pad** — the learner types the product and
  presses **Enter** (products up to 40 are too many for a tap grid).
- **Problem selection:** standard math-grid selection over the cells the
  ceiling allows.
- **Pass criteria:**
  `multiplication_equation_grid[op1][op2] >= cell_target(op1, op2)` for
  every cell the lesson can ask **AND** `win_streak >= 4` (per lesson) —
  never over pairs the product ceiling rules out.

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
- **Random variables:** the standard range for this family and
  Difficulty (see Rules § Standard operand ranges)
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `subtraction_grid[op1][op2] >= cell_target(op1, op2)`
  for every cell the lesson can ask **AND** `win_streak[7][0] >= 4`

### Horizontal Subtraction — Level 0
- **Game UID:** 7
- **Subject:** Subtraction
- **Difficulty:** 0
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** All Addition Difficulty 1 passed.
- **Screen:** Horizontal Equation Screen (subtraction operator)
- **Variables:** `subtraction_grid`, `win_streak[7][0]`
- **Random variables:** the standard range for this family and
  Difficulty (see Rules § Standard operand ranges)
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `subtraction_grid[op1][op2] >= cell_target(op1, op2)`
  for every cell the lesson can ask **AND** `win_streak[7][0] >= 4`

### Vertical Subtraction — Level 0
- **Game UID:** 7
- **Subject:** Subtraction
- **Difficulty:** 0
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** All Addition Difficulty 1 passed.
- **Screen:** Vertical Equation Screen (subtraction operator)
- **Variables:** `subtraction_grid`, `win_streak[7][0]`
- **Random variables:** the standard range for this family and
  Difficulty (see Rules § Standard operand ranges)
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `subtraction_grid[op1][op2] >= cell_target(op1, op2)`
  for every cell the lesson can ask **AND** `win_streak[7][0] >= 4`

### Number Line Subtraction — Level 0
- **Game UID:** 7
- **Subject:** Subtraction
- **Difficulty:** 0
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** All Addition Difficulty 1 passed.
- **Screen:** Number Line Equation Screen (subtraction operator)
- **Variables:** `subtraction_grid`, `win_streak[7][0]`
- **Random variables:** the standard range for this family and
  Difficulty (see Rules § Standard operand ranges)
- **Problem selection:** standard math-grid selection
- **Pass criteria:** `subtraction_grid[op1][op2] >= cell_target(op1, op2)`
  for every cell the lesson can ask **AND** `win_streak[7][0] >= 4`

### Counting Division — Level 0
- **Game UID:** 11
- **Subject:** Division
- **Difficulty:** 0
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** the end of the multiplication chain — all three
  symbolic Multiplication Level 1 lessons **and** Multiplication
  Operands — Level 0.
- **Screen:** Animal Division Screen, with `Y` pens
- **Variables:** `division_grid[0]`, `win_streak[11][0]`
- **Random variables:** the standard range for this family and
  Difficulty (see Rules § Standard operand ranges) — divisor `X ∈ 1..4`,
  quotient `Y ∈ 1..4`, dividend `Z = X × Y ≤ 16`. The dividend is always
  a whole number of groups, so the answer is always an integer.
- **Problem selection:** standard math-grid selection, restricted to the
  cells above (the rest of `division_grid` is never asked), **balanced on
  the divisor** (see Rules § Balanced operands) so dividing by one does
  not crowd out the rest
- **Pass criteria:**
  `division_grid[0][dividend][divisor] >= cell_target(dividend, divisor)`
  for every askable cell **AND** `win_streak[11][0] >= 4`

### Counting Division — Level 1
- **Game UID:** 11
- **Subject:** Division
- **Difficulty:** 1
- **Category:** Math
- **Runs per round:** 4
- **Unlock conditions:** Counting Division 0 passed.
- **Screen:** Animal Division Screen, with eight pens always
- **Variables:** `division_grid[1]` (its own slice — Level 0 progress
  does not count towards it), `win_streak[11][1]`
- **Random variables:** the Difficulty 1 range — divisor `X ∈ 1..8`,
  quotient `Y ∈ 1..8`, dividend `Z = X × Y ≤ 40`
- **Problem selection:** standard math-grid selection, restricted to the
  askable cells, balanced on the divisor as at Level 0
- **Pass criteria:**
  `division_grid[1][dividend][divisor] >= cell_target(dividend, divisor)`
  for every askable cell **AND** `win_streak[11][1] >= 4`

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
  the clip to finish in full (plus a short buffer) before advancing, so
  it is never cut off, and never less than a floor of 2 / 2.4 / 2.2
  seconds (correct / wrong / reveal) — longer than the usual holds
  because the shortest clips would otherwise flash past.
- **Pass criteria (either one):**
  - `win_streak[3][run] >= 8` (eight correct answers in a row), OR
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
  - `position` — any letter position within the word
- **Problem selection:** per-word list selection over the set of
  `(word, position)` pairs, so the position is driven by which pairs
  still need coverage rather than drawn uniformly.
- **Pass criteria:** `win_streak[5][word][p] >= 2` for every word
  and every letter position `p` of every word.

### Rhyming Words — Level 0 (Pick the rhyme)
- **Game UID:** 6
- **Subject:** Rhyming Words
- **Difficulty:** 0
- **Category:** Reading
- **Runs per round:** 2
- **Unlock conditions:** Sight Words — Level 1 passed.
- **Screen:** Rhyme Choice Screen.
- **Word bank:** `app/src/main/assets/config.yaml` under
  `rhyming_words.groups` (each list is a rhyme family).
- **Task:** a **target word** is spoken and shown; the learner taps the one
  of three word choices that **rhymes** with it (the other two are
  distractors drawn from other families).
- **Variables:** `win_streak[6]` — one slot per **target word**.
- **Problem selection:** prefer target words still below streak 2 (10%
  fully random); avoid repeating the previous target.
- **Pass criteria:** `win_streak[6][word] >= 2` for every word (i.e. every
  word has been correctly rhymed twice as the target).

### Rhyming Words — Level 1 (Odd one out)
- **Game UID:** 6
- **Subject:** Rhyming Words
- **Difficulty:** 1
- **Category:** Reading
- **Runs per round:** 2
- **Unlock conditions:** Rhyming Words — Level 0 passed.
- **Screen:** Rhyme Choice Screen (all four words spoken in sequence).
- **Task:** four words are shown — **three from one rhyme family plus one
  from another**; the learner taps the word that does **not** rhyme.
- **Variables:** `win_streak[6]` — one slot per **rhyme family**
  (`fam<index>`).
- **Problem selection:** prefer families still below streak 2; avoid
  repeating the previous family.
- **Pass criteria:** `win_streak` for every family `>= 2`.

### Position Words — Levels 0 / 1 / 2
- **Game UID:** 10
- **Subject:** Position Words
- **Category:** Reading
- **Runs per round:** 2
- **Unlock conditions:** Level 0 after Rhyming Words 1; Level 1 after
  Position Words 0; Level 2 after Position Words 1.
- **Screen:** Position Scene Screen.
- **Concept:** spatial prepositions — **on / in / over / under / by**. A
  scene shows a three-letter **animal** placed relative to a three-letter
  **object** (emoji), with the sentence
  `The <animal> is <prep> the <object>.` and exactly one slot blank. The
  learner taps the missing word from a few choices.
  - **Level 0** blanks the **animal** (choices = animals).
  - **Level 1** blanks the **preposition** (choices = on / in / over /
    under / by).
  - **Level 2** blanks the **object** (choices = objects).
- **Depiction:** `on` = animal resting directly on the object (touching);
  `under` = object above the animal; `over` = animal floating higher above
  the object (no arrow); `in` = for deep containers the object is drawn in
  front of the animal so it peeks out the top, for a flat pan the animal
  sits inside it; `by` = animal beside the object. Each object declares
  which prepositions read sensibly with it — `in` is limited to containers
  (box, bus, cup, jar, pot, net, tub, pan, bin); `by` works with anything.
  Objects: box, bus, hat, log, bed, cup, jar, pot, bag, net, tub, pan,
  can, bin.
- **Variables:** `win_streak[10]` — one slot per blanked item
  (`PositionWords0.<animal>`, `PositionWords1.<prep>`,
  `PositionWords2.<object>`).
- **Pass criteria:** every item of that level answered correctly at least
  twice (`>= 2`).

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

### Universal win-streak pass

Every lesson passes when **either** its own listed pass criteria are
met **or** its `run[lesson]` slot reaches **8** (eight correct answers
in a row). The run resets to zero on any wrong answer or **Give up**,
so the fast path requires a clean streak. This lets a learner who
already knows the material clear a lesson quickly instead of grinding
every cell, word, or letter to its per-item target.

The run is the same mechanism Tic Tac Toe always used; it is now shared
by every category. Where a lesson's own criteria previously combined a
run with per-item mastery using **AND** (Letter Sounds), the two are
now combined with **OR** — either alone passes the lesson. Games, which
already passed at a non-loss streak of 8, are unchanged.

### Manual unlock

The Progress screen exposes two switches beside every lesson:
**Unlocked** and **Passed**.

**Unlocked** flips `lesson_manual_unlock[lesson]`. On, it **manually
unlocks** the lesson — it appears in the menu and the Random / Mixed
rotation right away, even if its prerequisites haven't been passed yet.
Off relocks it (it remains available if its parents have been passed
naturally). This does **not** mark the lesson passed — the learner
still has to play and pass it the normal way for downstream gates to
open. Switches on entry-level lessons (no prerequisites) are disabled —
those lessons are always available regardless.

**Passed** flips `lesson_manual_override[lesson]` and sets
`lesson_passed[lesson]` to match. On, it marks the lesson complete by
hand — use it to skip a lesson the learner already knows — which
unlocks whatever it gates, exactly as a normally-earned pass would.
Off clears both flags so the lesson can be earned normally again. It is
enabled for every lesson.

Together these let a parent jump a learner ahead to try a more advanced
lesson before mastering everything below it, or skip a lesson outright.

## Random / Mixed mode

When the **Random / Mixed** menu option is active, after every
completed round the app draws the next activity randomly from the pool
of **all currently unlocked lessons**.

**Revision share:** one draw in ten goes to a lesson the learner has
**already passed**; the other nine go to lessons **still unpassed**.
Within each of the two groups the pick is uniform. When one group is
empty — nothing passed yet, or everything passed — every draw comes
from the other.

**Category change:** the whole category just played is excluded from the
next draw, so a run of Math is followed by Reading or a Game rather than
more Math. The filter is dropped if it would leave the pool empty (e.g.
only one category is unlocked).

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
