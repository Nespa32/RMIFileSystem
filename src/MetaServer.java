import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;


public class MetaServer implements MetaServerInterface {

    
    
    public MetaServer() { }

    // Client Methods
    
    @Override
    public String find() {
	return "fInD";
    }

    @Override
    public List<String>  list() {
    }

    // Storage Server Methods

    @Override
    public String subscribe() {
    }

    @Override
    public void unsubscribe() {
    }

    @Override
    public void notifyItemAdd(String path) {
    }

    @Override
    public void notifyItemDelete(String path) {
    }
    
    public static void main(String args[]) {

	// setup MetaServer
	try {
	    
	    MetaServer s = new MetaServer();
	    MetaServerInterface stub = (MetaServerInterface)UnicastRemoteObject.exportObject(s, 0);
	    Registry registry = LocateRegistry.getRegistry();
	    registry.bind("MS", stub);
	    System.err.println("MetaServer ready");
	}
	catch (Exception e) {
	    
	    System.err.println("MetaServer: " + e.toString());
	    e.printStackTrace();
	}

	// test availability
    }
 }
