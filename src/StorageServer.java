
import java.math.BigInteger;
import java.rmi.RemoteException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.lang.Thread;
import java.lang.Runtime;

import jgroup.core.ExternalGMIService;
import jgroup.core.IID;
import jgroup.core.GroupManager;
import jgroup.core.MembershipListener;
import jgroup.core.MembershipService;
import jgroup.core.View;
import jgroup.core.protocols.Anycast;
import jgroup.core.protocols.Multicast;
import jgroup.core.registry.DependableRegistry;
import jgroup.core.registry.RegistryFactory;
import jgroup.core.registry.RegistryService;

public class StorageServer
        implements StorageServerInterface, MembershipListener {

    public static void main(String args[]) throws Exception {

        if (args.length < 2) {
            printUsage();
            return;
        }

        String metaServerId = args[0];
        String serviceId = args[1];
        String localDirPath = args[2];
        String remoteMountPath = args[3];

        new StorageServer(metaServerId, serviceId, localDirPath, remoteMountPath);
    }

    public static void printUsage() {
        System.out.println("Usage: java StorageServer localDirPath remoteMountPath");
    }

    private final MetaServerInterface metaServer;
    private final String localDirPath;
    private final String remoteMountPath;
    // Reference to the partitionable group membership service
    private MembershipService membershipService;
    // Reference to the external group method invocation service
    private ExternalGMIService externalGMIService;
    // The binding identifier for the servers stub in the dependable registry
    private IID bindId;

    public StorageServer(String metaServerId, String serviceId,
                         String localDirPath, String remoteMountPath) throws Exception
    {
        // localDir must exist
        File localDir = new File(localDirPath);
        if (localDir.exists() == false || localDir.isDirectory() == false)
            throw new Exception("LocalDirPath <" + localDirPath + "> not found/not dir");

        localDirPath = localDir.getAbsolutePath();
        // local dir might be missing trailing '/' after fetching absolute path
        if (localDirPath.endsWith("/") == false)
            localDirPath += "/";

        DependableRegistry registry = RegistryFactory.getRegistry();
        MetaServerInterface metaServer = (MetaServerInterface)registry.lookup(metaServerId);

        this.metaServer = metaServer;
        this.localDirPath = localDirPath;
        this.remoteMountPath = remoteMountPath;

        GroupManager gm = GroupManager.getGroupManager(this);

        this.membershipService = (MembershipService)gm.getService(MembershipService.class);
        this.externalGMIService = (ExternalGMIService)gm.getService(ExternalGMIService.class);

        RegistryService registryService = (RegistryService)gm.getService(RegistryService.class);

        // @todo: should we keep using groupId 10?
        membershipService.join(10);
        this.bindId = registryService.bind(serviceId, this);

        // remove from Registry on shutdown
        Runtime.getRuntime().addShutdownHook(new StorageServerShutdownHook(this));

        synchronizeMetaServer();
    }

    public void close() throws Exception {

        metaServer.delStorageServer(remoteMountPath);

        GroupManager gm = GroupManager.getGroupManager(this);
        RegistryService registryService = (RegistryService)gm.getService(RegistryService.class);

        registryService.unbind(bindId);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    // Methods from StorageServerInterface
    ////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    @Multicast
    public void createDir(String remotePath) throws RemoteException {

        // append '/' if needed, it's a directory path
        if (remotePath.endsWith("/") == false)
            remotePath += "/";

        if (!validateRemotePath(remotePath))
            throw new RemoteException("Path <" + remotePath + "> not valid");

        String localPath = getLocalPath(remotePath);
        File file = new File(localPath);
        if (file.mkdir()) {
            // if it throws, let the exception propagate
            metaServer.notifyItemAdd(remotePath, null); // @todo: md5sum
        } else {

            throw new RemoteException("Failed to create directory <" + remotePath + ">");
        }
    }

    @Override
    @Multicast
    public void createFile(String remotePath, byte[] bytes) throws RemoteException {

        if (remotePath.endsWith("/") == true)
            throw new RemoteException("Path ends with '/', it's a directory");

        if (!validateRemotePath(remotePath))
            throw new RemoteException("Path <" + remotePath + "> not valid");

        String localPath = getLocalPath(remotePath);
        File file = new File(localPath);

        try {

            if (file.createNewFile()) {

                // if it throws, let the exception propagate
                metaServer.notifyItemAdd(remotePath, null); // @todo: md5sum

                FileOutputStream fos = new FileOutputStream(localPath);
                fos.write(bytes);
                fos.close();

            } else {

                throw new RemoteException("Failed to create file <" + remotePath + ">");
            }
        }
        catch (IOException e) {
            throw new RemoteException("IOException: " + e.toString());
        }
    }

    @Override
    @Multicast
    public void delDir(String remotePath) throws RemoteException {

        // append '/' if needed, it's a directory path
        if (remotePath.endsWith("/") == false)
            remotePath += "/";

        if (!validateRemotePath(remotePath))
            throw new RemoteException("Path <" + remotePath + "> not valid");

        String localPath = getLocalPath(remotePath);
        File file = new File(localPath);
        if (file.isDirectory() == false)
            throw new RemoteException("File at <" + remotePath + "> is not a directory");

        String[] files = file.list();
        if (files.length > 0)
            throw new RemoteException("Directory is not empty");

        if (file.delete()) {
            // if it throws, let the exception propagate
            metaServer.notifyItemDelete(remotePath);
        } else {

            throw new RemoteException("Failed to delete directory <" + remotePath + ">");
        }
    }

    @Override
    @Multicast
    public void delFile(String remotePath) throws RemoteException {

        if (remotePath.endsWith("/") == true)
            throw new RemoteException("Path ends with '/', it's a directory");

        if (!validateRemotePath(remotePath))
            throw new RemoteException("Path <" + remotePath + "> not valid");

        String localPath = getLocalPath(remotePath);
        File file = new File(localPath);
        if (file.isDirectory() == true)
            throw new RemoteException("File at <" + remotePath + "> is a directory");

        if (file.delete()) {
            // if it throws, let the exception propagate
            metaServer.notifyItemDelete(remotePath);
        } else {

            throw new RemoteException("Failed to delete file <" + remotePath + ">");
        }
    }

    @Override
    @Anycast
    public byte[] getFile(String remotePath) throws RemoteException {

        if (remotePath.endsWith("/") == true)
            throw new RemoteException("Path ends with '/', it's a directory");

        if (!validateRemotePath(remotePath))
            throw new RemoteException("Path <" + remotePath + "> not valid");

        String localPath = getLocalPath(remotePath);
        File file = new File(localPath);
        if (file.isDirectory() == true)
            throw new RemoteException("File at <" + remotePath + "> is a directory");

        Path path = file.toPath();
        try {
            byte[] data = Files.readAllBytes(path);
            return data;
        }
        catch (IOException e) {

            throw new RemoteException("IOException: " + e.toString());
        }
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

    private void synchronizeMetaServer() {

        System.out.println("synchronizeMetaServer call");
        // BFS through the directory tree
        LinkedList<File> files = new LinkedList<>();
        files.addLast(new File(localDirPath));

        while (!files.isEmpty()) {
            File f = files.removeFirst();
            String localPath = f.getAbsolutePath();

            if (f.isDirectory()) {
                if (localPath.endsWith("/") == false)
                    localPath += "/";

                String[] ls = f.list();
                for (String fileStr : ls) {
                    files.addLast(new File(localPath + fileStr));
                }
            }

            // calc remote path
            String remotePath = localPath.replaceFirst("^" + localDirPath, remoteMountPath);
            // remove double // (can happen during path conversion)
            remotePath = remotePath.replaceAll("//", "/");

            try {
                System.out.println("Pushing <" + remotePath + ">");
                metaServer.notifyItemAdd(remotePath, null); // @todo: md5sum
            } catch (RemoteException e) {
                System.err.println("synchronizeMetaServer - RemoteException: " + e.toString());
            }
        }
    }

    private boolean validateRemotePath(String remotePath) {

        // @todo: lots of validation logic
        // can't have dir manipulation
        if (remotePath.contains("/./") || remotePath.contains("/../"))
            return false;

        // needs to be a path that actually belongs to this StorageServer
        // also ensures that it's an absolute path
        if (!remotePath.startsWith(remoteMountPath))
            return false;

        return true;
    }

    private String getLocalPath(String remotePath) {

        return remotePath.replaceFirst("^" + remoteMountPath, localDirPath);
    }

    private String getMD5Sum(byte[] fileBytes) {

        try {

            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.reset();
            messageDigest.update(fileBytes);
            byte[] md5Hash = messageDigest.digest();
            String md5Str = new BigInteger(1, md5Hash).toString(16);
            return md5Str;
        }
        catch (NoSuchAlgorithmException e) {

            System.err.println("NoSuchAlgorithmException: " + e.toString());
            return null;
        }
    }
}

class StorageServerShutdownHook extends Thread {

    private final StorageServer storageServer;

    public StorageServerShutdownHook(StorageServer storageServer) {
        this.storageServer = storageServer;
    }

    @Override
    public void run() {

        try {
            storageServer.close();
        }
        catch (Exception e) {
            System.out.println("StorageServerShutdownHook - Exception: " + e.toString());
        }
    }
}
