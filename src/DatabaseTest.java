import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DatabaseTest {
    private static final Logger logger = Logger.getLogger(DatabaseTest.class.getName());

    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/distributed_system";
        String username = "root";
        String password = "";

        try {
            Connection connection = DriverManager.getConnection(url, username, password);
            logger.info("تم الاتصال بقاعدة البيانات بنجاح!");

            connection.close();
        } catch (SQLException e) {
            logger.severe("خطأ في الاتصال بقاعدة البيانات: " + e.getMessage());
        }
    }
}
