"""Random problem sets for a worksheet.

Every run produces a different sheet. Rather than drawing each problem
independently — which clusters and repeats — a generator walks a shuffled
pass over the lesson's whole problem space, so a page covers as much of
the space as it has room for before any problem comes round twice.
"""

import random
from dataclasses import dataclass
from typing import Callable, Dict, Iterator, List, Optional, Sequence, Tuple

import animals
import catalog

# Rendered form of each operator. The minus is U+2212, not a hyphen, to
# match the app and to keep the sign visible at print size.
OPERATOR_GLYPH = {"+": "+", "-": "−", "x": "×", "/": "÷"}


@dataclass(frozen=True)
class Problem:
    left: int
    right: int
    operator: str                        # a key of OPERATOR_GLYPH
    animal: Optional[animals.Animal] = None
    # Per-group "arrange on two lines" flags, decided once per problem so
    # a group's shape is stable — see the Counting Equation Screen in
    # docs/lessons.md.
    split_left: bool = False
    split_right: bool = False

    @property
    def answer(self) -> int:
        if self.operator == "+":
            return self.left + self.right
        if self.operator == "-":
            return self.left - self.right
        if self.operator == "x":
            return self.left * self.right
        if self.operator == "/":
            return self.left // self.right
        raise ValueError(f"unknown operator {self.operator!r}")

    @property
    def glyph(self) -> str:
        return OPERATOR_GLYPH[self.operator]


@dataclass(frozen=True)
class BinaryProblem:
    left: int
    right: int
    operator: str                        # "AND" | "OR" | "XOR"
    bits: int

    @property
    def answer(self) -> int:
        if self.operator == "AND":
            return self.left & self.right
        if self.operator == "OR":
            return self.left | self.right
        return self.left ^ self.right

    def binary(self, value: int) -> str:
        return format(value, "b").zfill(self.bits)


BINARY_OPERATORS = ("AND", "OR", "XOR")


def is_easy(operator: str, a: int, b: int) -> bool:
    """Whether ``a op b`` is a cell a learner gets right on sight.

    Mirrors ``PracticeGrid.isEasy`` in
    ``shared/.../practice/PracticeGrid.kt`` and the Easy cells rule in
    docs/lessons.md: a zero operand in addition or subtraction, a zero or
    one operand in multiplication, dividing by one.
    """
    if operator in ("+", "-"):
        return a == 0 or b == 0
    if operator == "x":
        return a <= 1 or b <= 1
    if operator == "/":
        return b <= 1 or a == 0
    return False


# Most of a page the easy cells may take between them. Mirrors
# PracticeGrid.EASY_SHARE_CAP; see docs/lessons.md § Easy cells.
EASY_SHARE_CAP = 1.0 / 6.0


