
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

    // StorageServer ID -> FileSystemObject
    private Map<String, FileSystemObject> storageServerToObj;
    // FileSystemObject -> StorageServer ID
    private Map<FileSystemObject, String> objToStorageServer;
    // root directory, i.e '/'
    private FileSystemObject rootObj;
    // StorageServer ID counter
    private int nextStorageServerId;

    public MetaServer()
    {
        storageServerToObj = new HashMap<String, FileSystemObject>();
        objToStorageServer = new HashMap<FileSystemObject, String>();

        rootObj = new FileSystemObject(null, "/", true);
        nextStorageServerId = 0;
    }


    // methods used by Client
    @Override
    public String find(String path) throws RemoteException {

        FileSystemObject obj = getObjectForPath(path);
        if (obj == null)
            throw new RemoteException("Object at path <" + path + "> not found");

        String serviceId = getStorageServerForObject(obj);
        if (serviceId == null)
            throw new RemoteException("No StorageServer available for <" + path + ">");

        return serviceId;
    }

    @Override
    public List<String> list(String path) throws RemoteException{

        FileSystemObject obj = getObjectForPath(path);
        if (obj == null)
            throw new RemoteException("Object at path <" + path + "> not found");

        if (obj.isDirectory() == false)
            throw new RemoteException("Object at path <" + path + "> is not a directory");

        Set<String> set = obj.getChildren();
        return new ArrayList<String>(set);
    }

    // methods used by StorageServer
    @Override
    public String addStorageServer(String mountPath) throws RemoteException {

        String s = String.format("SS_%d", nextStorageServerId);
        nextStorageServerId += 1;

        boolean isRootPath = mountPath.equals("/");
        boolean isRootSetup = objToStorageServer.containsKey(rootObj);
        if (isRootPath) {
            if (isRootSetup)
                throw new RemoteException("Root StorageServer is already setup");

            storageServerToObj.put(s, rootObj);
            objToStorageServer.put(rootObj, s);
            return s;
        }
        else {
            // root StorageServer has to be the first to mount, others need to retry
            if (isRootSetup == false)
                throw new RemoteException("Missing Root StorageServer");

            if (getObjectForPath(mountPath) != null)
                throw new RemoteException("Path <" + mountPath + "> already exists");

            String initialMountPath = "/";
            String[] splitPath = mountPath.split("/");
            for (int i = 1; i < splitPath.length - 1; ++i)
                initialMountPath += splitPath[i] + "/";

            FileSystemObject obj = getObjectForPath(initialMountPath);
            if (obj == null)
                throw new RemoteException("Path <" + initialMountPath + "> not found");

            // create new StorageServer root
            String name = splitPath[splitPath.length - 1];
            boolean isDirectory = true;
            FileSystemObject child = new FileSystemObject(obj, name, isDirectory);
            obj.addChild(child);

            storageServerToObj.put(s, child);
            objToStorageServer.put(child, s);
            return s;
        }
    }

    @Override
    public void delStorageServer(String mountPath) throws RemoteException {

        storageServerToObj.remove(mountPath);
        for (Map.Entry<FileSystemObject, String> entry : objToStorageServer.entrySet()) {
            String s = entry.getValue();
            if (s == mountPath) {
                storageServerToObj.remove(mountPath);
                objToStorageServer.remove(entry.getKey());
                return;
            }
        }

        throw new RemoteException("StorageServer mountPath " + mountPath + " not found!");
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

        String subPath = "/";
        for (int i = 1; i < splitPath.length - 1; ++i)
            subPath += splitPath[i] + "/";

        FileSystemObject obj = getObjectForPath(subPath);
        if (obj == null)
            throw new RemoteException("Couldn't find parent directory for item");

        FileSystemObject child = new FileSystemObject(obj, name, isDirectory);
        obj.addChild(child);
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
        for (int i = 1; i < splitPath.length; ++i)
        {
            String s = splitPath[i];
            obj = obj.getChild(s);
            if (obj == null)
                break;
        }

        return obj;
    }

    private String getStorageServerForObject(FileSystemObject obj) {

        String storageServerId = null;
        while (obj != null) {
            storageServerId = objToStorageServer.get(obj);
            if (storageServerId == null)
                obj = obj.getParent(); // obj will belong to parent's StorageServer
            else
                break; // found the closest StorageServer
        }

        return storageServerId;
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

    public Set<String> getChildren() {
        Set<String> set = children.keySet();
        return set;
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

    public boolean isDirectory() {
        return isDirectory;
    }
}
