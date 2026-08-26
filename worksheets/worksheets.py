#!/usr/bin/env python3
"""Print-ready PDF worksheets for the Homeschool Teacher lessons.

Each sheet mirrors one lesson from ``docs/lessons.md`` — same operands,
same presentation — and is filled with as many problems as the page
holds. Every run reshuffles, so the same command twice gives two
different worksheets.

    ./setup.sh                                  # once, to install ReportLab
    ./worksheets.py addition-horizontal --level 1
    ./worksheets.py division-counting binary
    ./worksheets.py --all --out ~/worksheets

Run with no arguments to list what's available.

Only the drawing path needs ReportLab, and it is imported lazily so that
--list and the argument errors still work on a bare interpreter.
"""

import argparse
import os
import random
import sys
from typing import Iterator, List, Optional, Sequence

import catalog
import problems

VENV_PYTHON = os.path.join(
    os.path.dirname(os.path.abspath(__file__)), ".venv", "bin", "python",
)
_REEXEC_GUARD = "HST_WORKSHEETS_REEXEC"


def _reexec_into_venv() -> None:
    """Re-run under ``.venv`` when ReportLab is missing from this Python.

    So ``./worksheets.py`` works straight from a shell after setup.sh,
    without the caller having to remember the venv's interpreter. The
    guard variable stops this recursing if the venv is broken too.
    """
    if os.environ.get(_REEXEC_GUARD):
        return
    try:
        import reportlab  # noqa: F401
        return
    except ImportError:
        pass
    if not os.path.exists(VENV_PYTHON):
        return
    # Compare the paths as written, not resolved: a venv's ``python`` is a
    # symlink to the base interpreter, so realpath() makes every venv look
    # like the one already running and this would never fire.
    if os.path.abspath(VENV_PYTHON) == os.path.abspath(sys.executable):
        return
    os.environ[_REEXEC_GUARD] = "1"
    os.execv(VENV_PYTHON, [VENV_PYTHON, os.path.abspath(__file__)] + sys.argv[1:])


def _require_reportlab() -> None:
    """Fail with instructions rather than a bare ImportError traceback."""
    try:
        import reportlab  # noqa: F401
    except ImportError:
        raise SystemExit(
            "worksheets: ReportLab is not installed, so no PDF can be drawn.\n"
            "\n"
            "    ./setup.sh\n"
            "\n"
            "creates a .venv beside this script and installs it; after that\n"
            "./worksheets.py picks the .venv up on its own. To use your own\n"
            "interpreter instead, install ReportLab into it and run that:\n"
            "\n"
            "    python3 -m pip install reportlab\n"
        )


def _blocks_for(sheet: catalog.Sheet, rng: random.Random,
                budget: Optional[float] = None) -> Iterator:
    """Turn the sheet's problem stream into a stream of drawable blocks."""
    import blocks

    stream = problems.generate(sheet, rng)
    span = _number_line_span(sheet)
    index = 0
    while True:
        index += 1
        problem = next(stream)
        if sheet.style == "horizontal":
            yield blocks.HorizontalBlock(problem, index)
        elif sheet.style == "vertical":
            yield blocks.VerticalBlock(problem, index)
        elif sheet.style == "counting":
            yield blocks.CountingBlock(
                problem, index,
                base_size=float(sheet.params.get("animal_size", 20.0)),
                max_rows=int(sheet.params.get("max_rows", 2)),
                height_budget=budget,
            )
        elif sheet.style == "numberline":
            lowest = _line_origin(sheet, problem)
            yield blocks.NumberLineBlock(
                problem, index, lowest, lowest + span, height_budget=budget,
            )
        elif sheet.style == "mult-counting":
            yield blocks.MultCountingBlock(problem, index)
        elif sheet.style == "mult-operands":
            yield blocks.MultOperandsBlock(problem, index)
        elif sheet.style == "division-counting":
            yield blocks.DivisionCountingBlock(problem, index)
        elif sheet.style == "binary":
            yield blocks.BinaryBlock(problem, index)
        else:
            raise ValueError(f"unknown style {sheet.style!r}")


def _cells(sheet: catalog.Sheet):
    """Every (left, right) the sheet can ask, for sizing its number line."""
    lo_l, hi_l = sheet.params["left"]
    lo_r, hi_r = sheet.params["right"]
    return [(a, b) for a in range(lo_l, hi_l + 1) for b in range(lo_r, hi_r + 1)]


def _answer(sheet: catalog.Sheet, left: int, right: int) -> int:
    operator = sheet.params["operator"]
    if operator == "+":
        return left + right
    if operator == "-":
        return left - right
    return left * right


def _line_origin(sheet: catalog.Sheet, problem) -> int:
    """Where this problem's number line starts.

    "zero" is the usual thing. "min-operand" starts the line at the
    smaller of the two numbers, which is what lets a Level 1 line stay
    short enough to sit two-up on the page.
    """
    if sheet.params.get("line_origin") == "min-operand":
        return min(problem.left, problem.right)
    return 0


