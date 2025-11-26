package org.example;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 学生统计子功能：负责所有统计相关的业务
 */
public class StudentCountService {
    private final CountStuController dbController;

    // 构造函数注入主控制器
    public StudentCountService(CountStuController dbController) {
        this.dbController = dbController;
    }

    /**
     * 子功能：统计学生总数
     */
    public int countTotalStudents() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        int total = 0;

        try {
            conn = dbController.getConnection();
            stmt = conn.createStatement();
            String sql = "SELECT COUNT(*) AS total FROM students";
            rs = stmt.executeQuery(sql);

            if (rs.next()) {
                total = rs.getInt("total");
            }
            System.out.printf("Total number of students: %d%n", total);

        } catch (Exception e) {
            System.err.println("Failed to count total students!");
            e.printStackTrace();
        } finally {
            dbController.closeResources(conn, stmt, rs);
        }
        return total;
    }

    /**
     * 子功能：按年龄统计学生数量（扩展子功能）
     */
    public int countStudentsByAge(int minAge, int maxAge) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        int count = 0;

        try {
            conn = dbController.getConnection();
            stmt = conn.createStatement();
            String sql = String.format(
                    "SELECT COUNT(*) AS count FROM students WHERE age BETWEEN %d AND %d",
                    minAge, maxAge);
            rs = stmt.executeQuery(sql);

            if (rs.next()) {
                count = rs.getInt("count");
            }
            System.out.printf("Number of students aged %d-%d: %d%n", minAge, maxAge, count);

        } catch (Exception e) {
            System.err.println("Failed to count students by age!");
            e.printStackTrace();
        } finally {
            dbController.closeResources(conn, stmt, rs);
        }
        return count;
    }
}