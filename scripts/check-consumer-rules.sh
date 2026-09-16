#!/usr/bin/env bash
#
# Fails when the AAR ships without the R8 keep rules it promises.
#
# `consumer-rules.pro` only protects anybody if Android Gradle packages it into the AAR as
# `proguard.txt`. Declaring the file is not enough: `consumerKeepRules.publish` defaults to false,
# and with it off the rules are collected at build time and then dropped. Nothing fails, nothing
# warns -- the AAR is simply one file lighter, every consumer runs R8 against the SDK with no rules
# at all, and the first sign of it is a release build that cannot parse a screen the debug build
# rendered fine. That shipped once already.
#
# The declaration is therefore not trusted, the artifact is: build the AAR, open it, and check.
#
#   ./scripts/check-consumer-rules.sh
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source_rules="$here/shared/consumer-rules.pro"
aar="$here/shared/build/outputs/aar/shared.aar"

[ -f "$source_rules" ] || { echo "FAIL: shared/consumer-rules.pro is missing."; exit 1; }

# Only the AAR. `assemble` would also link the iOS release frameworks, which costs tens of minutes
# and can exhaust the Kotlin/Native compiler's heap -- neither of which says anything about whether
# an Android artifact carries its keep rules.
echo "Building the AAR..."
"$here/gradlew" -p "$here" :shared:bundleAndroidMainAar -q --console=plain

[ -f "$aar" ] || { echo "FAIL: no AAR at ${aar#"$here"/}."; exit 1; }

# Listed into a variable rather than piped into grep: under `set -o pipefail` a `grep -q` that
# matches exits immediately, `unzip` dies of SIGPIPE, and the pipeline reports 141 -- so the check
# would announce a missing file on an AAR that has it. A guard that cries wolf gets ignored.
listing="$(unzip -l "$aar")"

if ! grep -q "proguard.txt" <<<"$listing"; then
  cat >&2 <<'EOF'
FAIL: the AAR carries no proguard.txt.

The keep rules are not reaching consumers. Check that shared/build.gradle.kts sets both:

    optimization {
        consumerKeepRules.files.add(file("consumer-rules.pro"))
        consumerKeepRules.publish = true
    }
EOF
  exit 1
fi

# Same bytes, not merely some file by that name: a stale or empty proguard.txt passes the presence
# check and protects nothing.
if ! diff -q <(unzip -p "$aar" proguard.txt) "$source_rules" >/dev/null; then
  echo "FAIL: the AAR's proguard.txt does not match shared/consumer-rules.pro." >&2
  diff <(unzip -p "$aar" proguard.txt) "$source_rules" >&2 || true
  exit 1
fi

echo "OK: the AAR ships consumer-rules.pro as proguard.txt ($(wc -l < "$source_rules" | tr -d ' ') lines)."
