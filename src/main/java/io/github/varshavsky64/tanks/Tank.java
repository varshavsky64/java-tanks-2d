package io.github.varshavsky64.tanks;

import java.awt.Rectangle;

/** Общее поведение танков: движение с выравниванием по сетке, стрельба, урон. */
public abstract class Tank extends Entity {

    public static final int SIZE = Level.CELL * 2;
    private static final int SLIDE_FRAMES = 16;
    /** Расстояние между стволами залпа. */
    private static final double BARREL_SPACING = 10;

    protected Direction direction = Direction.UP;
    protected double speed = 1.4;
    protected int health = 1;
    protected int reload = 28;
    protected int maxBullets = 1;
    protected double bulletSpeed = 5.0;
    protected int bulletPower = 1;
    /** Сколько снарядов уходит за один выстрел. */
    protected int shots = 1;

    protected int cooldown;
    protected int activeBullets;
    protected int shield;
    private int slide;
    private boolean movedThisFrame;

    protected Tank(double x, double y) {
        super(x, y, SIZE, SIZE);
    }

    /** Едет в указанную сторону, либо продолжает скользить по льду, если направление null. */
    protected void drive(World world, Direction requested) {
        movedThisFrame = false;
        if (requested != null) {
            move(world, requested);
            slide = world.getLevel().isOnIce(bounds()) ? SLIDE_FRAMES : 0;
        } else if (slide > 0) {
            slide--;
            move(world, direction);
        }
    }

    private void move(World world, Direction requested) {
        if (direction != requested) {
            direction = requested;
            alignToGrid(requested);
        }
        double nx = x + requested.dx * speed;
        double ny = y + requested.dy * speed;
        Rectangle next = new Rectangle((int) Math.round(nx), (int) Math.round(ny), width, height);
        if (world.canTankOccupy(next, this)) {
            x = nx;
            y = ny;
            movedThisFrame = true;
        }
    }

    /** Прижимает танк к сетке по перпендикулярной оси — иначе не проехать в проходы. */
    private void alignToGrid(Direction requested) {
        if (requested.isHorizontal()) {
            y = Math.round(y / Level.CELL) * (double) Level.CELL;
        } else {
            x = Math.round(x / Level.CELL) * (double) Level.CELL;
        }
    }

    protected void tickTimers() {
        if (cooldown > 0) {
            cooldown--;
        }
        if (shield > 0) {
            shield--;
        }
    }

    public boolean tryShoot(World world) {
        if (cooldown > 0 || activeBullets >= maxBullets) {
            return false;
        }
        cooldown = reload;
        for (int i = 0; i < shots; i++) {
            // Стволы разносятся поперёк полёта симметрично центру и укладываются в ширину танка.
            double offset = (i - (shots - 1) / 2.0) * BARREL_SPACING;
            activeBullets++;
            world.addBullet(Bullet.spawn(this, direction, bulletSpeed, bulletPower, offset));
        }
        world.play(this instanceof PlayerTank ? Sound.PLAYER_SHOT : Sound.ENEMY_SHOT);
        return true;
    }

    void bulletDestroyed() {
        if (activeBullets > 0) {
            activeBullets--;
        }
    }

    /** @return true, если танк уничтожен этим попаданием. */
    public boolean takeHit(int power) {
        if (shield > 0) {
            return false;
        }
        health -= power;
        if (health <= 0) {
            kill();
            return true;
        }
        return false;
    }

    public Direction getDirection() {
        return direction;
    }

    public int getHealth() {
        return health;
    }

    public boolean hasShield() {
        return shield > 0;
    }

    public void giveShield(int frames) {
        shield = Math.max(shield, frames);
    }

    public boolean movedThisFrame() {
        return movedThisFrame;
    }
}
