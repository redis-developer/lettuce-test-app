#!/bin/bash

# Script to build lettuce-test-app with REQUIRED custom lettuce-core from feature/maintenance-events branch
# This project REQUIRES the custom lettuce-core build to function properly
# Usage: ./scripts/build.sh

set -e

# Configuration
LETTUCE_REPO="https://github.com/redis/lettuce.git"
LETTUCE_BRANCH="feature/maintenance-events"
LETTUCE_VERSION="7.0.0.MAINT-SNAPSHOT"
TEMP_DIR="$(mktemp -d)"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "=========================================="
echo "Building lettuce-test-app with REQUIRED custom lettuce-core"
echo "=========================================="
echo "Lettuce repository: $LETTUCE_REPO"
echo "Lettuce branch: $LETTUCE_BRANCH"
echo "Lettuce version: $LETTUCE_VERSION"
echo "Temp directory: $TEMP_DIR"
echo "Project root: $PROJECT_ROOT"
echo "=========================================="

# Function to cleanup temp directory
cleanup() {
    echo "Cleaning up temporary directory: $TEMP_DIR"
    rm -rf "$TEMP_DIR"
}
trap cleanup EXIT

# Step 1: Clone lettuce-core repository
echo "Step 1: Cloning lettuce-core repository..."
cd "$TEMP_DIR"
git clone --branch "$LETTUCE_BRANCH" --depth 1 "$LETTUCE_REPO" lettuce-core
cd lettuce-core

# Step 2: Build and install custom lettuce-core
echo "Step 2: Building and installing custom lettuce-core..."
echo "Setting version to $LETTUCE_VERSION"
mvn versions:set -DnewVersion="$LETTUCE_VERSION" -DgenerateBackupPoms=false

echo "Building and installing lettuce-core..."
mvn clean install -DskipTests -B

echo "Verifying installation..."
if [ -d "$HOME/.m2/repository/io/lettuce/lettuce-core/$LETTUCE_VERSION" ]; then
    echo "✅ Custom lettuce-core $LETTUCE_VERSION successfully installed"
    ls -la "$HOME/.m2/repository/io/lettuce/lettuce-core/$LETTUCE_VERSION/"
else
    echo "❌ Failed to install custom lettuce-core"
    exit 1
fi

# Step 3: Build lettuce-test-app with custom version
echo "Step 3: Building lettuce-test-app with custom lettuce-core..."
cd "$PROJECT_ROOT"

echo "Checking code formatting..."
mvn formatter:validate

echo "Building lettuce-test-app with lettuce.version=$LETTUCE_VERSION..."
mvn clean verify -Dlettuce.version="$LETTUCE_VERSION" -B

echo "Running tests..."
mvn test -Dlettuce.version="$LETTUCE_VERSION"

echo "=========================================="
echo "✅ Successfully built lettuce-test-app with custom lettuce-core $LETTUCE_VERSION"
echo "=========================================="
echo ""
echo "To run the application with the custom lettuce-core version:"
echo "  java -jar target/lettuce-test-app-0.0.1-SNAPSHOT.jar"
echo ""java -jar target/lettuce-test-app-0.0.1-SNAPSHOT.jar
echo "=========================================="
