package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.example.StudentRandomQuery.Student;

/**
 * 学生查询子功能：对本次签到结果进行统计
 */
public class StudentQueryAnalyze {
    // 依赖主控制器（通过构造函数注入，解耦）
    private final StudentQueryController dbController;

    // 构造函数注入主控制器
    public StudentQueryAnalyze(StudentQueryController dbController) {
        this.dbController = dbController;
    }

    public List<Student> analizeStudents(List<Student> queriedStudents) {
        List<Student> analizeStudentsList = new ArrayList<>();
        if (queriedStudents.isEmpty()) {
            System.out.println("传入数组为空");
            return analizeStudentsList;
        }

        // 返回接受数组中迟到/缺席的人 并按总缺席次数排序
        for (Student student : queriedStudents) {
            if (student.isMarked() && !student.isHere()) {
                analizeStudentsList.add(student);
            }
        }

        // 排序输出
        analizeStudentsList.sort(Comparator.comparingInt(Student::getAbsence).reversed());
        return analizeStudentsList;
    }

    // 查询全部学生
    public List<Student> getAllStudentsWithStats() {
        List<Student> studentList = new ArrayList<>();
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        // 从数据库读取全部学生

        try {
            // 1. 获取数据库连接（通过注入的dbController）
            connection = dbController.getConnection();
            if (connection == null) {
                System.err.println("获取数据库连接失败");
                return studentList;
            }

            // 2. 编写SQL：查询学生表所有字段（假设表名：student，字段名与Student类属性对应）
            String sql = "SELECT student_id, name, late, absence, avatar_url FROM students";
            preparedStatement = connection.prepareStatement(sql);

            // 3. 执行查询
            resultSet = preparedStatement.executeQuery();
            // 4. 解析结果集，封装为Student对象
            while (resultSet.next()) {
                Student student = new Student();
                student.setStudent_id(resultSet.getInt("student_id"));
                student.setName(resultSet.getString("name"));
                student.setLate(resultSet.getInt("late"));
                student.setAbsence(resultSet.getInt("absence"));
                student.setAvatar_url(resultSet.getString("avatar_url"));

                // 补充必要的默认值（避免空指针）
                student.setMarked(false); // 未标记（默认状态）
                student.setHere(false); // 未到场（默认状态）

                studentList.add(student);
            }

            System.out.println("查询到 " + studentList.size() + " 名学生的完整统计数据");
        } catch (SQLException e) {
            System.err.println("查询全部学生统计数据失败：" + e.getMessage());
            e.printStackTrace();
        } finally {
            // 5. 关闭资源：PreparedStatement强转为Statement，匹配StudentQueryController的方法签名
            dbController.closeResources(connection, (Statement) preparedStatement, resultSet);
        }

        return studentList;
    }

    // 更新学生缺课&迟到数据
    public boolean updateStudentStats(List<Student> students) {
        if (students.isEmpty()) {
            System.out.println("没有需要更新的学生数据");
            return true;
        }
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            // 1. 获取数据库连接
            connection = dbController.getConnection();
            if (connection == null) {
                System.err.println("获取数据库连接失败");
                return false;
            }

            // 2. 开启事务（批量更新，保证原子性）
            connection.setAutoCommit(false);

            // 3. 编写更新SQL（根据student_id更新late_count和absent_count）
            String sql = "UPDATE students SET late = ?, absence = ? WHERE student_id = ?";
            preparedStatement = connection.prepareStatement(sql);

            // 4. 批量添加参数
            for (Student student : students) {
                preparedStatement.setInt(1, student.getLate()); // 迟到次数
                preparedStatement.setInt(2, student.getAbsence()); // 缺课次数
                preparedStatement.setInt(3, student.getStudent_id()); // 学生ID（主键）
                preparedStatement.addBatch(); // 添加到批处理
            }
            // 5. 执行批处理
            int[] updateCounts = preparedStatement.executeBatch();
            connection.commit(); // 提交事务

            // 6. 验证更新结果（所有更新都影响至少1行才视为成功）
            for (int count : updateCounts) {
                if (count < 1) {
                    System.err.println("部分学生更新失败（未找到对应ID）");
                    return false;
                }
            }

            System.out.println("成功更新 " + updateCounts.length + " 名学生的统计数据");
            return true;

        } catch (SQLException e) {
            // 事务回滚
            if (connection != null) {
                try {
                    connection.rollback();
                    System.err.println("更新失败，已回滚事务");
                } catch (SQLException ex) {
                    System.err.println("事务回滚失败：" + ex.getMessage());
                }
            }
            System.err.println("批量更新学生统计数据失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            // 7. 恢复自动提交并关闭资源
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException e) {
                    System.err.println("恢复自动提交失败：" + e.getMessage());
                }
            }
            dbController.closeResources(connection, preparedStatement, null);
        }
    }

}