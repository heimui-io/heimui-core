#!/usr/bin/env bash
#
# Fails when a version badge in the README disagrees with the version catalogue.
#
# A badge is a claim about what this library is built on, read by people deciding whether it fits
# their project -- and it is hand-written, which means it drifts the first time somebody bumps a
# dependency and does not think about the README. The Compose badge said 1.11.1 while the build had
# been on 1.12.0 since the beginning; nobody noticed because nothing was looking.
#
# The catalogue is the truth, the badge is a copy, and copies get checked.
#
#   ./scripts/check-readme-versions.sh
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

python3 - "$here" <<'PY'
import re, sys
from pathlib import Path

root = Path(sys.argv[1])
catalogue = (root / "gradle/libs.versions.toml").read_text()
readme = (root / "README.md").read_text()

# badge label as it appears in the shields.io URL  ->  key in the catalogue
BADGES = {
    "Kotlin": "kotlin",
    "Compose%20Multiplatform": "composeMultiplatform",
}

failed = False
for label, key in BADGES.items():
    declared = re.search(rf'^{key}\s*=\s*"([^"]+)"', catalogue, re.M)
    if not declared:
        print(f"FAIL: `{key}` is not in gradle/libs.versions.toml, so the {label} badge cannot be checked.")
        failed = True
        continue
    shown = re.search(rf'{label}-([^-\s]+)-', readme)
    if not shown:
        print(f"FAIL: no {label} badge found in README.md.")
        failed = True
        continue
    if shown.group(1) != declared.group(1):
        print(f"FAIL: the {label.replace('%20', ' ')} badge says {shown.group(1)}, "
              f"the build uses {declared.group(1)}.")
        failed = True
    else:
        print(f"OK: {label.replace('%20', ' ')} {declared.group(1)}")

sys.exit(1 if failed else 0)
PY
