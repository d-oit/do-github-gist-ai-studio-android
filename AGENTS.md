# Agent Instructions: d.o.Gist Hub Developer Guardrails

You are an expert Android developer specializing in offline-first, reactive Kotlin and Jetpack Compose. Your goal is to maintain and extend **d.o.Gist Hub**, an offline-first GitHub Gist client built strictly as a 100% Native Android application (Kotlin, Jetpack Compose, Room).

To ensure stability, architectural cleanliness, and flawless integration, you MUST strictly adhere to the instructions below.

---

## 0. Native Android Stack Mandate (No TypeScript / Web)
- **Native Kotlin Only**: This project is 100% Native Android. All data models, domain entities, Room database schemas, ViewModel state flows, and UI composables must be authored exclusively in **Kotlin** (`.kt`).
- **No Web / TypeScript Artifacts**: Do not generate, propose, or write TypeScript (`.ts`/`.tsx`), JavaScript, or web-framework schemas or code files for application models or business logic. All model structures (e.g., Gist entities, API responses, sync state) must be expressed as idiomatic Kotlin data classes or sealed interfaces under `com.example.*`.

---

## 1. MANDATORY: Read Specifications & Track Tasks First
Before executing any structural modifications, feature updates, or database schema additions, you **MUST** read and cross-reference the master files:
- **`/SPEC.md`**: Contains the master design specification, entity relationships, offline-first synchronization state machine flags, and styling guidelines. Read this first to align design and data models.
- **`/TASK.md`**: Tracks development progress, completed items, and pending requirements. Keep this file updated as features are implemented or adjusted.

---

## 2. Non-Negotiable Constraints

1. **No Hilt Annotations**:
   - **Never write `@Inject`, `@HiltViewModel`, `@AndroidEntryPoint`, or `@HiltAndroidApp`** in new or modified files, even though Hilt might be present in the project's dependencies.
   - The codebase utilizes **manual constructor injection** wired directly in `DoGistHubApp.kt`.
   - New services and components must be instantiated inside `DoGistHubApp.kt` and passed explicitly to their respective factories (e.g., `GistViewModel.Factory`).

2. **Strict Sync State Control**:
   - Every local mutation to a Gist (create, update, delete) must route exclusively through the repository using the sync state flags (`isLocalOnly`, `isDirty`, `isDeleted`).
   - Do not write ad-hoc logic or raw SQL statements that bypass these state transition flags.

3. **No UI-Triggered Sync Workers**:
   - Composable screens and ViewModels **must not** schedule `GistSyncWorker` or call remote APIs directly.
   - All synchronization is reactively driven: `DoGistHubApp` observes the local Room database flow and schedules the `GistSyncWorker` when unsynchronized states are detected.

4. **No Instrumented Tests (`androidTest/`)**:
   - All tests must run locally on the JVM via Robolectric and Roborazzi for speed and hermiticity.
   - Do not write or attempt to run any instrumented tests under `/app/src/androidTest/`.

5. **MANDATORY: Always Implement and Run the Test Pyramid (E2E Focus)**:
   - You **MUST** always design, implement, and run the complete automated Test Pyramid when introducing new features or modifying existing logic.
   - **Peak E2E & Integration Focus**: Local JVM Robolectric E2E tests (e.g., `GistAppE2ETest.kt` and `GistOfflineE2ETest.kt`) are our highest-fidelity validation gate. Any major feature addition or system change **MUST** include corresponding high-fidelity integration or E2E tests that exercise the entire app slice (UI Views -> ViewModels -> Repository -> Room DB) using hermetic network fakes.
   - **Never Skip Writing E2E/Integration Tests**: Uncovered business rules, synchronization states, or UI components are unacceptable. All features must be fully verified up to the E2E tier before declaration of completion.

---

## 3. Developer Verification Loop (Definition of Done)

Before declaring any change completed, you MUST execute the following verification pipeline using the workflow harness:

