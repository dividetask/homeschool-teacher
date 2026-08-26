"""One block renderer per worksheet style.

Each class measures itself first and draws second, so ``render.fill_page``
can pack ragged-height rows without knowing what a block contains.
"""

import math
from dataclasses import dataclass
from typing import List, Sequence

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

    The animal size shrinks until the whole equation fits the column, so
    the same block works in a two-up Level 0 sheet and a one-up Level 1
    sheet where a group can hold nine animals.
    """

    problem: Problem
    index: int
    base_size: float = 21.0
    _size: float = 0.0

    def _group_shape(self, count: int, split: bool):
        if count == 0:
            return 1, 1
        if split:
            return (count + 1) // 2, 2
        return count, 1

    def _fit(self, width: float) -> float:
        available = width - LABEL_WIDTH
        p = self.problem
        left_cols, _ = self._group_shape(p.left, p.split_left)
        right_cols, _ = self._group_shape(p.right, p.split_right)
        size = self.base_size
        while size > 9.0:
            symbols = pdfmetrics.stringWidth(f" {p.glyph} ", TEXT_BOLD, size) + \
                pdfmetrics.stringWidth(" = ", TEXT_BOLD, size)
            total = (left_cols + right_cols) * animal_advance(size) + symbols + BOX_WIDTH
            if total <= available:
                break
            size -= 0.5
        return size

    def height(self, width: float) -> float:
        self._size = self._fit(width)
        p = self.problem
        _, left_rows = self._group_shape(p.left, p.split_left)
        _, right_rows = self._group_shape(p.right, p.split_right)
        rows = max(left_rows, right_rows)
        return max(rows * render.animal_line_height(self._size), BOX_HEIGHT) + 8

    def draw(self, c: Canvas, x: float, top: float, width: float) -> None:
        number_label(c, x, top, self.index)
        p = self.problem
        size = self._size or self._fit(width)
        band = self.height(width)
        symbol_size = min(size, 22.0)

        left_cols, left_rows = self._group_shape(p.left, p.split_left)
        right_cols, right_rows = self._group_shape(p.right, p.split_right)
        advance = animal_advance(size)
        line_height = render.animal_line_height(size)

        operator = f" {p.glyph} "
        equals = " = "
        operator_width = pdfmetrics.stringWidth(operator, TEXT_BOLD, symbol_size)
        equals_width = pdfmetrics.stringWidth(equals, TEXT_BOLD, symbol_size)
        content = (left_cols + right_cols) * advance + operator_width + equals_width + BOX_WIDTH
        cursor = x + LABEL_WIDTH + max(0.0, (width - LABEL_WIDTH - content) / 2.0)

        middle = top - band / 2.0
        baseline = middle - symbol_size * 0.34
        c.setFillColor(black)

        def group(count: int, cols: int, rows: int, split: bool, at: float) -> float:
            block_height = rows * line_height
            group_top = middle + block_height / 2.0
            if count == 0:
                empty_pen(c, at, middle + line_height / 2.0, advance, line_height)
            else:
                per_row = (count + 1) // 2 if split else count
                draw_animal_grid(c, at, group_top, p.animal.emoji, count, size, per_row)
            return at + cols * advance

        cursor = group(p.left, left_cols, left_rows, p.split_left, cursor)
        c.setFont(TEXT_BOLD, symbol_size)
        c.drawString(cursor, baseline, operator)
        cursor += operator_width
        cursor = group(p.right, right_cols, right_rows, p.split_right, cursor)
        c.setFont(TEXT_BOLD, symbol_size)
        c.drawString(cursor, baseline, equals)
        cursor += equals_width
        answer_box(c, cursor, middle + BOX_HEIGHT / 2.0, BOX_WIDTH, BOX_HEIGHT)


# --- number line -----------------------------------------------------------

def next_multiple_of_ten(x: int) -> int:
    return ((x + 9) // 10) * 10


@dataclass
class NumberLineBlock:
    """A 0..``highest`` line with the equation printed beneath it.

    The range is fixed per sheet rather than per problem — the app widens
    its line to suit each problem, but on paper a line that changes length
    every few rows is hard to read across, so every line on a sheet is the
    same and sized to the sheet's hardest problem.
    """

    problem: Problem
    index: int
    highest: int
    size: float = 19.0

    LINE_HEIGHT = 26.0

    def height(self, width: float) -> float:
        return self.LINE_HEIGHT + self.size * 1.8 + 2

    def draw(self, c: Canvas, x: float, top: float, width: float) -> None:
        number_label(c, x, top, self.index)
        p = self.problem
        left = x + LABEL_WIDTH
        span = width - LABEL_WIDTH
        step = span / float(self.highest)

        # Label every tick when there's room; otherwise thin out to every
        # 5th or 10th and let the minor ticks carry the counting.
        if step >= 13:
            label_every = 1
        elif step >= 6:
            label_every = 5
        else:
            label_every = 10

        axis_y = top - 14
        c.saveState()
        c.setStrokeColor(black)
        c.setLineWidth(1.1)
        c.line(left, axis_y, left + span, axis_y)
        label_size = min(8.0, max(5.5, step * 0.62))
        c.setFont(TEXT, label_size)
        for n in range(self.highest + 1):
            tick_x = left + n * step
            major = n % label_every == 0
            tick = 6 if major else 3
            c.setLineWidth(1.1 if major else 0.7)
            c.line(tick_x, axis_y - tick, tick_x, axis_y + tick)
            if major:
                c.setFillColor(GREY)
                c.drawCentredString(tick_x, axis_y - tick - label_size - 1, str(n))
        c.restoreState()

        text = f"{p.left} {p.glyph} {p.right} ="
        text_width = pdfmetrics.stringWidth(text, TEXT_BOLD, self.size)
        baseline = top - self.LINE_HEIGHT - self.size
        c.setFont(TEXT_BOLD, self.size)
        c.setFillColor(black)
        c.drawString(left, baseline, text)
        answer_box(c, left + text_width + GAP, baseline + self.size * 0.78,
                   BOX_WIDTH, BOX_HEIGHT)


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

    third = rule_y - 6 - SLOT_HEIGHT
    if answer:
        c.setFont(TEXT_BOLD, size)
        c.setFillColor(black)
        baseline = third + SLOT_HEIGHT - size * 0.9
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
                        slot_width - 3, SLOT_HEIGHT, 3, stroke=1, fill=0)
        c.restoreState()
        subscript_two(c, slots_x + metrics["slots_width"], third + 4, size)

    return top - third


@dataclass
class BinaryBlock:
    problem: BinaryProblem
    index: int
    size: float = 17.0

    def height(self, width: float) -> float:
        return self.size * 2.25 + SLOT_HEIGHT + 14

    def draw(self, c: Canvas, x: float, top: float, width: float) -> None:
        number_label(c, x, top, self.index)
        stack_width = _binary_stack_width(self.problem.bits, self.size, self.problem.operator)
        left = x + LABEL_WIDTH + max(0.0, (width - LABEL_WIDTH - stack_width) / 2.0)
        _draw_binary_stack(c, left, top, self.size, self.problem, answer="")


def cheat_sheet_height(width: float) -> float:
    """Height the binary cheat sheet panel needs at ``width``."""
    return 3 * _CHEAT_ROW_HEIGHT + _CHEAT_TITLE_HEIGHT + 2 * _CHEAT_PAD


_CHEAT_SIZE = 12.0
_CHEAT_ROW_HEIGHT = 54.0
_CHEAT_TITLE_HEIGHT = 16.0
_CHEAT_PAD = 8.0


def draw_cheat_sheet(c: Canvas, x: float, top: float, width: float) -> float:
    """The single-bit truth tables, drawn in the app's stacked layout.

    Every binary sheet opens with all three, so a child can look up any
    column of any problem below without being told which operator to use.
    Returns the height consumed.
    """
    height = cheat_sheet_height(width)
    c.saveState()
    c.setStrokeColor(render.HAIRLINE)
    c.setLineWidth(1)
    c.roundRect(x, top - height, width, height, 6, stroke=1, fill=0)

    c.setFont(TEXT_BOLD, 10)
    c.setFillColor(GREY)
    c.drawString(x + _CHEAT_PAD, top - _CHEAT_PAD - 9, "Cheat sheet")
    c.restoreState()

    row_top = top - _CHEAT_PAD - _CHEAT_TITLE_HEIGHT
    inner_left = x + _CHEAT_PAD
    inner_width = width - 2 * _CHEAT_PAD

    for operator in BINARY_OPERATORS:
        cases = [(0, 0), (0, 1), (1, 0), (1, 1)]
        stack_width = _binary_stack_width(1, _CHEAT_SIZE, operator, slots=False)
        step = inner_width / 4.0
        offset = max(0.0, (step - stack_width) / 2.0)
        for i, (a, b) in enumerate(cases):
            problem = BinaryProblem(left=a, right=b, operator=operator, bits=1)
            _draw_binary_stack(
                c,
                inner_left + i * step + offset,
                row_top,
                _CHEAT_SIZE,
                problem,
                answer=problem.binary(problem.answer),
            )
        row_top -= _CHEAT_ROW_HEIGHT

    return height
