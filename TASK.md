# Task Board: d.o.Gist Hub

An offline-first GitHub Gist client utilizing **Jetpack Compose**, a local **Room SQLite cache**, reactive **WorkManager synchronization**, and **manual constructor injection**. This board tracks the production-ready implementation milestones.

---

## 📂 1. Harness and Local Developer Quality Gate

- **Goal**: Establish a unified, POSIX-compliant script as the single local and CI quality gate.
- **Files expected to change**: `harness.sh`
- **Implementation checklist**:
  - [x] Create POSIX-compatible Bash script (`#!/usr/bin/env bash`, `set -euo pipefail`).
  - [x] Implement subcommands: `help`, `verify`, `check`, `unit`, `lint`, `format-check`, `build`, `coverage`, `codacy`, `test`, `clean`.
  - [x] Verify that `./gradlew` is used exclusively and its execution permission is resolved automatically.
  - [x] Handle step headings, error logging, and exit codes.
- **Verification command(s)**:
  - `chmod +x harness.sh`
  - `./harness.sh help`
- **Definition of done**: Script is fully executable, POSIX compliant, runs on macOS/Linux, and cleanly delegates to Gradle task wrappers.
- **Explicit non-goals**: We do not support running the harness on pure native Windows Command Prompt without a bash-compatible environment (like Git Bash or WSL).

---

## 📂 2. Gradle Build Reproducibility and Static Analysis

