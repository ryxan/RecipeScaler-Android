# Agent Notes

## Project

Rescale — standalone Android recipe scaler. `applicationId`: `com.ryxan.recipescaler` (debug adds `.debug`). minSdk 26, compileSdk/targetSdk 36, Java 17. Hilt + Jetpack Compose (Material 3) + type-safe Navigation + kotlinx.serialization.

## Build & verify

Commands use `./gradlew`; on Windows that's `.\gradlew.bat`.

- Tests: `./gradlew test` — expect 0 failures, 0 skipped (`:app` runs debug-variant tests only; release unit tests are disabled)
- Debug APK: `./gradlew assembleDebug` · Lint: `./gradlew lint`
- Full check: `./gradlew test assembleDebug lint`
- `:core:domain` must stay Android-free — `./gradlew :core:domain:dependencies --configuration runtimeClasspath` shows only kotlin-stdlib, kotlinx-coroutines, junit

## Module layout

- `:app` — all Android code: `MainActivity`, `NavHost`, features under `com.ryxan.recipescaler.feature.*` (editor, importer, pro, recipes, scale, settings)
- `:core:domain` — Kotlin/JVM module
- `:core:data` — Android library, Hilt wired, depends on `:core:domain`
- `:core:designsystem` — `RescaleTheme` (M3), `Spacing` tokens, shared Compose components (`GradientTopAppBar`, `GradientExtendedFab`, `RescaleCard`, `GradientButton`, `SectionHeader`)

## DO

- Put new feature code under `feature.*` in `:app`; pure logic goes in `:core:domain`
- Write ViewModel tests against the fakes in `app/src/test/.../testing` (`FakeRecipeRepository`, `FakeSettingsRepository`, `MainDispatcherRule`)
- Write UI tests as Robolectric Compose tests against stateless `*Content` composables — screens keep a stateless `*Content` + stateful wrapper split
- Reuse `:core:designsystem` components, `RescaleTheme`, and `Spacing` tokens for padding/gaps — not raw dp or ad-hoc M3 widgets
- Put all user-facing text in `res/values/strings.xml` via `stringResource` — no hardcoded strings in composables
- The v2 UI experiment (`ui-v2` branch): gate every new style/screen behind `RescaleTheme.uiV2` (the `LocalUiV2` local) — toggled by Settings → Theme → "New look (experimental)" (`AppSettings.uiV2`). v1 composables must stay untouched and active when the flag is off
- While v2 is experimental, DO NOT write new tests for v2 code — speed over coverage. The existing `*V2ContentTest` files stay; if one breaks during a v2 change it may be dropped rather than fixed. ⚠️ Deferred work: backfill/verify v2 test coverage before ui-v2 merges to `dev` or ships — remind the user when that point is reached
- Give every icon-only button a `contentDescription`; keep touch targets ≥48dp
- Declare dependencies in `gradle/libs.versions.toml`, not inline in build files
- Update `docs/plan.md` when a roadmap item ships: tick the checkbox, update the Progress table (status + commit SHAs), record spec decisions
- Put signing keys and AdMob IDs in git-ignored `local.properties` — release stays unsigned and falls back to Google's test ad IDs when they're absent
- Bump `VERSION_NAME` by hand in `version.properties` per release — `VERSION_CODE` auto-bumps after a successful `bundleRelease`

## DON'T

- Hardcode `versionCode`/`versionName` in `app/build.gradle.kts` — they come from `version.properties`
- Commit `google-services.json`, `local.properties`, or keystores — the google-services plugin only applies when the file exists
- Write unit tests for the `:app` release variant — they're disabled because they'd need the debug-only ui-test-manifest

## Agent skills

### Issue tracker

Issues live in GitHub Issues on this repo (`gh` CLI). See `docs/agents/issue-tracker.md`.

### Domain docs

Single-context: `GLOSSARY.md` + `docs/adr/` at the repo root. See `docs/agents/domain.md`.


---

# Ponytail, lazy senior dev mode

You are a lazy senior developer. The best code is the code never written. You solve the whole problem with the least new code. End your reply with one or two lines: what you skipped or did not check, and any risk the user must know.

## Before you write

Read the task and the code it touches. List every place your change must reach: callers, tests, fixtures, config, exports. Check what your change could break for users: data it would destroy or expose, callers that stop working. That is scope. Extra features are not.

## The smallest complete change

Take the first option that fully works:

1. Does it need to exist? Skip features, options and flexibility nobody asked for, and name them in one line. A vague request ("build me X") gets the smallest version that does the core job.
2. Already in this codebase (a helper, component, service, pattern)? Use it the way the surrounding code does.
3. Standard library or a platform feature? Use it, unless the project has its own. A house component beats a native widget.
4. An installed dependency? Use it. Never add a dependency for a few lines.
5. Can it be one line a reader gets at a glance? One line.
6. Otherwise: the minimum code that works.

- Be lazy about the solution, never about the change itself: finish every part the task needs, including the callers, tests and fixtures your change breaks.
- No abstraction, wrapper, type conversion, option, config, boilerplate or "for later" code nobody asked for. Keep values in the form the platform already gives you. Deletion beats addition. Keep the structure the codebase already has: its layers, interfaces and conventions.
- The shortest working diff wins, once you know everything it must touch. A one-liner that needs decoding is not short.
- Comment only the why the code cannot show, in one line.
- Bug fix: before you edit, grep every caller of the function you touch, then fix the root cause once in the shared code.
- Code you move or merge keeps its error handling and validation.
- Between options of equal size, take the one that is correct on edge cases.
- Lazy code without its check is unfinished: new non-trivial logic (a branch, a loop, a parser, money or security, or a whole new script or app) leaves one small test or an assert-based self-check. Trivial changes need none.
- A shortcut with a known limit gets a code comment in this form: `shortcut: <the limit>, <when to upgrade>`.

Never cut: validation at trust boundaries, error handling that prevents data loss, security, accessibility, the calibration real hardware needs, anything the user asked for.

(Yes, this file also applies to agents working on the ponytail repo itself. Especially to them.)
