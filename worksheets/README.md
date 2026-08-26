# Worksheets

Printable PDF worksheets for the Homeschool Teacher lessons. This is a
plain command-line tool that runs on Linux — it has nothing to do with
the Android app and needs no Android SDK. The two just share a
curriculum: every worksheet mirrors one lesson from `../docs/lessons.md`,
using the same operands and the same presentation, so a printed sheet
drills exactly what the on-screen lesson drills.

Each sheet is filled with as many problems as the page holds, and every
run reshuffles — the same command twice gives two different worksheets.

## Setup

```
./setup.sh
```

That creates `.venv` beside the script and installs ReportLab, the only
dependency. It needs Python 3.8 or newer; if your `python3` is older than
3.9 it will use a newer `python3.x` from your PATH when one exists, and
say which. Re-running it rebuilds the venv from scratch, so it's also the
fix if the venv ends up in a bad state. `worksheets.py` looks for that `.venv` and re-runs itself
under it when the interpreter you invoked doesn't have ReportLab — so
after setup you can just call `./worksheets.py` and forget the venv
exists. (If you'd rather use your own interpreter, `pip install
reportlab` into it and run `python worksheets.py`; the `.venv` lookup
only kicks in when ReportLab is missing.)

The fonts are vendored (see `fonts/SOURCE.md`), so nothing is downloaded
when you generate a sheet.

## Use

```
./worksheets.py --list                     # what's available
./worksheets.py addition-horizontal        # every level of one sheet
./worksheets.py division-counting --level 0
./worksheets.py --all --out ~/worksheets   # the whole set
```

`--list` needs no dependencies at all — ReportLab is only imported when a
PDF is actually being drawn.

Every sheet carries at most **20 problems** — past that a worksheet stops
being one sitting's work. Sheets declare a `columns` × `rows` shape in
`catalog.py` and the blocks grow into the resulting height budget, so a
capped page still fills rather than sitting in the top half in small type.

PDFs land in `./out` — relative to **where you ran the command**, not to
this directory — unless `--out` says otherwise. They're named
`<sheet>-level<N>.pdf`, and pages are US Letter. Every run prints the
absolute directory it wrote to, so there's no guessing; note that `out/`
is gitignored, so `git status` won't show them.

`--seed N` fixes the shuffle, so a sheet can be regenerated exactly —
useful for printing a second copy of one a child already started.

There are no answer keys: every sheet is problems only.

## If it won't run

**`TypeError: 'usedforsecurity' is an invalid keyword argument`** — you're
on Python 3.8 with ReportLab 4.4.3 or newer. That release calls a hashlib
API that only exists from Python 3.9 on, but still advertises support for
3.7+, so pip installs it and every PDF then dies. `requirements.txt` pins
back to 4.4.2 on old interpreters; re-run `./setup.sh` to pick the pin
up. The sheets come out identical either way.

**`ModuleNotFoundError: No module named 'reportlab'`** — run `./setup.sh`.

## The sheets

| Sheet                       | Levels | Mirrors                            |
| --------------------------- | ------ | ---------------------------------- |
| `addition-counting`         | 0, 1   | Counting Addition                  |
| `addition-horizontal`       | 0, 1   | Horizontal Addition                |
| `addition-vertical`         | 0, 1   | Vertical Addition                  |
| `addition-numberline`       | 0, 1   | Number Line Addition               |
| `subtraction-counting`      | 0, 1   | Counting Subtraction               |
| `subtraction-horizontal`    | 0      | Horizontal Subtraction             |
| `subtraction-vertical`      | 0      | Vertical Subtraction               |
| `subtraction-numberline`    | 0      | Number Line Subtraction            |
| `multiplication-counting`   | 0, 1   | Counting Multiplication            |
| `multiplication-horizontal` | 0, 1   | Horizontal Multiplication          |
| `multiplication-vertical`   | 0, 1   | Vertical Multiplication            |
| `multiplication-numberline` | 0, 1   | Number Line Multiplication         |
| `division-counting`         | 0      | Counting Division                  |
| `binary`                    | 0, 1   | Binary                             |

Subtraction has a Level 1 only in its counting presentation, and division
has only the counting presentation at all — in both cases because that's
what the app teaches. Division has no Level 1 sheet either: the app's two division
levels differ only in how many pens the *screen* puts out, and the pens
aren't part of the printed sheet — so both levels would print the same
page.

Operand ranges come from the "Random variables" line of each lesson in
`../docs/lessons.md`, which is the source of truth. Two of them
deliberately disagree with `MathViewModel.kt`, which has drifted: the doc
puts every Addition Level 1 variant at `0..8` (the Kotlin uses `0..9`)
and Counting Addition Level 0 at `0..4` (the Kotlin starts at 1 to avoid
drawing an empty group). The sheets follow the doc.

**Easy cells** are damped the way the app damps them (docs/lessons.md §
Easy cells): adding or subtracting zero, multiplying by zero or one, and
dividing by one sit out half the passes, so any one of them is half as
likely to appear as an ordinary problem.

What that comes to on the page depends on how much of a sheet's cell
space is easy, which varies a lot. Addition Level 1 lands at ~12%.
Multiplication Level 0 lands at ~48%, because with operands 0..4 only
nine of its twenty-five cells have both operands above one — most of that
lesson's facts genuinely are easy ones. Division needed more than the
halving: every dividend from 1 to 24 divides by one, so `÷ 1` owned 24 of
58 cells and still filled a quarter of the page. Each divisor now gets
equal billing per pass, which brings `÷ 1` to about 9%.

A few sheets are worth calling out:

- **Counting Multiplication Level 1** asks for the two *operands* rather
  than the product — `▢ × ▢` under the pens of animals — because that's
  what the lesson asks for.
- **Division** prints X animals above `X ÷ Y = ▢`, deliberately ungrouped:
  sorting them into equal groups is the work. Divisors run 1–6 and the
  dividend is always a multiple of the divisor, so every answer is a
  whole number.
- **Binary** opens with the cheat sheet — all three single-bit truth
  tables in the same stacked layout the problems use — so a child can
  look up any column without being told which operator to use.
- **Horizontal and Vertical Multiplication** open with a reference number
  line, as the Level 1 addition sheets do.
- **Number Line Multiplication Level 1** drops any pair whose product
  reaches 30, matching the lesson: the other Level 1 presentations type
  the answer, so a large product costs nothing, but a number line has to
  draw every integer up to it.
- **Number lines** all span the same number of steps on a given sheet, so
  they share a scale and can be compared down the page — the app widens
  its line per problem, which on paper just makes the page hard to read
  across. Where that span *starts* is per-sheet: Level 0 lines start at
  zero, Level 1 lines start at the smaller of the two operands, which is
  what keeps them short enough to sit two-up.
- **The symbolic Level 1 sheets** (horizontal and vertical) open with a
  single reference number line instead of one per problem: the operands
  are past counting on fingers, but a line beside all 36 questions would
  bury the page.
- **Sheets that ask for an exact shape** — the four "two columns of
  twenty" ones — declare a `rows` target in `catalog.py`. The page filler
  hands each block a matching height budget and stops at that many rows,
  so the shape holds regardless of what the operand range does to the
  drawing.

## Layout

```
worksheets.py   CLI: resolves sheet names, drives the build
catalog.py      which sheets exist and their operand ranges
problems.py     random problem streams (a shuffled pass over the whole
                problem space, so a page covers as much of it as it can
                before repeating)
render.py       fonts, page furniture, and the ragged-row page filler
blocks.py       one block renderer per presentation style
fonts/          vendored font subsets — see fonts/SOURCE.md
```

To add a worksheet: add a `Sheet` to `catalog.py`, and a block class in
`blocks.py` if it needs a presentation none of the existing ones cover.
`worksheets.py` maps `Sheet.style` to the block class.

When a lesson's operand range changes in `../docs/lessons.md`, change the
matching `Sheet` in `catalog.py` so the two stay in step.
