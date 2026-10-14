# Rescale — UI/UX Review

**Scope:** `ryxan/RecipeScaler-Android` (main, 22 commits). Screens reviewed: Recipe List, Editor, Scale View (incl. Cook mode), Settings, scan/paywall sheets, design tokens.
**Method:** Read the Compose source, `strings.xml`, design-system tokens, nav graph and `docs/plan.md`. I did **not** see rendered screenshots, so layout comments are inferred from code (dp values, modifiers). Verify the ones marked *(check on device)*.

---

## 1. Summary

The foundations are good: clean M3 theme, undo-instead-of-confirm for delete, autosave, TalkBack move up/down actions, tabular numerals for quantities, fraction/unit accessory bar, cook mode with keep-screen-on. The domain logic is clearly the strong part.

The UX problems are mostly about **the main job not being the shortest path**. Someone standing in a kitchen wants: *open recipe → see scaled amounts → cook*. Right now that path goes through an edit form, and the scaled results sit below a stack of controls.

### Top 8 changes (highest impact first)

| # | Change | Why | Effort |
|---|---|---|---|
| 1 | **Tapping a recipe opens Scale View, not the Editor** | Core job is scaling; currently list → Editor → tap Scale FAB (`RescaleNavHost`: `onOpenRecipe` → `Editor`) | S |
| 2 | **Put the multiplier first: ½× / 1× / 2× / 3× + stepper** on Scale View | Doubling/halving is the dominant use case; the "×" chips were removed and replaced with bare numbers `1 2 3 4 5 6 8 10…` that don't say what they are | S |
| 3 | **Make results the hero; compact the inputs** | Inputs + chips + summary + warning + toggle all sit above the ingredient list | M |
| 4 | **Fix Editor ingredient row** (name truncation, font-scale breakage, placeholder-only labels) | Biggest accessibility and data-entry problem in the app | M |
| 5 | **Remove "Edit ingredients" mode**; use swipe-to-delete + always-visible drag handle | A mode toggle on a screen that is already editable is confusing | M |
| 6 | **Cook mode: bigger type, visible checkboxes, keep a scale control** | Used at arm's length with messy hands | M |
| 7 | **Hide the Debug section in release** | `showDebug = true // TODO` in `SettingsScreen.kt` ships "Pretend Pro" and AI-scan reset to users | XS |
| 8 | **Surface Quick scale properly** | It's the fastest path in the app and it's an unlabeled ⚡ icon in the top bar | S |

---

## 2. Navigation and information architecture

**Current flow**
```
List ──tap──▶ Editor ──Scale FAB──▶ Scale View
List ──⚡──▶ Editor (title "Quick scale") ──Scale FAB──▶ Scale View
```

**Problems**
- **Tap on a recipe goes to the Editor.** Users who just want amounts at 2× see an edit form, a keyboard-ready field set, and need a second tap. That's the wrong default for 90% of opens.
- Quick scale lands in an editor with the first qty field focused (good), but the keyboard covers half the screen and the result is still a FAB tap away.
- Scale View has no way back to editing except Back → Editor, and it can't be reached *from* editing without the FAB. The two screens are really one mode switch.

**Recommended flow**
```
List ──tap──▶ Scale View (default)  ──Edit action──▶ Editor
List ──⚡ / FAB menu──▶ Quick scale  (editor + live result on one screen)
```
- Add an **Edit** (pencil) action to Scale View's top bar.
- The plan already proposes Editor + Scale side-by-side on wide screens (`NavigableListDetailPaneScaffold`). A compact version of the same idea for phones: Quick scale as a **single screen** — rows on top, live scaled result below (or a bottom sheet with the result). This removes the Editor → FAB → Scale hop entirely for the throwaway case.

---

## 3. Recipe List

**Working well:** search only appears when there's something to search; swipe-delete with Undo snackbar; `CustomAccessibilityAction` for Delete; empty state with a direct Quick scale action; FAB collapses on scroll.

| Issue | Recommendation |
|---|---|
| ⚡ Quick scale is an icon-only action beside Settings; no label, no tooltip | Make it a visible entry: a second small FAB above "New recipe", or a persistent "Quick scale" card/chip at the top of the list. Add a tooltip at minimum. |
| Row leading icon is a first-letter circle: decorative, no information | Replace with useful state, or drop it and use the space. Supporting line could show **last scaled** ("Last used ×2") |
| No sort/recent ordering | Default to **recently opened**; add a sort option (Recent / A–Z). Pinning/favorites if the list grows past ~15 |
| No Duplicate | Recipe variants (half-batch, gluten-free) are common. Add a long-press / overflow menu: Duplicate, Delete |
| Swipe-to-delete has no visible affordance | Fine with Undo, but add the long-press menu above so deletion isn't gesture-only for sighted users either |
| Search field is a full-width pill on every visit | OK. Consider `SearchBar` M3 component if you later search ingredient names too |

