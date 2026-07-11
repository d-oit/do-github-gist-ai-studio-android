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
- **Compose State**: Managed via `StateFlow` in the `GistViewModel`. Screens observe the state reactively using `collectAsState()` or `collectAsStateWithLifecycle()` to minimize recomposition.
- **Type-safe Navigation**: Navigation route management occurs via state triggers without fragment transaction overhead.

### Domain / Repository Layer
- **GistRepository**: The single source of truth for all data operations. It mediates between the local Room SQLite cache and the remote GitHub Gist REST endpoints.
- **Kotlin Coroutines**: All background network/disk actions run safely off the main thread using `Dispatchers.IO` and structured concurrency.

### Data Layer
- **Room SQLite Cache**: Provides offline capabilities. Local modifications are instantly persistent and reactive.
- **Retrofit & Moshi**: Formulates typed, standard HTTP requests complying with the `GitHub Gist API v3`. JSON serialization is handled through reflection-free code-generated Moshi adapters.

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
               │                     └──────────────────┘
               │                              ▲
               └─────────► Sync Trigger ──────┘
```

### State Flags
1. **`isLocalOnly = true` (Draft)**: Indicates the snippet was created offline and does not exist on GitHub yet.
2. **`isDirty = true` (Modified)**: Indicates the snippet exists on GitHub but has local edits that haven't been pushed.
3. **`isDirty = false, isLocalOnly = false` (Synced)**: Completely in sync with the GitHub cloud storage.

### Synchronization Rules
- **Incremental Sync**: When trigger events occur (tapping "Sync" or launching the app), the repository identifies all unsynchronized records (`isLocalOnly == true` or `isDirty == true`) and resolves them sequentially:
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

Do Gist Hub's developer harness (`harness.sh`) and architecture are directly aligned with Fowler's core harness dimensions:

### Key Harness Engineering Dimensions

1. **Local Fidelity & Dev-CI Congruence**
   - *Fowler's Principle*: The local development harness must execute the exact same validation pathways and tasks as the remote Continuous Integration (CI) server. This prevents the "works on my machine" syndrome.
   - *Implementation*: `harness.sh` wraps standardized Gradle operations (`lintDebug`, `assembleDebug`, and `testDebugUnitTest`) using the same workspace configuration utilized by the build agents, guaranteeing that local verification translates 100% to remote compilation success.

2. **Optimized Inner-Loop Feedback**
   - *Fowler's Principle*: Developers require an extremely low-latency feedback loop. Slow checks discourage frequent running, leading to delayed integration issues.
   - *Implementation*: The harness categorizes tasks by speed and objective, enabling developers to run quick static analysis (`check`) before initiating heavier tasks like full compilation (`build`) or running JVM and Robolectric suite tests (`test`).

3. **Hermetic Isolation & Environmental Decoupling**
   - *Fowler's Principle*: Automated verification must be hermetic and decoupled from flaky network dependencies, live databases, or dynamic external variables.
   - *Implementation*: Our offline-first synchronization repository and local Room SQLite cache allow the unit and Robolectric test harness to run entirely in-memory and local-only, without depending on live connections to the GitHub API.

4. **Graduated Verification Pipeline**
   - *Fowler's Principle*: Checks should progress incrementally from low-cost static rules to deeper compilation checks, then to full test execution.
   - *Implementation*: The tool suite supports a graduated pipeline:
     - **Level 1 (`check`)**: Lightweight static analysis and linting to catch typos, formatting anomalies, and structural style issues.
     - **Level 2 (`build`)**: Core Kotlin compilation to verify type safety and build pipeline integrity.
     - **Level 3 (`test`)**: Execution of Robolectric simulation, MVVM unit tests, and local integration suites.

### Command Guide
The workflow harness script `./harness.sh` supports the following execution triggers:

- **`./harness.sh check`**: Runs full static analysis checks (formatting and linting) to verify compliance before commits.
- **`./harness.sh build`**: Executes compiler-level validation to compile the app completely, ensuring no breaking changes.
- **`./harness.sh test`**: Triggers local JVM unit tests and automated Robolectric regression testing.
- **`./harness.sh help`**: Outlines all available command triggers.

By adhering to this design specification and utilizing the workflow harness, the project guarantees pristine visual quality, structured offline performance, and robust development velocity.

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
- **Header Navigation Tabs**: 
  - Dual-mode horizontal tabs: **Write** (default editing canvas) and **Preview** (instant markdown rendering output).
  - High-contrast visual underline indicating the active tab, paired with a thin bottom border dividing the tabs from the editing area.
- **Markdown Formatting Toolbar**:
  - Horizontal tool belt containing clean, recognizable vector icons aligned to standard Material symbols.
  - Separated from the text field using subtle, low-impact spacing to prevent screen clutter.
- **Content/Comment Input Box**:
  - Spacious multi-line text input field with a raised minimum height (`120.dp`) and clean padding, featuring a neutral placeholder (e.g., `"Leave a comment"` or `"Gist file content..."`).
  - Text input utilizing a monospace font family when writing raw markdown.
- **Status Indicator Footer**:
  - **Bottom-Left Area**: "Markdown is supported" indicator with an official Markdown downward-arrow logo.
  - **Bottom-Right Area**: "Paste, drop, or click to add files" interactive zone with a vector paperclip/image icon representing media/attachment capabilities.

### 10.2 Formatting Helper Actions (Text Transformation Rules)
Clicking formatting buttons in the toolbar instantly mutates the active text field value by appending or wrapping markdown templates at the current selection or text end:
1. **Header (`H`)**: Prepends `### ` to line start or appends `### Header`.
2. **Bold (`B`)**: Wraps selected text in double asterisks `**bold_text**` or appends `**Bold**`.
3. **Italic (`I`)**: Wraps selected text in single asterisks `*italic_text*` or appends `*Italic*`.
4. **Blockquote (`>`)**: Prepends `> ` blockquote indicators.
5. **Code Block (`<>`)**: Wraps selection with multi-line code blocks ` ```code``` ` or inline backticks `` `code` ``.
6. **Link (Chain Icon)**: Generates standard hyperlinks template: `[Link Text](https://url)`.
7. **Numbered List (`1.`)**: Appends or prepends `1. ` prefix rules.
8. **Bulleted List (`-`)**: Appends or prepends `- ` bullet characters.
9. **Task List (`[x]`)**: Inserts empty checkbox brackets: `- [ ] Task`.
10. **File Attachment (`Paperclip`)**: Appends an image/file reference template: `![Alt Text](url)`.
11. **Mention User (`@`)**: Appends `@` character to trigger handle references.
12. **Quote Reference (Reply Bubble)**: Formulates nested blockquotes to reference prior statements.

