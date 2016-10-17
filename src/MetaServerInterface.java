import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MetaServerInterface extends Remote {
    // Client Methods
    public String find() throws RemoteException;
    public List<String> list() throws RemoteException;
    // Storage Server Methods
    public String subscribe() throws RemoteException;
    public void unsubscribe() throws RemoteException;
    public void notifyItemAdd(String path) throws RemoteException;
    public void notifyItemDelete(String path) throws RemoteException;
}
