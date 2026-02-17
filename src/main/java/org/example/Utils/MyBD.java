package org.example.Utils;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MyBD {
    private static final Logger LOGGER = Logger.getLogger(MyBD.class.getName());

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/re7la_3a9";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private final String url;
    private final String user;
    private final String password;
    private Connection connection;

    private MyBD() {
        this.url = System.getProperty("db.url", System.getenv().getOrDefault("DB_URL", DEFAULT_URL));
        this.user = System.getProperty("db.user", System.getenv().getOrDefault("DB_USER", DEFAULT_USER));
        this.password = System.getProperty("db.pass", System.getenv().getOrDefault("DB_PASS", DEFAULT_PASSWORD));
        this.connection = createConnection();
    }

    public static MyBD getInstance() {
        return Holder.INSTANCE;
    }

    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                LOGGER.warning("Database connection was closed/invalid. Reconnecting.");
                connection = createConnection();
            }
            return connection;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to validate database connection.", e);
        }
    }

    private Connection createConnection() {
        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            LOGGER.info("Connected to database successfully.");
            return conn;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to connect to database at " + url, e);
            throw new IllegalStateException("Database connection failed.", e);
        }
    }

    private static class Holder {
        private static final MyBD INSTANCE = new MyBD();
    }
}
