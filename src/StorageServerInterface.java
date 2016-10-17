import java.rmi.Remote;
import java.rmi.RemoteException;

public interface StorageServerInterface extends Remote {

    //Client Methods
    public void createDir(String path) throws RemoteException;
    public void createFile(String path, ByteBuffer bytes) throws RemoteException;
    public void delDir(String path) throws RemoteException;
    public void delFile(String path) throws RemoteException;
    public ByteBuffer getFile(String path) throws RemoteException;
}