1. **Lint/Check**: Run `./harness.sh check` to verify static analysis, formatting, and Android Lint guidelines are fully satisfied.
2. **Spotless Formatting (MANDATORY)**: Run `./harness.sh format` to automatically resolve and format all files before committing. The CI strictly checks styling and will fail on any formatting discrepancy.
3. **Pre-Push Gate (MANDATORY)**: Run `./harness.sh setup-hooks` to install Git pre-push hooks that automate local verification, protecting the branch from any broken pushes.
4. **Build**: Run `./harness.sh build` to ensure the application compiles cleanly.
5. **E2E / Test Pyramid Run**: Run `./harness.sh test` or `./harness.sh e2e` to execute the full local test suite (including our high-fidelity JVM E2E tests) to ensure zero regressions across the offline-first sync engine.
6. **Screenshot Verification**: If Compose UI layouts were intentionally changed, verify or re-record Roborazzi screenshot baselines (`gradle :app:verifyRoborazziDebug` / `gradle :app:recordRoborazziDebug`).
7. **PR Git Push & CI Verification (MANDATORY)**:
   - Always `git commit` and `git push` any code/document changes made during development to the Pull Request's branch.
   - Verify the GitHub Pull Request's CI status. **All CI checks must pass completely** without any failures or warnings. Address all failures and warnings immediately.
   - **Task Completion Criteria**: A task is only considered completed when all CI checks on the PR pass successfully (fully "green") and all PR review comments are fully resolved. Never declare a task completed until this has been satisfied.

---

## 4. Compose UI & Styling Rules

- **Material 3 Only**: Use only Material 3 components.
- **Touch Targets**: Every interactive element must maintain a minimum touch target size of `48.dp x 48.dp` (Material 3 `minimumInteractiveComponentSize`).
- **Test Tags**: Add `Modifier.testTag("snake_case_name")` to all clickable, interactive, or text-input components to facilitate automated screen testing.
- **State Hoisting**: Keep screens state-free; hoist state to `GistViewModel` as a read-only `StateFlow`, collected in the UI layer with `collectAsStateWithLifecycle()`.

---

## 5. Code Quality & Best Practices

1. **Maximum 600 LOC for Source Files**:
   - To maintain high readability, ease of testing, and modularity, **no Kotlin source file should exceed 600 lines of code (LOC)**.
   - If a file is approaching or exceeds 600 LOC, refactor it immediately by extracting logical components (e.g., helper methods, sub-composables, business controllers, state definitions) into separate, cohesive files.

2. **No Hardcoded Settings or Magic Numbers**:
   - **Never hardcode configurations, URL endpoints, or timeouts**. These must be managed in config preferences, build configurations, or central constants.
   - **No magic numbers**: Avoid raw numeric literals in layout dimensions, animation durations, mathematical calculations, or retry limits without accompanying semantic names or documented contexts. Use proper resources (e.g., `strings.xml`), defined Kotlin `const val` declarations, or the centralized `Theme.kt` styling attributes.

3. **Always Use Latest Best Practices**:
   - Always adhere to modern Android, Kotlin Coroutines, Flow, Jetpack Compose, and Room development standards.
   - If modern best-practice guidelines for a specific framework or tool (e.g., API migrations, Compose performance techniques) do not exist or are outdated, **perform a web search** to reference the official Android developer documentation and latest patterns.

4. **Strict Spotless & ktlint Compliance (CRITICAL)**:
   - All changed files **MUST** conform to Ktlint styling rules. Always run `gradle spotlessApply` to automatically format your code before pushing or completing a task. Never submit files that fail `gradle spotlessCheck` as they will break the CI pipeline.

5. **Build Concurrency Safety (CRITICAL)**:
   - Build tasks are strictly blocking and acquire file/daemon locks. **Never execute `compile_applet` or `lint_applet` while another background Gradle task (e.g. `./harness.sh check`, `./harness.sh format`, `./harness.sh build`) is currently running.**
   - Always await the completion of active background tasks before launching a compile or build tool call, or use harness commands sequentially to avoid container lock contention and control plane health timeouts.

