
import java.util.*;
import java.rmi.RemoteException;

import jgroup.core.*;
import jgroup.core.protocols.Anycast;
import jgroup.core.protocols.Multicast;
import jgroup.core.registry.RegistryService;

public class MetaServer
        implements MetaServerInterface, MembershipListener {

    public static void main(String args[]) throws Exception {

        // setup MetaServer
        String metaServerId = args[0];
        MetaServer metaServer = new MetaServer(metaServerId);

        System.out.println("MetaServer ready");
    }

    // FileSystemObject -> StorageServer ID
    private Map<FileSystemObject, String> objToStorageServer;
    // root directory, i.e '/'
    private FileSystemObject rootObj;
    // Reference to the partitionable group membership service
    private MembershipService membershipService;
    // Reference to the external group method invocation service
    private ExternalGMIService externalGMIService;
    // The binding identifier for the servers stub in the dependable registry
    private IID bindId;

    public MetaServer(String serviceId) throws Exception {

        this.objToStorageServer = new HashMap<>();
        this.rootObj = new FileSystemObject(null, "/", true);

        GroupManager gm = GroupManager.getGroupManager(this);

        this.membershipService = (MembershipService)gm.getService(MembershipService.class);
        this.externalGMIService = (ExternalGMIService)gm.getService(ExternalGMIService.class);

        RegistryService registryService = (RegistryService)gm.getService(RegistryService.class);

        // @todo: should we keep using groupId 10?
        membershipService.join(10);
        this.bindId = registryService.bind(serviceId, this);

        // remove from Registry on shutdown
        Runtime.getRuntime().addShutdownHook(new MetaServerShutdownHook(this));
    }

    public void close() throws Exception {

        GroupManager gm = GroupManager.getGroupManager(this);
        RegistryService registryService = (RegistryService)gm.getService(RegistryService.class);

        registryService.unbind(bindId);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    // Methods from MetaServerInterface
    ////////////////////////////////////////////////////////////////////////////////////////////

    // methods used by Client
    @Override
    @Anycast
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
    @Anycast
    public String getMD5(String filePath) throws RemoteException {

        // @todo
        return null;
    }

    @Override
    @Anycast
    public List<String> lstat(String path) throws RemoteException{

        FileSystemObject obj = getObjectForPath(path);
        if (obj == null)
            throw new RemoteException("Object at path <" + path + "> not found");

        if (obj.isDirectory() == false)
            throw new RemoteException("Object at path <" + path + "> is not a directory");

        Set<String> set = obj.getChildren();
        return new ArrayList<>(set);
    }

    // methods used by StorageServer
    @Override
    @Multicast
    public void addStorageServer(String serviceId, String mountPath) throws RemoteException {

        boolean isRootPath = mountPath.equals("/");
        boolean isRootSetup = objToStorageServer.containsKey(rootObj);
        if (isRootPath) {
            if (isRootSetup)
                throw new RemoteException("Root StorageServer is already setup");

            objToStorageServer.put(rootObj, serviceId);
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

            objToStorageServer.put(child, serviceId);
        }
    }

    @Override
    @Multicast
    public void delStorageServer(String mountPath) throws RemoteException {

        FileSystemObject mountObj = getObjectForPath(mountPath);
        if (mountObj == null)
            throw new RemoteException("FileSystemObject for mountPath <" + mountPath + "> not found!");

        for (Map.Entry<FileSystemObject, String> entry : objToStorageServer.entrySet()) {
            FileSystemObject obj = entry.getKey();

            if (mountObj == obj) {
                objToStorageServer.remove(entry.getKey());
                return;
            }
        }

        throw new RemoteException("FileSystemObject for mountPath" + mountPath + " does not mount a StorageServer");
    }

    @Override
    @Multicast
    public void notifyItemAdd(String path, String md5sum) throws RemoteException {

        // @todo: use md5

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
    @Multicast
    public void notifyItemDelete(String path) throws RemoteException {

        FileSystemObject obj = getObjectForPath(path);
        if (obj == null)
            throw new RemoteException("Item not found, can't delete");
        else if (obj == rootObj)
            throw new RemoteException("Did you really just try to delete root?");

        obj.getParent().removeChild(obj);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    // Methods from MembershipListener
    ////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    // @AllowDuplicateViews
    public void viewChange(View view)
    {
        // System.out.println("  ** HelloServer **" + view);

        try {
      /*
       * The time() method is defined in the InternalHello interface and
       * is marked as a group internal method.  By definition, all group
       * internal methods will return an array of values instead of a
       * single value as with standard remote (or external group) method
       * calls.
       */
            // Object[] objs = (Object[]) internalHello.time();
            // for (int i = 0; i < objs.length; i++)
            // System.out.println("Time: " + objs[i]);
            // answer.setTime(objs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void prepareChange()
    {
        // System.out.println("The current view is invalid; please await a new view...");
    }

    @Override
    public void hasLeft()
    {
        // System.out.println("I have left the group");
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

    private final MetaServer metaServer;

    public MetaServerShutdownHook(MetaServer metaServer) {
        this.metaServer = metaServer;
    }

    @Override
    public void run() {

        try {
            metaServer.close();
        }
        catch (Exception e) {
            System.out.println("MetaServerShutdownHook - Exception: " + e.toString());
        }
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
        this.children = new TreeMap<>();
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
