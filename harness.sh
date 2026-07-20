#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Do Gist Hub - Developer Coding Workflow Harness
# ==============================================================================

COLOR_HEADER='\033[1;35m'  # Purple Accent
COLOR_INFO='\033[0;34m'    # Info Blue
COLOR_SUCCESS='\033[1;32m' # Success Green
COLOR_ERROR='\033[1;31m'   # Error Red
COLOR_RESET='\033[0m'      # Reset

print_header() {
    echo -e "\n${COLOR_HEADER}=== $1 ===${COLOR_RESET}"
}

print_info() {
    echo -e "${COLOR_INFO}[INFO] $1${COLOR_RESET}"
}

print_success() {
    echo -e "${COLOR_SUCCESS}[SUCCESS] $1${COLOR_RESET}"
}

print_error() {
    echo -e "${COLOR_ERROR}[ERROR] $1${COLOR_RESET}" >&2
}

die() {
    print_error "$*"
    exit 1
}

# We use `gradle` to run tasks
run_gradle() {
    print_info "Running: gradle $*"
    gradle --stacktrace "$@"
}

log_step() {
    printf '\n==> %s\n' "$*"
}

show_help() {
    echo -e "${COLOR_HEADER}Do Gist Hub - Developer Coding Workflow Harness${COLOR_RESET}"
    echo -e "Usage: ./harness.sh [command]"
    echo -e ""
    echo -e "Available Commands:"
    echo -e "  ${COLOR_INFO}help${COLOR_RESET}         Show this usage information guide"
    echo -e "  ${COLOR_INFO}verify${COLOR_RESET}       Run the complete local verification pipeline:"
    echo -e "                     (format-check -> lint -> unit -> build)"
    echo -e "  ${COLOR_INFO}unit${COLOR_RESET}         Run local JVM and Robolectric unit tests (Tier 3)"
    echo -e "  ${COLOR_INFO}e2e${COLOR_RESET}          Run ONLY local high-fidelity JVM E2E tests (Tier 1)"
    echo -e "  ${COLOR_INFO}lint${COLOR_RESET}         Run Android Lint checker"
    echo -e "  ${COLOR_INFO}format-check${COLOR_RESET} Run Spotless code formatting check"
    echo -e "  ${COLOR_INFO}format${COLOR_RESET}       Apply Spotless code formatting (spotlessApply)"
    echo -e "  ${COLOR_INFO}build${COLOR_RESET}        Compile debug APK"
    echo -e "  ${COLOR_INFO}check${COLOR_RESET}        Run static-analysis gate (format-check -> detekt -> lint -> unit)"
    echo -e "  ${COLOR_INFO}detekt${COLOR_RESET}       Run Detekt static analysis"
    echo -e "  ${COLOR_INFO}coverage${COLOR_RESET}     Generate JaCoCo XML report"
    echo -e "  ${COLOR_INFO}codacy${COLOR_RESET}       Upload coverage to Codacy (requires CODACY_PROJECT_TOKEN)"
    echo -e "  ${COLOR_INFO}test${COLOR_RESET}         Run the full local test pyramid (E2E, Integration, & Unit)"
    echo -e "  ${COLOR_INFO}clean${COLOR_RESET}        Run Gradle clean"
    echo -e "  ${COLOR_INFO}wrapper-check${COLOR_RESET} Validate gradle-wrapper.jar integrity (zip + min size)"
    echo -e "  ${COLOR_INFO}setup-hooks${COLOR_RESET}  Install Git pre-push hooks to automate local verification"
    echo -e ""
}

