package org.example;

import java.sql.*;

public class StudentCountController {
    private static final String DB_TYPE = "POSTGRES";
    private static final String URL = "jdbc:postgresql://localhost:5432/student_management?currentSchema=public";
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

    // 获取数据库数据库连接
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    // 关闭数据库连接
    public void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                    System.out.println("DB connection closed success!");
                }
            } catch (SQLException e) {
                System.err.println("Failed to close connection!");
                e.printStackTrace();
            }
        }
    }

    // 关闭资源
    public void closeResources(Connection conn, java.sql.Statement stmt, java.sql.ResultSet rs) {
        // 先关ResultSet，再关Statement，最后关Connection
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        closeConnection(conn);
    }

    // void conndb() {
    // try {
    // conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
    // System.out.println("PostgreSQL connect success!");

    // } catch (SQLException e) {
    // e.printStackTrace();
    // } finally {
    // try {
    // if (conn != null)
    // conn.close();
    // System.out.println("connection closed!");
    // } catch (SQLException e) {
    // e.printStackTrace();
    // }
    // }
    // }

}
