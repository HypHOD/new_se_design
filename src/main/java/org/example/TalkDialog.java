package org.example;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * 独立的对话对话框类
 */
public class TalkDialog extends JDialog {
    // 保持原有功能依赖的组件和工具类
    private final JTextArea historyTextArea;
    private final JTextField inputField;
    private final Random random;

    // 构造函数：接收父窗口（GameFrame），保持模态对话框特性
    public TalkDialog(Frame parent) {
        super(parent, "Talk with Bot", true); // 模态对话框，阻塞父窗口
        this.random = new Random();
        this.historyTextArea = createHistoryTextArea();
        this.inputField = createInputField();

        // 初始化窗口配置
        initWindow();
        // 组装UI组件
        assembleUI();
        // 绑定事件
        bindEvents();
    }

    // 初始化窗口基本属性
    private void initWindow() {
        setSize(300, 500);
        setLocationRelativeTo(getParent()); // 相对于父窗口居中
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); // 关闭时释放资源
        setResizable(false); // 禁止调整大小
    }

    // 创建历史消息显示区域
    private JTextArea createHistoryTextArea() {
        JTextArea textArea = new JTextArea();
        textArea.setLineWrap(true); // 自动换行
        textArea.setWrapStyleWord(true); // 按单词换行
        textArea.setEditable(false); // 禁止编辑
        textArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        return textArea;
    }

    // 创建输入框
    private JTextField createInputField() {
        JTextField textField = new JTextField();
        textField.setPreferredSize(new Dimension(300, 50));
        textField.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        return textField;
    }

    // 组装UI组件（布局和之前保持一致）
    private void assembleUI() {
        // 历史消息区域添加滚动条
        JScrollPane scrollPane = new JScrollPane(historyTextArea);
        scrollPane.setPreferredSize(new Dimension(300, 400));

        // 发送按钮
        JButton sendButton = new JButton("Send");
        sendButton.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));

        // 布局管理（和原逻辑一致）
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.NORTH);
        add(inputField, BorderLayout.CENTER);
        add(sendButton, BorderLayout.SOUTH);

        // 设置默认按钮（按Enter触发发送）
        getRootPane().setDefaultButton(sendButton);
    }

    // 绑定按钮和输入框事件
    private void bindEvents() {
        JButton sendButton = (JButton) getContentPane().getComponent(2); // 获取发送按钮

        // 发送按钮点击事件
        sendButton.addActionListener(e -> handleSendMessage());

        // 输入框回车事件
        inputField.addActionListener(e -> handleSendMessage());
    }

    // 处理发送消息逻辑（迁移原方法核心逻辑）
    private void handleSendMessage() {
        String userTalk = inputField.getText().trim();
        if (userTalk.isEmpty()) {
            return; // 空消息不处理
        }

        // 追加用户消息到历史记录
        appendUserMessage(userTalk);
        // 清空输入框并滚动到底部
        inputField.setText("");
        scrollToBottom();

        // 异步生成AI回复（避免阻塞UI，保持原逻辑）
        appendBotPlaceholder(); // 显示"正在思考"占位
        new Thread(() -> {
            try {
                String aiReply = getAiReply(userTalk);
                // UI操作必须在EDT线程执行
                SwingUtilities.invokeLater(() -> {
                    replaceBotPlaceholder(aiReply); // 替换占位为真实回复
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

    // 追加用户消息
    private void appendUserMessage(String message) {
        String history = historyTextArea.getText();
        historyTextArea.setText(history + "用户：" + message + "\n");
    }

    // 显示AI思考占位符
    private void appendBotPlaceholder() {
        historyTextArea.append("Bot：正在思考...\n\n");
    }

    // 替换占位符为真实回复
    private void replaceBotPlaceholder(String reply) {
        String history = historyTextArea.getText();
        // 移除最后一行的占位符，替换为真实回复
        String newHistory = history.replaceAll("Bot：正在思考...\\n\\n", "Bot：" + reply + "\n\n");
        historyTextArea.setText(newHistory);
    }

    // 滚动到底部（保持原功能）
    private void scrollToBottom() {
        historyTextArea.setCaretPosition(historyTextArea.getDocument().getLength());
    }

    // 迁移原有的AI回复逻辑（保持功能一致）
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

        // 随机候选回复（保持原数组）
        String[] candidates = new String[] {
                "这是个有趣的问题，但我还在学习中。",
                "能详细描述一下么？",
                "我不太确定，能换个说法吗？",
                "嗯，我觉得可以试试其他方式来解决。"
        };
        return candidates[Math.abs(random.nextInt()) % candidates.length];
    }
}