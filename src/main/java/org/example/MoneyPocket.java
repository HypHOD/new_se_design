package org.example;

public abstract class MoneyPocket {
    protected Size size;

    public MoneyPocket(Size size) {
        this.size = size;
    }

    public abstract double calculatePoint();
}
