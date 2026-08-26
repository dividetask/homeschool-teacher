"""Which worksheets exist, and the operand ranges each one draws from.

Every entry mirrors a lesson in ``docs/lessons.md`` — same operands, same
presentation — so a printed sheet drills exactly what the Android lesson
of the same name drills. When a lesson's range changes there, change it
here.
"""

from dataclasses import dataclass, field
from typing import Dict, Tuple


@dataclass(frozen=True)
class Sheet:
    """One printable worksheet.

    ``style`` selects the block renderer in ``blocks.py``; ``columns`` is
    how many problems sit side by side; ``params`` carries the operand
    ranges that style needs.
    """

    key: str
    level: int
    title: str
    lesson: str          # the matching lesson in docs/lessons.md
    instructions: str
    style: str
    columns: int
    params: Dict[str, object] = field(default_factory=dict)

    @property
    def slug(self) -> str:
        return f"{self.key}-level{self.level}"


def _pair(left: Tuple[int, int], right: Tuple[int, int]) -> Dict[str, object]:
    return {"left": left, "right": right}


# Operand ranges are inclusive (low, high), matching lessonLeftRange /
# lessonRightRange in app/.../math/MathViewModel.kt.
_SHEETS = (
    # --- Addition -------------------------------------------------------
    Sheet("addition-counting", 0, "Counting Addition", "Counting Addition — Level 0",
          "Count the animals in each group, then write how many there are altogether.",
          "counting", 2, dict(_pair((1, 4), (1, 4)), operator="+")),
    Sheet("addition-counting", 1, "Counting Addition", "Counting Addition — Level 1",
          "Count the animals in each group, then write how many there are altogether.",
          "counting", 1, dict(_pair((0, 9), (0, 9)), operator="+")),
    Sheet("addition-horizontal", 0, "Addition", "Horizontal Addition — Level 0",
          "Write the answer in the box.",
          "horizontal", 3, dict(_pair((0, 4), (0, 4)), operator="+")),
    Sheet("addition-horizontal", 1, "Addition", "Horizontal Addition — Level 1",
          "Write the answer in the box.",
          "horizontal", 3, dict(_pair((0, 9), (0, 9)), operator="+")),
    Sheet("addition-vertical", 0, "Addition", "Vertical Addition — Level 0",
          "Add the two numbers and write the answer under the line.",
          "vertical", 4, dict(_pair((0, 4), (0, 4)), operator="+")),
    Sheet("addition-vertical", 1, "Addition", "Vertical Addition — Level 1",
          "Add the two numbers and write the answer under the line.",
          "vertical", 4, dict(_pair((0, 9), (0, 9)), operator="+")),
    Sheet("addition-numberline", 0, "Number Line Addition", "Number Line Addition — Level 0",
          "Hop along the number line to find each answer.",
          "numberline", 1, dict(_pair((0, 4), (0, 4)), operator="+")),
    Sheet("addition-numberline", 1, "Number Line Addition", "Number Line Addition — Level 1",
          "Hop along the number line to find each answer.",
          "numberline", 1, dict(_pair((0, 9), (0, 9)), operator="+")),

    # --- Subtraction (Level 0 only — the app has no Level 1 yet) --------
    Sheet("subtraction-counting", 0, "Counting Subtraction", "Counting Subtraction — Level 0",
          "Count the first group, take away the second, and write how many are left.",
          "counting", 1, dict(_pair((4, 9), (0, 4)), operator="-")),
    Sheet("subtraction-horizontal", 0, "Subtraction", "Horizontal Subtraction — Level 0",
          "Write the answer in the box.",
          "horizontal", 3, dict(_pair((4, 9), (0, 4)), operator="-")),
    Sheet("subtraction-vertical", 0, "Subtraction", "Vertical Subtraction — Level 0",
          "Subtract and write the answer under the line.",
          "vertical", 4, dict(_pair((4, 9), (0, 4)), operator="-")),
    Sheet("subtraction-numberline", 0, "Number Line Subtraction", "Number Line Subtraction — Level 0",
          "Hop backwards along the number line to find each answer.",
          "numberline", 1, dict(_pair((4, 9), (0, 4)), operator="-")),

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
          "numberline", 1, dict(_pair((0, 4), (0, 4)), operator="x")),
    Sheet("multiplication-numberline", 1, "Number Line Multiplication", "Number Line Multiplication — Level 1",
          "Count equal hops along the number line to find each answer.",
          "numberline", 1, dict(_pair((0, 9), (0, 9)), operator="x")),

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
          "binary", 4, {"bits": 1}),
    Sheet("binary", 1, "Binary Operations", "Binary — Level 1",
          "Use the cheat sheet at the top. Work one column at a time, right to left.",
          "binary", 3, {"bits": 3}),
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
