
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class IntegrationTest
{
    public static void main(String args[]) throws Exception {

        String metaServerId = "MS";
        Registry registry = LocateRegistry.getRegistry();

        // setup MetaServer
        {
            MetaServer s = new MetaServer();
            MetaServerInterface metaServer = (MetaServerInterface)UnicastRemoteObject.exportObject(s, 0);
            registry.bind(metaServerId, metaServer);
        }

        // fetch the MetaServer from the registry, ensure it works
        MetaServerInterface metaServer = (MetaServerInterface)registry.lookup("MS");

        // setup a StorageServer
        {
            String localDirPath = "data/";
            String remoteMountPath = "/";
            StorageServer.launchStorageServer(localDirPath, remoteMountPath);
        }

        // run some tests
        System.out.println("--- TEST CALL ---");
        // String s4 = metaServer.find("/help.txt");
        // System.out.println(s4);
    }
}
