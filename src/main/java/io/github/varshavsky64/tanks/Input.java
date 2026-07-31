package io.github.varshavsky64.tanks;

/** Состояние управления за текущий кадр. */
public final class Input {
    public boolean up;
    public boolean down;
    public boolean left;
    public boolean right;
    public boolean fire;

    public void clear() {
        up = down = left = right = fire = false;
    }
}
