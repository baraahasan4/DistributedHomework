import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Admin {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 5000);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);

            // تسجيل الدخول
            System.out.println(in.readLine()); // أدخل اسم المستخدم:
            String username = scanner.nextLine();
            out.println(username);

            System.out.println(in.readLine()); // أدخل كلمة المرور:
            String password = scanner.nextLine();
            out.println(password);

            // رسالة ترحيب
            System.out.println(in.readLine());

            // حلقة مهام الأدمن
            while (true) {
                // القائمة
                for (int i = 0; i < 4; i++) {
                    System.out.println(in.readLine());
                }

                String option = scanner.nextLine();
                out.println(option);

                if (option.equals("1")) {
                    // إضافة موظف
                    System.out.println(in.readLine()); // أدخل اسم المستخدم الجديد:
                    String newUsername = scanner.nextLine();
                    out.println(newUsername);

                    System.out.println(in.readLine()); // أدخل كلمة المرور:
                    String newPassword = scanner.nextLine();
                    out.println(newPassword);

                    System.out.println(in.readLine()); // أدخل القسم:
                    String department = scanner.nextLine();
                    out.println(department);

                    // نتيجة الإضافة
                    System.out.println(in.readLine());

                } else if (option.equals("2")) {
                    // تسجيل الخروج
                    System.out.println(in.readLine());
                    break;
                } else {
                    // خيار غير معروف
                    System.out.println(in.readLine());
                }
            }

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