---

## 4. Scale View (the core screen)

This is the screen that matters most and it needs the most work.

### 4.1 Layout / hierarchy

Current vertical order: inputs row → chips row → error text → **summary** → warning card → "Show original" switch → **results**. On a 360×640dp phone, results start roughly 2/3 of the way down *(check on device)*, and the warning card pushes them further when scale > 4×.

**Proposed layout**
```
┌ Top bar: Title            ✎  ⛶cook  ⋮(Copy/Share) ┐
│  Scale   [ ½×  1×  2×  3×  4× ] [−] 2.5 [+]        │  ← one compact control block
│  Makes 10 servings  (original 4)                    │  ← summary, 1 line, secondary style
│  ⚠ Big jump — baking doesn't scale linearly  ✕      │  ← dismissible, collapsed to 1 line
│ ─────────────────────────────────────────────────  │
│  2 ⅔ cups      all-purpose flour                    │
│  1 tsp         baking soda                          │  ← results start high on screen
```
- **Chips:** show the multiplier (`½×`, `2×`), not bare integers. The current `1 2 3 4 5 6 8 10 12 16 20 24` are *target servings*, but with no unit they read as multipliers. Offer both modes, clearly labeled: **Multiply by** vs **Make N servings**. Default to multiply.
- **Stepper:** add − / + buttons around the scale value. Typing on a decimal keypad to go from 2 to 3 is slow.
- **Fractional input:** the `Serves` field accepts `½` (parser supports it) but uses `KeyboardType.Decimal` with no fraction affordance. Either reuse the Editor's fraction bar here or add ½ to the chip set.
- **Rename the two fields.** "Serves" / "Scale to (servings)" is ambiguous. Use **"Original yield"** and **"Make"**, or hide the original yield behind the summary line and only expose it on tap when it's unset.
- **When the recipe has no yield** (blank or unparseable), "scale to servings" is meaningless. Hide that mode and show only multiply.
- **Summary line** (`×2 · Makes 8 servings`) is styled `titleMedium`, the same as ingredient quantities. It should read as a header or caption, not compete with the ingredients.
- **"Show original amounts"** gets a full-width switch row. Move it into the overflow menu or make it a small toggle icon in the results header. It's a secondary option.
- **Warning card:** make it dismissible and one line by default ("Large batches don't scale perfectly" with a "Details" expand). The current paragraph is useful the first time and noise on every subsequent visit.

### 4.2 Results list

- **Quantity column jitter.** The quantity text is inside `AnimatedContent` + `animateContentSize`, so its width changes with content, and the ingredient names start at different x positions. Give the quantity a fixed/min width (right-aligned, tabular numerals) so names form a clean column. Alternatively, put quantity above name on narrow widths.
- **Quantity/name sizing:** quantity is `titleMedium`, name `bodyLarge`. For kitchen reading distance, bump both one step in cook mode (see 4.3).
- **Mixed-unit display** like `5 Tbsp 1 tsp` is correct but dense; make sure it never wraps mid-quantity (`softWrap = false` on the quantity, wrap the name instead).
- **Unit system toggle in context.** US ↔ Metric lives only in Settings. Converting is a *mid-recipe* need ("my scale is in grams"). Add a small segmented control (US | Metric) or an overflow item on Scale View.
- **Settings interplay:** in *Decimals* and *Keep recipe units* modes the Measurement system setting has no effect (per `plan.md`, decimal mode stays in recipe units). The user sees a US/Metric radio that silently does nothing. See §6.

### 4.3 Cook mode

Good bones (tap to strike, screen stays on, state survives process death). Gaps:

| Issue | Recommendation |
|---|---|
| Entry is an unlabeled `Checklist` icon in the top bar | Label it ("Cook") or use a text button; first-time users won't guess it. |
| No visible checkbox; completion is only strikethrough + dimmed color | Add a leading `Checkbox`/check circle. Strikethrough alone is a weak affordance and a weak state cue. |
| Row hit area is the content height only | `heightIn(min = 56.dp)` with vertical padding across the full row width. |
| Same type sizes as normal view | Increase quantity/name by ~2sp steps, more row spacing. Phone propped against a backsplash. |
| Inputs are hidden entirely | Keep a compact "×2 ▾" chip in the header so users can change the scale without leaving cook mode. |
| Hint text "Tap an ingredient to check it off…" shows every time | Show once, then rely on the visible checkboxes. |
| Checks cleared on exit | Fine, but consider a "Reset checks" action while inside. |

