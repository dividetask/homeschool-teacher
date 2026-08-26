"""Which worksheets exist, and the operand ranges each one draws from.

``../docs/lessons.md`` is the source of truth. Every entry mirrors a
lesson defined there — same operands, same presentation — so a printed
sheet drills exactly what the on-screen lesson drills. When a range
changes there, change it here; where the doc and the Kotlin disagree,
follow the doc.

Alongside the ranges each sheet carries its own layout knobs — how many
problems sit side by side, how big the animals start out, whether the
page opens with a reference aid. They live here rather than in
``blocks.py`` because they're per-sheet editorial choices, not drawing
logic.
"""

from dataclasses import dataclass, field
from typing import Dict, Optional, Tuple


@dataclass(frozen=True)
class Sheet:
    """One printable worksheet.

    ``style`` selects the block renderer in ``blocks.py``; ``columns`` is
    how many problems sit side by side; ``header`` names an aid drawn
    once at the top of the page; ``params`` carries the operand ranges
    and any per-style layout knobs.
    """

    key: str
    level: int
    title: str
    lesson: str          # the matching lesson in docs/lessons.md
    instructions: str
    style: str
    columns: int
    params: Dict[str, object] = field(default_factory=dict)
    header: Optional[str] = None    # "numberline" | "binary-cheatsheet"

    @property
    def slug(self) -> str:
        return f"{self.key}-level{self.level}"


def _pair(left: Tuple[int, int], right: Tuple[int, int]) -> Dict[str, object]:
    return {"left": left, "right": right}


