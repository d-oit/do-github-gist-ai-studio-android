# Do Gist Hub

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/TODO-INSERT-REAL-PROJECT-UUID-HERE)](https://app.codacy.com/gh/d-oit/do-github-gist-ai-studio-android/dashboard) <!-- TODO: Replace TODO-INSERT-REAL-PROJECT-UUID-HERE with your actual Codacy project UUID when available -->

An offline-first, reactive GitHub Gist client built using **Kotlin**, **Jetpack Compose**, **Room SQLite**, and a local/remote synchronization engine.

---

## 🚀 Features
- **Offline-First Synchronization**: Create, edit, and delete gists offline; changes automatically sync when online.
- **On-Device & Gemini Hybrid AI Assistant**: Seamlessly analyze code complexity, maintainability, and optimization offline (with dynamic Gemini-REST fallback).
- **Secure Storage**: Personal Access Tokens (PATs) are hardware-encrypted using the unified Android Keystore (`MasterKey` + `EncryptedSharedPreferences`).
- **Markdown Editing**: Real-time write and preview markdown tabs with action formatting helpers.
- **Continuous Quality Gate**: Out-of-the-box support for Spotless formatting, Detekt static analysis, Android Lint, and Robolectric JVM integration testing.

---

## 🛠️ Local Developer Setup

### 1. Prerequisites
- JDK 17 (Zulu recommended)
- Android Studio Ladybug (2024.2.1) or higher

### 2. Configuration
Create a local configuration file named `.env` in the root directory (based on `.env.example`):
```properties
# .env
GEMINI_API_KEY=your_gemini_api_key_here
GITHUB_PAT=your_github_personal_access_token_here
```

### 3. Workflow Harness
The codebase includes a unified POSIX-compliant developer harness (`./harness.sh`) acting as the single local and CI quality gate:

- **Help**: `./harness.sh help`
- **Full Verification Gate**: `./harness.sh verify` (Runs Spotless Check -> Lint -> Unit Tests -> Debug Compile)
- **Unit & Robolectric Tests**: `./harness.sh unit`
- **Lint Scanner**: `./harness.sh lint`
- **Code Formatter Check**: `./harness.sh format-check`
- **Auto-Format Code**: `./gradlew spotlessApply`
- **Build Debug APK**: `./harness.sh build`

---

## 🛡️ Code Quality & Coverage (Codacy)

We use **Codacy** to automate static analysis code reviews and track test coverage variations on every commit and pull request.

### 1. Automated Quality Gates
- **Static Analysis**: Configured via `.github/workflows/codacy.yml` utilizing the official `codacy/codacy-analysis-cli-action@v4` action, which exports SARIF diagnostics directly to GitHub Security alerts.
- **Coverage Reports**: Configured via `.github/workflows/ci.yml` utilizing the official `codacy/codacy-coverage-reporter-action@v1` action.

### 2. Local Developer Integration
You can run analysis and generate coverage metrics locally using the developer harness:

- **Generate Coverage Report**: `./harness.sh coverage`
  - Generates full JaCoCo XML reports located at `app/build/reports/jacoco/jacocoTestReportDebug/jacocoTestReportDebug.xml`.
- **Upload Coverage to Codacy**: `./harness.sh codacy`
  - Uploads the generated reports to the Codacy platform. This requires exporting your project token to your environment first:
    ```bash
    export CODACY_PROJECT_TOKEN="your_codacy_project_token_here"
    ./harness.sh codacy
    ```
- **Local Verification Gate**: If `CODACY_PROJECT_TOKEN` is set, running `./harness.sh verify` automatically runs coverage generation and uploads it. If it is not set, those steps are safely skipped.

> [!WARNING]
> **CRITICAL SECURITY RULES**: Never hardcode, commit, or log your `CODACY_PROJECT_TOKEN`. It must be kept strictly local or as an encrypted secret in GitHub Actions.

---

## 🧪 Testing

All tests are hermetic and run locally on the JVM via Robolectric and Roborazzi (no emulator required).

- Run tests: `./harness.sh test`
- Verify screenshots: `./gradlew :app:verifyRoborazziDebug`
- Record screenshots: `./gradlew :app:recordRoborazziDebug`
