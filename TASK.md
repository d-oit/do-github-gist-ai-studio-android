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
