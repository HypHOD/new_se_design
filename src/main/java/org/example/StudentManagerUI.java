package org.example;

import org.example.StudentQueryController;
import org.example.StudentRandomQuery;
import org.example.StudentRandomQuery.Student;
import org.apache.logging.log4j.util.InternalException;
import org.example.StudentQueryAnalyze;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import javax.imageio.ImageIO;

/**
 * 学生管理系统Swing主界面
 * 只负责UI展示和用户交互，通过调用业务服务实现功能
 */
public class StudentManagerUI extends JFrame {
    // 1. 业务服务依赖（初始化后注入）
    private StudentQueryAnalyze queryService;
    private StudentRandomQuery randomQueryService;

    // 2. UI组件
    private JTable studentTable; // 学生数据展示表格
    private DefaultTableModel tableModel; // 表格数据模型
    private JLabel statusLabel; // 状态提示标签

    // 倒计时组件
    private JLabel countdownLabel;
    private Timer countdownTimer;
    private Map<Integer, Boolean> lateMarkMap;// 记录迟到学生

    /**
     * 构造函数：初始化UI和业务服务
     */
    public StudentManagerUI() {
        // 初始化业务服务
        StudentQueryController dbController = new StudentQueryController();
        this.queryService = new StudentQueryAnalyze(dbController);
        this.randomQueryService = new StudentRandomQuery(dbController);

        lateMarkMap = new HashMap<>();
        countdownLabel = new JLabel("");
        countdownLabel.setForeground(Color.RED);

        // 初始化UI
        initUI();
    }

