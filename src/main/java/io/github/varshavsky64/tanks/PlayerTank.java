package io.github.varshavsky64.tanks;

/**
 * Танк игрока. Уровень прокачки (0..3) растёт от бонуса-звезды, а тройной залп
 * и ускорение выдаются на время и по истечении сами сбрасываются.
 */
public final class PlayerTank extends Tank {

    public static final int SPAWN_SHIELD_FRAMES = 180;

    private static final int MAX_UPGRADE = 3;
    private static final int TRIPLE_SHOTS = 3;
    private static final double BASE_SPEED = 2.0;
    /** 3.2 — это 16/5: как и базовая скорость, делит клетку нацело и не рассинхронит сетку. */
    private static final double BOOST_SPEED = 3.2;

    private int upgrade;
    private int tripleTimer;
    private int boostTimer;

    public PlayerTank(double x, double y) {
        super(x, y);
        applyUpgrade();
        giveShield(SPAWN_SHIELD_FRAMES);
    }

    @Override
    public void update(World world) {
        tickTimers();
        Input input = world.getInput();
        Direction requested = null;
        if (input.up) {
            requested = Direction.UP;
        } else if (input.down) {
            requested = Direction.DOWN;
        } else if (input.left) {
            requested = Direction.LEFT;
        } else if (input.right) {
            requested = Direction.RIGHT;
        }
        drive(world, requested);
        if (input.fire) {
            tryShoot(world);
        }
    }

    @Override
    protected void tickTimers() {
        super.tickTimers();
        // Оба временных бонуса кончаются одинаково: пересобираем характеристики заново.
        if (tripleTimer > 0 && --tripleTimer == 0) {
            applyUpgrade();
        }
        if (boostTimer > 0 && --boostTimer == 0) {
            applyUpgrade();
        }
    }

    public void upgrade() {
        if (upgrade < MAX_UPGRADE) {
            upgrade++;
            applyUpgrade();
        }
    }

    public void giveTripleShot(int frames) {
        tripleTimer = Math.max(tripleTimer, frames);
        applyUpgrade();
    }

    public void giveSpeedBoost(int frames) {
        boostTimer = Math.max(boostTimer, frames);
        applyUpgrade();
    }

    public int getUpgradeLevel() {
        return upgrade;
    }

    public boolean hasTripleShot() {
        return tripleTimer > 0;
    }

    public boolean hasSpeedBoost() {
        return boostTimer > 0;
    }

    private void applyUpgrade() {
        speed = boostTimer > 0 ? BOOST_SPEED : BASE_SPEED;
        reload = 26;
        shots = tripleTimer > 0 ? TRIPLE_SHOTS : 1;
        // Слот занимает каждый снаряд, поэтому под залп лимит умножается: иначе после
        // первого же выстрела пришлось бы ждать, пока догорят все три.
        maxBullets = (upgrade >= 2 ? 2 : 1) * shots;
        bulletSpeed = upgrade >= 1 ? 7.5 : 5.5;
        bulletPower = upgrade >= MAX_UPGRADE ? 2 : 1;
        health = 1;
    }
}
