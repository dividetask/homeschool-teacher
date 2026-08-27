"""Which worksheets exist, and the operand ranges each one draws from.

``../docs/lessons.md`` is the source of truth. Every entry mirrors a
lesson defined there — same operands, same presentation — so a printed
sheet drills exactly what the on-screen lesson drills. When a range
changes there, change it here; where the doc and the Kotlin disagree,
follow the doc.

Ranges are not written out per sheet. Every Math lesson outside Binary
draws from one of four standard ranges chosen by operation family and
difficulty (docs/lessons.md § Standard operand ranges), so the sheets
build their params from that one table too — a sheet states its family
and level, and cannot drift from its lesson by hand-editing a bound.

Alongside the ranges each sheet carries its own layout knobs — how many
problems sit side by side, how big the animals start out, whether the
page opens with a reference aid. They live here rather than in
``blocks.py`` because they're per-sheet editorial choices, not drawing
logic.
"""

from dataclasses import dataclass, field
from typing import Dict, Optional, Tuple

# Every sheet carries exactly this many problems. One number rather than
# a range: a sheet that comes out short means its blocks outgrew the
# shape declared for it, which is a layout bug rather than something to
# ship, so building it fails loudly instead. Run `worksheets.py --check`
# to verify every sheet across several shuffles.
PROBLEMS_PER_SHEET = 12
MAX_PROBLEMS = PROBLEMS_PER_SHEET
MIN_PROBLEMS = PROBLEMS_PER_SHEET


# --- the standard ranges --------------------------------------------------
#
# docs/lessons.md § Standard operand ranges, as data. `operands` is the
# inclusive range both operands come from; `z_max` caps the big number in
# the multiplication families — the product a multiplication asks for and
# the dividend a division starts from.
STANDARD = {
    ("addsub", 0): {"operands": (0, 4)},
    ("addsub", 1): {"operands": (0, 8)},
    ("muldiv", 0): {"operands": (0, 4), "z_max": 16},
    ("muldiv", 1): {"operands": (0, 8), "z_max": 40},
}


def add_params(level: int, operator: str, **extra) -> Dict[str, object]:
    """Addition or subtraction at ``level``.

    Subtraction is built the way division is: the number taken away and
    the answer both come from the family range, and the number they come
    off is their sum, so it runs to twice the ceiling and no answer is
    ever negative (docs/lessons.md - Standard operand ranges).
    """
    span = STANDARD[("addsub", level)]["operands"]
    params: Dict[str, object] = {"left": span, "right": span, "operator": operator}
    if operator == "-":
        params["left"] = (span[0], span[1] * 2)
        params["derived"] = "subtraction"
    params.update(extra)
    return params


def mul_params(level: int, **extra) -> Dict[str, object]:
    """Multiplication at ``level``: operands from the range, product capped."""
    std = STANDARD[("muldiv", level)]
    span = std["operands"]
    params: Dict[str, object] = {
        "left": span, "right": span, "operator": "x", "answer_max": std["z_max"],
    }
    params.update(extra)
    return params


def div_params(level: int, **extra) -> Dict[str, object]:
    """Division at ``level``.

    Divisor and quotient both come from the family range with zero
    dropped — dividing by zero is undefined and dividing zero by
    something is degenerate — and the dividend is their product, capped.
    """
    std = STANDARD[("muldiv", level)]
    low, high = std["operands"]
    span = (max(1, low), high)
    params: Dict[str, object] = {
        "divisor": span, "quotient": span, "dividend_max": std["z_max"],
    }
    params.update(extra)
    return params


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


COUNT_BOTH = ("Count the animals in each group, then build the whole equation — "
              "both numbers and the answer.")

