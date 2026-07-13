# Task Board: Do Gist Hub

An offline-first GitHub Gist client utilizing **Jetpack Compose**, a local **Room SQLite cache**, reactive **WorkManager synchronization**, and **manual constructor injection**. This dashboard tracks the development progress of implemented modules and pending feature requirements.

---

## 📊 Project Status Summary

| Subsystem | Progress | Status |
| :--- | :---: | :--- |
| **Architecture & Core Framework** | 100% | ![Completed](https://img.shields.io/badge/Status-Completed-success?style=flat-square) |
| **Offline-First Sync Engine** | 100% | ![Completed](https://img.shields.io/badge/Status-Completed-success?style=flat-square) |
| **Visual Theming & User Interface** | 100% | ![Completed](https://img.shields.io/badge/Status-Completed-success?style=flat-square) |
| **Credential Management & Storage** | 100% | ![Completed](https://img.shields.io/badge/Status-Completed-success?style=flat-square) |
| **Developer Workflow & Testing** | 100% | ![Completed](https://img.shields.io/badge/Status-Completed-success?style=flat-square) |

---

## 📂 1. Architecture & Core Framework

Architectural blueprints ensuring clean data flow and dependency decoupling.

- [x] **Unidirectional Data Flow (UDF)**: Compose screens reactively observe read-only `StateFlow` from `GistViewModel` collected via `collectAsStateWithLifecycle()`.
- [x] **Manual Constructor Injection**: Strict avoidance of Hilt annotations (`@Inject`, `@HiltViewModel`, etc.). All dependencies (database, repository, preferences) are manually wired in `DoGistHubApp.kt` and passed explicitly to `GistViewModel.Factory`.
- [x] **Relational Schema Design**: Local Room cache mapping a one-to-many relationship of one `GistEntity` to multiple `GistFileEntity` records.
- [x] **Repository Mediation Pattern**: `GistRepository` acts as the single source of truth, mediating between the local Room SQLite cache and the remote GitHub Gist API.

---

## 🔄 2. Offline-First Sync Protocol

Advanced synchronization engine supporting seamless offline work states.

- [x] **Sync State Machine Flags**: Correct transitions and tracking of state flags on `GistEntity`:
  - `isLocalOnly = true`: Local draft offline-created snippet.
  - `isDirty = true`: Unsynced local edits on an existing remote snippet.
  - `isDeleted = true`: Pending deletion synced when online.
- [x] **Reactive Sync Observation**: `DoGistHubApp` observes the repository's `unsynchronizedGists` flow and triggers `GistSyncWorker` reactively instead of VM/UI layers calling sync.
- [x] **WorkManager Background Sync**: `GistSyncWorker` handles background network pushing of local drafts and updates.
- [x] **Conflict Avoidance**: Fetching from remote API never overwrites active local edits (`isLocalOnly` or `isDirty`).

---

## 🎨 3. Visual Theming & User Interface

Material Design 3 visual layouts paired with "Clean Minimalism" aesthetic styles.

- [x] **M3 Custom Color Palette**:
  - `SlateBg` (`#FDFBFF`) as primary background.
  - `SlateBorder` (`#CAC4D0`) for containers and separators.
  - `ActivePurple` (`#6750A4`) as primary accent color.
  - `GraySecondary` (`#49454F`) & `GrayTertiary` (`#938F99`) for text density and annotations.
- [x] **Navigation Setup**: Multi-tab interface (Home, Vault, Sync, Config) embedded cleanly inside a unified `Scaffold` layout.
- [x] **Touch Target Compliance**: Ensuring all interactive clickables have a minimum size of `48.dp x 48.dp`.
- [x] **Interactive Components**:
  - Gist Search/Filter query field matching filenames, content, and descriptions.
  - Full-screen Code Preview modal using dark developer style terminal backgrounds.
  - Interactive Spelling & Grammar helper dialog suggesting corrections directly inside drafts.
  - Pinning UI system to favorite specific Gists locally.

---

## 🔒 4. Credential Management & Storage

Secure, local user state initialization.

- [x] **Personal Access Token (PAT) Cache**: Safe management of GitHub authorization headers using private SharedPreferences.
- [x] **User Profiles Autoload**: Automated profile loading and profile picture (avatar) caching on startup.
- [x] **Config Maintenance**: Complete configuration reset mechanics ("Clear Configuration") wiping local cache data and state flows cleanly.

---

## 🧪 5. Developer Harness & Verification Loop

Continuous quality verification pipeline ensuring strict adherence to the Fowler developer harness model.

- [x] **Graduated Pipeline Tasks (`./harness.sh`)**:
  - `check`: Static analysis and Android Lint checking to catch formatting/structural anomalies.
  - `build`: Compiler level verification to guarantee correct application incremental builds.
  - `test`: Automated JVM unit testing.
- [x] **Robolectric Integration Testing**: Full mock-ready local unit and E2E simulation testing under `src/test/` to verify ViewModel, repositories, and preferences without real network or emulator overhead.
- [x] **Roborazzi Visual Screenshot Tests**: Automated pixel-perfect snapshot verification to catch UI regressions on layout adjustments.
- [x] **Instrumentation Block**: Absolute avoidance of slow, flaky emulator-based `androidTest/` suites.

---

## 📝 Running Validation Pipelines

To run validation checks and update project status:

```bash
# 1. Run static code checks and formatting linting
./harness.sh check

# 2. Compile the debug build of the application
./harness.sh build

# 3. Execute all unit and Robolectric tests
./harness.sh test
```

---

## 🚀 6. Advanced Parity & Local AI Features

High-impact features aligning with official GitHub Gist web parity, offline intelligence, and interactive checkers.

- [x] **Multi-File Draft Editing**: Allow creating, editing, and deleting multiple files inside a single Gist draft with dynamic file cards stacked inside the editor.
- [x] **Advanced Content Quality Checkers**:
  - **Spell Checker**: Dictionary lookup covering core developer concepts and standard typos.
  - **Grammar Checker**: Find duplicate words, improper sentence capitalization, and punctuation spacing.
  - **External Link Checker**: Parse URLs in description and content; flag insecure HTTP links and provide instant upgrade-to-HTTPS corrections.
- [x] **Hybrid Local LLM Assistant**:
  - **Offline Intelligence**: Evaluate cyclomatic complexity, estimate maintainability, create summaries, and suggest optimization actions 100% locally.
  - **Online API Integration**: If online and API key is present, communicate with `gemini-3.5-flash` using a REST client for deeper insights, with automatic offline fallback.
- [x] **Developer Loop Verification**: Verify compile and test targets using the harness, ensuring 100% compliance.
- [x] **High-Fidelity UI/UX Web Parity**:
  - **Optional Markdown Editor/Preview**: File cards in both `DraftEditorDialog` and `GistPreviewDialog` allow toggling between raw code writing/display and highly styled real-time Markdown preview rendering (Write vs Preview Markdown) resembling the GitHub web UI.
  - **Full Description Visibility & Raised Field Height**: Description lines are fully visible without truncation on the main Gist card and preview screen, and the editor's description field has raised minimum height with multi-line input.
  - **Persistent User Credentials**: Persistent storage of the user profile avatar and login together with the Personal Access Token, auto-loading user details from GitHub on startup if absent.
  - **Detailed Gist Creation Info**: Displays complete metadata (avatar, login handle, formatted Created At and Updated At timestamps, public/secret status badge, and copyable Web URL link) in the preview dialog.
- [x] **GitHub-Parity Rich Markdown Editor & Formatting Toolbar (Gist Content & Comments Scope Only)**:
  - [x] Implement dual-tab horizontal navigation (`Write` vs `Preview`) with bottom line indicator and neutral placeholder texts for file contents and gist comments.
  - [x] Render a specialized 12-action formatting toolbar containing standard vector iconography (`H`, `B`, `I`, quote, code, link, numbered list, bulleted list, task list, attachment, mention, and quote reply).
  - [x] Wire syntax helpers to inject appropriate markdown tags and templates at the active selection or text end of gist file contents or comments.
  - [x] Design decorative status footers containing left-aligned "Markdown is supported" logo indicator and right-aligned "Paste, drop, or click to add files" interactive attachment hint.
  - [x] Enforce Material 3 touch target compliance (minimum 48dp) and annotate components with custom accessibility semantics and `snake_case` test tags.
- [x] **Live GitHub API Gists Explorer**:
  - [x] Added `fetchGistsDirectly()` to `GistRepository` performing secure raw REST queries to the GitHub Gists API endpoint with the stored personal access token (PAT).
  - [x] Wired reactive states (`remoteGists`, `isFetchingRemote`, `remoteError`) in `GistViewModel` to cache and load direct cloud records cleanly on startup and demand.
  - [x] Built the `GitHubGistApiList` Material 3 list component showing Gist titles (resolved filename), descriptions, metadata, visibility badges, file count indicators, and creator avatar/profile references.
  - [x] Integrated the live explorer directly inside the scrollable container of the Sync tab screen with full refresh and error handling states.
- [x] **2026 Android Security Best Practices & Compliance**:
  - [x] Migrated deprecated `MasterKeys` Keystore API to standard unified `MasterKey.Builder` to ensure robust hardware-backed cryptographic co-processor protection (StrongBox/TEE).
  - [x] Integrated `EncryptedSharedPreferences` for secure preference file storage (`secure_gist_config_prefs`), encrypting stored GitHub Personal Access Tokens and profile values using AES-256 (SIV for keys, GCM for values).
  - [x] Ensured graceful fallback mechanisms to standard private shared preferences if system cryptographic keystore initialization fails.
  - [x] Implemented on-boot migration of plaintext configurations to the encrypted storage pool with subsequent plain files cleanups.
- [x] **Verify Token Dynamic Button State Implementation**:
  - [x] Declared the structured Kotlin sealed interface `TokenVerificationState` representing Idle, Verifying, Success, and Error states.
  - [x] Integrated the token verification state machine flow within `GistViewModel.kt` to drive state transitions reactively.
  - [x] Created dynamic UI string resources in `strings.xml` to avoid hardcoded text strings in source files.
  - [x] Configured rich Material 3 design transitions on the button layout in `ConfigScreen.kt` using custom color shifts (e.g., Success Green, Error Red) and contextual vector icons.
  - [x] Wired a success-triggered `LaunchedEffect` to display transient Toast notifications confirming verified token security.
- [x] **First File Default Filename & Restricted Addition Parity**:
  - [x] Configured the Gist draft creation and edit initialization flows to prepopulate the first file with the default filename `"gistfile1.md"`.
  - [x] Lifted restriction of requiring the first file to be named `"gistfile1.md"` to add more files. Users can now rename the first file to anything (e.g., custom code file extensions) and add multi-file drafts unconditionally.
- [x] **Immediate & Offline-First Gist Starring/Unstarring**:
  - [x] Expanded `GistEntity` schema to track `isStarred` and `isStarredDirty` fields with versioned database migration support.
  - [x] Added `checkIsStarred`, `starGist`, and `unstarGist` endpoints to `GitHubApiService` and wired them securely in `GistRepository`.
  - [x] Designed offline-first and online-proactive starring sync state machine. If online, toggling star pushes instantly to GitHub; if offline, it flags locally and syncs reactively during background Worker cycles.
  - [x] Integrated star buttons, dynamic status badges, and Material Symbols icons into `GistCard` across `HomeScreen` and `VaultScreen`.
  - [x] Added rigorous Robolectric JVM integration tests (`test_toggleStar_online_immediate_sync` and `test_toggleStar_offline_delayed_sync`) verifying state flow behaviors.
- [x] **Gist Revisions & Split/Unified Diff Parity**:
  - [x] Integrated GitHub Gist revisions API (`GET /gists/{id}` history fields and `GET /gists/{id}/{sha}` for specific revisions).
  - [x] Added `fetchRemoteGist` and `fetchRemoteGistRevision` to both `GistRepository` and `GistViewModel`.
  - [x] Added horizontal tab switcher inside `GistPreviewDialog` for `Files` vs `Revisions` navigation.
  - [x] Formatted and displayed revision lists with author avatar, login, committed timestamp, additions (green `+`), and deletions (red `-`).
  - [x] Created high-fidelity line-based LCS (Longest Common Subsequence) diff generator in pure Kotlin with aligned side-by-side (Split) or inline (Unified) options.
  - [x] Managed horizontal scroll states inside side-by-side split cards to support clean mobile viewing without truncation.
- [x] **Local Persistent Search & Filter Parity**:
  - [x] Designed and implemented beautiful persistent Material 3-compliant search bars at the top of both `HomeScreen` and `VaultScreen`.
  - [x] Configured rounded pill-shape text fields featuring leading search icons, trailing close icons with instant query clearing, and customized surface variant container backgrounds.
  - [x] Refined the search matching algorithm to scan through all files associated with each Gist, matching on any file name or file content in addition to Gist descriptions (case-insensitive).
  - [x] Annotated search components with standard accessibility descriptors and unique snake_case test tags (`home_search_bar` and `vault_search_bar`) for robust verification.
- [x] **Full-Fidelity JSON Backup Export**:
  - [x] Formulated version-controlled `GistBackupPayload`, `GistBackupItem`, and `GistBackupFile` models matching Room relational schemas.
  - [x] Integrated background `exportBackup` routine utilizing runtime Moshi serialization to construct highly-formatted backup JSON payloads.
  - [x] Handled Uri resolving robustly across both local `file://` schemes for test isolation and `content://` schemes for official Android System file selection.
  - [x] Developed custom interactive backup export card UI inside `ConfigScreen` with custom accessibility content descriptions and `config_backup_button` test tags.
  - [x] Wrote robust Robolectric unit tests to verify database extraction, Moshi formatting, and stream-write operations.
- [x] **Lightweight Code Syntax Highlighter**:
  - [x] Designed and implemented a high-performance regex-based syntax tokenizer supporting multiple languages (Kotlin, Java, JavaScript, TypeScript, Python, XML, HTML, CSS, SQL, JSON) with an automatic safe generic fallback.
  - [x] Embedded the custom code highlighter inside the Gist preview modal in `GistPreviewDialog.kt`, automatically styling tokens with modern, readable color palettes on dark containers.
  - [x] Integrated syntax highlighting for code blocks inside markdown previews (e.g. ````kotlin` or ````json`), parsing language tags automatically.
  - [x] Coded memory-efficient caching using Compose `remember(content, lang)` blocks to avoid unnecessary parsing during state recomposition.
  - [x] Authored robust Robolectric unit tests inside `SyntaxHighlighterTest.kt` verifying exact token mapping, italicised comments, and fallback behaviors.
- [x] **Local Gists Tagging & Grouping System**:
  - [x] Modified `GistEntity` database schema to support a list of tags (`tags: List<String> = emptyList()`) with fallback destructive migrations handling automatically.
  - [x] Implemented local database queries in `GistDao` to update tags for Gists dynamically.
  - [x] Added persistent tagging support in `GistRepository` during Gist draft creation and local modification, preserving tags on remote sync response writes.
  - [x] Integrated a comma-separated tag input field in `DraftEditorDialog` with custom accessibility parameters and `editor_tags` test tags.
  - [x] Rendered tag badges on the Gist list card prefixed with `#` in modern Material 3 `secondaryContainer` colors.
  - [x] Configured scrollable tag filtering chips below the home screen search bar for instant, zero-latency reactive tag filtering.
- [x] **Periodic Draft Auto-Save & Recovery**:
  - [x] Defined `AutoSavedDraft` and `DraftFile` data models inside `ConfigPrefs.kt` with dynamic JSON Moshi reflection support.
  - [x] Added `saveAutoSavedDraft`, `getAutoSavedDraft`, and `clearAutoSavedDraft` methods to `ConfigPrefs` utilizing MasterKey-protected encrypted preferences fallback.
  - [x] Implemented `GistViewModel` helpers to load, update, and delete active auto-save sessions, clearing draft states on successful creates or updates.
  - [x] Built debounced `LaunchedEffect` listener in `DraftEditorDialog` saving current changes after 3 seconds of keystroke/input inactivity.
  - [x] Formulated modern, highly reassuring "Auto-saved" status check indicators next to the dialog's header title.
  - [x] Engineered custom Material 3 recovery banner Card with distinct "Restore" and "Discard" actions with unique snake_case test tags.
  - [x] Authored robust Robolectric unit tests in `AutoSaveDraftTest.kt` verifying serializing, persistent loading, and clearing behaviors.
- [x] **Friendly Empty-State Placeholders & Fetch CTA**:
  - [x] Implemented high-fidelity empty-state placeholders inside `HomeScreen.kt` distinguishing between an empty local cache (`gists.isEmpty()`) and empty search/filter results (`filtered.isEmpty()`).
  - [x] Integrated a beautifully styled, Material 3-compliant "Fetch from GitHub" call-to-action button in the primary empty state with a loading state spinner (`CircularProgressIndicator`) and `"fetch_from_github_btn"` test tag.
  - [x] Implemented a "Reset Search & Filters" button in the search-empty state with the `"reset_filters_btn"` test tag.
  - [x] Aligned empty state visuals with "Clean Minimalism" guidelines, utilizing generous negative space, custom styled icon circles (`ActivePurpleContainer`), and polished secondary captions.


