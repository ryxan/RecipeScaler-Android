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
- Give every icon-only button a `contentDescription`; keep touch targets ≥48dp
- Declare dependencies in `gradle/libs.versions.toml`, not inline in build files
- Update `docs/plan.md` when a roadmap item ships: tick the checkbox, update the Progress table (status + commit SHAs), record spec decisions
- Put signing keys and AdMob IDs in git-ignored `local.properties` — release stays unsigned and falls back to Google's test ad IDs when they're absent
- Bump `VERSION_NAME` by hand in `version.properties` per release — `VERSION_CODE` auto-bumps after a successful `bundleRelease`

## DON'T

- Hardcode `versionCode`/`versionName` in `app/build.gradle.kts` — they come from `version.properties`
- Commit `google-services.json`, `local.properties`, or keystores — the google-services plugin only applies when the file exists
- Write unit tests for the `:app` release variant — they're disabled because they'd need the debug-only ui-test-manifest
