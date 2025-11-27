package org.example;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 学生查询子功能：查询全部学生
 */
public class StudentQueryService {
    // 依赖主控制器（通过构造函数注入，解耦）
    private final StudentCountController dbController;

    // 构造函数注入主控制器
    public StudentQueryService(StudentCountController dbController) {
        this.dbController = dbController;
    }

    // UI方法
    public Object[][] queryAllStudentForUI() throws Exception {
        Connection conn = null;
        Statement stm = null;
        ResultSet rs = null;
        List<Object[]> studentList = new ArrayList<>();

        return studentList.toArray(new Object[0][0]);
    }

    /**
     * 子功能：查询所有学生信息
     */
    public void queryAllStudents() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            // 从主控制器获取连接
            conn = dbController.getConnection();
            System.out.println("Connected to database for querying students.");

            // 执行查询
            stmt = conn.createStatement();
            String sql = "SELECT id, name, age, grade FROM students"; // 假设表名是students
            rs = stmt.executeQuery(sql);

            // 处理结果集
            System.out.println("All students:");
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int age = rs.getInt("age");
                String grade = rs.getString("grade");
                System.out.printf("ID: %d, Name: %s, Age: %d, Grade: %s%n", id, name, age, grade);
            }

        } catch (Exception e) {
            System.err.println("Failed to query all students!");
            e.printStackTrace();
        } finally {
            // 调用主控制器的方法统一释放资源
            dbController.closeResources(conn, stmt, rs);
        }
    }

    /**
     * 子功能：按年级查询学生（扩展子功能）
     */
    public void queryStudentsByGrade(String grade) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbController.getConnection();
            stmt = conn.createStatement();
            String sql = String.format("SELECT id, name, age FROM students WHERE grade = '%s'", grade);
            rs = stmt.executeQuery(sql);

            System.out.printf("Students in Grade %s:%n", grade);
            while (rs.next()) {
                System.out.printf("ID: %d, Name: %s, Age: %d%n",
                        rs.getInt("id"), rs.getString("name"), rs.getInt("age"));
            }

        } catch (Exception e) {
            System.err.println("Failed to query students by grade!");
            e.printStackTrace();
        } finally {
            dbController.closeResources(conn, stmt, rs);
        }
    }
}