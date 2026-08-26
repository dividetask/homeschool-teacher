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
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
```

ReportLab is the only dependency. The fonts are vendored (see
`fonts/SOURCE.md`), so nothing is downloaded when you generate a sheet.

## Use

```
.venv/bin/python worksheets.py --list                     # what's available
.venv/bin/python worksheets.py addition-horizontal        # every level of one sheet
.venv/bin/python worksheets.py division-counting --level 0
.venv/bin/python worksheets.py --all --out ~/worksheets   # the whole set
```

PDFs land in `./out` unless `--out` says otherwise, named
`<sheet>-level<N>.pdf`. Pages are US Letter.

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
lesson yet; division has only the counting presentation for the same
reason.

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
