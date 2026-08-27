"""Page furniture and the layout engine the worksheets are built on.

The generator never decides up front how many problems a sheet holds. It
hands :func:`fill_page` an endless problem stream and a rectangle, and the
filler pulls blocks until the next row would run off the page — so every
sheet carries as many problems as physically fit.
"""

import os
from typing import Iterable, List, Optional, Protocol, Sequence

from reportlab.lib.colors import Color, black
from reportlab.lib.pagesizes import letter
from reportlab.lib.units import inch
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen.canvas import Canvas

PAGE_SIZE = letter
PAGE_WIDTH, PAGE_HEIGHT = PAGE_SIZE
MARGIN = 0.6 * inch

TEXT = "HSText"
TEXT_BOLD = "HSText-Bold"
ANIMALS = "HSAnimals"

GREY = Color(0.42, 0.42, 0.45)
LIGHT = Color(0.72, 0.72, 0.76)
HAIRLINE = Color(0.85, 0.85, 0.88)

_FONT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "fonts")
_registered = False


def register_fonts() -> None:
    """Load the vendored subsets. Idempotent; safe to call per sheet."""
    global _registered
    if _registered:
        return
    pdfmetrics.registerFont(TTFont(TEXT, os.path.join(_FONT_DIR, "DejaVuSans-subset.ttf")))
    pdfmetrics.registerFont(TTFont(TEXT_BOLD, os.path.join(_FONT_DIR, "DejaVuSans-Bold-subset.ttf")))
    pdfmetrics.registerFont(TTFont(ANIMALS, os.path.join(_FONT_DIR, "NotoEmoji-animals-subset.ttf")))
    pdfmetrics.registerFontFamily(TEXT, normal=TEXT, bold=TEXT_BOLD)
    _registered = True


class Block(Protocol):
    """One problem, drawn into a column of the page grid.

    ``height`` is asked first so the filler can decide whether the row
    still fits; ``draw`` then paints it with its top-left corner at
    ``(x, top)`` in PDF coordinates (y grows upwards).
    """

    def height(self, width: float) -> float: ...

    def draw(self, c: Canvas, x: float, top: float, width: float) -> None: ...


# --- small drawing helpers -------------------------------------------------

def answer_box(c: Canvas, x: float, top: float, width: float, height: float) -> None:
    """The empty box a child writes an answer into."""
    c.saveState()
    c.setStrokeColor(LIGHT)
    c.setLineWidth(1.1)
    c.roundRect(x, top - height, width, height, 4, stroke=1, fill=0)
    c.restoreState()


def answer_rule(c: Canvas, x: float, y: float, width: float) -> None:
    """A write-on-the-line answer slot, used where a box would crowd."""
    c.saveState()
    c.setStrokeColor(LIGHT)
    c.setLineWidth(1.1)
    c.line(x, y, x + width, y)
    c.restoreState()


def subscript_two(c: Canvas, x: float, baseline: float, size: float) -> float:
    """Draw the base-2 marker and return its advance width.

    The subscript digit is in the vendored font, but drawing it as a
    shrunken, dropped glyph keeps it legible at worksheet sizes where the
    real ₂ turns to mush.
    """
    small = size * 0.62
    c.setFont(TEXT, small)
    c.drawString(x, baseline - size * 0.16, "2")
    width = pdfmetrics.stringWidth("2", TEXT, small)
    c.setFont(TEXT, size)
    return width


# The emoji glyphs fill their advance width edge to edge, so animals set
# at their natural advance touch each other and a row is hard to count.
# A little air between them is what makes a group countable.
ANIMAL_TRACKING = 1.15


def animal_advance(size: float, tracking: float = ANIMAL_TRACKING) -> float:
    return pdfmetrics.stringWidth("\U0001F431", ANIMALS, size) * tracking


def animal_line_height(size: float, leading: float = 1.12) -> float:
    return size * leading


def animal_grid_size(count: int, per_row: int, size: float,
                     tracking: float = ANIMAL_TRACKING, leading: float = 1.12):
    """(width, height) of ``count`` animals wrapped at ``per_row``."""
    if count <= 0:
        return 0.0, 0.0
    columns = min(count, per_row)
    rows = (count + per_row - 1) // per_row
    return (columns * animal_advance(size, tracking),
            rows * animal_line_height(size, leading))


def draw_animal_grid(
    c: Canvas,
    x: float,
    top: float,
    emoji: str,
    count: int,
    size: float,
    per_row: int,
    tracking: float = ANIMAL_TRACKING,
    leading: float = 1.12,
) -> None:
    """Draw ``count`` animals wrapped at ``per_row``, top-left at (x, top).

    The last row is centred under the full rows so a group like 7-over-4
    doesn't read as a ragged left-aligned block.
    """
    if count <= 0:
        return
    advance = animal_advance(size, tracking)
    line_height = animal_line_height(size, leading)
    # Centre each glyph in its (tracked) cell so the extra air sits either
    # side of the animal rather than all of it on the right.
    inset = (advance - pdfmetrics.stringWidth(emoji, ANIMALS, size)) / 2.0
    c.saveState()
    c.setFont(ANIMALS, size)
    c.setFillColor(black)
    remaining = count
    row = 0
    full_width = min(count, per_row) * advance
    while remaining > 0:
        in_row = min(remaining, per_row)
        baseline = top - row * line_height - size * 0.86 - (line_height - size * 1.12) / 2.0
        row_x = x + (full_width - in_row * advance) / 2.0 + inset
        for i in range(in_row):
            c.drawString(row_x + i * advance, baseline, emoji)
        remaining -= in_row
        row += 1
    c.restoreState()