- **Goal**: Streamline Gradle builds, enable caching, and integrate static analysis tools (Spotless, Detekt, Android Lint).
- **Files expected to change**: `gradle.properties`, `build.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `config/detekt/detekt.yml`
- **Implementation checklist**:
  - [x] Optimize `gradle.properties` with caching (`org.gradle.caching=true`), parallel execution, and Kotlin incremental compilation (`kotlin.incremental=true`).
  - [x] Add Spotless Gradle plugin with `ktfmt` support.
  - [x] Add Detekt Gradle plugin with a practical configuration base.
  - [x] Configure Android Lint with fatal issues enabled for CI.
- **Verification command(s)**:
  - `./harness.sh format-check`
  - `./harness.sh lint`
- **Definition of done**: The static verification tasks succeed, and Spotless/Detekt can be executed locally and in CI.
- **Explicit non-goals**: Standardizing third-party library updates beyond standard catalog definition or introducing extreme lint rule sets that generate excessive noise.

---

## Task Group 3: Codacy Integration

### 3.1 JaCoCo coverage report configuration
**Goal:** Produce a JaCoCo XML report from unit tests that Codacy can consume.
**Files:** `app/build.gradle.kts`
**Checklist:**
- [x] Apply the `jacoco` Gradle plugin to the app module.
- [x] Pin `jacoco.toolVersion` to `"0.8.12"` or align with version catalog.
- [x] Register a `jacocoTestReportDebug` task depending on `testDebugUnitTest`.
- [x] Configure XML output; disable CSV; keep HTML for local inspection.
- [x] Exclude generated, data-binding, R, BuildConfig, and Manifest classes.
- [x] Verify execution data path matches AGP output for debug unit tests.
- [x] `./harness.sh coverage` completes without error.
**Verification:**
```bash
./harness.sh coverage
ls app/build/reports/jacoco/jacocoTestReportDebug/jacocoTestReportDebug.xml
```
**Done when:** XML file exists at the documented path after a clean run.
**Non-goals:** Connected/instrumented coverage; Kover integration.

### 3.2 `.codacy.yml` project configuration
**Goal:** Configure Codacy analysis scope to exclude generated and build artifacts.
**Files:** `.codacy.yml`
**Checklist:**
- [x] Exclude `app/build/`, `.gradle/`, `**/generated/`, `**/build/`.
- [x] Do not disable language detection or tool selection.
- [x] Validate file is well-formed YAML.
**Verification:** `python3 -c "import yaml, sys; yaml.safe_load(sys.stdin)" < .codacy.yml`
**Done when:** `.codacy.yml` exists, is valid YAML, and contains exclusions.

### 3.3 Codacy static-analysis CI workflow
**Goal:** Run Codacy static analysis on every PR and push using the official action,
and upload SARIF to GitHub Security.
**Files:** `.github/workflows/codacy.yml`
**Checklist:**
- [x] Triggers: `push` to `main`, `pull_request`, `workflow_dispatch`.
- [x] Uses `codacy/codacy-analysis-cli-action@v4` (GitHub-verified).
- [x] Outputs SARIF format.
- [x] Uploads SARIF via `github/codeql-action/upload-sarif@v3`.
- [x] Sets `max-allowed-issues: 2147483647` (informational; gate is Codacy PR status).
- [x] Grants `security-events: write` permission.
- [x] Concurrency group cancels obsolete in-progress runs.
- [x] Minimal permissions: `contents: read`, `security-events: write`, `actions: read`.
**Verification:** YAML is valid; referenced action versions resolve on GitHub Marketplace.
**Done when:** Workflow file exists and passes YAML lint.

### 3.4 Coverage upload CI step
**Goal:** Upload JaCoCo coverage to Codacy on every push and PR.
**Files:** `.github/workflows/ci.yml`
**Checklist:**
- [x] After unit-test step, add `./gradlew jacocoTestReportDebug` step.
- [x] Add `codacy/codacy-coverage-reporter-action@v1` step (GitHub-verified).
- [x] Provide `project-token: ${{ secrets.CODACY_PROJECT_TOKEN }}`.
- [x] Provide `coverage-reports:` pointing to the XML output path.
- [x] Make the step conditional: `if: ${{ secrets.CODACY_PROJECT_TOKEN != '' }}`
      so fork PRs skip gracefully without failing.
- [x] Token is not echoed, not in job-level `env:`, not in any log output.
**Verification:** CI workflow YAML is valid; step is present and conditional.
**Done when:** Step exists in `ci.yml` and is gated on secret presence.

### 3.5 `harness.sh` coverage and codacy subcommands
**Goal:** Local developer can generate and upload coverage without the CI workflow.
**Files:** `harness.sh`
**Checklist:**
- [x] `coverage` subcommand runs `jacocoTestReportDebug`.
- [x] `codacy` subcommand validates token is set, validates report exists, then
      calls the official Codacy Coverage Reporter bash script.
- [x] `verify` calls `coverage` + `codacy` only when `CODACY_PROJECT_TOKEN` is set.
- [x] Token value is never printed.
**Verification:** `./harness.sh help` lists all subcommands including `coverage` and `codacy`.
**Done when:** Both subcommands work end-to-end in a configured environment.

### 3.6 `.gitignore` and `.env.example` updates
**Goal:** Ensure Codacy artifacts and the project token are never committed.
**Files:** `.gitignore`, `.env.example`
**Checklist:**
- [x] `results.sarif` is git-ignored.
- [x] `.codacy-temp/` is git-ignored.
- [x] `CODACY_PROJECT_TOKEN=` placeholder is present in `.env.example` with an
      empty value and a comment directing to the Codacy dashboard.
- [x] Real token is never present in `.env.example`.
**Done when:** Entries exist in both files.

### 3.7 README Documentation Update
**Goal:** Document onboarding, local usage, repository structure, and remove obsolete references.
**Files:** `README.md`
**Checklist:**
- [x] Document repository structure and developer setup instructions using the unified `.env`.
- [x] Document how to execute local static analysis, auto-formatting, and unit tests using `harness.sh`.
- [x] Remove all obsolete Codacy mentions, coverage upload workflows, and grade badges.
**Done when:** README is clean, accurate, has no Codacy badges, and reflects the current repository structure.

---

## 📂 4. Safe Debug/Release Signing and Configuration

- **Goal**: Configure standard debug signing by default, and provide opt-in release signing via environment variables/Gradle properties.
- **Files expected to change**: `app/build.gradle.kts`, `.env.example`
- **Implementation checklist**:
  - [x] Ensure debug builds use standard debug Keystores without requiring manual edits.
  - [x] Implement opt-in release signing resolving from `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD`.
  - [x] Define safe placeholders in `.env.example`.
- **Verification command(s)**:
  - `./harness.sh build`
- **Definition of done**: Running standard build commands compiles a debug APK successfully without requiring release keys or manual code changes.
- **Explicit non-goals**: Storing actual keystore passwords or certificate assets in source code or committing private key files.

---

## 📂 5. Secure Credential Storage and Privacy Redaction

- **Goal**: Secure personal access tokens using Android Keystore-backed encryption, and sanitize sensitive information from application logs.
- **Files expected to change**: `ConfigPrefs` (secure credential storage), logging classes, view models, logout routines.
- **Implementation checklist**:
  - [x] Implement secure credential storage in `ConfigPrefs` utilizing MasterKey-based `EncryptedSharedPreferences` with safe fallback (one-time migration of any pre-existing plaintext values).
  - [x] Ensure Personal Access Token is redacted in `toString()`, logs, UI error displays, and exceptions.
  - [x] Ensure logout / configuration reset wipes all cached credentials, profile metadata, and Room tables completely.
- **Verification command(s)**:
  - `./harness.sh test`
- **Definition of done**: Unit tests confirm stored credentials cannot be leaked in logs or error payloads and logout fully erases all local state.
- **Explicit non-goals**: Supporting multiple active GitHub user profiles simultaneously.

---

## 📂 6. App State, Data Boundaries, and Error-State Modeling

- **Goal**: Define domain boundaries and model application UI states with robust error classifications.
- **Files expected to change**: Models, ViewModels, repository interfaces.
- **Implementation checklist**:
  - [x] Refine/implement `GistRepository` and underlying service/local interfaces to return domain-level models.
  - [x] Expose state as read-only, immutable `StateFlow` from ViewModels using `collectAsStateWithLifecycle()` in UI.
  - [x] Explicitly model state variations: Loading, Content, Empty, Offline cached content, Recoverable error, Authentication expired.
  - [x] Create user-safe error classification models.
- **Verification command(s)**:
  - `./harness.sh test`
- **Definition of done**: ViewModel transitions are fully tested, and Compose views reactively render the appropriate UI depending on state.
- **Explicit non-goals**: Splitting the existing monolithic module into multiple standalone Gradle submodules in this change set.

---

## 📂 7. Cache, Conditional Refresh, Pagination, and Offline Behavior

- **Goal**: Optimize cache hits, implement conditional refreshes using ETags, and support paginated list loading.
- **Files expected to change**: `GistRepository`, `GistDao`, entities, HTTP clients.
- **Implementation checklist**:
  - [x] Emit cached content immediately to UI, then fetch remote updates asynchronously.
  - [x] Support conditional requests with headers (ETag, Last-Modified) where applicable.
  - [x] Implement pagination with structured limits.
  - [x] Retain cached content and display warning banners on network timeout or connection failure.
- **Verification command(s)**:
  - `./harness.sh test`
- **Definition of done**: Cache emits immediately on startup, list loads more elements on scroll, and offline modes are gracefully handled.
- **Explicit non-goals**: Implementing complex peer-to-peer offline database merging.

---

## 📂 8. Performance Diagnostics, Baseline Profiles, and Benchmarks

- **Goal**: Scaffold Baseline Profile generation and macrobenchmark frameworks to optimize startup performance.
- **Files expected to change**: `app/build.gradle.kts`, `gradle/libs.versions.toml`, Baseline Profile module scaffolding.
- **Implementation checklist**:
  - [x] Enable StrictMode checks in debug builds to catch thread-policy violations.
  - [x] Scaffold Baseline Profile generator rules covering cold starts and main lists.
  - [x] Scaffold Macrobenchmark tests for startup timing diagnostics.
- **Verification command(s)**:
  - `./harness.sh verify` (Scaffold compiles cleanly)
- **Definition of done**: Scaffolding modules compile successfully on JVM unit tests, and instructions are clearly documented.
- **Explicit non-goals**: Full hardware-dependent device execution of macrobenchmarks (marked as "Scaffolded" since local emulators are unavailable).

---

## 📂 9. CI Pull-Request Gate

- **Goal**: Setup an automated GitHub Actions pipeline validating all pull requests before merge.
- **Files expected to change**: `.github/workflows/ci.yml`, `.github/workflows/_build.yml`
- **Implementation checklist**:
  - [x] Define triggering paths (`pull_request`, `push` to main).
  - [x] Run graduated pipeline checks: Format -> Lint -> Unit -> Build.
  - [x] Configure dependency caches to optimize CI duration.
  - [x] Extract shared build/verify pipeline into a DRY reusable workflow (`_build.yml`).
  - [x] Pin all GitHub Actions to full-length commit SHAs (e.g., checkout, setup-java, setup-android) to satisfy strict repository security policies.
- **Verification command(s)**:
  - CI pipeline execution on branch push.
- **Definition of done**: All tests, formatting rules, and lints must pass in CI environment.
- **Explicit non-goals**: Automatically applying auto-fixes or commits on behalf of the user in CI.

---

## 📂 10. Signed GitHub Release Workflow

- **Goal**: Automate secure release builds generating signed APKs and computing checksums.
- **Files expected to change**: `.github/workflows/release.yml`, `.github/workflows/cleanup-caches.yml`
- **Implementation checklist**:
  - [x] Configure trigger on tag matching `v*`.
  - [x] Map GitHub secrets to Gradle properties for secure signing.
  - [x] Build signed release APK and compute SHA-256 checksums.
  - [x] Optimize release workflow by bypassing redundant, duplicate Quality Gate verification steps.
  - [x] Implement a dedicated PR closed cache cleanup workflow (`cleanup-caches.yml`) to automatically purge stale caches and keep actions storage well under the 10 GB limit.
  - [x] Publish official GitHub Release attaching signed binaries and changelogs.
- **Verification command(s)**:
  - Trigger release action manually or via tag push.
- **Definition of done**: Tag pushes automatically yield an authenticated, checksum-secured production release.
- **Explicit non-goals**: Publishing directly to Google Play Console.

---

## 📂 11. Documentation, Tests, and Final Verification

- **Goal**: Maintain detailed setup documentation, ensure test coverage, and perform a full harness validation.
- **Files expected to change**: `README.md`, test directories.
- **Implementation checklist**:
  - [x] Document local setup steps and configuration requirements.
  - [x] List secure variables, formatting, lint, and unit-test execution tasks.
  - [x] Ensure 100% test coverage over core repository, mapping, state, and verification logic.
  - [x] Run final `./harness.sh verify` to assert quality.
- **Verification command(s)**:
  - `./harness.sh verify`
- **Definition of done**: Harness successfully passes all levels with green status indicators.
- **Explicit non-goals**: Eliminating all generic project documentation in favor of extensive architectural guides.

---

## 📂 12. Create Gist Composable Screen & Database Integration

- **Goal**: Implement a fully featured unified editor/creation dialog for creating new Gists and editing drafts, with rich markdown support, and persist them locally via Room `GistDao`.
- **Files expected to change**: `DraftEditorDialog.kt`, `GistHubAppScreen.kt`, `GistRepository.kt`, `GistViewModel.kt`
- **Implementation checklist**:
  - [x] Create a dedicated dialog `DraftEditorDialog.kt` with fields for description, public status, list of files (filenames and content), and rich markdown edit/preview capabilities.
  - [x] Integrate Material 3 input fields and clean validations.
  - [x] Assign `testTag` identifiers to all input fields, buttons, and switches for testing.
  - [x] Route the submit action through `GistViewModel.createGist(...)` -> `GistRepository` (which persists via `GistDao.upsertGistWithFiles(...)`), preserving the sync-state flags per AGENTS.md; screens never touch the DAO directly.
  - [x] Expose local-only sync flags (`isLocalOnly = true`) on database entries.
  - [x] Integrate the creation dialog seamlessly with navigation and entry points in `GistHubAppScreen.kt` for both creation and editing.
- **Verification command(s)**:
  - `./harness.sh verify`
- **Definition of done**: Selecting the "New Draft" floating action button or "New Local Draft" button opens the unified dialog with rich markdown rendering, validates inputs, and persists a local-only draft directly in the SQLite cache using Room.

---

## 📂 13. Global Sync Error Handling and UI Notifications

- **Goal**: Propagate background synchronization failures (network issues, API errors, authorization expirations) to the UI and display as user-friendly notifications (Snackbars) rather than failing silently.
- **Files expected to change**: `GistSyncWorker.kt`, `GistRepository.kt`, `SyncStatus.kt`, `SyncErrorHandler.kt`, `ConfigPrefs.kt`, `GistViewModel.kt`, `GistHubAppScreen.kt`
- **Implementation checklist**:
  - [x] Create `SyncErrorHandler.kt` to classify various `Throwable` and `HttpException` types into user-friendly strings, utilizing `PrivacySanitizer` to redact sensitive tokens.
  - [x] Create `SyncStatus.kt` (sealed interface) to model the sync state (Idle, Syncing, Success, Error).
  - [x] Update `ConfigPrefs.kt` to persist `lastSyncError` and `lastSyncTime` to maintain state across app restarts.
  - [x] Update `GistRepository.kt` to expose a `StateFlow<SyncStatus>` and initialize it from `ConfigPrefs` on startup.
  - [x] Update `GistSyncWorker.kt` to update `repository.syncStatus` and run classification logic on error.
  - [x] Update `GistViewModel` to expose `syncStatus` and handle dismissal of sync errors.
  - [x] Update `GistHubAppScreen.kt` to observe `syncStatus` and trigger Snackbars.
- **Verification command(s)**:
  - `./harness.sh verify`
- **Definition of done**: Sync states are fully tracked, classified on failure, and reactively trigger polished Material 3 Snackbar alerts on the main UI.

---

## 📂 14. Network Connection Monitoring & Auto Re-Sync Hook

- **Goal**: Implement a background service/hook that periodically checks for internet connectivity and automatically triggers a re-sync of pending local changes to GitHub when restored.
- **Files expected to change**: `NetworkConnectivityMonitor.kt`, `DoGistHubApp.kt`, `GistRepository.kt`
- **Implementation checklist**:
  - [x] Create `NetworkConnectivityMonitor.kt` to monitor internet connections using `ConnectivityManager.NetworkCallback` and run periodic background checks.
  - [x] Add `getUnsynchronizedGists()` to `GistRepository.kt` to check for unsaved local changes.
  - [x] Instantiate and start `NetworkConnectivityMonitor` in `DoGistHubApp.kt` on startup.
  - [x] Verify offline-to-online transitions trigger immediate background synchronization of local-only and dirty gists.
- **Verification command(s)**:
  - `./harness.sh test`
- **Definition of done**: Foreground or background changes in internet connectivity are reactively captured, and any pending offline mutations are pushed to GitHub seamlessly.

---

## 📂 15. Gist Forking System

- **Goal**: Implement a fully integrated fork action to duplicate any public Gist from GitHub to the authenticated user's account and save it locally for offline editing, with proper 422 self-forking error classification.
- **Files expected to change**: `GitHubApiService.kt`, `GistRepository.kt`, `GistViewModel.kt`, `GitHubGistApiList.kt`, `GistDetailScreen.kt`, `GistHubAppScreen.kt`, `SyncErrorHandler.kt`, `GistAppE2ETest.kt`
- **Implementation checklist**:
  - [x] Add `POST gists/{id}/forks` to `GitHubApiService.kt`.
  - [x] Implement `forkGist` in `GistRepository.kt` to fork the Gist, retrieve its full details and files, and upsert them locally via `saveResponseToDb`.
  - [x] Create `forkGist` and `isForking` tracking in `GistViewModel.kt`, wrapping success/failure under the centralized `SyncStatus` system.
  - [x] Map HTTP 422 self-forking/unprocessable errors to a user-friendly guidance message in `SyncErrorHandler.kt`.
  - [x] Add a dedicated E2E unit/integration test `test_forkGist_failsWith422_showsHelpfulError` in `GistAppE2ETest.kt` to verify error mapping.
  - [x] Add a responsive "Fork" outlined button on the explorer list (`GitHubGistApiList.kt`) with specific loading spinners.
  - [x] Add a "Fork Gist" action button in the `GistDetailScreen.kt` top bar next to star/pin actions.
  - [x] Connect screen callbacks in `GistHubAppScreen.kt`.
- **Verification command(s)**:
  - `./harness.sh check`
  - `./harness.sh test`
- **Definition of done**: Public Gists can be successfully duplicated to the user's account and saved in the local Room DB with full offline editing capabilities, triggered by high-contrast M3 buttons in both the list and detail views, with self-forking 422 errors clearly communicated to the user.

---

## 📂 16. Gist Sync Visual Indicators

- **Goal**: Add visual indicators (icons/labels) to the Gist list items that represent their current sync status: 'Synced', 'Local Only', or 'Pending'.
- **Files expected to change**: `GistCard.kt`
- **Implementation checklist**:
  - [x] Create three distinct Material 3 badge designs for the different synchronization states ('Synced', 'Local Only', 'Pending').
  - [x] Use `Icons.Default.CloudOff` for local-only drafts, `Icons.Default.Sync` for pending changes, and `Icons.Default.Check` for fully synced items.
  - [x] Pair each icon with its matching descriptive label and semantic eye-safe color scheme (green for Synced, red for Local Only, orange for Pending).
  - [x] Assign unique test tags (`sync_status_local_only`, `sync_status_pending`, `sync_status_synced`) to all state badges.
- **Verification command(s)**:
  - `./harness.sh check`
  - `./harness.sh test`
- **Definition of done**: Each local Gist item displays an appropriate, beautifully formatted badge with custom icons, color contrast, and tags reflecting its exact synchronization state.

---

## 📂 17. Clickable Gist Browser Links

- **Goal**: Enable users to view Gists directly on GitHub's website by tapping on Web URL elements displayed inside the application.
- **Files expected to change**: `DetailedGistMetadata.kt`
- **Implementation checklist**:
  - [x] Implement `androidx.compose.ui.platform.LocalUriHandler.current` inside `DetailedCreationInfoCard`.
  - [x] Wrap the Web URL information row in a Material-ripple `clickable` container with an explicit minimum touch-target height of `48.dp`.
  - [x] Assign `testTag("detail_web_url_row")` for automated target testing.
  - [x] Gracefully catch any URI launch exception.
- **Verification command(s)**:
  - `./harness.sh check`
  - `./harness.sh test`
- **Definition of done**: Tapping on the Web URL field in the Gist details view launches the system browser and navigates directly to the Gist's GitHub webpage.

---

## 📂 18. App Logo Crash Fix, Gemini Model Label Corrections & PR Creation

- **Goal**: Resolve the critical startup crash due to the missing `img_app_logo.jpg` drawable asset, correct the Gemini model designation labels to accurately reflect Gemini 3.5-Flash, and automate committing, pushing, and creating the GitHub Pull Request.
- **Files expected to change**: `GistHubAppScreen.kt`, `ic_launcher_foreground.xml`, `GistAiAssistantCardView.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Fix the `ResourceResolutionException` crash by refactoring `GistHubAppScreen.kt` to load a beautiful Material-styled programmatic gradient container with a vector code symbol (`Icons.Default.Code`) instead of referencing the missing `img_app_logo.jpg`.
  - [x] Re-architect the launcher icon foreground `ic_launcher_foreground.xml` to use a high-contrast vector code bracket drawing instead of the missing rasterized image asset.
  - [x] Correct the confusing online AI analysis label in `GistAiAssistantCardView.kt` from "Gemini Pro" to "Gemini 3.5-Flash Online Model" to perfectly align with the underlying REST integration code.
  - [x] Successfully commit and push all code changes to a secure fix branch: `fix/gemini-label-and-logo-crash`.
  - [x] Programmatically open a Pull Request (`#25`) on GitHub using the repository's access token via the GitHub REST API.
