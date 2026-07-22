---
name: Do Gist Hub Minimalist Theme
version: alpha
colors:
  primary: "#6750A4"              # Active Purple accent
  on-primary: "#FFFFFF"           # Text on primary accent
  primary-container: "#EADDFF"    # Accent Container
  on-primary-container: "#21005D" # Text on primary container
  background: "#FDFBFF"           # Canvas background
  on-background: "#0E0E11"        # High-emphasis body text
  surface: "#FFFFFF"              # Surface card background
  on-surface: "#0E0E11"           # Text on surface card
  on-surface-variant: "#49454F"   # Secondary body text
  outline: "#5A5761"              # Component borders, muted captions
  error: "#B3261E"                # Error state
  on-error: "#FFFFFF"             # Text on error state
  error-container: "#F9DEDC"      # Local Draft indicator background
  on-error-container: "#410E0B"    # Local Draft indicator text
  pending-container: "#FFE0B2"    # Pending Sync indicator background
  on-pending-container: "#5D1F00"  # Pending Sync indicator text
typography:
  h1:
    fontFamily: sans-serif
    fontSize: 2.25rem
    fontWeight: 700
  body-md:
    fontFamily: sans-serif
    fontSize: 1rem
  label-caps:
    fontFamily: sans-serif
    fontSize: 0.75rem
rounded:
  sm: 4px
  md: 8px
  lg: 16px
spacing:
  sm: 8px
  md: 16px
  lg: 24px
components:
  canvas:
    backgroundColor: "{colors.background}"
    textColor: "{colors.on-background}"
  card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.md}"
    padding: 16px
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.md}"
    padding: 12px
  button-destructive:
    backgroundColor: "{colors.error}"
    textColor: "{colors.on-error}"
    rounded: "{rounded.md}"
    padding: 12px
  badge-synced:
    backgroundColor: "{colors.primary-container}"
    textColor: "{colors.on-primary-container}"
    rounded: "{rounded.sm}"
  badge-local:
    backgroundColor: "{colors.error-container}"
    textColor: "{colors.on-error-container}"
    rounded: "{rounded.sm}"
  badge-pending:
    backgroundColor: "{colors.pending-container}"
    textColor: "{colors.on-pending-container}"
    rounded: "{rounded.sm}"
  text-muted:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface-variant}"
  text-caption:
    backgroundColor: "{colors.background}"
    textColor: "{colors.outline}"
---

## Overview

Architectural Minimalism meets offline development productivity inside **d.o.Gist Hub**.
The application adopts a spacious, minimal, high-contrast palette aligned with Material Design 3 (M3) standards to deliver absolute visual clarity.

This specification outlines the dynamic, theme-aware semantic tokens used throughout the codebase. By avoiding static gray color states, we guarantee full compliance with **WCAG 2.2 AA/AAA** and **APCA** accessibility and contrast metrics across both light and dark OLED system canvases.

---

## Colors

The system uses dynamic semantic color tokens mapped directly to the ambient `MaterialTheme.colorScheme` instead of custom hardcoded values.

### The Contrast Problem of Hardcoded Grays
Static neutrals (such as `#35333A` for secondary text or `#5A5761` for borders) that look great on a light backdrop (`#FDFBFF`) crash down to sub-threshold contrast (e.g., **1.4:1**) on dark backgrounds (`#08080A`). 
To solve this, we map all colors dynamically:

- **Primary Accent (`#6750A4` / `{colors.primary}`):** Active Purple accent used for main branding, primary buttons, and active interactive elements.
- **Background (`#FDFBFF` / `{colors.background}`):** Clean off-white canvas backdrop providing maximum legibility.
- **Surface (`#FFFFFF` / `{colors.surface}`):** Raised containers, card wrappers, and dialog boxes.
- **On-Surface Variant (`#49454F` / `{colors.on-surface-variant}`):** Elegant medium slate gray for secondary labels and descriptions.
- **Outline (`#5A5761` / `{colors.outline}`):** Muted border lines, caption typography, and input outlines.

---

## Typography

Typography establishes structural hierarchies and high-density readability:

- **H1 Header (`h1`):** Large bold displays for key titles and greeting headers.
- **Body Medium (`body-md`):** High-legibility default sans-serif text for long-form content and gist details.
- **Label Caps (`label-caps`):** Tiny, wide-tracked labels for metadata and technical indicators.

---

## Layout

Spacing is governed strictly by the Material Design 8dp grid system to create elegant negative space:

- **Small Spacing (`spacing.sm` / 8dp):** Padding inside tight items, icon-to-text margins.
- **Medium Spacing (`spacing.md` / 16dp):** Standard padding for cards, lists, and screen boundaries.
- **Large Spacing (`spacing.lg` / 24dp):** Broad margins separating distinct semantic layout modules.

---

## Elevation & Depth

Components communicate focus and interactive hierarchy through standard Material 3 tonal elevation and surface colors:

- **Elevation 0 (Canvas):** Pure off-white background.
- **Elevation 1 (Cards):** Surface container color providing elevated contrast bounds without dark heavy shadows.

---

## Shapes

Rounded shapes provide a soft modern edge to coding blocks:

- **Small Rounded (`rounded.sm` / 4px):** Applied to badges, tag labels, and status pills.
- **Medium Rounded (`rounded.md` / 8px):** Applied to card panels, dialogue interfaces, and primary action buttons.
- **Large Rounded (`rounded.lg` / 16px):** Applied to bottom sheets and deep drawer overlays.

---

## Components

The semantic design token relationships are validated and executed in standard Material 3 layout containers:

- **Primary Canvas (`canvas`):** Integrates `{colors.background}` and `{colors.on-background}` to create the foundational edge-to-edge backdrop.
- **Card Wrapper (`card`):** Draws `{colors.surface}` and `{colors.on-surface}` with a smooth `{rounded.md}` boundary.
- **Action Buttons (`button-primary` / `button-destructive`):** Use solid primary/error colors with highly contrasting white labels.
- **Sync Badge Indicators (`badge-synced`, `badge-local`, `badge-pending`):** Low-saturation background containers paired with high-contrast text colors conforming to WCAG 2.2 contrast rules.

---

## Do's and Don'ts

### Do's
- **DO** always consume color parameters from `MaterialTheme.colorScheme` instead of hardcoded hex colors inside Compose screens.
- **DO** verify that the text-to-background contrast ratio is at least **4.5:1** for normal text and **3.0:1** for large titles.
- **DO** pair color tokens with dynamic transparency using `.copy(alpha = ...)` on semantic tokens instead of static gray tones.

### Don'ts
- **DON'T** use static global gray constants (like `GraySecondary` or `GrayTertiary`) for text on dynamic canvases.
- **DON'T** create layout dimensions or padding offsets outside the defined 8dp grid spacing tokens.
- **DON'T** implement custom color mappings that bypass the active system theme context.
