package io.github.varshavsky64.tanks;

/** Всплывающие очки над уничтоженным танком: поднимаются и растворяются. */
public final class ScorePopup extends Entity {

    private static final int LIFETIME = 55;
    private static final int RISE = 26;

    private final String text;
    private final double startY;
    private int frame;

    public ScorePopup(int centerX, int centerY, int amount) {
        super(centerX, centerY, 1, 1);
        this.text = "+" + amount;
        this.startY = centerY;
    }

    @Override
    public void update(World world) {
        frame++;
        y = startY - RISE * easeOut((double) frame / LIFETIME);
        if (frame >= LIFETIME) {
            kill();
        }
    }

    private static double easeOut(double t) {
        return 1 - Math.pow(1 - t, 3);
    }

    public String getText() {
        return text;
    }

    /** Прозрачность 0..1: последнюю треть жизни надпись тает. */
    public float alpha() {
        double t = (double) frame / LIFETIME;
        return (float) Math.max(0, Math.min(1, (1 - t) * 3));
    }
}
