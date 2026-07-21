# d.o.Gist Hub

An offline-first, highly reactive, and feature-rich GitHub Gist client built for modern Android platforms using **Kotlin**, **Jetpack Compose**, **Room SQLite**, and a robust synchronization engine.

Designed with a **Clean Minimalism** design philosophy, d.o.Gist Hub allows developers to seamlessly create, edit, browse, and sync multi-file gists offline, featuring built-in content assistant helpers and on-device hybrid AI capabilities.

---

## 🚀 Key Features

* **Offline-First Synchronization Engine**: Create, edit, fork, and delete gists offline. Local mutations are tracked via precise sync state flags and reactively synchronized via a structured background scheduler when connectivity is restored.
* **Multi-File Draft Editor**: Manage multiple files under a single Gist entity inside a cohesive dialog. Features responsive filename changes, real-time validations, deletion protection, and automatic programming language detection.
* **Advanced Content Quality Assistant**: Analyzes drafts in real-time for spellings (technical-aware), grammar, and external URLs. Flags insecure links (`http` instead of `https`) and offers instant upgrade actions.
* **On-Device & Gemini Hybrid AI**: Analyzes code complexity (1-10), maintainability, and provides structural optimizations. Runs instantly on-device using custom heuristic analyzers, with dynamic cloud-based **Gemini 3.5-Flash** REST fallback.
* **Gist Forking & Error Recovery**: One-click duplication of public gists to the authenticated user's account with local database integration. Implements robust error classification for `422 Unprocessable` self-forking attempts.
* **Secure Token Storage**: Encrypts GitHub Personal Access Tokens (PATs) and user profile metadata using hardware-backed key providers (`MasterKey` + `EncryptedSharedPreferences`). Fully redacts tokens from logs, traces, and UI diagnostics.
* **Visual Sync Indicators**: Each local Gist item displays Material 3 badges with custom icons representing its state: `Synced` (Green Check), `Local Only` (Red CloudOff), or `Pending` (Orange Sync).
* **Clickable Gist Browser Links**: Metadata cards render high-contrast, copyable Web URLs that cleanly launch the system browser using lifecycle-aware platform URI handlers with full touch accessibility.

---

## 📁 Repository Structure

```text
do-github-gist-ai-studio-android/
├── .github/              # Automated PR quality validation & signed release workflows
├── .agents/skills/       # Custom AI developer instructions and capabilities
├── app/                  # Android Application module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── core/         # Core utilities (cryptography, network, safety sanitizers)
│   │   │   │   ├── data/         # Offline-first repositories, database cache, and REST endpoints
│   │   │   │   │   ├── local/    # Room SQLite database schemas, entities, and DAOs
│   │   │   │   │   ├── remote/   # Retrofit HTTP services, models, and auth interceptors
│   │   │   │   │   ├── repository/# GistRepository (mediator & single source of truth)
│   │   │   │   │   └── sync/     # WorkManager sync workers & lifecycle schedulers
│   │   │   │   ├── di/           # Manual dependency injection modules
│   │   │   │   └── ui/           # Jetpack Compose UI (Theme, Components, Screens, ViewModels)
│   │   │   │       ├── components/ # Reusable widgets (markdown editor, metadata cards, code diffs)
│   │   │   │       ├── screens/  # Navigation-routed views (Home, Vault, Sync, Details)
│   │   │   │       └── viewmodel/# UI State flows & presentation logic controllers
│   │   │   └── res/              # Android XML layouts, icons, vectors, and string resources
│   │   └── test/                 # JVM-based Robolectric Integration and Unit test suites
│   └── build.gradle.kts      # App module build parameters & dependencies
├── harness.sh                # Standardized developer quality gate and workflow script
├── SPEC.md                   # Technical design specifications, schema definitions, and visual tokens
└── TASK.md                   # Real-time task board tracking development and PR progress
```

---

## 🏗️ Architectural Blueprint

d.o.Gist Hub is structured around an offline-first, reactive architecture using Unidirectional Data Flow (UDF) constraints.

### 1. Presentation Model (UDF)
* **Jetpack Compose UI**: UI elements are entirely declarative, state-free, and reactive.
* **State Management**: Screens observe state from `GistViewModel` as read-only `StateFlow` exposed via lifecycle-aware collectors (`collectAsStateWithLifecycle()`).
* **Test Tags**: Interactive, text, and input widgets are decorated with explicit `Modifier.testTag("snake_case_name")` to support automated E2E JVM validations.

### 2. Dependency Injection
* **Manual Injection**: To guarantee predictability, the app enforces **manual constructor injection**. All databases, repositories, interceptors, and background sync factories are wired explicitly inside `DoGistHubApp.kt`.
* **Zero Annotation Frameworks**: Traditional dependency injection annotations (such as Hilt `@Inject` or `@AndroidEntryPoint`) are **strictly prohibited** in the source files.

