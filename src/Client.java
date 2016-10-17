import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;

public class Client {

    public static void main(String args[]) {

	try {
	    
	    Registry registry = LocateRegistry.getRegistry();
	    MetaServerInterface stub = (MetaServerInterface)registry.lookup("MS");
	    String response = stub.Find();
	    System.out.println("Response: " + response);
	}
	catch (Exception e) {
	    // @todo
	}
    }
}