- **Verification command(s)**:
  - `./harness.sh check`
  - `./harness.sh test`
  - `./harness.sh build`
- **Definition of done**: The application starts up beautifully with zero asset-loading crashes, displays the correct modern vector launcher and brand logos, accurately names the "Gemini 3.5-Flash Online Model" on-screen, and automatically creates the corresponding Pull Request on GitHub.

---

## 📂 19. PR CI Formatting Fix and Developer Guardrails
- **Goal**: Resolve the CI check failures on Pull Request #25 caused by Spotless ktlint formatting discrepancies and prevent future occurrences by adding strict developer guardrails in `AGENTS.md`.
- **Files expected to change**: `AGENTS.md`, `TASK.md`
- **Implementation checklist**:
  - [x] Run `./harness.sh check` to pinpoint Spotless formatting discrepancies on the code edits in `GistHubAppScreen.kt`.
  - [x] Execute `gradle spotlessApply` to automatically format all files to align perfectly with the ktlint styling standards.
  - [x] Re-run the local verification quality gate to ensure detekt, lint, unit/integration tests, and formatting checks pass 100%.
  - [x] Commit and push the spotless-formatted changes to the remote branch `fix/gemini-label-and-logo-crash`.
  - [x] Update `AGENTS.md` to establish strict formatting rules in both the "Developer Verification Loop" and "Code Quality & Best Practices" sections, ensuring all future code edits undergo spotless checks before submission.
- **Verification command(s)**:
  - `./harness.sh check`
- **Definition of done**: The pull request CI workflow runs to 100% completion with a successful "green" status, and `AGENTS.md` contains strict, persistent formatting instructions for future AI developer agents.

---

## 📂 20. Mandatory Git Push, CI Verification & Completion Guidelines
- **Goal**: Ensure that all developer changes are committed, pushed, verified, and that the pull request CI passes completely without warnings or failures before task completion.
- **Files expected to change**: `AGENTS.md`, `TASK.md`
- **Implementation checklist**:
  - [x] Update `AGENTS.md` to define strict requirements for always committing and pushing modifications to the target PR branch.
  - [x] Instruct future agents to verify Pull Request CI runs and completely resolve all warnings/failures.
  - [x] Codify that a task is only complete when all CI checks pass ("green") and all comments on the PR are fully resolved.
  - [x] Safely repair local Git index/packfile corruption and align local branch with `origin/fix/gemini-label-and-logo-crash`.
  - [x] Confirm that the PR CI run completes successfully with the newly upgraded JDK 21 environment.
- **Verification command(s)**:
  - `git status`
  - GitHub Actions API checks
- **Definition of done**: `AGENTS.md` is updated with strict git commit, push, and CI guidelines, and the Pull Request CI runs to success ("green") with no warnings or failures.

---

## 📂 21. Coding Workflow & Verification Improvements (Harness Engineering)
- **Goal**: Optimize the developer inner-loop, automate local formatting, and enforce repository standards via automated Git hooks and formal custom agent skills.
- **Files expected to change**: `harness.sh`, `AGENTS.md`, `TASK.md`, `/.agents/skills/do-gist-hub-dev-workflow/SKILL.md`
- **Implementation checklist**:
  - [x] Add `./harness.sh format` subcommand mapping to Spotless ktlint formatting to easily auto-fix codebase style issues.
  - [x] Add `./harness.sh setup-hooks` subcommand to automatically configure a Git `pre-push` hook executing `./harness.sh check`, blocking broken pushes.
  - [x] Create a dedicated custom agent skill `do-gist-hub-dev-workflow` in `/.agents/skills/do-gist-hub-dev-workflow/SKILL.md` to enforce the codebase's strict manual constructor injection, 600 LOC limits, and synchronization states on future AI coding agents.
  - [x] Synchronize `AGENTS.md` and `TASK.md` to reference the newly introduced workflow commands.
- **Verification command(s)**:
  - `./harness.sh help`
  - `./harness.sh setup-hooks`
- **Definition of done**: Harness supports local auto-formatting and hook installation, all scripts execute cleanly, and the codebase rules are formalized under a custom agent skill.

---

