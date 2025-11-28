package org.example;

import org.example.StudentQueryController;
import org.example.StudentRandomQuery;
import org.example.StudentRandomQuery.Student;
import org.example.StudentQueryAnalyze;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

    /**
     * 构造函数：初始化UI和业务服务
     */
    public StudentManagerUI() {
        // 初始化业务服务
        StudentQueryController dbController = new StudentQueryController();
        this.queryService = new StudentQueryAnalyze(dbController);
        this.randomQueryService = new StudentRandomQuery(dbController);

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
        String[] columnNames = { "ID", "姓名", "照片", "是否到场" };
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

        // ---------------------- 随机查询的条件填写窗口 ----------------------
        // ---------------------- 顺序点名窗口 ----------------------

        // ---------------------- 绑定按钮事件 ----------------------
        bindButtonEvents(btnQueryAll, btnRandomQuery, btnQueryInOrder, btnAnalize);
    }

    // 绑定按钮点击事件
    private void bindButtonEvents(JButton btnQueryAll, JButton btnRandomQuery, JButton btnQueryInOrder,
            JButton btnAnalize) {
        btnQueryAll.addActionListener(e -> queryAllStudents());

        btnRandomQuery.addActionListener(e -> createRandomQueryWindow());

        btnQueryInOrder.addActionListener(e -> createQueryInOrderWindow());

        btnAnalize.addActionListener(e -> analizeQueriedStudents());

    }

    private Object queryAllStudents() {
        new Thread(() -> {

        }).start();
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'queryAllStudents'");
    }

    protected void analizeQueriedStudents() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'analizeQueriedStudents'");
    }

    protected void createQueryInOrderWindow() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createQueryInOrderWindow'");
    }

    protected void createRandomQueryWindow() {
        JDialog randomDialog = new JDialog(this, "随机查询", true);
        randomDialog.setSize(400, 280);
        randomDialog.setLayout(new BorderLayout(15, 15));
        randomDialog.setResizable(false);

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

    private void executeRandomQueryWithConditions(int minLateCount, int minAbsentCount, int queryCount) {

        // 后台线程
        new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("正在执行条件随机查询");
                    clearTableData();
                });

                // 执行查询
                List<Student> randomList = randomQueryService.randomQueryStudent(minLateCount, minAbsentCount,
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

    public static void main(String[] args) {
        // Swing界面必须在EDT（事件调度线程）中启动
        SwingUtilities.invokeLater(() -> {
            StudentManagerUI ui = new StudentManagerUI();
            ui.setVisible(true); // 显示窗口
        });
    }
}