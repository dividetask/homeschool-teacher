#!/usr/bin/env python3
"""Print-ready PDF worksheets for the Homeschool Teacher lessons.

Each sheet mirrors one lesson from ``docs/lessons.md`` — same operands,
same presentation — and is filled with as many problems as the page
holds. Every run reshuffles, so the same command twice gives two
different worksheets.

    ./worksheets.py addition-horizontal --level 1
    ./worksheets.py division-counting binary
    ./worksheets.py --all --out ~/worksheets

Run with no arguments to list what's available.
"""

import argparse
import os
import random
import sys
from typing import Iterator, List, Optional, Sequence

import blocks
import catalog
import problems
import render


def _blocks_for(sheet: catalog.Sheet, rng: random.Random) -> Iterator:
    """Turn the sheet's problem stream into a stream of drawable blocks."""
    stream = problems.generate(sheet, rng)
    highest = _number_line_range(sheet)
    index = 0
    while True:
        index += 1
        problem = next(stream)
        if sheet.style == "horizontal":
            yield blocks.HorizontalBlock(problem, index)
        elif sheet.style == "vertical":
            yield blocks.VerticalBlock(problem, index)
        elif sheet.style == "counting":
            yield blocks.CountingBlock(problem, index)
        elif sheet.style == "numberline":
            yield blocks.NumberLineBlock(problem, index, highest)
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


def _number_line_range(sheet: catalog.Sheet) -> int:
    """How far every number line on this sheet runs.

    Sized once, from the sheet's largest possible answer, so all the lines
    on a page share a scale (see NumberLineBlock).
    """
    if sheet.style != "numberline":
        return 0
    lo_l, hi_l = sheet.params["left"]
    lo_r, hi_r = sheet.params["right"]
    operator = sheet.params["operator"]
    if operator == "+":
        biggest = hi_l + hi_r
    elif operator == "-":
        biggest = hi_l - lo_r
    else:
        biggest = hi_l * hi_r
    return blocks.next_multiple_of_ten(biggest + 10)


def build(sheet: catalog.Sheet, path: str, seed: Optional[int] = None) -> int:
    """Write one worksheet PDF. Returns the number of problems on it."""
    render.register_fonts()
    rng = random.Random(seed)

    from reportlab.pdfgen.canvas import Canvas

    c = Canvas(path, pagesize=render.PAGE_SIZE)
    c.setTitle(f"{sheet.title} — Level {sheet.level}")
    c.setAuthor("Homeschool Teacher")
    c.setSubject(sheet.lesson)

    top = render.draw_header(c, sheet)
    if sheet.style == "binary":
        area_width = render.PAGE_WIDTH - 2 * render.MARGIN
        used = blocks.draw_cheat_sheet(c, render.MARGIN, top, area_width)
        top -= used + 18

    area = render.content_area(top)
    count = render.fill_page(c, _blocks_for(sheet, rng), area, sheet.columns)
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
    parser = argparse.ArgumentParser(
        prog="worksheets.py",
        description="Generate printable PDF worksheets for the Homeschool Teacher lessons.",
    )
    parser.add_argument("names", nargs="*", help="worksheet keys to build")
    parser.add_argument("--all", action="store_true", help="build every worksheet at every level")
    parser.add_argument("--level", type=int, default=None,
                        help="build only this level (default: every level the sheet has)")
    parser.add_argument("--out", default="out",
                        help="directory to write PDFs into (default: ./out)")
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

    os.makedirs(args.out, exist_ok=True)
    for sheet in sheets:
        path = os.path.join(args.out, f"{sheet.slug}.pdf")
        # Vary the seed per sheet so --seed still gives every sheet in a
        # batch its own problem set rather than the same shuffle.
        seed = None if args.seed is None else args.seed + hash(sheet.slug) % 100000
        count = build(sheet, path, seed)
        print(f"{path}  ({count} problems)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
