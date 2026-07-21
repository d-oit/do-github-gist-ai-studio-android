# d.o.Gist Hub

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

Below is the directory structure of the **d.o.Gist Hub** project:

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

### 3. Unified Coding Workflow Harness (MANDATORY GATEWAY)
To maintain absolute environment parity, enforce identical code quality standards as Continuous Integration (CI), and prevent build configuration drifts, **all standard Direct Gradle and Gradle Wrapper commands are strictly superseded by the unified developer workflow harness (`./harness.sh`)**.

Direct Gradle invocations or Gradle Wrapper commands (e.g., `./gradlew assembleDebug`, `./gradlew test`) are non-standard and **MUST NOT** be used for regular development or local verification tasks. The workflow harness is the designated single-source of local compilation, static analysis, and unit testing validation.

#### Harness Command Guide
Always run your local coding and verification loops exclusively through `./harness.sh`:

- **Help / Usage Guide**:
  ```bash
  ./harness.sh help
  ```
- **Full Verification Gate (Run before every push)**:
  ```bash
  ./harness.sh verify
  ```
  *Runs the complete local validation sequence: Spotless Format Check ➔ Android Lint ➔ Unit & Robolectric Tests ➔ Debug APK Compile. Automatically runs JaCoCo coverage and Codacy upload if `CODACY_PROJECT_TOKEN` is present in your shell environment.*
- **Static Analysis Quality Gate**:
  ```bash
  ./harness.sh check
  ```
  *Runs the static verification sequence: Spotless Format Check ➔ Android Lint ➔ Unit & Robolectric Tests.*
- **Build Debug APK**:
  ```bash
  ./harness.sh build
  ```
  *Compiles the debug application targets and outputs the debug APK.*
- **Run the Complete Test Suite**:
  ```bash
  ./harness.sh test
  ```
  *Executes the full suite of unit and integration tests locally on the JVM.*
- **Run Local JVM Unit Tests directly**:
  ```bash
  ./harness.sh unit
  ```
- **Run Android Static Analysis (Lint)**:
  ```bash
  ./harness.sh lint
  ```
- **Code Formatter Check (Spotless)**:
  ```bash
  ./harness.sh format-check
  ```
- **Generate Local JaCoCo Coverage Report**:
  ```bash
  ./harness.sh coverage
  ```
  *Runs the test suite and generates a unified JaCoCo coverage report locally.*
- **Upload Coverage Metrics to Codacy**:
  ```bash
  ./harness.sh codacy
  ```
  *Manual trigger to upload local reports (requires `CODACY_PROJECT_TOKEN` environment variable).*
- **Clean Build Artifacts**:
  ```bash
  ./harness.sh clean
  ```

---

## 🛡️ Code Quality & Local Coverage

We prioritize strict architectural separation, robust test coverage, and clean, standardized formatting through the harness.

### 1. Code Quality Gates
- **Formatting**: Enforced via **Spotless** and standardized Kotlin conventions. To automatically format your codebase when formatting checks fail, run:
  ```bash
  gradle spotlessApply
  ```
- **Static Analysis**: Verified via **Android Lint**. Run `./harness.sh lint` to inspect the codebase for security, performance, accessibility, and correctness issues instead of using raw Gradle tasks.

### 2. Local Test Coverage (JaCoCo)
Analyze local coverage metrics exclusively using the developer harness:
```bash
./harness.sh coverage
```
The unified JaCoCo reports are output locally:
- **HTML Report (visual inspection)**: `app/build/reports/jacoco/jacocoTestReportDebug/html/index.html`
- **XML Report (CI parser integration)**: `app/build/reports/jacoco/jacocoTestReportDebug/jacocoTestReportDebug.xml`

---

## 🧪 Testing

All tests are hermetic and run locally on the JVM via Robolectric and Roborazzi. Running tests on emulators or physical devices is entirely superseded.

- **Run all tests**:
  ```bash
  ./harness.sh test
  ```
- **Verify UI screenshots**:
  ```bash
  gradle :app:verifyRoborazziDebug
  ```
- **Record reference screenshots**:
  ```bash
  gradle :app:recordRoborazziDebug
  ```
