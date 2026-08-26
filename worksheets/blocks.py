"""One block renderer per worksheet style.

Each class measures itself first and draws second, so ``render.fill_page``
can pack ragged-height rows without knowing what a block contains.
"""

import math
from dataclasses import dataclass
from typing import List, Optional, Sequence

from reportlab.lib.colors import black
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfgen.canvas import Canvas

import render
from problems import BINARY_OPERATORS, BinaryProblem, Problem
from render import (
    ANIMALS,
    LABEL_WIDTH,
    LIGHT,
    GREY,
    TEXT,
    TEXT_BOLD,
    animal_advance,
    animal_grid_size,
    answer_box,
    draw_animal_grid,
    empty_pen,
    number_label,
    subscript_two,
)

BOX_WIDTH = 40.0
BOX_HEIGHT = 30.0
GAP = 7.0


def _centre_baseline(top: float, height: float, size: float) -> float:
    """Baseline that puts a line of ``size`` text in the middle of a band."""
    return top - height / 2.0 - size * 0.34


# --- X op Y = [ ] ----------------------------------------------------------

@dataclass
class HorizontalBlock:
    problem: Problem
    index: int
    size: float = 22.0

    def _text(self) -> str:
        p = self.problem
        return f"{p.left} {p.glyph} {p.right} ="

    def height(self, width: float) -> float:
        return max(BOX_HEIGHT, self.size * 1.25) + 6

    def draw(self, c: Canvas, x: float, top: float, width: float) -> None:
        number_label(c, x, top, self.index)
        band = self.height(width)
        text = self._text()
        text_width = pdfmetrics.stringWidth(text, TEXT_BOLD, self.size)
        content = text_width + GAP + BOX_WIDTH
        start = x + LABEL_WIDTH + max(0.0, (width - LABEL_WIDTH - content) / 2.0)

        c.setFont(TEXT_BOLD, self.size)
        c.setFillColor(black)
        c.drawString(start, _centre_baseline(top, band, self.size), text)
        answer_box(
            c,
            start + text_width + GAP,
            top - (band - BOX_HEIGHT) / 2.0,
            BOX_WIDTH,
            BOX_HEIGHT,
        )


# --- the stacked / "column" form ------------------------------------------

@dataclass
class VerticalBlock:
    problem: Problem
    index: int
    size: float = 20.0

    def height(self, width: float) -> float:
        return self.size * 2.62 + BOX_HEIGHT + 7

    def draw(self, c: Canvas, x: float, top: float, width: float) -> None:
        number_label(c, x, top, self.index)
        p = self.problem
        line = self.size * 1.15

        digits = max(len(str(p.left)), len(str(p.right)))
        digit_width = pdfmetrics.stringWidth("0", TEXT_BOLD, self.size)
        operator_width = pdfmetrics.stringWidth(p.glyph, TEXT_BOLD, self.size)
        stack_width = max(operator_width + 4 + digits * digit_width, BOX_WIDTH)
        left = x + LABEL_WIDTH + max(0.0, (width - LABEL_WIDTH - stack_width) / 2.0)
        right = left + stack_width

        c.setFont(TEXT_BOLD, self.size)
        c.setFillColor(black)
        c.drawRightString(right, top - line, str(p.left))
        c.drawRightString(right, top - 2 * line, str(p.right))
        c.drawString(left, top - 2 * line, p.glyph)

        rule_y = top - 2 * line - self.size * 0.32
        c.setStrokeColor(black)
        c.setLineWidth(1.4)
        c.line(left, rule_y, right, rule_y)

        answer_box(c, right - BOX_WIDTH, rule_y - 5, BOX_WIDTH, BOX_HEIGHT)


# --- animals + equation (Counting Addition / Subtraction) ------------------

