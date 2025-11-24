package org.example;

import java.sql.*;

public class CountStu {
    // private static final String DB_TYPE = "POSTGRES";
    private static final String URL = "jdbc:postgresql://localhost:5432/test_db";
    private static final String USERNAME = "hods";
    private static final String PASSWORD = "123";
    private static final String DRIVER = "org.postgresql.Driver";

    static Connection conn = null;

    void conndb() {
        try {
            Class.forName(DRIVER);
            conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("PostgreSQL connect success!");

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // 查询所有用户

}
