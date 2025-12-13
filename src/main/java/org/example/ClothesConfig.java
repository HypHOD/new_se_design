package org.example;

import org.example.GameFrame;
import java.awt.Image;
import java.util.HashMap;
import java.util.Map;

public class ClothesConfig {
    // 实际存在的衣服图片资源（与你的文件一一对应）
    private final Image initialClothesImg;
    // 夏季
    private final Image summerDaySunny;
    private final Image summerDayRainy;
    private final Image summerNightSunny;
    private final Image summerNightRainy;
    // 冬季
    private final Image winterDaySunny;
    private final Image winterDayRainy;
    private final Image winterNightSunny;
    private final Image winterNightRainy;

    // 配置映射
    private final Map<String, Map<String, Map<String, Image>>> clothesImgConfig = new HashMap<>();

    public ClothesConfig(ImageLoader imageLoader) {
        // 加载实际存在的图片
        this.initialClothesImg = imageLoader.load("img/clothes/initial_clothes.png");
        // 夏季
        this.summerDaySunny = imageLoader.load("img/clothes/summer_day_sunny.png");
        this.summerDayRainy = imageLoader.load("img/clothes/summer_day_rainy.png");
        this.summerNightSunny = imageLoader.load("img/clothes/summer_night_sunny.png");
        this.summerNightRainy = imageLoader.load("img/clothes/summer_night_rainy.png");
        // 冬季
        this.winterDaySunny = imageLoader.load("img/clothes/winter_day_sunny.png");
        this.winterDayRainy = imageLoader.load("img/clothes/winter_day_rainy.png");
        this.winterNightSunny = imageLoader.load("img/clothes/winter_night_sunny.png");
        this.winterNightRainy = imageLoader.load("img/clothes/winter_night_rainy.png");

        initConfig();
    }

    private void initConfig() {
        Map<String, Map<String, Image>> summerConfig = new HashMap<>();
        // 夏季-白天
        Map<String, Image> summerDay = new HashMap<>();
        summerDay.put("晴天", summerDaySunny);
        summerDay.put("雨天", summerDayRainy);
        // 夏季-晚上
        Map<String, Image> summerNight = new HashMap<>();
        summerNight.put("晴天", summerNightSunny);
        summerNight.put("雨天", summerNightRainy);
        summerConfig.put("白天", summerDay);
        summerConfig.put("晚上", summerNight);
        clothesImgConfig.put("夏季", summerConfig);

        Map<String, Map<String, Image>> winterConfig = new HashMap<>();
        // 冬季-白天
        Map<String, Image> winterDay = new HashMap<>();
        winterDay.put("晴天", winterDaySunny);
        winterDay.put("雨天", winterDayRainy);
        // 冬季-晚上
        Map<String, Image> winterNight = new HashMap<>();
        winterNight.put("晴天", winterNightSunny);
        winterNight.put("雨天", winterNightRainy);
        winterConfig.put("白天", winterDay);
        winterConfig.put("晚上", winterNight);
        clothesImgConfig.put("冬季", winterConfig);
    }

    public Image getInitialClothesImg() {
        return initialClothesImg;
    }

    public Map<String, Map<String, Map<String, Image>>> getClothesImgConfig() {
        return clothesImgConfig;
    }

    public interface ImageLoader {
        Image load(String imagePath);
    }
}