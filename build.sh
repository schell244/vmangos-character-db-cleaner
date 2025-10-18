#!/bin/bash

echo "=== Building VMangos Character DB Cleaner ==="

if ! command -v javac &> /dev/null; then
    echo "Java not found. Installing OpenJDK..."
    sudo apt update && sudo apt install default-jdk -y
fi

echo "Java version: $(javac -version 2>&1)"

echo "Creating build directory..."
mkdir -p build

echo "Compiling Java files..."
javac -cp "lib/mysql-connector-java-5.1.49.jar" -d build src/**/*.java src/*.java

if [ $? -eq 0 ]; then
    echo "Compilation successful!"
    echo "Creating MANIFEST.MF..."
    cat > MANIFEST.MF << EOF
Main-Class: Main
Class-Path: lib/mysql-connector-java-5.1.49.jar
EOF
    echo "Creating JAR file..."
    jar cfm vmangos-character-db-cleaner.jar MANIFEST.MF -C build .

    echo "Build complete!"
    echo "Run with: ./run.sh"
else
    echo "Compilation failed!"
    exit 1
fi
