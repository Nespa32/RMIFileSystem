#!/bin/bash

# exit on error
set -e

mkdir -p build
cd build

# kill previous rmiregistry instance
echo "Killing rmiregistry..."
pkill rmiregistry || true

# kill previous IntegrationTest
echo "Killing Integration Test..."
pkill -f "java IntegrationTest" || true

echo "Waiting..."
sleep 1

echo "Launching new rmiregistry..."
rmiregistry &

# compile
echo "Compiling..."

SRC_DIR="../src/"
BUILD_DIR="."

javac -d $BUILD_DIR ${SRC_DIR}/IntegrationTest.java \
        ${SRC_DIR}/MetaServer.java \
        ${SRC_DIR}/MetaServerInterface.java \
        ${SRC_DIR}/StorageServer.java \
        ${SRC_DIR}/StorageServerInterface.java

# launch
echo "Launching IntegrationTest..."
java -cp ${BUILD_DIR} IntegrationTest

echo "Exiting script..."