## 📂 22. Codebase Refactoring, 600 LOC Compliance & Build Concurrency Guidelines
- **Goal**: Audit codebase for strict compliance with 600 LOC limits, extract reusable test components, clean up deprecated AGP configuration warnings, and establish execution rules to prevent build container timeouts.
- **Files expected to change**: `app/src/test/java/com/example/GistAppE2ETest.kt`, `app/src/test/java/com/example/FakeGitHubApiService.kt`, `app/build.gradle.kts`, `AGENTS.md`, `/.agents/skills/do-gist-hub-dev-workflow/SKILL.md`, `TASK.md`
- **Implementation checklist**:
  - [x] Audit source files against 600 LOC threshold; identify `GistAppE2ETest.kt` (656 LOC).
  - [x] Extract `FakeGitHubApiService` into `app/src/test/java/com/example/FakeGitHubApiService.kt` to reduce `GistAppE2ETest.kt` to 522 LOC and make the fake service modular and reusable.
  - [x] Remove deprecated `buildToolsVersion = "35.0.0"` from `app/build.gradle.kts` to eliminate AGP 9.1.1 deprecation warnings.
  - [x] Document build concurrency rules in `AGENTS.md` and `do-gist-hub-dev-workflow/SKILL.md`: never execute `compile_applet` concurrently with background `./harness.sh` tasks to prevent lock contention and control plane health timeouts.
  - [x] Run `./harness.sh format` and `./harness.sh check` to verify 100% success across wrapper integrity, Spotless formatting, Detekt, Lint, and local unit/E2E test pyramid suites.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
- **Definition of done**: No file exceeds 600 LOC, AGP warnings are resolved, build concurrency safety guidelines are codified in skills/AGENTS.md, and harness checks pass cleanly.

---

## 📂 23. Swarm Agent Orchestration Framework & Small TODO Decomposition

- **Goal**: Implement a multi-agent swarm strategy led by an Orchestrator Agent to decompose features, quality checks, and refactoring tasks into atomic, verified TODOs.
- **Files expected to change**: `TASK.md`, `AGENTS.md`, `DraftEditorDialog.kt`, `DraftEditorAiRefactoringHelper.kt`
- **Implementation checklist**:
  - [x] **Sub-Agent 1 (Orchestrator)**: Decompose full application lifecycle and maintenance into specialized agent domains (Architect, UI/Compose, Sync Engine, QA/Test Pyramid, CI/CD).
  - [x] **Sub-Agent 2 (Architect)**: Enforce 600 LOC limits across all source files; extracted `DraftEditorAiRefactoringHelper.kt` from `DraftEditorDialog.kt` (reduced from 574 to 503 LOC).
  - [x] **Sub-Agent 3 (UI / Compose Specialist)**: Verify Material 3 components, touch targets (≥48dp), and explicit `testTag` identifiers on interactive elements.
  - [x] **Sub-Agent 4 (QA / Test Pyramid Specialist)**: Ensure Spotless ktfmt formatting compliance via `./harness.sh format`.
  - [x] **Sub-Agent 5 (CI / Build Specialist)**: Execute `./harness.sh check` pipeline (wrapper check, spotless, detekt, lint, unit & E2E tests).
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
- **Definition of done**: Orchestrator-driven task decomposition complete, 600 LOC compliance verified across all files, code formatted with Spotless, and 100% of pipeline checks passing green.

---

## 📂 24. Codebase Architectural Refactoring & 600 LOC Ceiling Enforcement

- **Goal**: Refactor `GistViewModel.kt` and `GistHubAppScreen.kt` to ensure strict compliance with the 600 LOC ceiling rule, extracting navigation components and revision extension functions.
- **Files expected to change**: `app/src/main/java/com/example/ui/viewmodel/GistViewModel.kt`, `app/src/main/java/com/example/ui/viewmodel/GistViewModelRevisionExtensions.kt`, `app/src/main/java/com/example/ui/screens/GistHubAppScreen.kt`, `app/src/main/java/com/example/ui/components/GistHubNavigationComponents.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Extract `GistViewModelRevisionExtensions.kt` from `GistViewModel.kt` (reduced `GistViewModel.kt` from 585 to 511 LOC).
  - [x] Extract `GistHubNavigationComponents.kt` containing `GistHubTopAppBar` and `GistHubBottomBar` from `GistHubAppScreen.kt` (reduced `GistHubAppScreen.kt` from 572 to 413 LOC).
  - [x] Suppress Detekt `VariableNaming` rule for internal backing state flows.
  - [x] Execute `./harness.sh format` to apply Spotless ktfmt formatting across all modified files.
  - [x] Execute `./harness.sh check` to verify wrapper integrity, spotless, detekt, lint, and full unit/integration/E2E test suites.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
- **Definition of done**: All Kotlin source files in `app/src/main` are under 550 LOC, code is spotless-formatted, and 100% of harness quality checks pass green.

---

## 📂 25. Pull-to-Refresh & WorkManager Integration

- **Goal**: Implement Pull-to-Refresh functionality on the Gist list screen using PullRefreshIndicator, triggering a manual sync WorkManager request.
- **Files expected to change**: `GistViewModel.kt`, `HomeScreen.kt`, `GistHubAppScreen.kt`, `ConfigScreen.kt`, `libs.versions.toml`, `app/build.gradle.kts`, `TASK.md`
- **Implementation checklist**:
  - [x] Register standard Compose Material library dependency (`androidx.compose.material`) in `libs.versions.toml` and implement it in `app/build.gradle.kts`.
  - [x] Update `GistViewModel.kt` to accept `Context` in `refreshGists(context)` and schedule a manual sync WorkManager request via `GistSyncWorker.enqueue(context)` when present.
  - [x] Collect `syncStatus` reactively within `GistViewModel.kt` to update `isRefreshing` state dynamically.
  - [x] Refactor `HomeScreen.kt` to use `@OptIn(ExperimentalMaterialApi::class)` and replace `PullToRefreshBox` with Material 2's `PullRefreshIndicator` and the `pullRefresh` state container.
  - [x] Update `GistHubAppScreen.kt` and `ConfigScreen.kt` to retrieve local `Context` via `LocalContext.current` and pass it to `viewModel.refreshGists(context)`.
  - [x] Run `./harness.sh format` and `./harness.sh check` to verify formatting, linter rules, and all Robolectric unit/integration/E2E tests pass 100%.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
- **Definition of done**: The Pull-to-Refresh gesture enqueues the `GistSyncWorker` through WorkManager, updates the reactive syncing state properly, and passes the entire quality gate suite with zero errors.

---

## 📂 26. Swipe-to-Delete Functionality & Confirmation Undo Snackbar

- **Goal**: Enable swipe-to-delete functionality for items in the Gist list (Home and Vault screens) using Material 3 `SwipeToDismissBox`, with a confirmation Snackbar that allows undoing the deletion.
- **Files expected to change**: `GistRepository.kt`, `GistViewModel.kt`, `HomeScreen.kt`, `VaultScreen.kt`, `GistHubAppScreen.kt`, `SwipeToDeleteAndUndoTest.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Add `restoreGist(gistWithFiles)` method to `GistRepository.kt` to allow undeleting soft-deleted Gists in Room.
  - [x] Update `GistViewModel.kt` with `pendingDeleteEvent`, `recentlyDeletedGist`, and `restoreGist(item)` state and action handlers.
  - [x] Wrap `GistCard` items in `HomeScreen.kt` and `VaultScreen.kt` using Material 3 `SwipeToDismissBox` with a red background and trash icon.
  - [x] Integrate `SnackbarHost` in `GistHubAppScreen.kt` triggered by `pendingDeleteEvent` with an "Undo" action that calls `viewModel.restoreGist(deletedItem)`.
  - [x] Add unit & UI component tests in `SwipeToDeleteAndUndoTest.kt` verifying deletion state, undo restoration flow, and SwipeToDismiss layout node existence.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
- **Definition of done**: Swiping a Gist item on either Home or Vault screen triggers soft-deletion, displays a confirmation Snackbar with an "Undo" action that successfully restores the item, and passes all tests.

---

## 📂 27. Gist List Sorting Menu ('Recently Updated', 'Created Date', 'Title')

- **Goal**: Add a menu to the Gist list screen (HomeScreen) allowing users to sort gists by 'Recently Updated', 'Created Date', or 'Title'.
- **Files expected to change**: `GistSortOption.kt`, `GistViewModel.kt`, `HomeScreen.kt`, `GistHubAppScreen.kt`, `GistSortTest.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Create `GistSortOption.kt` enum with `RECENTLY_UPDATED`, `CREATED_DATE`, and `TITLE` options.
  - [x] Add `sortOption` StateFlow and `updateSortOption(option)` handler to `GistViewModel.kt`.
  - [x] Add sort dropdown menu button (`sort_menu_button`) in `HomeScreen.kt` header bar and update `remember(gists, searchQuery, selectedTag, sortOption)` sorting comparator logic.
  - [x] Pass `sortOption` and `onSortOptionChange` from `GistHubAppScreen.kt` to `HomeScreen`.
  - [x] Write `GistSortTest.kt` verifying default sort option, StateFlow updates, comparator sorting logic, and Compose UI dropdown menu interaction.
  - [x] Format with `./harness.sh format` and run test suite with `./harness.sh test`.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh test`
- **Definition of done**: Gist list sorting dropdown menu is rendered on HomeScreen, sorting option updates the displayed Gist list dynamically, and all unit and Robolectric tests pass green.

---

## 📂 29. Last Synced Status Bar Component

