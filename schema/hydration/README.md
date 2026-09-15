# Hydration corpus

The cases every implementation of hydration has to agree on.

There are two of them — the one the Studio's canvas runs, and the one a service runs to answer a
device — and before this corpus existed they disagreed about almost everything that mattered:
which containers repeat, whether a name resolves inside an enclosing list, and what one item is
called. A screen could look finished in the editor and ship its placeholders to a user as literal
text, because nothing anywhere compared the two.

Each file is one case:

| Key | Meaning |
| --- | --- |
| `name` | Identifier used in test output. |
| `why` | The failure this case exists to prevent. |
| `options.onUnresolved` | `keep` leaves the braces in place, `blank` empties them. Both report. |
| `screen` | The authored document, before hydration. |
| `data` | The payload. |
| `expected` | The document a device receives. `repeat`, `scope` and `data` are gone; `metadata` is not. |
| `expectedUnresolved` | Every expression that resolved to nothing: nodes in document order, and within one object in the order of its keys sorted. |

This directory is canonical. Consumers hold a copy:

- `heimui-studio/ui/src/__tests__/fixtures/hydration/`
- `prototype-heimui-backend/src/test/resources/hydration/`

Copy it across with `./scripts/sync-schema.sh`, which also removes cases a consumer still holds
after they are deleted here. `--check` reports drift without touching anything, and is what
`.githooks/pre-push` runs so a schema change cannot leave a consumer behind unnoticed.

A consumer whose copy is stale passes its own tests and disagrees with the other implementation,
which is the exact failure this corpus exists to catch. Three things guard it, because the one that
would be most convenient is the one that cannot always run:

- `scripts/sync-schema.sh --check`, on the machine where the copy is actually made.
- `heimui-studio/ui/src/__tests__/schemaSync.test.ts`, which compares against a sibling checkout
  and skips when there is not one.
- The `schema-sync` job in the Studio's CI, when a credential for reading this private repository
  is available. It says plainly when it could not check, rather than passing quietly.
