package org.example;

import java.sql.Connection;
import java.sql.ResultSet;
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

    // UI方法

}