# System Design & Developer Architecture: Do Gist Hub

Welcome to the **Do Gist Hub** technical design specification. This document outlines the modern architectural blueprints, offline-first state synchronization engine, Material 3 "Clean Minimalism" styling guidelines, and developer workflow harness designed for sustainable high-frequency product iterations.

---

## 1. Architectural Blueprint (Modern Android 2026 MAP)

Do Gist Hub is built upon the modern **Unidirectional Data Flow (UDF)** model using **Jetpack Compose**, **Kotlin Coroutines/Flows**, and **MVVM (Model-View-ViewModel)** with an offline-first repository pattern.

```
                    ┌────────────────────────┐
                    │     Compose Screens    │
                    │   (Home, Vault, Sync)  │
                    └───────────┬────────────┘
                                │ ▲ UIState (StateFlow)
                      UI Actions │ │ 
                                ▼ │
                    ┌────────────────────────┐
                    │      GistViewModel     │
                    └───────────┬────────────┘
                                │ ▲ Flow<List<GistWithFiles>>
               Service Requests │ │
                                ▼ │
                    ┌────────────────────────┐
                    │     GistRepository     │
                    └─────┬────────────┬─────┘
                          │            │
          Read/Write Local│            │Remote Fetch/Sync
                          ▼            ▼
             ┌───────────────┐      ┌───────────────┐
             │ Room Database │      │  Retrofit APi │
             │ (SQLite Cache)│      │  (GitHub API) │
             └───────────────┘      └───────────────┘
```

### Presentation Layer
- **Jetpack Compose**: Declarative UI built exclusively with composables. No XML layouts are used.
- **Compose State**: Managed via screen-level ViewModels exposing immutable `UiState` models as `StateFlow`. Screens observe the state reactively using `collectAsStateWithLifecycle()` to minimize recomposition.
- **One-off Effects**: Transient, non-persistent events (e.g., navigation, snackbars, and toasts) are channeled via a dedicated, lifecycle-aware one-off effect channel (e.g., standard SharedFlow or Channel) to prevent state replay.
- **Type-safe Navigation**: Navigation route management occurs via state triggers without fragment transaction overhead.

### Domain / Repository Layer
- **GistRepository**: The single source of truth for all data operations. It mediates between the local Room SQLite cache, secure storage, and the remote GitHub Gist REST endpoints.
- **Isolation of APIs**: The UI layer never directly communicates with GitHub, Gemini, `SharedPreferences`, or the Android Keystore APIs. All integrations are isolated behind well-defined repository/datasource boundaries.
- **Structured Concurrency**: All background network, disk, or cryptographic operations run safely off the main thread using appropriate Coroutine dispatchers (`Dispatchers.IO`) and are scoped to lifecycle-aware contexts.
- **Mutable Singletons**: No mutable app-wide singleton state is permitted, except for correctly scoped dependency providers wired during application startup.

---

## 2. Offline-First Synchronization Protocol

To support a seamless offline coding workflow, Do Gist Hub implements an advanced dirty-flag-based syncing protocol.

```
                    ┌────────────────────────┐
                    │    User Saves Gist     │
                    └───────────┬────────────┘
                                │
                       Offline? │ Online?
                ┌───────────────┴───────────────┐
                ▼                               ▼
      ┌──────────────────┐            ┌──────────────────┐
      │   Mark Draft:    │            │ Push to GitHub   │
      │isLocalOnly = true│            │   Immediately    │
      └─────────┬────────┘            └─────────┬────────┘
                │                               │
                │                               ▼
                │                     ┌──────────────────┐
                │                     │   Save to Room   │
                │                     │isLocalOnly=false │
                │                     │  isDirty=false   │
                └─────────► Sync Trigger ──────┘
```

### State Flags
1. **`isLocalOnly = true` (Draft)**: Indicates the snippet was created offline and does not exist on GitHub yet.
2. **`isDirty = true` (Modified)**: Indicates the snippet exists on GitHub but has local edits that haven't been pushed.
3. **`isDirty = false, isLocalOnly = false` (Synced)**: Completely in sync with the GitHub cloud storage.

