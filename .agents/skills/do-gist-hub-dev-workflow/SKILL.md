---
name: do-gist-hub-dev-workflow
description: Strictly enforces Do Gist Hub's manual constructor injection, 600 LOC file limits, Room sync protocols, local test pyramid validation, and local harness commands.
---

# d.o.Gist Hub Developer Workflow Skill

This skill governs the development, extension, and verification of **Do Gist Hub**—an offline-first, reactive GitHub Gist client built using Kotlin, Jetpack Compose, and Room. 

Any AI coding agent modifying this codebase **MUST** read and adhere to this skill's specifications before writing code.

---

## 1. Core Architectural Constraints (Non-Negotiable)

### 1.0 Native Android Stack Mandate (No TypeScript / Web)
* **Native Kotlin Only**: **d.o.Gist Hub** is a 100% Native Android app. All data models, Room database entities, Retrofit DTOs, ViewModel states, and UI screens must be written in **Kotlin** (`.kt`).
* **Prohibition of Web/TypeScript**: Do NOT write or create TypeScript (`.ts`/`.tsx`), JavaScript, or Web framework files for app data models or business logic. All models must be represented as Kotlin `data class`es or `sealed class`/`interface` declarations under `com.example.*`.

### 1.1 No Hilt / Dependency Injection Frameworks
* **Rule**: Do **NOT** write `@Inject`, `@HiltViewModel`, `@AndroidEntryPoint`, or `@HiltAndroidApp` in any Kotlin files, even if Hilt is declared in dependencies.
* **Mechanism**: The project utilizes **manual constructor injection**.
* **Instantiation**: All services, databases, repositories, and workers must be instantiated inside `DoGistHubApp.kt` (or within its manual modules) and passed explicitly into factories like `GistViewModel.Factory`.

### 1.2 Strict Offline-First Synchronization Protocol
* **Rule**: Local mutations (insert, update, delete) must route exclusively through `GistRepository` using Room, keeping sync states accurate.
* **State Transition Flags**:
  - `isLocalOnly = true` (Draft): Created offline, does not exist on GitHub yet.
  - `isDirty = true` (Modified): Exists on GitHub, but has unpushed local modifications.
  - `isDirty = false, isLocalOnly = false` (Synced): Completely in sync with the GitHub cloud.
* **Reactive Sync Hook**: Do **NOT** trigger WorkManager workers or call raw remote APIs directly from ViewModels or Composables. ViewModels write to the repository, and `DoGistHubApp` reactively observes Room database flows to schedule `GistSyncWorker` upon detecting unsynchronized states.

### 1.3 Modularity & Code Quality
* **Maximum 600 LOC**: No Kotlin source file may exceed **600 Lines of Code (LOC)**. If a file is approaching this limit, extract helper methods, sub-composables, or business logic into separate, cohesive Kotlin files.
* **Extension Function Imports**: When extracting class methods into top-level Kotlin extension functions (e.g. `GistViewModelRevisionExtensions.kt`), update all consumer composables/dialogs with explicit imports for the new extension functions.
* **Detekt Suppressions for Internal Backing StateFlows**: When exposing backing `MutableStateFlow` properties as `internal` for extension function access within the same package, annotate them with `@Suppress("VariableNaming")` to satisfy Detekt static analysis.
* **No Magic Numbers or Hardcoded Settings**: Never hardcode endpoints, timeouts, or visual dimensions. Use `strings.xml`, central Kotlin constants, or Material 3 `Theme` tokens.
* **Build Concurrency Safety**: Never execute `compile_applet` or `lint_applet` while a background Gradle task (`./harness.sh check`, `./harness.sh format`, etc.) is in progress to prevent build container daemon lock contention. Always run build and check steps serially.

---

## 2. Testing Pyramid & Local Verification

Traditional instrumented testing via Emulators or Android Debug Bridge (ADB) is unavailable in our workspace. Consequently, verification relies entirely on **local, hermetic JVM testing** (Tier 1-3).

```
                ┌───────────────────────────────────┐
                │             Tier 1:               │
                │        Local JVM E2E              │
                │     Robolectric (Peak)            │
                ├───────────────────────────────────┤
                │             Tier 2:               │
                │    Integration & Feature Tests    │
                │      (Robolectric & Roborazzi)    │
                ├───────────────────────────────────┤
                │             Tier 3:               │
                │        Unit & Logic Tests         │
                │          (Pure JUnit)             │
                └───────────────────────────────────┘
```

