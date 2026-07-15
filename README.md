# Do Gist Hub

An offline-first, reactive GitHub Gist client built using **Kotlin**, **Jetpack Compose**, **Room SQLite**, and a local/remote synchronization engine.

---

## 🚀 Features
- **Offline-First Synchronization**: Create, edit, and delete gists offline; changes automatically sync when online.
- **On-Device & Gemini Hybrid AI Assistant**: Seamlessly analyze code complexity, maintainability, and optimization offline (with dynamic Gemini-REST fallback).
- **Secure Storage**: Personal Access Tokens (PATs) are hardware-encrypted using the unified Android Keystore (`MasterKey` + `EncryptedSharedPreferences`).
- **Markdown Editing**: Real-time write and preview markdown tabs with action formatting helpers.
- **Continuous Quality Gate**: Out-of-the-box support for Spotless formatting, Detekt static analysis, Android Lint, and Robolectric JVM integration testing.

---

## 📁 Repository Structure

Below is the directory structure of the **Do Gist Hub** project:

```text
do-github-gist-ai-studio-android/
├── .github/              # GitHub Action workflows (CI)
├── app/                  # Main Android Application module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── core/         # Unified core systems and utilities (cryptography, network, etc.)
│   │   │   │   ├── data/         # Data layer (local database, remote REST client, repository, sync engine)
│   │   │   │   │   ├── local/    # Room Database setup, entities, and DAOs
│   │   │   │   │   ├── remote/   # Retrofit API clients and request/response models
│   │   │   │   │   ├── repository/# GistRepository (single source of truth mediating cache vs cloud)
│   │   │   │   │   └── sync/     # WorkManager background sync workers
│   │   │   │   ├── di/           # Manual Constructor Injection wiring and service locators
│   │   │   │   └── ui/           # Jetpack Compose UI (Theme, Components, Screens, ViewModels)
│   │   │   │       ├── components/ # Reusable UI widgets (Markdown editor, code view cards)
│   │   │   │       ├── screens/  # Navigation-routed screen views
│   │   │   │       ├── theme/    # Material 3 colors, typography, and shape specifications
│   │   │   │       └── viewmodel/# Stateflows and actions coordinating presentation logic
│   │   │   └── res/              # Static Android resources (strings, drawables, assets)
│   │   └── test/                 # JVM-based Unit and Robolectric integration tests
│   └── build.gradle.kts  # App module Gradle configuration
├── build.gradle.kts      # Root project Gradle configuration
├── settings.gradle.kts   # Project and dependency catalog resolution
├── harness.sh            # Developer workflow harness script (single quality gate)
├── SPEC.md               # Comprehensive system design and state specification
└── TASK.md               # Developer checklist tracking development progress
```

---

## 🛠️ Local Developer Setup

### 1. Prerequisites
- **JDK 17** (Zulu OpenJDK recommended)
- **Android Studio Ladybug** (2024.2.1) or higher

### 2. Configuration
The application accesses the GitHub Gist REST API and optional cloud-based Gemini AI endpoints securely. Create a local configuration file named `.env` in the root directory (based on `.env.example`):

```properties
# .env
# GITHUB_PAT: Required for remote Gist synchronization.
GITHUB_PAT=your_github_personal_access_token_here

# GEMINI_API_KEY: Optional. If provided, enables deep cloud-based generative AI analysis.
# If omitted or left blank, the app gracefully falls back instantly to the fast local on-device heuristics analyzer.
GEMINI_API_KEY=your_gemini_api_key_here
```

### 3. Workflow Harness
The codebase includes a unified POSIX-compliant developer harness (`./harness.sh`) acting as the single local and CI quality gate:

- **Help**: `./harness.sh help` - Show all available workflow tasks
- **Full Verification Gate**: `./harness.sh verify` - Runs Spotless Check -> Lint -> Unit Tests -> Debug Compile
- **Unit & Robolectric Tests**: `./harness.sh unit` - Run local JVM unit tests
- **Lint Scanner**: `./harness.sh lint` - Run Android static analyzer
- **Code Formatter Check**: `./harness.sh format-check` - Verify Spotless formatting compliance
- **Auto-Format Code**: `./gradlew spotlessApply` - Automatically format all code
- **Build Debug APK**: `./harness.sh build` - Compile debug build targets

---

## 🛡️ Code Quality & Local Coverage

We prioritize strict architectural separation, robust test coverage, and clean, standardized formatting.

### 1. Code Quality Gates
- **Formatting**: Enforced via **Spotless** and standardized Kotlin conventions. Run `./gradlew spotlessApply` to automatically format files.
- **Static Analysis**: Verified via **Android Lint** and static analyzers. Run `./harness.sh lint` to check for security, accessibility, and correctness issues.

### 2. Local Test Coverage (JaCoCo)
You can run analysis and generate local coverage metrics using the developer harness:

- **Generate Coverage Report**:
  ```bash
  ./harness.sh coverage
  ```
  This command will run all tests and generate a unified JaCoCo XML and HTML report.
- **Report Location**:
  - HTML Report (visual inspection): `app/build/reports/jacoco/jacocoTestReportDebug/html/index.html`
  - XML Report (CI tools parsing): `app/build/reports/jacoco/jacocoTestReportDebug/jacocoTestReportDebug.xml`

---

## 🧪 Testing

All tests are hermetic and run locally on the JVM via Robolectric and Roborazzi (no emulator or device connection required).

- Run tests: `./harness.sh test`
- Verify UI screenshots: `./gradlew :app:verifyRoborazziDebug`
- Record reference screenshots: `./gradlew :app:recordRoborazziDebug`
