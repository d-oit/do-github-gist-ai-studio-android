# Do Gist Hub — Codebase Improvement Plan

**Status:** Approved by user (full cleanup, full state hoist to ViewModel).
**Scope:** All four tiers — compliance, correctness, code quality, quality gates/security/docs.
**Constraint reminder:** No Hilt annotations, manual constructor injection in `DoGistHubApp.kt`, no UI-triggered sync/remote calls, no `androidTest/`, max 600 LOC/file, no magic numbers, `testTag` + 48dp targets on interactive elements, `collectAsStateWithLifecycle()`.

Each phase should end at a green `./harness.sh check` / `build` / `test`.

---

## Phase 1 — Tier 1: Compliance violations (non-negotiable)

1. **Strip dead Hilt annotations** (Hilt plugin is never applied in `app/build.gradle.kts`, so all are inert but violate AGENTS.md Rule 1). Remove `@Inject`, `@Singleton`, `@Module`, `@Provides`, `@InstallIn`, `@ApplicationContext` + dagger/javax imports from:
   - `data/repository/GistRepository.kt:16-22`
   - `data/local/pref/ConfigPrefs.kt:9-26`
   - `data/remote/interceptor/GitHubAuthInterceptor.kt:4-14`
   - `di/NetworkModule.kt:7-57`
   - `di/StorageModule.kt:7-27`
   Keep the manual `provide*` object functions; they are wired in `DoGistHubApp.kt`.

2. **Kill the global singleton** (`DoGistHubApp.kt:46,83-86` `companion object { lateinit var instance }`):
   - Remove the `instance` companion object.
   - Add a manual `WorkerFactory` (`GistSyncWorkerFactory`) that injects `GistRepository` into `GistSyncWorker`.
   - In `DoGistHubApp.onCreate`, initialize WorkManager with this factory (`WorkManager.initialize(this, Configuration.Builder().setWorkerFactory(factory).build())`) and disable default WorkManager auto-init in `AndroidManifest.xml` (remove `WorkManagerInitializer` via `tools:node="remove"`).
   - Replace `GistSyncWorker.kt:26` `DoGistHubApp.instance.repository` with the injected `repository` constructor param.

3. **Fix UI bypassing repository + sync state machine** — `ui/screens/CreateGistScreen.kt:237` calls `viewModel.gistDao.upsertGistWithFiles(...)` directly. Replace with `viewModel.createGist(...)` so `isLocalOnly=true` transition is honored (Rules 2 & 3).

4. **Fix UI-triggered remote API** — `ui/components/GistPreviewDialog.kt:90-145` `LaunchedEffect` calls `getRemoteGistDetails`/`getRemoteGistRevision` on the main context. Move these into `GistViewModel` (suspend fns exposed via `StateFlow`) and collect in the composable.

5. **Remove instrumented tests** (Rule 4):
   - Delete `app/src/androidTest/java/com/example/ExampleInstrumentedTest.kt`.
   - Remove `testInstrumentationRunner` (`app/build.gradle.kts:28`) and `androidTestImplementation` block (`:229-233`).
   - Remove `connectedAndroidTest` branch in `harness.sh:123-133`.

---

## Phase 2 — Tier 2: Correctness bugs

6. **Preserve tags on draft→GitHub sync** (`GistRepository.kt:417-426`): read `tags` from the existing draft *before* `deleteGistById`, pass into `saveResponseToDb`; replace unsafe `response.id!!` (`:412`) with a null-safe check — if `response.id` is null, keep the draft and surface a failure instead of silently losing content (`:504` early-return).

7. **UTC timestamps** (`:219`,`:267`): replace `SimpleDateFormat("…'Z'")` (device-local, mislabeled UTC) with `java.time.Instant.now()` / `DateTimeFormatter.ISO_INSTANT`. Centralize format string as a `const val`.

8. **Real worker retry path**: let `syncWithGitHub` propagate per-item failures (return granular `Result`s) so `GistSyncWorker.kt:44-47` `Result.retry()` is reachable; use `Result.failure()` only for permanent (token) errors. Replace fragile `errorMsg.contains("token")` (`:38`).

9. **Soft-delete filter** (`data/local/dao/GistDao.kt:21-26`): add `isDeleted = 0` to `observeGistById`/`getGistById`.

10. **Preserve local star divergence**: in `fetchFromRemote`/`fetchFromRemotePaginated`, only overwrite `isStarred` from remote when `!isStarredDirty`.

