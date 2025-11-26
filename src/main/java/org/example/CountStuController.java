package org.example;

import java.sql.*;

public class CountStuController {
    private static final String DB_TYPE = "POSTGRES";
    private static final String URL = "jdbc:postgresql://localhost:5432/test_db";
    private static final String USERNAME = "hods";
    private static final String PASSWORD = "123";
    private static final String DRIVER = "org.postgresql.Driver";

    static Connection conn = null;

    // 加载驱动
    static {
        try {
            Class.forName(DRIVER);
            System.out.println(DB_TYPE + " load success!");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(DB_TYPE + " load error!");
        }
    }

    void conndb() {
        try {
            conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("PostgreSQL connect success!");

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null)
                    conn.close();
                System.out.println("connection closed!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // 查询所有用户

}