- **Goal**: Create a status bar component that displays the 'Last Synced' timestamp from the local Room database to improve user trust in data freshness.
- **Files expected to change**: `SyncStatusBar.kt`, `GistHubNavigationComponents.kt`, `GistHubAppScreen.kt`, `SyncScreen.kt`, `SyncStatusBarTest.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Implement `SyncStatusBar.kt` component in `com.example.ui.components` featuring relative timestamp formatting (`formatLastSyncTime`), status badges (Synced, Syncing, Error, Not Synced), and explicit `testTag` attributes (`sync_status_bar`, `last_synced_text`, `sync_status_indicator`).
  - [x] Integrate `SyncStatusBar` into top navigation bar (`GistHubNavigationComponents.kt`) and `SyncScreen.kt`.
  - [x] Connect `lastSyncTime` and `syncStatus` from `GistViewModel.kt` through `GistHubAppScreen.kt`.
  - [x] Write `SyncStatusBarTest.kt` verifying timestamp formatting function, component rendering, and sync state badges across all conditions.
  - [x] Format codebase using `./harness.sh format`.
  - [x] Verify app compilation with `compile_applet`.
- **Verification command(s)**:
  - `./harness.sh format`
  - `compile_applet`
- **Definition of done**: 'Last Synced' status bar component displays formatted Room DB sync timestamps and status indicators across app screens, fully tested with Compose test rules and formatted with Spotless.

---

## 📂 30. Centralized Loading State Handling Mechanism

- **Goal**: Implement a centralized loading state handling mechanism in the UI using Compose to manage data fetching feedback across all application flows (syncing, refreshing, profile verifying, remote querying, AI analyzing, and forking).
- **Files expected to change**: `LoadingFeedbackComponents.kt`, `GistHubAppScreen.kt`, `GitHubGistApiList.kt`, `LoadingFeedbackTest.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Create `LoadingFeedbackComponents.kt` containing `DataLoadingState` sealed models, `LoadingFeedbackBar` top banner, `LoadingFeedbackOverlay` modal progress card, `SkeletonLoadingList` pulse placeholders, and `LoadingFeedbackBox` universal layout wrapper with test tag annotations.
  - [x] Integrate `LoadingFeedbackBar` into `GistHubAppScreen.kt` for top banner sync feedback and `LoadingFeedbackOverlay` for modal operations (token verification and gist forking).
  - [x] Integrate `SkeletonLoadingList` into `GitHubGistApiList.kt` during direct remote API loading.
  - [x] Create `LoadingFeedbackTest.kt` verifying rendering of `LoadingFeedbackBar`, `LoadingFeedbackOverlay` (with cancel CTA click), `SkeletonLoadingList`, and `LoadingFeedbackBox` empty/error/retry states.
  - [x] Format codebase using `./harness.sh format`.
  - [x] Execute full test suite via `./harness.sh test` and verify app compilation with `compile_applet`.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh test`
  - `compile_applet`
- **Definition of done**: Centralized loading state handling mechanism is fully implemented in Jetpack Compose, integrated across app screens, tested with Compose test rules on JVM Robolectric, formatted with Spotless, and compiled without errors.

---

## 📂 31. Pull-To-Refresh Swipe Refresh Indicator on Main Gist List

- **Goal**: Add a SwipeRefreshLayout / Compose `PullRefreshIndicator` to the main Gist list screen (`HomeScreen.kt`) to allow manual swipe triggers for local-to-remote Gist synchronization.
- **Files expected to change**: `HomeScreen.kt`, `PullToRefreshHomeScreenTest.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Configure `rememberPullRefreshState` and `.pullRefresh()` modifier on `HomeScreen.kt`'s primary root container.
  - [x] Attach `PullRefreshIndicator` styled with Material 3 primary theme colors at `Alignment.TopCenter` with `testTag("pull_refresh_indicator")`.
  - [x] Create `PullToRefreshHomeScreenTest.kt` verifying rendering of `PullRefreshIndicator` during active pull-to-refresh state and idle state.
  - [x] Format codebase using `./harness.sh format`.
  - [x] Execute full test suite via `./harness.sh test` and verify app compilation with `compile_applet`.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh test`
  - `compile_applet`
- **Definition of done**: Pull-to-refresh indicator is integrated into `HomeScreen.kt`, allowing manual pull triggers for remote-to-local Gist synchronization, verified via Robolectric JVM tests, formatted with Spotless, and compiled cleanly.

---

## 📂 32. WorkManager Periodic Background Synchronization

- **Goal**: Configure a WorkManager periodic task to perform periodic background synchronization of local gist changes with the remote GitHub API.
- **Files expected to change**: `DoGistHubApp.kt`, `GistSyncWorker.kt`, `GistSyncWorkerTest.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Configure `DoGistHubApp` to implement `Configuration.Provider` returning custom WorkManager configuration with `GistSyncWorkerFactory`.
  - [x] Implement `enqueuePeriodic` in `GistSyncWorker` with `15` minutes periodic interval, `NetworkType.CONNECTED` constraint, exponential backoff, and `ExistingPeriodicWorkPolicy.UPDATE`.
  - [x] Schedule periodic background synchronization on application startup in `DoGistHubApp`.
  - [x] Add unit tests in `GistSyncWorkerTest.kt` verifying `DoGistHubApp` WorkManager configuration, `GistSyncWorker.enqueuePeriodic` WorkInfo scheduling, and periodic worker execution pushing unsynced local drafts.
  - [x] Format codebase using `./harness.sh format`.
  - [x] Execute full verification via `./harness.sh check` and `compile_applet`.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
  - `compile_applet`
- **Definition of done**: WorkManager periodic background synchronization task is fully configured and scheduled, supported by `Configuration.Provider` in `DoGistHubApp`, verified via Robolectric JVM tests, formatted with Spotless, and compiled without errors.

---

## 📂 33. Synchronization Status Dashboard View for Locally Modified Gists

- **Goal**: Create a status dashboard view that displays the current synchronization state (e.g., 'Synced', 'Pending', 'Offline') for locally modified Gists.
- **Files expected to change**: `SyncStatusDashboardView.kt`, `SyncScreen.kt`, `GistViewModel.kt`, `GistHubAppScreen.kt`, `SyncStatusDashboardTest.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Create `SyncStatusDashboardView.kt` featuring overall status state badges ('Synced', 'Pending', 'Offline'), metrics summary boxes (Total Local, Pending Sync, Network), list of locally modified Gists (Drafts, Modified, Pending Delete), and a push sync CTA button with test tags.
  - [x] Add `isOnline` StateFlow to `GistViewModel.kt` and integrate it into `GistHubAppScreen.kt` and `SyncScreen.kt`.
  - [x] Embed `SyncStatusDashboardView` into `SyncScreen.kt` to present a unified synchronization status view for locally modified Gists.
  - [x] Create `SyncStatusDashboardTest.kt` verifying rendering of 'Synced', 'Pending', and 'Offline' status badges and metrics under all network and local modification conditions.
  - [x] Format codebase using `./harness.sh format`.
  - [x] Execute full pipeline checks via `./harness.sh check` and verify compilation via `compile_applet`.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
  - `compile_applet`
- **Definition of done**: Synchronization status dashboard view is fully implemented and integrated, displaying current sync state ('Synced', 'Pending', 'Offline') and locally modified Gists, verified with Compose test rules on JVM Robolectric, formatted with Spotless, and compiled without errors.

---

## 📂 34. Gist List View Empty State UI Component

- **Goal**: Create an empty state UI component for the Gist list view that displays a helpful message and a call-to-action button when there are no Gists synchronized or stored locally.
- **Files expected to change**: `GistListEmptyState.kt`, `HomeScreen.kt`, `GistListEmptyStateTest.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Create `GistListEmptyState.kt` with modern M3 styling, illustration icon, helpful message, and "Fetch / Sync Gists" CTA button with test tag `empty_state_fetch_btn`.
  - [x] Integrate `GistListEmptyState` in `HomeScreen.kt` when the local database contains zero Gists.
  - [x] Add unit test `GistListEmptyStateTest.kt` verifying rendering and CTA click behavior.
  - [x] Format codebase using `./harness.sh format`.
  - [x] Execute full verification via `./harness.sh check` and `compile_applet`.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
  - `compile_applet`
- **Definition of done**: Gist list view empty state component is fully implemented, integrated into `HomeScreen.kt`, verified via Robolectric JVM tests, formatted with Spotless, and compiled without errors.

---

## 📂 35. Deletion Confirmation Dialog Flow Verification

- **Goal**: Verify and refine the deletion confirmation dialog flow across Home screen and Detail screen.
- **Files expected to change**: `DeleteConfirmationDialogTest.kt`, `GistDetailScreen.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Refine `DeleteConfirmationDialogTest.kt` to cover cancel, confirm, and detail screen deletion flows.
  - [x] Clean up icon content description redundancy inside `GistDetailScreen.kt` buttons to ensure clean accessibility semantics and button node targeting.
  - [x] Execute full pipeline checks via `./harness.sh check` and verify compilation via `compile_applet`.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
  - `compile_applet`
- **Definition of done**: Deletion confirmation dialog flows are fully verified with Robolectric JVM unit tests passing green, formatted with Spotless, and compiled without errors.

---

## 📂 36. Local Gist Content Encryption via androidx.security.crypto

- **Goal**: Encrypt sensitive Gist file content using the `androidx.security.crypto` library before saving to the local Room database, and transparently decrypt when accessed via the repository.
- **Files expected to change**: `GistContentEncryptor.kt`, `GistRepository.kt`, `GistRepositoryExtensions.kt`, `DoGistHubApp.kt`, `GistContentEncryptionTest.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Create `GistContentEncryptor.kt` using `androidx.security.crypto.MasterKey` with AES-256 GCM cipher encryption and decryption.
  - [x] Integrate `GistContentEncryptor` into `GistRepository` to encrypt file content before persisting to Room DB and decrypt when reading.
  - [x] Wire `GistContentEncryptor` instance in `DoGistHubApp.kt`.
  - [x] Add comprehensive unit test `GistContentEncryptionTest.kt` verifying encryption/decryption, legacy unencrypted pass-through, and repository/Room integration.
  - [x] Execute full pipeline checks via `./harness.sh check` and verify compilation via `compile_applet`.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
  - `compile_applet`