### 10.3 Accessibility & Developer Identifiers
- **Minimum Interactive Size**: Every button element on the formatting toolbar maintains a touch target of at least `48.dp x 48.dp`.
- **Test Tags**: Buttons and input areas are annotated with consistent `snake_case` test tags:
  - `markdown_toolbar`: The layout wrapper.
  - `markdown_btn_header`, `markdown_btn_bold`, `markdown_btn_italic`, `markdown_btn_quote`, `markdown_btn_code`, `markdown_btn_link`, `markdown_btn_num_list`, `markdown_btn_bullet_list`, `markdown_btn_task_list`, `markdown_btn_attachment`, `markdown_btn_mention`, `markdown_btn_quote_reply`.
  - `markdown_footer_logo`: Markdown support logo.
  - `markdown_footer_attachment_hint`: File addition hint.
- **Content Descriptions**: Every icon button explicitly defines localized semantic labels (e.g., `contentDescription = "Format Bold"`).
- **Test Tags**: The explorer list component and cards are fully tagged for automated testing with `github_gist_api_list_container`, `remote_gist_refresh_button`, and `remote_gist_card_UUID`.

---

## 11. Secure Token Storage (2026 Android Security Best Practices)
To prevent unauthorized access to personal access tokens and secure credentials, Do Gist Hub complies fully with modern 2026 Android platform security directives:
1. **MasterKey Hardware Backing**:
   - Migration from deprecated `MasterKeys` APIs to the unified `MasterKey.Builder` to configure a secure hardware-backed key (stored within the Android Keystore system).
   - Generates an AES-256 GCM master key that leverages secure hardware-backed cryptographic co-processors (StrongBox / Trusted Execution Environment) whenever available.
2. **EncryptedSharedPreferences Integration**:
   - Saves personal access tokens (PAT) and user profile information in an encrypted preference file (`secure_gist_config_prefs`).
   - Leverages AES256-SIV for filename/key encryption and AES256-GCM for preference values, mitigating side-channel leaks, debugging dumps, and device-root level snooping.
   - Falls back gracefully to standard private shared preferences if the secure keystore initialization fails.
## 12. Token Verification Button State Architecture
To deliver absolute visual and functional clarity, the "Verify Token" button transitions reactively through four distinct lifecycle states governed by a structured Kotlin sealed interface `TokenVerificationState`:

### 12.1 State Architecture Definitions
*   `TokenVerificationState.Idle`: Initial state. The user inputs their PAT and taps "Verify". Text reads **"Verify Token & Autofill Profile"**, using standard primary color schemes.
*   `TokenVerificationState.Verifying`: Active validation state. Button is disabled to block double-taps, displaying a dynamic **CircularProgressIndicator** spinner and text reads **"Verifying Token..."**.
*   `TokenVerificationState.Success`: Definitively indicates verification and storage success. Color transitions to a secure **Material Green (Color(0xFF2E7D32))** containing a checkmark circle. Spawns a localized Toast declaring **"GitHub personal access token has been verified and safely saved to secure storage."**
*   `TokenVerificationState.Error`: Displays a robust diagnostic state. The background transitions to the Material error color showing an error outline icon. Text updates to **"Verification Failed! Tap to Retry"** while displaying the detailed authentication failure reason.

### 12.2 Visual and Accessibility Specifications
*   **Color Shifts**: Primary Blue/Teal (Idle) -> Primary Faded (Verifying) -> Success Green (Success) -> Error Red (Error).
*   **String Resources**: Fully configured in `strings.xml` to support localizations and maintain zero hardcoded magic text strings in source files.
*   **Test Tags**: The verify button remains targetable under the automated testing identifier `config_verify_button`.



