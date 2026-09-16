#!/usr/bin/env bash
#
# Fails when a dependency ships without an attribution.
#
# Every library inside a distributed artifact is licensed by somebody else, and every one of those
# licences asks the same minimum: say what you are shipping and whose it is. NOTICE is that answer,
# and a hand-written answer goes stale on the first `implementation(...)` somebody adds -- which is
# how a product ends up distributing software it never attributed, and nobody finds out until a
# customer's legal review does.
#
# So the list is not trusted, it is derived: the runtime classpath is asked, and every group id it
# reports has to appear in NOTICE.
#
#   ./scripts/check-notice.sh          report what is missing, exit 1 if anything is
#   ./scripts/check-notice.sh --list   print the groups that ship, for updating NOTICE
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
notice="$here/NOTICE"

# What a customer's app actually links. The Android runtime classpath is the superset: the iOS
# framework links the same libraries through the same source sets, plus Skiko in place of AndroidX.
configurations=("androidRuntimeClasspath" "iosArm64CompileKlibraries")

groups() {
  for configuration in "${configurations[@]}"; do
    "$here/gradlew" -p "$here" :shared:dependencies --configuration "$configuration" -q 2>/dev/null |
      grep -oE "[a-z][a-z0-9.-]+:[a-z0-9.-]+:[0-9][^ ]*" |
      cut -d: -f1
  done | sort -u
}

shipped="$(groups)"
if [[ -z "$shipped" ]]; then
  echo "check-notice: the dependency listing came back empty, which means this script is asking for a" >&2
  echo "configuration that no longer exists. Fix the script rather than trusting the silence." >&2
  exit 1
fi

if [[ "${1:-}" == "--list" ]]; then
  echo "$shipped"
  exit 0
fi

missing=()
while read -r group; do
  [[ -z "$group" ]] && continue
  grep -qF "$group" "$notice" || missing+=("$group")
done <<< "$shipped"

if (( ${#missing[@]} > 0 )); then
  echo "NOTICE does not mention $(( ${#missing[@]} )) group(s) that ship inside this SDK:"
  printf '  %s\n' "${missing[@]}"
  echo
  echo "Add them with the licence their POM declares. ./scripts/check-notice.sh --list prints them all."
  exit 1
fi

echo "NOTICE covers every group on the runtime classpath ($(wc -l <<< "$shipped" | tr -d ' ') of them)."