- **Definition of done**: Sensitive Gist content is encrypted before persisting to the local Room database using `androidx.security.crypto`, transparently decrypted on access, verified via Robolectric JVM unit tests, formatted with Spotless, and compiled without errors.

---

## 📂 37. Visual Sync State Indicator Component

- **Goal**: Create a visual sync state indicator component that tracks whether local Gist changes have been pushed to GitHub using the Room database sync status flags (`isLocalOnly`, `isDirty`, `isDeleted`).
- **Files expected to change**: `GistSyncStateIndicator.kt`, `GistCard.kt`, `GistDetailScreen.kt`, `GistSyncStateIndicatorTest.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Create `GistSyncStateIndicator.kt` providing compact badges and detailed banner indicators for Room DB sync status flags (`isLocalOnly`, `isDirty`, `isDeleted`, `synced`).
  - [x] Integrate `GistSyncStateIndicator` into `GistCard.kt` and `GistDetailScreen.kt`.
  - [x] Write Robolectric test `GistSyncStateIndicatorTest.kt` verifying state resolution logic and UI rendering for all sync flags.
  - [x] Execute pipeline checks via `./harness.sh format` and `./harness.sh check`.
  - [x] Verify applet compilation via `compile_applet`.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
  - `compile_applet`
- **Definition of done**: Visual sync state indicator component cleanly renders Room sync status flags (`isLocalOnly`, `isDirty`, `isDeleted`, `synced`), integrated in list and detail views, fully verified via Robolectric JVM tests, formatted with Spotless, and compiled without errors.

---

## 📂 38. Local Gists Repository Screen

- **Goal**: Implement a screen that lists all local gists, showing their title, snippet, and sync status icon.
- **Files expected to change**: `LocalGistsScreen.kt`, `VaultScreen.kt`, `LocalGistsScreenTest.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Create `LocalGistsScreen.kt` listing all local gists stored in Room database with title, code snippet preview box, and sync status icon indicator (`GistSyncStateIndicator`).
  - [x] Add search filtering and category filter chips (All Local, Local Only, Unpushed Edits, Synced).
  - [x] Integrate `LocalGistsScreen` into `VaultScreen.kt` with a seamless view mode toggle between Unsynced Drafts and All Local Gists.
  - [x] Write Robolectric test `LocalGistsScreenTest.kt` verifying screen rendering, search/filter logic, and click action callbacks.
  - [x] Execute pipeline checks via `./harness.sh format` and `./harness.sh check`.
  - [x] Verify applet compilation via `compile_applet`.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
  - `compile_applet`
- **Definition of done**: Local gists repository screen lists all local gists showing title, snippet, and sync status icon, fully integrated, verified via Robolectric JVM tests, formatted with Spotless, and compiled without errors.

---

## 📂 39. Expandable Content View and Detail Screen with Auto-Decryption Display

- **Goal**: Create/enhance an expandable view and detail screen that displays the full content of a selected Gist, automatically decrypting the content for display and showing explicit decryption status indicators.
- **Files expected to change**: `GistCard.kt`, `GistDetailScreen.kt`, `ExpandableGistViewTest.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Add inline expandable full content view (`isExpanded` state) in `GistCard.kt` with `expand_gist_button` toggle and `auto_decrypted_badge`.
  - [x] Add `detail_decrypted_banner` security status card to `GistDetailScreen.kt` indicating automatic AES-256 decryption for display.
  - [x] Create JVM Robolectric unit test `ExpandableGistViewTest.kt` verifying expandable card view toggle behavior, auto-decrypted badge rendering, and detail screen full content display.
  - [x] Format codebase using `./harness.sh format`.
  - [x] Execute pipeline checks via `./harness.sh check` and verify compilation via `compile_applet`.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
  - `compile_applet`
- **Definition of done**: Expandable view in Gist cards and full detail screen display full auto-decrypted content with clear decryption status indicators, fully verified via Robolectric JVM tests, formatted with Spotless, and compiled without errors.

---

## 📂 40. Gist Creation and Edit Screen with Integrated Room Storage Encryption

- **Goal**: Create a dedicated screen for creating and editing Gists, including fields for title/filename and description, integrated with the Room storage encryption logic (`GistContentEncryptor`).
- **Files expected to change**: `CreateEditGistScreen.kt`, `CreateEditGistScreenTest.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Create `CreateEditGistScreen.kt` with fields for Title/Filename, Description, File Content, Tags, Public/Secret toggle, and explicit Room AES-256 Encryption Security status card.
  - [x] Create JVM Robolectric unit test `CreateEditGistScreenTest.kt` verifying rendering of fields, pre-populating existing Gists for editing, encryption badge visibility, and onSave callback invocation.
  - [x] Format codebase using `./harness.sh format`.
  - [x] Execute pipeline checks via `./harness.sh check` and verify compilation via `compile_applet`.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
  - `compile_applet`
- **Definition of done**: Dedicated Gist creation and edit screen with title, description, content fields, and Room AES-256 encryption status card implemented, fully verified via Robolectric JVM tests, formatted with Spotless, and compiled without errors.

---

## 🏷️ 41. Custom Gist Tagging System & Room Storage Integration

- **Goal**: Add a tagging system for Gists to allow users to categorize their snippets with custom labels, updating Room schema and UI accordingly.
- **Files expected to change**: `GistEntity.kt`, `RoomConverters.kt`, `LocalGistsScreen.kt`, `GistCard.kt`, `GistDetailScreen.kt`, `TaggingSystemTest.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Verify Room entity `tags: List<String>` and `RoomConverters` JSON list adapter in SQLite database schema.
  - [x] Add tag chip filtering and tag-based search matching in `LocalGistsScreen.kt`.
  - [x] Add explicit `testTag` attributes for tag chips across `GistCard.kt`, `GistDetailScreen.kt`, and `LocalGistsScreen.kt`.
  - [x] Create JVM Robolectric test `TaggingSystemTest.kt` testing RoomConverters serialization, entity custom tags support, and tag chip filter rendering/selection in `LocalGistsScreen`.
  - [x] Format code via `./harness.sh format`.
  - [x] Execute pipeline checks via `./harness.sh check` and compile applet via `compile_applet`.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
  - `compile_applet`
- **Definition of done**: Custom Gist tagging system with Room schema JSON converters, tag filter chips, and tag search in UI fully implemented, verified with Robolectric tests, formatted, and compiled cleanly.

---

## 💾 42. Offline Room Persistence Layer for GitHub Gists Data

- **Goal**: Implement a Room-based local persistence layer to store GitHub Gist data for offline access, ensuring the app functions seamlessly when the network is unavailable.
- **Files expected to change**: `GistRepository.kt`, `GistDao.kt`, `FakeGitHubApiService.kt`, `OfflineRoomPersistenceTest.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Verify Room database schema and DAO entities (`GistEntity`, `GistFileEntity`, `GistWithFiles`) providing local SQLite caching for Gist metadata and file content.
  - [x] Configure reactive `Flow<List<GistWithFiles>>` streams in `GistRepository` ensuring offline UI access to all cached and draft gists.
  - [x] Implement offline fallback mechanism in `GistRepository` so network connection failures gracefully fall back to local Room persistence.
  - [x] Enable network error simulation in `FakeGitHubApiService` for offline testing.
  - [x] Create JVM Robolectric unit test `OfflineRoomPersistenceTest.kt` verifying offline draft creation, offline cached data retrieval, offline local edits with dirty-flag management, and local search queries.
  - [x] Format codebase using `./harness.sh format`.
  - [x] Execute pipeline checks via `./harness.sh check` and verify compilation via `compile_applet`.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
  - `compile_applet`
- **Definition of done**: Room-based local persistence layer for storing GitHub Gist data offline implemented and thoroughly tested with Robolectric unit tests, formatted with Spotless, and compiled without errors.

---

## 🔍 43. Full-Text Search Bar with Local Room Database Content Snippet Query
- **Goal**: Implement a full-text search bar at the top of the Gist list that queries the local Room database to filter results by file names, descriptions, or content snippets.
- **Files expected to change**: `GistDao.kt`, `HomeScreen.kt`, `GistDaoTest.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Update Room DAO query in `GistDao.kt` (`searchLocalGists`) to match file names (`filename`), descriptions (`description`), or file content snippets (`content`).
  - [x] Update search bar placeholder in `HomeScreen.kt` to clearly indicate filename, content snippet, or description full-text search capability.
  - [x] Create/update Robolectric unit tests in `GistDaoTest.kt` verifying Room full-text search queries filtering across filename, description, and content snippets.
  - [x] Format codebase using `./harness.sh format`.
  - [x] Execute static analysis and test suite via `./harness.sh check` and `./harness.sh test`.
  - [x] Verify applet compilation via `compile_applet`.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
  - `./harness.sh test`
  - `compile_applet`
- **Definition of done**: Full-text search bar querying Room database for file names, descriptions, and content snippets implemented, verified via Robolectric unit tests, formatted with Spotless, checked with static analysis, and compiled cleanly.

---

## 📴 44. Global Offline-Only Mode Toggle in Dashboard Header
- **Goal**: Add a global 'Offline-Only' mode toggle in the dashboard header that pauses network synchronization and displays a clear visual indicator when enabled.
- **Files expected to change**: `ConfigPrefs.kt`, `GistViewModel.kt`, `GistHubNavigationComponents.kt`, `SyncStatusDashboardView.kt`, `SyncStatusBar.kt`, `SyncScreen.kt`, `GistHubAppScreen.kt`, `GistSyncWorker.kt`, `DoGistHubApp.kt`, `OfflineOnlyModeTest.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Add `isOfflineOnly` flag and preference setter/getter in `ConfigPrefs.kt`.
  - [x] Expose `isOfflineOnly` state flow and toggle logic in `GistViewModel.kt` to pause manual and background network synchronization when enabled.
  - [x] Update `GistHubNavigationComponents.kt` and `SyncStatusDashboardView.kt` with header toggle button, switch, and active warning banner.
  - [x] Update `GistSyncWorker.kt` and `DoGistHubApp.kt` to respect offline-only preference before enqueuing or triggering sync workers.
  - [x] Add Robolectric unit tests in `OfflineOnlyModeTest.kt` verifying preferences persistence, ViewModel state transitions, and Compose UI header indicators.
  - [x] Format codebase using `./harness.sh format`.
  - [x] Run static analysis and tests via `./harness.sh check` and `./harness.sh test`.
  - [x] Verify applet compilation via `compile_applet`.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
  - `./harness.sh test`
  - `compile_applet`
