package io.github.varshavsky64.tanks;

/** Анимация взрыва: маленькая от попадания в стену, большая от уничтожения танка. */
public final class Explosion extends Entity {

    private final int totalFrames;
    private int frame;

    private Explosion(int centerX, int centerY, int size, int totalFrames) {
        super(centerX - size / 2.0, centerY - size / 2.0, size, size);
        this.totalFrames = totalFrames;
    }

    public static Explosion small(int centerX, int centerY) {
        return new Explosion(centerX, centerY, 20, 10);
    }

    public static Explosion big(int centerX, int centerY) {
        return new Explosion(centerX, centerY, 52, 22);
    }

    @Override
    public void update(World world) {
        if (++frame >= totalFrames) {
            kill();
        }
    }

    /** Прогресс анимации от 0 до 1. */
    public double progress() {
        return (double) frame / totalFrames;
    }
}
