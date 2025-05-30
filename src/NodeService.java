import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface NodeService extends Remote {
    // التوابع الموجودة
    String writeFile(String department, String fileName, String content) throws RemoteException;
    List<String> listFiles(String department) throws RemoteException;
    String getFileContent(String department, String fileName) throws RemoteException;
    boolean hasFile(String department, String fileName) throws RemoteException;
    String DeleteFile(String department, String fileName) throws RemoteException;

    // توابع جديدة
    Map<String, Long> listFilesAllDepartments() throws RemoteException;

    String getFileContentByName(String fileName) throws RemoteException;
    String writeFileToNodeInDepartment(String fileName, String content, String department) throws RemoteException;
    String deleteFile(String fileName) throws RemoteException;
}
