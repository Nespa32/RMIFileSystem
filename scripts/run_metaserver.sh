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
echo "Killing MetaServer..."
pkill -f "java MetaServer" || true

echo "Waiting..."
sleep 1

echo "Launching new rmiregistry..."
rmiregistry &

# launch
echo "Launching IntegrationTest..."
java -cp . MetaServer

echo "Exiting script..."

