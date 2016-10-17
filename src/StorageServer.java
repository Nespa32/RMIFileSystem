import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class StorageServer implements StorageServerInterface {

    //Fields

    private String id;
    
    //Methods
    
    public StorageServer() {}

    @Override
    public void createDir() {
	
    }

    @Override
    public void createFile() {
	
    }

    @Override
    public void delDir() {
	
    }

    @Override
    public void getFile() {

    }

    public static void main(String args[]) {

	try {

	    Registry registry = LocateRegistry.getRegistry();
	    MetaServerInterface stub = (MetaServerInterface)registry.lookup("MS");

	    String id = stub.subscribe();
	    StorageServer s = new StorageServer(id);

	    StorageServerInterface storageStub = UnitcastRemoteObject.exportObject(s, 0);
	    registry.bind(id, storageStub);
	}
	catch (Exception e) {
	    //Handling exception
	}
    }
}
