package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Classe utilitaire pour gérer la connexion à la base de données MySQL
 * 
 * IMPORTANT: Cette classe est prête pour la connexion BD réelle.
 * Pour le moment, le projet utilise des données statiques (ArrayList).
 * 
 * Pour activer la connexion BD:
 * 1. Exécutez le fichier database.sql pour créer les tables
 * 2. Configurez les paramètres ci-dessous
 * 3. Modifiez les Services pour utiliser cette connexion au lieu des ArrayList
 */
public class DatabaseConnection {
    
    // ========== CONFIGURATION ==========

    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/re7la_db?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    
    // Driver JDBC
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    
    // Singleton
    private static Connection connection = null;
    
    /**
     * Obtenir une connexion à la base de données
     * @return Connection object
     */
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                // Charger le driver JDBC
                Class.forName(JDBC_DRIVER);
                
                // Établir la connexion
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                
                System.out.println("✅ Connexion à la base de données établie avec succès!");
            }
            return connection;
            
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Driver JDBC non trouvé!");
            System.err.println("Assurez-vous que mysql-connector-j est dans le classpath");
            e.printStackTrace();
            return null;
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur de connexion à la base de données!");
            System.err.println("Vérifiez: URL, USER, PASSWORD");
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Fermer la connexion
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✅ Connexion fermée");
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la fermeture de la connexion");
            e.printStackTrace();
        }
    }
    
    /**
     * Tester la connexion
     */
    public static void testConnection() {
        Connection conn = getConnection();
        if (conn != null) {
            System.out.println("✅ Test de connexion réussi!");
            try {
                System.out.println("📊 Base de données: " + conn.getCatalog());
                System.out.println("🔗 URL: " + conn.getMetaData().getURL());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("❌ Test de connexion échoué!");
        }
    }
    
    /**
     * Main pour tester la connexion
     */
    public static void main(String[] args) {
        System.out.println("=== Test de Connexion à la Base de Données ===");
        testConnection();
        closeConnection();
    }
}
