
# exit on error
set -e

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
javac MetaServer.java MetaServerInterface.java Client.java

# launch
echo "Launching background MetaServer..."
java MetaServer &

echo "Exiting script..."
