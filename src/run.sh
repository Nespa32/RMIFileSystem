
# exit on error
set -e

# kill previous rmiregistry instance
echo "Killing rmiregitry..."
pkill rmiregistry || true

echo "Waiting..."
sleep 1

# launch clean new instance
echo "Launching new rmiregistry..."
rmiregistry &

# compile
echo "Compiling..."
javac MetaServer.java MetaServerInterface.java

# launch
echo "Launching..."
java MetaServer

echo "Exiting script..."

