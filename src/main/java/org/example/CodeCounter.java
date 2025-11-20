package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;

class CodeStatisticsUI extends JFrame {
    private JTextField directoryField;
    private JTextArea resultArea;
    private JTable resultTable;
    private JProgressBar progressBar;
    private JButton statsButton;

    // 语言配置
    private static final Map<String, String[]> LANGUAGE_CONFIG = new HashMap<>();
    static {
        LANGUAGE_CONFIG.put("Java", new String[] { ".java" });
        LANGUAGE_CONFIG.put("C", new String[] { ".c", ".h" });
        LANGUAGE_CONFIG.put("C++", new String[] { ".cpp", ".cc", ".hpp" });
        LANGUAGE_CONFIG.put("Python", new String[] { ".py" });
        LANGUAGE_CONFIG.put("C#", new String[] { ".cs" });
    }

    // 跳过目录
    private static final Set<String> SKIP_DIRS = new HashSet<>(Arrays.asList(
            ".git", ".idea", "target", "build", "out", "node_modules", "__pycache__"));

    // 新增：对外静态调用返回的计数结果模型
    public static class CountResult {
        public int fileCount;
        public int totalLines;
        public int blankLines;
        public int commentLines;
        public int codeLines;

        public CountResult() {
        }
    }

    // 新增：静态方法，按 extensions 列表统计目录（递归）
    public static CountResult countCodeLines(File rootDir, String[] extensions, boolean countEmpty,
            boolean countComment) throws IOException {
        CountResult result = new CountResult();
        if (rootDir == null || !rootDir.exists())
            return result;
        List<String> extList = new ArrayList<>();
        if (extensions != null) {
            for (String e : extensions) {
                if (e != null && !e.trim().isEmpty())
                    extList.add(e.toLowerCase());
            }
        }
        countRecursive(rootDir, extList, result);
        // 如果不统计空行或注释行，则将这些计数视为 0（上层逻辑可能依赖）
        if (!countEmpty)
            result.blankLines = 0;
        if (!countComment)
            result.commentLines = 0;
        // codeLines = totalLines - blank - comment （保底）
        result.codeLines = Math.max(0, result.totalLines - result.blankLines - result.commentLines);
        return result;
    }

    // 递归扫描计算
    private static void countRecursive(File dir, List<String> extensions, CountResult result) throws IOException {
        File[] files = dir.listFiles();
        if (files == null)
            return;
        for (File f : files) {
            if (f.isDirectory()) {
                String name = f.getName();
                if (name.equals(".git") || name.equals(".idea") || name.equals("target") || name.equals("build")
                        || name.equals("out") || name.equals("node_modules") || name.equals("__pycache__")) {
                    continue;
                }
                countRecursive(f, extensions, result);
            } else {
                String fname = f.getName().toLowerCase();
                boolean match = extensions.isEmpty();
                for (String ext : extensions) {
                    if (fname.endsWith(ext)) {
                        match = true;
                        break;
                    }
                }
                if (!match)
                    continue;
                // 分析文件
                analyzeFileStatic(f, result);
                result.fileCount++;
            }
        }
    }

