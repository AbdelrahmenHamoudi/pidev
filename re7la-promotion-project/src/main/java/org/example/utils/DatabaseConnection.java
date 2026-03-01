package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * ✅ FIXED: DB URL changed from re7la_db → re7la_3a9 (shared team database)
 */
public class DatabaseConnection {

    // ========== CONFIGURATION ==========
    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/re7la_3a9?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER     = "root";
    private static final String DB_PASSWORD = "";
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";

    private static Connection connection = null;

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName(JDBC_DRIVER);
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                System.out.println("✅ Connexion à re7la_3a9 établie !");
            }
            return connection;
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Driver JDBC non trouvé !");
            e.printStackTrace();
            return null;
        } catch (SQLException e) {
            System.err.println("❌ Erreur de connexion à re7la_3a9 !");
            e.printStackTrace();
            return null;
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✅ Connexion fermée");
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la fermeture");
            e.printStackTrace();
        }
    }

    public static void testConnection() {
        Connection conn = getConnection();
        if (conn != null) {
            System.out.println("✅ Test connexion re7la_3a9 réussi !");
            try {
                System.out.println("📊 Base : " + conn.getCatalog());
            } catch (SQLException e) { e.printStackTrace(); }
        } else {
            System.out.println("❌ Test connexion échoué !");
        }
    }

    public static void main(String[] args) {
        testConnection();
        closeConnection();
    }
}