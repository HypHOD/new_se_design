package org.example;

public class DotMoneyPocket extends MoneyPocket {
    public static final double BASE_POINT = 2.0;

    public DotMoneyPocket(Size size) {
        super(size);
    }

    @Override
    public double calculatePoint() {
        return BASE_POINT * size.getCoefficient();
    }

}
