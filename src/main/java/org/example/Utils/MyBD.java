package org.example.Utils;
import java.sql.*;
public class MyBD {
    private Connection conn;
    final private String url = "jdbc:mysql://localhost:3306/re7la_3a9";
    final private String user = "root";
    final private String pass = "";
    private static MyBD instance;
    private MyBD() {

        try {
            conn = DriverManager.getConnection(url, user, pass);
            System.out.println("Connected to database successfully");
        }catch(SQLException s){
            System.out.println(s.getMessage());
        }
    }
    public   static MyBD getInstance(){
        if(instance == null){
            instance = new MyBD();
        }
        return instance;
    }
    public Connection getConnection(){
        return conn;
    }
}