_SHEETS = (
    # --- Addition -------------------------------------------------------
    Sheet("addition-counting", 0, "Counting Addition", "Counting Addition — Level 0",
          "Count the animals in each group, then write how many there are altogether.",
          "counting", 2, add_params(0, "+", animal_size=20.0, max_rows=2, rows=6)),
    Sheet("addition-counting", 1, "Counting Addition", "Counting Addition — Level 1",
          "Count the animals in each group, then write how many there are altogether.",
          "counting", 2, add_params(1, "+", animal_size=20.0, max_rows=3, rows=6)),
    Sheet("addition-construction", 0, "Counting Addition — Construction",
          "Counting Addition — Level 0", COUNT_BOTH,
          "counting-blanks", 2, add_params(0, "+", animal_size=20.0, max_rows=2, rows=6)),
    Sheet("addition-construction", 1, "Counting Addition — Construction",
          "Counting Addition — Level 1", COUNT_BOTH,
          "counting-blanks", 2, add_params(1, "+", animal_size=20.0, max_rows=3, rows=6)),
    Sheet("addition-horizontal", 0, "Addition", "Horizontal Addition — Level 0",
          "Write the answer in the box.",
          "horizontal", 2, add_params(0, "+", rows=6)),
    Sheet("addition-horizontal", 1, "Addition", "Horizontal Addition — Level 1",
          "Use the number line at the top to help. Write the answer in the box.",
          "horizontal", 2, add_params(1, "+", rows=6), header="numberline"),
    Sheet("addition-vertical", 0, "Addition", "Vertical Addition — Level 0",
          "Add the two numbers and write the answer under the line.",
          "vertical", 4, add_params(0, "+", rows=3)),
    Sheet("addition-vertical", 1, "Addition", "Vertical Addition — Level 1",
          "Use the number line at the top to help. Write each answer under the line.",
          "vertical", 4, add_params(1, "+", rows=3), header="numberline"),
    Sheet("addition-numberline", 0, "Number Line Addition", "Number Line Addition — Level 0",
          "Start at the first number and hop forward. Write where you land.",
          "numberline", 2, add_params(0, "+", line_origin="zero", rows=6)),
    Sheet("addition-numberline", 1, "Number Line Addition", "Number Line Addition — Level 1",
          "Each line starts at the smaller number. Hop forward and write where you land.",
          "numberline", 2, add_params(1, "+", line_origin="min-operand", rows=6)),

    # --- Subtraction ----------------------------------------------------
    # Only the counting presentation has a Level 1; the symbolic
    # subtraction screens stay at Level 0, as in the app.
    Sheet("subtraction-counting", 0, "Counting Subtraction", "Counting Subtraction — Level 0",
          "Count the first group, take away the second, and write how many are left.",
          "counting", 2, add_params(0, "-", animal_size=20.0, max_rows=2, rows=6)),
    Sheet("subtraction-counting", 1, "Counting Subtraction", "Counting Subtraction — Level 1",
          "Count the first group, take away the second, and write how many are left.",
          "counting", 2, add_params(1, "-", animal_size=20.0, max_rows=3, rows=6)),
    Sheet("subtraction-construction", 0, "Counting Subtraction — Construction",
          "Counting Subtraction — Level 0", COUNT_BOTH,
          "counting-blanks", 2, add_params(0, "-", animal_size=20.0, max_rows=2, rows=6)),
    Sheet("subtraction-construction", 1, "Counting Subtraction — Construction",
          "Counting Subtraction — Level 1", COUNT_BOTH,
          "counting-blanks", 2, add_params(1, "-", animal_size=20.0, max_rows=3, rows=6)),
    Sheet("subtraction-horizontal", 0, "Subtraction", "Horizontal Subtraction — Level 0",
          "Write the answer in the box.",
          "horizontal", 2, add_params(0, "-", rows=6)),
    Sheet("subtraction-vertical", 0, "Subtraction", "Vertical Subtraction — Level 0",
          "Subtract and write the answer under the line.",
          "vertical", 4, add_params(0, "-", rows=3)),
    Sheet("subtraction-numberline", 0, "Number Line Subtraction", "Number Line Subtraction — Level 0",
          "Start at the first number and hop backwards. Write where you land.",
          "numberline", 2, add_params(0, "-", line_origin="zero", rows=6)),

    # --- Multiplication -------------------------------------------------
    Sheet("multiplication-counting", 0, "Counting Multiplication", "Counting Multiplication — Level 0",
          "Count the groups and how many are in each, then write the total.",
          "mult-counting", 2, mul_params(0, rows=6)),
    # The construction multiplication sheet, mirroring the lesson of the
    # same name: read the two numbers off the pens. It asks for the
    # product besides, which the lesson does not, so one sheet covers both
    # that lesson and the answer half of Counting Multiplication.
    #
    # Zero is dropped from the range here, the way division drops a zero
    # divisor: `0 × 3` draws no animals, and a picture of nothing has no
    # equation to read off it. The answer-first sheet keeps zero, since
    # there the equation is given.
    Sheet("multiplication-construction", 0, "Multiplication Construction",
          "Multiplication Construction — Level 0",
          "Count how many groups and how many are in each, then build the "
          "whole equation.",
          "grouped-blanks", 2, mul_params(0, left=(1, 4), right=(1, 4), rows=6)),
    Sheet("multiplication-horizontal", 0, "Multiplication", "Horizontal Multiplication — Level 0",
          "Write the answer in the box.",
          "horizontal", 2, mul_params(0, rows=6), header="numberline"),
    Sheet("multiplication-horizontal", 1, "Multiplication", "Horizontal Multiplication — Level 1",
          "Write the answer in the box.",
          "horizontal", 2, mul_params(1, rows=6), header="numberline"),
    Sheet("multiplication-vertical", 0, "Multiplication", "Vertical Multiplication — Level 0",
          "Multiply and write the answer under the line.",
          "vertical", 4, mul_params(0, rows=3), header="numberline"),
    Sheet("multiplication-vertical", 1, "Multiplication", "Vertical Multiplication — Level 1",
          "Multiply and write the answer under the line.",
          "vertical", 4, mul_params(1, rows=3), header="numberline"),
    Sheet("multiplication-numberline", 0, "Number Line Multiplication", "Number Line Multiplication — Level 0",
          "Count equal hops along the number line to find each answer.",
          "numberline", 2, mul_params(0, line_origin="zero", rows=6)),
    Sheet("multiplication-numberline", 1, "Number Line Multiplication", "Number Line Multiplication — Level 1",
          "Count equal hops along the number line to find each answer.",
          "numberline", 2, mul_params(1, line_origin="zero", rows=6)),

    # --- Division -------------------------------------------------------
    Sheet("division-counting", 0, "Counting Division", "Counting Division — Level 0",
          "Share the animals into equal groups. Write how many end up in each group.",
          "division-counting", 2, div_params(0, rows=6)),
    Sheet("division-counting", 1, "Counting Division", "Counting Division — Level 1",
          "Share the animals into equal groups. Write how many end up in each group.",
          "division-counting", 2, div_params(1, rows=6)),
    Sheet("division-construction", 0, "Counting Division — Construction",
          "Counting Division — Level 0",
          "The animals are already shared out. Build the whole equation.",
          "grouped-blanks", 2, div_params(0, operator="/", rows=6)),
    Sheet("division-construction", 1, "Counting Division — Construction",
          "Counting Division — Level 1",
          "The animals are already shared out. Build the whole equation.",
          "grouped-blanks", 2, div_params(1, operator="/", rows=6)),

    # --- Binary ---------------------------------------------------------
    Sheet("binary", 0, "Binary Operations", "Binary — Level 0",
          "Use the cheat sheet at the top. Write each answer bit in the box.",
          "binary", 4, {"bits": 1, "rows": 3}, header="binary-cheatsheet"),
    Sheet("binary", 1, "Binary Operations", "Binary — Level 1",
          "Use the cheat sheet at the top. Work one column at a time, right to left.",
          "binary", 3, {"bits": 3, "rows": 4}, header="binary-cheatsheet"),
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
