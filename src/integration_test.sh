#!/bin/bash

# exit on error
set -e

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
javac IntegrationTest.java MetaServer.java MetaServerInterface.java

# launch
echo "Launching IntegrationTest..."
java IntegrationTest

echo "Exiting script..."
