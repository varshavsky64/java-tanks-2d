package io.github.varshavsky64.tanks;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Карта уровня. Карты пишутся в «крупной» сетке 22x16, где один символ — блок 2x2 клетки
 * (ровно размер танка). При загрузке блок разворачивается в четыре клетки, поэтому кирпич
 * разрушается по четвертинкам, как в оригинальной Battle City.
 */
public final class Level {

    public static final int CELL = 16;
    public static final int COLS = 44;
    public static final int ROWS = 32;
    public static final int WIDTH = COLS * CELL;
    public static final int HEIGHT = ROWS * CELL;

    private static final int MACRO_COLS = COLS / 2;
    private static final int MACRO_ROWS = ROWS / 2;

    /** '.' пусто, '#' кирпич, 'S' броня, 'W' вода, 'T' кусты, 'I' лёд, 'B' база. */
    private static final String[][] MAPS = {
            {
                    "......................",
                    "..##..##..##..##..##..",
                    "..##..##..##..##..##..",
                    "......................",
                    "..##..##..SS..##..##..",
                    "..##..##..SS..##..##..",
                    "......................",
                    ".##..##..####..##..##.",
                    "......................",
                    "..##..##..##..##..##..",
                    "..##..##..##..##..##..",
                    "......................",
                    ".....##......##.......",
                    "......................",
                    ".........###..........",
                    ".........#B#..........",
            },
            {
                    "......................",
                    ".SS................SS.",
                    "......................",
                    "..####..........####..",
                    "..#WW#..........#WW#..",
                    "..#WW#..........#WW#..",
                    "..####..........####..",
                    "......................",
                    ".....TTT......TTT.....",
                    ".....TTT......TTT.....",
                    "......................",
                    "...##....####....##...",
                    "...##....####....##...",
                    "......................",
                    ".........###..........",
                    ".........#B#..........",
            },
            {
                    "..##..............##..",
                    "..##..............##..",
                    "......................",
                    "....SSSSSSSSSSSSSS....",
                    "......................",
                    "...IIII........IIII...",
                    "...IIII........IIII...",
                    "......................",
                    "..##..####..####..##..",
                    "......................",
                    "....WWWW......WWWW....",
                    "......................",
                    "..####..........####..",
                    "......................",
                    ".........###..........",
                    ".........#B#..........",
            },
            {
                    "...S..............S...",
                    "...S..............S...",
                    "......................",
                    "....##############....",
                    "....#WWWWWWWWWWWW#....",
                    "....#WW..TTTT..WW#....",
                    "....#WWWWWWWWWWWW#....",
                    "....######..######....",
                    "......................",
                    "..TT..............TT..",
                    "..TT..............TT..",
                    "......................",
                    "...####........####...",
                    "......................",
                    ".........###..........",
                    ".........#B#..........",
            },
    };

    private final TileType[][] tiles = new TileType[ROWS][COLS];
    private final List<Point> enemySpawns = new ArrayList<>();
    private Point baseCell = new Point(MACRO_COLS - 2, ROWS - 2);
    private boolean baseDestroyed;

    private Level() {
    }

    public static int count() {
        return MAPS.length;
    }

    public static Level load(int index) {
        Level level = new Level();
        String[] macro = MAPS[Math.floorMod(index, MAPS.length)];
        if (macro.length != MACRO_ROWS) {
            throw new IllegalStateException("карта %d: строк %d, ожидалось %d"
                    .formatted(index, macro.length, MACRO_ROWS));
        }
        for (int mr = 0; mr < MACRO_ROWS; mr++) {
            String row = macro[mr];
            if (row.length() != MACRO_COLS) {
                throw new IllegalStateException("карта %d, строка %d: длина %d, ожидалось %d"
                        .formatted(index, mr, row.length(), MACRO_COLS));
            }
            for (int mc = 0; mc < MACRO_COLS; mc++) {
                TileType type = fromChar(row.charAt(mc));
                if (type == TileType.BASE) {
                    level.baseCell = new Point(mc * 2, mr * 2);
                }
                for (int dy = 0; dy < 2; dy++) {
                    for (int dx = 0; dx < 2; dx++) {
                        level.tiles[mr * 2 + dy][mc * 2 + dx] = type;
                    }
                }
            }
        }
        int macroPixels = CELL * 2;
        level.enemySpawns.add(new Point(0, 0));
        level.enemySpawns.add(new Point(MACRO_COLS / 2 * macroPixels, 0));
        level.enemySpawns.add(new Point((MACRO_COLS - 1) * macroPixels, 0));
        return level;
    }