11. **Deduplicate**: extract `private suspend fun fetchAndPersist(page, perPage)` shared by `fetchFromRemote`/`fetchFromRemotePaginated` (`:105-209`); extract `private fun requireTokenConfigured(): Result<Unit>` for the 7× token-empty guard (`:48-375`).

---

## Phase 3 — Tier 3: Code quality

12. **Refactor `DraftEditorDialog.kt` (659 LOC → <600)**: extract `FileEditorCard`, `AutoSaveBanner`, `TagInputRow`, `QualityAssistantSection` into `ui/components/draft/`.

13. **Centralize magic numbers**: add dimension/`Shape` tokens in `ui/theme/Theme.kt` (`TouchTarget = 48.dp`, spacing, corner radii) + semantic colors in `ui/theme/Color.kt` (`SuccessGreen`, `StarAmber`, `CodeSurface`, etc.). Replace inline `dp`/`sp`/`Color(0x…)` across all screens/components (e.g. `GistCard.kt:147,200`, `DiffViews.kt:39-222`, `CreateGistScreen.kt:180`). Enable `MagicNumber` in `config/detekt/detekt.yml:13`.

14. **`collectAsStateWithLifecycle()`**: swap every `collectAsState()` in UI for `collectAsStateWithLifecycle()` (import `androidx.lifecycle.compose`).

15. **Full state hoist to `GistViewModel`**: move editor/form state out of `GistHubAppScreen.kt:82-104`, `CreateGistScreen.kt:53-59`, `DraftEditorDialog.kt:100-178`, `GitHubMarkdownEditor.kt:65-68` into `GistViewModel` as read-only `StateFlow`s; screens become state-free.

16. **Add missing `testTag`** (snake_case) to all interactive elements: `GistCard.kt:262,270`, `GistPreviewDialog.kt:180,195-212,294,308,452`, `GistRevisionViews.kt:57,124,169-183`, `GistAiAssistantCardView.kt:74,157`, `DraftEditorDialog.kt:233,638,645,596,622`, `GistHubAppScreen.kt:249`, `ConfigScreen.kt:505-514`, `VaultScreen.kt:154-191`.

17. **Fix <48dp touch targets**: `GistDetailScreen.kt:307,320`, `GistPreviewDialog.kt:296,310`, `ContentQualityAssistantView.kt:78,177`, `GistAiAssistantCardView.kt:165,208,229` — use `Modifier.minimumInteractiveComponentSize()` or `height(48.dp)`.

18. **Add lazy-list `key`s** (stable ids): `GistDetailScreen.kt:265`, `GistPreviewDialog.kt:259`, `GistRevisionViews.kt:51,208`, `HomeScreen.kt:153`.

---

## Phase 4 — Tier 4: Quality gates, security, docs

19. **Add `check` subcommand to `harness.sh`** (format-check → lint → unit) — AGENTS.md:41 references it but it doesn't exist.

20. **Enforce detekt**: wire `detekt` into `harness.sh` + `.github/workflows/ci.yml`; set `MagicNumber: active: true` in `config/detekt/detekt.yml`.

21. **Token-leak fix**: guard `HttpLoggingInterceptor` with `BuildConfig.DEBUG` + `Level.BASIC` (`di/NetworkModule.kt:34`); wire `core/security/PrivacySanitizer` into logging/error paths so tokens/headers are redacted (SPEC §226).

22. **CredentialStore**: either implement the class referenced in `TASK.md:159` (extract from `ConfigPrefs`) or update `TASK.md` to reflect `ConfigPrefs` as the store; remove silent plaintext fallback or make it explicitly logged/acceptable.

23. **README fixes**: correct the missing `.github/workflows/codacy.yml` claim  - remove codacy badge

24. **Test coverage** (Robolectric/JVM only): add unit tests for `GistSyncWorker` (with injected fake repo), `TokenVerificationState` state machine, `DoGistHubApp` reactive scheduling, `GistDao` (soft-delete, upsert, unsynced query), `PrivacySanitizer`, `GitHubAuthInterceptor`. Extract the duplicated fake `GitHubApiService` used by `GistAppE2ETest.kt`/`GistOfflineE2ETest.kt` into a shared fixture. Delete tautological/placeholder tests (`MacrobenchmarkScaffold.kt:35`, `ExampleUnitTest.kt:13`).

---

## Execution order
1. Phase 1 → `./harness.sh build` + `test`
2. Phase 2 → `./harness.sh test`
3. Phase 3 → `./harness.sh check` + `test` (+ Roborazzi record if layouts changed)
4. Phase 4 → `./harness.sh verify`