# Operand ranges are inclusive (low, high), taken from the "Random
# variables" line of each lesson in docs/lessons.md.
#
# Two of these deliberately disagree with app/.../math/MathViewModel.kt,
# which has drifted from the doc: the doc puts every Addition Level 1
# variant at 0..8 (the Kotlin uses 0..9) and Counting Addition Level 0 at
# 0..4 (the Kotlin starts at 1 to avoid drawing an empty group). The doc
# wins; a zero group prints as an empty pen.
_SHEETS = (
    # --- Addition -------------------------------------------------------
    Sheet("addition-counting", 0, "Counting Addition", "Counting Addition — Level 0",
          "Count the animals in each group, then write how many there are altogether.",
          "counting", 2,
          dict(_pair((0, 4), (0, 4)), operator="+", animal_size=20.0, max_rows=2, rows=10)),
    Sheet("addition-counting", 1, "Counting Addition", "Counting Addition — Level 1",
          "Count the animals in each group, then write how many there are altogether.",
          "counting", 2,
          dict(_pair((0, 8), (0, 8)), operator="+", animal_size=20.0, max_rows=3, rows=10)),
    Sheet("addition-horizontal", 0, "Addition", "Horizontal Addition — Level 0",
          "Write the answer in the box.",
          "horizontal", 3, dict(_pair((0, 4), (0, 4)), operator="+")),
    Sheet("addition-horizontal", 1, "Addition", "Horizontal Addition — Level 1",
          "Use the number line at the top to help. Write the answer in the box.",
          "horizontal", 3, dict(_pair((0, 8), (0, 8)), operator="+"),
          header="numberline"),
    Sheet("addition-vertical", 0, "Addition", "Vertical Addition — Level 0",
          "Add the two numbers and write the answer under the line.",
          "vertical", 4, dict(_pair((0, 4), (0, 4)), operator="+")),
    Sheet("addition-vertical", 1, "Addition", "Vertical Addition — Level 1",
          "Use the number line at the top to help. Write each answer under the line.",
          "vertical", 4, dict(_pair((0, 8), (0, 8)), operator="+"),
          header="numberline"),
    Sheet("addition-numberline", 0, "Number Line Addition", "Number Line Addition — Level 0",
          "Start at the first number and hop forward. Write where you land.",
          "numberline", 2,
          dict(_pair((0, 4), (0, 4)), operator="+", line_origin="zero", rows=10)),
    Sheet("addition-numberline", 1, "Number Line Addition", "Number Line Addition — Level 1",
          "Each line starts at the smaller number. Hop forward and write where you land.",
          "numberline", 2,
          dict(_pair((0, 8), (0, 8)), operator="+", line_origin="min-operand", rows=10)),

    # --- Subtraction ----------------------------------------------------
    # Only the counting presentation has a Level 1; the symbolic
    # subtraction screens stay at Level 0, as in the app.
    Sheet("subtraction-counting", 0, "Counting Subtraction", "Counting Subtraction — Level 0",
          "Count the first group, take away the second, and write how many are left.",
          "counting", 2,
          dict(_pair((4, 9), (0, 4)), operator="-", animal_size=20.0, max_rows=3, rows=10)),
    Sheet("subtraction-counting", 1, "Counting Subtraction", "Counting Subtraction — Level 1",
          "Count the first group, take away the second, and write how many are left.",
          "counting", 2,
          dict(_pair((8, 16), (0, 8)), operator="-", animal_size=20.0, max_rows=4, rows=10)),
    Sheet("subtraction-horizontal", 0, "Subtraction", "Horizontal Subtraction — Level 0",
          "Write the answer in the box.",
          "horizontal", 3, dict(_pair((4, 9), (0, 4)), operator="-")),
    Sheet("subtraction-vertical", 0, "Subtraction", "Vertical Subtraction — Level 0",
          "Subtract and write the answer under the line.",
          "vertical", 4, dict(_pair((4, 9), (0, 4)), operator="-")),
    Sheet("subtraction-numberline", 0, "Number Line Subtraction", "Number Line Subtraction — Level 0",
          "Start at the first number and hop backwards. Write where you land.",
          "numberline", 2,
          dict(_pair((4, 9), (0, 4)), operator="-", line_origin="zero", rows=10)),

    # --- Multiplication -------------------------------------------------
    Sheet("multiplication-counting", 0, "Counting Multiplication", "Counting Multiplication — Level 0",
          "Count the groups and how many are in each, then write the total.",
          "mult-counting", 2, _pair((0, 4), (0, 4))),
    Sheet("multiplication-counting", 1, "Counting Multiplication", "Counting Multiplication — Level 1",
          "Write the two numbers being multiplied: how many in each group × how many groups.",
          "mult-operands", 2, _pair((1, 4), (1, 4))),
    Sheet("multiplication-horizontal", 0, "Multiplication", "Horizontal Multiplication — Level 0",
          "Write the answer in the box.",
          "horizontal", 3, dict(_pair((0, 4), (0, 4)), operator="x")),
    Sheet("multiplication-horizontal", 1, "Multiplication", "Horizontal Multiplication — Level 1",
          "Write the answer in the box.",
          "horizontal", 3, dict(_pair((0, 9), (0, 9)), operator="x")),
    Sheet("multiplication-vertical", 0, "Multiplication", "Vertical Multiplication — Level 0",
          "Multiply and write the answer under the line.",
          "vertical", 4, dict(_pair((0, 4), (0, 4)), operator="x")),
    Sheet("multiplication-vertical", 1, "Multiplication", "Vertical Multiplication — Level 1",
          "Multiply and write the answer under the line.",
          "vertical", 4, dict(_pair((0, 9), (0, 9)), operator="x")),
    Sheet("multiplication-numberline", 0, "Number Line Multiplication", "Number Line Multiplication — Level 0",
          "Count equal hops along the number line to find each answer.",
          "numberline", 1,
          dict(_pair((0, 4), (0, 4)), operator="x", line_origin="zero")),
    Sheet("multiplication-numberline", 1, "Number Line Multiplication", "Number Line Multiplication — Level 1",
          "Count equal hops along the number line to find each answer.",
          "numberline", 1,
          dict(_pair((0, 9), (0, 9)), operator="x", line_origin="zero")),

    # --- Division -------------------------------------------------------
    # Dividend 1..24, divisor 1..6, and the dividend is always a multiple
    # of the divisor so every answer is a whole number.
    Sheet("division-counting", 0, "Counting Division", "Counting Division — Level 0",
          "Share the animals into equal groups. Write how many end up in each group.",
          "division-counting", 2,
          {"dividend_max": 24, "divisor": (1, 6)}),

    # --- Binary ---------------------------------------------------------
    Sheet("binary", 0, "Binary Operations", "Binary — Level 0",
          "Use the cheat sheet at the top. Write each answer bit in the box.",
          "binary", 4, {"bits": 1}, header="binary-cheatsheet"),
    Sheet("binary", 1, "Binary Operations", "Binary — Level 1",
          "Use the cheat sheet at the top. Work one column at a time, right to left.",
          "binary", 3, {"bits": 3}, header="binary-cheatsheet"),
)

BY_SLUG: Dict[str, Sheet] = {s.slug: s for s in _SHEETS}
ALL = _SHEETS


def levels(key: str):
    """Levels available for a sheet key, lowest first."""
    return sorted(s.level for s in _SHEETS if s.key == key)


def keys():
    """Every sheet key, in catalog order, without duplicates."""
    seen = []
    for s in _SHEETS:
        if s.key not in seen:
            seen.append(s.key)
    return seen


def get(key: str, level: int) -> Sheet:
    slug = f"{key}-level{level}"
    if slug not in BY_SLUG:
        available = ", ".join(str(x) for x in levels(key)) or "none"
        raise KeyError(
            f"no worksheet '{key}' at level {level} (levels for {key}: {available})"
        )
    return BY_SLUG[slug]
