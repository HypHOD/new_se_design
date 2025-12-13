package org.example;

public class TriangleMoneyPocket extends MoneyPocket {
    public static final double BASE_POINT = 3.0;

    public TriangleMoneyPocket(Size size) {
        super(size);
    }

    @Override
    public double calculatePoint() {
        return BASE_POINT * size.getCoefficient();
    }

}
