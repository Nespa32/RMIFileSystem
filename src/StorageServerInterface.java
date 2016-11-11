
import java.rmi.RemoteException;

import jgroup.core.ExternalGMIListener;

public interface StorageServerInterface extends ExternalGMIListener {

    //Client Methods
    public void createDir(String path) throws RemoteException;
    public void createFile(String path, byte[] bytes) throws RemoteException;
    public void delDir(String path) throws RemoteException;
    public void delFile(String path) throws RemoteException;
    public byte[] getFile(String path) throws RemoteException;
}

