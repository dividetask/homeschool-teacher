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

Every sheet carries exactly **12 problems** — one sitting's work. Sheets
declare a `columns` × `rows` shape in `catalog.py` multiplying to twelve,
and the blocks grow into the resulting height budget, so the page fills
rather than sitting in the top half in small type. A sheet that comes out
short means a block outgrew its shape, and fails the build rather than
shipping thin.

```
./worksheets.py --check
```

builds every sheet several times over and reports the counts. A sheet
whose count varies between shuffles has a block outgrowing its shape.

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

| Sheet                            | Levels | Mirrors                       |
| -------------------------------- | ------ | ----------------------------- |
| `addition-counting`              | 0, 1   | Counting Addition             |
| `addition-construction`          | 0, 1   | Counting Addition             |
| `addition-horizontal`            | 0, 1   | Horizontal Addition           |
| `addition-vertical`              | 0, 1   | Vertical Addition             |
| `addition-numberline`            | 0, 1   | Number Line Addition          |
| `subtraction-counting`           | 0, 1   | Counting Subtraction          |
| `subtraction-construction`       | 0, 1   | Counting Subtraction          |
| `subtraction-horizontal`         | 0      | Horizontal Subtraction        |
| `subtraction-vertical`           | 0      | Vertical Subtraction          |
| `subtraction-numberline`         | 0      | Number Line Subtraction       |
| `multiplication-counting`        | 0      | Counting Multiplication       |
| `multiplication-construction`    | 0      | Counting Multiplication       |
| `multiplication-horizontal`      | 0, 1   | Horizontal Multiplication     |
| `multiplication-vertical`        | 0, 1   | Vertical Multiplication       |
| `multiplication-numberline`      | 0, 1   | Number Line Multiplication    |
| `division-counting`              | 0, 1   | Counting Division             |
| `division-construction`          | 0, 1   | Counting Division             |
| `binary`                         | 0, 1   | Binary                        |

The `-construction` sheets are the build-the-whole-equation variant of
each counting sheet: instead of being handed `3 + 2 = ▢`, the child reads
both numbers off the picture and writes `▢ + ▢ = ▢`. Multiplication and
division share one picture there — so many pens holding so many each —
because they *are* the same picture: multiplication reads it as `pens ×
per pen = total`, division reads it as `total ÷ pens = per pen`.

`multiplication-construction` also covers what the Multiplication
Operands lesson drills — reading the two numbers off the pens — and asks
for the product besides, so there is no separate operands sheet; it would
print the same page. It drops zero from the range the way division drops
a zero divisor: `0 × 3` draws no groups at all, and there is no equation
to read off nothing.

**Multiplication is always `op1` groups of `op2`**, matching the app and
`../docs/lessons.md` § Counting Multiplication Screen — `4 × 3` draws
four pens of three, not three of four. Division follows from the same
reading: the divisor is how many pens.

Subtraction has no Level 1 in its symbolic presentations, and division
has no symbolic presentations at all, because that's what the app
teaches.

Operand ranges are not written out sheet by sheet. Every Math lesson
outside Binary draws from one of four standard ranges chosen by family
and level (`../docs/lessons.md` § Standard operand ranges), and
`catalog.py` builds its params from that same table, so a sheet cannot
drift from its lesson by hand-editing a bound:

| Family                    | Level 0                    | Level 1                    |
| ------------------------- | -------------------------- | -------------------------- |
| Addition / Subtraction    | `op1, op2 ∈ 0..4`          | `op1, op2 ∈ 0..8`          |
| Multiplication / Division | `X, Y ∈ 0..4`, `Z ∈ 0..16` | `X, Y ∈ 0..8`, `Z ∈ 0..40` |

Read each pair of families as one triple from either side: the forward
operation takes `X` and `Y` from the range and lets `Z` fall out, the
backward one takes `X` and the *answer* from the range and derives the
`Z` it starts from. So subtraction's first operand runs to twice the
ceiling and its answer can never be negative, and division's dividend is
always a whole number of groups. `Z` is a ceiling, so pairs like `7 × 8`
are simply never asked.

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
- **Multiplication Level 1** caps the product at 40 across all its
  presentations, so the number line stays countable and the reference
  line at the top of the symbolic sheets stays readable.
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
