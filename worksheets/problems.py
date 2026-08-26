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


def _cycle_shuffled(
    cells: Sequence,
    rng: random.Random,
    easy: Optional[Callable[[object], bool]] = None,
) -> Iterator:
    """Yield every cell in random order, reshuffling for each new pass.

    A pass never repeats a problem, and the join between passes avoids
    handing out the same cell twice in a row.

    Easy cells sit out half the passes, so they come up about half as
    often as ordinary ones — the printed equivalent of the half weight
    they carry in the app (docs/lessons.md § Easy cells). Without this a
    page of Addition Level 1 comes out a third "+ 0".
    """
    previous = None
    while True:
        order = [
            cell for cell in cells
            if easy is None or not easy(cell) or rng.random() < 0.5
        ]
        if not order:
            order = list(cells)
        rng.shuffle(order)
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

    Usually the two ranges crossed, but a sheet may cap the answer —
    Number Line Multiplication Level 1 drops anything reaching 30 so the
    line stays countable, matching the lesson. Both the generator and the
    number-line sizing ask here, so a sheet is never scaled for a problem
    it cannot show.
    """
    lo_l, hi_l = params["left"]
    lo_r, hi_r = params["right"]
    cells = [(a, b) for a in range(lo_l, hi_l + 1) for b in range(lo_r, hi_r + 1)]
    ceiling = params.get("answer_max")
    if ceiling is None:
        return cells
    operator = params["operator"]
    return [(a, b) for a, b in cells if _apply(operator, a, b) <= ceiling]


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
    lo, hi = params["divisor"]
    return [
        (divisor * quotient, divisor)
        for divisor in range(lo, hi + 1)
        for quotient in range(1, max_dividend // divisor + 1)
    ]


def _division_pass(params, rng: random.Random) -> List[Tuple[int, int]]:
    """One page's worth of division problems, balanced across divisors.

    This is docs/lessons.md § Balanced operands, in the shuffled-pass form
    the sheets use: where the app divides a cell's pick weight by the
    number of cells sharing its divisor, a pass gives every divisor the
    same number of slots, which comes to the same thing. The easy-cell
    rule still halves the ÷1 slots on top of that, so dividing by one
    lands at about a twelfth of the page rather than a quarter.
    """
    by_divisor: Dict[int, List[Tuple[int, int]]] = {}
    for dividend, divisor in _division_cells(params):
        by_divisor.setdefault(divisor, []).append((dividend, divisor))
    slots = min(len(v) for v in by_divisor.values())
    chosen: List[Tuple[int, int]] = []
    for divisor, cells in by_divisor.items():
        take = slots
        if is_easy("/", cells[0][0], divisor) and rng.random() < 0.5:
            continue
        chosen.extend(rng.sample(cells, min(take, len(cells))))
    rng.shuffle(chosen)
    return chosen


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

    if sheet.style == "division-counting":
        previous = None
        while True:
            for dividend, divisor in _division_pass(sheet.params, rng):
                if (dividend, divisor) == previous:
                    continue
                previous = (dividend, divisor)
                yield Problem(
                    left=dividend,
                    right=divisor,
                    operator="/",
                    animal=rng.choice(animals.ALL),
                )

    if sheet.style in ("mult-counting", "mult-operands"):
        cells = _arithmetic_cells(sheet.params)
        easy = lambda cell: is_easy("x", cell[0], cell[1])
        for a, b in _cycle_shuffled(cells, rng, easy):
            yield Problem(left=a, right=b, operator="x", animal=rng.choice(animals.ALL))
        return

    operator = sheet.params["operator"]
    picture = sheet.style == "counting"
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