# Ensure at least one argument is provided
if [ $# -lt 1 ]; then
    show_help
    exit 1
fi

COMMAND=$1

case "$COMMAND" in
    help|--help|-h)
        show_help
        ;;
    format-check)
        print_header "Step: Format Check"
        run_gradle spotlessCheck || die "Format check failed. Run './harness.sh format' to fix."
        print_success "Format check passed."
        ;;
    format)
        print_header "Step: Apply Code Formatting"
        run_gradle spotlessApply || die "Applying formatting failed."
        print_success "Code formatting applied successfully."
        ;;
    detekt)
        print_header "Step: Detekt Static Analysis"
        run_gradle detekt || die "Detekt analysis failed."
        print_success "Detekt static analysis passed."
        ;;
    lint)
        print_header "Step: Android Lint"
        run_gradle :app:lintDebug || die "Lint check failed."
        print_success "Lint check passed."
        ;;
    unit)
        print_header "Step: Unit & Integration Tests"
        run_gradle :app:testDebugUnitTest || die "Unit tests failed."
        print_success "Unit tests passed."
        ;;
    build)
        print_header "Step: Compile Debug Build"
        run_gradle :app:assembleDebug || die "Build compilation failed."
        print_success "Debug build compiled successfully."
        ;;
    check)
        print_header "Pipeline: Static analysis, formatting & unit tests"
        "$0" wrapper-check
        "$0" format-check
        "$0" detekt
        "$0" lint
        "$0" unit
        print_success "Check pipeline passed (wrapper + format + detekt + lint + unit)."
        ;;
    verify)
        print_header "Pipeline: Running local verification gate"
        "$0" wrapper-check
        "$0" format-check
        "$0" detekt
        "$0" lint
        "$0" unit
        "$0" build
        if [ -n "${CODACY_PROJECT_TOKEN:-}" ]; then
            "$0" coverage
            "$0" codacy
        fi
        print_success "Verification pipeline passed completely."
        ;;
    coverage)
        log_step "Generating JaCoCo coverage report"
        run_gradle jacocoTestReportDebug
        ;;
    codacy)
        log_step "Uploading coverage to Codacy"
        if [ -z "${CODACY_PROJECT_TOKEN:-}" ]; then
            die "CODACY_PROJECT_TOKEN is not set. Export it before running this command."
        fi
        REPORT="app/build/reports/jacoco/jacocoTestReportDebug/jacocoTestReportDebug.xml"
        if [ ! -f "$REPORT" ]; then
            die "Coverage report not found at $REPORT. Run './harness.sh coverage' first."
        fi
        bash <(curl -Ls https://coverage.codacy.com/get.sh) report -r "$REPORT"
        ;;
    e2e)
        print_header "Step: End-to-End (E2E) Test Execution (Tier 1)"
        print_info "Running peak-tier high-fidelity local JVM E2E tests (*E2ETest)..."
        run_gradle :app:testDebugUnitTest --tests "*E2ETest" || die "E2E tests failed."
        print_success "All local JVM E2E tests passed successfully!"
        ;;
    test)
        print_header "Step: Full Test Pyramid Execution"
        print_info "Executing all levels: Tier 1 (E2E), Tier 2 (Integration), & Tier 3 (Unit)..."
        "$0" unit
        print_success "The complete local Test Pyramid built & passed successfully."
        print_info "All tests run locally on the JVM (Robolectric/Roborazzi) — no instrumented/connected tests."
        ;;
    clean)
        print_header "Step: Gradle Clean"
        run_gradle clean || die "Gradle clean failed."
        print_success "Gradle clean completed."
        ;;
    wrapper-check)
        print_header "Step: Gradle Wrapper Integrity Check"
        WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"
        [ -f "$WRAPPER_JAR" ] || die "Wrapper jar missing at $WRAPPER_JAR."
        if ! (unzip -t "$WRAPPER_JAR" >/dev/null 2>&1); then
            die "Wrapper jar at $WRAPPER_JAR is corrupt or not a valid zip. Regenerate with: rm -f $WRAPPER_JAR && gradle wrapper --gradle-version \$(grep -oP 'gradle-\K[0-9.]+' gradle/wrapper/gradle-wrapper.properties)-bin.zip --no-daemon"
        fi
        MIN_BYTES=35000
        ACTUAL_BYTES=$(wc -c < "$WRAPPER_JAR")
        if [ "$ACTUAL_BYTES" -lt "$MIN_BYTES" ]; then
            die "Wrapper jar at $WRAPPER_JAR is suspiciously small (${ACTUAL_BYTES} bytes < ${MIN_BYTES}). It is likely truncated/corrupt."
        fi
        print_success "Wrapper jar integrity OK (${ACTUAL_BYTES} bytes)."
        ;;
    setup-hooks)
        print_header "Step: Install Git Local Verification Hooks"
        if [ ! -d ".git" ]; then
            die "Not a git repository. Cannot install git hooks."
        fi
        HOOK_FILE=".git/hooks/pre-push"
        print_info "Writing pre-push hook to $HOOK_FILE..."
        cat << 'EOF' > "$HOOK_FILE"
#!/usr/bin/env bash
# Automatically installed by Do Gist Hub harness.sh setup-hooks
set -euo pipefail

echo "===================================================="
echo "Running git pre-push local verification..."
echo "===================================================="

if ! ./harness.sh check; then
    echo "===================================================="
    echo "❌ Local verification failed! Push aborted."
    echo "Please fix formatting, lints, or unit tests first."
    echo "You can run './harness.sh format' to auto-fix formatting."
    echo "===================================================="
    exit 1
fi

echo "===================================================="
echo "✅ Local verification passed. Pushing code..."
echo "===================================================="
EOF
        chmod +x "$HOOK_FILE"
        print_success "Git pre-push hook installed and made executable."
        ;;
    *)
        print_error "Unknown command: '$COMMAND'"
        show_help
        exit 1
        ;;
esac
