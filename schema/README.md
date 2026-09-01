# HeimUI Screen Schema

The wire contract between a HeimUI client and whatever serves its screens.

Any backend, in any language, that emits JSON matching `heimui-screen.schema.json` can drive a
HeimUI app. There is no required server library — the schema is the contract.

```
heimui-screen.schema.json   the contract
examples/                   one runnable payload per component family
```

## Use it while you build

**In your editor.** Add `$schema` to a JSON file and VSCode or IntelliJ will autocomplete
component types and flag invalid fields as you type, with nothing to install:

```json
{
  "$schema": "https://heimui.io/schema/v1/screen.json",
  "id": "checkout",
  "root": { "type": "container", "id": "root", "children": [] }
}
```

**In your CI.** Fail the pull request instead of shipping a broken screen:

```bash
npx ajv-cli validate -s heimui-screen.schema.json -d "screens/**/*.json" --spec=draft2020
```

**Against a real device.** Point the sample app at a directory of static JSON files and see how
the SDK actually renders them before any backend exists.

## The smallest valid screen

```json
{
  "id": "checkout",
  "root": {
    "type": "container",
    "id": "root",
    "children": [
      { "type": "text", "id": "titulo", "text": "Hola" },
      { "type": "button", "id": "pagar", "title": "Pagar",
        "actions": [ { "type": "submit_form", "endpoint": "/ms-pagos/transfer" } ] }
    ]
  }
}
```

Only `id` and `root` are required. Every other field has a default.

## Rules worth knowing before you write payloads

**Field names are `snake_case`** — `max_lines`, `visible_if`, `background_color`.

**Unknown component types are accepted on purpose.** A newer server can ship a component that
older clients do not know; they render a fallback and the rest of the tree survives. The
trade-off is that a typo like `"contaner"` passes validation and shows up as a fallback on the
device — the schema cannot tell a typo from a genuinely new component.

**Out-of-range values are repaired, not rejected.** Negative padding becomes `0`, `max_lines: 0`
becomes unlimited, duplicate sibling ids are disambiguated. The client reports every repair as a
`PayloadViolation` telemetry event, which is how you find out your backend is emitting invalid
SDUI before users do.

**Padding takes a number or an object.** Both are valid; the number form is the shorthand for all
four sides.

```json
"padding": 16
"padding": { "horizontal": 16, "vertical": 24 }
"padding": { "start": 48, "top": 32, "end": 8, "bottom": 0 }
```

Sides are `start`/`end`, not left/right, so one payload lays out correctly in Arabic and Hebrew
without the server knowing the reader's locale. `all`, `horizontal` and `vertical` are shorthands,
and an explicit side always overrides the shorthand that would otherwise set it — JSON guarantees
no key order, so `{ "horizontal": 16, "start": 0 }` reads the same either way round.

**Padding means one of two things, depending on the component.** On `container`, `card` and `box`
it is layout padding, applied inside the scroll viewport — it scrolls away with the content. On
`lazy_row` and `lazy_column` it is content padding, which stays put while items scroll edge to
edge. For a chip strip or a carousel that must keep a fixed inset at both ends, `lazy_row` is the
component you want.

**Name icons, don't draw them.** A `button` takes an optional `icon`, and there is a standalone
`icon` component. Both name a glyph that the app's `HeimIconProvider` resolves, so the same payload
renders Material symbols in one app and brand assets in another. Prefer this to an emoji in a
label: an emoji is announced by screen readers, renders differently on every OS version, and
cannot take the button's content colour.

**`visible_if` is presentation, not authorization.** The payload already reached the device.
Never use it to hide data the user must not see.

**Form endpoints are origin-locked.** The session token travels with a submission, so an absolute
URL must be HTTPS and match the `baseUrl` origin, or a host listed in
`HeimConfig.allowedSubmitHosts`. Relative paths always work.

## Keeping this file honest

Every example here is verified twice on each build:

1. It validates against the schema (JSON Schema 2020-12).
2. It parses and maps through the real SDK pipeline — `HeimSchemaExamplesTest`.

A schema that documents a shape the client cannot render is worse than no schema at all: a
backend team would build against it, pass their own CI, and still ship a broken screen. The
second check is what prevents that.

## Versioning

This is a published contract. Adding a **required** field breaks every backend already emitting
payloads. New fields must be optional with a default — the same discipline the DTOs follow, which
is why a `text` component missing its `text` field degrades instead of failing the screen.