### Synchronization Rules
- **Incremental Sync**: When trigger events occur (tapping "Sync" or launching the app), the repository identifies all unsynchronized records (`isLocalOnly == true` or `isDirty == true`) and resolves them sequentially.
  - If `isLocalOnly`, execute a `POST /gists` to create the Gist on GitHub.
  - If `isDirty`, execute a `PATCH /gists/{id}` to update the Gist content on GitHub.
  - On network success, replace/update the local Room records with the official remote response, clearing both flags.
- **Conflict Avoidance**: During a remote fetch (`fetchFromRemote()`), incoming cloud entries never overwrite local rows where `isLocalOnly` or `isDirty` is true. This preserves local modifications during offline sessions.

---

## 3. Database Schema & Entity Relationships

The schema is divided into a **one-to-many** relationship mapping one `GistEntity` to multiple `GistFileEntity` records.

### `GistEntity` (Table: `gists`)
- `id`: String (Primary Key; matches GitHub Gist ID or generated `draft_UUID`)
- `description`: String (Nullable description of the gist)
- `htmlUrl`: String (URL to view the gist on the web)
- `url`: String (API endpoint URL)
- `createdAt`: String (ISO 8601 Timestamp)
- `updatedAt`: String (ISO 8601 Timestamp)
- `nodeId`: String (GitHub internal node graph ID)
- `isPublic`: Boolean (Visibility modifier)
- `isPinned`: Boolean (User preference for ordering at the top)
- `isLocalOnly`: Boolean (Local-only draft flag)
- `isDirty`: Boolean (Unsynced edits flag)
- `ownerLogin`: String (Owner user handle)
- `ownerId`: Int (Owner unique ID)
- `ownerAvatarUrl`: String (Owner profile image URL)
- `tags`: List<String> (Local tags associated with the Gist)

### `GistFileEntity` (Table: `gist_files`)
- `fileId`: String (Primary Key; generated UUID)
- `gistId`: String (Foreign Key linking to `gists.id` with cascade deletion)
- `filename`: String (Name of the file including extensions)
- `type`: String (MIME media type identifier)
- `language`: String (Programming language identified automatically)
- `rawUrl`: String (Direct URL to the raw file contents)
- `size`: Long (Byte size of content)
- `content`: String (The code or text content)

---

## 4. Visual Theming: "Clean Minimalism"

In alignment with Material Design 3 and a refined developer aesthetic, the app relies on a spacious, minimal, high-contrast palette.

| Component / Property | Value / Color Token | CSS Hex Equivalent |
|:---|:---|:---|
| **Primary Background** | `SlateBg` | `#FDFBFF` |
| **Borders & Dividers** | `SlateBorder` | `#CAC4D0` |
| **Interactive Accent** | `ActivePurple` | `#6750A4` |
| **Accent Containers** | `ActivePurpleContainer`| `#EADDFF` |
| **Secondary Text** | `GraySecondary` | `#49454F` |
| **Muted Annotations** | `GrayTertiary` | `#938F99` |
| **Local Draft Indicator**| `LightPinkContainer` | `#F2B8B5` |
| **Error / Destructive** | `ErrorRed` | `#B3261E` |

### Spacing Principles
- **Grid Alignment**: Built strictly on an 8dp grid (8dp, 16dp, 24dp padding/margin metrics).
- **Spacious Design**: Generous negative space surrounding lists and containers to allow comfortable reading and scanning.
- **Dashed Borders**: Unsynced creation prompts and interactive placeholder slots utilize dashed borders styled with `GrayTertiary` to clearly differentiate drafts from synchronized files.

---

## 5. Developer Coding Workflow Harness (Harness Engineering Reference)

Following the principles of **"Harness Engineering"** as outlined by Martin Fowler, a software project's development velocity is highly dependent on the quality of its surrounding developer harness. A harness is not merely a collection of scripts; it is an intentionally engineered environment designed to support high-fidelity local iterations, rapid feedback loops, and automated, decoupled validation.

