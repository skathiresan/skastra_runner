#!/bin/bash

# Validation script to verify the Astra Runner setup
# This script performs basic checks to ensure the application is properly configured

set -e

echo "🔍 Astra Runner Setup Validation"
echo "================================"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Color codes for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

success() {
    echo -e "${GREEN}✓${NC} $1"
}

warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

error() {
    echo -e "${RED}✗${NC} $1"
}

# Check Java version
echo "Checking Java installation..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 21 ]; then
        success "Java $JAVA_VERSION found"
    else
        error "Java 21 or higher required, found Java $JAVA_VERSION"
        exit 1
    fi
else
    error "Java not found. Please install Java 21 or higher"
    exit 1
fi

# Check Gradle wrapper
echo "Checking Gradle wrapper..."
if [ -f "./gradlew" ] && [ -x "./gradlew" ]; then
    success "Gradle wrapper found and executable"
else
    error "Gradle wrapper not found or not executable"
    exit 1
fi

# Check directory structure
echo "Checking project structure..."
required_dirs=("contracts" "engine" "app" "examples/sample-task")
for dir in "${required_dirs[@]}"; do
    if [ -d "$dir" ]; then
        success "Directory $dir exists"
    else
        error "Required directory $dir missing"
        exit 1
    fi
done

# Check key files
echo "Checking key files..."
key_files=(
    "settings.gradle.kts"
    "build.gradle.kts"
    "contracts/build.gradle.kts"
    "engine/build.gradle.kts"
    "app/build.gradle.kts" 
    "contracts/src/main/java/com/acme/spi/Task.java"
    "engine/src/main/java/com/acme/astra/engine/AstraEngine.java"
    "app/src/main/java/com/acme/astra/app/AstraRunnerApplication.java"
    "Jenkinsfile"
    "README.md"
)

for file in "${key_files[@]}"; do
    if [ -f "$file" ]; then
        success "File $file exists"
    else
        error "Required file $file missing"
        exit 1
    fi
done

# Check environment variables
echo "Checking environment configuration..."
if [ -n "$ARTIFACTORY_URL" ]; then
    success "ARTIFACTORY_URL is set"
else
    warning "ARTIFACTORY_URL not set - required for artifact resolution"
fi

if [ -n "$ARTIFACTORY_USER" ]; then
    success "ARTIFACTORY_USER is set"
else
    warning "ARTIFACTORY_USER not set - required for authentication"
fi

if [ -n "$ARTIFACTORY_PASSWORD" ]; then
    success "ARTIFACTORY_PASSWORD is set"
else
    warning "ARTIFACTORY_PASSWORD not set - required for authentication"
fi

# Try to build the project
echo "Testing build process..."
if ./gradlew build -x test --quiet; then
    success "Project builds successfully"
else
    error "Build failed - check Gradle output"
    exit 1
fi

# Check if JAR was created
JAR_FILE="app/build/libs/astra-runner.jar"
if [ -f "$JAR_FILE" ]; then
    success "Executable JAR created: $JAR_FILE"
else
    error "Executable JAR not found at $JAR_FILE"
    exit 1
fi

# Test JAR execution (help command)
echo "Testing JAR execution..."
if java -jar "$JAR_FILE" --help &> /dev/null; then
    success "JAR executes successfully"
else
    warning "JAR execution test failed - may need Artifactory credentials"
fi

# Check wrapper script
if [ -f "./astra.sh" ] && [ -x "./astra.sh" ]; then
    success "Wrapper script astra.sh found and executable"
else
    error "Wrapper script astra.sh not found or not executable"
    exit 1
fi

echo ""
echo "🎉 Validation completed successfully!"
echo ""
echo "Next steps:"
echo "1. Set up Artifactory credentials:"
echo "   export ARTIFACTORY_URL='https://your-artifactory.com/artifactory/libs-release'"
echo "   export ARTIFACTORY_USER='your-username'"
echo "   export ARTIFACTORY_PASSWORD='your-password'"
echo ""
echo "2. Run the example:"
echo "   ./astra.sh example"
echo ""
echo "3. Start the REST API:"
echo "   ./astra.sh server"
echo ""
echo "For more information, see README.md and DEVELOPMENT.md"