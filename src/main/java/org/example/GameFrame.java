package org.example;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFRow;

public class GameFrame extends Frame {
    // 定义duck行为组
    private DuckBark.Duck duck;
    private final DuckBark.DuckAction getPointAction = new DuckBark.GetPointAction();

    int totalScore = 0;
    long startTimeMillis = 0L;
    // 游戏资源
    Image bg = loadImage("img/bg.png");
    Image tank = loadImage("img/tank.png");
    Image clickAreaImg = loadImage("img/click_area.png");

    // 衣服图片资源（按季节-时间-天气分类）
    Image initialClothesImg = loadImage("img/clothes/initial_clothes.png");
    Image summerDaySunny = loadImage("img/clothes/summer_day_sunny.png");
    Image summerDayRainy = loadImage("img/clothes/summer_day_rainy.png");
    Image summerNightSunny = loadImage("img/clothes/summer_night_sunny.png");
    Image summerNightRainy = loadImage("img/clothes/summer_night_rainy.png");
    Image winterDaySunny = loadImage("img/clothes/winter_day_sunny.png");
    Image winterDayRainy = loadImage("img/clothes/winter_day_rainy.png");
    Image winterNightSunny = loadImage("img/clothes/winter_night_sunny.png");
    Image winterNightRainy = loadImage("img/clothes/winter_night_rainy.png");

    Image currentClothesImg = initialClothesImg;

    private final Map<String, Map<String, Map<String, Image>>> clothesImgConfig = new HashMap<>();
    // 坦克状态
    boolean left = false;
    boolean right = false;
    boolean up = false;
    boolean down = false;
    boolean isGameStarted = false;
    int tankX = 300; // 坦克x坐标
    int tankY = 300; // 坦克y坐标
    int tankWidth = 100; // 坦克宽度
    int tankHeight = 50; // 坦克高度

    // 子弹管理
    List<Bullet> bulletList = new ArrayList<>();
    Random random = new Random(); // 用于生成随机值
    int bulletSpawnRate = 10; // 子弹生成概率（数值越大生成越慢）

    // 衣服颜色配置（wear对话框使用）
    // private final Map<String, Map<String, Map<String, Color>>> clothesColorConfig
    // = new HashMap<>();

    public static void main(String[] args) {
        GameFrame frame = new GameFrame();

        // Duck行为
        frame.duck = new DuckBark.Duck();
        frame.duck.setBehaveStrategy(new DuckBark.Behave_GetPoint());
        frame.duck.setSoundStrategy(new DuckBark.Sound_GetPoint());

        frame.InitialFrame();
    }

