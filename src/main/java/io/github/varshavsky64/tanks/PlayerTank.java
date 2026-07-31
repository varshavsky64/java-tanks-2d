package io.github.varshavsky64.tanks;

/** Танк игрока. Уровень прокачки (0..3) растёт от бонуса-звезды. */
public final class PlayerTank extends Tank {

    public static final int SPAWN_SHIELD_FRAMES = 180;

    private int upgrade;

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

    public void upgrade() {
        if (upgrade < 3) {
            upgrade++;
            applyUpgrade();
        }
    }

    public int getUpgradeLevel() {
        return upgrade;
    }

    private void applyUpgrade() {
        speed = 2.0;
        reload = 26;
        maxBullets = upgrade >= 2 ? 2 : 1;
        bulletSpeed = upgrade >= 1 ? 7.5 : 5.5;
        bulletPower = upgrade >= 3 ? 2 : 1;
        health = 1;
    }
}
