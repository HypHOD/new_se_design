package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

public class ShowClothesSelectDialog extends JDialog {
    private final Map<String, Map<String, Map<String, Image>>> clothesImgConfig;
    private final ClothesCallback callback;

    private JComboBox<String> seasonCombo;
    private JComboBox<String> timeCombo;
    private JComboBox<String> weatherCombo;

    /**
     * 构造方法
     * 
     * @param parent           父窗口（主窗口）
     * @param clothesImgConfig 衣服配置（从ClothesConfig传入）
     * @param callback         回调接口（与主窗口通信）
     */
    public ShowClothesSelectDialog(Frame parent, Map<String, Map<String, Map<String, Image>>> clothesImgConfig,
            ClothesCallback callback) {
        super(parent, "选择衣服图片", true);
        this.clothesImgConfig = clothesImgConfig;
        this.callback = callback;
        initUI();
    }

    // 初始化对话框
    private void initUI() {
        setSize(400, 300);
        setLocationRelativeTo(getParent());
        setLayout(null);
        setBackground(Color.WHITE);
        setResizable(false);

        // 添加组件
        addSeasonComponent();
        addTimeComponent();
        addWeatherComponent();
        addConfirmButton();

        // 关闭事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                callback.onSelectCanceled();
                dispose();
            }
        });
    }

    // 季节选择
    private void addSeasonComponent() {
        Label seasonLabel = new Label("选择季节：");
        seasonLabel.setBounds(50, 60, 60, 25);
        seasonLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        add(seasonLabel);

        String[] seasons = clothesImgConfig.keySet().toArray(new String[0]);
        seasonCombo = new JComboBox<>(seasons);
        seasonCombo.setBounds(120, 60, 100, 25);
        seasonCombo.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        add(seasonCombo);
    }

    // 时间选择
    private void addTimeComponent() {
        Label timeLabel = new Label("选择时间：");
        timeLabel.setBounds(50, 110, 60, 25);
        timeLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        add(timeLabel);

        String[] times = { "白天", "晚上" };
        timeCombo = new JComboBox<>(times);
        timeCombo.setBounds(120, 110, 100, 25);
        timeCombo.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        add(timeCombo);
    }

    // 天气选择
    private void addWeatherComponent() {
        Label weatherLabel = new Label("选择天气：");
        weatherLabel.setBounds(50, 160, 60, 25);
        weatherLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        add(weatherLabel);

        String[] weathers = { "晴天", "雨天" };
        weatherCombo = new JComboBox<>(weathers);
        weatherCombo.setBounds(120, 160, 100, 25);
        weatherCombo.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        add(weatherCombo);
    }

    // 确认按钮
    private void addConfirmButton() {
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

            // 从配置获取图片
            Image targetClothes = clothesImgConfig
                    .getOrDefault(season, new HashMap<>())
                    .getOrDefault(time, new HashMap<>())
                    .get(weather);

            // 回调结果
            if (targetClothes != null) {
                String selectInfo = String.format("%s-%s-%s 衣服", season, time, weather);
                callback.onSelectSuccess(targetClothes, selectInfo);
            } else {
                callback.onSelectFailure("未找到对应衣服图片配置！");
            }

            dispose();
        });

        add(confirmBtn);
    }
}