#!/usr/bin/env bash

# ==============================================================================
# Do Gist Hub - Developer Coding Workflow Harness
# ==============================================================================
# Inspired by Martin Fowler's "Harness Engineering" paradigm, this script builds
# a high-fidelity local execution environment ("the developer harness") to wrap
# and validate the application code.
#
# Key Architectural Dimensions Supported:
# 1. Local Fidelity & Dev-CI Congruence (Identical verification path as CI)
# 2. Optimized Inner-Loop Feedback (Fast, targeted execution stages)
# 3. Hermetic Isolation (Using mock-ready configurations and local databases)
# 4. Graduated Verification Pipeline (Static Linting -> Build -> Test Suites)
#
# IMPORTANT: Always use system-level 'gradle' instead of './gradlew' in this env.
# ==============================================================================

# Terminal styling helper colors (Clean Minimalism Palette)
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
    echo -e "${COLOR_ERROR}[ERROR] $1${COLOR_RESET}"
}

show_help() {
    echo -e "${COLOR_HEADER}Do Gist Hub - Developer Coding Workflow Harness${COLOR_RESET}"
    echo -e "Usage: ./harness.sh [command]"
    echo -e ""
    echo -e "Available Commands:"
    echo -e "  ${COLOR_INFO}check${COLOR_RESET}   Run static code analysis and linting (gradle :app:lintDebug)"
    echo -e "  ${COLOR_INFO}build${COLOR_RESET}   Run complete incremental debug build (gradle assembleDebug)"
    echo -e "  ${COLOR_INFO}test${COLOR_RESET}    Run local JVM and Robolectric unit tests (gradle :app:testDebugUnitTest)"
    echo -e "  ${COLOR_INFO}help${COLOR_RESET}    Show this usage information guide"
    echo -e ""
}

# Ensure at least one argument is provided
if [ $# -lt 1 ]; then
    show_help
    exit 1
fi

COMMAND=$1

case "$COMMAND" in
    check|lint)
        print_header "Static Analysis & Lint Validation"
        print_info "Triggering Android Lint checker..."
        gradle :app:lintDebug
        if [ $? -eq 0 ]; then
            print_success "Static analysis completed with no blocking issues."
        else
            print_error "Static analysis found critical issues or lint errors."
            exit 1
        fi
        ;;
    build)
        print_header "Incremental Build Compilation"
        print_info "Triggering clean compiler verification loop..."
        gradle assembleDebug
        if [ $? -eq 0 ]; then
            print_success "Application successfully compiled."
        else
            print_error "Compilation failed with compiler errors."
            exit 1
        fi
        ;;
    test)
        print_header "Unit & Integration Test Suite"
        print_info "Running local JVM Unit Tests (Robolectric enabled)..."
        gradle :app:testDebugUnitTest
        if [ $? -eq 0 ]; then
            print_success "All tests built and passed cleanly!"
        else
            print_error "Test failures detected in suite."
            exit 1
        fi
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        print_error "Unknown command: '$COMMAND'"
        show_help
        exit 1
        ;;
esac
