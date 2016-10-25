
# exit on error
set -e

mkdir -p build
cd build

# kill previous rmiregistry instance
echo "Killing rmiregitry..."
pkill rmiregistry || true

# kill previous MetaServers
echo "Killing MetaServers..."
ps -aux | grep "[j]ava MetaServer" | sed -e 's/  / /g' | cut -f 4 -d' ' | xargs kill || true

echo "Waiting..."
sleep 1

# launch clean new instance
echo "Launching new rmiregistry..."
rmiregistry &

# compile
echo "Compiling..."

SRC_DIR="../src/"
BUILD_DIR="."

javac -d $BUILD_DIR ${SRC_DIR}/MetaServer.java \
        ${SRC_DIR}/MetaServerInterface.java

# launch
echo "Launching background MetaServer..."
java -cp ${SRC_DIR} MetaServer &

echo "Exiting script..."