@dataclass
class CountingBlock:
    """Two groups of animals either side of an operator.

    Each group wraps into as many rows as it needs, and the animal size
    shrinks until the whole equation fits its column — so the same block
    works two-up at Level 0, where a group holds at most four, and
    two-up at Level 1, where it can hold eight.
    """

    problem: Problem
    index: int
    base_size: float = 20.0
    max_rows: int = 2
    pad: float = 6.0
    height_budget: Optional[float] = None
    _layout_cache: Optional[tuple] = None

    MIN_SIZE = 8.0

    def _shape(self, count: int, rows_budget: int, split: bool):
        """(columns, rows) for a group of ``count`` animals.

        ``split`` is the app's habit of putting a small group on two
        lines now and then, so the child sees that the count doesn't
        depend on the arrangement (see the Counting Equation Screen in
        docs/lessons.md). Bigger groups wrap because they have to.
        """
        if count == 0:
            return 1, 1
        rows = min(rows_budget, count)
        if split and count >= 2:
            rows = min(max(rows, 2), count)
        columns = math.ceil(count / rows)
        return columns, math.ceil(count / columns)

    def _content_width(self, size: float, left_cols: int, right_cols: int) -> float:
        p = self.problem
        symbol = min(size, 22.0)
        symbols = (
            pdfmetrics.stringWidth(f" {p.glyph} ", TEXT_BOLD, symbol)
            + pdfmetrics.stringWidth(" = ", TEXT_BOLD, symbol)
        )
        return (left_cols + right_cols) * animal_advance(size) + symbols + BOX_WIDTH

    def _layout(self, width: float):
        """Largest animal size, on the fewest rows, that fits ``width``."""
        if self._layout_cache and self._layout_cache[0] == width:
            return self._layout_cache[1]
        p = self.problem
        available = width - LABEL_WIDTH
        chosen = None
        size = self.base_size
        while chosen is None:
            for budget in range(1, self.max_rows + 1):
                left = self._shape(p.left, budget, p.split_left)
                right = self._shape(p.right, budget, p.split_right)
                if self._content_width(size, left[0], right[0]) > available:
                    continue
                if self.height_budget is not None:
                    rows = max(left[1], right[1])
                    tall = max(rows * render.animal_line_height(size), BOX_HEIGHT)
                    if tall + self.pad > self.height_budget:
                        continue
                chosen = (size, left, right)
                break
            if chosen is None:
                if size <= self.MIN_SIZE:
                    # Nothing fits: take the tightest shape and let it be
                    # a hair wide rather than drawing nothing.
                    chosen = (
                        self.MIN_SIZE,
                        self._shape(p.left, self.max_rows, p.split_left),
                        self._shape(p.right, self.max_rows, p.split_right),
                    )
                else:
                    size = max(self.MIN_SIZE, size - 0.5)
        self._layout_cache = (width, chosen)
        return chosen

    def height(self, width: float) -> float:
        size, left, right = self._layout(width)
        rows = max(left[1], right[1])
        return max(rows * render.animal_line_height(size), BOX_HEIGHT) + self.pad

    def draw(self, c: Canvas, x: float, top: float, width: float) -> None:
        number_label(c, x, top, self.index)
        p = self.problem
        size, (left_cols, left_rows), (right_cols, right_rows) = self._layout(width)
        band = self.height(width)
        symbol_size = min(size, 22.0)
        advance = animal_advance(size)
        line_height = render.animal_line_height(size)

        operator = f" {p.glyph} "
        equals = " = "
        operator_width = pdfmetrics.stringWidth(operator, TEXT_BOLD, symbol_size)
        equals_width = pdfmetrics.stringWidth(equals, TEXT_BOLD, symbol_size)
        content = (
            (left_cols + right_cols) * advance + operator_width + equals_width + BOX_WIDTH
        )
        cursor = x + LABEL_WIDTH + max(0.0, (width - LABEL_WIDTH - content) / 2.0)

        middle = top - band / 2.0
        baseline = middle - symbol_size * 0.34
        c.setFillColor(black)

        def group(count: int, columns: int, rows: int, at: float) -> float:
            if count == 0:
                empty_pen(c, at, middle + line_height / 2.0, advance, line_height)
            else:
                group_top = middle + (rows * line_height) / 2.0
                draw_animal_grid(c, at, group_top, p.animal.emoji, count, size, columns)
            return at + columns * advance

        cursor = group(p.left, left_cols, left_rows, cursor)
        c.setFont(TEXT_BOLD, symbol_size)
        c.drawString(cursor, baseline, operator)
        cursor += operator_width
        cursor = group(p.right, right_cols, right_rows, cursor)
        c.setFont(TEXT_BOLD, symbol_size)
        c.drawString(cursor, baseline, equals)
        cursor += equals_width
        answer_box(c, cursor, middle + BOX_HEIGHT / 2.0, BOX_WIDTH, BOX_HEIGHT)