### Key Harness Engineering Dimensions

1. **Local Fidelity & Dev-CI Congruence**
   - The local development harness must execute the exact same validation pathways and tasks as the remote Continuous Integration (CI) server.
   - `harness.sh` wraps standardized Gradle operations (`lintDebug`, `assembleDebug`, and `testDebugUnitTest`) using the same workspace configuration, guaranteeing that local verification translates to remote build success.

2. **Optimized Inner-Loop Feedback**
   - low-latency feedback loop enables developers to run quick static analysis (`format-check`/`lint`) before initiating heavier tasks like full compilation (`build`) or running test suites (`test`).

3. **Hermetic Isolation & Environmental Decoupling**
   - Automated verification must be hermetic and decoupled from flaky network dependencies or live databases.
   - The offline-first synchronization repository and local Room SQLite cache allow the unit and Robolectric test harness to run entirely in-memory and local-only.

4. **Graduated Verification Pipeline**
   - **Level 1 (`format-check`)**: Checks Spotless formatting.
   - **Level 2 (`lint`)**: Runs Android Lint checks.
   - **Level 3 (`unit`)**: Executes JVM unit tests.
   - **Level 4 (`build`)**: Compiles debug APK.

---

## 6. Multi-File Draft Editing Architecture
To match the official GitHub Gist web interface, Do Gist Hub supports managing multiple files under a single Gist entity.
- **UI Presentation**: Stacking input cards inside `DraftEditorDialog`. Each file block permits editing the filename and raw content, with full validation.
- **Addition & Deletion**: Interactive "+ Add File" buttons spawn fresh cards. Deletion is supported if more than one file exists, preventing empty gists.
- **Languages**: File language identification triggers reactively via filename extension checking.

---

## 7. Advanced Content Quality Checkers
Content is checked continuously in the Draft Editor, rendering real-time suggestions:
1. **Spell Checker**: Dictionary lookup matching technical and common terms.
2. **Grammar Checker**: Scans for duplicated adjacent words, checks for sentence-starting capitalization, and verifies standard punctuation spacing.
3. **External Link Checker**: Parses URLs, checking for correct structures. Flags insecure URLs (`http` instead of `https`), and provides inline upgrade actions to immediately replace insecure references.

---

## 8. Hybrid Local LLM Assistant
To assist code development offline, a specialized AI module operates on-device:
- **Offline Model**: Uses structured parsing, regex keyword indexing, and token-weight complexity analyzers to instantly output code complexity (1-10), maintainability profiles, descriptive summaries, language suggestions, and performance optimizations.
- **Online Fallback**: Connects directly to the Gemini API (`gemini-3.5-flash`) via REST with security-conscious keys managed via `BuildConfig`. When offline or keyless, it gracefully degrades to the local on-device engine without interrupting user flow.

---

## 9. High-Fidelity UI/UX Web Parity
To deliver a desktop-class experience mirroring the GitHub web interface:
- **Optional Markdown Editor/Preview**: Both the draft editor (`DraftEditorDialog`) and preview screens (`GistPreviewDialog`) support real-time Markdown rendering with an inline-formatting parser (Write vs Preview Markdown tabs) resembling the official GitHub web UI.
- **Full Description Visibility**: Gist descriptions are fully visible without truncation on the main Gist card and preview screen. The editor's description input field utilizes an enhanced, multi-line container with a raised minimum height (`100.dp`).
- **Persistent User Credentials**: The persistent storage subsystem saves the user profile avatar, login handle, and secure API personal access token together. On startup, user details are automatically loaded and hydrated.
- **Detailed Gist Creation Info**: Displays complete metadata in a dedicated card, including the owner's avatar, login handle, formatted Created At and Updated At ISO timestamps, public/secret status badge, and copyable Web URL link.

---

