package io.github.varshavsky64.tanks;

import java.awt.Point;
import java.awt.Rectangle;

/** Базовый игровой объект с позицией и прямоугольником столкновений. */
public abstract class Entity {

    protected double x;
    protected double y;
    protected final int width;
    protected final int height;
    protected boolean alive = true;

    protected Entity(double x, double y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void update(World world);

    public Rectangle bounds() {
        return new Rectangle((int) Math.round(x), (int) Math.round(y), width, height);
    }

    public Point center() {
        return new Point((int) Math.round(x) + width / 2, (int) Math.round(y) + height / 2);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isAlive() {
        return alive;
    }

    public void kill() {
        alive = false;
    }
}