### 4.4 Errors and validation
- When input can't be read, the error is a red `Text` placed *below the chips*, not attached to the field. The text fields don't set `isError` or `supportingText`. Attach the message to the failing field (`isError = true`, `supportingText`) so sighted and TalkBack users know which field.
- The fallback ("list falls back to 1×") is silent apart from that message; the summary line should visibly say `×1` with a muted "(input not read)" so users don't mistake it for their value.

### 4.5 Top bar
Back + Cook + Copy ▾ + Share is four controls on a ~360dp bar with a long recipe title. Merge **Copy** and **Share** into one **Share** action with a menu: *Share…, Copy plain, Copy Markdown, Copy shopping list*. Add Edit (see §2) in the freed slot.

---

## 5. Editor

### 5.1 Ingredient row (most impactful fix here)

Current row: `[drag?] [Qty 88dp] [Unit button] [Name weight(1f)] [✕?]`, single-line fields.

- **Name truncation.** On a 360dp screen: 16dp padding ×2, qty 88, unit ~56–64, gaps → the name field is roughly 150–170dp. "extra virgin olive oil, divided" doesn't fit on one line, and users can't see what they typed. *(check on device)* Allow the name to be **multi-line** (`singleLine = false`, `maxLines = 2–3`).
- **Large font / display size.** At 200% font scale the three-across layout will break (qty digits clip, unit button label clips). The plan calls for testing to 200%; make the row responsive: if `fontScale > 1.3` or width < ~400dp, stack as `[Qty][Unit]` on line 1 and `[Name]` full-width on line 2.
- **Placeholder-only labels.** "Qty" and "Ingredient" are placeholders, so they disappear once text is entered and aren't reliable accessible names. Add `semantics { contentDescription }` such as "Quantity, ingredient 3" / "Ingredient name, row 3", or add a one-time column header row above the list.
- **IME flow.** The qty field has no `imeAction`; only the name field has `Next`. After typing a quantity, the keyboard action doesn't move to the name field. Set `ImeAction.Next` on qty so Qty → Name is one key press (unit is inferred or set via the bar).
- **Unit control** is an `OutlinedButton` that looks like a text field neighbor but behaves like a dropdown. Add a trailing ▾ icon so it reads as a picker.
- **Needs-review state** is communicated by tertiary-colored text only ("Check this line"). Add a ⚠ icon, so it isn't color-only.

### 5.2 Edit mode
`Edit ingredients` (pencil → ✓) toggles drag handles and ✕ buttons. But the screen is already fully editable, so "edit" mode really means "reorder/delete mode", and the name doesn't say that.
- Replace with **swipe-to-delete** on rows (same pattern + Undo as the list) and a **persistent drag handle** at the row's leading edge (or long-press-to-drag).
- If you keep the mode, rename it **Reorder / Remove** and show the ✕ and handle together with 48dp targets. The drag handle icon is a bare 24dp `Icon` with `.padding(top = 16.dp)` + `draggableHandle()`, which is under the 48dp minimum.
- Dropping it frees a top-bar slot and removes the auto-exit logic when the last row is removed.

### 5.3 Accessory bar
- Seven fraction chips (`½ ⅓ ¼ ¾ ⅔ ⅛ /`) + five unit chips + `Next` in one horizontal scroll. **`Next` is the last item in the scroll row**, so it's off-screen on most phones. Pin `Next` (or ✓ Done) to the end, outside the scroll container.
- When the **name** field is focused, fraction chips stay visible but disabled (greyed). Hide them in that state and show only units, or show nothing. Disabled chips look broken.
- Order chips by frequency: `½ ¼ ¾ ⅓ ⅔ ⅛ /`; units `cup Tbsp tsp g oz`. (Consider `ml`/`kg` when Metric is the chosen system.)

### 5.4 Header fields
- Section header **"Name"** over the recipe title is ambiguous next to the ingredient "Ingredient" name field. Use **"Recipe title"** (and drop the duplicate placeholder).
- The yield field is a 120dp numeric box under a header "Serves". The data model has `yieldLabel` ("cookies", "servings") and the list shows it, but the editor doesn't expose it. Use **"Makes [ 4 ] [ servings ]"** on one row so "24 cookies" works.
- Yield error text "Can't read '%1$s'" is fine; add an example ("Try 4, ½ or 4–6").

