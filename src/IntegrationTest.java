
import java.util.*;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class IntegrationTest
{
    public static void main(String args[]) {

        Registry registry = null;
        // setup MetaServer
        try {

            MetaServer s = new MetaServer();
            MetaServerInterface metaServer = (MetaServerInterface)UnicastRemoteObject.exportObject(s, 0);
            registry = LocateRegistry.getRegistry();
            registry.bind("MS", metaServer);
        }
        catch (Exception e) {
            System.err.println("MetaServer setup exception: " + e.toString());
            return;
        }

        // fetch the MetaServer from the registry, ensure it works
        MetaServerInterface metaServer = null;
        try {
            metaServer = (MetaServerInterface)registry.lookup("MS");
        }
        catch (Exception e) {
            System.err.println("Couldn't find MetaServer in registry");
            return;
        }

        // setup a StorageServer
        try {

            String localDirPath = "data/";
            String remoteMountPath = "/";
            StorageServer.launchStorageServer(localDirPath, remoteMountPath);
        }
        catch (Exception e) {
            System.err.println("StorageServer setup exception: " + e.toString());
            return;
        }

        // setup a fake directory tree
        try {
            // fake StorageServer, won't actually exist
            /*
            metaServer.notifyItemAdd("/help.txt");
            metaServer.notifyItemAdd("/b/");
            metaServer.notifyItemAdd("/temp/");
            metaServer.notifyItemAdd("/temp/src/");
            metaServer.notifyItemDelete("/temp/src/");
            metaServer.notifyItemAdd("/temp/src");
            metaServer.notifyItemAdd("/temp/a");
            metaServer.notifyItemAdd("/temp/b");
            metaServer.notifyItemAdd("/am_i_human/");
            metaServer.notifyItemAdd("/am_i_human/or/");
            metaServer.notifyItemAdd("/am_i_human/or/am_i_dancer");
            */
        }
        catch (Exception e) {
            System.err.println("Exception in MetaServer tree setup: " + e.toString());
            return;
        }

        // run some tests
        try {
            System.out.println("--- TEST CALL ---");
            String s4 = metaServer.find("/help.txt");
            System.out.println(s4);
        }
        catch (Exception e) {
            System.err.println("Exception in MetaServer tests: " + e.toString());
            return;
        }
    }
}
