#!/usr/bin/env python3
"""Print-ready PDF worksheets for the Homeschool Teacher lessons.

Each sheet mirrors one lesson from ``docs/lessons.md`` — same operands,
same presentation — and is filled with as many problems as the page
holds. Every run reshuffles, so the same command twice gives two
different worksheets.

One run writes one PDF: every sheet asked for becomes a page of it, in
curriculum order (easiest first), so a batch prints as a workbook to
work front to back.

    ./setup.sh                                  # once, to install ReportLab
    ./worksheets.py addition-horizontal --level 1
    ./worksheets.py division-counting binary
    ./worksheets.py --all --out ~/worksheets

Run with no arguments to list what's available.

Only the drawing path needs ReportLab, and it is imported lazily so that
--list and the argument errors still work on a bare interpreter.
"""

import argparse
import itertools
import os
import random
import sys
import zlib
from typing import Iterator, List, Optional, Sequence, Tuple

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
            yield blocks.HorizontalBlock(problem, index, height_budget=budget)
        elif sheet.style == "vertical":
            yield blocks.VerticalBlock(problem, index, height_budget=budget)
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
            yield blocks.MultCountingBlock(problem, index, height_budget=budget)
        elif sheet.style == "counting-blanks":
            yield blocks.CountingBlanksBlock(
                problem, index,
                base_size=float(sheet.params.get("animal_size", 20.0)),
                max_rows=int(sheet.params.get("max_rows", 2)),
                height_budget=budget,
            )
        elif sheet.style == "grouped-blanks":
            yield blocks.GroupedBlanksBlock(
                problem, index,
                operator=str(sheet.params.get("operator", "x")),
                height_budget=budget,
            )
        elif sheet.style == "division-counting":
            yield blocks.DivisionCountingBlock(problem, index, height_budget=budget)
        elif sheet.style == "binary":
            yield blocks.BinaryBlock(problem, index, height_budget=budget)
        else:
            raise ValueError(f"unknown style {sheet.style!r}")


# Styles that draw animals. On these a problem is a picture with its
# equation above or below it, so without a divider the eye runs one
# problem's picture together with the next one's equation. The symbolic
# sheets are one line each and need no help.
PICTURE_STYLES = frozenset({
    "counting", "counting-blanks", "mult-counting",
    "grouped-blanks", "division-counting",
})


