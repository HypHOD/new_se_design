package org.example;

import javax.swing.*;
import com.alibaba.fastjson.JSON; // 需导入fastjson（处理JSON序列化）
import java.awt.*;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 接入大模型API的方式实现对话功能（Java调用Python脚本版）
 * 依赖：fastjson（用于JSON序列化对话历史）
 * Maven依赖：
 * <dependency>
 * <groupId>com.alibaba</groupId>
 * <artifactId>fastjson</artifactId>
 * <version>2.0.41</version>
 * </dependency>
 */
public class TryAPI extends JDialog {
    private final JTextArea historyTextArea;
    private final JTextField inputField;
    private final Random random;
    private final List<ChatMessage> chatHistory = new ArrayList<>();
    private ChatMessage thinkingMessage;

    // 内部类：结构化存储单条对话（保持不变）
    private static class ChatMessage {
        private final String role;
        private String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        // getter/setter（必须，fastjson需要序列化）
        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    // 构造函数（保持不变）
    public TryAPI(Frame parent) {
        super(parent, "Talk with Bot", true);
        this.random = new Random();
        this.historyTextArea = createHistoryTextArea();
        this.inputField = createInputField();
        initWindow();
        assembleUI();
        bindEvents();
    }

    // 以下方法（initWindow、createHistoryTextArea、createInputField、assembleUI、bindEvents）
    // 保持原有逻辑不变，此处省略（与原代码一致）

    // -------------------------- 核心修改：handleSendMessage 保持不变
    // --------------------------
    private void handleSendMessage() {
        String userTalk = inputField.getText().trim();
        if (userTalk.isEmpty())
            return;

        appendUserMessage(userTalk);
        inputField.setText("");
        scrollToBottom();

        appendBotPlaceholder();
        new Thread(() -> {
            try {
                String aiReply = getAiReply(); // 核心修改此方法
                SwingUtilities.invokeLater(() -> {
                    replaceBotPlaceholder(aiReply);
                    scrollToBottom();
                });
            } catch (Exception err) {
                SwingUtilities.invokeLater(() -> {
                    replaceBotPlaceholder("回复失败：" + err.getMessage());
                    scrollToBottom();
                });
            }
        }).start();
    }

    // -------------------------- 核心修改：getAiReply（Java调用Python的核心逻辑）
    // --------------------------
    private String getAiReply() throws Exception {
        // 1. 过滤聊天历史：移除"正在思考"的占位符，保留真实对话
        List<ChatMessage> validHistory = new ArrayList<>();
        for (ChatMessage msg : chatHistory) {
            if (!"正在思考...".equals(msg.getContent())) {
                validHistory.add(msg);
            }
        }

        // 2. 序列化对话历史为JSON（传递给Python）
        String jsonHistory = JSON.toJSONString(validHistory);
        String pythonScriptPath = "/Users/hods/Documents/github/new_se_design/src/main/java/org/example/ai_chat.py"; // 替换为你的Python脚本路径
        // String pythonExec = ""; // Windows系统改为"python"
        // 替换原来的 "python3"，用虚拟环境的Python路径
        String pythonExec = "/Users/hods/Documents/github/new_se_design/.venv/bin/python3";
        // 3. 构建Python进程（关键：通过STDIN传递JSON，避免命令行参数问题）
        ProcessBuilder pb = new ProcessBuilder(pythonExec, pythonScriptPath);
        pb.redirectErrorStream(true); // 合并错误流，便于排查问题
        Process process = pb.start();

        // 4. 向Python脚本写入JSON格式的对话历史（UTF-8编码）
        OutputStream os = process.getOutputStream();
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(os, StandardCharsets.UTF_8));
        writer.write(jsonHistory);
        writer.flush();
        writer.close(); // 关闭输出流，告知Python已写完数据

        // 5. 读取Python脚本的输出（AI回复或错误信息）
        InputStream is = process.getInputStream();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line).append("\n");
        }

        // 6. 等待进程执行完成，检查退出码
        int exitCode = process.waitFor();
        reader.close();
        process.destroy();

        // 7. 处理执行结果
        if (exitCode != 0) {
            throw new RuntimeException("Python脚本执行失败（退出码：" + exitCode + "），错误信息：" + result.toString().trim());
        }

        String aiReply = result.toString().trim();
        if (aiReply.isEmpty()) {
            throw new RuntimeException("Python脚本未返回有效回复");
        }

        return aiReply;
    }

    // 以下方法（appendUserMessage、appendBotPlaceholder、replaceBotPlaceholder、scrollToBottom）
    // 保持原有逻辑不变，此处省略（与原代码一致）

    // 测试入口（保持不变）
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("测试窗口");
            frame.setSize(400, 300);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);

            JButton openChatBtn = new JButton("打开 DeepSeek-OCR 聊天");
            openChatBtn.addActionListener(e -> {
                TryAPI dialog = new TryAPI(frame);
                dialog.setVisible(true);
            });

            frame.add(openChatBtn, BorderLayout.CENTER);
            frame.setVisible(true);
        });
    }

    // -------------------------- 原有辅助方法（保持不变） --------------------------
    private void initWindow() {
        setSize(300, 500);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
    }

    private JTextArea createHistoryTextArea() {
        JTextArea textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        return textArea;
    }

    private JTextField createInputField() {
        JTextField textField = new JTextField();
        textField.setPreferredSize(new Dimension(300, 50));
        textField.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        return textField;
    }

    private void assembleUI() {
        JScrollPane scrollPane = new JScrollPane(historyTextArea);
        scrollPane.setPreferredSize(new Dimension(300, 400));
        JButton sendButton = new JButton("Send");
        sendButton.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.NORTH);
        add(inputField, BorderLayout.CENTER);
        add(sendButton, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(sendButton);
    }

    private void bindEvents() {
        JButton sendButton = (JButton) getContentPane().getComponent(2);
        sendButton.addActionListener(e -> handleSendMessage());
        inputField.addActionListener(e -> handleSendMessage());
    }

    private void appendUserMessage(String message) {
        ChatMessage userMsg = new ChatMessage("user", message);
        chatHistory.add(userMsg);
        String history = historyTextArea.getText();
        historyTextArea.setText(history + "用户：" + message + "\n");
    }

    private void appendBotPlaceholder() {
        this.thinkingMessage = new ChatMessage("assistant", "正在思考...");
        chatHistory.add(thinkingMessage);
        historyTextArea.append("Bot：正在思考...\n\n");
    }

    private void replaceBotPlaceholder(String reply) {
        if (thinkingMessage != null && chatHistory.contains(thinkingMessage)) {
            thinkingMessage.setContent(reply);
            this.thinkingMessage = null;
        }
        String history = historyTextArea.getText();
        String newHistory = history.replaceAll("Bot：正在思考...\\n\\n", "Bot：" + reply + "\n\n");
        historyTextArea.setText(newHistory);
    }

    private void scrollToBottom() {
        historyTextArea.setCaretPosition(historyTextArea.getDocument().getLength());
    }
}