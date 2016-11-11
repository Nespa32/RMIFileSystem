
import jgroup.core.registry.DependableRegistry;
import jgroup.core.registry.RegistryFactory;

public class IntegrationTest
{
    public static void main(String args[]) throws Exception {

        String metaServerId = "MS";
        String storageServerId = "S1";

        // setup MetaServer
        {
            MetaServer s = new MetaServer(metaServerId);
        }

        // fetch the MetaServer from the registry, ensure it works
        DependableRegistry registry = RegistryFactory.getRegistry();
        MetaServerInterface metaServer = (MetaServerInterface)registry.lookup(metaServerId);

        // setup a StorageServer
        {
            String localDirPath = "data/";
            String remoteMountPath = "/";
            StorageServer s = new StorageServer(metaServerId, storageServerId, localDirPath, remoteMountPath);
        }

        StorageServerInterface storageServer = (StorageServerInterface)registry.lookup(storageServerId);

        // run some tests
        System.out.println("--- TEST CALL ---");
        // String s4 = metaServer.find("/help.txt");
        // System.out.println(s4);
    }
}