# --- number line -----------------------------------------------------------

def next_multiple_of_ten(x: int) -> int:
    return ((x + 9) // 10) * 10


AXIS_HEIGHT = 22.0


def draw_axis(c: Canvas, x: float, top: float, width: float,
              lowest: int, highest: int, height: float = AXIS_HEIGHT) -> float:
    """A labelled number line, laid out to fit inside ``height``.

    Labels every tick when there's room, thinning to every 5th or 10th
    when the steps get tight; the minor ticks still carry the counting.
    Tick and label sizes come off ``height`` so a squeezed line stays
    inside its band rather than colliding with whatever is below it.
    """
    steps = max(1, highest - lowest)
    step = width / float(steps)
    if step >= 13:
        label_every = 1
    elif step >= 6:
        label_every = 5
    else:
        label_every = 10

    label_size = min(8.5, max(5.0, step * 0.62), height * 0.4)
    major_tick = min(6.0, height * 0.26)
    axis_y = top - height + label_size + 1 + major_tick

    c.saveState()
    c.setStrokeColor(black)
    c.setLineWidth(1.1)
    c.line(x, axis_y, x + width, axis_y)
    c.setFont(TEXT, label_size)
    for n in range(lowest, highest + 1):
        tick_x = x + (n - lowest) * step
        major = n % label_every == 0
        tick = major_tick if major else major_tick * 0.5
        c.setLineWidth(1.1 if major else 0.7)
        c.line(tick_x, axis_y - tick, tick_x, axis_y + tick)
        if major:
            c.setFillColor(GREY)
            c.drawCentredString(tick_x, axis_y - major_tick - label_size - 1, str(n))
    c.restoreState()
    return height


@dataclass
class NumberLineBlock:
    """A number line with the equation printed beneath it.

    Every line on a sheet spans the same number of steps so they share a
    scale and the eye can compare them, but where that span *starts* is
    per-sheet: from zero, or from the smaller operand, which keeps a
    Level 1 line short enough to sit two-up on the page.

    Given a ``height_budget`` the block shrinks to it, taking the space
    out of the axis and the answer box and leaving the gap between line
    and equation alone — that gap is what makes the two read as separate
    things, so it is the last thing worth spending.
    """

    problem: Problem
    index: int
    lowest: int
    highest: int
    size: float = 17.0
    height_budget: Optional[float] = None

    AXIS_GAP = 12.0
    MIN_AXIS = 15.0
    MIN_BOX = 20.0

    def _metrics(self):
        """(axis height, gap, box height, font size, total height)."""
        natural = AXIS_HEIGHT + self.AXIS_GAP + BOX_HEIGHT + 2
        budget = self.height_budget
        if budget is None or budget >= natural:
            return AXIS_HEIGHT, self.AXIS_GAP, BOX_HEIGHT, self.size, natural

        floor = self.MIN_AXIS + self.AXIS_GAP + self.MIN_BOX + 2
        target = max(floor, budget)
        shrinkable = AXIS_HEIGHT + BOX_HEIGHT
        keep = (target - self.AXIS_GAP - 2) / shrinkable
        axis = max(self.MIN_AXIS, AXIS_HEIGHT * keep)
        box = max(self.MIN_BOX, BOX_HEIGHT * keep)
        font = max(12.0, self.size * (box / BOX_HEIGHT))
        return axis, self.AXIS_GAP, box, font, axis + self.AXIS_GAP + box + 2

    def height(self, width: float) -> float:
        return self._metrics()[4]

    def draw(self, c: Canvas, x: float, top: float, width: float) -> None:
        number_label(c, x, top, self.index)
        p = self.problem
        axis, gap, box_height, font, _ = self._metrics()
        left = x + LABEL_WIDTH
        span = width - LABEL_WIDTH
        draw_axis(c, left, top, span, self.lowest, self.highest, axis)

        row_top = top - axis - gap
        text = f"{p.left} {p.glyph} {p.right} ="
        text_width = pdfmetrics.stringWidth(text, TEXT_BOLD, font)
        c.setFont(TEXT_BOLD, font)
        c.setFillColor(black)
        c.drawString(left, row_top - box_height / 2.0 - font * 0.34, text)
        answer_box(c, left + text_width + GAP, row_top, BOX_WIDTH, box_height)


REFERENCE_PAD = 8.0
REFERENCE_TITLE = 14.0


def reference_line_height(width: float) -> float:
    return AXIS_HEIGHT + REFERENCE_TITLE + 2 * REFERENCE_PAD


def draw_reference_line(c: Canvas, x: float, top: float, width: float,
                        lowest: int, highest: int) -> float:
    """One number line in a panel at the top of the page.

    The symbolic Level 1 sheets get this instead of a line per problem:
    the operands are large enough that counting on fingers stops working,
    but a line beside every question would crowd the page.
    """
    height = reference_line_height(width)
    c.saveState()
    c.setStrokeColor(render.HAIRLINE)
    c.setLineWidth(1)
    c.roundRect(x, top - height, width, height, 6, stroke=1, fill=0)
    c.setFont(TEXT_BOLD, 9)
    c.setFillColor(GREY)
    c.drawString(x + REFERENCE_PAD, top - REFERENCE_PAD - 8, "Number line")
    c.restoreState()
    draw_axis(
        c,
        x + REFERENCE_PAD,
        top - REFERENCE_PAD - REFERENCE_TITLE,
        width - 2 * REFERENCE_PAD,
        lowest,
        highest,
    )
    return height


# --- boxed groups (Counting Multiplication) --------------------------------

@dataclass
class _BoxedGroups:
    """Shared layout for ``groups`` pens of ``per_group`` animals.

    Pens wrap onto extra lines when they don't fit across the column, and
    a pen is never split across a line — that "this many groups of this
    many" shape is the whole point of the picture.
    """

    groups: int
    per_group: int
    emoji: str
    size: float = 14.0
    pad: float = 4.0
    pen_gap: float = 8.0
    row_gap: float = 6.0

    def _pen_size(self):
        per_row = min(self.per_group, 4)
        inner_w, inner_h = animal_grid_size(self.per_group, per_row, self.size)
        return inner_w + 2 * self.pad, inner_h + 2 * self.pad, per_row

    def layout(self, width: float):
        pen_w, pen_h, per_row = self._pen_size()
        per_line = max(1, int((width + self.pen_gap) // (pen_w + self.pen_gap)))
        lines = max(1, math.ceil(self.groups / per_line)) if self.groups else 0
        height = lines * pen_h + max(0, lines - 1) * self.row_gap
        return pen_w, pen_h, per_row, per_line, height

    def height(self, width: float) -> float:
        if self.groups == 0 or self.per_group == 0:
            return 22.0
        return self.layout(width)[4]

    def draw(self, c: Canvas, x: float, top: float, width: float) -> None:
        if self.groups == 0 or self.per_group == 0:
            c.saveState()
            c.setFont(TEXT, 10)
            c.setFillColor(LIGHT)
            c.drawString(x, top - 14, "(no animals)")
            c.restoreState()
            return
        pen_w, pen_h, per_row, per_line, _ = self.layout(width)
        c.saveState()
        c.setStrokeColor(LIGHT)
        c.setLineWidth(1)
        for i in range(self.groups):
            line = i // per_line
            column = i % per_line
            pen_x = x + column * (pen_w + self.pen_gap)
            pen_top = top - line * (pen_h + self.row_gap)
            c.roundRect(pen_x, pen_top - pen_h, pen_w, pen_h, 5, stroke=1, fill=0)
            draw_animal_grid(
                c, pen_x + self.pad, pen_top - self.pad,
                self.emoji, self.per_group, self.size, per_row,
            )
        c.restoreState()


@dataclass
class MultCountingBlock:
    """``X × Y = [ ]`` above Y pens of X animals."""

    problem: Problem
    index: int
    size: float = 19.0

    def _groups(self) -> _BoxedGroups:
        p = self.problem
        return _BoxedGroups(groups=p.right, per_group=p.left, emoji=p.animal.emoji)

    def height(self, width: float) -> float:
        inner = width - LABEL_WIDTH
        return max(BOX_HEIGHT, self.size * 1.3) + 6 + self._groups().height(inner)

    def draw(self, c: Canvas, x: float, top: float, width: float) -> None:
        number_label(c, x, top, self.index)
        p = self.problem
        left = x + LABEL_WIDTH
        text = f"{p.left} × {p.right} ="
        text_width = pdfmetrics.stringWidth(text, TEXT_BOLD, self.size)
        head = max(BOX_HEIGHT, self.size * 1.3)

        c.setFont(TEXT_BOLD, self.size)
        c.setFillColor(black)
        c.drawString(left, top - head / 2.0 - self.size * 0.34, text)
        answer_box(c, left + text_width + GAP, top - (head - BOX_HEIGHT) / 2.0,
                   BOX_WIDTH, BOX_HEIGHT)
        self._groups().draw(c, left, top - head - 6, width - LABEL_WIDTH)


@dataclass
class MultOperandsBlock:
    """Pens of animals above ``[ ] × [ ]`` — name the two operands.

    Mirrors Counting Multiplication Level 1, where the learner picks how
    many are in each group and how many groups there are rather than the
    product.
    """

    problem: Problem
    index: int
    size: float = 19.0

    def _groups(self) -> _BoxedGroups:
        p = self.problem
        return _BoxedGroups(groups=p.right, per_group=p.left, emoji=p.animal.emoji)

    def height(self, width: float) -> float:
        inner = width - LABEL_WIDTH
        return self._groups().height(inner) + 8 + BOX_HEIGHT

    def draw(self, c: Canvas, x: float, top: float, width: float) -> None:
        number_label(c, x, top, self.index)
        left = x + LABEL_WIDTH
        groups = self._groups()
        groups_height = groups.height(width - LABEL_WIDTH)
        groups.draw(c, left, top, width - LABEL_WIDTH)

        row_top = top - groups_height - 8
        times = " × "
        times_width = pdfmetrics.stringWidth(times, TEXT_BOLD, self.size)
        answer_box(c, left, row_top, BOX_WIDTH, BOX_HEIGHT)
        c.setFont(TEXT_BOLD, self.size)
        c.setFillColor(black)
        c.drawString(left + BOX_WIDTH, row_top - BOX_HEIGHT / 2.0 - self.size * 0.34, times)
        answer_box(c, left + BOX_WIDTH + times_width, row_top, BOX_WIDTH, BOX_HEIGHT)


# --- division --------------------------------------------------------------

@dataclass
class DivisionCountingBlock:
    """``X`` animals above ``X ÷ Y = [ ]``.

    The animals are laid out in a plain rectangle — deliberately *not*
    pre-grouped, since sorting them into equal groups is the work the
    lesson asks the child to picture.
    """

    problem: Problem
    index: int
    size: float = 15.0
    per_row: int = 6

    def _grid(self, width: float):
        available = width - LABEL_WIDTH
        size = self.size
        per_row = self.per_row
        while size > 8.0 and per_row * animal_advance(size) > available:
            size -= 0.5
        return size, per_row

    def height(self, width: float) -> float:
        size, per_row = self._grid(width)
        _, grid_height = animal_grid_size(self.problem.left, per_row, size)
        return grid_height + 6 + max(BOX_HEIGHT, self.size * 1.3)

    def draw(self, c: Canvas, x: float, top: float, width: float) -> None:
        number_label(c, x, top, self.index)
        p = self.problem
        left = x + LABEL_WIDTH
        size, per_row = self._grid(width)
        _, grid_height = animal_grid_size(p.left, per_row, size)
        draw_animal_grid(c, left, top, p.animal.emoji, p.left, size, per_row)

        head = max(BOX_HEIGHT, self.size * 1.3)
        row_top = top - grid_height - 6
        text = f"{p.left} ÷ {p.right} ="
        text_width = pdfmetrics.stringWidth(text, TEXT_BOLD, 19.0)
        c.setFont(TEXT_BOLD, 19.0)
        c.setFillColor(black)
        c.drawString(left, row_top - head / 2.0 - 19.0 * 0.34, text)
        answer_box(c, left + text_width + GAP, row_top - (head - BOX_HEIGHT) / 2.0,
                   BOX_WIDTH, BOX_HEIGHT)


# --- binary ----------------------------------------------------------------

MIN_SLOT_WIDTH = 17.0
SLOT_HEIGHT = 20.0
ANSWER_GAP = 6.0


def _answer_band(size: float, printed: bool) -> float:
    """Height the answer line takes: a box to write in, or one line of type."""
    return size * 1.15 if printed else SLOT_HEIGHT


def binary_stack_height(size: float, printed_answer: bool = False) -> float:
    """Total height of one stacked binary equation at ``size``.

    Both the problems and the cheat sheet lay these out, and getting it
    wrong silently overlaps them, so both ask here rather than guessing.
    """
    return (
        size                      # first operand line
        + size * 1.25             # second operand line
        + size * 0.34             # drop to the rule
        + ANSWER_GAP
        + _answer_band(size, printed_answer)
    )


def _binary_metrics(bits: int, size: float, operator: str, slots: bool) -> dict:
    """Measure one stacked binary equation.

    A one-bit answer slot as wide as the digit above it is too narrow for
    a child to write in, so slots get a comfortable minimum width and the
    rule line stretches to cover whichever of the two is wider. Offsets
    are relative to the operator word's left edge, which is why ``left``
    can be negative.
    """
    word_width = pdfmetrics.stringWidth(operator + " ", TEXT_BOLD, size)
    digits_width = pdfmetrics.stringWidth("0" * bits, TEXT_BOLD, size)
    marker_width = pdfmetrics.stringWidth("2", TEXT, size * 0.62)
    digits_x = word_width

    slot_width = max(digits_width / bits, MIN_SLOT_WIDTH) if slots else 0.0
    slots_width = slot_width * bits
    slots_x = digits_x + (digits_width - slots_width) / 2.0 if slots else digits_x

    left = min(0.0, slots_x) if slots else 0.0
    right = digits_x + digits_width + marker_width
    if slots:
        right = max(right, slots_x + slots_width + marker_width)
    return {
        "word_width": word_width,
        "digits_x": digits_x,
        "digits_width": digits_width,
        "marker_width": marker_width,
        "slot_width": slot_width,
        "slots_x": slots_x,
        "slots_width": slots_width,
        "left": left,
        "right": right,
        "width": right - left,
    }


def _binary_stack_width(bits: int, size: float, operator: str, slots: bool = True) -> float:
    return _binary_metrics(bits, size, operator, slots)["width"]


def _draw_binary_stack(
    c: Canvas,
    x: float,
    top: float,
    size: float,
    problem: BinaryProblem,
    answer: str,
) -> float:
    """Draw one stacked binary equation. Returns the height used.

    ``answer`` is the digits to print on the answer line (the cheat sheet
    prints them), or "" to leave empty slots for the child to fill.
    """
    metrics = _binary_metrics(problem.bits, size, problem.operator, slots=not answer)
    origin = x - metrics["left"]
    digits_x = origin + metrics["digits_x"]
    marker_x = digits_x + metrics["digits_width"]
    line = size * 1.25

    c.setFillColor(black)
    c.setFont(TEXT_BOLD, size)
    first = top - size
    c.drawString(digits_x, first, problem.binary(problem.left))
    subscript_two(c, marker_x, first, size)

    second = first - line
    c.setFont(TEXT_BOLD, size)
    c.drawString(origin, second, problem.operator + " ")
    c.drawString(digits_x, second, problem.binary(problem.right))
    subscript_two(c, marker_x, second, size)

    rule_y = second - size * 0.34
    c.setStrokeColor(black)
    c.setLineWidth(1.3)
    c.line(origin + metrics["left"], rule_y, origin + metrics["right"], rule_y)

    answer_band = _answer_band(size, bool(answer))
    third = rule_y - ANSWER_GAP - answer_band
    if answer:
        c.setFont(TEXT_BOLD, size)
        c.setFillColor(black)
        baseline = third + answer_band - size * 0.9
        c.drawString(digits_x, baseline, answer)
        subscript_two(c, marker_x, baseline, size)
    else:
        slots_x = origin + metrics["slots_x"]
        slot_width = metrics["slot_width"]
        c.saveState()
        c.setStrokeColor(LIGHT)
        c.setLineWidth(1.1)
        for i in range(problem.bits):
            c.roundRect(slots_x + i * slot_width + 1.5, third,
                        slot_width - 3, answer_band, 3, stroke=1, fill=0)
        c.restoreState()
        subscript_two(c, slots_x + metrics["slots_width"], third + 4, size)

    return top - third


@dataclass
class BinaryBlock:
    problem: BinaryProblem
    index: int
    size: float = 17.0

    def height(self, width: float) -> float:
        return binary_stack_height(self.size) + 6

    def draw(self, c: Canvas, x: float, top: float, width: float) -> None:
        number_label(c, x, top, self.index)
        stack_width = _binary_stack_width(self.problem.bits, self.size, self.problem.operator)
        left = x + LABEL_WIDTH + max(0.0, (width - LABEL_WIDTH - stack_width) / 2.0)
        _draw_binary_stack(c, left, top, self.size, self.problem, answer="")


_CHEAT_SIZE = 14.0
_CHEAT_PAD = 9.0
_CHEAT_TITLE = 18.0
_CHEAT_GAP = 10.0
# Row pitch has to clear a whole stack, or the answer of one rule collides
# with the first operand of the rule beneath it and the box reads as one
# six-line run instead of four separate rules.
_CHEAT_ROW = binary_stack_height(_CHEAT_SIZE, printed_answer=True) + 12.0


def cheat_sheet_height(width: float) -> float:
    """Height the binary cheat sheet needs at ``width``."""
    return _CHEAT_TITLE + 2 * _CHEAT_ROW + 2 * _CHEAT_PAD


def draw_cheat_sheet(c: Canvas, x: float, top: float, width: float) -> float:
    """The single-bit truth tables — one bordered box per operator.

    All three are always shown, so a child can look up any column of any
    problem below without being told which operator to use. Each operator
    gets its own box with its name on it, and its four rules sit in a 2×2
    grid inside: at three-across that leaves enough width to set them
    bigger than a single twelve-wide row ever could.
    """
    height = cheat_sheet_height(width)
    box_width = (width - 2 * _CHEAT_GAP) / 3.0

    for i, operator in enumerate(BINARY_OPERATORS):
        box_x = x + i * (box_width + _CHEAT_GAP)
        c.saveState()
        c.setStrokeColor(render.LIGHT)
        c.setLineWidth(1.2)
        c.roundRect(box_x, top - height, box_width, height, 7, stroke=1, fill=0)
        c.setFont(TEXT_BOLD, 12)
        c.setFillColor(black)
        c.drawCentredString(box_x + box_width / 2.0, top - _CHEAT_PAD - 10, operator)
        c.restoreState()

        # Four rules in a 2×2 grid inside the box.
        cases = ((0, 0), (0, 1), (1, 0), (1, 1))
        inner_left = box_x + _CHEAT_PAD
        inner_width = box_width - 2 * _CHEAT_PAD
        cell_width = inner_width / 2.0
        stack_width = _binary_stack_width(1, _CHEAT_SIZE, operator, slots=False)
        offset = max(0.0, (cell_width - stack_width) / 2.0)
        for j, (a, b) in enumerate(cases):
            problem = BinaryProblem(left=a, right=b, operator=operator, bits=1)
            _draw_binary_stack(
                c,
                inner_left + (j % 2) * cell_width + offset,
                top - _CHEAT_PAD - _CHEAT_TITLE - (j // 2) * _CHEAT_ROW,
                _CHEAT_SIZE,
                problem,
                answer=problem.binary(problem.answer),
            )

    return height
