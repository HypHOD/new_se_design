package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class CodeStatisticsUI extends JFrame {
    private JTextField directoryField;
    private JTextArea resultArea;
    private JTable resultTable;
    private JProgressBar progressBar;
    private JButton statsButton;

    private static final Map<String, String[]> LANGUAGE_CONFIG = new HashMap<>();
    static {
        LANGUAGE_CONFIG.put("Java", new String[] { ".java" });
        LANGUAGE_CONFIG.put("C", new String[] { ".c", ".h" });
        LANGUAGE_CONFIG.put("C++", new String[] { ".cpp", ".cc", ".hpp" });
        LANGUAGE_CONFIG.put("Python", new String[] { ".py" });
        LANGUAGE_CONFIG.put("C#", new String[] { ".cs" });
    }

    private static final Set<String> SKIP_DIRS = new HashSet<>(Arrays.asList(
            ".git", ".idea", "target", "build", "out", "node_modules", "__pycache__"));

    private Set<String> selectedLanguages;
    private boolean countBlankInTotal;
    private boolean countCommentInTotal;

    private BarChartPanel barChartPanel;
    private PieChartPanel pieChartPanel1;
    private PieChartPanel pieChartPanel2;

    private static final Color COLOR_FILE_COUNT = new Color(59, 130, 246);
    private static final Color COLOR_CODE_LINE = new Color(249, 115, 22);
    private static final Color[] PIE_COLORS = {
            new Color(34, 197, 94),
            new Color(251, 191, 36),
            new Color(239, 68, 68)
    };
    private static final Color[] LANG_PIE_COLORS = {
            new Color(59, 130, 246),
            new Color(168, 85, 247),
            new Color(239, 68, 68),
            new Color(34, 197, 94),
            new Color(249, 115, 22)
    };

    public CodeStatisticsUI() {
        selectedLanguages = new HashSet<>(LANGUAGE_CONFIG.keySet());
        countBlankInTotal = true;
        countCommentInTotal = true;

        barChartPanel = new BarChartPanel();
        pieChartPanel1 = new PieChartPanel("代码行构成（整体）", -13);
        pieChartPanel2 = new PieChartPanel("各语言文件数占比");

        initUI();
    }

    private void initUI() {
        setTitle("代码统计工具");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel controlPanel = new JPanel(new BorderLayout(5, 5));
        directoryField = new JTextField("D:\\testcase5");
        JButton browseButton = new JButton("浏览...");
        JButton configButton = new JButton("统计配置");
        statsButton = new JButton("开始统计");

        browseButton.addActionListener(e -> browseDirectory());
        configButton.addActionListener(e -> showConfigDialog());
        statsButton.addActionListener(e -> startStatistics());

        JPanel pathPanel = new JPanel(new BorderLayout(5, 5));
        pathPanel.add(new JLabel("目录路径:"), BorderLayout.WEST);
        pathPanel.add(directoryField, BorderLayout.CENTER);
        pathPanel.add(browseButton, BorderLayout.EAST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.add(configButton);
        buttonPanel.add(statsButton);

        controlPanel.add(pathPanel, BorderLayout.CENTER);
        controlPanel.add(buttonPanel, BorderLayout.EAST);

        progressBar = new JProgressBar();
        progressBar.setVisible(false);

        String[] columnNames = { "语言", "文件数", "总行数", "代码行", "空行", "注释行", "函数数",
                "最大代码行", "最小代码行", "平均代码行", "中位数" };
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultTable = new JTable(tableModel);
        resultTable.setPreferredScrollableViewportSize(new Dimension(1000, 200));

        resultArea = new JTextArea(20, 80);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JPanel chartPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        JPanel pieSubPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        pieSubPanel.add(pieChartPanel1);
        pieSubPanel.add(pieChartPanel2);
        chartPanel.add(barChartPanel);
        chartPanel.add(pieSubPanel);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("汇总统计", new JScrollPane(resultTable));
        tabbedPane.addTab("详细结果", new JScrollPane(resultArea));
        tabbedPane.addTab("数据可视化", chartPanel);

        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(progressBar, BorderLayout.CENTER);
        mainPanel.add(tabbedPane, BorderLayout.SOUTH);

        add(mainPanel);

        resultArea.setText("代码统计工具已就绪\n");
        resultArea.append("请选择目录并点击\"开始统计\"按钮\n\n");
        resultArea.append("默认配置：统计所有支持语言，空行/注释行计入总行数\n");
        resultArea.append("支持的语言: " + String.join(", ", LANGUAGE_CONFIG.keySet()) + "\n");
    }

    class BarChartPanel extends JPanel {
        private List<String> languages = new ArrayList<>();
        private List<Integer> fileCounts = new ArrayList<>();
        private List<Integer> codeLines = new ArrayList<>();
        private int maxValue = 1;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            int marginLeft = 80;
            int marginRight = 20;
            int marginTop = 40;
            int marginBottom = 60;

            int plotWidth = width - marginLeft - marginRight;
            int plotHeight = height - marginTop - marginBottom;

            if (languages.isEmpty()) {
                g2.setFont(new Font("微软雅黑", Font.PLAIN, 16));
                g2.drawString("暂无统计数据", width / 2 - 40, height / 2);
                return;
            }

            int barGroupWidth = plotWidth / languages.size();
            int barWidth = barGroupWidth / 4;
            int barGap = barGroupWidth / 8;

            g2.setFont(new Font("微软雅黑", Font.BOLD, 18));
            g2.drawString("各语言文件数 & 代码行数对比", width / 2 - 100, 25);

            g2.setColor(Color.BLACK);
            g2.drawLine(marginLeft, marginTop, marginLeft, height - marginBottom);

            int tickCount = 5;
            g2.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            for (int i = 0; i <= tickCount; i++) {
                int y = height - marginBottom - (i * plotHeight / tickCount);
                int value = (int) (maxValue * (i / (double) tickCount));
                g2.drawLine(marginLeft - 5, y, marginLeft, y);
                g2.drawString(String.valueOf(value), marginLeft - 50, y + 5);
            }

            g2.drawLine(marginLeft, height - marginBottom, width - marginRight, height - marginBottom);

            g2.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            for (int i = 0; i < languages.size(); i++) {
                String lang = languages.get(i);
                int fileCount = fileCounts.get(i);
                int codeLine = codeLines.get(i);

                int groupX = marginLeft + i * barGroupWidth;
                int fileBarX = groupX + barGap;
                int codeBarX = fileBarX + barWidth + barGap;

                int fileBarHeight = (int) (fileCount / (double) maxValue * plotHeight);
                int codeBarHeight = (int) (codeLine / (double) maxValue * plotHeight);

                g2.setColor(COLOR_FILE_COUNT);
                Rectangle2D fileBar = new Rectangle2D.Double(
                        fileBarX, height - marginBottom - fileBarHeight,
                        barWidth, fileBarHeight);
                g2.fill(fileBar);
                g2.setColor(Color.BLACK);
                g2.draw(fileBar);

                g2.setColor(COLOR_CODE_LINE);
                Rectangle2D codeBar = new Rectangle2D.Double(
                        codeBarX, height - marginBottom - codeBarHeight,
                        barWidth, codeBarHeight);
                g2.fill(codeBar);
                g2.setColor(Color.BLACK);
                g2.draw(codeBar);

                g2.drawString(lang, groupX + barGroupWidth / 2 - 15, height - marginBottom + 20);

                if (fileBarHeight > 0) {
                    g2.drawString(String.valueOf(fileCount),
                            fileBarX + barWidth / 2 - 10,
                            height - marginBottom - fileBarHeight - 5);
                }
                if (codeBarHeight > 0) {
                    g2.drawString(String.valueOf(codeLine),
                            codeBarX + barWidth / 2 - 10,
                            height - marginBottom - codeBarHeight - 5);
                }
            }

            int legendX = width - 200;
            int legendY = marginTop;
            g2.setColor(COLOR_FILE_COUNT);
            g2.fillRect(legendX, legendY, 15, 15);
            g2.setColor(Color.BLACK);
            g2.drawRect(legendX, legendY, 15, 15);
            g2.drawString("文件数", legendX + 20, legendY + 12);

            g2.setColor(COLOR_CODE_LINE);
            g2.fillRect(legendX, legendY + 20, 15, 15);
            g2.setColor(Color.BLACK);
            g2.drawRect(legendX, legendY + 20, 15, 15);
            g2.drawString("代码行数", legendX + 20, legendY + 32);
        }

        public void updateData(List<String> langs, List<Integer> fileCounts, List<Integer> codeLines) {
            this.languages = new ArrayList<>(langs);
            this.fileCounts = new ArrayList<>(fileCounts);
            this.codeLines = new ArrayList<>(codeLines);
            this.maxValue = 1;
            for (int val : fileCounts) {
                maxValue = Math.max(maxValue, val);
            }
            for (int val : codeLines) {
                maxValue = Math.max(maxValue, val);
            }
            maxValue = (int) (Math.ceil(maxValue / 10.0) * 10);
            if (maxValue == 0)
                maxValue = 1;
            repaint();
        }

        public void clearData() {
            languages.clear();
            fileCounts.clear();
            codeLines.clear();
            repaint();
        }
    }

    class PieChartPanel extends JPanel {
        private String title;
        private List<String> labels = new ArrayList<>();
        private List<Integer> values = new ArrayList<>();
        private List<Color> colors = new ArrayList<>();
        private int verticalOffset = 0;

        public PieChartPanel(String title) {
            this.title = title;
        }

        public PieChartPanel(String title, int verticalOffset) {
            this.title = title;
            this.verticalOffset = verticalOffset;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            g2.setFont(new Font("微软雅黑", Font.BOLD, 18));
            g2.drawString(title, width / 2 - 80, 30);

            if (values.isEmpty() || sum(values) == 0) {
                g2.setFont(new Font("微软雅黑", Font.PLAIN, 16));
                g2.drawString("暂无统计数据", width / 2 - 40, height / 2);
                return;
            }

            int pieSize = Math.min(width, height) - 100;
            int pieX = (width - pieSize) / 2;
            int pieY = (height - pieSize) / 2 + 30 + verticalOffset;

            int total = sum(values);
            double startAngle = 0;
            g2.setFont(new Font("微软雅黑", Font.PLAIN, 12));

            for (int i = 0; i < values.size(); i++) {
                int value = values.get(i);
                if (value == 0)
                    continue;

                double arcAngle = (value / (double) total) * 360;
                Color color = colors.size() > i ? colors.get(i) : Color.GRAY;

                g2.setColor(color);
                Arc2D arc = new Arc2D.Double(
                        pieX, pieY, pieSize, pieSize,
                        startAngle, arcAngle, Arc2D.PIE);
                g2.fill(arc);
                g2.setColor(Color.BLACK);
                g2.draw(arc);

                double midAngle = startAngle + arcAngle / 2;
                double radian = Math.toRadians(midAngle);
                int labelRadius = pieSize / 2 + 30;
                int labelX = (int) (pieX + pieSize / 2 + Math.cos(radian) * labelRadius);
                int labelY = (int) (pieY + pieSize / 2 - Math.sin(radian) * labelRadius);

                double percent = (value / (double) total) * 100;
                String label = String.format("%s (%.1f%%)", labels.get(i), percent);
                g2.drawString(label, labelX - 30, labelY + 5);

                startAngle += arcAngle;
            }

            int legendX = 40;
            int legendY = height - 80;
            for (int i = 0; i < labels.size(); i++) {
                if (values.get(i) == 0)
                    continue;

                Color color = colors.size() > i ? colors.get(i) : Color.GRAY;
                g2.setColor(color);
                g2.fillRect(legendX, legendY + i * 20, 15, 15);
                g2.setColor(Color.BLACK);
                g2.drawRect(legendX, legendY + i * 20, 15, 15);
                g2.drawString(labels.get(i), legendX + 20, legendY + i * 20 + 12);
            }
        }

        private int sum(List<Integer> list) {
            int total = 0;
            for (int val : list) {
                total += val;
            }
            return total;
        }

        public void updateData(List<String> labels, List<Integer> values, List<Color> colors) {
            this.labels = new ArrayList<>(labels);
            this.values = new ArrayList<>(values);
            this.colors = new ArrayList<>(colors);
            repaint();
        }

        public void clearData() {
            labels.clear();
            values.clear();
            colors.clear();
            repaint();
        }
    }

    private void updateCharts(Map<String, LanguageStats> statsMap, int totalCode, int totalBlank, int totalComment) {
        List<String> barLangs = new ArrayList<>();
        List<Integer> barFileCounts = new ArrayList<>();
        List<Integer> barCodeLines = new ArrayList<>();
        for (Map.Entry<String, LanguageStats> entry : statsMap.entrySet()) {
            String lang = entry.getKey();
            LanguageStats stats = entry.getValue();
            if (stats.fileCount > 0) {
                barLangs.add(lang);
                barFileCounts.add(stats.fileCount);
                barCodeLines.add(stats.codeLines);
            }
        }
        barChartPanel.updateData(barLangs, barFileCounts, barCodeLines);

        List<String> pie1Labels = Arrays.asList("代码行", "空行", "注释行");
        List<Integer> pie1Values = Arrays.asList(totalCode, totalBlank, totalComment);
        List<Color> pie1Colors = Arrays.asList(PIE_COLORS[0], PIE_COLORS[1], PIE_COLORS[2]);
        pieChartPanel1.updateData(pie1Labels, pie1Values, pie1Colors);

        List<String> pie2Labels = new ArrayList<>();
        List<Integer> pie2Values = new ArrayList<>();
        List<Color> pie2Colors = new ArrayList<>();
        int colorIndex = 0;
        for (Map.Entry<String, LanguageStats> entry : statsMap.entrySet()) {
            String lang = entry.getKey();
            int count = entry.getValue().fileCount;
            if (count > 0) {
                pie2Labels.add(lang);
                pie2Values.add(count);
                pie2Colors.add(LANG_PIE_COLORS[colorIndex % LANG_PIE_COLORS.length]);
                colorIndex++;
            }
        }
        pieChartPanel2.updateData(pie2Labels, pie2Values, pie2Colors);
    }

    private void clearCharts() {
        barChartPanel.clearData();
        pieChartPanel1.clearData();
        pieChartPanel2.clearData();
    }

    private void showConfigDialog() {
        JDialog configDialog = new JDialog(this, "统计配置", true);
        configDialog.setSize(400, 350);
        configDialog.setLocationRelativeTo(this);
        configDialog.setLayout(new BorderLayout(10, 10));
        configDialog.setResizable(false);

        JPanel contentPanel = new JPanel();
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JPanel languagePanel = new JPanel();
        languagePanel.setBorder(BorderFactory.createTitledBorder("选择要统计的语言"));
        languagePanel.setLayout(new GridLayout(0, 2, 10, 5));

        Map<String, JCheckBox> langCheckBoxes = new HashMap<>();
        for (String lang : LANGUAGE_CONFIG.keySet()) {
            JCheckBox checkBox = new JCheckBox(lang);
            checkBox.setSelected(selectedLanguages.contains(lang));
            langCheckBoxes.put(lang, checkBox);
            languagePanel.add(checkBox);
        }

        JPanel langButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton selectAllBtn = new JButton("全选");
        JButton deselectAllBtn = new JButton("取消全选");
        selectAllBtn.addActionListener(e -> langCheckBoxes.values().forEach(cb -> cb.setSelected(true)));
        deselectAllBtn.addActionListener(e -> langCheckBoxes.values().forEach(cb -> cb.setSelected(false)));
        langButtonPanel.add(selectAllBtn);
        langButtonPanel.add(deselectAllBtn);

        JPanel totalLinePanel = new JPanel();
        totalLinePanel.setBorder(BorderFactory.createTitledBorder("总行数计算规则"));
        totalLinePanel.setLayout(new GridLayout(2, 1, 5, 5));

        JCheckBox blankInTotalCb = new JCheckBox("空行计入总行数", countBlankInTotal);
        JCheckBox commentInTotalCb = new JCheckBox("注释行计入总行数", countCommentInTotal);
        totalLinePanel.add(blankInTotalCb);
        totalLinePanel.add(commentInTotalCb);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton confirmBtn = new JButton("确认");
        JButton cancelBtn = new JButton("取消");

        confirmBtn.addActionListener(e -> {
            selectedLanguages.clear();
            for (Map.Entry<String, JCheckBox> entry : langCheckBoxes.entrySet()) {
                if (entry.getValue().isSelected()) {
                    selectedLanguages.add(entry.getKey());
                }
            }
            if (selectedLanguages.isEmpty()) {
                JOptionPane.showMessageDialog(configDialog, "请至少选择一种统计语言！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            countBlankInTotal = blankInTotalCb.isSelected();
            countCommentInTotal = commentInTotalCb.isSelected();
            configDialog.dispose();

            resultArea.append("\n=== 配置已更新 ===\n");
            resultArea.append("选中的统计语言: " + String.join(", ", selectedLanguages) + "\n");
            resultArea.append("空行计入总行数: " + (countBlankInTotal ? "是" : "否") + "\n");
            resultArea.append("注释行计入总行数: " + (countCommentInTotal ? "是" : "否") + "\n\n");
        });

        cancelBtn.addActionListener(e -> configDialog.dispose());

        buttonPanel.add(confirmBtn);
        buttonPanel.add(cancelBtn);

        contentPanel.add(languagePanel);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(langButtonPanel);
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(totalLinePanel);
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(buttonPanel);

        configDialog.add(contentPanel, BorderLayout.CENTER);
        configDialog.setVisible(true);
    }

    private void browseDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            directoryField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void startStatistics() {
        String dirPath = directoryField.getText().trim();
        File testDir = dirPath.isEmpty() ? new File("D:\\testcase5") : new File(dirPath);

        if (!testDir.exists() || !testDir.isDirectory()) {
            JOptionPane.showMessageDialog(this, "目录不存在或不是有效目录！", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (selectedLanguages.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先在统计配置中选择至少一种语言！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ((DefaultTableModel) resultTable.getModel()).setRowCount(0);
        resultArea.setText("");
        clearCharts();

        statsButton.setEnabled(false);

        new StatisticsWorker(testDir).execute();
    }

    private class StatisticsWorker extends SwingWorker<Map<String, LanguageStats>, String> {
        private final File rootDir;

        public StatisticsWorker(File rootDir) {
            this.rootDir = rootDir;
        }

        @Override
        protected Map<String, LanguageStats> doInBackground() throws Exception {
            publish("开始统计代码量...\n");
            publish("扫描目录: " + rootDir.getAbsolutePath() + "\n");
            publish("统计配置：\n");
            publish("  选中语言: " + String.join(", ", selectedLanguages) + "\n");
            publish("  空行计入总行数: " + (countBlankInTotal ? "是" : "否") + "\n");
            publish("  注释行计入总行数: " + (countCommentInTotal ? "是" : "否") + "\n");

            Map<String, LanguageStats> result = new HashMap<>();
            for (String lang : selectedLanguages) {
                result.put(lang, new LanguageStats());
            }

            publish("\n选中语言的文件类型:\n");
            for (String lang : selectedLanguages) {
                publish("  " + lang + ": " + String.join(", ", LANGUAGE_CONFIG.get(lang)) + "\n");
            }
            publish("\n");

            progressBar.setVisible(true);
            progressBar.setIndeterminate(true);

            scanDirectory(rootDir, result, rootDir);

            return result;
        }

        @Override
        protected void process(List<String> chunks) {
            for (String chunk : chunks) {
                resultArea.append(chunk);
                resultArea.setCaretPosition(resultArea.getDocument().getLength());
            }
        }

        @Override
        protected void done() {
            progressBar.setVisible(false);
            progressBar.setIndeterminate(false);
            statsButton.setEnabled(true);

            try {
                Map<String, LanguageStats> result = get();
                if (result != null) {
                    displayResults(result);
                } else {
                    resultArea.append("统计结果为空\n");
                }
            } catch (Exception e) {
                resultArea.append("统计过程中出现错误: " + e.getMessage() + "\n");
                e.printStackTrace();
            }
        }

        private void scanDirectory(File directory, Map<String, LanguageStats> result, File baseDir) {
            File[] files = directory.listFiles();
            if (files == null) {
                publish("无法访问目录: " + directory.getAbsolutePath() + "\n");
                return;
            }

            for (File file : files) {
                if (isCancelled())
                    return;

                if (file.isDirectory()) {
                    if (!SKIP_DIRS.contains(file.getName())) {
                        scanDirectory(file, result, baseDir);
                    }
                } else {
                    String language = getLanguage(file);
                    if (language != null && selectedLanguages.contains(language)) {
                        FileStats stats = analyzeFile(file, language);
                        if (stats != null) {
                            result.get(language).addFileStats(stats, getRelativePath(file, baseDir));
                            publish("已统计: " + getRelativePath(file, baseDir) + " (" + stats + ")\n");
                        }
                    }
                }
            }
        }

        private String getLanguage(File file) {
            String fileName = file.getName().toLowerCase();
            for (Map.Entry<String, String[]> entry : LANGUAGE_CONFIG.entrySet()) {
                for (String ext : entry.getValue()) {
                    if (fileName.endsWith(ext.toLowerCase())) {
                        return entry.getKey();
                    }
                }
            }
            return null;
        }
    }

    private void displayResults(Map<String, LanguageStats> result) {
        DefaultTableModel model = (DefaultTableModel) resultTable.getModel();
        model.setRowCount(0);

        int totalFiles = 0, totalLines = 0, totalCodeLines = 0,
                totalBlankLines = 0, totalCommentLines = 0, totalFunctions = 0;
        List<Integer> allCodeLines = new ArrayList<>();

        resultArea.append("\n=== 代码统计结果汇总 ===\n");

        boolean hasData = false;
        for (Map.Entry<String, LanguageStats> entry : result.entrySet()) {
            String language = entry.getKey();
            LanguageStats stats = entry.getValue();

            if (stats.fileCount == 0) {
                continue;
            }
            hasData = true;

            Map<String, Double> statistics = calculateStatistics(stats.codeLinesList);

            model.addRow(new Object[] {
                    language, stats.fileCount, stats.totalLines, stats.codeLines,
                    stats.blankLines, stats.commentLines, stats.functionCount,
                    statistics.get("max"), statistics.get("min"),
                    statistics.get("mean"), statistics.get("median")
            });

            totalFiles += stats.fileCount;
            totalLines += stats.totalLines;
            totalCodeLines += stats.codeLines;
            totalBlankLines += stats.blankLines;
            totalCommentLines += stats.commentLines;
            totalFunctions += stats.functionCount;
            allCodeLines.addAll(stats.codeLinesList);

            resultArea.append(String.format("\n%s 语言统计:\n", language));
            resultArea.append(String.format("文件数量: %d\n", stats.fileCount));
            resultArea.append(String.format("总行数: %d, 代码行: %d, 空行: %d, 注释行: %d, 函数数: %d\n",
                    stats.totalLines, stats.codeLines, stats.blankLines, stats.commentLines, stats.functionCount));
            resultArea.append("代码行数统计:\n");
            resultArea.append(String.format("  最大值: %.0f\n", statistics.get("max")));
            resultArea.append(String.format("  最小值: %.0f\n", statistics.get("min")));
            resultArea.append(String.format("  平均值: %.2f\n", statistics.get("mean")));
            resultArea.append(String.format("  中位数: %.2f\n", statistics.get("median")));

            if (stats.totalLines > 0) {
                double codePercent = (stats.codeLines * 100.0) / stats.totalLines;
                double blankPercent = (stats.blankLines * 100.0) / stats.totalLines;
                double commentPercent = (stats.commentLines * 100.0) / stats.totalLines;
                resultArea.append(String.format("代码比例: %.1f%%, 空行比例: %.1f%%, 注释比例: %.1f%%\n",
                        codePercent, blankPercent, commentPercent));
            }
        }

        if (!hasData) {
            resultArea.append("\n未找到任何选中语言的代码文件！\n");
            resultArea.append("请检查目录路径和选中的语言类型\n");
            clearCharts();
            return;
        }

        Map<String, Double> overallStatistics = calculateStatistics(allCodeLines);

        model.addRow(new Object[] {
                "总计", totalFiles, totalLines, totalCodeLines,
                totalBlankLines, totalCommentLines, totalFunctions,
                overallStatistics.get("max"), overallStatistics.get("min"),
                overallStatistics.get("mean"), overallStatistics.get("median")
        });

        resultArea.append("\n=== 总体统计 ===\n");
        resultArea.append(String.format("总文件数: %d\n", totalFiles));
        resultArea.append(String.format("总行数: %d, 代码行: %d, 空行: %d, 注释行: %d, 函数数: %d\n",
                totalLines, totalCodeLines, totalBlankLines, totalCommentLines, totalFunctions));
        resultArea.append("总体代码行数统计:\n");
        resultArea.append(String.format("  最大值: %.0f\n", overallStatistics.get("max")));
        resultArea.append(String.format("  最小值: %.0f\n", overallStatistics.get("min")));
        resultArea.append(String.format("  平均值: %.2f\n", overallStatistics.get("mean")));
        resultArea.append(String.format("  中位数: %.2f\n", overallStatistics.get("median")));

        if (totalLines > 0) {
            double codePercent = (totalCodeLines * 100.0) / totalLines;
            double blankPercent = (totalBlankLines * 100.0) / totalLines;
            double commentPercent = (totalCommentLines * 100.0) / totalLines;
            resultArea.append(String.format("总体比例 - 代码: %.1f%%, 空行: %.1f%%, 注释: %.1f%%\n",
                    codePercent, blankPercent, commentPercent));
        }

        updateCharts(result, totalCodeLines, totalBlankLines, totalCommentLines);

        model.fireTableDataChanged();
        resultTable.repaint();
    }

    public static Map<String, Double> calculateStatistics(List<Integer> values) {
        Map<String, Double> stats = new HashMap<>();

        if (values.isEmpty()) {
            stats.put("mean", 0.0);
            stats.put("max", 0.0);
            stats.put("min", 0.0);
            stats.put("median", 0.0);
            return stats;
        }

        Collections.sort(values);

        double sum = 0;
        for (int value : values) {
            sum += value;
        }
        double mean = sum / values.size();

        int max = values.get(values.size() - 1);
        int min = values.get(0);

        double median;
        int middle = values.size() / 2;
        if (values.size() % 2 == 0) {
            median = (values.get(middle - 1) + values.get(middle)) / 2.0;
        } else {
            median = values.get(middle);
        }

        stats.put("mean", Math.round(mean * 100.0) / 100.0);
        stats.put("max", (double) max);
        stats.put("min", (double) min);
        stats.put("median", Math.round(median * 100.0) / 100.0);

        return stats;
    }

    static class FileStats {
        int totalLines, codeLines, blankLines, commentLines, functionCount;

        FileStats() {
            this.totalLines = this.codeLines = this.blankLines = this.commentLines = this.functionCount = 0;
        }

        @Override
        public String toString() {
            return String.format("总行:%d, 代码:%d, 空行:%d, 注释:%d, 函数:%d",
                    totalLines, codeLines, blankLines, commentLines, functionCount);
        }
    }

    static class LanguageStats {
        int fileCount, totalLines, codeLines, blankLines, commentLines, functionCount;
        List<Integer> codeLinesList;

        LanguageStats() {
            this.fileCount = 0;
            this.totalLines = 0;
            this.codeLines = 0;
            this.blankLines = 0;
            this.commentLines = 0;
            this.functionCount = 0;
            this.codeLinesList = new ArrayList<>();
        }

        void addFileStats(FileStats stats, String filePath) {
            fileCount++;
            totalLines += stats.totalLines;
            codeLines += stats.codeLines;
            blankLines += stats.blankLines;
            commentLines += stats.commentLines;
            functionCount += stats.functionCount;
            codeLinesList.add(stats.codeLines);
        }
    }

    private FileStats analyzeFile(File file, String language) {
        FileStats stats = new FileStats();
        boolean inBlockComment = false;
        boolean inPythonMultiLineString = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();
                int lineContribute = 0;

                if (trimmedLine.isEmpty()) {
                    stats.blankLines++;
                    if (countBlankInTotal) {
                        lineContribute = 1;
                    }
                } else if (language.equals("Python")) {
                    if (trimmedLine.startsWith("#")) {
                        stats.commentLines++;
                        if (countCommentInTotal) {
                            lineContribute = 1;
                        }
                    } else if ((trimmedLine.startsWith("\"\"\"") || trimmedLine.startsWith("'''")) &&
                            (trimmedLine.endsWith("\"\"\"") || trimmedLine.endsWith("'''"))
                            && trimmedLine.length() > 3) {
                        stats.commentLines++;
                        if (countCommentInTotal) {
                            lineContribute = 1;
                        }
                    } else if (inPythonMultiLineString) {
                        stats.commentLines++;
                        if (countCommentInTotal) {
                            lineContribute = 1;
                        }
                        if (trimmedLine.contains("\"\"\"") || trimmedLine.contains("'''")) {
                            inPythonMultiLineString = false;
                        }
                    } else if (trimmedLine.startsWith("\"\"\"") || trimmedLine.startsWith("'''")) {
                        stats.commentLines++;
                        if (countCommentInTotal) {
                            lineContribute = 1;
                        }
                        inPythonMultiLineString = true;
                    } else {
                        if (isFunctionLine(line, language)) {
                            stats.functionCount++;
                        }
                        stats.codeLines++;
                        lineContribute = 1;
                    }
                } else {
                    if (inBlockComment) {
                        stats.commentLines++;
                        if (countCommentInTotal) {
                            lineContribute = 1;
                        }
                        if (trimmedLine.contains("*/")) {
                            inBlockComment = false;
                        }
                    } else if (trimmedLine.startsWith("/*")) {
                        stats.commentLines++;
                        if (countCommentInTotal) {
                            lineContribute = 1;
                        }
                        inBlockComment = !trimmedLine.contains("*/");
                    } else if (trimmedLine.startsWith("//")) {
                        stats.commentLines++;
                        if (countCommentInTotal) {
                            lineContribute = 1;
                        }
                    } else {
                        if (isFunctionLine(line, language)) {
                            stats.functionCount++;
                        }
                        stats.codeLines++;
                        lineContribute = 1;
                    }
                }

                stats.totalLines += lineContribute;
            }
        } catch (IOException e) {
            System.err.println("读取文件失败: " + file.getAbsolutePath());
            return null;
        }

        return stats;
    }

    private boolean isFunctionLine(String line, String language) {
        line = line.trim();
        switch (language) {
            case "Java":
            case "C#":
                return line.matches(".*(public|private|protected|static)?\\s+\\w+\\s+\\w+\\s*\\(.*\\).*\\{?\\s*");
            case "C":
            case "C++":
                return line.matches("^\\s*\\w+\\s+\\w+\\s*\\([^)]*\\).*\\{?\\s*$");
            case "Python":
                return line.matches("^\\s*def\\s+\\w+\\s*\\([^)]*\\).*:");
            default:
                return false;
        }
    }

    private String getRelativePath(File file, File baseDir) {
        try {
            String basePath = baseDir.getCanonicalPath();
            String filePath = file.getCanonicalPath();
            return filePath.startsWith(basePath) ? filePath.substring(basePath.length() + 1) : filePath;
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                try {
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            CodeStatisticsUI ui = new CodeStatisticsUI();
            ui.setVisible(true);
        });
    }
}
