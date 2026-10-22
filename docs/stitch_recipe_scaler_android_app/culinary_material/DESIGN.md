---
name: Culinary Material
colors:
  surface: '#fff8f6'
  surface-dim: '#f2d3cd'
  surface-bright: '#fff8f6'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#fff0ee'
  surface-container: '#ffe9e5'
  surface-container-high: '#ffe2dc'
  surface-container-highest: '#fbdcd5'
  on-surface: '#281714'
  on-surface-variant: '#58413d'
  inverse-surface: '#3f2c28'
  inverse-on-surface: '#ffede9'
  outline: '#8c716c'
  outline-variant: '#e0bfb9'
  surface-tint: '#aa3523'
  primary: '#9c2b1a'
  on-primary: '#ffffff'
  primary-container: '#bd432f'
  on-primary-container: '#ffeae7'
  inverse-primary: '#ffb4a6'
  secondary: '#765a2c'
  on-secondary: '#ffffff'
  secondary-container: '#fed79e'
  on-secondary-container: '#785c2e'
  tertiary: '#345e2e'
  on-tertiary: '#ffffff'
  tertiary-container: '#4c7744'
  on-tertiary-container: '#ccfdbe'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdad4'
  primary-fixed-dim: '#ffb4a6'
  on-primary-fixed: '#3f0300'
  on-primary-fixed-variant: '#891d0e'
  secondary-fixed: '#ffdeae'
  secondary-fixed-dim: '#e6c189'
  on-secondary-fixed: '#281900'
  on-secondary-fixed-variant: '#5c4216'
  tertiary-fixed: '#bff0b1'
  tertiary-fixed-dim: '#a4d397'
  on-tertiary-fixed: '#002201'
  on-tertiary-fixed-variant: '#275022'
  background: '#fff8f6'
  on-background: '#281714'
  surface-variant: '#fbdcd5'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 57px
    fontWeight: '400'
    lineHeight: 64px
    letterSpacing: -0.25px
  display-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 45px
    fontWeight: '400'
    lineHeight: 52px
    letterSpacing: 0px
  display-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 36px
    fontWeight: '400'
    lineHeight: 44px
    letterSpacing: 0px
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: 0px
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
    letterSpacing: 0px
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
    letterSpacing: 0px
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: 0px
  title-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: 0px
  title-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: 0.15px
  title-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.1px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.5px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.25px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0.4px
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.1px
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.5px
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.5px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-tablet: 1.5rem
  gutter-desktop: 1.5rem
  margin: 1rem
  margin-tablet: 1.5rem
  margin-desktop: 2rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system translates the utility, warmth, and precision of a modern culinary workstation into a native Android Material Design 3 (Material You) interface. The aesthetic balances utilitarian ergonomics with domestic comfort, evoking the tactile sensation of cast iron, glazed terracotta, honed stone, and butcher block. 

Designed for home cooks, bakers, and culinary planners who interact with their screens under fast-paced, high-distraction kitchen conditions. The interface prioritizes immediate legibility, effortless single-handed touch interactions, and clear affordances. High surface tonal contrast and deep, appetite-stimulating pigments ground the utility in warmth without descending into retro nostalgia or skeuomorphism.

## Colors

The palette is derived from kiln-fired terracotta, raw spices, garden herbs, and toasted grains, following strict Material 3 tonal role mapping:

- **Primary (`#BD432F` / Deep Persimmon):** Used for focal actions, FABs, active state indicators, and prominent progress states. Communicates heat, appetite, and decisive action.
- **Secondary (`#765A2C` / Toasted Umber):** Complements the primary with an organic, grounded tone. Applied to tonal buttons, filter chips, and secondary category badging.
- **Tertiary (`#3E6837` / Rosemary Green):** Serves as an organic counterpoint for dietary tags, positive nutritional markers, completed kitchen steps, and fresh inventory signals.
- **Neutral (`#8C736E` / Warm Stone Gray):** Forms the backbone of all surface layers (`surface`, `surface-dim`, `surface-container-lowest` through `highest`), infusing the UI with parchment-like warmth rather than clinical, stark whites or cold grays.

Ensure all text on primary, secondary, and container fills satisfies WCAG 2.1 AA (4.5:1 minimum) contrast ratios on physical OLED and LCD screens under ambient kitchen lighting.

## Typography

The typography implements the standard Material Design 3 type scale rendered in Plus Jakarta Sans. Its rounded geometry and clear aperture balance warm contemporary friendliness with the surgical legibility needed for timers, gram measurements, and rapid ingredient scanning.

- **Display & Headlines:** Used for recipe names, collection showcases, and cooking stage checkpoints.
- **Titles:** Delineate ingredient groups, step sequences, and sub-headers inside elevated cards.
- **Body:** Calibrated line heights prevent eye fatigue during sequential step reading from arm’s length.
- **Labels:** Rendered with semibold weights (`fontWeight: 600`) to guarantee legibility on small-format badges, timers, chip elements, and interactive control bars.

## Layout & Spacing

Layout adheres to standard Android responsive screen adaptations based on an 8dp grid rhythm:

