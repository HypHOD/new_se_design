package org.example;

public class DotPocket extends PocketItem {
    public static final double BASE_DAMAGE = 1.0;

    public DotPocket(Size size) {
        super(size);
    }

    @Override
    public double calculateDamage() {
        return BASE_DAMAGE * size.getCoefficient();
    }
}
