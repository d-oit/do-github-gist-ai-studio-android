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