def _easy_slots(ordinary: int, easy: int) -> int:
    """How many easy cells belong in one pass.

    Half of them, per the easy-cell rule — but never more than the cap
    allows against the ordinary cells beside them. Returning a *count*
    rather than rolling per cell is what keeps a page predictable: rolling
    each easy cell independently gave pages that were half empty of them
    and pages that were full, with the average right and no page like it.
    """
    if easy == 0 or ordinary == 0:
        return 0
    allowed = round(ordinary * EASY_SHARE_CAP / (1.0 - EASY_SHARE_CAP))
    return max(1, min(easy // 2 or 1, allowed))


def _build_pass(
    cells: Sequence,
    rng: random.Random,
    easy: Optional[Callable[[object], bool]] = None,
    balance: Optional[Callable[[object], int]] = None,
) -> List:
    """One pass over the problem space, honouring both damping rules.

    Ordinary cells all appear. Where the lesson names a balance operand
    (docs/lessons.md § Balanced operands) every value of it gets the same
    number of slots, so a value that owns more cells does not crowd out
    one that owns fewer. Easy cells then fill [_easy_slots] places.
    """
    ordinary = [c for c in cells if easy is None or not easy(c)]
    easy_cells = [c for c in cells if easy is not None and easy(c)]

    if balance is not None and ordinary:
        by_key: Dict[int, List] = {}
        for cell in ordinary:
            by_key.setdefault(balance(cell), []).append(cell)
        slots = min(len(v) for v in by_key.values())
        ordinary = [c for group in by_key.values() for c in rng.sample(group, slots)]

    chosen = list(ordinary)
    slots = _easy_slots(len(ordinary), len(easy_cells))
    if slots:
        chosen.extend(rng.sample(easy_cells, slots))
    rng.shuffle(chosen)
    return chosen


def _cycle_shuffled(
    cells: Sequence,
    rng: random.Random,
    easy: Optional[Callable[[object], bool]] = None,
    balance: Optional[Callable[[object], int]] = None,
) -> Iterator:
    """Yield passes back to back, never repeating across the join."""
    previous = None
    while True:
        order = _build_pass(cells, rng, easy, balance)
        if not order:
            order = list(cells)
        if previous is not None and len(order) > 1 and order[0] == previous:
            order[0], order[1] = order[1], order[0]
        for cell in order:
            yield cell
            previous = cell


def _split_flags(count: int, rng: random.Random) -> bool:
    """Whether a group of ``count`` animals is drawn on two lines.

    Always for groups above four (a long row is hard to count), and
    sometimes for 2–4 so the child sees the count doesn't depend on the
    arrangement. Mirrors ``splitGroup`` in MathScreen.kt.
    """
    if count > 4:
        return True
    if count >= 2:
        return rng.randrange(10) < 3
    return False


def arithmetic_cells(params) -> List[Tuple[int, int]]:
    """Every (left, right) the sheet may ask.

    Usually the two ranges crossed, but subtraction derives its first
    operand from the answer (see below) and a sheet may cap the answer —
    Multiplication Level 1 drops anything past 40 so the number line
    stays countable, matching the lesson. Both the generator and the
    number-line sizing ask here, so a sheet is never scaled for a problem
    it cannot show.
    """
    lo_l, hi_l = params["left"]
    lo_r, hi_r = params["right"]
    operator = params["operator"]
    if params.get("derived") == "subtraction":
        # Subtraction is built from the answer and the number taken away,
        # both from the family range; the number they come off is their
        # sum. Same construction as division, so an answer is always in
        # range and never negative.
        return [
            (right + answer, right)
            for right in range(lo_r, hi_r + 1)
            for answer in range(lo_r, hi_r + 1)
        ]
    cells = [(a, b) for a in range(lo_l, hi_l + 1) for b in range(lo_r, hi_r + 1)]
    ceiling = params.get("answer_max")
    if ceiling is not None:
        cells = [(a, b) for a, b in cells if _apply(operator, a, b) <= ceiling]
    return cells


def _apply(operator: str, a: int, b: int) -> int:
    if operator == "+":
        return a + b
    if operator == "-":
        return a - b
    if operator == "x":
        return a * b
    return a // b


_arithmetic_cells = arithmetic_cells


def _division_cells(params) -> List[Tuple[int, int]]:
    """(dividend, divisor) pairs that divide exactly.

    The dividend runs 1..``dividend_max`` and the divisor 1..6, but only
    the pairs where the division comes out whole are ever asked — so the
    coverage grid the Android lesson keeps has gaps, by design.
    """
    max_dividend = params["dividend_max"]
    lo_d, hi_d = params["divisor"]
    lo_q, hi_q = params["quotient"]
    return [
        (divisor * quotient, divisor)
        for divisor in range(lo_d, hi_d + 1)
        for quotient in range(lo_q, hi_q + 1)
        if divisor * quotient <= max_dividend
    ]


def _binary_cells(bits: int) -> List[Tuple[str, int, int]]:
    top = (1 << bits) - 1
    return [
        (op, a, b)
        for op in BINARY_OPERATORS
        for a in range(top + 1)
        for b in range(top + 1)
    ]


def generate(sheet: catalog.Sheet, rng: random.Random) -> Iterator:
    """An endless stream of problems for ``sheet``.

    The page filler pulls from this until the page is full, so the caller
    never has to guess how many problems fit.
    """
    if sheet.style == "binary":
        cells = _binary_cells(sheet.params["bits"])
        for op, a, b in _cycle_shuffled(cells, rng):
            yield BinaryProblem(left=a, right=b, operator=op, bits=sheet.params["bits"])
        return

    if sheet.style == "division-counting" or (
        sheet.style == "grouped-blanks" and sheet.params.get("operator") == "/"
    ):
        # Division is balanced on the divisor: every dividend from 1 up
        # divides by one, so it owns far more cells than the rest.
        for dividend, divisor in _cycle_shuffled(
            _division_cells(sheet.params), rng,
            easy=lambda cell: is_easy("/", cell[0], cell[1]),
            balance=lambda cell: cell[1],
        ):
            yield Problem(
                left=dividend,
                right=divisor,
                operator="/",
                animal=rng.choice(animals.ALL),
            )
        return

    if sheet.style in ("mult-counting", "grouped-blanks"):
        cells = _arithmetic_cells(sheet.params)
        easy = lambda cell: is_easy("x", cell[0], cell[1])
        for a, b in _cycle_shuffled(cells, rng, easy):
            yield Problem(left=a, right=b, operator="x", animal=rng.choice(animals.ALL))
        return

    operator = sheet.params["operator"]
    picture = sheet.style in ("counting", "counting-blanks")
    cells = _arithmetic_cells(sheet.params)
    easy = lambda cell: is_easy(operator, cell[0], cell[1])
    for a, b in _cycle_shuffled(cells, rng, easy):
        yield Problem(
            left=a,
            right=b,
            operator=operator,
            animal=rng.choice(animals.ALL) if picture else None,
            split_left=_split_flags(a, rng) if picture else False,
            split_right=_split_flags(b, rng) if picture else False,
        )
