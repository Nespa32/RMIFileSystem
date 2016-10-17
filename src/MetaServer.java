
import java.util.*;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Remote;
import java.rmi.RemoteException;

public class MetaServer implements MetaServerInterface {

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

            System.err.println("MetaServer exception: " + e.toString());
            e.printStackTrace();
        }
    }

    // StorageServer ID -> Filesystem Path
    private Map<String, String> storageServerMap;
    private FileSystemObject rootObj;
    private int nextStorageServer;

    public MetaServer()
    {
        storageServerMap = new HashMap<String, String>();
        rootObj = new FileSystemObject(null, "/", true);
        nextStorageServer = 0;
    }


    // methods used by Client
    @Override
    public String find(String path) throws RemoteException {

        // @todo
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
    public void notifyItemAdd(String path) throws RemoteException {

        String[] splitPath = path.split("/");
        if (splitPath.length == 0)
            throw new RemoteException("Bad path <" + path + ">");

        FileSystemObject existingObj = getObjectForPath(path);
        if (existingObj != null)
            throw new RemoteException("Item already exists");

        boolean isDirectory = path.endsWith("/");
        String name = splitPath[splitPath.length - 1];

        String subPath = "";
        for (int i = 0; i < splitPath.length - 1; ++i)
            subPath += "/" + splitPath[i];

        FileSystemObject obj = getObjectForPath(subPath);
        if (obj == null)
            throw new RemoteException("Couldn't find parent directory for item");

        FileSystemObject child = new FileSystemObject(obj, name, isDirectory);
    }

    @Override
    public void notifyItemDelete(String path) throws RemoteException {

        FileSystemObject obj = getObjectForPath(path);
        if (obj == null)
            throw new RemoteException("Item not found, can't delete");
        else if (obj == rootObj)
            throw new RemoteException("Did you really just try to delete root?");

        obj.getParent().removeChild(obj);
    }


    private FileSystemObject getObjectForPath(String path) {

        String[] splitPath = path.split("/");

        FileSystemObject obj = rootObj;
        for (String s : splitPath)
        {
            obj = obj.getChild(s);
            if (obj == null)
                break;
        }

        return obj;
    }
}

class FileSystemObject implements Comparable<FileSystemObject>
{
    private FileSystemObject parent;
    private String name;
    private boolean isDirectory;
    private Map<String, FileSystemObject> children;

    @Override
    public int compareTo(FileSystemObject other) {
        return this.name.compareTo(other.name);
    }

    public FileSystemObject(FileSystemObject parent, String name, boolean isDirectory) {

        this.parent = parent;
        this.name = name;
        this.isDirectory = isDirectory;
        this.children = new TreeMap<String, FileSystemObject>();
    }

    public void addChild(FileSystemObject child) {
        children.put(child.getName(), child);
    }

    public void removeChild(FileSystemObject child) {
        children.remove(child.getName());
    }

    public FileSystemObject getChild(String name) {

        FileSystemObject obj = children.get(name);
        return obj;
    }

    public FileSystemObject getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }
}
