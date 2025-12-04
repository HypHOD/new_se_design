package org.example;

import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 随机统计学生子功能：负责所有随机相关的学生查询/统计业务
 */
public class StudentRandomQuery {
    // 依赖主控制器
    private final StudentQueryController dbController;
    // 随机数生成器（用于随机选年级、年龄段等）
    private final Random random;

    public static Integer lateCount = 0;
    public static Integer absentCount = 0;

    // 构造函数：注入主控制器（必须传入，保证连接复用）
    public StudentRandomQuery(StudentQueryController dbController) {
        this.dbController = dbController;
        this.random = new Random();
    }

    // 随机抽点大于n次迟到 & 大于n次缺课的学生
    public List<Student> randomQueryStudent(int lateCount, int absentCount, int limit) {
        if (lateCount <= 0 && absentCount <= 0 || limit <= 0) {
            System.out.println("迟到/缺课阈值至少一个大于0，且查询数量必须大于0！");
            return new ArrayList<>();// 避免NPE
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Student> allStudents = new ArrayList<>();

        try {
            // 获取数据库连接
            conn = dbController.getConnection();
            System.out.printf("开始查询迟到次数大于%d , 缺课次数大于%d的 %d 名学生%n", lateCount, absentCount, limit);

            // PostgreSQL查询指定学生
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT student_id, name, avatar_url, absence, late ")
                    .append("FROM students ")
                    .append("WHERE 1=1 ");

            if (lateCount > 0) {
                sqlBuilder.append("AND late>? ");
            }
            if (absentCount > 0) {
                sqlBuilder.append("AND absence>? ");
            }
            sqlBuilder.append("ORDER BY RANDOM() LIMIT ?");

            pstmt = conn.prepareStatement(sqlBuilder.toString());
            System.out.println("执行SQL：" + sqlBuilder.toString());

            // 设置查询参数
            int paramIndex = 1;
            if (lateCount > 0) {
                pstmt.setInt(paramIndex++, lateCount); // 只有lateCount>0时才绑定
            }
            if (absentCount > 0) {
                pstmt.setInt(paramIndex++, absentCount); // 只有absentCount>0时才绑定
            }
            pstmt.setInt(paramIndex++, limit); // 最后绑定limit
            rs = pstmt.executeQuery();

            // 将查询到的结果封装
            while (rs.next()) {
                Student student = new Student();
                student.setStudent_id(rs.getInt("student_id"));
                student.setName(rs.getString("name"));
                student.setAvatar_url(rs.getString("avatar_url") != null ? rs.getString("avatar_url") : "");
                student.setLate(rs.getInt("late"));
                student.setAbsence(rs.getInt("absence"));
                student.setMarked(false);
                student.setHere(false);
                allStudents.add(student);
            }

            printAllStudents(allStudents);
            System.out.println("搜索完成, 准备抽点");

            // 将查询到的学生存储在数组传递给ui

            // 接收ui对每个学生迟到/缺勤的标记, 修改数据库
        } catch (SQLException e) {
            System.err.println("获取失败!");
            e.printStackTrace();
        } finally {
            dbController.closeResources(conn, pstmt, rs);
        }
        return allStudents;
    }

    // 辅助函数, 打印全部学生
    public void printAllStudents(List<Student> allStudents) {
        if (allStudents.isEmpty()) {
            System.err.println("查询结果为空");
            return;
        }
        System.out.println("=" + "=".repeat(100));
        System.out.printf("%-8s %-12s %-20s %-8s %-8s %-6s %n", "学号", "姓名", "头像", "迟到", "缺勤", "状态");
        System.out.println("-" + "-".repeat(100));

        for (int i = 0; i < allStudents.size(); i++) {
            Student student = allStudents.get(i);
            System.out.printf("%-8s %-12s %-20s %-8s %-8s %-6s %n",
                    student.getStudent_id(),
                    student.getName(),
                    student.getAvatar_url(),
                    student.getLate(),
                    student.getAbsence(),
                    student.isHere() ? "在场" : "缺席");
            System.out.println("=" + "=".repeat(100));
        }
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

    public static class Student {

        private int student_id;
        private String name;
        private String avatar_url;
        private int late;
        private int absence;
        private boolean isMarked = false;
        private boolean isHere = false;

        public Student() {
        };

        public Student(int student_id, String name, String avatar_url, int late, int absence) {
            this.student_id = student_id;
            this.name = name;
            this.avatar_url = avatar_url;
            this.late = late;
            this.absence = absence;
        }

        public Object[] getStuInfo() {
            return new Object[] {
                    student_id,
                    name,
                    avatar_url,
                    late,
                    absence
            };
        }

        public int getStudent_id() {
            return student_id;
        }

        public void setStudent_id(int student_id) {
            this.student_id = student_id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAvatar_url() {
            return avatar_url;
        }

        public void setAvatar_url(String avatar_url) {
            this.avatar_url = avatar_url;
        }

        public int getLate() {
            return late;
        }

        public void setLate(int late) {
            this.late = late;
        }

        public int getAbsence() {
            return absence;
        }

        public void setAbsence(int absence) {
            this.absence = absence;
        }

        public boolean isMarked() {
            return isMarked;
        }

        public void setMarked(boolean marked) {
            isMarked = marked;
        }

        public boolean isHere() {
            return isHere;
        }

        public void setHere(boolean here) {
            isHere = here;
        }

    }
}