#!/bin/bash

# Astra Runner wrapper script
# This script provides convenient commands for building and running the application

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_FILE="$SCRIPT_DIR/app/build/libs/astra-runner.jar"

usage() {
    echo "Usage: $0 <command> [options]"
    echo ""
    echo "Commands:"
    echo "  build                 Build the application"
    echo "  test                  Run tests"
    echo "  run                   Run CLI mode with provided arguments"
    echo "  server                Start REST API server"
    echo "  example               Run the sample task example"
    echo "  clean                 Clean build artifacts"
    echo ""
    echo "Examples:"
    echo "  $0 build"
    echo "  $0 run --ga=\"com.example:my-task\" --version=\"1.0.0\" --mode=\"SPI\""
    echo "  $0 server"
    echo "  $0 example"
    echo ""
}

build() {
    echo "Building Astra Runner..."
    cd "$SCRIPT_DIR"
    ./gradlew clean build
    echo "Build completed. Executable JAR: $JAR_FILE"
}

test_app() {
    echo "Running tests..."
    cd "$SCRIPT_DIR"
    ./gradlew test
    echo "Tests completed."
}

run_cli() {
    if [ ! -f "$JAR_FILE" ]; then
        echo "Application not built. Building first..."
        build
    fi
    
    echo "Running Astra Runner CLI..."
    java -jar "$JAR_FILE" "$@"
}

run_server() {
    if [ ! -f "$JAR_FILE" ]; then
        echo "Application not built. Building first..."
        build
    fi
    
    echo "Starting Astra Runner REST API server..."
    java -jar "$JAR_FILE" --rest
}

run_example() {
    if [ ! -f "$JAR_FILE" ]; then
        echo "Application not built. Building first..."
        build
    fi
    
    echo "Building sample task..."
    cd "$SCRIPT_DIR"
    ./gradlew :examples:sample-task:publishToMavenLocal
    
    echo "Running sample task example..."
    mkdir -p example-reports
    
    java -jar "$JAR_FILE" \
        --ga="com.example:sample-task" \
        --version="1.0.0" \
        --mode="SPI" \
        --argsJson='{"name":"Astra","greeting":"Hello","createExtra":"true"}' \
        --reportsDir="example-reports" \
        --workspace="example-workspace"
    
    echo ""
    echo "Example completed! Check the reports in: example-reports/"
    echo "- run-summary.json    - Complete execution summary"
    echo "- summary.html        - Human-readable report" 
    echo "- junit.xml           - JUnit test results"
    echo "- results.json        - Raw task results"
}

clean() {
    echo "Cleaning build artifacts..."
    cd "$SCRIPT_DIR"
    ./gradlew clean
    rm -rf example-reports example-workspace reports workspace
    echo "Clean completed."
}

# Main command dispatch
case "$1" in
    "build")
        build
        ;;
    "test")
        test_app
        ;;
    "run")
        shift
        run_cli "$@"
        ;;
    "server")
        run_server
        ;;
    "example")
        run_example
        ;;
    "clean")
        clean
        ;;
    "help"|"--help"|"-h"|"")
        usage
        ;;
    *)
        echo "Unknown command: $1"
        echo ""
        usage
        exit 1
        ;;
esac