### 5.5 Scale FAB and state
- The FAB uses `alpha(0.5f)` when disabled. On a gradient that's about **2:1 contrast** against the background for the label *(estimated)*, so it reads as washed out rather than disabled, and gives no reason. Use the standard disabled container, and show "Add an ingredient to scale" on tap (snackbar) so it isn't a dead control.
- Gradient top bar **and** gradient FAB share the same colors, so the FAB doesn't stand out. Keep the gradient on the bar and use `primary` (solid) for the FAB, or vice versa. Reserve the strongest color for the primary action.

### 5.6 Import / scan
- The scan sheet lists *Scan document*, *Take photo*, *Choose from gallery*, plus an **AI switch**. The ML Kit document scanner already includes camera capture and gallery import (`setGalleryImportAllowed(true)`), so three entries are mostly redundant. Offer **one "Scan"** primary with "Choose existing photo" as secondary.
- Importing is split across top-bar scan icon, overflow "Paste recipe text", and "Add ingredient". For a **new, empty** recipe, show a clear empty state in the ingredient area: **Scan · Paste · Type**. Discoverability of Paste is currently low.
- **Replace/Append** import confirmation is a dialog with both actions in the `confirmButton` slot. Use a bottom sheet with two full-width options, or stack the buttons, so Replace (destructive) and Append aren't adjacent text buttons.
- **Clear ingredients** uses a confirm dialog while Delete uses Undo. Make Clear use Undo too for a consistent mental model (and one less dialog).
- **Paywall:** "Watch a short ad … Soon" shows an unavailable option. Hide it until it works. Keep the paywall to at most two choices (Pro, scan pack) with the recommended one preselected.

---

## 6. Settings

| Issue | Recommendation |
|---|---|
| **Measurement system** radio does nothing in *Decimals* / *Keep recipe units* modes | Disable it in those modes with helper text ("Applies to Kitchen fractions"), or reorder so *Display style* comes first. |
| Radio rows have no examples | Add a live preview under *Display*: "1⅓ cups · 2 Tbsp" vs "1.33 cup" vs "315 mL". |
| *Display* options are 3 radios, *Theme* 3 more, *Measurement* 3 more | Use **segmented buttons** for short exclusive choices (US/Metric/Match, System/Light/Dark). Cuts the screen length by a third. |
| Pro row is the first element | Fine commercially. For free users show one line of value ("Unlimited AI scans"); for Pro users move it lower. |
| **Debug section visible** (`showDebug = true // TODO`) | Gate on `BuildConfig.DEBUG` before any closed testing. "Pretend Pro" in prod is a revenue hole and a trust issue. |

---

## 7. Accessibility checklist

**Already good:** `contentDescription`s on icon buttons; `Role.Checkbox`/`RadioButton` on toggleables; move up/down + delete custom actions; 48dp radio rows; planned font-scale test; `selectableGroup` on groups.

**Contrast** (computed from your tokens, WCAG AA = 4.5:1 text)

| Pair | Ratio | Result |
|---|---|---|
| White on gradient start (light) `#1F7A4D` | 5.3:1 | Pass |
| White on gradient end (light) `#00796B` | 5.3:1 | Pass |
| White on gradient start (dark) `#0F5A3A` | 8.3:1 | Pass |
| Primary text on background (light) | 5.1:1 | Pass (tight) |
| Tertiary "Check this line" on surface (light) | 5.8:1 | Pass |
| Disabled Scale FAB label (50% alpha) | ~2:1 | **Fail** (disabled controls are exempt from WCAG, but this also hurts comprehension) |

**To do**
1. **Live-region the summary.** When scale changes, results update silently for TalkBack. Mark the summary line `liveRegion = Polite` (e.g. "×2, makes 8 servings").
2. **Quantity announcements.** Provide a spoken form for `⅔ cup` / `5 Tbsp 1 tsp` via `contentDescription` (the plan already lists this: TalkBack-friendly fractions).
3. **Field/error association** on Scale View (`isError`, `supportingText`).
4. **Row labels** in the Editor (see 5.1).
5. **Touch targets:** drag handle, row ✕ (OK at 48dp), accessory chips (check min height under large fonts), cook-mode rows.
6. **Color-only cues:** "needs review", cook-mode completion, error text. Add icon/shape.
7. **Font scale 200% pass** on Editor row, Scale input row, accessory bar, top bars with long titles.
8. **Reduce motion:** `AnimatedContent` on every quantity change flickers while typing a number. Keep it short (≤150ms) or skip it when the change comes from typing.
9. **Keyboard/D-pad / external keyboard:** `Tab` order Qty → Unit → Name on each row; unit button should be focusable and open with Enter/Space.

