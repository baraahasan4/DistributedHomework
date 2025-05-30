import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        try (
                Socket socket = new Socket("localhost", 5000);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                Scanner scanner = new Scanner(System.in)
        ) {
            String serverMsg;

            // تسجيل الدخول
            while ((serverMsg = in.readLine()) != null) {
                System.out.println(serverMsg);
                if (serverMsg.contains("أدخل")) {
                    out.println(scanner.nextLine());
                } else if (serverMsg.contains("تم تسجيل الدخول بنجاح")) {
                    break;
                } else if (serverMsg.contains("بيانات تسجيل الدخول غير صحيحة")) {
                    return;
                }
            }

            while (true) {
                // عرض قائمة المهام من السيرفر
                while ((serverMsg = in.readLine()) != null) {
                    if (serverMsg.trim().isEmpty()) continue;
                    System.out.println(serverMsg);
                    if (serverMsg.contains("اختر خياراً")) {
                        break;
                    }
                }

                String choice = scanner.nextLine();
                out.println(choice);

                switch (choice) {
                    case "1": // رفع ملف
                        System.out.println(in.readLine());
                        String fileName = scanner.nextLine();
                        out.println(fileName);

                        System.out.println(in.readLine());
                        String line;
                        while (!(line = scanner.nextLine()).equalsIgnoreCase("finish")) {
                            out.println(line);
                        }
                        out.println("finish");

                        System.out.println(in.readLine());
                        break;

                    case "2": // تعديل ملف عبر السوكيت
                        // استقبال قائمة الملفات حتى رسالة "اختر رقم الملف للتعديل:"
                        while (!(serverMsg = in.readLine()).startsWith("اختر رقم الملف")) {
                            System.out.println(serverMsg);
                        }
                        System.out.println(serverMsg);

                        String fileChoice = scanner.nextLine();
                        out.println(fileChoice);

                        // استقبال محتوى الملف
                        while (!(serverMsg = in.readLine()).startsWith("📝")) {
                            System.out.println(serverMsg);
                        }
                        System.out.println(serverMsg);

                        // إرسال المحتوى الجديد سطر بسطر
                        while (true) {
                            String newLine = scanner.nextLine();
                            out.println(newLine);
                            if (newLine.equalsIgnoreCase("finish")) break;
                        }

                        // استقبال رسالة تأكيد أو خطأ
                        System.out.println(in.readLine());
                        break;

                    case "3": // حذف ملف
                        // استقبال قائمة الملفات للحذف
                        List<String> delFiles = new ArrayList<>();
                        String ServerMsg;
                        while (!(ServerMsg = in.readLine()).equals("أدخل رقم الملف للحذف:")) {
                            if (!ServerMsg.isEmpty())
                                delFiles.add(ServerMsg);
                        }
                        if (delFiles.isEmpty()) {
                            System.out.println("لا توجد ملفات للحذف.");
                            break;
                        }
                        delFiles.forEach(System.out::println);

                        // الآن يطلب من المستخدم إدخال رقم الملف للحذف
                        System.out.print("أدخل رقم الملف للحذف: ");
                        String delChoice = scanner.nextLine();

                        // نرسل رقم الملف للخادم
                        out.println(delChoice);

                        // ننتظر نتيجة الحذف من الخادم (رسالة النجاح أو الفشل)
                        System.out.println(in.readLine());
                        break;


                    case "4": // استعراض ملف من قسم آخر
                        // استقبال قائمة الأقسام وطباعة حتى "اختر القسم:"
                        while (!(serverMsg = in.readLine()).equals("اختر القسم:")) {
                            System.out.println(serverMsg);  // هنا تطبع أسماء الأقسام أولاً
                        }

                        // بعد الانتهاء من طباعة القائمة، اطبع رسالة "اختر القسم:"
                        System.out.println("اختر القسم:");

                        // إدخال رقم القسم
                        String deptChoice = scanner.nextLine();
                        out.println(deptChoice);

                        // استقبال قائمة الملفات حتى "اختر الملف لعرضه:"
                        List<String> viewFiles = new ArrayList<>();
                        while (!(serverMsg = in.readLine()).equals("اختر الملف لعرضه:")) {
                            if (!serverMsg.trim().isEmpty()) {
                                viewFiles.add(serverMsg);
                            }
                        }

                        // طباعة قائمة الملفات أولاً
                        for (String f : viewFiles) {
                            System.out.println(f);
                        }

                        // بعدها اطبع "أدخل رقم الملف:"
                        System.out.print("أدخل رقم الملف: ");

                        // استلام اختيار الملف
                        String viewChoice = scanner.nextLine();
                        out.println(viewChoice);

                        // استقبال محتوى الملف حتى علامة النهاية ---END---
                        while (!(serverMsg = in.readLine()).equals("---END---")) {
                            System.out.println(serverMsg);
                        }
                        break;



                    case "5": // إنهاء الجلسة
                        System.out.println(in.readLine());
                        return;

                    default:
                        System.out.println("خيار غير صالح.");
                        break;
                }
            }

        } catch (IOException e) {
            System.out.println("فشل الاتصال بالسيرفر: " + e.getMessage());
        }
    }
}
