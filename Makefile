
SRC_DIR=src
BUILD_DIR=build

CLASSES = \
	$(SRC_DIR)/IntegrationTest.java \
	$(SRC_DIR)/MetaServer.java \
	$(SRC_DIR)/MetaServerInterface.java \
	$(SRC_DIR)/StorageServer.java \
	$(SRC_DIR)/StorageServerInterface.java \
    $(SRC_DIR)/Client.java \
    $(SRC_DIR)/Util.java

default:
	$(shell mkdir -p $(BUILD_DIR))
	javac -d $(BUILD_DIR) $(CLASSES)
	# setup initial apps.conf file for Client
	cp -n $(SRC_DIR)/apps.conf.dist $(BUILD_DIR)/apps.conf

run_integration: default
	# resets rmiregistry
	./scripts/integration_test.sh

run_metaserver: default
	# resets rmiregistry
	./scripts/run_metaserver.sh

run_storageserver:
	cd $(BUILD_DIR) && java StorageServer "data/" "/"

run_client:
	cd $(BUILD_DIR) && java Client

clean: 
	$(RM) $(BUILD_DIR)/*.class
