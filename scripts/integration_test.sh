#!/bin/bash

# exit on error
set -e

# move to dir where bash script is
DIR="$(dirname "$(readlink -f "$0")")"
cd $DIR

# move to dir
cd ../build

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

# launch
echo "Launching IntegrationTest..."
java -cp . IntegrationTest

echo "Exiting script..."