    private static TileType fromChar(char c) {
        return switch (c) {
            case '#' -> TileType.BRICK;
            case 'S' -> TileType.STEEL;
            case 'W' -> TileType.WATER;
            case 'T' -> TileType.TREES;
            case 'I' -> TileType.ICE;
            case 'B' -> TileType.BASE;
            default -> TileType.EMPTY;
        };
    }

    public TileType tileAt(int col, int row) {
        if (col < 0 || row < 0 || col >= COLS || row >= ROWS) {
            return TileType.STEEL;
        }
        return tiles[row][col];
    }

    public List<Point> enemySpawns() {
        return enemySpawns;
    }

    public Point playerSpawn() {
        return new Point((baseCell.x - 4) * CELL, baseCell.y * CELL);
    }

    public Rectangle baseBounds() {
        return new Rectangle(baseCell.x * CELL, baseCell.y * CELL, CELL * 2, CELL * 2);
    }

    public boolean isBaseDestroyed() {
        return baseDestroyed;
    }

    public void destroyBase() {
        baseDestroyed = true;
    }

    public boolean blocksTank(Rectangle r) {
        return anyCell(r, TileType::blocksTank);
    }

    public boolean blocksBullet(Rectangle r) {
        return anyCell(r, TileType::blocksBullet);
    }

    public boolean isOnIce(Rectangle r) {
        return anyCell(r, t -> t == TileType.ICE);
    }

    public boolean hitsBase(Rectangle r) {
        return r.intersects(baseBounds());
    }

    /** true, если в зоне есть броня — нужно, чтобы отличить звук рикошета от звука кирпича. */
    public boolean hasSteel(Rectangle r) {
        return anyCell(r, t -> t == TileType.STEEL);
    }

    private boolean anyCell(Rectangle r, java.util.function.Predicate<TileType> test) {
        int c0 = Math.max(0, r.x / CELL);
        int c1 = Math.min(COLS - 1, (r.x + r.width - 1) / CELL);
        int r0 = Math.max(0, r.y / CELL);
        int r1 = Math.min(ROWS - 1, (r.y + r.height - 1) / CELL);
        for (int row = r0; row <= r1; row++) {
            for (int col = c0; col <= c1; col++) {
                if (test.test(tiles[row][col])) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Разрушает клетки в зоне попадания. power >= 2 пробивает броню. */
    public void damage(Rectangle area, int power) {
        int c0 = Math.max(0, area.x / CELL);
        int c1 = Math.min(COLS - 1, (area.x + area.width - 1) / CELL);
        int r0 = Math.max(0, area.y / CELL);
        int r1 = Math.min(ROWS - 1, (area.y + area.height - 1) / CELL);
        for (int row = r0; row <= r1; row++) {
            for (int col = c0; col <= c1; col++) {
                TileType type = tiles[row][col];
                if (type == TileType.BRICK || (type == TileType.STEEL && power >= 2)) {
                    tiles[row][col] = TileType.EMPTY;
                }
            }
        }
    }

    /** Бонус «лопата»: окружает базу бронёй. */
    public void fortifyBase() {
        for (Point cell : baseRing()) {
            tiles[cell.y][cell.x] = TileType.STEEL;
        }
    }

    /** Возвращает стену вокруг базы к кирпичу после окончания действия бонуса. */
    public void restoreBaseWall() {
        for (Point cell : baseRing()) {
            tiles[cell.y][cell.x] = TileType.BRICK;
        }
    }

    private List<Point> baseRing() {
        List<Point> ring = new ArrayList<>();
        for (int row = baseCell.y - 2; row <= baseCell.y + 3; row++) {
            for (int col = baseCell.x - 2; col <= baseCell.x + 3; col++) {
                boolean insideBase = col >= baseCell.x && col <= baseCell.x + 1
                        && row >= baseCell.y && row <= baseCell.y + 1;
                if (insideBase || col < 0 || row < 0 || col >= COLS || row >= ROWS) {
                    continue;
                }
                ring.add(new Point(col, row));
            }
        }
        return ring;
    }
}
