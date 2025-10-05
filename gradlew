#!/bin/bash

# Gradle wrapper script for Unix systems
# This allows the project to be built without requiring Gradle to be installed

set -e

GRADLE_VERSION="8.5"
GRADLE_DISTRIBUTION_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_HOME="$HOME/.gradle/wrapper/gradle-${GRADLE_VERSION}"
GRADLE_JAR="$GRADLE_HOME/lib/gradle-launcher-${GRADLE_VERSION}.jar"

# Download and install Gradle if not present
if [ ! -f "$GRADLE_JAR" ]; then
    echo "Downloading Gradle ${GRADLE_VERSION}..."
    mkdir -p "$GRADLE_HOME"
    curl -L "$GRADLE_DISTRIBUTION_URL" -o "/tmp/gradle-${GRADLE_VERSION}.zip"
    unzip -q "/tmp/gradle-${GRADLE_VERSION}.zip" -d "/tmp"
    mv "/tmp/gradle-${GRADLE_VERSION}"/* "$GRADLE_HOME/"
    rm "/tmp/gradle-${GRADLE_VERSION}.zip"
    rm -rf "/tmp/gradle-${GRADLE_VERSION}"
fi

# Execute Gradle
exec java -jar "$GRADLE_JAR" "$@"