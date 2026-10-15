# Graph Report - RecipeScaler-Android  (2026-10-13)

## Corpus Check
- 121 files · ~64,705 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 21 file(s) not represented in the graph (top: .xml 11, .properties 4, (none) 2)

## Summary
- 1921 nodes · 4919 edges · 86 communities (41 shown, 45 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 126 edges (avg confidence: 0.86)
- Token cost: 16,500 input · 4,200 output

## Community Hubs (Navigation)
- OCR Recipe Parsing
- Kitchen Formatter Tests
- Play Billing Integration
- Kitchen Formatter Core
- AdMob Ads Module
- Recipe Database Layer
- Recipe Editor ViewModel
- AI Recipe DTO Tests
- Editor Screen UI
- Pro & Settings Screens
- Feature Flags DI
- Scale Screen UI
- Undo Delete Flow
- AI Paywall Dialog UI
- Fake Recipe Repository
- Ingredient Name Correction
- Recipe List Screen
- Recipe Scaling Tests
- Main Activity & Theme
- Design System Components
- AI Import Quota Tests
- Pro ViewModel
- Scale ViewModel
- Editor Scan Tests
- Scale ViewModel Tests
- Recipe List ViewModel
- Scaled Recipe Export
- Scan Importer Tests
- ViewModel Test Harness
- Data Layer DI Module
- Roadmap & Bug Docs
- Settings ViewModel
- Scaling Engine
- AI Import Quota
- AI Recipe Importer
- Scan Importer
- Settings Repository
- Navigation Graph
- Editor Content Tests
- Recipe List UI Tests
- Settings Content Tests
- Settings Repository Tests
- Editor Interaction & A11y
- Scale Screen Tests
- AI Ingredient Mapping
- Scan Test Fakes
- OCR Engine
- Pro Screen Tests
- Fake Settings Repository
- Purchase Flow
- Image Preprocessor Tests
- Image Preprocessing
- Scan Error Handling
- Coroutine Scopes DI
- Scan Import Concepts
- Main ViewModel Theme Tests
- Scan Source Sheet Tests
- Undo Delete Tests
- Repository DI Bindings
- Quantity Text Styling
- Editor Event Flow
- DB Migration Tests
- Ingredient Mapper Tests
- Recipe Domain Models
- Rewarded Ads
- Measurement System Defaults
- Editor Dialogs
- Domain Purity Rule
- On-Device AI Import
- CI Workflow
- App Module Testing
- Design System Module
- Release Signing

## God Nodes (most connected - your core abstractions)
1. `RecipeOcrParserTest` - 90 edges
2. `KitchenFormatterTest` - 84 edges
3. `EditorViewModel` - 77 edges
4. `FakeRecipeRepository` - 58 edges
5. `KitchenUnit` - 57 edges
6. `FakeProRepository` - 46 edges
7. `UndoDeleteController` - 42 edges
8. `Recipe` - 40 edges
9. `PlayBillingProRepository` - 32 edges
10. `testScanImporter()` - 30 edges

## Surprising Connections (you probably didn't know these)
- `GitHub Actions CI workflow (test, lint, assembleDebug)` --semantically_similar_to--> `Build & verify commands (gradlew test assembleDebug lint)`  [INFERRED] [semantically similar]
  .github/workflows/ci.yml → AGENTS.md
- `:core:domain module (pure Kotlin/JVM)` --semantically_similar_to--> `Pure-JVM :core:domain module (no Android imports)`  [INFERRED] [semantically similar]
  AGENTS.md → docs/plan.md
- `Scale by yield, by ingredient, or by factor` --semantically_similar_to--> `ScaleMode sealed interface (ByYield, ByIngredient, ByFactor)`  [INFERRED] [semantically similar]
  README.md → docs/plan.md
- `MainActivity` --references--> `AdConsentManager`  [EXTRACTED]
  app/src/main/java/com/ryxan/recipescaler/MainActivity.kt → core/data/src/main/java/com/ryxan/recipescaler/core/data/ads/AdConsentManager.kt
- `MainViewModel` --references--> `AppSettings`  [EXTRACTED]
  app/src/main/java/com/ryxan/recipescaler/MainViewModel.kt → core/domain/src/main/java/com/ryxan/recipescaler/core/domain/model/Settings.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Gradle module architecture (:app, :core:domain, :core:data, :core:designsystem)** — agents_module_app, agents_module_core_domain, agents_module_core_data, agents_module_core_designsystem [EXTRACTED 1.00]
- **Scaling engine domain components (parser, units, formatter, use case, modes)** — docs_plan_quantityparser, docs_plan_kitchenunit, docs_plan_kitchenformatter, docs_plan_scalerecipe, docs_plan_scalemode [EXTRACTED 1.00]
- **AI import and monetization flow (scan orchestration, quota, Pro billing, rewarded ads)** — docs_plan_scanimporter, docs_plan_ai_import, docs_plan_ai_quota, docs_plan_billing_pro, docs_plan_rewarded_ads [INFERRED 0.85]

## Communities (86 total, 45 thin omitted)

### Community 0 - "OCR Recipe Parsing"
Cohesion: 0.06
Nodes (5): ParsedIngredient, OcrLine, RawLine, RecipeOcrParser, RecipeOcrParserTest

### Community 2 - "Play Billing Integration"
Cohesion: 0.06
Nodes (11): Entitlement, entitlementOf(), PurchaseSnapshot, PurchaseStatus, OTHER, PENDING, PURCHASED, onResume() (+3 more)

### Community 3 - "Kitchen Formatter Core"
Cohesion: 0.06
Nodes (34): KitchenFormatter, toScaleItem(), KitchenUnit, CAN, CLOVE, CUP, DASH, EACH (+26 more)

### Community 4 - "AdMob Ads Module"
Cohesion: 0.06
Nodes (8): AdsModule, AdConfig, AdConsentManager, AdMobRewardedAds, RewardedAdLoadCallback, FullScreenContentCallback, AdMobRewardedAdsLogicTest, AiImportConfig

### Community 5 - "Recipe Database Layer"
Cohesion: 0.07
Nodes (7): IngredientEntity, RecipeEntity, RecipeSummaryTuple, RecipeWithIngredients, RecipeDao, RecipeDatabase, RoomRecipeRepositoryTest

### Community 6 - "Recipe Editor ViewModel"
Cohesion: 0.07
Nodes (3): ConfirmImport, EditorViewModel, ImportPrefill

### Community 7 - "AI Recipe DTO Tests"
Cohesion: 0.09
Nodes (6): AiRecipeDtoTest, splitYield(), YieldSplit, QuantityParser, SplitYieldTest, QuantityParserTest

### Community 8 - "Editor Screen UI"
Cohesion: 0.06
Nodes (15): AccessoryBar(), clickableEntry(), EditorCallbacks, EditorContent(), EditorDialogs(), EditorScreen(), FocusedField, IngredientRow() (+7 more)

### Community 9 - "Pro & Settings Screens"
Cohesion: 0.06
Nodes (8): ProContent(), ProScreen(), RadioRow(), SettingsCard(), SettingsContent(), SettingsScreen(), RescaleCard(), Spacing

### Community 10 - "Feature Flags DI"
Cohesion: 0.06
Nodes (3): FeatureFlagsModule, RescaleApplication, FirebaseAiRecipeImporter

### Community 11 - "Scale Screen UI"
Cohesion: 0.06
Nodes (6): messageText(), ScaleCallbacks, ScaleContent(), ScaledRow(), ScaleScreen(), SummaryLine()

### Community 12 - "Undo Delete Flow"
Cohesion: 0.19
Nodes (5): UndoDeleteController, EditorViewModelTest, FakeProRepository, testScanImporter(), NoopRewardedAds

### Community 13 - "AI Paywall Dialog UI"
Cohesion: 0.09
Nodes (11): AdOptionCard(), AiPaywallDialog(), BestValueChip(), FreeDivider(), Header(), IconBadge(), OptionCard(), ProCard() (+3 more)

### Community 14 - "Fake Recipe Repository"
Cohesion: 0.11
Nodes (8): toDomain(), toEntity(), RoomRecipeRepository, toIngredient(), Ingredient, Recipe, RecipeSummary, RecipeRepository

### Community 16 - "Recipe List Screen"
Cohesion: 0.08
Nodes (6): EmptyMessage(), RecipeListContent(), RecipeListScreen(), RecipeRow(), supportingText(), SwipeToDeleteItem()

### Community 17 - "Recipe Scaling Tests"
Cohesion: 0.23
Nodes (4): Failure, ScaleOutcome, Success, ScaleRecipeTest

### Community 19 - "Design System Components"
Cohesion: 0.13
Nodes (7): GradientButton(), GradientExtendedFab(), GradientTopAppBar(), GradientTopAppBarDarkPreview(), GradientTopAppBarLightPreview(), SectionHeader(), SectionHeaderDarkPreview()

### Community 21 - "Pro ViewModel"
Cohesion: 0.14
Nodes (3): ProUiState, ProViewModel, ProViewModelTest

### Community 22 - "Scale ViewModel"
Cohesion: 0.16
Nodes (9): CantRead, EnterAmountAboveZero, ScaleField, SCALE_TO, SERVES, ScaleInputs, ScaleMessage, ScaleUiState (+1 more)

### Community 23 - "Editor Scan Tests"
Cohesion: 0.22
Nodes (3): EditorViewModelScanTest, Fixture, OcrText

### Community 24 - "Scale ViewModel Tests"
Cohesion: 0.33
Nodes (3): ScaleViewModelTest, FakeRecipeRepository, FakeSettingsRepository

### Community 25 - "Recipe List ViewModel"
Cohesion: 0.15
Nodes (3): RecipeListUiState, RecipeListViewModel, RecipeListViewModelTest

### Community 26 - "Scaled Recipe Export"
Cohesion: 0.15
Nodes (7): ScaledIngredient, CopyFormat, MARKDOWN, PLAIN, SHOPPING_LIST, RecipeScalerCalculator, RecipeScalerCalculatorTest

### Community 30 - "Roadmap & Bug Docs"
Cohesion: 0.10
Nodes (22): Gradle module layout, Rescale app (com.ryxan.recipescaler), B1-B10 accuracy bugs (whole-number rounding in formatQuantity/formatOther), Editor autosave (500ms debounce straight to Room), DisplayMode enum (KITCHEN, KEEP_UNITS, DECIMAL), Ingredient data class (rawQuantity, quantity: Double?, unit, name, note), KitchenFormatter (measurement display), KitchenUnit enum (+14 more)

### Community 31 - "Settings ViewModel"
Cohesion: 0.13
Nodes (3): SettingsViewModel, MainActivity, MainViewModel

### Community 32 - "Scaling Engine"
Cohesion: 0.15
Nodes (14): DisplayPrefs, IngredientItem, ByFactor, ByIngredient, ByYield, IncompatibleUnits, IngredientHasNoQuantity, IngredientNotFound (+6 more)

### Community 35 - "AI Recipe Importer"
Cohesion: 0.17
Nodes (13): FakeAiRecipeImporter, AiImportResult, AiIngredientDto, AiRecipeDto, AiRecipeImporter, Blocked, Failed, NetworkError (+5 more)

### Community 36 - "Scan Importer"
Cohesion: 0.26
Nodes (5): Failed, Parsed, PaywallRequired, ScanImporter, ScanOutcome

### Community 37 - "Settings Repository"
Cohesion: 0.15
Nodes (6): DataStoreSettingsRepository, Keys, MeasurementSystem, MATCH_RECIPE, METRIC, US

### Community 38 - "Navigation Graph"
Cohesion: 0.15
Nodes (7): Editor, Pro, RecipeList, RescaleNavHost(), Scale, Settings, RescaleMotion

### Community 43 - "Editor Interaction & A11y"
Cohesion: 0.15
Nodes (17): Cook mode (strike-through rows, keep screen on), Scale View inputs: Serves + Scale to fields with 1x-10x chips, Screen flow: Recipe List -> Recipe Editor -> Scale View -> Settings, Accessibility checklist (liveRegion, spoken fractions, 48dp targets, 200% font pass), Cook mode, Gate Settings Debug section on BuildConfig.DEBUG, Recipe Editor screen, Editor ingredient row rework (multi-line name, font-scale responsive, real labels) (+9 more)

### Community 46 - "Scan Test Fakes"
Cohesion: 0.19
Nodes (3): FakeAiImportQuota, FakeImagePreprocessor, FakeOcrEngine

### Community 49 - "Fake Settings Repository"
Cohesion: 0.15
Nodes (5): DisplayMode, DECIMAL, KEEP_UNITS, KITCHEN, SettingsRepository

### Community 50 - "Purchase Flow"
Cohesion: 0.16
Nodes (6): Cancelled, Error, Launched, ProProductIds, ProRepository, PurchaseLaunch

### Community 53 - "Scan Error Handling"
Cohesion: 0.13
Nodes (9): scanErrorText(), ScanError, AI_BLOCKED, AI_FAILED, AI_LIMIT_REACHED, AI_NETWORK_ERROR, AI_RATE_LIMITED, NO_INGREDIENTS_FOUND (+1 more)

### Community 56 - "Scan Import Concepts"
Cohesion: 0.14
Nodes (14): AI recipe import (FirebaseAiRecipeImporter via Firebase AI Logic / Gemini), AI import quota (10 free lifetime + bonus credits + hidden Pro 30/day cap), Pro via Play Billing 8 (pro_monthly + pro_lifetime), ImagePreprocessor (image decode/EXIF/JPEG helpers moved out of the ViewModel), Paste recipe text import, RecipeOcrParser, RewardedAds (+5 scans; NoopRewardedAds stub until AdMob), ScanImporter orchestration (+6 more)

### Community 57 - "Main ViewModel Theme Tests"
Cohesion: 0.18
Nodes (5): MainViewModelTest, ThemeMode, DARK, LIGHT, SYSTEM

### Community 63 - "Editor Event Flow"
Cohesion: 0.22
Nodes (7): CreditsGranted, EditorEvent, IngredientCapReached, NavigateUp, NothingToImport, PurchaseError, ScanLooksPoor

### Community 66 - "Recipe Domain Models"
Cohesion: 0.25
Nodes (8): SavedRecipe, SavedRecipeIngredient, ScaleStatus, BLANK, RANGE_LOW_END, SCALED, UNPARSED, UnitOption

### Community 71 - "Editor Dialogs"
Cohesion: 0.33
Nodes (6): AiPaywall, ConfirmClear, EditorDialog, PasteText, ScanFailed, ScanSource

### Community 75 - "On-Device AI Import"
Cohesion: 0.67
Nodes (3): ImportBusy, AI, ON_DEVICE

## Knowledge Gaps
- **93 isolated node(s):** `QTY`, `NAME`, `ON_DEVICE`, `AI`, `PasteText` (+88 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 374 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **45 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `KitchenUnit` connect `Kitchen Formatter Core` to `OCR Recipe Parsing`, `Scaling Engine`, `Kitchen Formatter Tests`, `AdMob Ads Module`, `Recipe Database Layer`, `Recipe Editor ViewModel`, `Editor Screen UI`, `Undo Delete Flow`, `ViewModel Test Harness`, `Fake Recipe Repository`, `AI Ingredient Mapping`, `Recipe List ViewModel`, `Scaled Recipe Export`, `Undo Delete Tests`, `Editor IME Tests`?**
  _High betweenness centrality (0.081) - this node is a cross-community bridge._
- **Are the 18 inferred relationships involving `EditorViewModel` (e.g. with `.`a new empty recipe is never inserted`()` and `.`a recipe deleted elsewhere is not resurrected by the debounced save`()`) actually correct?**
  _`EditorViewModel` has 18 INFERRED edges - model-reasoned connections that need verification._
- **What connects `QTY`, `NAME`, `ON_DEVICE` to the rest of the system?**
  _93 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `OCR Recipe Parsing` be split into smaller, more focused modules?**
  _Cohesion score 0.06025641025641026 - nodes in this community are weakly interconnected._
- **Why does `DisplayPrefs` connect `Scaling Engine` to `Kitchen Formatter Tests`, `Kitchen Formatter Core`, `Settings Repository`, `Settings Content Tests`, `Fake Settings Repository`, `Recipe Scaling Tests`, `Scale ViewModel`?**
  _High betweenness centrality (0.052) - this node is a cross-community bridge._
- **Should `Kitchen Formatter Tests` be split into smaller, more focused modules?**
  _Cohesion score 0.06785225718194254 - nodes in this community are weakly interconnected._
- **Why does `Recipe` connect `Fake Recipe Repository` to `Compose UI Test Setup`, `Recipe Database Layer`, `Recipe Editor ViewModel`, `Undo Delete Controller`, `Recipe List UI Tests`, `Undo Delete Flow`, `Scale ViewModel`, `Scale ViewModel Tests`, `Recipe List ViewModel`, `Undo Delete Tests`, `ViewModel Test Harness`?**
  _High betweenness centrality (0.047) - this node is a cross-community bridge._