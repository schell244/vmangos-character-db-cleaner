#!/bin/bash

echo "Starting VMangos Character DB Cleaner..."

if [ ! -f "vmangos-character-db-cleaner.jar" ]; then
    echo "JAR file not found. Building first..."
    ./build.sh
fi

if [ ! -f "lib/mysql-connector-java-5.1.49.jar" ]; then
    echo "MySQL connector not found in lib/"
    echo "Please ensure lib/mysql-connector-java-5.1.49.jar exists"
    exit 1
fi

echo "Starting application..."
java -cp "vmangos-character-db-cleaner.jar:lib/mysql-connector-java-5.1.49.jar" Main
