package io.github.varshavsky64.tanks;

import java.util.Random;

/** Бонус, выпадающий из «бонусного» вражеского танка. Живёт ограниченное время. */
public final class PowerUp extends Entity {

    public enum Type {
        /** Звезда — улучшение оружия. */
        STAR,
        /** Каска — временная неуязвимость. */
        HELMET,
        /** Граната — уничтожает всех врагов на экране. */
        GRENADE,
        /** Танк — дополнительная жизнь. */
        LIFE,
        /** Лопата — база обрастает бронёй. */
        SHOVEL,
        /** Часы — враги замирают. */
        CLOCK,
        /** Три снаряда — на время танк стреляет залпом из трёх стволов. */
        TRIPLE,
        /** Молния — на время танк едет быстрее. */
        BOOST;

        static Type random(Random random) {
            return values()[random.nextInt(values().length)];
        }
    }

    public static final int SIZE = 28;
    private static final int LIFETIME = 60 * 30;

    private final Type type;
    private int life = LIFETIME;

    public PowerUp(double x, double y, Type type) {
        super(x, y, SIZE, SIZE);
        this.type = type;
    }

    public static PowerUp randomAt(Random random) {
        int x = random.nextInt(Level.WIDTH - SIZE);
        int y = random.nextInt(Level.HEIGHT - SIZE - Level.CELL * 4);
        return new PowerUp(x, y, Type.random(random));
    }

    @Override
    public void update(World world) {
        if (--life <= 0) {
            kill();
        }
    }

    public Type getType() {
        return type;
    }

    /** Мигание перед исчезновением. */
    public boolean isVisible() {
        return life > 60 * 5 || (life / 6) % 2 == 0;
    }
}
