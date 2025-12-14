package org.example;

public class CrossPocket extends PocketItem {
    public static final double BASE_DAMAGE = 1.0;

    public CrossPocket(Size size) {
        super(size);
    }

    @Override
    public double calculateDamage() {
        return BASE_DAMAGE * size.getCoefficient();
    }
}