    private void initUI() {
        // 窗口配置
        setTitle("学生信息管理系统");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 居中显示
        setLayout(new BorderLayout(10, 10)); // 边界布局，间距10px
        setResizable(false);

        // ---------------------- 顶部按钮区域 ----------------------
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 10)); // 流式布局，按钮间距15px

        // 创建功能按钮
        JButton btnQueryAll = new JButton("查询所有学生");
        JButton btnRandomQuery = new JButton("随机查询");
        JButton btnQueryInOrder = new JButton("顺序点名");
        JButton btnAnalize = new JButton("统计分析");

        // 设置按钮大小
        Dimension btnSize = new Dimension(150, 30);
        btnQueryAll.setPreferredSize(btnSize);
        btnRandomQuery.setPreferredSize(btnSize);
        btnQueryInOrder.setPreferredSize(btnSize);
        btnAnalize.setPreferredSize(btnSize);

        // 添加按钮到面板
        buttonPanel.add(btnQueryAll);
        buttonPanel.add(btnRandomQuery);
        buttonPanel.add(btnQueryInOrder);
        buttonPanel.add(btnAnalize);

        // ---------------------- 中间表格展示区域 ----------------------
        // 表格列名
        String[] columnNames = { "ID", "姓名", "照片", "是否到场", "迟到", "补签" };
        // 表格数据模型（空数据初始化）
        tableModel = new DefaultTableModel(null, columnNames) {
            // 指定可编辑列
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5;
            }

            // 指定补签列按钮
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 4) {
                    return JButton.class; // 补签列是按钮类型
                }
                return super.getColumnClass(columnIndex);
            }
        };
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

        // 增加底部倒计时
        countdownLabel = new JLabel("");
        countdownLabel.setForeground(Color.RED);

        statusPanel.add(Box.createHorizontalGlue());// 填充空白调整布局
        statusPanel.add(countdownLabel);

        // 设置列宽
        studentTable.getColumnModel().getColumn(0).setPreferredWidth(60); // ID列
        studentTable.getColumnModel().getColumn(1).setPreferredWidth(80); // 姓名列
        studentTable.getColumnModel().getColumn(2).setPreferredWidth(120); // 照片列
        studentTable.getColumnModel().getColumn(3).setPreferredWidth(80); // 是否到场列
        studentTable.getColumnModel().getColumn(4).setPreferredWidth(80); // 迟到列
        studentTable.getColumnModel().getColumn(5).setPreferredWidth(80); // 补签按钮列

        // 补签按钮渲染
        studentTable.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        studentTable.getColumnModel().getColumn(5).setCellEditor(new ButtonEditor(new JCheckBox()));

        // ---------------------- 组装窗口 ----------------------
        add(buttonPanel, BorderLayout.NORTH); // 顶部按钮面板
        add(tableScrollPane, BorderLayout.CENTER); // 中间表格
        add(statusPanel, BorderLayout.SOUTH); // 底部状态栏

        // ---------------------- 随机查询的条件填写窗口 ----------------------
        // ---------------------- 顺序点名窗口 ----------------------

        // ---------------------- 绑定按钮事件 ----------------------
        bindButtonEvents(btnQueryAll, btnRandomQuery, btnQueryInOrder, btnAnalize);
    }

    // 绑定按钮点击事件
    private void bindButtonEvents(JButton btnQueryAll, JButton btnRandomQuery, JButton btnQueryInOrder,
            JButton btnAnalize) {
        btnQueryAll.addActionListener(e -> createQueryAllStudentsWindow());

        btnRandomQuery.addActionListener(e -> createRandomQueryWindow());

        btnQueryInOrder.addActionListener(e -> createQueryInOrderWindow());

        btnAnalize.addActionListener(e -> createAnalizeQueriedStudentsWindow());

    }

    private Object createQueryAllStudentsWindow() {

        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("正在查询全部学生数据...");
        });
        new Thread(() -> {
            try {
                List<Student> allStudents = queryService.getAllStudentsWithStats();
                // 更新主页表格
                SwingUtilities.invokeLater(() -> {
                    clearTableData();
                    if (allStudents != null && !allStudents.isEmpty()) {
                        for (Student student : allStudents) {
                            tableModel.addRow(new Object[] {
                                    student.getStudent_id(),
                                    student.getName(),
                                    student.getAvatar_url(),
                                    false
                            });
                        }
                        statusLabel.setText("查询成功：共加载 " + allStudents.size() + " 名学生数据");
                    } else {
                        statusLabel.setText("查询结果: 无记录");
                        JOptionPane.showMessageDialog(StudentManagerUI.this, "数据库无数据", "提示",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                });
            } catch (Exception e) {
                // 异常处理
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("查询失败：" + e.getMessage());
                    JOptionPane.showMessageDialog(
                            StudentManagerUI.this,
                            "查询所有学生失败：" + e.getMessage(),
                            "错误",
                            JOptionPane.ERROR_MESSAGE);
                    // 恢复按钮可用
                    // btnQueryAll.setEnabled(true);
                });
                e.printStackTrace();

            }
        }).start();
        // 读取所有学生写入列表
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'queryAllStudents'");
    }

    protected void createAnalizeQueriedStudentsWindow() {
        // todo 新建窗口, 显示本班所有学生, 增加筛选条件: 本次参与点名的学生
        // 允许对学生列表按照id、姓名、迟到次数、缺课次数排序
        // 根据本次点到的结果对学生迟到缺课次数做出修改
        // 增加结束签到按钮, 点击后将变化数据写回数据库

        // 创建窗口
        JDialog analyzeDialog = new JDialog(this, "学生点名统计分析", true);
        analyzeDialog.setSize(850, 600);
        analyzeDialog.setLayout(new BorderLayout(10, 10));
        analyzeDialog.setResizable(false);
        analyzeDialog.setLocationRelativeTo(this);

        // 顶部筛选排序功能
        JPanel topPanel = new JPanel(new BorderLayout(15, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 5, 15));

        // 筛选条件面板left
        JPanel filiterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        filiterPanel.setBorder(BorderFactory.createTitledBorder("筛选条件"));
        JLabel filliterLabel = new JLabel("显示学生:");
        JComboBox<String> filiterComboBox = new JComboBox<>(new String[] { "全部学生", "仅参与点名学生" });
        filiterComboBox.setPreferredSize(new Dimension(150, 25));

        filiterPanel.add(filliterLabel);
        filiterPanel.add(filiterComboBox);

        // 筛选条件面板right
        JPanel sortPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        sortPanel.setBorder(BorderFactory.createTitledBorder("排序方式"));
        JLabel sortLabel = new JLabel("选择排序条件:");
        JComboBox<String> sortFieldComboBox = new JComboBox<>(new String[] { "ID", "姓名", "缺课次数", "迟到次数" });
        JComboBox<String> sortOrderComboBox = new JComboBox<>(new String[] { "升序", "降序" });
        JButton sortBtn = createSmallButton("执行排序");

        sortFieldComboBox.setPreferredSize(new Dimension(100, 25));
        sortOrderComboBox.setPreferredSize(new Dimension(80, 25));

        sortPanel.add(sortLabel);
        sortPanel.add(sortFieldComboBox);
        sortPanel.add(sortOrderComboBox);
        sortPanel.add(sortBtn);

        topPanel.add(filiterPanel, BorderLayout.WEST);
        topPanel.add(sortPanel, BorderLayout.EAST);

        // 表格展示 center

        String[] columnNames = { "ID", "NAME", "LATE", "ABSENT", "本次签到" };
        DefaultTableModel analyzeTableModel = new DefaultTableModel(null, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2 || column == 3;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 2 || columnIndex == 3) {
                    return Integer.class; // 次数列是整数类型
                } else if (columnIndex == 4) {
                    return String.class; // 点名状态是字符串
                }
                return super.getColumnClass(columnIndex);
            }
        };
        JTable analyzeTable = new JTable(analyzeTableModel);
        analyzeTable.setRowHeight(25);
        analyzeTable.setDefaultEditor(String.class, null);

        analyzeTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        analyzeTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        analyzeTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        analyzeTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        analyzeTable.getColumnModel().getColumn(4).setPreferredWidth(120);

        JScrollPane tableScrollPane = new JScrollPane(analyzeTable);

        // 统计信息和按钮区域 bottom
        JPanel bottomPanel = new JPanel(new BorderLayout(15, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 10, 15));

        // 统计信息标签
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        JLabel totalStatsLabel = new JLabel("总学生数：0 人");
        JLabel lateStatsLabel = new JLabel("累计迟到：0 人次");
        JLabel absentStatsLabel = new JLabel("累计缺课：0 人次");
        statsPanel.add(totalStatsLabel);
        statsPanel.add(lateStatsLabel);
        statsPanel.add(absentStatsLabel);

        // 功能按钮
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 5));
        JButton saveBtn = createFunctionButton("保存修改");
        JButton resetBtn = createFunctionButton("重置数据");
        JButton closeBtn = createFunctionButton("关闭窗口");
        btnPanel.add(saveBtn);
        btnPanel.add(resetBtn);
        btnPanel.add(closeBtn);

        bottomPanel.add(statsPanel, BorderLayout.WEST);
        bottomPanel.add(btnPanel, BorderLayout.EAST);

        // 组装窗口
        analyzeDialog.add(topPanel, BorderLayout.NORTH);
        analyzeDialog.add(tableScrollPane, BorderLayout.CENTER);
        analyzeDialog.add(bottomPanel, BorderLayout.SOUTH);

        // 数据容器
        List<StudentAnalysisData> originalStudentData = new ArrayList<>();
        Map<Integer, Boolean> presentStatusMap = new HashMap<>();

        // 加载数据
        new Thread(() -> {
            try {
                List<Student> tableStudents = extractStudentsFromTableModel();
                if (tableStudents.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(analyzeDialog, "请先查询学生并完成点名", "无数据提示",
                                JOptionPane.INFORMATION_MESSAGE);
                        analyzeDialog.dispose();
                        statusLabel.setText("统计分析失败：无学生数据");
                    });
                    return;
                }

                // 提取点名结果
                for (int row = 0; row < tableModel.getRowCount(); row++) {
                    try {
                        int studentId = Integer.parseInt(tableModel.getValueAt(row, 0).toString().trim());
                        boolean isPresent = (boolean) tableModel.getValueAt(row, 3);
                        presentStatusMap.put(studentId, isPresent);
                    } catch (Exception e) {
                        System.err.println("提取点名状态失败（行号：" + (row + 1) + "）：" + e.getMessage());
                    }
                }
                // 从数据库查询全部学生
                List<Student> fullStudentList = queryService.getAllStudentsWithStats();
                for (Student dbStudent : fullStudentList) {
                    int student_id = dbStudent.getStudent_id();
                    boolean isPresent = presentStatusMap.getOrDefault(student_id, false);
                    boolean isLate = presentStatusMap.getOrDefault(student_id, false);
                    StudentAnalysisData analysisData = new StudentAnalysisData(
                            student_id,
                            dbStudent.getName(),
                            dbStudent.getLate(),
                            dbStudent.getAbsence(),
                            isPresent,
                            isLate);
                    originalStudentData.add(analysisData);
                }
                // 初始化表格
                SwingUtilities.invokeLater(() -> {
                    updateAnalyzeTable(originalStudentData, analyzeTableModel,
                            filiterComboBox.getSelectedItem().toString());
                    updateStatsInfo(originalStudentData, totalStatsLabel, lateStatsLabel, absentStatsLabel);

                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(analyzeDialog, "数据加载失败：" + e.getMessage(), "错误提示",
                            JOptionPane.ERROR_MESSAGE);
                    analyzeDialog.dispose();
                    statusLabel.setText("统计分析失败：" + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();

        // 更新筛选条件
        filiterComboBox.addActionListener(e -> {
            String filter = filiterComboBox.getSelectedItem().toString();
            updateAnalyzeTable(originalStudentData, analyzeTableModel, filter);
        });

        // 排序按钮
        sortBtn.addActionListener(e -> {
            String sortField = sortFieldComboBox.getSelectedItem().toString();
            String sortOrder = sortOrderComboBox.getSelectedItem().toString();
            sortStudentData(originalStudentData, analyzeTableModel, sortField, sortOrder,
                    filiterComboBox.getSelectedItem().toString());

        });

        // 保存签到结果,写回数据库
        saveBtn.addActionListener(e -> {
            // 收集表格数据
            List<StudentAnalysisData> modifiedData = new ArrayList<>();
            for (int row = 0; row < analyzeTableModel.getRowCount(); row++) {
                try {
                    int studentId = Integer.parseInt(analyzeTableModel.getValueAt(row, 0).toString().trim());
                    String name = analyzeTableModel.getValueAt(row, 1).toString().trim();
                    int lateCount = Integer.parseInt(analyzeTableModel.getValueAt(row, 2).toString().trim());
                    int absentCount = Integer.parseInt(analyzeTableModel.getValueAt(row, 3).toString().trim());

                    // 找到原始数据并更新
                    for (StudentAnalysisData original : originalStudentData) {
                        if (original.getStudentId() == studentId) {
                            original.setLateCount(lateCount);
                            original.setAbsentCount(absentCount);
                            modifiedData.add(original);
                            break;
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("收集修改数据失败（行号：" + (row + 1) + "）：" + ex.getMessage());
                }
            }
            // 后台保存到数据库
            new Thread(() -> {
                try {
                    boolean saveSuccess = queryService.updateStudentStats(modifiedData.stream()
                            .map(data -> {
                                Student student = new Student();
                                student.setStudent_id(data.getStudentId());
                                student.setName(data.getName());
                                student.setLate(data.getLateCount());
                                student.setAbsence(data.getAbsentCount());
                                return student;
                            })
                            .collect(Collectors.toList()));

                    SwingUtilities.invokeLater(() -> {
                        if (saveSuccess) {
                            JOptionPane.showMessageDialog(analyzeDialog, "学生统计数据已成功保存到数据库", "保存成功",
                                    JOptionPane.INFORMATION_MESSAGE);
                            statusLabel.setText("统计数据保存成功：共更新 " + modifiedData.size() + " 名学生信息");
                        } else {
                            JOptionPane.showMessageDialog(analyzeDialog, "数据保存失败，请重试", "保存失败",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(analyzeDialog, "保存异常：" + ex.getMessage(), "错误提示",
                                JOptionPane.ERROR_MESSAGE);
                    });
                    ex.printStackTrace();
                }
            }).start();

        });

        // 重置数据
        resetBtn.addActionListener(e -> {
            String filter = filiterComboBox.getSelectedItem().toString();
            updateAnalyzeTable(originalStudentData, analyzeTableModel, filter);
            JOptionPane.showMessageDialog(analyzeDialog, "重制数据为初始状态");
        });

        // 关闭窗口
        closeBtn.addActionListener(e -> analyzeDialog.dispose());

        // 显示窗口
        analyzeDialog.setVisible(true);

    }

    // 辅助类
    public class StudentAnalysisData {
        private int student_id;
        private String name;
        private int lateCount;
        private int absentCount;
        private boolean isPresent;
        private boolean isLate;

        public StudentAnalysisData(int id, String name, int late, int absent, boolean isPresent, boolean isLate) {
            this.student_id = id;
            this.name = name;
            this.lateCount = late;
            this.absentCount = absent;
            this.isPresent = isPresent;
            this.isLate = isLate;
        }

        public int getStudentId() {
            return student_id;
        }

        public String getName() {
            return name;
        }

        public int getLateCount() {
            return lateCount;
        }

        public void setLateCount(int lateCount) {
            this.lateCount = lateCount;
        }

        public int getAbsentCount() {
            return absentCount;
        }

        public void setAbsentCount(int absentCount) {
            this.absentCount = absentCount;
        }

        public boolean isPresent() {
            return isPresent;
        }

        public boolean isLate() {
            return isLate;
        }

    }

    // 辅助函数 排序
    private void sortStudentData(List<StudentAnalysisData> dataList, DefaultTableModel tableModel, String sortField,
            String sortOrder, String filter) {
        List<StudentAnalysisData> sortedList = new ArrayList<>(dataList);

        // 定义比较器
        Comparator<StudentAnalysisData> comparator = null;
        switch (sortField) {
            case "ID":
                comparator = Comparator.comparingInt(StudentAnalysisData::getStudentId);
                break;
            case "姓名":
                comparator = Comparator.comparing(StudentAnalysisData::getName);
                break;
            case "迟到次数":
                comparator = Comparator.comparingInt(StudentAnalysisData::getLateCount);
                break;
            case "缺课次数":
                comparator = Comparator.comparingInt(StudentAnalysisData::getAbsentCount);
                break;
        }
        // 处理降序
        if ("降序".equals(sortOrder) && comparator != null) {
            comparator = comparator.reversed();
        }

        // 执行排序并更新表格
        if (comparator != null) {
            sortedList.sort(comparator);
            updateAnalyzeTable(sortedList, tableModel, filter);
        }
    }

    // 辅助函数 更行状态信息
    private void updateStatsInfo(List<StudentAnalysisData> dataList, JLabel totalLabel,
            JLabel lateLabel, JLabel absentLabel) {
        int totalCount = dataList.size();
        int totalLate = dataList.stream().mapToInt(StudentAnalysisData::getLateCount).sum();
        int totalAbsent = dataList.stream().mapToInt(StudentAnalysisData::getAbsentCount).sum();

        totalLabel.setText("总学生数：" + totalCount + " 人");
        lateLabel.setText("累计迟到：" + totalLate + " 人次");
        absentLabel.setText("累计缺课：" + totalAbsent + " 人次");
    }

    // 辅助函数 更新表格内容
    private void updateAnalyzeTable(List<StudentAnalysisData> dataList, DefaultTableModel tableModel, String filter) {
        // 清空表格
        while (tableModel.getRowCount() > 0) {
            tableModel.removeRow(0);
        }

        // 筛选并添加数据
        for (StudentAnalysisData data : dataList) {
            // 根据筛选条件过滤
            if ("仅参与点名学生".equals(filter) && !data.isPresent()) {
                continue;
            }

            // 转换点名状态为中文显示
            String presentStatus = data.isPresent() ? "已到场" : "未参与/未到场";

            // 添加到表格
            tableModel.addRow(new Object[] {
                    data.getStudentId(),
                    data.getName(),
                    data.getLateCount(),
                    data.getAbsentCount(),
                    presentStatus
            });
        }
    }

    protected void createQueryInOrderWindow() {
        // 顺序点名tableModel的学生
        JDialog orderDialog = new JDialog(this, "顺序点名", true);
        orderDialog.setSize(500, 450);
        orderDialog.setLayout(new BorderLayout(15, 15));
        orderDialog.setResizable(false);
        orderDialog.setLocationRelativeTo(this);

        // 界面
        // List<Student> orderStudentsList = Collections.emptyList();
        final AtomicReference<List<Student>> orderStudentsList = new AtomicReference<List<Student>>(
                Collections.emptyList());
        int[] currentIndex = { 0 };
        Map<Long, Boolean> presentStatusMap = new HashMap<>();

        JLabel progressLabel = new JLabel("加载学生数据..", SwingConstants.CENTER);

        // 学生信息
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 30, 20, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 15, 12, 15);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel idLabel = new JLabel("ID: ");
        JLabel idValueLabel = new JLabel("-");

        JLabel nameLabel = new JLabel("NAME: ");
        JLabel nameValueLabel = new JLabel("-");

        JLabel avatarLabel = new JLabel("avatar: ");
        JLabel avatarValueLabel = new JLabel("无照片", SwingConstants.CENTER);
        avatarValueLabel.setPreferredSize(new Dimension(100, 130));
        avatarValueLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        JLabel presentLabel = new JLabel("签到: ");
        JCheckBox presentCheckBox = new JCheckBox();

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 15));
        JButton prevBtn = createFunctionButton("上一个");
        JButton nextBtn = createFunctionButton("下一个");
        JButton finishBtn = createFunctionButton("结束点名");

        prevBtn.setEnabled(false);

        // 设置布局
        gbc.gridx = 0;
        gbc.gridy = 0;
        infoPanel.add(idLabel, gbc);
        gbc.gridx = 1;
        infoPanel.add(idValueLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        infoPanel.add(nameLabel, gbc);
        gbc.gridx = 1;
        infoPanel.add(nameValueLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        infoPanel.add(avatarLabel, gbc);
        gbc.gridx = 1;
        infoPanel.add(avatarValueLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        infoPanel.add(presentLabel, gbc);
        gbc.gridx = 1;
        infoPanel.add(presentCheckBox, gbc);

        orderDialog.add(progressLabel, BorderLayout.NORTH);
        orderDialog.add(infoPanel, BorderLayout.CENTER);
        orderDialog.add(btnPanel, BorderLayout.SOUTH);
        btnPanel.add(prevBtn);
        btnPanel.add(nextBtn);
        btnPanel.add(finishBtn);

        Runnable updateStudentInfo = () -> {
            if (orderStudentsList.get().isEmpty())
                return;

            Student currentStudent = orderStudentsList.get().get(currentIndex[0]);
            int studentId = currentStudent.getStudent_id();

            idValueLabel.setText(String.valueOf(studentId));
            nameValueLabel.setText(currentStudent.getName());
            loadStudentAvatar(currentStudent.getAvatar_url(), avatarValueLabel);

            presentCheckBox.setSelected(presentStatusMap.getOrDefault(studentId, false));
            // 更新进度显示
            progressLabel
                    .setText(String.format("顺序点名: 第%d/%d名学生", currentIndex[0] + 1, orderStudentsList.get().size()));

            prevBtn.setEnabled(currentIndex[0] > 0);
            nextBtn.setEnabled(currentIndex[0] < orderStudentsList.get().size() - 1);
        };

        // 按钮事件绑定
        prevBtn.addActionListener(e -> {
            if (currentIndex[0] <= 0) {
                return;
            }
            Student currentStudent = orderStudentsList.get().get(currentIndex[0]);
            presentStatusMap.put(Long.valueOf(currentStudent.getStudent_id()), presentCheckBox.isSelected());

            currentIndex[0]--;
            updateStudentInfo.run();
        });

        nextBtn.addActionListener(e -> {
            if (currentIndex[0] < orderStudentsList.get().size() - 1) {
                Student currentStudent = orderStudentsList.get().get(currentIndex[0]);
                presentStatusMap.put(Long.valueOf(currentStudent.getStudent_id()), presentCheckBox.isSelected());

                // 切换到下一个学生
                currentIndex[0]++;
                updateStudentInfo.run();

                // 语音点到
                Student speakStudent = orderStudentsList.get().get(currentIndex[0]);
                speakQuery(speakStudent.getName());

            }
        });

        finishBtn.addActionListener(e -> {
            finishBtn.setEnabled(false);
            if (!orderStudentsList.get().isEmpty()) {
                Student currentStudent = orderStudentsList.get().get(currentIndex[0]);
                presentStatusMap.put(Long.valueOf(currentStudent.getStudent_id()), presentCheckBox.isSelected());

                // 统计到场人数
                long presentCount = presentStatusMap.values().stream()
                        .filter(Boolean::booleanValue)
                        .count();

                SwingUtilities.invokeLater(() -> {
                    // 遍历主界面表格的所有行，同步签到状态
                    for (int row = 0; row < tableModel.getRowCount(); row++) {
                        try {
                            // 1. 提取表格当前行的学生ID（转为Long，匹配presentStatusMap的key）
                            int tableStudentId = Integer.parseInt(tableModel.getValueAt(row, 0).toString().trim());
                            Long studentIdKey = Long.valueOf(tableStudentId);

                            // 2. 从Map中获取签到状态（无记录则默认未到场）
                            boolean isPresent = presentStatusMap.getOrDefault(studentIdKey, false);

                            // 3. 更新表格第3列（「是否到场」列，索引为3）
                            tableModel.setValueAt(isPresent, row, 3);
                        } catch (Exception ex) {
                            // 跳过无效数据行，不影响整体更新
                            System.err.println("跳过表格中无效数据行（行号：" + (row + 1) + "）：" + ex.getMessage());
                            continue;
                        }
                    }
                    // 4. 通知表格数据变化，强制刷新显示
                    tableModel.fireTableDataChanged();
                });

                // 启动倒计时
                startCountDown(10);

                // 更新主界面状态提示
                SwingUtilities.invokeLater(() -> statusLabel.setText(String.format("顺序点名结束：共%d名学生，到场%d人",
                        orderStudentsList.get().size(), presentCount)));
                countdownLabel.setText("补签倒计时:");
            }
            orderDialog.dispose();
        });

        // 后台加载数据
        new Thread(() -> {
            try {
                // todo 使用查询到的tableModel的数据加载
                List<Student> studentsList = extractStudentsFromTableModel();
                if (studentsList.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(orderDialog,
                                "请先通过前两项查询学生",
                                "无数据提示",
                                JOptionPane.INFORMATION_MESSAGE);
                        orderDialog.dispose();
                        statusLabel.setText("请先查询学生再点名");
                    });
                    return;
                }
                // 按照ID升序
                Collections.sort(studentsList, Comparator.comparingInt(Student::getStudent_id));

                // 初始化点名状态
                presentStatusMap.clear();
                for (Student student : studentsList) {
                    presentStatusMap.put(Long.valueOf(student.getStudent_id()), false);
                }
                // 更新全局变量
                // orderStudentsList = studentsList;
                orderStudentsList.set(studentsList);
                currentIndex[0] = 0;
                SwingUtilities.invokeLater(() -> {
                    updateStudentInfo.run();
                    statusLabel.setText(String.format("顺序点名, 共%d名学生", studentsList.size()));
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(orderDialog,
                            "加载点名数据失败：" + e.getMessage(),
                            "错误提示",
                            JOptionPane.ERROR_MESSAGE);
                    orderDialog.dispose();
                    statusLabel.setText("就绪：数据加载失败，请重试");
                });
                e.printStackTrace();
            }

        }).start();

        orderDialog.setVisible(true);

    }

    // 辅助函数 倒计时
    private void startCountDown(int timeSet) {
        if (countdownTimer != null) {
            countdownTimer.cancel();
        }
        lateMarkMap.clear();

        // int totalSeconds = timeSet * 60;// 设定总倒计时时间
        countdownTimer = new Timer();
        AtomicInteger totalSeconds = new AtomicInteger(timeSet * 60);
        countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (totalSeconds.get() <= 0) {
                    SwingUtilities.invokeLater(() -> {
                        countdownLabel.setText("倒计时结束!补签视为迟到!");
                        statusLabel.setText(statusLabel.getText().replace("补签倒计时开始", "倒计时结束"));

                    });
                    markLateAfterCountdown();
                    countdownTimer.cancel();
                    return;
                }
                int minutes = totalSeconds.get() / 60;
                int seconds = totalSeconds.get() % 60;
                String timeStr = String.format("%02d:%02d", minutes, seconds);
                SwingUtilities.invokeLater(() -> countdownLabel.setText("补签倒计时：" + timeStr));

                totalSeconds.decrementAndGet();
            }
        }, 0, 1000);
    }

    // 辅助函数 迟到后补签视为迟到不缺席
    private void markLateAfterCountdown() {
        SwingUtilities.invokeLater(() -> {
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                try {
                    int studentId = Integer.parseInt(tableModel.getValueAt(row, 0).toString().trim());
                    boolean isPresent = (boolean) tableModel.getValueAt(row, 3);
                    // 未签到且已补签的学生：标记为迟到（不缺席）
                    if (isPresent && lateMarkMap.containsKey(studentId)) {
                        tableModel.setValueAt(true, row, 4); // 迟到列=true
                        // 缺席列保持false（不缺席）
                    }
                } catch (Exception e) {
                    System.err.println("标记迟到失败：" + e.getMessage());
                    continue;
                }
            }
            tableModel.fireTableDataChanged();
        });
    }

    private List<Student> extractStudentsFromTableModel() {
        List<Student> studentList = new ArrayList<>();
        int rowCount = tableModel.getRowCount();

        // 遍历tableModel的每一行，转换为Student对象
        for (int row = 0; row < rowCount; row++) {
            try {
                // 从表格列中提取数据（列索引对应：0=ID，1=姓名，2=照片URL，3=是否到场）
                int studentId = Integer.parseInt(tableModel.getValueAt(row, 0).toString().trim());
                String name = tableModel.getValueAt(row, 1) == null ? "未知姓名"
                        : tableModel.getValueAt(row, 1).toString().trim();
                String avatarUrl = tableModel.getValueAt(row, 2) == null ? ""
                        : tableModel.getValueAt(row, 2).toString().trim();

                // 构造Student对象（如果Student类需要迟到/缺课次数，可设为默认值0，不影响点名功能）
                Student student = new Student();
                student.setStudent_id(studentId);
                student.setName(name);
                student.setAvatar_url(avatarUrl);
                // 若Student类有late_count/absent_count字段，补充默认值（根据实际类结构调整）
                // student.setLate_count(0);
                // student.setAbsent_count(0);

                studentList.add(student);
            } catch (Exception e) {
                // 跳过数据格式错误的行，不影响整体点名功能
                System.err.println("跳过无效学生数据（行号：" + (row + 1) + "）：" + e.getMessage());
                continue;
            }
        }

        return studentList;
    }

    private void loadStudentAvatar(String avatarUrl, JLabel targetJLabel) {
        try {
            if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
                targetJLabel.setIcon(null);
                targetJLabel.setText("无照片");
                return;
            }

            // 判断是本地存储还是oss存储
            String urlTrim = avatarUrl.trim();
            Image image;
            if (urlTrim.startsWith("http://") || urlTrim.startsWith("https://")) {
                // 加载oss图片
                URI ossUrl = new URI(urlTrim);
                URL ossUrl2 = ossUrl.toURL();
                image = ImageIO.read(ossUrl2);
                if (image == null) {
                    throw new IOException("OSS图片为空或格式不支持");
                }
            } else {
                File avatarFile = new File(avatarUrl);
                if (!avatarFile.exists()) {
                    throw new FileNotFoundException("学生照片不存在");
                }
                if (!avatarFile.isFile()) {
                    throw new IOException("本地路径无效");

                }
                image = ImageIO.read(avatarFile);

            }

            // 调整图片比例
            Image scaledImage = image.getScaledInstance(
                    targetJLabel.getPreferredSize().width,
                    targetJLabel.getPreferredSize().height,
                    Image.SCALE_SMOOTH);

            targetJLabel.setIcon(new ImageIcon(scaledImage));
            targetJLabel.setText("");
        } catch (Exception e) {
            targetJLabel.setIcon(null);
            targetJLabel.setText("照片加载失败");
            System.err.println("照片加载失败: " + e.getMessage());
        }
    }

    protected void createRandomQueryWindow() {
        JDialog randomDialog = new JDialog(this, "随机查询", true);
        randomDialog.setSize(400, 280);
        randomDialog.setLayout(new BorderLayout(15, 15));
        randomDialog.setResizable(false);
        randomDialog.setLocationRelativeTo(this);

        // 输入选择信息
        JPanel inputJPanel = new JPanel(new GridBagLayout());
        inputJPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 5, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 15);

        // 迟到
        JLabel lateLabel = new JLabel("迟到次数大于:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0;
        inputJPanel.add(lateLabel, gbc);

        JTextField lateField = new JTextField();
        lateField.setHorizontalAlignment(JTextField.CENTER);
        lateField.setPreferredSize(new Dimension(100, 25));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;
        inputJPanel.add(lateField, gbc);

        // 缺课
        JLabel absentLabel = new JLabel("缺课次数大于:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0;
        inputJPanel.add(absentLabel, gbc);

        JTextField absentField = new JTextField();
        absentField.setHorizontalAlignment(JTextField.CENTER);
        absentField.setPreferredSize(new Dimension(100, 25));
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;
        inputJPanel.add(absentField, gbc);

        // 指定数量
        JLabel countLabel = new JLabel("查询数量限制");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0;
        inputJPanel.add(countLabel, gbc);

        JTextField countField = new JTextField("1");
        countField.setHorizontalAlignment(JTextField.CENTER);
        countField.setPreferredSize(new Dimension(100, 25));
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;
        inputJPanel.add(countField, gbc);

        // 按钮面板
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 15));
        JButton confirmBtn = createSmallButton("confirm");
        JButton cancelBtn = createSmallButton("cancel");
        btnPanel.add(confirmBtn);
        btnPanel.add(cancelBtn);

        randomDialog.add(inputJPanel, BorderLayout.CENTER);
        randomDialog.add(btnPanel, BorderLayout.SOUTH);

        // 按钮监听
        confirmBtn.addActionListener(e -> {
            // 验证输入
            String lateStr = lateField.getText().trim();
            String absentStr = absentField.getText().trim();
            String countStr = countField.getText().trim();

            if (!validateRandomQueryInput(lateStr, absentStr, countStr, randomDialog)) {
                return;
            }
            // 2. 解析输入值（空值表示不限制该条件，用-1标记）
            int minLateCount = lateStr.isEmpty() ? -1 : Integer.parseInt(lateStr);
            int minAbsentCount = absentStr.isEmpty() ? -1 : Integer.parseInt(absentStr);
            int queryCount = Integer.parseInt(countStr);

            // 3. 关闭对话框并执行查询
            randomDialog.dispose();
            executeRandomQueryWithConditions(minLateCount, minAbsentCount, queryCount);

        });

        // 取消按钮事件
        cancelBtn.addActionListener(e -> randomDialog.dispose());

        randomDialog.setVisible(true);
    }

    private boolean validateRandomQueryInput(String lateStr, String absentStr, String countStr, JDialog dialog) {
        // 1. 验证「数量限制」：必填项，必须是 >0 的整数
        if (countStr.isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "请输入查询数量限制！", "输入错误", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        try {
            int count = Integer.parseInt(countStr);
            if (count <= 0) {
                JOptionPane.showMessageDialog(dialog, "查询数量必须大于0！", "输入错误", JOptionPane.WARNING_MESSAGE);
                return false;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(dialog, "查询数量请输入有效的数字！", "输入错误", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        // 2. 验证「迟到次数大于」：可选填，非空时必须是 ≥0 的整数（次数不能为负）
        if (!lateStr.isEmpty()) {
            try {
                int late = Integer.parseInt(lateStr);
                if (late < 0) {
                    JOptionPane.showMessageDialog(dialog, "迟到次数不能为负数！", "输入错误", JOptionPane.WARNING_MESSAGE);
                    return false;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "迟到次数请输入有效的数字！", "输入错误", JOptionPane.WARNING_MESSAGE);
                return false;
            }
        }

        // 3. 验证「缺课次数大于」：可选填，非空时必须是 ≥0 的整数（次数不能为负）
        if (!absentStr.isEmpty()) {
            try {
                int absent = Integer.parseInt(absentStr);
                if (absent < 0) {
                    JOptionPane.showMessageDialog(dialog, "缺课次数不能为负数！", "输入错误", JOptionPane.WARNING_MESSAGE);
                    return false;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "缺课次数请输入有效的数字！", "输入错误", JOptionPane.WARNING_MESSAGE);
                return false;
            }
        }

        // 所有验证通过
        return true;
    }

    JButton createFunctionButton(String text) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(110, 35));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(86, 133, 222));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(100, 149, 237));
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(72, 118, 200));
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (button.getBounds().contains(e.getPoint())) {
                    button.setBackground(new Color(86, 133, 222));
                } else {
                    button.setBackground(new Color(100, 149, 237));
                }
            }
        });
        return button;
    }

    private JButton createSmallButton(String text) {
        JButton button = new JButton(text);
        // 1. 设置按钮尺寸（适配对话框场景，比主界面按钮小）
        button.setPreferredSize(new Dimension(80, 30));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            // 鼠标移入：加深背景色
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(86, 133, 222));
            }

            // 鼠标移出：恢复原背景色
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(100, 149, 237));
            }

            // 鼠标按压：进一步加深背景色
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(72, 118, 200));
            }

            // 鼠标释放：恢复悬停色（如果还在按钮上）
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (button.getBounds().contains(e.getPoint())) {
                    button.setBackground(new Color(86, 133, 222));
                } else {
                    button.setBackground(new Color(100, 149, 237));
                }
            }
        });
        return button;
    }

    // 清空表格数据
    private void clearTableData() {
        int rowCount = tableModel.getRowCount();
        // 从最后一行开始删除（避免索引异常）
        for (int i = rowCount - 1; i >= 0; i--) {
            tableModel.removeRow(i);
        }
    }

    // 查询方法
    /**
     * 
     * @param minLateCount
     * @param minAbsentCount
     * @param queryCount
     */

    private void executeRandomQueryWithConditions(int minLateCount, int minAbsentCount, int queryCount) {

        // 后台线程
        new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("正在执行条件随机查询");
                    clearTableData();
                });

                // 执行查询
                StudentQueryController dbController = new StudentQueryController();
                StudentRandomQuery randomCount = new StudentRandomQuery(dbController);
                List<Student> randomList = randomCount.randomQueryStudent(minLateCount, minAbsentCount,
                        queryCount);

                // 更新ui
                SwingUtilities.invokeLater(() -> {
                    if (randomList != null && !randomList.isEmpty()) {
                        for (Student student : randomList) {
                            tableModel.addRow(new Object[] {
                                    student.getStudent_id(),
                                    student.getName(),
                                    student.getAvatar_url(),
                                    false
                            });
                        }
                        // 输出查询结果

                        String conditionDesc = buildConditionDescription(minLateCount, minAbsentCount, queryCount);
                        statusLabel.setText(("随机查询成功:" + conditionDesc + ", 共" + randomList.size() + "人"));
                    } else {
                        statusLabel.setText("随机查询结束: 无符合条件学生");
                        JOptionPane.showMessageDialog(
                                StudentManagerUI.this,
                                "没有找到符合条件的学生！",
                                "提示",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                });
            } catch (Exception e) {
                System.err.println("查询出错:" + e);
            }
        }).start();
    }

    /**
     * 
     * @param minLateCount
     * @param minAbsentCount
     * @param queryCount
     * @return
     */
    private String buildConditionDescription(int minLateCount, int minAbsentCount, int queryCount) {
        // 用 StringBuilder 拼接字符串（效率高于直接 + 拼接，尤其条件多时）
        StringBuilder desc = new StringBuilder();

        // 1. 先拼接「数量限制」（必选条件，始终显示）
        desc.append("数量限制").append(queryCount).append("人");

        // 2. 拼接「迟到次数条件」（-1 表示不限制，不显示该条件）
        if (minLateCount != -1) {
            desc.append("，迟到次数>").append(minLateCount);
        }

        // 3. 拼接「缺课次数条件」（-1 表示不限制，不显示该条件）
        if (minAbsentCount != -1) {
            desc.append("，缺课次数>").append(minAbsentCount);
        }

        // 返回最终拼接的描述文本
        return desc.toString();
    }

    // 语音播报点到
    private void speakQuery(String text) {
        String speakText = text == null || text.trim().isEmpty() ? "未知" : text.trim();

        new Thread(() -> {
            ProcessBuilder pb = null;
            String os = System.getProperty("os.name").toLowerCase();
            try {
                if (os.contains("win")) {
                    pb = new ProcessBuilder(
                            "powershell",
                            "-Command",
                            "Add-Type -AssemblyName System.Speech; " +
                                    "$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                                    "$speak.Speak('" + speakText + "');");
                } else if (os.contains("mac")) {
                    pb = new ProcessBuilder("say", speakText);
                } else if (os.contains("linux")) {
                    pb = new ProcessBuilder("espeak", speakText);
                } else {
                    System.err.println("不支持的系统");
                    return;
                }
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                pb.redirectError(ProcessBuilder.Redirect.DISCARD);
                pb.start().waitFor(); // 等待播报完成（不影响UI）
            } catch (IOException | InterruptedException e) {
                System.err.println("语音播报失败: " + e.getMessage());
            }

        }).start();
    }

    // 编辑补签按钮
    // ---------------------- 补签按钮相关自定义组件 ----------------------
    /**
     * 按钮渲染器：用于在表格中显示按钮
     */
    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setFont(new Font("宋体", Font.PLAIN, 11));
            setBackground(new Color(100, 149, 237));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            // 设置按钮文本
            setText((value == null) ? "" : value.toString());

            // 根据"是否到场"状态设置按钮是否可用
            boolean isPresent = (boolean) table.getValueAt(row, 3);
            setEnabled(!isPresent); // 未到场时可用，已到场时禁用

            // 禁用时改变按钮样式
            if (!isEnabled()) {
                setBackground(Color.LIGHT_GRAY);
                setText("已签到");
            } else {
                setBackground(new Color(100, 149, 237));
                setText("补签");
            }

            return this;
        }
    }

    /**
     * 按钮编辑器：用于处理按钮点击事件
     */
    class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;
        private boolean isPushed;
        private int currentRow;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.setFont(new Font("宋体", Font.PLAIN, 11));

            // 按钮点击事件
            button.addActionListener(e -> {
                fireEditingStopped(); // 停止编辑
                if (isPushed) {
                    // 执行补签操作
                    doSupplementSign();
                }
                isPushed = false;
            });
        }

        /**
         * 执行补签操作
         * 新增迟到逻辑
         */
        private void doSupplementSign() {
            int studentId = Integer.parseInt(tableModel.getValueAt(currentRow, 0).toString().trim());
            // 判断倒计时是否结束
            boolean isCountdownEnd = (countdownTimer == null);
            if (isCountdownEnd) {
                tableModel.setValueAt(true, currentRow, 4);// 记录迟到
                lateMarkMap.put(studentId, true);// 记录到迟到缓存
            } else {
                tableModel.setValueAt(false, currentRow, 4);// 倒计时内不记为迟到
            }
            // 更新"是否到场"状态为true
            tableModel.setValueAt(true, currentRow, 3);
            // 刷新补签按钮状态
            tableModel.fireTableCellUpdated(currentRow, 5);

            // 提示补签成功
            String studentName = tableModel.getValueAt(currentRow, 1).toString();
            statusLabel.setText("已为学生【" + studentName + "】完成补签");

            // 语音提示
            speakQuery(studentName + "补签成功");
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            currentRow = row;
            label = (value == null) ? "" : value.toString();
            button.setText(label);

            // 根据"是否到场"状态设置按钮可用性
            boolean isPresent = (boolean) table.getValueAt(row, 3);
            button.setEnabled(!isPresent);

            if (!button.isEnabled()) {
                button.setBackground(Color.LIGHT_GRAY);
                button.setText("已签到");
            } else {
                button.setText("补签");
            }

            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                // 按钮点击后返回新的文本
                return "已签到";
            }
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }

        @Override
        protected void fireEditingStopped() {
            super.fireEditingStopped();
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