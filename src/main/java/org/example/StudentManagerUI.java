package org.example;

import org.example.StudentCountController;
import org.example.StudentRandomCount;
import org.example.StudentQueryService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 学生管理系统Swing主界面
 * 只负责UI展示和用户交互，通过调用业务服务实现功能
 */
public class StudentManagerUI extends JFrame {
    // 1. 业务服务依赖（初始化后注入）
    private StudentQueryService queryService;
    private StudentRandomCount randomCountService;

    // 2. UI组件
    private JTable studentTable; // 学生数据展示表格
    private DefaultTableModel tableModel; // 表格数据模型
    private JLabel statusLabel; // 状态提示标签

    /**
     * 构造函数：初始化UI和业务服务
     */
    public StudentManagerUI() {
        // 初始化业务服务
        StudentCountController dbController = new StudentCountController();
        this.queryService = new StudentQueryService(dbController);
        this.randomCountService = new StudentRandomCount(dbController);

        // 初始化UI
        initUI();
    }

    private void initUI() {
        // 窗口配置
        setTitle("学生信息管理系统");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 居中显示
        setLayout(new BorderLayout(10, 10)); // 边界布局，间距10px

        // ---------------------- 顶部按钮区域 ----------------------
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 10)); // 流式布局，按钮间距15px

        // 创建功能按钮
        JButton btnQueryAll = new JButton("查询所有学生");
        JButton btnRandomQuery = new JButton("随机查询3名学生");
        JButton btnRandomCountGrade = new JButton("随机统计年级学生数");
        JButton btnRandomAgeRange = new JButton("随机查询年龄段学生");

        // 设置按钮大小
        Dimension btnSize = new Dimension(150, 30);
        btnQueryAll.setPreferredSize(btnSize);
        btnRandomQuery.setPreferredSize(btnSize);
        btnRandomCountGrade.setPreferredSize(btnSize);
        btnRandomAgeRange.setPreferredSize(btnSize);

        // 添加按钮到面板
        buttonPanel.add(btnQueryAll);
        buttonPanel.add(btnRandomQuery);
        buttonPanel.add(btnRandomCountGrade);
        buttonPanel.add(btnRandomAgeRange);

        // ---------------------- 中间表格展示区域 ----------------------
        // 表格列名
        String[] columnNames = { "ID", "姓名", "年龄", "年级" };
        // 表格数据模型（空数据初始化）
        tableModel = new DefaultTableModel(null, columnNames);
        // 创建表格（禁止编辑）
        studentTable = new JTable(tableModel);
        studentTable.setDefaultEditor(Object.class, null); // 禁止表格单元格编辑
        studentTable.getTableHeader().setFont(new Font("宋体", Font.BOLD, 12)); // 表头字体
        studentTable.setFont(new Font("宋体", Font.PLAIN, 11)); // 表格内容字体
        studentTable.setRowHeight(25); // 行高

        // 给表格添加滚动条
        JScrollPane tableScrollPane = new JScrollPane(studentTable);

        // ---------------------- 底部状态提示区域 ----------------------
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        statusLabel = new JLabel("就绪：请点击上方功能按钮");
        statusLabel.setFont(new Font("宋体", Font.PLAIN, 11));
        statusPanel.add(statusLabel);

        // ---------------------- 组装窗口 ----------------------
        add(buttonPanel, BorderLayout.NORTH); // 顶部按钮面板
        add(tableScrollPane, BorderLayout.CENTER); // 中间表格
        add(statusPanel, BorderLayout.SOUTH); // 底部状态栏

        // ---------------------- 绑定按钮事件 ----------------------
        bindButtonEvents(btnQueryAll, btnRandomQuery, btnRandomCountGrade, btnRandomAgeRange);
    }

    /**
     * 绑定按钮点击事件
     */
    private void bindButtonEvents(JButton btnQueryAll, JButton btnRandomQuery,
            JButton btnRandomCountGrade, JButton btnRandomAgeRange) {
        // 1. 查询所有学生
        btnQueryAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 数据库操作是耗时任务，放在子线程中执行
                new Thread(() -> {
                    try {
                        // 更新状态提示（Swing组件需在EDT线程更新）
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("正在查询所有学生...");
                            clearTableData(); // 清空表格旧数据
                        });

                        // 调用业务服务查询数据（返回二维数组用于表格展示）
                        // Object[][] allStudents = queryService.queryAllStudentsForUI();

                        // 更新表格数据（必须在EDT线程中操作Swing组件）
                        SwingUtilities.invokeLater(() -> {
                            if (allStudents.length == 0) {
                                statusLabel.setText("查询完成：无学生数据");
                            } else {
                                for (Object[] student : allStudents) {
                                    tableModel.addRow(student);
                                }
                                statusLabel.setText("查询完成：共找到 " + allStudents.length + " 名学生");
                            }
                        });

                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("查询失败：" + ex.getMessage());
                            JOptionPane.showMessageDialog(StudentManagerUI.this,
                                    "查询所有学生失败！\n" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                        });
                    }
                }).start();
            }
        });

        // 2. 随机查询3名学生
        btnRandomQuery.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(() -> {
                    try {
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("正在随机查询3名学生...");
                            clearTableData();
                        });

                        Object[][] randomStudents = randomCountService.randomQueryStudentsForUI(3);

                        SwingUtilities.invokeLater(() -> {
                            if (randomStudents.length == 0) {
                                statusLabel.setText("随机查询完成：无学生数据");
                            } else {
                                for (Object[] student : randomStudents) {
                                    tableModel.addRow(student);
                                }
                                statusLabel.setText("随机查询完成：共找到 3 名学生");
                            }
                        });

                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("随机查询失败：" + ex.getMessage());
                            JOptionPane.showMessageDialog(StudentManagerUI.this,
                                    "随机查询学生失败！\n" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                        });
                    }
                }).start();
            }
        });

        // 3. 随机统计年级学生数（弹窗展示结果）
        btnRandomCountGrade.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(() -> {
                    try {
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("正在随机统计年级学生数...");
                            clearTableData(); // 清空表格（该功能只展示统计结果，不展示列表）
                        });

                        // 调用业务服务，返回统计信息（年级名称 + 学生数）
                        String[] gradeCountInfo = randomCountService.randomCountByGradeForUI();
                        String grade = gradeCountInfo[0];
                        int count = Integer.parseInt(gradeCountInfo[1]);

                        // 弹窗展示结果
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("统计完成：年级【" + grade + "】共 " + count + " 名学生");
                            JOptionPane.showMessageDialog(StudentManagerUI.this,
                                    "随机选中年级：" + grade + "\n该年级学生总数：" + count,
                                    "年级统计结果", JOptionPane.INFORMATION_MESSAGE);
                        });

                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("统计失败：" + ex.getMessage());
                            JOptionPane.showMessageDialog(StudentManagerUI.this,
                                    "随机统计年级失败！\n" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                        });
                    }
                }).start();
            }
        });

        // 4. 随机查询年龄段学生
        btnRandomAgeRange.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(() -> {
                    try {
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("正在随机查询年龄段学生...");
                            clearTableData();
                        });

                        // 调用业务服务，返回：年龄段描述 + 学生数据数组
                        Object[] ageRangeResult = randomCountService.randomQueryByAgeRangeForUI();
                        String ageRangeDesc = (String) ageRangeResult[0];
                        Object[][] studentsInAgeRange = (Object[][]) ageRangeResult[1];

                        SwingUtilities.invokeLater(() -> {
                            if (studentsInAgeRange.length == 0) {
                                statusLabel.setText("查询完成：" + ageRangeDesc + " 无学生数据");
                            } else {
                                for (Object[] student : studentsInAgeRange) {
                                    tableModel.addRow(student);
                                }
                                statusLabel
                                        .setText("查询完成：" + ageRangeDesc + " 共 " + studentsInAgeRange.length + " 名学生");
                            }
                        });

                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("查询失败：" + ex.getMessage());
                            JOptionPane.showMessageDialog(StudentManagerUI.this,
                                    "随机查询年龄段学生失败！\n" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                        });
                    }
                }).start();
            }
        });
    }

    /**
     * 清空表格数据
     */
    private void clearTableData() {
        int rowCount = tableModel.getRowCount();
        // 从最后一行开始删除（避免索引异常）
        for (int i = rowCount - 1; i >= 0; i--) {
            tableModel.removeRow(i);
        }
    }

    public static void main(String[] args) {
        // Swing界面必须在EDT（事件调度线程）中启动
        SwingUtilities.invokeLater(() -> {
            StudentManagerUI ui = new StudentManagerUI();
            ui.setVisible(true); // 显示窗口
        });
    }
}