- **Definition of done**: Global Offline-Only mode header toggle implemented with visual indicators and network sync pause logic, verified with Robolectric unit tests, formatted, and compiled without errors.

---

## 🔔 45. Sync Snackbar Notification Utility
- **Goal**: Implement a utility to show snackbar notifications when sync operations succeed or fail, ensuring the user knows the status of their local data.
- **Files expected to change**: `SyncNotificationUtil.kt`, `GistHubAppScreen.kt`, `SyncNotificationUtilTest.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Create `SyncNotificationUtil.kt` providing a reusable utility to present structured snackbars for sync success and failure states.
  - [x] Integrate `SyncNotificationUtil.showSyncStatusSnackbar` into `GistHubAppScreen.kt` reactive syncStatus flow collector.
  - [x] Create Robolectric unit tests in `SyncNotificationUtilTest.kt` testing snackbar display logic and dismiss callbacks.
  - [x] Format codebase using `./harness.sh format`.
  - [x] Execute static analysis and test suite via `./harness.sh check` and `./harness.sh test`.
  - [x] Verify applet compilation via `compile_applet`.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
  - `./harness.sh test`
  - `compile_applet`
- **Definition of done**: Sync notification utility displaying snackbars for sync success and error statuses implemented, integrated into the UI lifecycle, tested with Robolectric, formatted, and compiled cleanly.

---

## 🔑 46. GitHub PAT Authentication Status Indicator & Update Dialog
- **Goal**: Implement a simple status indicator or dialog that displays whether the user is authenticated via their GitHub PAT, with an option to update or clear the token.
- **Files expected to change**: `AuthStatusDialog.kt`, `GistHubNavigationComponents.kt`, `GistHubAppScreen.kt`, `AuthStatusDialogTest.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Create `AuthStatusIndicatorChip` badge displaying current PAT authentication state (`PAT Active` / `@username` when authenticated, `No PAT` when unauthenticated).
  - [x] Create `AuthStatusDialog` presenting authentication status card, PAT token input field with show/hide password toggle, verification progress feedback, "Verify & Save Token" action, "Clear Token" option, and "Done" close button.
  - [x] Integrate `AuthStatusIndicatorChip` into `GistHubTopAppBar` (`GistHubNavigationComponents.kt`).
  - [x] Connect `AuthStatusDialog` state and callbacks (`viewModel.updateToken`, `viewModel.validateAndFetchProfile`, `viewModel.clearConfig`) in `GistHubAppScreen.kt`.
  - [x] Write `AuthStatusDialogTest.kt` verifying status chip rendering, dialog layout, token updates, verification triggers, clear actions, and dismissal.
  - [x] Format codebase using `./harness.sh format`.
  - [x] Verify applet compilation via `compile_applet`.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
  - `compile_applet`
- **Definition of done**: GitHub PAT authentication status indicator chip and interactive update dialog implemented, connected to GistViewModel, tested with Compose Robolectric rules, formatted with Spotless, and compiled without errors.

---

## 📏 47. Strict 600 LOC Ceiling Modularization & Extraction
- **Goal**: Enforce Section 5.1 rule that no Kotlin source file should exceed 600 LOC by extracting components and viewmodel extensions.
- **Files expected to change**: `GistViewModel.kt`, `LocalGistsScreen.kt`, `LocalGistItemCard.kt`, `GistViewModelRemoteExtensions.kt`, `GistHubAppScreen.kt`, `GistAppE2ETest.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Extract `LocalGistItemCard` composable out of `LocalGistsScreen.kt` into `app/src/main/java/com/example/ui/components/LocalGistItemCard.kt` (reducing `LocalGistsScreen.kt` from 628 to 399 LOC).
  - [x] Extract `forkGist` and `fetchRemoteGistsDirectly` from `GistViewModel.kt` into `app/src/main/java/com/example/ui/viewmodel/GistViewModelRemoteExtensions.kt` (reducing `GistViewModel.kt` from 640 to 593 LOC).
  - [x] Provide explicit imports across consumer files (`GistHubAppScreen.kt`, `GistAppE2ETest.kt`) per Rule 5.6.
  - [x] Enforce Detekt suppression annotations (`@Suppress("VariableNaming")`) for internal state properties per Rule 5.7.
  - [x] Verify zero Kotlin source files across the entire codebase exceed 600 LOC.
  - [x] Run `./harness.sh format` and `./harness.sh check` to pass Spotless, Detekt, Android Lint, and all Robolectric unit/E2E tests.
  - [x] Verify compilation using `compile_applet`.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
  - `compile_applet`
- **Definition of done**: All Kotlin source files remain strictly under 600 LOC, with spotless formatting, clean Detekt analysis, full test green status, and clean applet compilation.

---

## 📜 48. Revision History & Visual Diff Viewer in GistDetailScreen
- **Goal**: Integrate existing revision history and visual diff viewer capabilities directly into `GistDetailScreen`, providing seamless inspection of past commits and changes with split/unified views while keeping source files strictly under 600 LOC.
- **Files expected to change**: `GistDetailScreen.kt`, `DetailFileItemCard.kt`, `GistHubAppScreen.kt`, `GistDetailCopyTest.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Extract `DetailFileItemCard` composable out of `GistDetailScreen.kt` to preserve modularity and maintain LOC well below the 600 LOC ceiling.
  - [x] Add Revisions toggle button in top bar and interactive "Files" / "Revisions" tab selector in `GistDetailScreen`.
  - [x] Connect `GistDetailScreen` to `GistViewModel` reactive revision state flows (`historyList`, `selectedRevisionSha`, `currentRevisionGist`, `parentRevisionGist`, `diffViewMode`, etc.).
  - [x] Integrate `RevisionHistoryListView` and `DetailedRevisionChangesView` with unified and split visual diffing.
  - [x] Pass `viewModel` parameter from `GistHubAppScreen.kt` to `GistDetailScreen`.
  - [x] Add test coverage in `GistDetailCopyTest.kt` verifying file rendering, markdown preview mode toggling, and copy actions.
  - [x] Format codebase using `./harness.sh format`.
  - [x] Verify local tests pass via `gradle :app:testDebugUnitTest`.
  - [x] Verify applet compilation via `compile_applet`.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
  - `compile_applet`
- **Definition of done**: Revision history and visual diff viewer are fully integrated into GistDetailScreen, modularized into reusable components, tested, formatted with Spotless, and compiled without errors.

---

## 🔧 49. Resolution of FrameTracker IME Inset Animation Timeouts
- **Goal**: Fix `FrameTracker` error (`time out: J<IME_INSETS_SHOW_ANIMATION>`) during soft keyboard show/hide animations across dialogs and search inputs.
- **Files expected to change**: `AndroidManifest.xml`, `DraftEditorDialog.kt`, `HomeScreen.kt`, `GistHubNavigationComponents.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Configure `android:windowSoftInputMode="adjustResize"` on `MainActivity` in `AndroidManifest.xml` to guarantee proper window resizing during IME insets transition.
  - [x] Apply `.imePadding()` to the root dialog surface in `DraftEditorDialog.kt` to ensure dialog container honors keyboard insets without blocking animation frames.
  - [x] Wire `LocalFocusManager.current.clearFocus()` and `ImeAction.Search` to search inputs in `HomeScreen.kt` and `GistHubNavigationComponents.kt` for responsive keyboard dismissal.
  - [x] Format codebase with `./harness.sh format`.
  - [x] Run `./harness.sh check`, `./harness.sh e2e`, and `./harness.sh build` to verify static analysis, tests, and build stability.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
  - `./harness.sh e2e`
  - `./harness.sh build`
- **Definition of done**: Manifest, dialogs, and top-level search inputs handle IME insets cleanly without animation timeouts, with all verification checks and builds passing green.

---

## 🎨 50. Modernize UI/UX Design System & Component Hierarchy
- **Goal**: Elevate application aesthetics with modern Material 3 styling, refined typography scale, language-specific syntax dot indicators, enriched color scheme supporting system dark mode, modernized navigation bar with pill indicators, and polished empty/card states adhering to 8dp grid and accessibility standards.
- **Files expected to change**: `Type.kt`, `Color.kt`, `Theme.kt`, `GistCard.kt`, `GistHubNavigationComponents.kt`, `HomeScreen.kt`, `GistListEmptyState.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Complete Material 3 typography hierarchy with optimal line heights, letter spacing, and semantic weights (`Type.kt`).
  - [x] Add developer-focused language badge colors (Kotlin, Python, JS, TS, Markdown, JSON, Shell) and semantic status colors (`Color.kt`).
  - [x] Support automatic system dark theme switching and rich dark slate/OLED GitHub tones in `Theme.kt`.
  - [x] Modernize `GistHubTopAppBar` with squircle brand mark, responsive search field, and clean offline warning banner (`GistHubNavigationComponents.kt`).
  - [x] Modernize `GistHubBottomBar` with pill-shaped active tab indicator and accessible 48dp touch targets (`GistHubNavigationComponents.kt`).
  - [x] Modernize `GistCard` with language dot badges, clean elevation, quick copy action, and dark terminal code preview container (`GistCard.kt`).
  - [x] Modernize `HomeScreen` search bar, filter chips, items counter badge, and sort dropdown (`HomeScreen.kt`).
  - [x] Enhance `GistListEmptyState` with squircle layered illustrations and clean button styling (`GistListEmptyState.kt`).
  - [x] Format all code with `./harness.sh format` and verify with `./harness.sh check` & `./harness.sh test`.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
  - `./harness.sh test`
  - `compile_applet`