* **Tier 1 (E2E)**: High-fidelity JVM E2E tests (`GistAppE2ETest`, `GistOfflineE2ETest`) verify the entire app slice (UI -> ViewModels -> Repository -> Room DB) with hermetic network fakes. **Every new major feature requires a Tier 1 E2E test.**
* **Tier 2 (Integration/UI)**: Verifies isolated composables, suggestions, or token verification state machines using Robolectric resources and Roborazzi screenshot verification.
* **Tier 3 (Unit)**: Verifies pure logic, regex parsers, and API interceptors via standard JUnit.

---

## 3. Developer Workflow Harness (`harness.sh`)

To maintain dev-CI congruence, always execute local validations using the unified developer harness script:

```bash
# Display help and usage instructions
./harness.sh help

# Run the full static analysis gate (wrapper check -> formatting check -> detekt -> lint -> unit tests)
./harness.sh check

# Apply spotless formatting automatically to fix style violations
./harness.sh format

# Install Git pre-push hook to block pushes of broken code
./harness.sh setup-hooks

# Run all test pyramid suites (E2E, Integration, and Unit tests)
./harness.sh test

# Verify complete build compilation
./harness.sh build
```

### 3.1 GitHub Pull Request Management (`gh` CLI)

Always manage GitHub PRs using the `gh` CLI tool:

- **Environment Setup**:
  ```bash
  which gh || (curl -sS -L https://github.com/cli/cli/releases/download/v2.52.0/gh_2.52.0_linux_amd64.tar.gz | tar -xz && mkdir -p bin && mv gh_2.52.0_linux_amd64/bin/gh bin/ && rm -rf gh_2.52.0_linux_amd64)
  export GH_TOKEN=$(git remote get-url origin | sed -n 's|.*https://\([^@]*\)@.*|\1|p')
  ```
- **New PR Creation (`gh pr create`)**:
  ```bash
  cat << 'EOF' > /tmp/pr_body.md
  ## Summary
  - ...
  EOF
  gh pr create --title "type: short description" --body-file /tmp/pr_body.md --base main
  ```
- **Get / Inspect PR Status & CI Checks (`gh pr view` / `gh pr checks`)**:
  ```bash
  gh pr view <pr_number> --json number,title,state,url,headRefName,baseRefName,statusCheckRollup
  gh pr checks <pr_number>
  ```
- **List PRs (`gh pr list`)**:
  ```bash
  gh pr list --state open --json number,title,headRefName,url
  ```
- **Update Existing PR (`gh pr edit`)**:
  ```bash
  gh pr edit <pr_number> --title "new title" --body-file /tmp/updated_body.md
  ```
- **Checkout PR (`gh pr checkout`)**:
  ```bash
  gh pr checkout <pr_number>
  ```

---

## 4. Definition of Done (DoD) Checklist

Before submitting or pushing any task:
1. [ ] **Format**: Run `./harness.sh format` to apply Ktlint styling.
2. [ ] **Pre-Push Hook**: Install pre-push hooks using `./harness.sh setup-hooks` to automate local code validation.
3. [ ] **Verify**: Ensure `./harness.sh check` compiles and passes format, Detekt, Lint, and Unit testing with 100% success.
4. [ ] **Test**: Run `./harness.sh test` to execute E2E and unit test suites locally.
5. [ ] **Build**: Run `./harness.sh build` to verify clean compilation.
6. [ ] **PR & CI Verification**: Push to your branch, manage the PR using `gh pr create` / `gh pr edit` / `gh pr view`, and verify via `gh pr view <number> --json statusCheckRollup` or `gh pr checks` that all GitHub Actions CI checks are 100% "green" with zero warnings or failures.

---

## 5. Master Orchestrator Swarm Pipeline Integration

When executing complex tasks on Do Gist Hub:
1. **Decompose**: Decompose the task into atomic To-Dos assigned to specialized personas (Architect, Diagnostics, UI, Sync Engine, QA, CI/Build).
2. **Code-First Inspection**: Execute a read-only diagnostic action (e.g. `view_file` or static analysis) before writing code changes.
3. **Verification**: Format via `./harness.sh format` and run `./harness.sh check` to ensure zero regressions across lint, detekt, spotless, and the local test pyramid.

