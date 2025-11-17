package org.example;

public class TriangleBullet extends Bullet{
    public static final double BASE_DAMAGE = 3.0;
    public TriangleBullet(Size size){
        super(size);
    }
    @Override
    public double calculateDamage() {return BASE_DAMAGE*size.getCoefficient();}
}
