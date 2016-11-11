
import java.util.List;
import java.rmi.RemoteException;

import jgroup.core.ExternalGMIListener;


public interface MetaServerInterface extends ExternalGMIListener {
    // Client Methods
    public String find(String path) throws RemoteException;
    public String getMD5(String filePath) throws RemoteException;
    public List<String> lstat(String path) throws RemoteException;
    // Storage Server Methods
    public void addStorageServer(String serviceId, String mountPath) throws RemoteException;
    public void delStorageServer(String mountPath) throws RemoteException;
    public void notifyItemAdd(String path, String md5sum) throws RemoteException;
    public void notifyItemDelete(String path) throws RemoteException;
}