    // 初始化窗口
    public void InitialFrame() {
        setTitle("TankF");
        setSize(800, 600);
        setLocationRelativeTo(null); // 居中显示
        setResizable(false); // 固定窗口大小
        startTimeMillis = System.currentTimeMillis();
        // 初始化衣服颜色配置
        initClothesImgConfig();

        // 启动绘制线程
        new PaintThread().start();
        // 监听键盘
        addKeyListener(new KeyMonitor());
        // 监听窗口关闭
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                if (clickableArea.contains(x, y)) {
                    showInputDialog();
                }
            }
        });

        setVisible(true);
    }

    private void showInputDialog() {
        JDialog inputDialog = new JDialog(this, "Input cmd", true);
        inputDialog.setSize(300, 200); // 增大窗口高度以容纳提示区域
        inputDialog.setLocationRelativeTo(null);
        inputDialog.setLayout(new BorderLayout(10, 10)); // 设置组件间距

        // 提示文本区域（不可编辑）
        JTextArea tipArea = new JTextArea();
        tipArea.setEditable(false);
        tipArea.setLineWrap(true); // 自动换行
        tipArea.setWrapStyleWord(true); // 按单词换行
        tipArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        tipArea.setForeground(new Color(66, 66, 66));
        // 设置提示内容（列出可用命令）
        tipArea.setText("可用命令：\n" +
                "start - 开始游戏\n" +
                "wear - 更换睡衣颜色\n" +
                "count - 统计代码行数\n" +
                "talk - 与AI对话");
        // 添加滚动条（防止文本过多时溢出）
        JScrollPane scrollPane = new JScrollPane(tipArea);
        scrollPane.setPreferredSize(new Dimension(280, 80)); // 固定提示区域高度
        inputDialog.add(scrollPane, BorderLayout.NORTH);

        // 输入区域
        TextField inputField = new TextField();
        inputField.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        inputDialog.add(inputField, BorderLayout.CENTER);

        // 确认按钮
        JButton submitButton = new JButton("Submit");
        submitButton.setForeground(Color.BLACK);
        submitButton.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        submitButton.addActionListener(e -> {
            String command = inputField.getText().trim();
            handleInput(command);
            inputDialog.dispose();
        });
        // 设置默认按钮（按Enter直接触发）
        inputDialog.getRootPane().setDefaultButton(submitButton);
        inputDialog.add(submitButton, BorderLayout.SOUTH);

        // 关闭事件
        inputField.addActionListener(e -> submitButton.doClick());
        inputDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                inputDialog.dispose();
            }
        });

        inputDialog.setVisible(true);
    }

    private void handleInput(String command) {
        if (command.isEmpty()) {
            return;
        }
        switch (command) {
            case "start":
                // 启动游戏：设置状态为true，重置时间和分数
                isGameStarted = true;
                startTimeMillis = System.currentTimeMillis(); // 重置计时
                totalScore = 0; // 重置分数
                break;
            case "wear":
                showClothesDialog(); // 打开衣服颜色配置对话框
                break;
            case "count":
                showCountDialog(); // 打开代码行数统计对话框
                break;
            case "talk":
                showTipDialog("Input talk command");
                // todo 用户与ai对话
                showTalkDialog();
                break;
            default:
                showTipDialog("Invalid command");
                break;
        }
    }

    // ----------- 对话功能 -----------
    private void showTalkDialog() {
        // JDialog dialog = new JDialog(this, "Talk with Bot", true);
        // dialog.setSize(300, 500);
        // dialog.setLocationRelativeTo(null);
        // // 历史内容
        // JTextArea textArea = new JTextArea();
        // textArea.setLineWrap(true);
        // textArea.setWrapStyleWord(true);
        // textArea.setEditable(false);
        // textArea.setPreferredSize(new Dimension(300, 400));

        // dialog.add(textArea, BorderLayout.NORTH);
        // // 输入框
        // JTextField inputField = new JTextField();
        // inputField.setPreferredSize(new Dimension(300, 50));
        // dialog.add(inputField);
        // // 发送按钮
        // JButton sendButton = new JButton("Send");
        // sendButton.addActionListener(e -> {
        // String user_talk = inputField.getText().trim();
        // if (!user_talk.isEmpty()) {
        // System.out.println("user_talk:" + user_talk);
        // String history = textArea.getText();
        // textArea.setText(history + "用户：" + user_talk + "\n");
        // inputField.setText("");
        // textArea.setCaretPosition(textArea.getDocument().getLength());
        // try {
        // // String aiReply = getAiReply(userTalk);
        // // textArea.setText(textArea.getText() + "Bot：" + aiReply + "\n\n");
        // } catch (Exception err) {
        // System.out.println("Bot can not reply beacuse:" + err.getMessage());
        // }
        // }
        // });
        // inputField.addActionListener(e -> sendButton.doClick());
        // dialog.add(sendButton, BorderLayout.SOUTH);

        // dialog.setVisible(true);

        JDialog dialog = new JDialog(this, "Talk with Bot", true);
        dialog.setSize(300, 500);
        dialog.setLocationRelativeTo(null);
        // 历史内容
        JTextArea textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setPreferredSize(new Dimension(300, 400));

        dialog.add(textArea, BorderLayout.NORTH);
        // 输入框
        JTextField inputField = new JTextField();
        inputField.setPreferredSize(new Dimension(300, 50));
        dialog.add(inputField);
        // 发送按钮
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> {
            String user_talk = inputField.getText().trim();
            if (!user_talk.isEmpty()) {
                System.out.println("user_talk:" + user_talk);
                String history = textArea.getText();
                textArea.setText(history + "用户：" + user_talk + "\n");
                // 清空输入并滚动到底部
                inputField.setText("");
                textArea.setCaretPosition(textArea.getDocument().getLength());

                // 异步生成 AI 回复，避免阻塞 UI
                // 先显示占位提示
                SwingUtilities.invokeLater(() -> {
                    textArea.append("Bot：正在思考...\n\n");
                    textArea.setCaretPosition(textArea.getDocument().getLength());
                });

                new Thread(() -> {
                    try {
                        String aiReply = getAiReply(user_talk);
                        SwingUtilities.invokeLater(() -> {
                            // 将回复追加到历史（保留占位亦可，这里直接追加）
                            textArea.append("Bot：" + aiReply + "\n\n");
                            textArea.setCaretPosition(textArea.getDocument().getLength());
                        });
                    } catch (Exception err) {
                        SwingUtilities.invokeLater(() -> {
                            textArea.append("Bot：回复失败：" + err.getMessage() + "\n\n");
                            textArea.setCaretPosition(textArea.getDocument().getLength());
                        });
                    }
                }).start();
            }
        });
        inputField.addActionListener(e -> sendButton.doClick());
        dialog.add(sendButton, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private String getAiReply(String userTalk) {
        if (userTalk == null || userTalk.trim().isEmpty()) {
            return "请说点什么～";
        }
        String lower = userTalk.toLowerCase();
        try {
            if (lower.contains("你好") || lower.contains("hello") || lower.contains("hi")) {
                return "你好，我是本地助理，有什么我可以帮你的？";
            }
            if (lower.contains("时间") || lower.contains("现在几点") || lower.contains("几点")) {
                return "当前时间：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            if (lower.contains("帮助") || lower.contains("help")) {
                return "可用命令：start / wear / count / talk。你也可以问我当前时间或打招呼。";
            }
            if (lower.contains("谢谢") || lower.contains("thanks")) {
                return "不客气，乐意效劳。";
            }
        } catch (Exception ignored) {
        }

        // 随机候选回复
        String[] candidates = new String[] {
                "这是个有趣的问题，但我还在学习中。",
                "能详细描述一下么？",
                "我不太确定，能换个说法吗？",
                "嗯，我觉得可以试试其他方式来解决。"
        };
        return candidates[Math.abs(random.nextInt()) % candidates.length];
    }

    // -------------------------- 1. 衣服颜色配置对话框（wear命令）相关代码
    // --------------------------
    // 初始化衣服图片配置：季节->时间->天气->衣服图片
    private void initClothesImgConfig() {
        // 夏季配置
        Map<String, Map<String, Image>> summerConfig = new HashMap<>();
        Map<String, Image> summerDay = new HashMap<>();
        summerDay.put("晴天", summerDaySunny);
        summerDay.put("雨天", summerDayRainy);
        Map<String, Image> summerNight = new HashMap<>();
        summerNight.put("晴天", summerNightSunny);
        summerNight.put("雨天", summerNightRainy);
        summerConfig.put("白天", summerDay);
        summerConfig.put("晚上", summerNight);

        // 冬季配置
        Map<String, Map<String, Image>> winterConfig = new HashMap<>();
        Map<String, Image> winterDay = new HashMap<>();
        winterDay.put("晴天", winterDaySunny);
        winterDay.put("雨天", winterDayRainy);
        Map<String, Image> winterNight = new HashMap<>();
        winterNight.put("晴天", winterNightSunny);
        winterNight.put("雨天", winterNightRainy);
        winterConfig.put("白天", winterDay);
        winterConfig.put("晚上", winterNight);

        clothesImgConfig.put("夏季", summerConfig);
        clothesImgConfig.put("冬季", winterConfig);
    }

    // 衣服图片选择对话框
    private void showClothesDialog() {
        JDialog clothesDialog = new JDialog(this, "选择衣服图片", true);
        clothesDialog.setSize(400, 300);
        clothesDialog.setLocationRelativeTo(this);
        clothesDialog.setLayout(null);
        clothesDialog.setBackground(Color.WHITE);

        // 季节选择
        Label seasonLabel = new Label("选择季节：");
        seasonLabel.setBounds(50, 60, 60, 25);
        seasonLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        clothesDialog.add(seasonLabel);

        String[] seasons = { "夏季", "冬季" };
        JComboBox<String> seasonCombo = new JComboBox<>(seasons);
        seasonCombo.setBounds(120, 60, 100, 25);
        seasonCombo.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        clothesDialog.add(seasonCombo);

        // 时间选择
        Label timeLabel = new Label("选择时间：");
        timeLabel.setBounds(50, 110, 60, 25);
        timeLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        clothesDialog.add(timeLabel);

        String[] times = { "白天", "晚上" };
        JComboBox<String> timeCombo = new JComboBox<>(times);
        timeCombo.setBounds(120, 110, 100, 25);
        timeCombo.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        clothesDialog.add(timeCombo);

        // 天气选择
        Label weatherLabel = new Label("选择天气：");
        weatherLabel.setBounds(50, 160, 60, 25);
        weatherLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        clothesDialog.add(weatherLabel);

        String[] weathers = { "晴天", "雨天" };
        JComboBox<String> weatherCombo = new JComboBox<>(weathers);
        weatherCombo.setBounds(120, 160, 100, 25);
        weatherCombo.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        clothesDialog.add(weatherCombo);

        // 确认按钮
        JButton confirmBtn = new JButton("确认选择");
        confirmBtn.setBounds(150, 220, 100, 30);
        confirmBtn.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        confirmBtn.setBackground(new Color(66, 133, 244));
        confirmBtn.setForeground(Color.BLACK);
        confirmBtn.setBorderPainted(false);
        confirmBtn.setFocusPainted(false);
        confirmBtn.addActionListener(e -> {
            String season = (String) seasonCombo.getSelectedItem();
            String time = (String) timeCombo.getSelectedItem();
            String weather = (String) weatherCombo.getSelectedItem();

            // 根据选择获取对应的衣服图片并更新当前衣服
            Image targetClothes = clothesImgConfig.get(season).get(time).get(weather);
            if (targetClothes != null) {
                currentClothesImg = targetClothes;
                showTipDialog(String.format("已更换为：%s-%s-%s 衣服", season, time, weather));
            } else {
                showTipDialog("未找到对应衣服图片配置！");
            }
            clothesDialog.dispose();
        });
        clothesDialog.add(confirmBtn);

        // 关闭事件
        clothesDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                clothesDialog.dispose();
            }
        });

        clothesDialog.setVisible(true);
    }

    // -------------------------- 2. 代码行数统计对话框（count命令）相关代码
    // --------------------------
    // 代码行数统计结果模型
    static class CodeLineCountResult {
        private int fileCount;
        private int totalLines;
        private int emptyLines;
        private int commentLines;
        private int codeLines;

        public CodeLineCountResult(int fileCount, int totalLines, int emptyLines, int commentLines, int codeLines) {
            this.fileCount = fileCount;
            this.totalLines = totalLines;
            this.emptyLines = emptyLines;
            this.commentLines = commentLines;
            this.codeLines = codeLines;
        }

        // Getters
        public int getFileCount() {
            return fileCount;
        }

        public int getTotalLines() {
            return totalLines;
        }

        public int getEmptyLines() {
            return emptyLines;
        }

        public int getCommentLines() {
            return commentLines;
        }

        public int getCodeLines() {
            return codeLines;
        }
    }

    // 代码行数统计对话框（修复所有按钮为JButton）
    private void showCountDialog() {
        Dialog countDialog = new Dialog(this, "code line count", true);
        countDialog.setSize(450, 350);
        countDialog.setLocationRelativeTo(this);
        countDialog.setLayout(null);
        countDialog.setBackground(Color.WHITE);

        // 标题
        Label titleLabel = new Label("count code lines in folder");
        titleLabel.setBounds(0, 20, 450, 20);
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        titleLabel.setAlignment(Label.CENTER);
        countDialog.add(titleLabel);

        // 文件夹选择
        Label folderLabel = new Label("path:");
        folderLabel.setBounds(30, 60, 80, 25);
        folderLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        countDialog.add(folderLabel);

        TextField folderField = new TextField(System.getProperty("user.dir"));
        folderField.setBounds(120, 60, 250, 25);
        folderField.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        countDialog.add(folderField);

        // 浏览按钮（JButton）
        JButton browseBtn = new JButton("browse");
        browseBtn.setBounds(380, 60, 60, 25);
        browseBtn.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        browseBtn.setBackground(new Color(0, 0, 0));
        browseBtn.setBorderPainted(false);
        browseBtn.setFocusPainted(false);
        browseBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = fileChooser.showOpenDialog(countDialog);
            if (result == JFileChooser.APPROVE_OPTION) {
                folderField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });
        countDialog.add(browseBtn);

        // 语言类型选择
        Label langLabel = new Label("language:");
        langLabel.setBounds(30, 110, 80, 25);
        langLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        countDialog.add(langLabel);

        Map<String, String[]> langExtensions = new HashMap<>();
        langExtensions.put("Java", new String[] { ".java" });
        langExtensions.put("Python", new String[] { ".py" });
        langExtensions.put("JavaScript", new String[] { ".js" });
        langExtensions.put("All", new String[] { ".java", ".py", ".js", ".cpp", ".c", ".html", ".css" });

        String[] languages = langExtensions.keySet().toArray(new String[0]);
        JComboBox<String> langCombo = new JComboBox<>(languages);
        langCombo.setBounds(120, 110, 120, 25);
        langCombo.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        countDialog.add(langCombo);

        // 选项复选框
        JCheckBox emptyLineBox = new JCheckBox("count empty lines");
        emptyLineBox.setBounds(30, 160, 120, 25);
        emptyLineBox.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        emptyLineBox.setSelected(true);
        countDialog.add(emptyLineBox);

        JCheckBox commentBox = new JCheckBox("count comments");
        commentBox.setBounds(180, 160, 120, 25);
        commentBox.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        commentBox.setSelected(true);
        countDialog.add(commentBox);

        // 保存结果选项
        JCheckBox saveToFileBox = new JCheckBox("save result to file");
        saveToFileBox.setBounds(30, 190, 160, 25);
        saveToFileBox.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        countDialog.add(saveToFileBox);

        // 格式选择
        Label formatLabel = new Label("format:");
        formatLabel.setBounds(200, 190, 60, 25);
        formatLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        countDialog.add(formatLabel);

        String[] formats = new String[] { "csv", "json", "xlsx" };
        JComboBox<String> formatCombo = new JComboBox<>(formats);
        formatCombo.setBounds(260, 190, 80, 25);
        formatCombo.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        countDialog.add(formatCombo);

        // 保存路径输入
        TextField savePathField = new TextField(System.getProperty("user.dir") + File.separator + "count_result.csv");
        savePathField.setBounds(30, 220, 300, 25);
        savePathField.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        countDialog.add(savePathField);

        JButton browseSaveBtn = new JButton("save to");
        browseSaveBtn.setBounds(340, 220, 100, 25);
        browseSaveBtn.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        browseSaveBtn.setBackground(new Color(0, 0, 0));
        browseSaveBtn.setBorderPainted(false);
        browseSaveBtn.setFocusPainted(false);
        browseSaveBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File(savePathField.getText()));
            int result = fileChooser.showSaveDialog(countDialog);
            if (result == JFileChooser.APPROVE_OPTION) {
                savePathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });
        countDialog.add(browseSaveBtn);

        // 统计按钮（修复为JButton，添加样式方法）
        JButton countBtn = new JButton("start count");
        countBtn.setBounds(170, 220, 120, 30);
        countBtn.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        countBtn.setBackground(new Color(66, 133, 244));
        countBtn.setForeground(Color.BLACK);
        countBtn.setBorderPainted(false); // 现在有效
        countBtn.setFocusPainted(false); // 现在有效
        countBtn.addActionListener(e -> {
            String folderPath = folderField.getText().trim();
            String language = (String) langCombo.getSelectedItem();
            boolean countEmpty = emptyLineBox.isSelected();
            boolean countComment = commentBox.isSelected();

            // 验证文件夹是否存在
            File folder = new File(folderPath);
            if (!folder.exists() || !folder.isDirectory()) {
                showTipDialog("file not exist or not a folder!");
                return;
            }

            // 异步统计（避免阻塞UI）
            new Thread(() -> {
                try {
                    String[] extensions = langExtensions.get(language);
                    CodeLineCountResult result = countCodeLines(folder, extensions, countEmpty, countComment);

                    // 显示统计结果
                    SwingUtilities.invokeLater(() -> {
                        String resultMsg = String.format(
                                "统计结果：\n" +
                                        "目标文件夹：%s\n" +
                                        "语言类型：%s\n" +
                                        "总文件数：%d\n" +
                                        "总行数：%d\n" +
                                        "空行数：%d\n" +
                                        "注释行数：%d\n" +
                                        "有效代码行数：%d",
                                folderPath, language,
                                result.getFileCount(), result.getTotalLines(),
                                result.getEmptyLines(), result.getCommentLines(),
                                result.getCodeLines());
                        // 如果勾选了保存，则在后台写入文件
                        if (saveToFileBox.isSelected()) {
                            String path = savePathField.getText().trim();
                            String fmt = (String) formatCombo.getSelectedItem();
                            if (path.isEmpty()) {
                                showTipDialog("请先选择保存路径！\n" + resultMsg);
                            } else {
                                // 执行保存（在后台线程完成，回调提示）
                                new Thread(() -> {
                                    try {
                                        File outFile = new File(path);
                                        if (fmt.equalsIgnoreCase("csv")) {
                                            saveResultAsCSV(result, outFile);
                                        } else if (fmt.equalsIgnoreCase("json")) {
                                            saveResultAsJSON(result, outFile);
                                        } else if (fmt.equalsIgnoreCase("xlsx")) {
                                            saveResultAsXLSX(result, outFile);
                                        }
                                        SwingUtilities.invokeLater(
                                                () -> showTipDialog(resultMsg + "\n已保存到：" + outFile.getAbsolutePath()));
                                    } catch (Exception saveEx) {
                                        SwingUtilities.invokeLater(
                                                () -> showTipDialog(resultMsg + "\n保存失败：" + saveEx.getMessage()));
                                    }
                                }).start();
                            }
                        } else {
                            showTipDialog(resultMsg);
                        }
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> showTipDialog("统计失败：" + ex.getMessage()));
                }
            }).start();

            countDialog.dispose();
        });
        countDialog.add(countBtn);

        // 关闭事件
        countDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                countDialog.dispose();
            }
        });

        countDialog.setVisible(true);
    }

    // 代码行数统计核心方法
    private CodeLineCountResult countCodeLines(File folder, String[] extensions, boolean countEmpty,
            boolean countComment) throws IOException {
        int fileCount = 0;
        int totalLines = 0;
        int emptyLines = 0;
        int commentLines = 0;

        // 遍历文件夹下所有指定后缀的文件
        File[] files = folder.listFiles((dir, name) -> {
            for (String ext : extensions) {
                if (name.toLowerCase().endsWith(ext.toLowerCase())) {
                    return true;
                }
            }
            return false;
        });

        if (files == null)
            return new CodeLineCountResult(0, 0, 0, 0, 0);

        for (File file : files) {
            if (file.isFile()) {
                fileCount++;
                List<String> lines = java.nio.file.Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                totalLines += lines.size();

                boolean inBlockComment = false;
                for (String line : lines) {
                    String trimmedLine = line.trim();

                    // 统计空行
                    if (trimmedLine.isEmpty()) {
                        emptyLines++;
                        continue;
                    }

                    // 统计注释行（支持//和/* */）
                    if (inBlockComment) {
                        commentLines++;
                        if (trimmedLine.endsWith("*/")) {
                            inBlockComment = false;
                        }
                    } else {
                        if (trimmedLine.startsWith("//")) {
                            commentLines++;
                        } else if (trimmedLine.startsWith("/*")) {
                            commentLines++;
                            if (!trimmedLine.endsWith("*/")) {
                                inBlockComment = true;
                            }
                        }
                    }
                }
            }
        }

        // 计算有效代码行数
        int codeLines = totalLines;
        if (!countEmpty)
            codeLines -= emptyLines;
        if (!countComment)
            codeLines -= commentLines;

        return new CodeLineCountResult(fileCount, totalLines, emptyLines, commentLines, codeLines);
    }

    // ---------- 保存统计结果的方法 ----------
    private void saveResultAsCSV(CodeLineCountResult result, File file) throws IOException {
        try (BufferedWriter bw = java.nio.file.Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            bw.write("fileCount,totalLines,emptyLines,commentLines,codeLines\n");
            bw.write(String.format("%d,%d,%d,%d,%d\n", result.getFileCount(), result.getTotalLines(),
                    result.getEmptyLines(), result.getCommentLines(), result.getCodeLines()));
        }
    }

    private void saveResultAsJSON(CodeLineCountResult result, File file) throws IOException {
        String json = String.format(
                "{\n  \"fileCount\": %d,\n  \"totalLines\": %d,\n  \"emptyLines\": %d,\n  \"commentLines\": %d,\n  \"codeLines\": %d\n}",
                result.getFileCount(), result.getTotalLines(), result.getEmptyLines(), result.getCommentLines(),
                result.getCodeLines());
        java.nio.file.Files.write(file.toPath(), json.getBytes(StandardCharsets.UTF_8));
    }

    private void saveResultAsXLSX(CodeLineCountResult result, File file) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("count");
            XSSFRow header = sheet.createRow(0);
            header.createCell(0).setCellValue("fileCount");
            header.createCell(1).setCellValue("totalLines");
            header.createCell(2).setCellValue("emptyLines");
            header.createCell(3).setCellValue("commentLines");
            header.createCell(4).setCellValue("codeLines");

            XSSFRow data = sheet.createRow(1);
            data.createCell(0).setCellValue(result.getFileCount());
            data.createCell(1).setCellValue(result.getTotalLines());
            data.createCell(2).setCellValue(result.getEmptyLines());
            data.createCell(3).setCellValue(result.getCommentLines());
            data.createCell(4).setCellValue(result.getCodeLines());

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }

    // 补充缺失的BufferedImage内部类（避免编译错误）
    private static class BufferedImage extends java.awt.image.BufferedImage {
        public BufferedImage(int width, int height, int imageType) {
            super(width, height, imageType);
        }
    }

    // 提示对话框
    private void showTipDialog(String message) {
        Dialog tipDialog = new Dialog(this, "提示", true);
        tipDialog.setSize(320, 150);
        tipDialog.setLocationRelativeTo(this);
        tipDialog.setLayout(null);

        Label tipLabel = new Label(message);
        tipLabel.setBounds(30, 40, 260, 60);
        tipLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        tipDialog.add(tipLabel);

        // 确定按钮（修改为JButton）
        JButton okBtn = new JButton("确定");
        okBtn.setBounds(130, 120, 60, 30);
        okBtn.setBackground(new Color(240, 240, 240));
        okBtn.setBorderPainted(false);
        okBtn.setFocusPainted(false);
        okBtn.addActionListener(e -> tipDialog.dispose());
        tipDialog.add(okBtn);

        tipDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                tipDialog.dispose();
            }
        });

        tipDialog.setVisible(true);
    }

    // 子弹的大小
    Size small = new SmallSize();
    Size medium = new MediumSize();
    Size large = new LargeSize();

    // 子弹类
    class Bullet {
        int x; // 子弹x坐标
        int y; // 子弹y坐标
        int speed; // 子弹移动速度
        int width = 10; // 子弹宽度
        int height = 10; // 子弹高度
        String kind;
        double damage = 1.0;

        // 子弹构造方法
        public Bullet(int startX, int startY, int speed, String kind, Size size) {
            this.x = startX;
            this.y = startY;
            this.speed = speed;
            this.kind = kind;
            switch (kind) {
                case "Cross": {
                    this.damage = new CrossBullet(size).calculateDamage();
                }
                    break;
                case "Triangle": {
                    this.damage = new TriangleBullet(size).calculateDamage();
                }
                    break;
                case "Dot": {
                    this.damage = new DotBullet(size).calculateDamage();
                }
                    break;
                default:
                    break;
            }
        }

        // 更新子弹位置（向右移动）
        public void update() {
            this.x += this.speed;
        }

        // 绘制子弹
        public void draw(Graphics g) {
            // 根据种类绘制不同的子弹
            switch (kind) {
                case "Cross": {
                    // 画矩形
                    g.setColor(Color.BLACK);
                    g.fillRect(x, y, (int) (width * this.damage), (int) (height * this.damage));
                }
                    break;
                case "Dot": {
                    // 画圆形边框
                    g.setColor(Color.YELLOW);
                    g.drawRoundRect(x, y, (int) (width * this.damage), (int) (height * this.damage), 10, 10);
                }
                    break;
                default:
                    break;
            }
        }

        // 判断子弹是否超出屏幕
        public boolean isOutOfScreen() {
            return this.x > GameFrame.this.getWidth();
        }
    }

    // 绘制线程
    class PaintThread extends Thread {
        @Override
        public void run() {
            while (true) {
                // 随机生成新子弹（从屏幕左侧）
                spawnRandomBullet();

                // 更新坦克位置
                updateTankPos();

                // 更新所有子弹位置并清理超出屏幕的子弹
                updateBullets();

                // 重绘画面
                repaint();

                // 控制帧率
                try {
                    Thread.sleep(33); // 约30帧/秒
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        // 随机生成子弹（从屏幕左侧）
        private void spawnRandomBullet() {
            if (isGameStarted && random.nextInt(bulletSpawnRate) == 0) {
                int startX = 0;
                int startY = random.nextInt(getHeight() - 10);
                int speed = random.nextInt(7) + 2;

                // 随机生成不同类型和大小的子弹
                switch (new Random().nextInt(7)) {
                    case 0:
                        break;
                    case 1:
                        bulletList.add(new Bullet(startX, startY, speed, "Cross", small));
                        break;
                    case 2:
                        bulletList.add(new Bullet(startX, startY, speed, "Cross", medium));
                        break;
                    case 3:
                        bulletList.add(new Bullet(startX, startY, speed, "Cross", large));
                        break;
                    case 4:
                        bulletList.add(new Bullet(startX, startY, speed, "Dot", small));
                        break;
                    case 5:
                        bulletList.add(new Bullet(startX, startY, speed, "Dot", medium));
                        break;
                    case 6:
                        bulletList.add(new Bullet(startX, startY, speed, "Dot", large));
                        break;
                }
            }
        }

        // 更新坦克位置
        private void updateTankPos() {
            if (left && tankX > 0)
                tankX -= 5;
            if (right && tankX < getWidth() - tankWidth)
                tankX += 5;
            if (up && tankY > 0)
                tankY -= 5;
            if (down && tankY < getHeight() - tankHeight)
                tankY += 5;
        }

        // 更新所有子弹
        private void updateBullets() {
            for (int i = 0; i < bulletList.size(); i++) {
                Bullet bullet = bulletList.get(i);
                bullet.update(); // 移动子弹
                // 碰撞检测：子弹与坦克相交则加分并移除子弹
                Rectangle bulletRect = new Rectangle(bullet.x, bullet.y, bullet.width, bullet.height);
                Rectangle tankRect = new Rectangle(tankX, tankY, tankWidth, tankHeight);
                if (bulletRect.intersects(tankRect)) {
                    totalScore += (int) bulletList.get(i).damage;
                    // duck.act();
                    // 改为异步执行
                    if (duck != null) {
                        duck.setAction(getPointAction);
                        DuckBark.AsyncActionUtil.execute(duck::act);
                    }
                    bulletList.remove(i);
                    i--;
                    continue;
                }
                if (bullet.isOutOfScreen()) {
                    bulletList.remove(i); // 移除超出屏幕的子弹
                    i--; // 调整索引，避免漏检
                }
            }
        }
    }

    // 双缓冲解决闪屏
    private Image offScreenImage = null;

    @Override
    public void update(Graphics g) {
        if (offScreenImage == null) {
            offScreenImage = createImage(getWidth(), getHeight());
        }
        Graphics offG = offScreenImage.getGraphics();
        offG.setColor(getBackground());
        offG.fillRect(0, 0, getWidth(), getHeight());
        paint(offG);
        g.drawImage(offScreenImage, 0, 0, null);
        offG.dispose();
    }

    // 键盘监听
    class KeyMonitor extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    left = true;
                    break;
                case KeyEvent.VK_RIGHT:
                    right = true;
                    break;
                case KeyEvent.VK_UP:
                    up = true;
                    break;
                case KeyEvent.VK_DOWN:
                    down = true;
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    left = false;
                    break;
                case KeyEvent.VK_RIGHT:
                    right = false;
                    break;
                case KeyEvent.VK_UP:
                    up = false;
                    break;
                case KeyEvent.VK_DOWN:
                    down = false;
                    break;
            }
        }
    }

    // 定义可点击区域
    private final Rectangle clickableArea = new Rectangle(100, 200, 180, 210);
    private final Rectangle changeClothingArea = new Rectangle(300, 200, 180, 210);

    // 绘制游戏元素
    @Override
    public void paint(Graphics g) {
        // 绘制背景
        g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
        Color keepColor = g.getColor();
        // 绘制可点击图片
        if (clickableArea != null) {
            g.drawImage(
                    clickAreaImg,
                    clickableArea.x, clickableArea.y,
                    clickableArea.width, clickableArea.height,
                    this);
        }
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        FontRenderContext frc = ((Graphics2D) g).getFontRenderContext();
        String clickableAreaText = "Click here";
        Rectangle2D textRect = g.getFont().getStringBounds(clickableAreaText, frc);
        int textX1 = clickableArea.x + (int) ((clickableArea.width - textRect.getWidth()) / 2);
        int textY1 = clickableArea.y + (int) ((clickableArea.height + textRect.getHeight()) / 2) - 3;
        g.drawString(clickableAreaText, textX1, textY1);
        g.setColor(keepColor);

        // 绘制衣服切换区域
        if (changeClothingArea != null) {
            g.drawImage(
                    currentClothesImg,
                    changeClothingArea.x, changeClothingArea.y,
                    changeClothingArea.width, changeClothingArea.height,
                    this);
        }

        // 绘制坦克
        g.drawImage(tank, tankX, tankY, tankWidth, tankHeight, this);
        // 绘制所有子弹
        for (Bullet bullet : bulletList) {
            bullet.draw(g);
        }
        // 中间下方时间与分数
        g.setFont(new Font("Arial", Font.BOLD, 18));
        long elapsedSec = Math.max(0, (System.currentTimeMillis() - startTimeMillis) / 1000);
        long mm = elapsedSec / 60;
        long ss = elapsedSec % 60;
        String leftTimeText = String.format("Time: %02d:%02d", mm, ss);
        String leftScoreText = "  |  Score: " + totalScore;

        // 计算居中位置
        int timeWidth = g.getFontMetrics().stringWidth(leftTimeText);
        int scoreWidth = g.getFontMetrics().stringWidth(leftScoreText);
        int totalWidth = timeWidth + scoreWidth;
        int baseX = (getWidth() - totalWidth) / 2; // 水平居中
        int baseY = getHeight() - 30; // 距离底部30像素

        // 绘制半透明背景
        Color old = g.getColor();
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(baseX - 10, baseY - 18, totalWidth + 20, 25, 5, 5);

        // 绘制文字（黄色，更醒目）
        g.setColor(Color.YELLOW);
        g.drawString(leftTimeText, baseX, baseY);
        g.drawString(leftScoreText, baseX + timeWidth, baseY);
        g.setColor(old);
        // 右上角分数
        g.setFont(new Font("Arial", Font.BOLD, 18));
        String scoreText = "Score: " + totalScore;
        int textWidth = g.getFontMetrics().stringWidth(scoreText);
        int textX = getWidth() - textWidth - 12;
        int textY = 24;
        // 背景遮罩增强可读性
        Color oldColor = g.getColor();
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(textX - 8, textY - 20, textWidth + 16, 26, 8, 8);
        // 阴影 + 文字
        g.setColor(Color.BLACK);
        g.drawString(scoreText, textX + 1, textY + 1);
        g.setColor(Color.WHITE);
        g.drawString(scoreText, textX, textY);
        g.setColor(oldColor);
        // 绘制边框
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
    }

    // 图像加载工具
    public Image loadImage(String imagePath) {

        if (!imagePath.startsWith("/")) {
            imagePath = "/" + imagePath;
        }

        URL url = GameFrame.class.getResource(imagePath);
        if (url != null) {
            try {
                return ImageIO.read(url);
            } catch (IOException e) {
                System.err.println("图像读取失败：" + imagePath);
                throw new RuntimeException("图像加载失败：" + imagePath, e);
            }
        }
        System.err.println("图像文件不存在：" + imagePath);
        System.exit(0);
        return null;
    }
}
