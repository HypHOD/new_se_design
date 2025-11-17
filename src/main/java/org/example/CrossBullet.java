package Game;

public class CrossBullet extends Bullet {
    public static final double BASE_DAMAGE = 1.0;
    public CrossBullet(Size size){
        super(size);
    }
    @Override
    public double calculateDamage() {return BASE_DAMAGE*size.getCoefficient();}
}
