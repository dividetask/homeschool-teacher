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
dependency. `worksheets.py` looks for that `.venv` and re-runs itself
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

PDFs land in `./out` — relative to **where you ran the command**, not to
this directory — unless `--out` says otherwise. They're named
`<sheet>-level<N>.pdf`, and pages are US Letter. Every run prints the
absolute directory it wrote to, so there's no guessing; note that `out/`
is gitignored, so `git status` won't show them.

`--seed N` fixes the shuffle, so a sheet can be regenerated exactly —
useful for printing a second copy of one a child already started.

There are no answer keys: every sheet is problems only.

## The sheets

| Sheet                       | Levels | Mirrors                            |
| --------------------------- | ------ | ---------------------------------- |
| `addition-counting`         | 0, 1   | Counting Addition                  |
| `addition-horizontal`       | 0, 1   | Horizontal Addition                |
| `addition-vertical`         | 0, 1   | Vertical Addition                  |
| `addition-numberline`       | 0, 1   | Number Line Addition               |
| `subtraction-counting`      | 0      | Counting Subtraction               |
| `subtraction-horizontal`    | 0      | Horizontal Subtraction             |
| `subtraction-vertical`      | 0      | Vertical Subtraction               |
| `subtraction-numberline`    | 0      | Number Line Subtraction            |
| `multiplication-counting`   | 0, 1   | Counting Multiplication            |
| `multiplication-horizontal` | 0, 1   | Horizontal Multiplication          |
| `multiplication-vertical`   | 0, 1   | Vertical Multiplication            |
| `multiplication-numberline` | 0, 1   | Number Line Multiplication         |
| `division-counting`         | 0      | Counting Division                  |
| `binary`                    | 0, 1   | Binary                             |

Subtraction has no Level 1 because the app has no Level 1 subtraction
lesson yet, and division has only the counting presentation for the same
reason. Division has no Level 1 sheet either: the app's two division
levels differ only in how many pens the *screen* puts out, and the pens
aren't part of the printed sheet — so both levels would print the same
page.

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
- **Number lines** are all the same length on a given sheet, sized to
  that sheet's largest possible answer. The app widens its line per
  problem, but a line that changes length every few rows is hard to read
  across on paper.

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
