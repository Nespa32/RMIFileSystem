import java.rmi.Remote;
import java.rmi.RemoteException;

public interface StorageServerInterface extends Remote {

    //Client Methods
    public void createDir() throws RemoteException;
    public void createFile() throws RemoteException;
    public void delDir() throws RemoteException;
    public void delFile() throws RemoteException;
    public void getFile() throws RemoteException;
}

