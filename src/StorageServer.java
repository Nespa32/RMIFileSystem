import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.nio.ByteBuffer;
import java.io.File;

public class StorageServer implements StorageServerInterface {

    private final MetaServerInterface metaServer;
    private final String serviceId;
    private final String localDirPath;
    private final String remoteMountPath;

    public StorageServer(MetaServerInterface metaServer, String localDirPath,
        String remoteMountPath) throws RemoteException
    {
        assert(metaServer);

        // localDir must exist
        File localDir = new File(localDirPath);
        if (localDir.exists() == false || localDir.isDirectory() == false)
            throw new Exception("LocalDirPath <" + localDirPath + "> not found/not dir");

        this.metaServer = metaServer;
        this.serviceId = metaServer.addStorageServer(remoteMountPath);
        this.localDirPath = localDirPath;
        this.remoteMountPath = remoteMountPath;

        synchronizeMetaServer();
    }

    @Override
    public void finalize() {
        metaServer.delStorageServer(remoteMountPath);
    }

    @Override
    public void createDir(String path) throws RemoteException {
        // @todo
    }

    @Override
    public void createFile(String path, ByteBuffer bytes) throws RemoteException {
        // @todo
    }

    @Override
    public void delDir(String path) throws RemoteException {
        // @todo
    }

    @Override
    public void delFile(String path) throws RemoteException {
        // @todo
    }

    @Override
    public ByteBuffer getFile(String path) throws RemoteException {

        ByteBuffer b = null;
        // @todo
        return b;
    }

    private void synchronizeMetaServer() {
        // @todo
    }

    public static void main(String args[]) {

        if (args.length < 2) {
            printUsage();
            return;
        }

        String localDirPath = args[0];
        String remoteMountPath args[1];

        // check if local dir exists
        File localDir = new File(localDirPath);
        if (localDir.exists() == false || localDir.isDirectory() == false) {

            System.err.println("LocalDirPath <" + localDirPath + "> not found" +
                " or is not a dir");
            printUsage();
            return;
        }

        try {

            Registry registry = LocateRegistry.getRegistry();
            MetaServerInterface metaServer = (MetaServerInterface)registry.lookup("MS");

            String serviceId = metaServer.subscribe();
            StorageServer s = new StorageServer(serviceId);

            StorageServerInterface storageServer = (StorageServerInterface)UnicastRemoteObject.exportObject(s, 0);
            registry.bind(serviceId, storageServer);
        }
        catch (Exception e) {
            System.err.println("StorageServer init exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public static void printUsage() {
        System.out.println("Usage: java StorageServer localDirPath remoteMountPath");
    }
}
