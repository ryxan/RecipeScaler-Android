# Rescale UX Implementation Plan

Source: `docs/Rescale-UX-Review.md`. Update this file as work completes: tick items, set status and add commit SHAs in the Progress table, and record decisions made along the way.

Verify each phase with `.\gradlew.bat test assembleDebug lint`.

## Progress

| Phase | Scope | Status | Commits |
|---|---|---|---|
| 1 | Quick wins and release blockers | In progress (1.1, 1.2 skipped; 1.3–1.7 implemented) | |
| 2 | Core flow: list to Scale View | Done | |
| 3 | Editor rework | Not started | |
| 4 | Cook mode and Settings | Not started | |
| 5 | Larger features (list, Quick scale, persistence) | Not started | |
| 6 | Adaptive layouts and visual polish | Not started | |
| 7 | User validation | Not started | |

Legend: `[x]` done, `[ ]` todo, `[-]` skipped.

---

## Phase 1: Quick wins and release blockers (~1 day)

- [-] 1.1 Gate the Settings Debug section on `BuildConfig.DEBUG` (`showDebug = true // TODO`). **Skipped — see Decisions.**
- [-] 1.2 Hide the "Watch a short ad ... Soon" paywall option. **Skipped.**
- [x] 1.3 Editor input flow
  - [x] `ImeAction.Next` on the qty field (moves focus to the row's name field)
  - [x] Pin `Next` outside the accessory bar's scroll row
  - [x] Hide fraction chips when the name field is focused
  - [-] Reorder chips by frequency (`½ ¼ ¾ ⅓ ⅔ ⅛ /`; units `cup Tbsp tsp g oz`) **Skipped - not a priority**
- [x] 1.4 Scale View copy and validation
  - [-] Relabel fields ("Original yield" / "Make") **Skipped - not a priority**
  - [x] `isError` and `supportingText` on the failing field (`ScaleMessage` now carries a `ScaleField`)
  - [x] Summary shows `×1 (input not read)` on parse failure
- [x] 1.5 Accessibility
  - [x] `liveRegion = Polite` on the summary line
  - [x] Content descriptions on Editor qty and name fields ("Quantity, ingredient 3")
  - [x] Warning icon on the needs-review state
- [x] 1.6 Dismissible one-line scale warning (tap to expand detail); "Show original" moved into a new overflow menu (hidden in cook mode)
- [x] 1.7 Editor header: rename "Name" to "Recipe title"; duplicate placeholder dropped, field gets a content description

Tests: extend the `*Content` Robolectric tests for the new labels and errors.

## Phase 2: Core flow: list to Scale View (a few days)

- [x] 2.1 `RescaleNavHost`: `onOpenRecipe` goes to Scale View; add Edit (pencil) action to Scale View
- [x] 2.2 Scale View top bar
  - [x] Merge Copy and Share into one Share menu (Share, Copy plain, Copy Markdown, Copy shopping list)
  - [x] Label the Cook entry
- [x] 2.3 Scale controls
  - [-] Multiplier chips `½× 1× 2× 3× 4×` **(skipped - not a priority)**
  - [x] Stepper (−/+)
  - [-] "Multiply by" vs "Make N servings" mode; hide servings mode when no yield **(skipped - not a priority)**
  - [x] Summary styled as a caption, not `titleMedium`
- [x] 2.4 Compact input block so results start high; keep last factor in-session when returning from Editor
- [x] 2.5 US | Metric control in Scale View overflow

Tests: nav and VM tests; chips set multiplier, stepper bounds, mode hiding.

## Phase 3: Editor rework (a few days)

- [ ] 3.1 Responsive ingredient row
  - [ ] Multi-line name (`maxLines = 3`)
  - [ ] Stack `[Qty][Unit]` over `[Name]` when `fontScale > 1.3` or width < ~400dp
  - [ ] ▾ icon on the unit picker
- [ ] 3.2 Remove "Edit ingredients" mode
  - [ ] Swipe-to-delete with Undo snackbar
  - [ ] Persistent drag handle with 48dp target
  - [ ] Remove auto-exit logic
- [ ] 3.3 Clear ingredients uses Undo instead of a dialog; Replace/Append dialog becomes a bottom sheet or stacked buttons
- [ ] 3.4 Yield row: "Makes [4] [servings]" exposing `yieldLabel`; example in error text
- [ ] 3.5 Scale FAB: standard disabled container, snackbar "Add an ingredient to scale", solid `primary`
- [ ] 3.6 Empty ingredient area with Scan, Paste, Type; scan sheet collapsed to one "Scan" plus "Choose existing photo"

Tests: swipe and undo, Clear and Undo, compact layout at large font scale. Check the Editor on device at 200% font.

## Phase 4: Cook mode and Settings (2-3 days)

- [ ] 4.1 Cook mode
  - [ ] Visible checkboxes; rows at least 56dp
  - [ ] Type sizes up about 2sp
  - [ ] Compact "×2 ▾" chip in header
  - [ ] Hint shown once
  - [ ] "Reset checks" action
  - [ ] Haptic on check-off
- [ ] 4.2 Settings
  - [ ] *Display style* first; disable Measurement system radio in Decimals and Keep-units modes with helper text
  - [ ] Live preview under Display
  - [ ] Segmented buttons for Theme and Measurement
  - [ ] One-line value pitch in Pro row for free users
- [ ] 4.3 Motion and results polish
  - [ ] Skip or shorten (≤150ms) `AnimatedContent` while typing
  - [ ] Fix quantity column jitter (min width, right-aligned, `softWrap = false`)
  - [ ] TalkBack spoken forms for fractions

## Phase 5: Larger features (1-2 weeks)

Touches the data layer: needs a Room migration and a migration test.

- [ ] 5.1 List improvements
  - [ ] Visible Quick scale entry (small FAB or card)
  - [ ] Sort by Recent and A-Z
  - [ ] Long-press menu (Duplicate, Delete)
  - [ ] "Last used ×N" on rows (`lastOpenedAt` / `lastFactor` fields)
- [ ] 5.2 Single-screen Quick scale (rows plus live result); reuses Phase 2 and 3 components
- [ ] 5.3 Paste-from-clipboard banner on new empty recipe
- [ ] 5.4 Remember last factor per recipe (shares persistence with 5.1)

## Phase 6: Adaptive layouts and visual polish (~1 week)

- [ ] 6.1 `WindowSizeClass`; cap content width to ~600dp on Scale View and Editor
- [ ] 6.2 Landscape Scale View in two columns; later, list-detail scaffold per `docs/plan.md`
- [ ] 6.3 Gradient cleanup (flat top bar, gradient on hero element only)
- [ ] 6.4 Migrate raw dp values to `Spacing` tokens
- [ ] 6.5 Verify tabular numerals and fraction glyphs on device

## Phase 7: User validation

- [ ] 7.1 Test with 5 people: double this recipe, add from photo/text, switch to grams, cook mode at arm's length
- [ ] 7.2 Repeat at largest system font size
- [ ] 7.3 Feed findings back into Phases 2-4

## Decisions

- 1.1 skipped: app is in closed testing and testers need the AI-scan presets (0/1/5/10/Reset) to test flows without burning the 10-scan lifetime limit. `showDebug` stays `true` for now — **must be revisited before public launch** ("Pretend Pro" and scan reset must not ship to production). Alternatives if needed later: tap-version-to-reveal, or a build flag enabled only for testing builds.
- 1.2 skipped by owner choice.
- 2.1: tapping a recipe now opens Scale View directly; the Editor is reached via a pencil action on Scale View. The Editor's Scale FAB pops back to the existing `Scale` entry when it's directly below (same recipe), so List → Scale → Editor → FAB doesn't stack duplicate Scale screens and the in-session factor is preserved.
- 2.2: Copy and Share merged into the ⋮ overflow (Share…, Copy plain, Copy Markdown, Copy shopping list) alongside "Show original" and the unit system — frees two top-bar slots for Edit + the labeled Cook entry. Cook is a text-labeled pill ("Cook"/"Done") rather than a bare icon.
- 2.3: the stepper steps the *factor* on a 0.5 grid clamped to [0.5, 20] (snaps off-grid values first), writes `serves × factor` back into "Scale to", and lives at the end of the servings-chip row so the block stays one line. An unreadable Serves resets to 1, matching the chips' behavior.
- 2.4: inputs write through to `SavedStateHandle` (`scaleServes`/`scaleScaleTo`), so the last factor survives returning from the Editor and process death — session-only by design.
- 2.5: overflow shows Match recipe / US / Metric only when `DisplayMode.KITCHEN` — in Decimals and Keep-units modes the setting has no effect (per review §4.2), so the control is hidden rather than silently inert.
