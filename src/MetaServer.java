
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Set;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;

public class MetaServer implements MetaServerInterface {

    public static void main(String args[]) {

        // setup MetaServer
        try {

            String metaServerId = "MS";
            MetaServer s = new MetaServer();
            MetaServerInterface metaServer = (MetaServerInterface)UnicastRemoteObject.exportObject(s, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(metaServerId, metaServer);

            // remove from Registry on shutdown
            Runtime.getRuntime().addShutdownHook(new MetaServerShutdownHook(registry, metaServerId));

            System.out.println("MetaServer ready");
        }
        catch (Exception e) {

            System.err.println(e);
            e.printStackTrace();
        }
    }

    // FileSystemObject -> StorageServer ID
    private Map<FileSystemObject, String> objToStorageServer;
    // root directory, i.e '/'
    private FileSystemObject rootObj;
    // StorageServer ID counter
    private int nextStorageServerId;

    public MetaServer()
    {
        objToStorageServer = new HashMap<>();

        rootObj = new FileSystemObject(null, "/", true, null);
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
    public String getMD5(String path) throws RemoteException {

        FileSystemObject obj = getObjectForPath(path);
        if (obj == null)
            throw new RemoteException("Object at path <" + path + "> not found");

        if (obj.isDirectory())
            throw new RemoteException("Object at path <" + path + "> is not a file");

        return obj.getMD5Sum();
    }

    @Override
    public String[] lstat(String path) throws RemoteException{

        FileSystemObject obj = getObjectForPath(path);
        if (obj == null)
            throw new RemoteException("Object at path <" + path + "> not found");

        if (obj.isDirectory() == false)
            throw new RemoteException("Object at path <" + path + "> is not a directory");

        Set<String> set = obj.getChildren();
        return set.toArray(new String[set.size()]);
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
            FileSystemObject child = new FileSystemObject(obj, name, isDirectory, null);
            obj.addChild(child);

            objToStorageServer.put(child, s);
            return s;
        }
    }

    @Override
    public void delStorageServer(String mountPath) throws RemoteException {

        FileSystemObject mountObj = getObjectForPath(mountPath);
        if (mountObj == null)
            throw new RemoteException("FileSystemObject for mountPath <" + mountPath + "> not found!");

        for (Map.Entry<FileSystemObject, String> entry : objToStorageServer.entrySet()) {
            FileSystemObject obj = entry.getKey();

            if (mountObj == obj) {

                // @todo: fail if subtree contains another StorageServer
                // remove entire subtree
                if (rootObj != obj)
                    obj.getParent().removeChild(obj);
                else {

                    // don't remove root obj, just all children
                    obj.removeAllChildren();
                }

                objToStorageServer.remove(obj);
                return;
            }
        }

        throw new RemoteException("FileSystemObject for mountPath" + mountPath + " does not mount a StorageServer");
    }

    @Override
    public void notifyItemAdd(String path, String md5sum) throws RemoteException {

        // ignore root, it exists by default
        if (path.equals("/"))
            return;

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

        FileSystemObject child = new FileSystemObject(obj, name, isDirectory, md5sum);
        obj.addChild(child);
    }

    @Override
    public void notifyItemDelete(String path) throws RemoteException {

        // ignore root, it exists by default
        if (path.equals("/"))
            return;

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

class MetaServerShutdownHook extends Thread {

    private final Registry registry;
    private final String metaServerId;

    public MetaServerShutdownHook(Registry registry, String metaServerId) {
        this.registry = registry;
        this.metaServerId = metaServerId;
    }

    @Override
    public void run() {
        try {
            registry.unbind(metaServerId);
        }
        catch (Exception e) {

            System.out.println(e);
            e.printStackTrace();
        }
    }
}

class FileSystemObject implements Comparable<FileSystemObject>
{
    private FileSystemObject parent;
    private String name;
    private boolean isDirectory;
    private Map<String, FileSystemObject> children;
    private String md5sum;

    @Override
    public int compareTo(FileSystemObject other) {
        return this.name.compareTo(other.name);
    }

    public FileSystemObject(FileSystemObject parent, String name, boolean isDirectory, String md5sum) {

        this.parent = parent;
        this.name = name;
        this.isDirectory = isDirectory;
        this.children = new TreeMap<>();
        this.md5sum = md5sum;
    }

    public void addChild(FileSystemObject child) {
        children.put(child.getName(), child);
    }

    public void removeChild(FileSystemObject child) {
        children.remove(child.getName());
    }

    public void removeAllChildren() {
        children.clear();
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

    public String getMD5Sum() {
        return md5sum;
    }
}
