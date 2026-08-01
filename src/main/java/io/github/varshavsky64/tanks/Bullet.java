package io.github.varshavsky64.tanks;

import java.awt.Point;

/** Снаряд: узкая гильза, вытянутая вдоль полёта. Столкновения считает {@link World}. */
public final class Bullet extends Entity {

    /** Длина вдоль направления полёта. */
    public static final int LENGTH = 9;
    /** Толщина поперёк полёта. */
    public static final int GIRTH = 3;

    private final Direction direction;
    private final double speed;
    private final Tank owner;
    private final int power;

    private Bullet(double x, double y, int width, int height,
                   Direction direction, double speed, int power, Tank owner) {
        super(x, y, width, height);
        this.direction = direction;
        this.speed = speed;
        this.power = power;
        this.owner = owner;
    }

    /**
     * Создаёт снаряд у дульного среза танка.
     *
     * @param offset сдвиг поперёк полёта — им разносятся стволы тройного залпа
     */
    public static Bullet spawn(Tank owner, Direction direction, double speed, int power, double offset) {
        int w = direction.isHorizontal() ? LENGTH : GIRTH;
        int h = direction.isHorizontal() ? GIRTH : LENGTH;
        Point c = owner.center();
        double x = c.x + direction.dx * (owner.getWidth() / 2.0) - w / 2.0 - direction.dy * offset;
        double y = c.y + direction.dy * (owner.getHeight() / 2.0) - h / 2.0 + direction.dx * offset;
        return new Bullet(x, y, w, h, direction, speed, power, owner);
    }

    @Override
    public void update(World world) {
        x += direction.dx * speed;
        y += direction.dy * speed;
    }

    public Direction getDirection() {
        return direction;
    }

    public int getPower() {
        return power;
    }

    public boolean isPlayerBullet() {
        return owner instanceof PlayerTank;
    }

    @Override
    public void kill() {
        if (alive) {
            super.kill();
            owner.bulletDestroyed();
        }
    }
}
