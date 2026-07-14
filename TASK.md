# Task Board: Do Gist Hub

An offline-first GitHub Gist client utilizing **Jetpack Compose**, a local **Room SQLite cache**, reactive **WorkManager synchronization**, and **manual constructor injection**. This board tracks the production-ready implementation milestones.

---

## 📂 1. Harness and Local Developer Quality Gate

- **Goal**: Establish a unified, POSIX-compliant script as the single local and CI quality gate.
- **Files expected to change**: `harness.sh`
- **Implementation checklist**:
  - [x] Create POSIX-compatible Bash script (`#!/usr/bin/env bash`, `set -euo pipefail`).
  - [x] Implement subcommands: `help`, `verify`, `unit`, `lint`, `format-check`, `build`, `test`, `clean`.
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

### 3.7 README Codacy documentation
**Goal:** Document onboarding, local usage, and badge.
**Files:** `README.md`
**Checklist:**
- [x] Add a Codacy grade badge using the official badge URL pattern:
      `https://app.codacy.com/project/badge/Grade/<PROJECT_UUID>`
      Use a placeholder UUID with a TODO comment until the real one is available.
- [x] Add "Code Quality" section with onboarding steps (see section 9 below).
- [x] Document `./harness.sh coverage` and `./harness.sh codacy`.
- [x] State that `CODACY_PROJECT_TOKEN` must never be committed.
**Done when:** README section is present, accurate, and badge renders.

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
- **Files expected to change**: Secure credential storage implementations, logging classes, view models, logout routines.
- **Implementation checklist**:
  - [x] Implement `CredentialStore` utilizing MasterKey-based `EncryptedSharedPreferences` with safe fallback.
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
- **Files expected to change**: `.github/workflows/ci.yml`
- **Implementation checklist**:
  - [x] Define triggering paths (`pull_request`, `push` to main).
  - [x] Run graduated pipeline checks: Format -> Lint -> Unit -> Build.
  - [x] Configure dependency caches to optimize CI duration.
  - [x] Upload static analysis and test reports on build failures.
- **Verification command(s)**:
  - CI pipeline execution on branch push.
- **Definition of done**: All tests, formatting rules, and lints must pass in CI environment.
- **Explicit non-goals**: Automatically applying auto-fixes or commits on behalf of the user in CI.

---

## 📂 10. Signed GitHub Release Workflow

- **Goal**: Automate secure release builds generating signed APKs and computing checksums.
- **Files expected to change**: `.github/workflows/release.yml`
- **Implementation checklist**:
  - [x] Configure trigger on tag matching `v*`.
  - [x] Map GitHub secrets to Gradle properties for secure signing.
  - [x] Build signed release APK and compute SHA-256 checksums.
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

- **Goal**: Implement a fully featured dedicated Composable screen for creating new Gists and persist them locally via Room `GistDao`.
- **Files expected to change**: `CreateGistScreen.kt`, `GistHubAppScreen.kt`, `GistRepository.kt`, `GistViewModel.kt`
- **Implementation checklist**:
  - [x] Create a dedicated screen `CreateGistScreen.kt` with fields for description, public status, filename, and file content.
  - [x] Integrate Material 3 input fields and clean validations.
  - [x] Assign `testTag` identifiers to all input fields, buttons, and switches for testing.
  - [x] Wire the submit action directly to `GistDao.upsertGistWithFiles(...)` inside a coroutine scope.
  - [x] Expose local-only sync flags (`isLocalOnly = true`) on database entries.
  - [x] Integrate the creation screen seamlessly with navigation and entry points in `GistHubAppScreen.kt`.
- **Verification command(s)**:
  - `./harness.sh verify`
- **Definition of done**: Selecting the "New Draft" floating action button opens the new screen, validates inputs, and persists a local-only draft directly in the SQLite cache using Room.

