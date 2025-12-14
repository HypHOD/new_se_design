package org.example;

public abstract class PocketItem {
    protected Size size;

    public PocketItem(Size size) {
        this.size = size;
    }

    public abstract double calculateDamage();
}