## 10. GitHub-Parity Rich Markdown Editor Specification (Gist Content & Comments Scope Only)
To provide a high-fidelity editor resembling the official GitHub comment and file editing interface, the markdown input panels support an interactive, action-oriented formatting environment scoped strictly for editing and viewing the **content of gist files** and **gist comments**:

### 10.1 Structural Layout & Visual Hierarchy
- **Header Navigation Tabs**: Dual-mode horizontal tabs: **Write** (default editing canvas) and **Preview** (instant markdown rendering output).
- **Markdown Formatting Toolbar**: Horizontal tool belt containing clean, recognizable vector icons aligned to standard Material symbols.
- **Content/Comment Input Box**: Spacious multi-line text input field with a raised minimum height (`120.dp`) and clean padding, featuring a neutral placeholder.
- **Status Indicator Footer**: Bottom-Left: "Markdown is supported" indicator; Bottom-Right: "Paste, drop, or click to add files" interactive attachment hint.

### 10.2 Formatting Helper Actions (Text Transformation Rules)
Clicking formatting buttons in the toolbar instantly mutates the active text field value by appending or wrapping markdown templates:
- Header, Bold, Italic, Blockquote, Code Block, Link, Numbered List, Bulleted List, Task List, File Attachment, Mention User, and Quote Reference.

### 10.3 Accessibility & Developer Identifiers
- **Minimum Interactive Size**: Every button element on the formatting toolbar maintains a touch target of at least `48.dp x 48.dp`.
- **Test Tags**: Buttons and input areas are annotated with consistent `snake_case` test tags.

---

## 11. Secure Token Storage (2026 Android Security Best Practices)
To prevent unauthorized access to personal access tokens and secure credentials, Do Gist Hub complies fully with modern 2026 Android platform security directives:
1. **MasterKey Hardware Backing**: Migration to the unified `MasterKey.Builder` to configure a secure hardware-backed key (stored within the Android Keystore system).
2. **EncryptedSharedPreferences Integration**: Saves personal access tokens (PAT) and user profile information in an encrypted preference file (`secure_gist_config_prefs`). Leverages AES256-SIV for filename/key encryption and AES256-GCM for preference values.
3. **Wipe Out & Account Reset**: On logout or account configuration reset, all locally stored credentials and account-specific cached data must be securely erased from both the database and encrypted preferences.
4. **Redaction & Logging Safety**: Tokens, API keys, HTTP authorization headers, gist contents, and user-identifying data must be redacted from logs, crash diagnostics, and UI error messages. Release builds must disable debug-only logging entirely.
5. **API Key Startup Isolation**: Validation of the Gemini API-key occurs lazily. The app startup must not crash or fail if Gemini is not configured.

---

## 12. Token Verification Button State Architecture
To deliver absolute visual and functional clarity, the "Verify Token" button transitions reactively through four distinct lifecycle states governed by a structured Kotlin sealed interface `TokenVerificationState`:
- `Idle`: "Verify Token & Autofill Profile", using standard primary color schemes.
- `Verifying`: Button disabled, displays a `CircularProgressIndicator` spinner, "Verifying Token...".
- `Success`: Transitions to secure Green (`Color(0xFF2E7D32)`) with a checkmark circle, showing a temporary success Toast.
- `Error`: Transitions to Material error color, 更新 text to "Verification Failed! Tap to Retry", displaying detailed diagnostic reasons.

---

## 13. Gist Data Behavior (Caching & Networking)

- **Cached First, Then Refresh**: The repository must emit previously cached Gist metadata promptly to ensure immediate rendering, subsequently starting an asynchronous network refresh.
- **Conditional Refresh Metadata**: Retrieve and store conditional HTTP headers (such as ETag and Last-Modified) to optimize network bandwidth and prevent rate limiting.
- **Lazy Loading of Expensive Fields**: Unbounded raw content, full revision histories, AI complexity reports, and syntax highlighting details must only be retrieved on explicit user action.
- **State Modelling**: State is explicitly modeled using a sealed state structure covering: Loading, Content, Empty, Offline cached content, Recoverable error with retry, and Authentication expired/Signed out.
- **Conflict Management**: Mutations must never silently overwrite remote modifications. In the event of a sync collision, the app must surface a recoverable conflict state to the user.