- **Mobile (Compact, &lt;600dp):** 4-column fluid layout with 16dp margins and 16dp gutters. Content stacks vertically. Primary actions live in a persistent Bottom Navigation Bar or dynamic bottom-anchored Extended FAB.
- **Tablet & Foldable (Medium, 600dp–839dp):** 8-column layout with 24dp margins and 24dp gutters. Bottom navigation converts to a vertical Navigation Rail on the start edge. Cards reflow into a two-column staggered grid for kitchen browsing.
- **Desktop & Landscape Tablet (Expanded, &ge;840dp):** 12-column layout with 32dp margins and 24dp gutters. Supports split-screen cooking execution: fixed ingredient pane alongside an active, large-type procedural guide.

All touch targets adhere to a minimum envelope of 48×48dp, even when visual containers are more compact.

## Elevation & Depth

In alignment with Material Design 3, elevation is conveyed primarily through **tonal color shifts** rather than dense drop shadows. Higher surfaces receive higher warm tonal containers:

- **Level 0 (Base Canvas):** `surface` (default warm off-white).
- **Level 1 (Card Default / Inactive Surface):** `surface-container-low` with subtle, diffuse shadow tinted with the primary hue: `0px 1px 3px rgba(189, 67, 47, 0.08)`.
- **Level 2 (Active Cards / Modals):** `surface-container` with tinted shadow: `0px 2px 6px rgba(189, 67, 47, 0.12)`.
- **Level 3 (Floating Menus / Extended FABs):** `surface-container-high` with clear separation: `0px 4px 12px rgba(144, 42, 24, 0.16)`.
- **Level 4 & 5 (Dialogs / Date-Time Pickers):** `surface-container-highest` with concentrated ambient shadow: `0px 8px 24px rgba(144, 42, 24, 0.20)`.

Surfaces retain zero border strokes by default; separation is achieved cleanly through contrast between container color steps.

## Shapes

The design system embraces the expressive end of Material 3 geometric curvature:

- **Full/Pill Shape (9999px):** Used for Extended FABs, standard FABs, assistive chips, suggestion chips, active indicator pills on bottom navigation, and pill text buttons.
- **Extra Large (`rounded-3xl` / 28dp):** Recipe showcase cards, hero cards, dialogs, and bottom sheets.
- **Large (`rounded-2xl` / 16dp):** Standard recipe step cards, ingredient modules, image banners, and text input fields.
- **Medium (`rounded-xl` / 12dp):** Menus, snackbars, and segmented button clusters.
- **Small (`rounded-lg` / 8dp):** Nested badges, timers, and tooltips.

## Components

### Buttons & FAB
- **Extended Floating Action Button (FAB):** Dynamic pill shape with primary container fill (`#BD432F`) and on-primary content. Features an icon accompanied by `label-lg` text, collapsing gracefully to a circular 56dp FAB upon scroll.
- **Filled Button:** 40dp height, fully rounded pill (`rounded-full`), primary color fill with zero elevation, rising to Elevation Level 1 on press.
- **Tonal Button:** Secondary container fill with on-secondary-container text; used for non-destructive actions like "Add to Grocery List."
- **Outlined Button:** Transparent background, 1dp outline using `outline-variant` warm gray, pill-shaped.

### Chips
- **Filter & Input Chips:** 32dp height, 8dp corner radius (`rounded-lg`) or fully pill-shaped. Inactive chips use a 1dp outline with transparent background. Active filter chips fill with `secondary-container` and display a leading checkmark icon.

### Cards
- **Elevated Card:** `rounded-2xl` (16dp) or `rounded-3xl` (28dp). Uses `surface-container-low` background with ambient warm tinted drop shadow.
- **Filled Card:** Uses `surface-container-highest` with zero shadow, ideal for distinct ingredient lists inside recipe steps.
- **Outlined Card:** 1dp `outline-variant` boundary with neutral background, used for secondary reference info.

### Top App Bar & Navigation
- **Top App Bar:** Center-aligned or small format, adopting `surface` background that morphs into `surface-container` upon scroll. Employs standard Material 24dp icons (Navigation Drawer, Up Arrow, Overflow menu).
- **Bottom Navigation Bar:** 80dp tall container with active pill indicator in `secondary-container` surrounding the selected navigation icon, paired with `label-sm` persistent text.
- **Navigation Rail:** 80dp wide vertical anchor on the left side of expanded screen layouts, retaining the same active pill geometry as the bottom bar.

### Lists, Checkboxes & Controls
- **Lists:** Ingredient lines utilize 56dp minimum row height, trailing quantity labels, and soft dividing baselines.
- **Checkboxes & Radios:** Primary persimmon tint when checked, with smooth spring-physics scale animation. Minimum tap envelope of 48×48dp.
- **Text Inputs:** M3 filled style with warm stone container fill, 16dp top corner radius, active indicator line in primary persimmon, and floating labels adhering to `body-sm`.

### Culinary Utility Components
- **Cooking Mode Step Counter:** Sticky elevated bottom bar showing large step indicators (`title-lg`), dual primary buttons ("Previous", "Next Step"), and an integrated circular progress timer.
- **Ingredient Scaling Selector:** Segmented pill button allowing 1x, 2x, 4x recalculations with instantaneous layout shifts.