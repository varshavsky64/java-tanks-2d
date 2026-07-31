package io.github.varshavsky64.tanks;

import java.util.Random;

/** Четыре направления движения. Угол используется при отрисовке спрайтов. */
public enum Direction {
    UP(0, -1, 0.0),
    RIGHT(1, 0, Math.PI / 2),
    DOWN(0, 1, Math.PI),
    LEFT(-1, 0, -Math.PI / 2);

    public final int dx;
    public final int dy;
    public final double angle;

    Direction(int dx, int dy, double angle) {
        this.dx = dx;
        this.dy = dy;
        this.angle = angle;
    }

    public Direction opposite() {
        return switch (this) {
            case UP -> DOWN;
            case DOWN -> UP;
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
        };
    }

    public boolean isHorizontal() {
        return dx != 0;
    }

    public static Direction random(Random random) {
        return values()[random.nextInt(values().length)];
    }
}
