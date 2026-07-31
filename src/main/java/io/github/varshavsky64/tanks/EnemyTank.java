package io.github.varshavsky64.tanks;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Random;

/** Вражеский танк с простым ИИ: едет к базе или к игроку и стреляет по прямой. */
public final class EnemyTank extends Tank {

    public enum Type {
        /** Обычный. */
        BASIC(1.3, 4.8, 1, 100),
        /** Быстрый. */
        FAST(2.4, 5.2, 1, 200),
        /** Скорострельный, снаряд летит быстрее. */
        POWER(1.4, 8.0, 1, 300),
        /** Тяжёлый, выдерживает четыре попадания. */
        ARMOR(1.1, 4.8, 4, 400);

        final double speed;
        final double bulletSpeed;
        final int health;
        final int score;

        Type(double speed, double bulletSpeed, int health, int score) {
            this.speed = speed;
            this.bulletSpeed = bulletSpeed;
            this.health = health;
            this.score = score;
        }

        public int getScore() {
            return score;
        }
    }

    private static final int APPEAR_FRAMES = 60;
    private static final int RAY_STEP = 8;

    private final Type type;
    private final boolean bonus;
    private final Random random;
    private int appearTimer = APPEAR_FRAMES;
    private int decisionTimer;

    public EnemyTank(double x, double y, Type type, boolean bonus, Random random) {
        super(x, y);
        this.type = type;
        this.bonus = bonus;
        this.random = random;
        this.direction = Direction.DOWN;
        this.speed = type.speed;
        this.bulletSpeed = type.bulletSpeed;
        this.health = type.health;
        this.reload = type == Type.POWER ? 30 : 60;
    }

    @Override
    public void update(World world) {
        if (appearTimer > 0) {
            appearTimer--;
            return;
        }
        tickTimers();
        if (world.isFreezeActive()) {
            return;
        }
        if (--decisionTimer <= 0) {
            chooseDirection(world);
        }
        drive(world, direction);
        if (!movedThisFrame()) {
            decisionTimer = 0;
        }
        maybeShoot(world);
    }

    private void chooseDirection(World world) {
        decisionTimer = 45 + random.nextInt(90);
        Point target = pickTarget(world);
        if (target != null && random.nextInt(100) < 35) {
            Direction preferred = directionTowards(target);
            if (canGo(world, preferred)) {
                direction = preferred;
                return;
            }
        }
        Direction[] options = Direction.values();
        for (int attempt = 0; attempt < 8; attempt++) {
            Direction candidate = options[random.nextInt(options.length)];
            if (candidate == direction.opposite() && attempt < 4) {
                continue;
            }
            if (canGo(world, candidate)) {
                direction = candidate;
                return;
            }
        }
        direction = direction.opposite();
    }

    private Point pickTarget(World world) {
        PlayerTank player = world.getPlayer();
        if (player != null && player.isAlive() && random.nextInt(100) < 65) {
            return player.center();
        }
        Rectangle base = world.getLevel().baseBounds();
        return new Point(base.x + base.width / 2, base.y + base.height / 2);
    }

    private Direction directionTowards(Point target) {
        Point c = center();
        int dx = target.x - c.x;
        int dy = target.y - c.y;
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? Direction.RIGHT : Direction.LEFT;
        }
        return dy > 0 ? Direction.DOWN : Direction.UP;
    }

    private boolean canGo(World world, Direction candidate) {
        Rectangle probe = bounds();
        probe.translate((int) Math.ceil(candidate.dx * speed) * 2, (int) Math.ceil(candidate.dy * speed) * 2);
        return world.canTankOccupy(probe, this);
    }

    private void maybeShoot(World world) {
        if (cooldown > 0) {
            return;
        }
        if (hasTargetInLine(world) || random.nextInt(1000) < 8) {
            tryShoot(world);
        }
    }

    /** Проверяет, стоит ли впереди по прямой игрок или база. По базе бьют только вблизи. */
    private boolean hasTargetInLine(World world) {
        Point c = center();
        Rectangle ray = new Rectangle(c.x - 4, c.y - 4, 8, 8);
        int baseRangeSteps = 10 * Level.CELL / RAY_STEP;
        for (int step = 0; step < Level.WIDTH / RAY_STEP; step++) {
            ray.translate(direction.dx * RAY_STEP, direction.dy * RAY_STEP);
            if (ray.x < 0 || ray.y < 0 || ray.x > Level.WIDTH || ray.y > Level.HEIGHT) {
                return false;
            }
            if (world.getLevel().hitsBase(ray)) {
                return step <= baseRangeSteps;
            }
            if (world.getLevel().blocksBullet(ray)) {
                return false;
            }
            PlayerTank player = world.getPlayer();
            if (player != null && player.isAlive() && ray.intersects(player.bounds())) {
                return true;
            }
        }
        return false;
    }

    public boolean isAppearing() {
        return appearTimer > 0;
    }

    public double appearProgress() {
        return 1.0 - (double) appearTimer / APPEAR_FRAMES;
    }

    public Type getType() {
        return type;
    }

    public boolean isBonus() {
        return bonus;
    }

    @Override
    public boolean takeHit(int power) {
        return appearTimer <= 0 && super.takeHit(power);
    }
}
