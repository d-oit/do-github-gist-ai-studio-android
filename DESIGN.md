# d.o.Gist Hub Design Tokens & Contrast Specification (2026 Best Practices)

This document establishes the architecture for design tokens, themes, and high-contrast styling inside **d.o.Gist Hub**. It outlines the transition from hardcoded color states to dynamic, theme-aware Material 3 (M3) semantic tokens to prevent low-contrast text and icons on dark backgrounds (complying with **WCAG 2.2 AA/AAA** and **APCA** guidelines).

---

## 1. Core Problem: Hardcoded Grays on OLED Dark Canvases

In early mobile layouts, neutral text and decorative symbols often relied on static colors (like `#35333A` for secondary body text or `#5A5761` for captions). While these grays deliver great contrast on a light canvas (`#FDFBFF`), they become completely unreadable when the application is switched to a dark or pitch-black OLED background (`#08080A`).

| Mode | Background | Static Gray (`#35333A`) | Contrast Ratio | WCAG Compliance |
| :--- | :--- | :--- | :--- | :--- |
| **Light Mode** | `#FDFBFF` (SlateBg) | `#35333A` (GraySecondary) | **14.1:1** | ✅ AAA Pass |
| **Dark Mode** | `#08080A` (DarkBg) | `#35333A` (GraySecondary) | **1.4:1** | ❌ Fail (Sub-threshold) |

### Solution: Dynamic Semantic Tokens
To resolve accessibility failures under dark theme configurations, the application avoids using hardcoded static gray palette constants (like `GraySecondary` and `GrayTertiary`) in Compose views. Instead, it utilizes **Material 3 Semantic Design Tokens** mapped directly to the active theme context (`MaterialTheme.colorScheme`).

---

## 2. 2026 Design Token Mapping Matrix

| Design Token Category | Compose Theme Access | Light Hex Value | Dark Hex Value | WCAG AA Contrast | Perceptual Role |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Primary Accent** | `MaterialTheme.colorScheme.primary` | `#4F378B` | `#EADDFF` | > 6.5:1 | Main branding, primary buttons, active tabs. |
| **On-Primary** | `MaterialTheme.colorScheme.onPrimary` | `#FFFFFF` | `#21005D` | > 4.5:1 | Text/icons on primary containers. |
| **Primary Container** | `MaterialTheme.colorScheme.primaryContainer`| `#EADDFF` | `#4F378B` | - | Secondary highlights, card headers. |
| **Canvas Background** | `MaterialTheme.colorScheme.background` | `#FBFBFC` | `#08080A` | - | Full screen backdrop, side sheets. |
| **On-Background** | `MaterialTheme.colorScheme.onBackground` | `#0E0E11` | `#FFFFFF` | > 19.1:1 | Primary headers, long-form content. |
| **Surface (Cards)** | `MaterialTheme.colorScheme.surface` | `#FFFFFF` | `#121214` | - | Dialog containers, card wrappers. |
| **On-Surface (High)** | `MaterialTheme.colorScheme.onSurface` | `#0E0E11` | `#FFFFFF` | > 21.1:1 | High-emphasis body, list items. |
| **On-Surface Variant** | `MaterialTheme.colorScheme.onSurfaceVariant`| `#35333A` | `#E2E1E6` | **> 7.0:1** | Secondary body, inactive states, description labels. |
| **Outline (Muted)** | `MaterialTheme.colorScheme.outline` | `#5A5761` | `#94919E` | **> 4.5:1** | Component borders, caption text, placeholder outlines. |

---

## 3. Jetpack Compose Integration Best Practices

### A. Referencing Theme-Aware Color Schemes
To allow seamless runtime switching and high-contrast scaling, always consume colors from the ambient `MaterialTheme.colorScheme` instead of custom global variables.

```kotlin
// ❌ BAD: Bypasses the theme, creating contrast crashes on dark background
Text(
    text = description,
    color = GraySecondary
)

//  GOOD: Dynamically maps to light/dark equivalents automatically
Text(
    text = description,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)
```

### B. Alpha Blending & Transparency
When implementing semi-transparent overlays or muted secondary text (e.g., line numbers in diff views), do not blend static grays. Apply alpha opacity directly to dynamic semantic tokens:

```kotlin
// ❌ BAD: Static gray alpha blended
color = GraySecondary.copy(alpha = 0.5f)

//  GOOD: Theme-aware variant text alpha blended
color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
```

### C. M3 Ripple and Interaction States
Button hover, focus, and press states should use the M3 dynamic ripple. Avoid hardcoded visual feedback bounds:
- Under **WCAG 2.2**, interactive inputs require a minimum focus indicator contrast of **3.0:1** against adjacent colors.
- Outlines of text fields should transition from `MaterialTheme.colorScheme.outline` (inactive) to `MaterialTheme.colorScheme.primary` (focused).

---

## 4. Accessibility Checklists

- [ ] **Touch Target Size**: Minimum `48.dp` height/width on all interactive surfaces.
- [ ] **Minimum Text Size**: Sub-captions are kept above `11.sp` to support high visual density without sacrificing readability.
- [ ] **TalkBack Screen Readers**: All icons and action buttons have a meaningful `contentDescription` or are marked decorative with `null`.
- [ ] **System Bars Alignment**: Fully supports edge-to-edge rendering by applying `WindowInsets.safeDrawing` or `.navigationBarsPadding()`.