---

## 14. Performance & Accessibility Specifications

- **Main Thread Protection**: StrictMode checks are enabled in debug builds to detect accidental disk/network operations on the main thread.
- **Pagination & Bounded Feeds**: Feeds must load incrementally (using bounded page size constants) rather than pulling raw unbounded gist arrays.
- **Compose Stability**: Compose lists utilize stable element keys (e.g., Gist IDs) to prevent unnecessary item recomposition during data updates.
- **Baseline Profiles & Benchmarks**: Support for Baseline Profiles and Macrobenchmarks is scaffolded to optimize cold start and list scrolling performance.
- **TalkBack & Accessibility**: All interactive elements have non-null `contentDescription` attributes or text alternatives, maintaining a minimum touch target size of 48dp. Error, empty, and loading views are completely readable.
- **Responsive Layouts**: Supports compact (mobile) and expanded (tablet/foldable) layout configurations without replicating business logic, utilizing container-based scaling.

---

## 15. Quality, Testing, and Delivery Gate

- **Deterministic Gates**: Pull requests must pass formatting (`spotlessCheck`), static analysis (`detekt`), unit tests (`testDebugUnitTest`), and debug assembly (`assembleDebug`).
- **Release Verification**: Signed release builds are driven entirely via secure Gradle parameters mapped from environment variables or local ignored properties.
- **Release Assets**: CI automated releases generate and publish APKs, SHA-256 checksums, and version metadata cleanly on tag creation.
- **Targeted Test Coverage**: Every business repository, network mapper, ViewModel state transition, and error path requires corresponding Robolectric/JVM local unit tests.

---

## Code Quality and Coverage (Codacy)

### Static analysis
- All commits and pull requests are analyzed by Codacy using the official
  `codacy/codacy-analysis-cli-action` GitHub Action (pinned to major version v4).
- Codacy is the primary automated code review surface for Kotlin, Shell, and YAML
  in this repository.
- Static analysis results are reported as GitHub pull-request status checks and
  inline issue annotations on changed files.
- SARIF output is additionally uploaded to GitHub Security / Code Scanning for
  centralized alert management.
- The Codacy quality gate must show no new issues introduced in the diff for a
  pull request to be considered ready to merge.

### Coverage
- JaCoCo XML reports are generated during the unit-test phase via the
  `jacocoTestReportDebug` Gradle task.
- The official `codacy/codacy-coverage-reporter-action` GitHub Action uploads
  coverage data to Codacy on every push to `main` and every pull request.
- Coverage is tracked as a quality gate dimension: diff coverage and coverage
  variation are surfaced in PR summary comments via the Codacy GitHub integration.
- `CODACY_PROJECT_TOKEN` is stored exclusively as an encrypted GitHub Actions
  secret. It is never committed, logged, embedded in build config, or included
  in release artifacts.

### Quality gate configuration (Codacy dashboard)
Enable the following in the Codacy repository settings under Integrations → GitHub:
- Status checks (required)
- Issue annotations (requires status checks to be enabled first)
- Pull request summary comments

Enable the following quality gate rules in Gates:
- New issues introduced: 0
- Diff coverage is under: configurable threshold (suggested: 60% initially)

### Local developer workflow
- `./harness.sh coverage` — generates the JaCoCo XML report without uploading.
- `./harness.sh codacy` — uploads the report when `CODACY_PROJECT_TOKEN` is
  set in the local shell environment.
- `./harness.sh verify` — runs coverage upload automatically only when
  `CODACY_PROJECT_TOKEN` is present; silently skipped otherwise, so all local
  developer runs remain credential-free.

### What Codacy analyzes
- Kotlin source files under `app/src/main/` and `app/src/test/`
- Shell scripts including `harness.sh`
- YAML files including GitHub Actions workflows
- Excludes: generated directories, build outputs, Gradle cache directories