## Progress (as of implementation session)

**Done:**
- Phase 1 (Tier 1 compliance): all 5 items.
  - Removed dead Hilt annotations from `GistRepository`, `ConfigPrefs`, `GitHubAuthInterceptor`, `NetworkModule`, `StorageModule`; dropped `hilt.android`/`hilt.compiler` deps.
  - Removed `DoGistHubApp.instance` singleton; added `GistSyncWorkerFactory` + manual WorkManager init (manifest disables default initializer).
  - `CreateGistScreen` now routes through `viewModel.createGist` (no direct DAO write).
  - `GistPreviewDialog` remote calls moved into `GistViewModel` (`loadGistHistory`/`loadGistRevision`/`clearPreviewRevisionState`); composable only triggers ViewModel methods.
  - Deleted `app/src/androidTest/…/ExampleInstrumentedTest.kt`; removed `testInstrumentationRunner` + `androidTestImplementation` from `app/build.gradle.kts` and the `connectedAndroidTest` branch in `harness.sh`.
- Phase 2 (Tier 2 correctness): items 6 (preserve tags + null-safe id on draft sync), 7 (UTC timestamps via `java.time`), 8 (partial-sync now returns `Result.failure` so worker retry is reachable), 9 (soft-delete filter in `observeGistById`/`getGistById`), 11 (deduped `fetchFromRemote`/`fetchFromRemotePaginated` into `fetchAndPersistGists`). Item 10 reviewed — local star divergence is already preserved when `isStarredDirty`.
- Phase 4 quick wins: added `check` subcommand to `harness.sh`; guarded `HttpLoggingInterceptor` to `BASIC` (debug) / `NONE` (release) so the Bearer token is never logged.

- Phase 3 (Tier 3 code quality): done.
  - `DraftEditorDialog.kt` refactored 659 → 357 LOC (sub-composables under `ui/components/draft/`).
  - Magic numbers centralized in `ui/theme/Theme.kt` (`Dimens`) + semantic colors in `ui/theme/Color.kt`.
  - `collectAsStateWithLifecycle()` applied across all UI, including the previously-missed `ConfigScreen.kt` and `MainActivity.kt`.
  - Full app-level/editor state hoisted into `GistViewModel` StateFlows; screens are state-free.
  - `testTag`s, 48dp touch targets, and lazy-list `key`s applied across screens/components.
- Phase 3 follow-up (post-hoist regression): the state hoist pushed `GistViewModel.kt` to 779 LOC (over the 600 budget). Refactored back to **585 LOC** via API-preserving extraction — no screen changes:
  - `ui/viewmodel/GistBackupExporter.kt` (backup JSON serialization + URI write; also fixes the lingering `SimpleDateFormat` → true-UTC `java.time` inconsistency).
  - `ui/viewmodel/PreviewRevisionLoader.kt` (preview dialog history/revision remote state).
  - `ui/viewmodel/ConfigController.kt` (token/profile/theme config state + validation; message callbacks keep the VM the single message channel).
  - VM re-exposes each via passthrough `get()` properties / one-line delegators so `viewModel.token`, `viewModel.gistHistory`, `viewModel.loadGistHistory(...)`, etc. are unchanged.

**Doc reconciliation (this session):** `harness.sh` help text now lists `check` and the `test` line no longer references removed connected tests. `TASK.md` updated: §1 subcommand list includes `check`/`coverage`/`codacy`; §5 references secure storage in `ConfigPrefs` (no separate `CredentialStore` class exists); §12 reflects routing through `GistViewModel.createGist` → `GistRepository` (not a direct DAO write). `AGENTS.md` and `SPEC.md` unchanged (still accurate).

**Not yet done:** remaining Phase 4 — detekt enforcement + `MagicNumber` enable (risky without a compiler to catch new violations), `PrivacySanitizer` wiring, README fixes, test coverage for `GistSyncWorker`/`TokenVerificationState`/`DoGistHubApp`/`GistDao`/`PrivacySanitizer`/`GitHubAuthInterceptor`.


## Key risk areas
- WorkManager custom `WorkerFactory` requires disabling default auto-init in `AndroidManifest.xml` or `WorkManager.initialize` will throw "already initialized".
- Centralizing magic numbers touches nearly every UI file — do it behind `Theme.kt` tokens and run `./harness.sh check` (ktfmt) frequently to avoid reformat churn.