---

## 8. Speed of use

- **Fewer taps to the answer:** List → Scale directly (§2). Quick scale as a one-screen flow.
- **Remember last factor per recipe** (e.g. "you usually make this ×2") and open at it.
- **Number entry:** stepper for scale, fraction chips on every numeric field, ImeAction chain through each row.
- **Paste-first import:** on opening a *new* recipe, if the clipboard looks like a recipe (≥2 lines with quantities), offer "Paste from clipboard" as a banner instead of burying it in ⋮.
- **Haptics:** light tick on chip select / stepper (`HapticFeedbackType.SegmentTick`, or `TextHandleMove` as fallback) and a confirm on check-off in cook mode.
- **Result persistence:** returning to Scale from Editor should keep the last factor in-session (currently Serves edits are "session-only" by design, fine, but keep *Scale to* too).

---

## 9. Tablets, landscape, foldables

`docs/plan.md` defers adaptive layout, and there's no `WindowSizeClass` in `:app` yet. Minimum viable approach before Play Store:
1. **Cap content width** (~600dp, centered) on Scale View and Editor so fields don't stretch across a 10" screen.
2. **Landscape Scale View:** two columns: controls left, results right. A tablet leaning on a counter in landscape is a realistic cook-mode scenario.
3. Later: list-detail scaffold per the plan.

---

## 10. Visual design notes

- The **herb green + teal + citrus** palette is cohesive and passes contrast. Dynamic color off by default is a sensible brand choice.
- The **gradient** is applied to top bar, FAB, buttons and the empty-state badge. That's a lot of the same treatment; the FAB loses prominence against the bar (see 5.5). Consider flat `primary` for the top bar and keep the gradient for the one hero element (FAB / key button).
- **Typography:** quantities use tabular numerals (`tnum`), which is a good detail. Confirm the font supports `tnum` plus the vulgar-fraction glyphs (`½ ⅓ ⅔ ⅛`) consistently; some fonts fall back to a different face for fractions, which makes mixed quantities look uneven. *(check on device)*
- **Spacing:** tokens exist (`Spacing.xs…xxl`) but the screens use raw `8.dp` / `16.dp` / `88.dp` in many places. Migrate to tokens to keep density consistent.
- **Copy:** "Scale" is both a verb (FAB) and a noun/screen title; "Serves" vs "Makes" vs "servings"; "Name" header. Pick one vocabulary: **Original yield / Makes / Multiplier**.

---

## 11. Suggested implementation order

**Quick wins (≤ 1 day total)**
1. Gate Debug section on `BuildConfig.DEBUG`.
2. `ImeAction.Next` on qty field; pin `Next` outside accessory-bar scroll; hide fraction chips when name is focused.
3. Relabel Scale fields; add `isError`/`supportingText`.
4. `liveRegion` on the summary; accessible labels on Editor fields.
5. Dismissible/collapsed scale warning; move "Show original" into overflow.
6. Hide the "Soon" ad option.

**Medium (a few days)**
7. List tap → Scale View; Edit action on Scale View.
8. Multiplier chips (`½× 1× 2× 3×`) + stepper; compact input block.
9. Multi-line / responsive Editor row.
10. Swipe-to-delete on ingredient rows; remove edit mode.
11. Cook mode: checkboxes, larger type, compact scale chip.
12. Merge Copy/Share; Settings preview + disable no-op setting; segmented buttons.

**Larger**
13. Single-screen Quick scale.
14. Adaptive layouts (width cap → two-pane landscape → list-detail).
15. Sort/recents, duplicate, last-used factor.

---

## 12. Things to validate with users (5 people, 15 minutes each)

1. "Double this recipe." (time to correct amounts; do they find the multiplier or retype servings?)
2. "Add the ingredients from this photo/text." (do they find scan vs paste?)
3. "Switch to grams." (do they find it in Settings?)
4. Cook mode: phone on the counter at arm's length, flour on hands. Can they check off items?
5. Repeat 1–3 at system font size "Largest".

---

*Reviewed from source only. If you share screenshots or a screen recording of the three main screens, I can turn this into annotated wireframes and tighten the layout recommendations in §4 and §5.*