- **Definition of done**: UI/UX modernized across theme, navigation, cards, and empty states while preserving all test tags and passing all linters and tests.

---

## 🛠️ 51. Standardize Agent Skills to agentskills.io Specification & llms.txt Best Practices
- **Goal**: Align all 24 skills under `.agents/skills/` with the open Agent Skills standard (https://agentskills.io) and `llms.txt` best practices.
- **Files expected to change**: `.agents/skills/*/SKILL.md`, `.agents/skills/android-intent-security/references/reporting-template.md`, `.agents/skills/agp-9-upgrade/references/*.md`, `.agents/skills/validate_skills.py`, `llms.txt`, `TASK.md`
- **Implementation checklist**:
  - [x] Audit all 24 skills for schema, name patterns, description constraints, metadata mapping, line count, and reference validity.
  - [x] Refactor `android-intent-security` to extract reporting guidelines and templates into `references/reporting-template.md`, bringing `SKILL.md` under 500 lines (499 lines).
  - [x] Standardize YAML frontmatter across all skills: ensure `name` matches directory (regex `^[a-z0-9]+(-[a-z0-9]+)*$`), `description` is 1-1024 characters with clear "what it does" and "when to use it" triggers without angle brackets, and `metadata` is strictly a string-to-string mapping (`map[string]string`).
  - [x] Resolve missing reference links in `agp-9-upgrade` by creating self-contained reference files (`references/release-notes.md`, `references/built-in-kotlin.md`).
  - [x] Implement automated validation script (`.agents/skills/validate_skills.py`) verifying 100% compliance with zero errors or warnings.
  - [x] Create root `llms.txt` cataloging all 24 agent skills by domain with trigger contexts and links following the standard.
  - [x] Run `./harness.sh check` and `./harness.sh test` to guarantee zero regressions.
- **Verification command(s)**:
  - `./.agents/skills/validate_skills.py`
  - `./harness.sh check`
- **Definition of done**: All 24 agent skills pass automated validation against the agentskills.io specification, all SKILL.md files are under 500 lines, descriptions contain trigger contexts, and a root llms.txt discovery index is established.

---

## 📋 52. Compose-Based List View for Fetched Gists with Three-State Sync Indicators (Synced, Pending, Error)
- **Goal**: Implement a polished, modular Jetpack Compose list view (`GistListView`) to display fetched Gists with comprehensive Material 3 item cards, robust empty/loading handling, and explicit visual sync status indicators (synced, pending, or error) using icons and test tags.
- **Files expected to change**: `app/src/main/java/com/example/ui/components/GistListView.kt`, `app/src/main/java/com/example/ui/components/GistSyncStateIndicator.kt`, `app/src/main/java/com/example/ui/components/GistCard.kt`, `app/src/main/java/com/example/ui/screens/HomeScreen.kt`, `app/src/test/java/com/example/GistListViewTest.kt`, `app/src/test/java/com/example/GistSyncStateIndicatorTest.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Extend `GistSyncStateIndicator.kt` to support explicit 3-state sync representations: `synced` (`Icons.Default.CloudDone`), `pending` (`Icons.Default.Sync`), and `error` (`Icons.Default.ErrorOutline`), with `SyncStatusType`, `resolveSyncStatusType`, `SyncStatusIcon`, and `SyncStatusChip`.
  - [x] Enhance `GistCard.kt` with `hasSyncError: Boolean` and `onSyncIndicatorClick` callback wiring into `GistSyncStateIndicator`.
  - [x] Build standalone, modular `GistListView.kt` displaying fetched Gists in a `LazyColumn` with stable keys (`key = { it.gist.id }`), swipe-to-dismiss deletion support, custom empty state handling, and sync error highlighting.
  - [x] Integrate `GistListView` into `HomeScreen.kt`, reducing `HomeScreen.kt` LOC from 544 to 457.
  - [x] Add high-fidelity JVM unit tests in `GistListViewTest.kt` verifying rendering, item actions, empty states, and synced/pending/error indicator icons.
  - [x] Update `GistSyncStateIndicatorTest.kt` to cover the new three-state sync resolution and icons.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
  - `./harness.sh e2e`
  - `compile_applet`
- **Definition of done**: A Compose-based list view displays fetched Gists with icon-based sync indicators for synced, pending, and error states, with full unit test coverage and clean pass across all quality checks.

---

## 💾 53. Offline JSON Backup Import/Restore Engine & String Resource Localization
- **Goal**: Implement a complete offline-first JSON backup restore/import engine (`BackupImporter`), wire it to the UI in `ConfigScreen` with an "Import JSON Backup" button, modularize configuration cards (`ConfigCards.kt`) to strictly enforce the 600 LOC ceiling, localize all hardcoded UI strings in `ConfigScreen`, and verify round-trip export/import via automated tests.
- **Files expected to change**: `BackupImporter.kt`, `GistViewModelExtensions.kt`, `strings.xml`, `ConfigCards.kt`, `ConfigScreen.kt`, `BackupImportExportTest.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Create `BackupImporter.kt` to parse `GistBackupPayload` from content URIs or file streams using Moshi, map back into `GistWithFiles`, and restore into Room DB with AES-256 encryption.
  - [x] Expose `importBackup(...)` extension in `GistViewModelExtensions.kt`.
  - [x] Add localized string resources in `strings.xml` for backup restore and all `ConfigScreen` UI elements, completely eliminating hardcoded strings per AGENTS.md Rule 5.2.
  - [x] Extract modular `ConfigCards.kt` (`ConfigThemeSelectorCard`, `ConfigBackupCard`, `ConfigConnectionStatusCard`) reducing `ConfigScreen.kt` to ~340 LOC, well below the 600 LOC ceiling.
  - [x] Add "Import JSON Backup" action button (`testTag("config_import_backup_button")`) with file picker launcher in `ConfigBackupCard`.
  - [x] Create comprehensive Robolectric unit test `BackupImportExportTest.kt` verifying export format, import restore into Room DB, malformed JSON error handling, and round-trip fidelity.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
  - `compile_applet`
- **Definition of done**: Backup import and export engine works end-to-end, all strings are localized, files are strictly under 600 LOC, test suite passes, and the application builds cleanly.

---

## 💬 54. Gist Comments Integration & Discussion Panel
- **Goal**: Implement full GitHub Gist comments interaction per SPEC.md Section 10, connecting `GitHubApiService` endpoints (`getGistComments`, `createGistComment`, `deleteGistComment`), repository operations, `GistViewModel` reactive state flows, `GistCommentsView` discussion composer & list, and integrating the "Comments" tab into `GistDetailScreen` with end-to-end unit and Robolectric test coverage.
- **Files expected to change**: `GitHubApiService.kt`, `GistRepositoryCommentsExtensions.kt`, `GistViewModelCommentsExtensions.kt`, `GistCommentsView.kt`, `GistDetailScreen.kt`, `GistRevisionViews.kt`, `GistCommentsTest.kt`, `TASK.md`
- **Implementation checklist**:
  - [x] Connect GitHub Gist Comments API endpoints (`GET /gists/{id}/comments`, `POST /gists/{id}/comments`, `DELETE /gists/{id}/comments/{comment_id}`).
  - [x] Expose reactive comments state flows (`commentsList`, `isLoadingComments`, `commentsError`, `isPostingComment`) and actions (`loadComments`, `postComment`, `deleteComment`, `clearCommentsState`) in `GistViewModel`.
  - [x] Integrate "Comments" navigation tab in `GistDetailScreen` tab bar (`detail_tab_comments`).
  - [x] Modularize `DetailRevisionsTabSection` in `GistRevisionViews.kt` keeping `GistDetailScreen.kt` well below the 600 LOC ceiling.
  - [x] Render `GistCommentsView` with comment composer, author metadata, markdown body, timestamp, error retry, and author delete actions.
  - [x] Update test fakes (`FakeGitHubApiService`, `DeleteConfirmationDialogTest`, `GistOfflineE2ETest`, `GistSortTest`, `NetworkConnectivityMonitorTest`, `StarredGistTest`, `SwipeToDeleteAndUndoTest`) to implement comment endpoints.
  - [x] Create comprehensive Robolectric test suite `GistCommentsTest.kt` verifying comments rendering, posting, deletion, error handling, empty state, and detail screen tab integration.
- **Verification command(s)**:
  - `./harness.sh format`
  - `./harness.sh check`
  - `./harness.sh build`
- **Definition of done**: Users can seamlessly read, compose, and manage comments on Gists, the Comments tab renders with web parity, and all tests and quality checks pass 100% green.






















