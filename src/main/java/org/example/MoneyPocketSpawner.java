package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MoneyPocketSpawner {
    // 生成配置
    private int moneypocketSpawnRate;
    private final Random random;
    private final Size smallSize;
    private final Size mediumSize;
    private final Size largeSize;

    // 枚举红包类型
    private enum MoneyPocketType {
        CROSS, DOT, TRIANGLE
    }

    // 构造函数
    public MoneyPocketSpawner(int moneypocketSpawnRate, Size smallSize, Size mediumSize, Size largeSize) {
        this.moneypocketSpawnRate = moneypocketSpawnRate;
        this.smallSize = smallSize;
        this.mediumSize = mediumSize;
        this.largeSize = largeSize;
        this.random = new Random();
    }

    // 单个生成-从左侧随机
    public MoneyPocket spawnSingleMoneyPocket(int screenHeight) {
        // 随机到0就生成
        if (random.nextInt(moneypocketSpawnRate) != 0) {
            return null;
        }

        // 随机属性
        int startX = 0;
        int startY = random.nextInt(screenHeight - 10);
        int speed = random.nextInt(7) + 2;
        MoneyPocketType moneyPocketType = getRandomMoneyPocketType();
        Size moneyPocketSize = getRandomMoneyPocketSize();

        return createMoneyPocketByRandom(startX, startY, speed, moneyPocketType, moneyPocketSize);
    }

    // 批量生成
    public List<MoneyPocket> spawnMoneyPockets(int screenHeight) {
        List<MoneyPocket> moneyPockets = new ArrayList<>();
        MoneyPocket moneyPocket = spawnSingleMoneyPocket(screenHeight);
        if (moneyPocket != null) {
            moneyPockets.add(moneyPocket);
        }
        return moneyPockets;
    }

    // 选择类型
    private MoneyPocketType getRandomMoneyPocketType() {
        MoneyPocketType[] types = MoneyPocketType.values();
        return types[random.nextInt(types.length)];
    }

    // 选择大小
    private Size getRandomMoneyPocketSize() {
        switch (random.nextInt(3)) {
            case 0:
                return smallSize;
            case 1:
                return mediumSize;
            default:
                return largeSize;
        }
    }

    // 创建对象
    private MoneyPocket createMoneyPocketByRandom(int x, int y, int speed, MoneyPocketType type, Size size) {
        switch (type) {
            case CROSS:
                return new CrossMoneyPocket(size);
            case DOT:
                return new DotMoneyPocket(size);
            case TRIANGLE:
                return new DotMoneyPocket(size);
            default:
                throw new IllegalArgumentException("未知红包类型");
        }
    }

    // 修改速度和生成率
    public void setMoneyPocketSpawnRate(int rate) {
        this.moneypocketSpawnRate = rate;
    }

    public int getMoneyPocketSpawnRate() {
        return moneypocketSpawnRate;
    }

}
