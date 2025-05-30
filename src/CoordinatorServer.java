import java.rmi.Naming;
import java.rmi.RemoteException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CoordinatorServer {
    static final String[] NODE_URLS = {
            "rmi://localhost:6001/Node1Service",
            "rmi://localhost:6002/Node2Service",
            "rmi://localhost:6003/Node3Service"
    };

    static final String[] DEPARTMENTS = {"development", "graphic_design", "QA"};
    static NodeService[] nodeServices = new NodeService[NODE_URLS.length];
    static int currentNodeIndex = 0;
    static ConcurrentHashMap<String, ReentrantReadWriteLock> fileLocks = new ConcurrentHashMap<>();


    public static void main(String[] args) {
        try {
            for (int i = 0; i < NODE_URLS.length; i++) {
                nodeServices[i] = (NodeService) Naming.lookup(NODE_URLS[i]);
            }
            // إضافة جدولة المزامنة كل يوم الساعة 11:59
            scheduleDailySync();
        } catch (MalformedURLException | NotBoundException | RemoteException e) {
            System.err.println("خطأ في الاتصال بخدمات RMI: " + e.getMessage());
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("Coordinator Server يعمل على المنفذ 5000...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("خطأ في الـ Coordinator: " + e.getMessage());
        }
    }
    private static void scheduleDailySync() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        Runnable syncTask = new Runnable() {
            public void run() {
                System.out.println("🔄 بدء المزامنة المجدولة: " + LocalDateTime.now());
                synchronizeFilesAcrossNodes();
            }
        };

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime firstRun = now.withHour(23).withMinute(59).withSecond(0).withNano(0);

        if (now.isAfter(firstRun)) {
            firstRun = firstRun.plusDays(1);
        }

        long initialDelay = Duration.between(now, firstRun).getSeconds();
        long period = 24 * 60 * 60; // كل يوم بالثواني

        scheduler.scheduleAtFixedRate(syncTask, initialDelay, period, TimeUnit.SECONDS);
    }


    private static synchronized int getNextNodeIndex() {
        int index = currentNodeIndex;
        currentNodeIndex = (currentNodeIndex + 1) % nodeServices.length;
        return index;
    }

    private static void showTaskMenu(PrintWriter out) {
        out.println("\n--- قائمة المهام ---");
        out.println("1. رفع ملف إلى مجلد القسم");
        out.println("2. تعديل ملف");
        out.println("3. حذف ملف");
        out.println("4. استعراض ملف من قسم آخر");
        out.println("5. إنهاء الجلسة");
        out.println("اختر خياراً:");
    }

    private static ReentrantReadWriteLock getLockForFile(String department, String filename) {
        String key = department + ":" + filename;
        fileLocks.putIfAbsent(key, new ReentrantReadWriteLock());
        return fileLocks.get(key);
    }
    private static void handleClient(Socket clientSocket) {
        try (
                BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            clientOut.println("أدخل اسم المستخدم:");
            String username = clientIn.readLine();
            clientOut.println("أدخل كلمة المرور:");
            String password = clientIn.readLine();

            String role = UserManager.validateUser(username, password);
            if (role == null) {
                clientOut.println("بيانات تسجيل الدخول غير صحيحة.");
                return;
            }

            String token = UUID.randomUUID().toString();
            UserManager.updateToken(username, token);
            clientOut.println("تم تسجيل الدخول بنجاح كـ " + role);

            if (role.equals("manager")) {
                while (true) {
                    clientOut.println("--- مهام الأدمن ---");
                    clientOut.println("1. إضافة موظف جديد");
                    clientOut.println("2. خروج");
                    clientOut.println("اختر خياراً:");

                    String adminOption = clientIn.readLine();

                    if ("1".equals(adminOption)) {
                        clientOut.println("أدخل اسم المستخدم الجديد:");
                        String newUsername = clientIn.readLine();

                        clientOut.println("أدخل كلمة المرور:");
                        String newPassword = clientIn.readLine();

                        clientOut.println("أدخل القسم:");
                        String newDept = clientIn.readLine();

                        boolean added = UserManager.addEmployeeToDatabase(newUsername, newPassword, "employee", newDept);
                        clientOut.println(added ? " تمت إضافة الموظف بنجاح." : " فشلت عملية الإضافة.");

                    } else if ("2".equals(adminOption)) {
                        clientOut.println("تم تسجيل الخروج.");
                        break;
                    } else {
                        clientOut.println("⚠️ خيار غير معروف.");
                    }
                }
                return;
            }


            String department = UserManager.getDepartmentByUsername(username);

            while (true) {
                showTaskMenu(clientOut);
                String option = clientIn.readLine();

                if (option == null || option.equals("5")) {
                    clientOut.println("تم تسجيل الخروج. إلى اللقاء!");
                    break;
                }

                switch (option) {
                    case "1":
                        clientOut.println("أدخل اسم الملف لرفعه:");
                        String fileName = clientIn.readLine();
                        if (!fileName.endsWith(".txt")) fileName += ".txt";
                        clientOut.println("أدخل محتوى الملف (finish لإنهاء):");
                        StringBuilder content = new StringBuilder();
                        String line;
                        while (!(line = clientIn.readLine()).equalsIgnoreCase("finish")) {
                            content.append(line).append("\n");
                        }
                        int nodeIndex = getNextNodeIndex();
                        String result = nodeServices[nodeIndex].writeFile(department, fileName, content.toString());
                        clientOut.println(result);
                        break;
                    case "2": // تعديل ملف
                        Set<String> uniqueFiles = new LinkedHashSet<>();
                        for (NodeService service : nodeServices) {
                            try {
                                uniqueFiles.addAll(service.listFiles(department));
                            } catch (Exception e) {
                                clientOut.println("خطأ عند الوصول لعقدة.");
                            }
                        }

                        if (uniqueFiles.isEmpty()) {
                            clientOut.println("لا توجد ملفات.");
                            break;
                        }

                        List<String> files = new ArrayList<>(uniqueFiles); // للتحويل لقائمة مرتبة
                        for (int i = 0; i < files.size(); i++) {
                            clientOut.println((i + 1) + ". " + files.get(i));
                        }

                        clientOut.println("اختر رقم الملف للتعديل:");

                        int fileChoice;
                        try {
                            fileChoice = Integer.parseInt(clientIn.readLine()) - 1;
                        } catch (NumberFormatException e) {
                            clientOut.println("⚠️ إدخال غير صالح.");
                            break;
                        }

                        if (fileChoice < 0 || fileChoice >= files.size()) {
                            clientOut.println("⚠️ رقم غير صالح.");
                            break;
                        }

                        String fileToEdit = files.get(fileChoice);
                        boolean fileFound = false;

                        for (NodeService service : nodeServices) {
                            if (service.hasFile(department, fileToEdit)) {
                                fileFound = true;

                                ReentrantReadWriteLock lock = getLockForFile(department, fileToEdit);

                                // حاول الحصول على القفل بدون انتظار
                                boolean lockAcquired = lock.writeLock().tryLock();
                                if (!lockAcquired) {
                                   clientOut.println("⚠️ الملف '" + fileToEdit + "' مقفول حاليًا ويجري تعديله من قبل مستخدم آخر. يرجى المحاولة لاحقاً.");
                                    break;
                                }

                                try {
                                    // قراءة المحتوى الحالي
                                    String currentContent = service.getFileContent(department, fileToEdit);
                                    clientOut.println("----- محتوى الملف الحالي -----");
                                    clientOut.println(currentContent);

                                    clientOut.println(" أدخل المحتوى الجديد كاملًا. اكتب 'finish' بسطر منفصل عند الانتهاء:");

                                    List<String> newLines = new ArrayList<>();
                                    while (!(line = clientIn.readLine()).equalsIgnoreCase("finish")) {
                                        newLines.add(line);
                                    }

                                    String newContent = String.join(System.lineSeparator(), newLines);
                                    result = service.writeFile(department, fileToEdit, newContent);

                                    clientOut.println(result);
                                } finally {
                                    lock.writeLock().unlock();
                                }

                                break;
                            }
                        }

                        if (!fileFound) {
                            clientOut.println("⚠️ الملف غير موجود في أي عقدة.");
                        }
                        break;

                    case "3":
                        List<String> delList = new ArrayList<>();

                        clientOut.println("جاري جلب الملفات المتاحة للحذف...");

                        for (NodeService service : nodeServices) {
                            try {
                                List<String> filesFromNode = service.listFiles(department);
                                if (filesFromNode != null) {
                                    delList.addAll(filesFromNode);
                                }
                            } catch (Exception e) {
                                clientOut.println("⚠️ خطأ عند الوصول لعقدة: " + e.getMessage());
                            }
                        }

                        // إزالة التكرارات مع الحفاظ على الترتيب
                        uniqueFiles = new LinkedHashSet<>(delList);

                        if (uniqueFiles.isEmpty()) {
                            clientOut.println("لا توجد ملفات متاحة للحذف.");
                            break;
                        }

                        List<String> fileList = new ArrayList<>(uniqueFiles);

                        for (int i = 0; i < fileList.size(); i++) {
                            clientOut.println((i + 1) + ". " + fileList.get(i));
                        }
                        clientOut.println("أدخل رقم الملف للحذف:");

                        int delIndex;
                        try {
                            delIndex = Integer.parseInt(clientIn.readLine()) - 1;
                        } catch (NumberFormatException e) {
                            clientOut.println("⚠️ إدخال غير صالح.");
                            break;
                        }

                        if (delIndex < 0 || delIndex >= fileList.size()) {
                            clientOut.println("⚠️ رقم غير صالح.");
                            break;
                        }

                        String fileToDelete = fileList.get(delIndex);
                        boolean deleted = false;

                        for (NodeService service : nodeServices) {
                            try {
                                if (service.hasFile(department, fileToDelete)) {
                                    service.DeleteFile(department, fileToDelete);
                                    deleted = true;
                                }
                            } catch (Exception e) {
                                clientOut.println("⚠️ فشل الحذف من عقدة: " + e.getMessage());
                            }
                        }

                        if (deleted) {
                            clientOut.println(" تم حذف الملف: " + fileToDelete);
                        } else {
                            clientOut.println(" تعذر العثور على الملف في أي عقدة.");
                        }
                        break;

                    case "4":
                        // طباعة قائمة الأقسام
                        for (int i = 0; i < DEPARTMENTS.length; i++) {
                            clientOut.println((i + 1) + ". " + DEPARTMENTS[i]);
                        }

                        clientOut.println("اختر القسم:");
                        int deptIndex;
                        try {
                            deptIndex = Integer.parseInt(clientIn.readLine()) - 1;
                            if (deptIndex < 0 || deptIndex >= DEPARTMENTS.length) {
                                clientOut.println("⚠️ رقم قسم غير صالح.");
                                break;
                            }
                        } catch (NumberFormatException e) {
                            clientOut.println("⚠️ إدخال غير صالح.");
                            break;
                        }

                        String targetDept = DEPARTMENTS[deptIndex];
                        Set<String> deptFiles = new LinkedHashSet<>();

                        // تنفيذ متوازي لطلبات listFiles لكل عقدة
                        ExecutorService executor = Executors.newFixedThreadPool(nodeServices.length);
                        List<Future<List<String>>> futures = new ArrayList<>();

                        for (NodeService node : nodeServices) {
                            futures.add(executor.submit(() -> {
                                try {
                                    return node.listFiles(targetDept);
                                } catch (Exception e) {
                                    return Collections.emptyList(); // في حال فشل العقدة
                                }
                            }));
                        }

                        for (Future<List<String>> future : futures) {
                            try {
                                deptFiles.addAll(future.get()); // دمج النتائج من كل العقد
                            } catch (Exception e) {
                                // تجاهل الفشل في الحصول على نتيجة من عقدة معينة
                            }
                        }

                        executor.shutdown();

                        if (deptFiles.isEmpty()) {
                            clientOut.println("لا توجد ملفات في القسم " + targetDept);
                            break;
                        }

                        List<String> viewList = new ArrayList<>(deptFiles);
                        for (int i = 0; i < viewList.size(); i++) {
                            clientOut.println((i + 1) + ". " + viewList.get(i));
                        }

                        clientOut.println("اختر الملف لعرضه:");
                        int viewIndex;
                        try {
                            viewIndex = Integer.parseInt(clientIn.readLine()) - 1;
                            if (viewIndex < 0 || viewIndex >= viewList.size()) {
                                clientOut.println("⚠️ رقم ملف غير صالح.");
                                break;
                            }
                        } catch (NumberFormatException e) {
                            clientOut.println("⚠️ إدخال غير صالح.");
                            break;
                        }

                        String fileToView = viewList.get(viewIndex);
                        boolean found = false;

                        //  فحص الملف بنفس الطريقة (تسلسليًا)
                        for (NodeService service : nodeServices) {
                            try {
                                if (service.hasFile(targetDept, fileToView)) {
                                    ReentrantReadWriteLock lock = getLockForFile(targetDept, fileToView);
                                    boolean readLockAcquired = lock.readLock().tryLock();
                                    if (!readLockAcquired) {
                                        clientOut.println("⚠️ الملف '" + fileToView + "' مقفول حاليًا من قبل مستخدم آخر. يرجى المحاولة لاحقاً.");
                                        break;
                                    }

                                    try {
                                        String Content = service.getFileContent(targetDept, fileToView);
                                        clientOut.println(Content);
                                        clientOut.println("---END---");
                                        found = true;
                                    } finally {
                                        lock.readLock().unlock();
                                    }

                                    break;
                                }
                            } catch (Exception e) {
                                clientOut.println("⚠️ خطأ عند الوصول للملف.");
                            }
                        }

                        if (!found) {
                            clientOut.println("⚠️ الملف غير موجود.");
                        }
                        break;



                    default:
                        clientOut.println("خيار غير صالح.");
                        break;
                }
            }

        } catch (IOException e) {
            System.out.println("خطأ أثناء التعامل مع العميل: " + e.getMessage());
        }
    }

    private static void synchronizeFilesAcrossNodes() {
        Map<String, Map<Integer, String>> fileDepartmentMap = new HashMap<>();
        Map<String, Map<Integer, Long>> fileTimestamps = new HashMap<>();

        for (int i = 0; i < nodeServices.length; i++) {
            try {
                Map<String, Long> filesMap = nodeServices[i].listFilesAllDepartments();

                for (Map.Entry<String, Long> entry : filesMap.entrySet()) {
                    String key = entry.getKey();
                    String[] parts = key.split("::");
                    if (parts.length != 2) continue;

                    String fileName = parts[0];
                    String department = parts[1];

                    fileDepartmentMap.putIfAbsent(fileName, new HashMap<Integer, String>());
                    fileTimestamps.putIfAbsent(fileName, new HashMap<Integer, Long>());

                    fileDepartmentMap.get(fileName).put(i, department);
                    fileTimestamps.get(fileName).put(i, entry.getValue());
                }
            } catch (Exception e) {
                System.out.println("⚠️ خطأ عند الاتصال بالعقدة " + i + ": " + e.getMessage());
            }
        }

        for (String fileName : fileTimestamps.keySet()) {
            Map<Integer, Long> nodeData = fileTimestamps.get(fileName);
            Map<Integer, String> deptData = fileDepartmentMap.get(fileName);

            int newestNode = -1;
            long newestTime = -1;
            String department = null;

            for (Map.Entry<Integer, Long> entry : nodeData.entrySet()) {
                int nodeIndex = entry.getKey();
                long time = entry.getValue();
                if (time > newestTime) {
                    newestTime = time;
                    newestNode = nodeIndex;
                    department = deptData.get(nodeIndex);
                }
            }

            if (newestNode == -1 || department == null) continue;

            try {
                String content = nodeServices[newestNode].getFileContentByName(fileName);

                for (int i = 0; i < nodeServices.length; i++) {
                    if (i == newestNode) continue;

                    Long existingTime = nodeData.get(i);
                    if (existingTime == null) {
                        // العقدة لا تحتوي على الملف، انسخه إليها
                        nodeServices[i].writeFileToNodeInDepartment(fileName, content, department);
                    } else if (existingTime < newestTime) {
                        // حذف الملف الأقدم واستبداله بالأحدث
                        nodeServices[i].deleteFile(fileName);
                        nodeServices[i].writeFileToNodeInDepartment(fileName, content, department);
                    }
                }
            } catch (Exception e) {
                System.out.println(" فشل مزامنة الملف " + fileName + ": " + e.getMessage());
            }
        }

        System.out.println(" المزامنة انتهت بنجاح.");
    }
}
