import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class UserManager {

    public static boolean addEmployeeToDatabase(String username, String password, String role, String department) {
        String token = UUID.randomUUID().toString();
        String sql = "INSERT INTO employees (username, password, role, department, token) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, role);
            stmt.setString(4, department);
            stmt.setString(5, token);

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.out.println("خطأ عند إضافة الموظف: " + e.getMessage());
            return false;
        }
    }

    public static String validateUser(String username, String password) {
        String sql = "SELECT role FROM employees WHERE username = ? AND password = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("role");
            }

        } catch (SQLException e) {
            System.out.println("خطأ في التحقق من المستخدم: " + e.getMessage());
        }
        return null;
    }

    public static void updateToken(String username, String token) {
        String sql = "UPDATE employees SET token = ? WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, token);
            stmt.setString(2, username);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("خطأ في تحديث التوكن: " + e.getMessage());
        }
    }

    public static String getDepartmentByUsername(String username) {
        String sql = "SELECT department FROM employees WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("department");
            }
        } catch (SQLException e) {
            System.out.println("خطأ في جلب القسم: " + e.getMessage());
        }
        return null;
    }

}