    // 简化复用的文件分析逻辑（统计 total/blank/comment，采用 C 风格与 Python 简单处理）
    private static void analyzeFileStatic(File file, CountResult result) {
        boolean inBlockComment = false;
        boolean inPythonMulti = false;
        String name = file.getName().toLowerCase();
        boolean isPython = name.endsWith(".py");
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.totalLines++;
                String t = line.trim();
                if (t.isEmpty()) {
                    result.blankLines++;
                    continue;
                }
                if (isPython) {
                    if (t.startsWith("#")) {
                        result.commentLines++;
                        continue;
                    }
                    if ((t.startsWith("\"\"\"") || t.startsWith("'''"))) {
                        // 判断单行三引号注释
                        if ((t.endsWith("\"\"\"") || t.endsWith("'''")) && t.length() > 3) {
                            result.commentLines++;
                            continue;
                        } else {
                            result.commentLines++;
                            inPythonMulti = !inPythonMulti;
                            continue;
                        }
                    }
                    if (inPythonMulti) {
                        result.commentLines++;
                        if (t.contains("\"\"\"") || t.contains("'''"))
                            inPythonMulti = false;
                        continue;
                    }
                } else {
                    if (inBlockComment) {
                        result.commentLines++;
                        if (t.contains("*/"))
                            inBlockComment = false;
                        continue;
                    }
                    if (t.startsWith("/*")) {
                        result.commentLines++;
                        if (!t.contains("*/"))
                            inBlockComment = true;
                        continue;
                    }
                    if (t.startsWith("//")) {
                        result.commentLines++;
                        continue;
                    }
                }
                // 其余视为代码行，具体的 codeLines 由调用端按需计算
            }
        } catch (Exception ignored) {
            // 读取失败不抛出，跳过该文件

        }
    }

    public CodeStatisticsUI() {
        initUI();
    }

    private void initUI() {
        setTitle("代码统计工具");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 顶部控制面板
        JPanel controlPanel = new JPanel(new BorderLayout(5, 5));
        directoryField = new JTextField("D:\\testcase5");
        JButton browseButton = new JButton("浏览...");
        statsButton = new JButton("开始统计");

        browseButton.addActionListener(e -> browseDirectory());
        statsButton.addActionListener(e -> startStatistics());

        JPanel pathPanel = new JPanel(new BorderLayout(5, 5));
        pathPanel.add(new JLabel("目录路径:"), BorderLayout.WEST);
        pathPanel.add(directoryField, BorderLayout.CENTER);
        pathPanel.add(browseButton, BorderLayout.EAST);

        controlPanel.add(pathPanel, BorderLayout.CENTER);
        controlPanel.add(statsButton, BorderLayout.EAST);

        // 进度条
        progressBar = new JProgressBar();
        progressBar.setVisible(false);

        // 结果表格 - 增加统计信息列
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

        // 详细结果区域
        resultArea = new JTextArea(20, 80);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // 标签面板
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("汇总统计", new JScrollPane(resultTable));
        tabbedPane.addTab("详细结果", new JScrollPane(resultArea));

        // 布局组装
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(progressBar, BorderLayout.CENTER);
        mainPanel.add(tabbedPane, BorderLayout.SOUTH);

        add(mainPanel);

        // 初始显示一些信息
        resultArea.setText("代码统计工具已就绪\n");
        resultArea.append("请选择目录并点击\"开始统计\"按钮\n\n");
        resultArea.append("支持的语言: " + String.join(", ", LANGUAGE_CONFIG.keySet()) + "\n");
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

        // 清空之前的结果
        ((DefaultTableModel) resultTable.getModel()).setRowCount(0);
        resultArea.setText("");

        // 禁用按钮防止重复点击
        statsButton.setEnabled(false);

        // 在新线程中执行统计任务
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

            Map<String, LanguageStats> result = new HashMap<>();
            for (String lang : LANGUAGE_CONFIG.keySet()) {
                result.put(lang, new LanguageStats());
            }

            // 显示支持的文件类型
            publish("支持的文件类型:\n");
            for (Map.Entry<String, String[]> entry : LANGUAGE_CONFIG.entrySet()) {
                publish("  " + entry.getKey() + ": " + String.join(", ", entry.getValue()) + "\n");
            }
            publish("\n");

            progressBar.setVisible(true);
            progressBar.setIndeterminate(true); // 使用不确定进度条

            scanDirectory(rootDir, result, rootDir);
            return result;
        }

        @Override
        protected void process(List<String> chunks) {
            for (String chunk : chunks) {
                resultArea.append(chunk);
                // 滚动到底部
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
                    if (language != null) {
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
        // 更新表格
        DefaultTableModel model = (DefaultTableModel) resultTable.getModel();
        model.setRowCount(0);

        int totalFiles = 0, totalLines = 0, totalCodeLines = 0,
                totalBlankLines = 0, totalCommentLines = 0, totalFunctions = 0;

        // 收集所有代码行数用于总体统计
        List<Integer> allCodeLines = new ArrayList<>();

        resultArea.append("\n=== 代码统计结果汇总 ===\n");

        boolean hasData = false;
        for (Map.Entry<String, LanguageStats> entry : result.entrySet()) {
            String language = entry.getKey();
            LanguageStats stats = entry.getValue();

            if (stats.fileCount == 0) {
                continue; // 跳过没有文件的语言
            }

            hasData = true;

            // 计算统计信息
            Map<String, Double> statistics = calculateStatistics(stats.codeLinesList);

            // 添加到表格
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

            // 添加到总体统计列表
            allCodeLines.addAll(stats.codeLinesList);

            resultArea.append(String.format("\n%s 语言统计:\n", language));
            resultArea.append(String.format("文件数量: %d\n", stats.fileCount));
            resultArea.append(String.format("总行数: %d, 代码行: %d, 空行: %d, 注释行: %d, 函数数: %d\n",
                    stats.totalLines, stats.codeLines, stats.blankLines, stats.commentLines, stats.functionCount));

            // 显示统计信息
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
            resultArea.append("\n未找到任何代码文件！\n");
            resultArea.append("请检查目录路径和文件类型\n");
            return;
        }

        // 计算总体统计信息
        Map<String, Double> overallStatistics = calculateStatistics(allCodeLines);

        // 添加总计行
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

        // 显示总体统计信息
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

        // 刷新表格
        model.fireTableDataChanged();
        resultTable.repaint();
    }

    // 计算统计值：均值、最大值、最小值、中位数
    public static Map<String, Double> calculateStatistics(List<Integer> values) {
        Map<String, Double> stats = new HashMap<>();

        if (values.isEmpty()) {
            stats.put("mean", 0.0);
            stats.put("max", 0.0);
            stats.put("min", 0.0);
            stats.put("median", 0.0);
            return stats;
        }

        // 排序以便计算中位数
        Collections.sort(values);

        // 计算平均值
        double sum = 0;
        for (int value : values) {
            sum += value;
        }
        double mean = sum / values.size();

        // 最大值和最小值
        int max = values.get(values.size() - 1);
        int min = values.get(0);

        // 中位数
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

    // 简化的文件统计类
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

    // 语言统计类 - 增加代码行数列表用于统计计算
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
            codeLinesList.add(stats.codeLines); // 记录每个文件的代码行数
        }
    }

    // 分析单个文件
    private FileStats analyzeFile(File file, String language) {
        FileStats stats = new FileStats();
        boolean inBlockComment = false;
        boolean inPythonMultiLineString = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stats.totalLines++;
                String trimmedLine = line.trim();

                if (trimmedLine.isEmpty()) {
                    stats.blankLines++;
                    continue;
                }

                // 注释处理
                if (language.equals("Python")) {
                    if (trimmedLine.startsWith("#")) {
                        stats.commentLines++;
                        continue;
                    }
                    if ((trimmedLine.startsWith("\"\"\"") || trimmedLine.startsWith("'''")) &&
                            (trimmedLine.endsWith("\"\"\"") || trimmedLine.endsWith("'''"))
                            && trimmedLine.length() > 3) {
                        stats.commentLines++;
                        continue;
                    }
                    if (inPythonMultiLineString) {
                        stats.commentLines++;
                        if (trimmedLine.contains("\"\"\"") || trimmedLine.contains("'''")) {
                            inPythonMultiLineString = false;
                        }
                        continue;
                    }
                    if (trimmedLine.startsWith("\"\"\"") || trimmedLine.startsWith("'''")) {
                        stats.commentLines++;
                        inPythonMultiLineString = true;
                        continue;
                    }
                } else {
                    // C风格语言注释处理
                    if (inBlockComment) {
                        stats.commentLines++;
                        if (trimmedLine.contains("*/")) {
                            inBlockComment = false;
                        }
                        continue;
                    }

                    if (trimmedLine.startsWith("/*")) {
                        stats.commentLines++;
                        inBlockComment = !trimmedLine.contains("*/");
                        continue;
                    }

                    if (trimmedLine.startsWith("//")) {
                        stats.commentLines++;
                        continue;
                    }
                }

                // 函数计数（简化版）
                if (isFunctionLine(line, language)) {
                    stats.functionCount++;
                }

                stats.codeLines++;
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
                // 设置系统外观
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
