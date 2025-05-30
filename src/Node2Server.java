import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class Node2Server extends UnicastRemoteObject implements NodeService {

    private final File baseDir;

    protected Node2Server() throws RemoteException {
        super();
        baseDir = new File("C:\\Users\\MSI USER\\IdeaProjects\\DistributedHomework\\node2");
        baseDir.mkdirs();
    }

    private File getDepartmentDir(String department) {
        File deptDir = new File(baseDir, department);
        if (!deptDir.exists()) deptDir.mkdirs();
        return deptDir;
    }

    private File getFile(String department, String fileName) {
        return new File(getDepartmentDir(department), fileName);
    }

    @Override
    public synchronized String writeFile(String department, String fileName, String content) throws RemoteException {
        try {
            File file = getFile(department, fileName);
            Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
            return "تم حفظ الملف بنجاح.";
        } catch (IOException e) {
            return "خطأ أثناء الحفظ: " + e.getMessage();
        }
    }

    @Override
    public synchronized List<String> listFiles(String department) throws RemoteException {
        File deptDir = getDepartmentDir(department);
        List<String> filesList = new ArrayList<>();
        File[] files = deptDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    filesList.add(f.getName());
                }
            }
        }
        return filesList;
    }

    @Override
    public synchronized String getFileContent(String department, String fileName) throws RemoteException {
        File file = getFile(department, fileName);
        if (!file.exists()) return "الملف غير موجود.";
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "خطأ في قراءة الملف: " + e.getMessage();
        }
    }

    @Override
    public synchronized boolean hasFile(String department, String fileName) throws RemoteException {
        return getFile(department, fileName).exists();
    }

    @Override
    public synchronized String DeleteFile(String department, String fileName) throws RemoteException {
        File file = getFile(department, fileName);
        if (file.exists() && file.delete()) {
            return "تم حذف الملف.";
        } else {
            return "فشل في حذف الملف أو الملف غير موجود.";
        }
    }




    @Override
    public synchronized Map<String, Long> listFilesAllDepartments() throws RemoteException {
        Map<String, Long> filesMap = new HashMap<>();
        File[] departments = baseDir.listFiles(File::isDirectory);
        if (departments != null) {
            for (File dept : departments) {
                File[] files = dept.listFiles(File::isFile);
                if (files != null) {
                    for (File f : files) {
                        String key = f.getName() + "::" + dept.getName();
                        filesMap.put(key, f.lastModified());
                    }
                }
            }
        }
        return filesMap;
    }

    private File findFileByName(String fileName) {
        File[] departments = baseDir.listFiles(File::isDirectory);
        if (departments != null) {
            for (File dept : departments) {
                File file = new File(dept, fileName);
                if (file.exists() && file.isFile()) {
                    return file;
                }
            }
        }
        return null;
    }

    @Override
    public synchronized String getFileContentByName(String fileName) throws RemoteException {
        File file = findFileByName(fileName);
        if (file == null) return "الملف غير موجود.";
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "خطأ في قراءة الملف: " + e.getMessage();
        }
    }

    @Override
    public synchronized String writeFileToNodeInDepartment(String fileName, String content, String department) throws RemoteException {
        File deptDir = new File(baseDir, department);
        if (!deptDir.exists()) deptDir.mkdirs();
        File file = new File(deptDir, fileName);
        try {
            Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
            return " تم حفظ الملف في قسم " + department;
        } catch (IOException e) {
            return " خطأ أثناء الحفظ في القسم " + department + ": " + e.getMessage();
        }
    }

    @Override
    public synchronized String deleteFile(String fileName) throws RemoteException {
        File file = findFileByName(fileName);
        if (file != null && file.exists()) {
            if (file.delete()) {
                return "️ تم حذف الملف.";
            }
        }
        return "⚠️ فشل حذف الملف أو الملف غير موجود.";
    }
}
