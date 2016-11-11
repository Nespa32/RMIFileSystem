#!/bin/bash

set -e

mkdir -p build
cd build

SRC_DIR="../src/"
BUILD_DIR="."
javac -d $BUILD_DIR ${SRC_DIR}/Client.java
java -cp ${BUILD_DIR} \
    -Djgroup.system.config="file:example-config.xml" \
    -Djgroup.system.services="file:services.xml" \
    -Djgroup.system.applications="file:applications.xml" \
    -Djgroup.log.config="file:log4j.xml" \
    -Djgroup.log.msgcontent="false" \
    -Djgroup.log.measures="false" \
    Client
