#!/usr/bin/env bash
#
# Copies the wire contract from here to whoever consumes it.
#
# The schema and the hydration corpus are the definition of what a screen is, and three codebases
# hold a copy: this one, the editor that validates against it, and any service that hydrates. A
# copy that goes stale is the worst kind of wrong -- both sides pass their own tests while
# disagreeing about what a screen renders, so a screen looks finished in the editor and ships its
# placeholders to a user as literal text.
#
# This exists because the copying was a command in a README, and a command in a README is a command
# somebody forgets. Run it from anywhere; it finds the consumers as siblings of this repository and
# skips the ones that are not checked out.
#
#   ./scripts/sync-schema.sh            copy, reporting what changed
#   ./scripts/sync-schema.sh --check    report only, exit 1 if anything differs
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
siblings="$(cd "$here/.." && pwd)"
check_only=false
[[ "${1:-}" == "--check" ]] && check_only=true

schema="$here/schema/heimui-screen.schema.json"
corpus="$here/schema/hydration"
drift=0

# Consumer definitions: where the schema goes, and where the corpus goes. A consumer that wants
# only one of them leaves the other empty.
consumers=(
  "heimui-studio|server/src/main/resources/heimui-screen.schema.json|ui/src/__tests__/fixtures/hydration"
  "prototype-heimui-backend||src/test/resources/hydration"
)

copy_file() {
  local from=$1 to=$2 label=$3
  if cmp -s "$from" "$to" 2>/dev/null; then
    return 0
  fi
  drift=1
  if $check_only; then
    echo "  DRIFT  $label"
  else
    mkdir -p "$(dirname "$to")"
    cp "$from" "$to"
    echo "  copied $label"
  fi
}

for entry in "${consumers[@]}"; do
  IFS='|' read -r name schema_path corpus_path <<< "$entry"
  root="$siblings/$name"

  if [[ ! -d "$root" ]]; then
    echo "$name: not checked out, skipped"
    continue
  fi
  echo "$name:"

  if [[ -n "$schema_path" ]]; then
    copy_file "$schema" "$root/$schema_path" "schema"
  fi

  if [[ -n "$corpus_path" ]]; then
    # The README beside the corpus documents it for a reader of this repository; a consumer holds
    # the cases only, so a stray file there is a case that no longer exists upstream.
    for case_file in "$corpus"/*.json; do
      copy_file "$case_file" "$root/$corpus_path/$(basename "$case_file")" "corpus/$(basename "$case_file")"
    done
    for existing in "$root/$corpus_path"/*.json; do
      [[ -e "$existing" ]] || continue
      if [[ ! -f "$corpus/$(basename "$existing")" ]]; then
        drift=1
        if $check_only; then
          echo "  EXTRA  corpus/$(basename "$existing")"
        else
          rm "$existing"
          echo "  removed corpus/$(basename "$existing") (no longer upstream)"
        fi
      fi
    done
  fi
done

if $check_only && [[ $drift -eq 1 ]]; then
  echo
  echo "A consumer's copy differs from this repository. Run ./scripts/sync-schema.sh to fix it."
  exit 1
fi

if [[ $drift -eq 0 ]]; then
  echo
  echo "Everything already in sync."
fi
