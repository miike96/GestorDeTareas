package org.miguel.gestordetareas.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {
    private static final String URL = "jdbc:sqlite:tasks.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC"); // Fuerza la carga del driver SQLite
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