def empty_pen(c: Canvas, x: float, top: float, width: float, height: float) -> None:
    """A dashed empty box, drawn where a group holds zero animals.

    A blank gap reads as a printing mistake; an empty pen reads as "none".
    """
    c.saveState()
    c.setStrokeColor(LIGHT)
    c.setLineWidth(1)
    c.setDash(2, 2)
    c.roundRect(x, top - height, width, height, 4, stroke=1, fill=0)
    c.setDash()
    c.setFillColor(LIGHT)
    label = min(13.0, height * 0.55)
    c.setFont(TEXT, label)
    c.drawCentredString(x + width / 2.0, top - height / 2.0 - label * 0.34, "0")
    c.restoreState()


# --- page furniture --------------------------------------------------------

HEADER_GAP = 16.0
FOOTER_HEIGHT = 20.0


def draw_header(c: Canvas, sheet, subtitle: Optional[str] = None) -> float:
    """Draw the title block. Returns the y of the top of the problem area."""
    top = PAGE_HEIGHT - MARGIN
    right = PAGE_WIDTH - MARGIN

    c.setFont(TEXT, 9)
    c.setFillColor(GREY)
    name_line = "Name ______________________"
    c.drawRightString(right, top - 6, name_line)
    c.drawRightString(right, top - 19, "Date ______________________")

    # Shrink the title rather than let it run into the name block: the
    # write-the-sentence sheets carry long names.
    room = (PAGE_WIDTH - 2 * MARGIN) - pdfmetrics.stringWidth(name_line, TEXT, 9) - 14
    title = f"{sheet.title} — Level {sheet.level}"
    title_size = 17.0
    while title_size > 10.0 and pdfmetrics.stringWidth(title, TEXT_BOLD, title_size) > room:
        title_size -= 0.5
    c.setFont(TEXT_BOLD, title_size)
    c.setFillColor(black)
    c.drawString(MARGIN, top - 15, title)

    c.setFont(TEXT, 9.5)
    c.setFillColor(GREY)
    c.drawString(MARGIN, top - 30, subtitle or sheet.instructions)

    rule_y = top - 39
    c.setStrokeColor(HAIRLINE)
    c.setLineWidth(1)
    c.line(MARGIN, rule_y, right, rule_y)
    c.setFillColor(black)
    return rule_y - HEADER_GAP


def draw_footer(c: Canvas, sheet) -> None:
    c.saveState()
    c.setFont(TEXT, 7.5)
    c.setFillColor(LIGHT)
    c.drawString(MARGIN, MARGIN - 10, f"Homeschool Teacher · {sheet.lesson}")
    c.drawRightString(PAGE_WIDTH - MARGIN, MARGIN - 10, sheet.slug)
    c.restoreState()


def content_area(top: float):
    """(x, top, width, height) of the region problems may occupy."""
    bottom = MARGIN + FOOTER_HEIGHT
    return MARGIN, top, PAGE_WIDTH - 2 * MARGIN, top - bottom


# --- the filler ------------------------------------------------------------

COLUMN_GAP = 16.0
ROW_GAP = 12.0


def fill_page(
    c: Canvas,
    blocks: Iterable[Block],
    area,
    columns: int,
    max_rows: Optional[int] = None,
) -> int:
    """Lay blocks into a ``columns``-wide grid until the page is full.

    Rows are ragged-height: each row is as tall as its tallest block, so a
    counting problem with a big group of animals doesn't force blank space
    onto the shorter problems beside it.

    ``max_rows`` stops after that many rows even if more would fit, for
    sheets that ask for an exact shape; the blocks on those sheets have
    already been given a matching height budget, so the page still fills.

    Returns how many blocks were drawn. Blocks pulled from the stream but
    left undrawn are discarded — the stream is endless, so nothing is lost.
    """
    x0, top, total_width, total_height = area
    column_width = (total_width - COLUMN_GAP * (columns - 1)) / columns

    stream = iter(blocks)
    used = 0.0
    drawn = 0
    placed_rows = 0
    while True:
        if max_rows is not None and placed_rows >= max_rows:
            break
        row: List[Block] = []
        for _ in range(columns):
            try:
                row.append(next(stream))
            except StopIteration:
                break
        if not row:
            break

        row_height = max(b.height(column_width) for b in row)
        needed = row_height if drawn == 0 else row_height + ROW_GAP
        if used + needed > total_height:
            break

        used += needed
        placed_rows += 1
        row_top = top - used + row_height
        for i, block in enumerate(row):
            block.draw(c, x0 + i * (column_width + COLUMN_GAP), row_top, column_width)
            drawn += 1

        if len(row) < columns:
            break
    return drawn


def number_label(c: Canvas, x: float, top: float, index: int) -> None:
    """The small grey problem number in a cell's top-left corner."""
    c.saveState()
    c.setFont(TEXT, 8)
    c.setFillColor(LIGHT)
    c.drawString(x, top - 8, f"{index}.")
    c.restoreState()


LABEL_WIDTH = 16.0
