#!/bin/sh
# Create the virtualenv the worksheet generator runs in.
#
# Run once. After this, ./worksheets.py finds .venv on its own, so you
# can call it directly without activating anything.
#
# Set PYTHON=/path/to/python3 to pick the interpreter yourself.
set -e
cd "$(dirname "$0")"

at_least() {
    "$1" -c "import sys; sys.exit(0 if sys.version_info[:2] >= ($2, $3) else 1)" 2>/dev/null
}

version_of() {
    "$1" -c 'import platform; print(platform.python_version())' 2>/dev/null
}

PY="${PYTHON:-python3}"
command -v "$PY" >/dev/null 2>&1 || {
    echo "setup: no '$PY' on PATH" >&2
    exit 1
}

# ReportLab from 4.4.3 on needs Python 3.9, so on anything older
# requirements.txt has to pin back to 4.4.2. Prefer a newer interpreter
# when the machine has one rather than silently settling for the pin.
if [ -z "$PYTHON" ] && ! at_least "$PY" 3 9; then
    for candidate in python3.13 python3.12 python3.11 python3.10 python3.9; do
        if command -v "$candidate" >/dev/null 2>&1 && at_least "$candidate" 3 9; then
            echo "note: python3 is $(version_of "$PY"); using $candidate instead"
            PY="$candidate"
            break
        fi
    done
fi

# Hard floor: the generator uses typing.Protocol.
at_least "$PY" 3 8 || {
    echo "setup: need Python 3.8 or newer, found $(version_of "$PY")" >&2
    exit 1
}

at_least "$PY" 3 9 || cat <<NOTE
note: Python $(version_of "$PY") is older than 3.9, so ReportLab is pinned
      to 4.4.2 — the last release that runs there. Everything works; you
      just won't get newer ReportLab until you upgrade Python.
NOTE

# Recreate rather than reuse: an existing .venv may have been built from
# a different interpreter, or hold the broken ReportLab this pin avoids.
if [ -f .venv/pyvenv.cfg ]; then
    rm -rf .venv
fi

"$PY" -m venv .venv
.venv/bin/pip install --quiet --upgrade pip
.venv/bin/pip install --quiet -r requirements.txt

echo "Ready (Python $(version_of .venv/bin/python), ReportLab $(.venv/bin/python -c 'import reportlab; print(reportlab.Version)'))."
echo "Try:  ./worksheets.py --list"
