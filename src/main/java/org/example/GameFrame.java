package org.example;

// import org.example.CodeStatisticsUI;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
// import java.time.LocalDateTime;
// import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

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

    // 通关配置
    private static final int PASS_SCORE = 5; // 通关所需分数
    private boolean isGamePassed = false; // 游戏是否通关
    private Dialog passDialog; // 通关弹窗

    // 子弹管理
    List<Bullet> bulletList = new ArrayList<>();
    Random random = new Random(); // 用于生成随机值
    int bulletSpawnRate = 10; // 子弹生成概率,数值越大生成越慢

    // wear配置rS
    private Image currentClothesImg; // 当前衣服图片
    private ClothesConfig clothesConfig; // wear模块的配置

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
        // initClothesImgConfig();
        InitWearModule();
        // 通关弹窗
        initPassDialog();

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

    private void initPassDialog() {
        passDialog = new Dialog(this, "游戏通关！", true);
        passDialog.setSize(400, 300);
        passDialog.setLocationRelativeTo(this);
        passDialog.setLayout(null);
        passDialog.setResizable(false);

        // 通关标题
        Label passTitle = new Label("恭喜通关！");
        passTitle.setFont(new Font("Microsoft YaHei", Font.BOLD, 24));
        passTitle.setForeground(new Color(233, 30, 99));
        passTitle.setBounds(130, 50, 140, 30);
        passDialog.add(passTitle);

        // 通关信息（分数 + 用时）
        Label scoreInfo = new Label("最终得分：" + totalScore);
        scoreInfo.setFont(new Font("Microsoft YaHei", Font.PLAIN, 16));
        scoreInfo.setBounds(150, 100, 100, 20);
        passDialog.add(scoreInfo);

        Label timeInfo = new Label("用时：00:00");
        timeInfo.setFont(new Font("Microsoft YaHei", Font.PLAIN, 16));
        timeInfo.setBounds(150, 130, 100, 20);
        passDialog.add(timeInfo);

        // 重新开始按钮
        JButton restartBtn = new JButton("重新开始");
        restartBtn.setBounds(80, 180, 100, 40);
        restartBtn.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        restartBtn.setBackground(new Color(76, 175, 80));
        // restartBtn.setForeground(Color.WHITE);
        restartBtn.setBorderPainted(false);
        restartBtn.addActionListener(e -> {
            restartGame(); // 重启游戏
            passDialog.dispose();
        });
        passDialog.add(restartBtn);

        // 退出按钮
        JButton exitBtn = new JButton("退出游戏");
        exitBtn.setBounds(220, 180, 100, 40);
        exitBtn.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        exitBtn.setBackground(new Color(244, 67, 54));
        // exitBtn.setForeground(Color.WHITE);
        exitBtn.setBorderPainted(false);
        exitBtn.addActionListener(e -> System.exit(0));
        passDialog.add(exitBtn);

        // 弹窗关闭事件
        passDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                passDialog.dispose();
                restartGame(); // 关闭弹窗后重启游戏
            }
        });
    }

    // 重启游戏功能
    private void restartGame() {
        totalScore = 0;
        startTimeMillis = System.currentTimeMillis();
        isGameStarted = true;
        isGamePassed = false;
        // 使用同步锁, 防止同时绘制
        synchronized (bulletList) {
            bulletList.clear(); // 清空子弹
        }
        tankX = 300; // 重置坦克位置
        tankY = 300;
        left = false;
        right = false;
        up = false;
        down = false;

        if (offScreenImage != null) {
            offScreenImage.flush(); // 清空双缓冲图像
            offScreenImage = null;
        }
        repaint();

        // 更新通关弹窗的分数和时间显示
        for (Component comp : passDialog.getComponents()) {
            if (comp instanceof Label) {
                Label label = (Label) comp;
                if (label.getText().startsWith("最终得分：")) {
                    label.setText("最终得分：" + totalScore);
                } else if (label.getText().startsWith("用时：")) {
                    label.setText("用时：00:00");
                }
            }
        }
    }

    // 检测是否通关功能
    private void checkGamePass() {
        if (totalScore >= PASS_SCORE && !isGamePassed) {
            isGamePassed = true;

            // 计算通关用时
            long elapsedSec = (System.currentTimeMillis() - startTimeMillis) / 1000;
            long mm = elapsedSec / 60;
            long ss = elapsedSec % 60;
            String timeText = String.format("用时：%02d:%02d", mm, ss);

            // 更新弹窗的分数和时间显示
            for (Component comp : passDialog.getComponents()) {
                if (comp instanceof Label) {
                    Label label = (Label) comp;
                    if (label.getText().startsWith("最终得分：")) {
                        label.setText("最终得分：" + totalScore);
                    } else if (label.getText().startsWith("用时：")) {
                        label.setText(timeText);
                    }
                }
            }

            // 显示通关弹窗
            passDialog.setVisible(true);
        }
    }

    private void InitWearModule() {
        // 1. 实现图片加载器（将主窗口的loadImage方法适配给wear模块）
        ClothesConfig.ImageLoader imageLoader = imagePath -> GameFrame.this.loadImage(imagePath);

        // 2. 初始化衣服配置（资源加载、配置构建由wear模块内部完成）
        clothesConfig = new ClothesConfig(imageLoader);

        // 3. 设置初始衣服图片
        currentClothesImg = clothesConfig.getInitialClothesImg();

    }

    // 唐老师对话窗口
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
                // showClothesDialog(); // 打开衣服颜色配置对话框
                openClothesSelectDialog();
                break;
            case "count":
                // showCountDialog(); // 打开代码行数统计对话框
                CodeStatisticsUI.main(null);
                break;
            case "talk":
                // new ShowTalkDialog(this).setVisible(true);
                new TryAPI(this).setVisible(true);
                break;
            default:
                showTipDialog("Invalid command");
                break;
        }
    }

    // 解耦的wear
    private void openClothesSelectDialog() {
        new ShowClothesSelectDialog(
                this, // 父窗口
                clothesConfig.getClothesImgConfig(), // 从wear模块获取配置
                new ClothesCallback() {
                    @Override
                    public void onSelectSuccess(Image selectedClothes, String selectInfo) {
                        currentClothesImg = selectedClothes;
                        showTipDialog("已更换为：" + selectInfo);
                    }

                    @Override
                    public void onSelectFailure(String errorMsg) {
                        showTipDialog(errorMsg);
                    }

                    @Override
                    public void onSelectCanceled() {
                        // 取消选择时可添加逻辑（可选）
                    }
                }).setVisible(true);
    }

    // 提示对话框 输入对话出错提示
    private void showTipDialog(String message) {
        Dialog tipDialog = new Dialog(this, "提示", true);
        tipDialog.setSize(320, 150);
        tipDialog.setLocationRelativeTo(this);
        tipDialog.setLayout(null);

        Label tipLabel = new Label(message);
        tipLabel.setBounds(30, 40, 260, 60);
        tipLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        tipDialog.add(tipLabel);

        // 确定按钮
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

    // 红包的大小
    Size small = new SmallSize();
    Size medium = new MediumSize();
    Size large = new LargeSize();

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

        // 更新所有子弹-修改为使用独立类
        private void updateBullets() {
            for (int i = 0; i < bulletList.size(); i++) {
                Bullet bullet = bulletList.get(i);
                bullet.update(); // 移动子弹
                // 碰撞检测：子弹与坦克相交则加分并移除子弹
                Rectangle bulletRect = new Rectangle(bullet.x, bullet.y, bullet.width, bullet.height);
                Rectangle tankRect = new Rectangle(tankX, tankY, tankWidth, tankHeight);
                if (bulletRect.intersects(tankRect)) {
                    totalScore += (int) bulletList.get(i).damage;

                    // 检测是否通关
                    checkGamePass();
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
                if (bullet.isOutOfScreen(getWidth())) {
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

        // 绘制文字
        g.setColor(Color.YELLOW);
        g.drawString(leftTimeText, baseX, baseY);
        g.drawString(leftScoreText, baseX + timeWidth, baseY);
        g.setColor(old);
        // 分数
        g.setFont(new Font("Arial", Font.BOLD, 18));
        String scoreText = "Score: " + totalScore;
        int textWidth = g.getFontMetrics().stringWidth(scoreText);
        int textX = getWidth() - textWidth - 12;
        int textY = 24;
        // 背景遮罩
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
