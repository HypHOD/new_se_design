package org.example;

public class CrossMoneyPocket extends MoneyPocket {
    public static final double BASE_POINT = 1.0;

    public CrossMoneyPocket(Size size) {
        super(size);
    }

    @Override
    public double calculatePoint() {
        return BASE_POINT * size.getCoefficient();
    }
}
