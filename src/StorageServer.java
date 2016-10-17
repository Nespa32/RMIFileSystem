import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.nio.ByteBuffer;

public class StorageServer implements StorageServerInterface {

    private final String serviceId;

    public StorageServer(String _serviceId)
    {
        serviceId = _serviceId;
    }

    @Override
    public void createDir(String path) {
        // @todo
    }

    @Override
    public void createFile(String path, ByteBuffer bytes) {
        // @todo
    }

    @Override
    public void delDir(String path) {
	    // @todo
    }

    @Override
    public void delFile(String path) {
        // @todo
    }

    @Override
    public ByteBuffer getFile(String path) {

        ByteBuffer b = null;
        // @todo
        return b;
    }

    public static void main(String args[]) {

	    try {

	        Registry registry = LocateRegistry.getRegistry();
    	    MetaServerInterface metaServer = (MetaServerInterface)registry.lookup("MS");

	        String serviceId = metaServer.subscribe();
	        StorageServer s = new StorageServer(serviceId);

	        StorageServerInterface storageServer = (StorageServerInterface)UnicastRemoteObject.exportObject(s, 0);
	        registry.bind(serviceId, storageServer);
	    }
	    catch (Exception e) {
	        // @todo
	    }
    }
}
