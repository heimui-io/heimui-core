# 🐾 HeimUI — Brand & Design System Guidelines

> **"Heimdall: The All-Seeing Guardian of Server-Driven UI"**
> Official visual identity and branding specifications for the **HeimUI Engine**.

---

## 📖 1. Brand Story & Concept

**HeimUI** takes its name and soul from **Heimdall**, the faithful Australian Cattle Dog (*Blue Heeler*), and the mythological Norse guardian of the Bifrost bridge between realms.

* **The Guardian Mascot:** Just as Heimdall guards the bridge, HeimUI acts as the rock-solid, cryptographic, fail-safe bridge between backend services and native Compose Multiplatform clients (Android & iOS).
* **Visual Metaphor:** The logo fuses Heimdall’s distinctive physical traits (pointed ears, black eye patches, white forehead blaze, tan eyebrow dots) with cybernetic circuitry, UI wireframe nodes, and glowing neon gradients.

---

## 🎨 2. Official Color Palette

| Token Name | HEX | RGB | HSL | Semantic Role |
| :--- | :--- | :--- | :--- | :--- |
| **Obsidian Dark** | `#0B0F19` | `11, 15, 25` | `223°, 39%, 7%` | Background Base / Canvas |
| **Slate Surface** | `#161D2F` | `22, 29, 47` | `223°, 36%, 14%` | Card / Container Background |
| **Electric Cyan** | `#00E5FF` | `0, 229, 255` | `186°, 100%, 50%` | Primary Accent / Bifrost Network / Active State |
| **Neon Violet** | `#A855F7` | `168, 85, 247` | `271°, 91%, 65%` | Secondary Accent / Compose Native Engine |
| **Deep Indigo** | `#4F46E5` | `79, 70, 229` | `243°, 75%, 59%` | Gradient Midtone / Structural Accents |
| **Guardian Tan** | `#D97706` | `217, 119, 6` | `32°, 95%, 44%` | Heimdall Brow Accent / Warnings / Badges |
| **Blaze White** | `#FFFFFF` | `255, 255, 255` | `0°, 0%, 100%` | High-contrast Foreground / Central Forehead Blaze |
| **Muted Smoke** | `#94A3B8` | `148, 163, 184`| `215°, 20%, 65%` | Secondary Text / Subtitles |

### Primary Brand Gradient
```css
background: linear-gradient(135deg, #00E5FF 0%, #4F46E5 50%, #A855F7 100%);
```

---

## 🔤 3. Typography Hierarchy

### A. Display & Headings: **Plus Jakarta Sans** / **Inter**
* **Usage:** App titles, marketing landing pages, headers, banners.
* **Weights:** `Bold (700)`, `SemiBold (600)`.
* **Letter Spacing:** `-0.02em` (Tight, modern).

### B. Body & UI Controls: **Inter** / **Geist Sans**
* **Usage:** Form fields, descriptions, buttons, dialog messages, table text.
* **Weights:** `Regular (400)`, `Medium (500)`.
* **Line Height:** `1.5` (150%).

### C. Code & Payloads: **JetBrains Mono** / **Fira Code**
* **Usage:** JSON schema previews, SDK code samples, terminal CLI logs.
* **Weights:** `Regular (400)`, `SemiBold (600)`.

---

## 📐 4. Logo Clear Space & Minimum Sizes

```
      ┌───────────────────────────────┐
      │              [ X ]            │
      │       ┌─────────────┐         │
[ X ] │       │   HEIMDALL  │   [ X ] │
      │       │     LOGO    │         │
      │       └─────────────┘         │
      │              [ X ]            │
      └───────────────────────────────┘
  Clear space [X] = 20% of logo width
```

* **Minimum Digital Size:**
  * App Icon / Favicon: `32 x 32 px`
  * Organization Avatar: `180 x 180 px`
  * Header Banner: `1280 x 720 px` (16:9 aspect ratio)
* **Contrast Requirement:** Always display on dark surfaces (`#0B0F19` or `#161D2F`) or inside dark container frames.

---

## 🚫 5. Brand Don'ts
* ❌ Do **not** stretch, skew, or distort the proportions of Heimdall's head.
* ❌ Do **not** alter the signature eye-patch / tan eyebrow dots.
* ❌ Do **not** place the neon logo on bright yellow or light gray backgrounds without a dark backing card.
* ❌ Do **not** add heavy drop-shadows that conflict with the clean cyber glow.

---

## 📂 6. Asset Catalog

* **Avatar (1:1):** `art/heimui-avatar.jpg`
* **Banner (16:9):** `art/heimui-banner.jpg`
* **Vector Logo Mark (SVG):** `art/heimui-logo.svg`
* **Vector Banner (SVG):** `art/heimui-banner.svg`
