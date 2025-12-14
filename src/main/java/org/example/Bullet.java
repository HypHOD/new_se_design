package org.example;

import java.awt.*;

/**
 * 解耦Bullet类
 */
public class Bullet {
    int x; // 子弹x坐标
    int y; // 子弹y坐标
    int speed; // 子弹移动速度
    int width = 10; // 子弹宽度
    int height = 10; // 子弹高度
    String kind;
    double damage = 1.0;

    // 关联已有的Size、CrossPocket等类
    public Bullet(int startX, int startY, int speed, String kind, Size size) {
        this.x = startX;
        this.y = startY;
        this.speed = speed;
        this.kind = kind;
        // 复用你已有的CrossPocket、DotPocket等类计算伤害
        switch (kind) {
            case "Cross":
                this.damage = new CrossPocket(size).calculateDamage();
                break;
            case "Triangle":
                this.damage = new TriangleBullet(size).calculateDamage();
                break;
            case "Dot":
                this.damage = new DotPocket(size).calculateDamage();
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
        switch (kind) {
            case "Cross":
                g.setColor(Color.BLACK);
                g.fillRect(x, y, (int) (width * this.damage), (int) (height * this.damage));
                break;
            case "Dot":
                g.setColor(Color.YELLOW);
                g.drawRoundRect(x, y, (int) (width * this.damage), (int) (height * this.damage), 10, 10);
                break;
            default:
                break;
        }
    }

    // 判断子弹是否超出屏幕
    public boolean isOutOfScreen(int screenWidth) {
        return this.x > screenWidth;
    }

    // 获取子弹的碰撞矩形
    public Rectangle getCollisionRect() {
        return new Rectangle(x, y, (int) (width * damage), (int) (height * damage));
    }

    // 获取点数
    public double getDamage() {
        return damage;
    }

    // 基础接口
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

}