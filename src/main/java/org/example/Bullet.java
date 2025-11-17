package org.example;

public abstract class Bullet {
    protected Size size;
    public Bullet(Size size) {
        this.size = size;
    }
    public abstract double calculateDamage();
}
