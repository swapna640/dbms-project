package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL  = "jdbc:mysql://localhost:3306/pizza_db";
    private static final String USER = "root";
    private static final String PASS = "root";

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}