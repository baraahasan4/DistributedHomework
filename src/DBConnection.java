import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/distributed_system";
    private static final String USER = "root"; // غيّر هذا إذا كانت مختلفة
    private static final String PASS = "";     // غيّر هذا إذا كان هناك كلمة مرور

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
