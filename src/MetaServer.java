
import java.util.*;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Remote;
import java.rmi.RemoteException;

public class MetaServer implements MetaServerInterface {

    // StorageServer ID -> Filesystem Path
    HashMap<String, String> storageServerMap;
    int nextStorageServer;

    public MetaServer()
    {
        nextStorageServer = 0;
    }


    // methods used by Client
    @Override
    public String find(String path) {
        return "find";
    }

    @Override
    public List<String> list(String path) {
        // @todo
        List<String> l = null;
        return l;
    }

    // methods used by StorageServer
    @Override
    public String subscribe() {

        String s = String.format("SS_%d", nextStorageServer);
        nextStorageServer += 1;
        // @todo: how to distribute (?)
        storageServerMap.put(s, "/");

        return s;
    }

    @Override
    public void unsubscribe(String s) throws RemoteException {
        String path = storageServerMap.remove(s);
        if (path == null)
            throw new RemoteException("StorageServer " + s + " not found!");
    }

    @Override
    public void notifyItemAdd(String path) {
        // @todo
    }

    @Override
    public void notifyItemDelete(String path) {
        // @todo
    }

    public static void main(String args[]) {

        // setup MetaServer
        try {

            MetaServer s = new MetaServer();
            MetaServerInterface metaServer = (MetaServerInterface)UnicastRemoteObject.exportObject(s, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("MS", metaServer);

            System.out.println("MetaServer ready");
        }
        catch (Exception e) {

            System.err.println("MetaServer: " + e.toString());
            e.printStackTrace();
        }
    }
 }
