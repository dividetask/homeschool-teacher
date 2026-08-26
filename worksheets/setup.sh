#!/bin/sh
# Create the virtualenv the worksheet generator runs in.
#
# Run once. After this, ./worksheets.py finds .venv on its own, so you
# can call it directly without activating anything.
set -e
cd "$(dirname "$0")"

python3 -m venv .venv
.venv/bin/pip install --quiet --upgrade pip
.venv/bin/pip install --quiet -r requirements.txt

echo "Ready. Try:  ./worksheets.py --list"
