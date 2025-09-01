#!/bin/bash

# Script to build lettuce-test-app with lettuce-core snapshot version
# Uses the publicly available lettuce-core version specified in pom.xml
# Usage: ./scripts/build.sh

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Get lettuce version from pom.xml
LETTUCE_VERSION=$(mvn -f "$PROJECT_ROOT/pom.xml" help:evaluate -Dexpression=lettuce.version -q -DforceStdout 2>/dev/null)

echo "=========================================="
echo "Building lettuce-test-app with lettuce-core"
echo "=========================================="
echo "Lettuce version: $LETTUCE_VERSION (from pom.xml)"
echo "Project root: $PROJECT_ROOT"
echo "=========================================="

# Step 1: Check code formatting
echo "Step 1: Checking code formatting..."
cd "$PROJECT_ROOT"
mvn formatter:validate

# Step 2: Build lettuce-test-app
echo "Step 2: Building lettuce-test-app..."
echo "Building lettuce-test-app with lettuce.version=$LETTUCE_VERSION..."
mvn clean verify -B

# Step 3: Run tests
echo "Step 3: Running tests..."
mvn test

echo "=========================================="
echo "✅ Successfully built lettuce-test-app with lettuce-core $LETTUCE_VERSION"
echo "=========================================="
echo ""
echo "To run the application with the custom lettuce-core version:"
echo "  java -jar target/lettuce-test-app-0.0.1-SNAPSHOT.jar"
echo ""java -jar target/lettuce-test-app-0.0.1-SNAPSHOT.jar
echo "=========================================="
