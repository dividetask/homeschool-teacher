# Vendored fonts

The worksheet generator needs two things a printer-friendly PDF can't get
from the PDF base-14 fonts: recognisable black-and-white animal pictures,
and the subscript `₂` the binary sheets use. Both come from freely
licensed fonts, subset down to just the characters this tool draws so the
repository carries kilobytes rather than megabytes.

Nothing is downloaded at run time — the subsets below are all the
generator ever loads.

## `NotoEmoji-animals-subset.ttf`

Noto Emoji (the **monochrome** family, not Noto Color Emoji), subset to
the 22 animal code points listed in `animals.py` — the same animals the
Android app uses in `shared/.../reading/Animal.kt`. The glyphs are black
line art, so they photocopy and print cleanly on a mono printer and a
child can colour them in.

Source: <https://fonts.google.com/noto/specimen/Noto+Emoji>
License: SIL Open Font License 1.1 — see `LICENSE-NotoEmoji.txt`.

Regenerated with:

```
pyftsubset NotoEmoji-Regular.ttf \
  --unicodes="$(python3 -c "import animals; print(animals.subset_unicodes())")" \
  --output-file=NotoEmoji-animals-subset.ttf \
  --no-hinting --drop-tables+=GSUB,STAT,vhea,vmtx,gasp \
  --name-IDs=1,2,3,4,5,6 --notdef-outline
```

Keep `--name-IDs` non-empty: ReportLab reads the TTF name table and
raises `AttributeError` on a font that has none.

## `DejaVuSans-subset.ttf`, `DejaVuSans-Bold-subset.ttf`

DejaVu Sans, subset to printable ASCII plus the symbols the sheets draw:
`× ÷ − ─ □ ✓` and the subscript digits `₀`–`₉`.

Source: <https://dejavu-fonts.github.io/>
License: Bitstream Vera / DejaVu (permissive) — see `LICENSE-DejaVu.txt`.

Regenerated with:

```
pyftsubset DejaVuSans.ttf \
  --unicodes='U+0020-007E,U+00A0,U+00B7,U+00D7,U+00F7,U+2010-2015,U+2018,U+2019,U+201C,U+201D,U+2022,U+2026,U+2212,U+2080-2089,U+25A1,U+2500,U+2502,U+2713' \
  --output-file=DejaVuSans-subset.ttf \
  --no-hinting --name-IDs=1,2,3,4,5,6 --notdef-outline
```