def _cells(sheet: catalog.Sheet):
    """Every (left, right) the sheet can ask, for sizing its number line."""
    return problems.arithmetic_cells(sheet.params)


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
    span = max(10, ((needed + 1 + 4) // 5) * 5)
    # Never draw past the sheet's own answer ceiling: a Multiplication
    # Level 1 line running to 45 offers five numbers no problem can reach.
    ceiling = sheet.params.get("answer_max")
    if ceiling is not None:
        span = min(span, int(ceiling))
    return span


# One run makes one document: every worksheet asked for lands in this
# file, in curriculum order, so a batch prints as a workbook rather than
# thirty-one files to collate by hand.
DEFAULT_NAME = "worksheets.pdf"


def resolve_out(out: str) -> str:
    """Turn ``--out`` into the absolute path of the PDF to write.

    A path ending in ``.pdf`` names the file itself; anything else is a
    directory to drop ``worksheets.pdf`` into, which is what keeps
    ``--out ~/worksheets`` meaning what it always did.
    """
    path = os.path.abspath(out)
    if os.path.splitext(path)[1].lower() == ".pdf":
        return path
    return os.path.join(path, DEFAULT_NAME)


def _sheet_seed(seed: Optional[int], sheet: catalog.Sheet) -> Optional[int]:
    """Vary a run's seed per sheet, so a batch isn't the same shuffle twice.

    crc32 of the slug rather than hash(): string hashing is salted per
    process, so hash() gave a different worksheet every run from the same
    --seed — which is the one thing --seed exists to prevent.
    """
    if seed is None:
        return None
    return seed + zlib.crc32(sheet.slug.encode("utf-8")) % 100000


def draw_sheet(c, sheet: catalog.Sheet, seed: Optional[int] = None) -> int:
    """Draw one worksheet as the next page of ``c``.

    Returns the number of problems on it. The canvas is left ready for
    the next page.
    """
    import blocks
    import render

    rng = random.Random(seed)

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
        c,
        itertools.islice(_blocks_for(sheet, rng, budget), catalog.MAX_PROBLEMS),
        area,
        sheet.columns,
        max_rows=int(rows) if rows else None,
        row_rules=sheet.style in PICTURE_STYLES,
    )
    render.draw_footer(c, sheet)
    c.showPage()

    if count < catalog.MIN_PROBLEMS:
        raise SystemExit(
            f"worksheets: {sheet.slug} came out with {count} problems, under the "
            f"{catalog.MIN_PROBLEMS} minimum.\n"
            "Its blocks have outgrown the shape declared in catalog.py — widen "
            "the shape or shrink the block rather than shipping a thin page."
        )
    return count


def build(sheets: Sequence[catalog.Sheet], path: str,
          seed: Optional[int] = None) -> List[Tuple[catalog.Sheet, int]]:
    """Write one PDF holding every sheet given, easiest first.

    Returns (sheet, problem count) pairs in the order the pages come out,
    which is the order the caller should report them in.
    """
    _require_reportlab()

    import render
    from reportlab.pdfgen.canvas import Canvas

    render.register_fonts()
    ordered = catalog.in_difficulty_order(sheets)

    c = Canvas(path, pagesize=render.PAGE_SIZE)
    c.setAuthor("Homeschool Teacher")
    if len(ordered) == 1:
        only = ordered[0]
        c.setTitle(f"{only.title} — Level {only.level}")
        c.setSubject(only.lesson)
    else:
        c.setTitle("Homeschool Teacher Worksheets")
        c.setSubject(f"{len(ordered)} worksheets, easiest first")

    built: List[Tuple[catalog.Sheet, int]] = []
    for sheet in ordered:
        # Bookmark before drawing: the outline entry points at whichever
        # page the canvas is on, and draw_sheet is what ends it. Thirty
        # pages need a way to jump to one.
        c.bookmarkPage(sheet.slug)
        c.addOutlineEntry(f"{sheet.title} — Level {sheet.level}", sheet.slug)
        built.append((sheet, draw_sheet(c, sheet, _sheet_seed(seed, sheet))))
    if len(ordered) > 1:
        c.showOutline()
    c.save()
    return built


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


def _check(seeds: int = 8) -> int:
    """Build every sheet a few times over and report the problem counts.

    A sheet's count should be identical whatever the shuffle: the shape is
    declared, not discovered. A range here means some operand combination
    is pushing a block past its budget.
    """
    import tempfile

    counts = {}
    with tempfile.TemporaryDirectory() as tmp:
        for seed in range(seeds):
            path = os.path.join(tmp, f"check-{seed}.pdf")
            for sheet, n in build(catalog.ALL, path, seed):
                counts.setdefault(sheet.slug, set()).add(n)

    width = max(len(k) for k in counts)
    unstable = 0
    print(f"{seeds} shuffles of each sheet "
          f"(floor {catalog.MIN_PROBLEMS}, cap {catalog.MAX_PROBLEMS})\n")
    for slug in sorted(counts, key=lambda k: (min(counts[k]), k)):
        seen = sorted(counts[slug])
        note = ""
        if len(seen) > 1:
            note = "   varies with the shuffle"
            unstable += 1
        print(f"  {slug.ljust(width)}  {', '.join(str(n) for n in seen)}{note}")
    if unstable:
        print(f"\n{unstable} sheet(s) vary between shuffles.")
        return 1
    print("\nEvery sheet is stable and within bounds.")
    return 0


def _list_sheets() -> None:
    print("Available worksheets (pass --level to pick one level):\n")
    width = max(len(k) for k in catalog.keys())
    for key in catalog.keys():
        levels = ", ".join(str(lv) for lv in catalog.levels(key))
        sample = catalog.get(key, catalog.levels(key)[0])
        print(f"  {key.ljust(width)}  levels {levels}   {sample.lesson.split('—')[0].strip()}")
    print("\n  --all builds every worksheet at every level.")
    print("  Whatever you pick lands in one PDF, easiest first.")


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
                        help="where to write the PDF: a directory to put "
                             f"{DEFAULT_NAME} in, or a path ending in .pdf to "
                             "name the file. Relative to where you're standing "
                             "unless absolute (default: ./out)")
    parser.add_argument("--seed", type=int, default=None,
                        help="fix the shuffle so a sheet can be reproduced exactly")
    parser.add_argument("--list", action="store_true", help="list the available worksheets")
    parser.add_argument("--check", action="store_true",
                        help="build every sheet several times and report problem counts")
    args = parser.parse_args(argv)

    if args.check:
        _require_reportlab()
        return _check()

    if args.list or (not args.names and not args.all):
        _list_sheets()
        return 0

    sheets = list(catalog.ALL) if args.all else _resolve(args.names, args.level)
    if args.all and args.level is not None:
        sheets = [s for s in sheets if s.level == args.level]

    # Resolve before printing: "out/worksheets.pdf" doesn't tell you which
    # "out", and the answer depends on where you ran this from.
    path = resolve_out(args.out)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    print(f"Writing {path}\n")

    built = build(sheets, path, args.seed)
    width = max(len(s.slug) for s, _ in built)
    for page, (sheet, count) in enumerate(built, 1):
        print(f"  {page:>3}.  {sheet.slug.ljust(width)}  {count} problems")

    if len(built) == 1:
        print(f"\n1 page written to {path}")
    else:
        print(f"\n{len(built)} pages, easiest first, written to {path}")
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
