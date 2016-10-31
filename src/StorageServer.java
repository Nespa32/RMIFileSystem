import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.*;
import java.lang.Thread;
import java.lang.Runtime;

public class StorageServer implements StorageServerInterface {

    public static void main(String args[]) {

        if (args.length < 2) {
            printUsage();
            return;
        }

        String localDirPath = args[0];
        String remoteMountPath = args[1];

        try {
            launchStorageServer(localDirPath, remoteMountPath);
        }
        catch(Exception e) {
            System.err.println("StorageServer init exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public static void launchStorageServer(String localDirPath, String remoteMountPath) throws Exception {

        // check if local dir exists
        File localDir = new File(localDirPath);
        if (localDir.exists() == false || localDir.isDirectory() == false)
            throw new Exception("LocalDirPath <" + localDirPath + "> not found");

        Registry registry = LocateRegistry.getRegistry();
        MetaServerInterface metaServer = (MetaServerInterface)registry.lookup("MS");

        String serviceId = metaServer.addStorageServer(remoteMountPath);
        StorageServer s = new StorageServer(metaServer, localDirPath, remoteMountPath);

        StorageServerInterface storageServer = (StorageServerInterface)UnicastRemoteObject.exportObject(s, 0);
        registry.bind(serviceId, storageServer);

        // remove from Registry on shutdown
        Runtime.getRuntime().addShutdownHook(new StorageServerShutdownHook(registry, s, serviceId));
    }

    public static void printUsage() {
        System.out.println("Usage: java StorageServer localDirPath remoteMountPath");
    }

    private final MetaServerInterface metaServer;
    private final String localDirPath;
    private final String remoteMountPath;

    public StorageServer(MetaServerInterface metaServer, String localDirPath,
                         String remoteMountPath) throws Exception
    {
        assert(metaServer != null);

        // localDir must exist
        File localDir = new File(localDirPath);
        if (localDir.exists() == false || localDir.isDirectory() == false)
            throw new Exception("LocalDirPath <" + localDirPath + "> not found/not dir");

        this.metaServer = metaServer;
        this.localDirPath = localDir.getAbsolutePath();
        this.remoteMountPath = remoteMountPath;

        synchronizeMetaServer();
    }

    public void close() {

        try {
            // will also call unbind() on the registry
            metaServer.delStorageServer(remoteMountPath);
        }
        catch (RemoteException e) {
            System.err.println("Failed to remove StorageServer, exception: " + e.toString());
        }
    }

    @Override
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
            metaServer.notifyItemAdd(remotePath);
        } else {

            throw new RemoteException("Failed to create directory <" + remotePath + ">");
        }
    }

    @Override
    public void createFile(String remotePath, ByteBuffer bytes) throws RemoteException {

        if (remotePath.endsWith("/") == true)
            throw new RemoteException("Path ends with '/', it's a directory");

        if (!validateRemotePath(remotePath))
            throw new RemoteException("Path <" + remotePath + "> not valid");

        String localPath = getLocalPath(remotePath);
        File file = new File(localPath);

        try {

            if (file.createNewFile()) {

                // if it throws, let the exception propagate
                metaServer.notifyItemAdd(remotePath);

                FileOutputStream fos = new FileOutputStream(localPath);
                fos.write(bytes.array());
                fos.close();

            } else {

                throw new RemoteException("Failed to create directory <" + remotePath + ">");
            }
        }
        catch (IOException e) {
            throw new RemoteException("IOException: " + e.toString());
        }
    }

    @Override
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
    public ByteBuffer getFile(String remotePath) throws RemoteException {

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
            ByteBuffer buf = ByteBuffer.wrap(data);
            return buf;
        }
        catch (IOException e) {

            throw new RemoteException("IOException: " + e.toString());
        }
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
                metaServer.notifyItemAdd(remotePath);
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
}

class StorageServerShutdownHook extends Thread {

    private final Registry registry;
    private final StorageServer s;
    private final String storageServerId;

    public StorageServerShutdownHook(Registry registry, StorageServer s,
                                     String storageServerId) {
        this.registry = registry;
        this.s = s;
        this.storageServerId = storageServerId;
    }

    @Override
    public void run() {
        s.close();
        try {
            registry.unbind(storageServerId);
        }
        catch (Exception e) {
            System.out.println("StorageServerShutdownHook - Exception: " + e.toString());
        }
    }
}
