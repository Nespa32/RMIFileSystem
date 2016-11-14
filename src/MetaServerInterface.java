
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MetaServerInterface extends Remote {
    // Client Methods
    public String find(String path) throws RemoteException;
    public String getMD5(String filePath) throws RemoteException;
    public String[] lstat(String path) throws RemoteException;

    // Storage Server Methods
    // returns new Storage Server ID, used in RMI registry
    public String addStorageServer(String mountPath) throws RemoteException;
    public void delStorageServer(String mountPath) throws RemoteException;
    public void notifyItemAdd(String path, String md5sum) throws RemoteException;
    public void notifyItemDelete(String path) throws RemoteException;
}
