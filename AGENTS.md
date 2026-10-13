# Agent Notes

## Project

Rescale — a standalone Android recipe scaler. `applicationId`: `com.ryxan.recipescaler` (debug adds `.debug`). minSdk 26, compileSdk/targetSdk 36, Java 17. Hilt + Jetpack Compose (Material 3) + type-safe Navigation.

## Build & verify

- Tests: `.\gradlew.bat test`
- Current test baseline (0 failures, 0 skipped): **273** `:core:domain`, **38** per variant `:core:data`, **119** `:app` (debug only — release unit tests are disabled in `:app`)
- `:app` UI tests are Robolectric Compose tests against stateless `*Content` composables; VM tests use `app/src/test/.../testing` fakes (`FakeRecipeRepository`, `FakeSettingsRepository`, `MainDispatcherRule`).
- Debug APK: `.\gradlew.bat assembleDebug`
- Lint: `.\gradlew.bat lint`
- Full check: `.\gradlew.bat test assembleDebug lint`
- `:core:domain` purity check: `.\gradlew.bat :core:domain:dependencies --configuration runtimeClasspath` must show only kotlin-stdlib, kotlinx-coroutines-core, junit — no Android deps.

## Module layout

- `:app` — Android application: `RescaleApplication`, `MainActivity`, `NavHost`. Feature code lives under `com.ryxan.recipescaler.feature.*` inside this module for now.
- `:core:domain` — **plain Kotlin/JVM** module. `com.ryxan.recipescaler.core.domain`. **Must never depend on Android** — no `android.*`, `androidx.*`, or Android Gradle plugins here.
- `:core:data` — Android library (`com.ryxan.recipescaler.core.data`), Hilt wired; depends on `:core:domain`.
- `:core:designsystem` — Android library (`com.ryxan.recipescaler.core.designsystem`); `RescaleTheme` (M3, dynamic color on API 31+ with terracotta fallback) and `Typography`.

## Versioning & signing

- `version.properties` (root) holds `VERSION_CODE`/`VERSION_NAME`.
- Release signing is configured only when `RELEASE_*` keys in `local.properties` are set and the keystore file exists; otherwise release builds unsigned.

## Plan

Full roadmap: `docs/plan.md`. It's the source of truth for progress. When a phase or checklist item finishes, tick it, update the **Progress** table (status + commit SHAs), and record any spec decision made during implementation.