### 3. Sync State Machine Protocol
Mutations (creation, file edits, and deletions) write directly to the local Room database using the following state transition matrix:

| Database State Flag | Representation | Sync Engine Behavior |
|:---|:---|:---|
| `isLocalOnly = true` | Local Draft | Engine executes `POST /gists` to create on GitHub. |
| `isDirty = true` | Local Changes | Engine executes `PATCH /gists/{id}` to update on GitHub. |
| `isLocalOnly=false, isDirty=false` | Fully Synced | Local cache is identical to GitHub cloud. |

* **Reactive Sync Hook**: ViewModels do not trigger workers or call APIs directly. Schedulers observe the database flow inside `DoGistHubApp` and automatically enqueue `GistSyncWorker` upon detecting unsynchronized records.

---

## 🛡️ Security & Redaction Best Practices

* **Keystore Encrypted Preferences**: Secure tokens are saved inside `secure_gist_config_prefs` using AES-GCM and AES-SIV encryption with hardware-backed keys.
* **Privacy Sanitization**: Tokens, private access headers, and sensitive gist contents are intercepted and redacted using `PrivacySanitizer.redact()` to prevent leakage in debug logs, crash reports, and error messages.
* **Complete State Erasure**: Triggering a logout or resetting credentials securely erases all cached databases, encrypted preferences, and WorkManager queues in parallel.

---

## 🛠️ Local Developer Onboarding

### 1. Prerequisites
* **JDK 17 or JDK 21** (Zulu OpenJDK recommended)
* **Android Studio Ladybug** (2024.2.1) or higher

### 2. Environment Configuration
Create a local configuration file named `.env` in the root directory:
```properties
# .env
# GITHUB_PAT: Required for remote Gist synchronization.
GITHUB_PAT=your_github_personal_access_token_here

# GEMINI_API_KEY: Optional. If present, enables advanced cloud-based analysis.
# If omitted or empty, the app falls back instantly to local on-device heuristics.
GEMINI_API_KEY=your_gemini_api_key_here
```

### 3. Unified Developer Workflow Harness (`harness.sh`)
To maintain dev-CI congruence, standard Gradle commands are superseded by `./harness.sh`. This POSIX-compliant script executes identical validation checks locally as the Continuous Integration (CI) environment.

```bash
# Display help and usage instructions
./harness.sh help

# Install Git pre-push hooks to block pushing of broken builds
./harness.sh setup-hooks

# Apply Spotless ktlint formatting automatically to fix style violations
./harness.sh format

# Run full static analysis (formatting, detekt, and lint checks)
./harness.sh check

# Run all Tier 1, 2, and 3 local JVM test suites (including E2E & screenshots)
./harness.sh test

# Generate unified JaCoCo local coverage report (HTML + XML)
./harness.sh coverage

# Compile and package a clean Debug APK
./harness.sh build

# Clean all local build and compilation caches
./harness.sh clean
```

---

## 🧪 Testing Pyramid (E2E JVM Testing Focus)

Traditional Android Emulator or ADB instrumented tests are not supported in our workspace. Instead, the codebase utilizes **local high-fidelity JVM Robolectric tests** as the peak validation gate.

### 1. Test Tiers
* **Tier 1: Local JVM E2E Tests**: Spawns the entire application stack (UI, ViewModels, Repository, and real Room SQLite databases) on the JVM using Robolectric. Exercises complex sync behavior under network fakes (`GistAppE2ETest`, `GistOfflineE2ETest`).
* **Tier 2: Component & Integration Tests**: Validates specific composables, quality checker suggestions, and state transitions (`DraftEditorAiSuggestionsTest`). Uses **Roborazzi** native graphics mode for visual screenshot regression testing.
* **Tier 3: Pure Logic Unit Tests**: Verifies core mappers, URL redactors, spelling engines, and API interceptors under isolation.

### 2. Visual Regression Tasks
* **Verify screenshots**:
  ```bash
  gradle :app:verifyRoborazziDebug
  ```
* **Record reference screenshots (if UI changes intentionally)**:
  ```bash
  gradle :app:recordRoborazziDebug
  ```

---

## 🛑 Strict Quality Guidelines

* **600 LOC Limit**: To prevent monolithic layouts and preserve testability, no Kotlin source file may exceed **600 lines of code**. If approaching this limit, refactor immediately by extracting components.
* **No Magic Numbers**: Avoid hardcoding configurations, endpoints, or layout dimensions. Use Material 3 token systems, central Kotlin constants, and `strings.xml`.
* **CI Congruence**: Pull requests are automatically analyzed on pushes and branch updates by GitHub Actions. Every verification gate, Detekt rule, and unit test must pass completely with zero warnings.
