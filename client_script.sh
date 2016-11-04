#!/bin/bash

set -e


./integration_test.sh &

sleep 5

mkdir -p build
cd build

SRC_DIR="../src/"
BUILD_DIR="."
javac -d $BUILD_DIR ${SRC_DIR}/Client.java
java -cp ${BUILD_DIR} Client