def _number_line_span(sheet: catalog.Sheet) -> int:
    """How many steps every number line on this sheet covers.

    Measured across the sheet's whole problem space so one span serves
    every problem — all the lines then share a scale, which a line
    resized per problem would not. Rounded up to a multiple of five so
    the labelled ticks land on round numbers.
    """
    if sheet.style != "numberline":
        return 0
    origin_is_min = sheet.params.get("line_origin") == "min-operand"
    needed = 0
    for left, right in _cells(sheet):
        origin = min(left, right) if origin_is_min else 0
        # The line has to reach both the answer and the starting operand:
        # a subtraction hops backwards from the left operand.
        furthest = max(_answer(sheet, left, right), left, right)
        needed = max(needed, furthest - origin)
    return max(10, ((needed + 1 + 4) // 5) * 5)
def build(sheet: catalog.Sheet, path: str, seed: Optional[int] = None) -> int:
    """Write one worksheet PDF. Returns the number of problems on it."""
    _require_reportlab()

    import blocks
    import render
    from reportlab.pdfgen.canvas import Canvas

    render.register_fonts()
    rng = random.Random(seed)

    c = Canvas(path, pagesize=render.PAGE_SIZE)
    c.setTitle(f"{sheet.title} — Level {sheet.level}")
    c.setAuthor("Homeschool Teacher")
    c.setSubject(sheet.lesson)

    top = render.draw_header(c, sheet)
    if sheet.header:
        area_width = render.PAGE_WIDTH - 2 * render.MARGIN
        if sheet.header == "binary-cheatsheet":
            used = blocks.draw_cheat_sheet(c, render.MARGIN, top, area_width)
        elif sheet.header == "numberline":
            highest = max(
                _answer(sheet, left, right) for left, right in _cells(sheet)
            )
            used = blocks.draw_reference_line(
                c, render.MARGIN, top, area_width, 0, highest,
            )
        else:
            raise ValueError(f"unknown header {sheet.header!r}")
        top -= used + 14

    area = render.content_area(top)
    # A sheet that asks for a fixed number of rows gets a per-row height
    # budget, and the blocks that can size themselves shrink into it. That
    # makes "two columns of twenty" a property of the layout rather than
    # something that happens to come out right for one operand range.
    rows = sheet.params.get("rows")
    budget = None
    if rows:
        budget = (area[3] - render.ROW_GAP * (int(rows) - 1)) / float(rows)
    count = render.fill_page(
        c, _blocks_for(sheet, rng, budget), area, sheet.columns,
        max_rows=int(rows) if rows else None,
    )
    render.draw_footer(c, sheet)
    c.showPage()
    c.save()
    return count


def _resolve(names: Sequence[str], level: Optional[int]) -> List[catalog.Sheet]:
    """Expand the command line into the list of sheets to build."""
    chosen: List[catalog.Sheet] = []
    for name in names:
        if name not in catalog.keys():
            raise SystemExit(
                f"worksheets: unknown worksheet '{name}'\n"
                f"try one of: {', '.join(catalog.keys())}"
            )
        if level is None:
            chosen.extend(catalog.get(name, lv) for lv in catalog.levels(name))
        else:
            try:
                chosen.append(catalog.get(name, level))
            except KeyError as exc:
                raise SystemExit(f"worksheets: {exc.args[0]}")
    return chosen


def _list_sheets() -> None:
    print("Available worksheets (pass --level to pick one level):\n")
    width = max(len(k) for k in catalog.keys())
    for key in catalog.keys():
        levels = ", ".join(str(lv) for lv in catalog.levels(key))
        sample = catalog.get(key, catalog.levels(key)[0])
        print(f"  {key.ljust(width)}  levels {levels}   {sample.lesson.split('—')[0].strip()}")
    print("\n  --all builds every worksheet at every level.")


def main(argv: Optional[Sequence[str]] = None) -> int:
    _reexec_into_venv()
    parser = argparse.ArgumentParser(
        prog="worksheets.py",
        description="Generate printable PDF worksheets for the Homeschool Teacher lessons.",
    )
    parser.add_argument("names", nargs="*", help="worksheet keys to build")
    parser.add_argument("--all", action="store_true", help="build every worksheet at every level")
    parser.add_argument("--level", type=int, default=None,
                        help="build only this level (default: every level the sheet has)")
    parser.add_argument("--out", default="out",
                        help="directory to write PDFs into, relative to where "
                             "you're standing unless absolute (default: ./out)")
    parser.add_argument("--seed", type=int, default=None,
                        help="fix the shuffle so a sheet can be reproduced exactly")
    parser.add_argument("--list", action="store_true", help="list the available worksheets")
    args = parser.parse_args(argv)

    if args.list or (not args.names and not args.all):
        _list_sheets()
        return 0

    sheets = list(catalog.ALL) if args.all else _resolve(args.names, args.level)
    if args.all and args.level is not None:
        sheets = [s for s in sheets if s.level == args.level]

    # Resolve before printing: "out/foo.pdf" doesn't tell you which "out",
    # and the answer depends on where you ran this from.
    out_dir = os.path.abspath(args.out)
    os.makedirs(out_dir, exist_ok=True)
    print(f"Writing to {out_dir}\n")

    width = max(len(f"{s.slug}.pdf") for s in sheets)
    for sheet in sheets:
        name = f"{sheet.slug}.pdf"
        # Vary the seed per sheet so --seed still gives every sheet in a
        # batch its own problem set rather than the same shuffle.
        seed = None if args.seed is None else args.seed + hash(sheet.slug) % 100000
        count = build(sheet, os.path.join(out_dir, name), seed)
        print(f"  {name.ljust(width)}  {count} problems")

    sheet_word = "worksheet" if len(sheets) == 1 else "worksheets"
    print(f"\n{len(sheets)} {sheet_word} written to {out_dir}")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except BrokenPipeError:
        # `worksheets.py --list | head` closes the pipe early. Point the
        # rest of stdout at /dev/null so the interpreter doesn't report a
        # second failure while flushing on the way out.
        os.dup2(os.open(os.devnull, os.O_WRONLY), sys.stdout.fileno())
        sys.exit(0)
