package org.example;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.Random;

/**
 * 随机统计学生子功能：负责所有随机相关的学生查询/统计业务
 */
public class RandomCountStudent {
    // 依赖主控制器（连接管理），通过构造函数注入
    private final CountStuController dbController;
    // 随机数生成器（用于随机选年级、年龄段等）
    private final Random random;

    // 预设可选年级（可根据实际数据库调整）
    private static final String[] GRADES = { "Grade 9", "Grade 10", "Grade 11", "Grade 12" };
    // 预设可选年龄段（minAge, maxAge）
    private static final int[][] AGE_RANGES = { { 15, 17 }, { 18, 20 }, { 21, 23 } };

    /**
     * 构造函数：注入主控制器（必须传入，保证连接复用）
     */
    public RandomCountStudent(CountStuController dbController) {
        this.dbController = dbController;
        this.random = new Random();
    }

    /**
     * 子功能1：随机查询 N 名学生（无重复，基于数据库随机抽样）
     * 
     * @param limit 要查询的学生数量
     */
    public void randomQueryStudents(int limit) {
        if (limit <= 0) {
            System.out.println("查询数量必须大于0！");
            return;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            // 从主控制器获取连接
            conn = dbController.getConnection();
            System.out.printf("开始随机查询 %d 名学生...%n", limit);

            // PostgreSQL 随机抽样语法：ORDER BY RANDOM() LIMIT ?
            String sql = "SELECT id, name, age, grade FROM students ORDER BY RANDOM() LIMIT ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, limit); // 传入查询数量
            rs = pstmt.executeQuery();

            // 处理结果
            int count = 0;
            System.out.println("随机查询到的学生：");
            while (rs.next()) {
                count++;
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int age = rs.getInt("age");
                String grade = rs.getString("grade");
                System.out.printf("%d. ID: %d, 姓名: %s, 年龄: %d, 年级: %s%n",
                        count, id, name, age, grade);
            }

            if (count == 0) {
                System.out.println("未查询到任何学生！");
            }

        } catch (Exception e) {
            System.err.println("随机查询学生失败！");
            e.printStackTrace();
        } finally {
            // 统一释放资源（主控制器提供的方法）
            dbController.closeResources(conn, pstmt, rs);
        }
    }

    /**
     * 子功能2：随机选择一个年级，统计该年级的学生数
     * 
     * @return 随机年级的学生数量
     */
    public int randomCountByGrade() {
        // 随机选择一个年级（从预设GRADES数组中）
        String randomGrade = GRADES[random.nextInt(GRADES.length)];
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int studentCount = 0;

        try {
            conn = dbController.getConnection();
            System.out.printf("随机统计年级【%s】的学生数...%n", randomGrade);

            // 统计指定年级的学生数（使用PreparedStatement防止SQL注入）
            String sql = "SELECT COUNT(*) AS count FROM students WHERE grade = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, randomGrade);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                studentCount = rs.getInt("count");
            }

            System.out.printf("年级【%s】的学生总数：%d%n", randomGrade, studentCount);

        } catch (Exception e) {
            System.err.println("随机统计年级学生数失败！");
            e.printStackTrace();
        } finally {
            dbController.closeResources(conn, pstmt, rs);
        }

        return studentCount;
    }

    /**
     * 子功能3：随机选择一个年龄段，查询该年龄段的学生
     */
    public void randomQueryByAgeRange() {
        // 随机选择一个年龄段（从预设AGE_RANGES数组中）
        int[] randomAgeRange = AGE_RANGES[random.nextInt(AGE_RANGES.length)];
        int minAge = randomAgeRange[0];
        int maxAge = randomAgeRange[1];

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = dbController.getConnection();
            System.out.printf("随机查询年龄段【%d-%d岁】的学生...%n", minAge, maxAge);

            // 查询指定年龄段的学生
            String sql = "SELECT id, name, age, grade FROM students WHERE age BETWEEN ? AND ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, minAge);
            pstmt.setInt(2, maxAge);
            rs = pstmt.executeQuery();

            // 处理结果
            int count = 0;
            System.out.printf("【%d-%d岁】的学生列表：%n", minAge, maxAge);
            while (rs.next()) {
                count++;
                System.out.printf("%d. 姓名: %s, 年龄: %d, 年级: %s%n",
                        count, rs.getString("name"), rs.getInt("age"), rs.getString("grade"));
            }

            if (count == 0) {
                System.out.printf("【%d-%d岁】暂无学生！%n", minAge, maxAge);
            }

        } catch (Exception e) {
            System.err.println("随机查询年龄段学生失败！");
            e.printStackTrace();
        } finally {
            dbController.closeResources(conn, pstmt, rs);
        }
    }
}