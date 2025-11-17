package org.example;

public class DotBullet extends Bullet {
    public static final double BASE_DAMAGE = 1.0;
    public DotBullet(Size size){
        super(size);
    }
    @Override
    public double calculateDamage() {return BASE_DAMAGE*size.getCoefficient();}
}
