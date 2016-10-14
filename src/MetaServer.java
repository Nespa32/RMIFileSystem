

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;


public class MetaServer implements MetaServerInterface {
    
    public MetaServer() { }

    @Override
    public String Find() {
	return "fInD";
    }
    
    public static void main(String args[]) {

	// setup MetaServer
	try {
	    
	    MetaServer s = new MetaServer();
	    MetaServerInterface stub = (MetaServerInterface)UnicastRemoteObject.exportObject(s, 0);
	    Registry registry = LocateRegistry.getRegistry();
	    registry.bind("Find", stub);
	    System.err.println("MetaServer ready");
	}
	catch (Exception e) {
	    
	    System.err.println("MetaServer: " + e.toString());
	    e.printStackTrace();
	}

	// test availability
	try {
	    
	    Registry registry = LocateRegistry.getRegistry();
	    MetaServerInterface stub = (MetaServerInterface)registry.lookup("Find");
	    String response = stub.Find();
	    System.out.println("Response: " + response);
	}
	catch (Exception e) {
	    // @todo
	}
    }
 }
