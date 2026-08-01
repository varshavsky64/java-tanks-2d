package io.github.varshavsky64.tanks;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

/** Вся отрисовка. Спрайты рисуются примитивами, внешние ресурсы не нужны. */
public final class Renderer {

    public static final int HUD_WIDTH = 150;
    public static final int SCREEN_WIDTH = Level.WIDTH + HUD_WIDTH;
    public static final int SCREEN_HEIGHT = Level.HEIGHT;

    private static final Color FIELD_BG = new Color(10, 11, 14);
    private static final Color PANEL_TOP = new Color(32, 35, 44);
    private static final Color PANEL_BOTTOM = new Color(19, 20, 26);
    private static final Color ACCENT = new Color(232, 190, 78);
    private static final Color LABEL = new Color(126, 133, 150);
    private static final Color CARD_BG = new Color(14, 15, 20);
    private static final Color CARD_BORDER = new Color(58, 62, 78);

    private static final Color BRICK_DARK = new Color(30, 15, 9);
    private static final Color BRICK_FACE = new Color(76, 38, 23);
    private static final Color BRICK_EDGE = new Color(38, 19, 11);
    private static final Color STEEL = new Color(150, 155, 168);
    private static final Color STEEL_DARK = new Color(52, 56, 68);
    private static final Color WATER = new Color(20, 52, 120);
    private static final Color WATER_LIGHT = new Color(48, 104, 190);
    private static final Color TREES_DARK = new Color(20, 74, 36);
    private static final Color TREES = new Color(32, 116, 54);
    private static final Color ICE = new Color(138, 172, 198);

    private static final Color PLAYER_BODY = new Color(46, 120, 60);
    private static final Color PLAYER_TRIM = new Color(20, 52, 30);
    private static final Color PLAYER_HIGHLIGHT = new Color(104, 186, 116);

    private static final Font FONT_LABEL = new Font(Font.SANS_SERIF, Font.BOLD, 10);
    private static final Font FONT_VALUE = new Font(Font.MONOSPACED, Font.BOLD, 20);
    private static final Font FONT_SMALL = new Font(Font.MONOSPACED, Font.BOLD, 13);
    private static final Font FONT_BADGE = new Font(Font.MONOSPACED, Font.BOLD, 26);
    private static final Font FONT_POPUP = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    private static final Font FONT_BIG = new Font(Font.SANS_SERIF, Font.BOLD, 30);

    private int frame;

    public void render(Graphics2D g, World world) {
        frame++;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g.setColor(FIELD_BG);
        g.fillRect(0, 0, Level.WIDTH, Level.HEIGHT);

        Level level = world.getLevel();
        drawTiles(g, level, false);
        drawBase(g, level);
        drawPowerUps(g, world);
        drawTanks(g, world);
        drawBullets(g, world);
        drawTiles(g, level, true);
        drawExplosions(g, world);
        drawPopups(g, world);
        drawOverlay(g, world);
        drawHud(g, world);
    }

    // --- поле ---------------------------------------------------------------

    private void drawTiles(Graphics2D g, Level level, boolean treesLayer) {
        for (int row = 0; row < Level.ROWS; row++) {
            for (int col = 0; col < Level.COLS; col++) {
                TileType type = level.tileAt(col, row);
                if ((type == TileType.TREES) != treesLayer) {
                    continue;
                }
                int x = col * Level.CELL;
                int y = row * Level.CELL;
                switch (type) {
                    case BRICK -> drawBrick(g, x, y, row);
                    case STEEL -> drawSteel(g, x, y);
                    case WATER -> drawWater(g, x, y);
                    case ICE -> drawIce(g, x, y);
                    case TREES -> drawTrees(g, x, y);
                    default -> {
                    }
                }
            }
        }
    }

    /** Кладка рисуется от абсолютных координат, чтобы узор стыковался между клетками. */
    private void drawBrick(Graphics2D g, int x, int y, int row) {
        int c = Level.CELL;
        int half = c / 2;
        g.setColor(BRICK_FACE);
        g.fillRect(x, y, c, c);
        g.setColor(BRICK_EDGE);
        for (int i = 0; i < 2; i++) {
            int by = y + i * half;
            g.fillRect(x, by, c, 1);
            int offset = (row * 2 + i) % 2 == 0 ? 0 : half / 2;
            for (int lineX = x + offset; lineX < x + c; lineX += half) {
                g.fillRect(lineX, by, 1, half);
            }
        }
        g.setColor(BRICK_DARK);
        g.fillRect(x, y + c - 1, c, 1);
    }

    private void drawSteel(Graphics2D g, int x, int y) {
        int c = Level.CELL;
        g.setColor(STEEL_DARK);
        g.fillRect(x, y, c, c);
        g.setColor(STEEL);
        g.fillRect(x + 1, y + 1, c - 2, c - 2);
        g.setColor(Color.WHITE);
        g.fillRect(x + 2, y + 2, c / 2 - 2, c / 2 - 2);
        g.setColor(STEEL.darker());
        g.fillRect(x + c / 2, y + c / 2, c / 2 - 2, c / 2 - 2);
    }

    private void drawWater(Graphics2D g, int x, int y) {
        int c = Level.CELL;
        g.setColor(WATER);
        g.fillRect(x, y, c, c);
        g.setColor(WATER_LIGHT);
        int wave = (frame / 14) % 2 == 0 ? 0 : 3;
        g.fillRect(x + 1 + wave, y + 3, c - 6, 2);
        g.fillRect(x + 4 - wave, y + 10, c - 6, 2);
    }

    private void drawIce(Graphics2D g, int x, int y) {
        int c = Level.CELL;
        g.setColor(ICE);
        g.fillRect(x, y, c, c);
        g.setColor(new Color(190, 214, 232));
        g.drawLine(x + 2, y + 2, x + c - 3, y + c - 3);
        g.drawLine(x + c - 3, y + 2, x + 2, y + c - 3);
    }

    private void drawTrees(Graphics2D g, int x, int y) {
        int c = Level.CELL;
        g.setColor(TREES_DARK);
        g.fillRect(x, y, c, c);
        g.setColor(TREES);
        g.fillOval(x, y, c - 4, c - 4);
        g.fillOval(x + 4, y + 4, c - 5, c - 5);
    }

    private void drawBase(Graphics2D g, Level level) {
        Rectangle b = level.baseBounds();
        int cx = b.x + b.width / 2;
        if (level.isBaseDestroyed()) {
            g.setColor(new Color(70, 70, 74));
            g.fillRect(b.x + 3, b.y + b.height / 2, b.width - 6, b.height / 2 - 2);
            g.setColor(new Color(40, 40, 44));
            g.fillRect(b.x + 7, b.y + 8, b.width - 14, b.height - 10);
            return;
        }
        g.setColor(new Color(226, 228, 234));
        g.fillPolygon(
                new int[]{cx, b.x + 2, b.x + b.width - 3},
                new int[]{b.y + 2, b.y + 15, b.y + 15}, 3);
        g.fillRect(b.x + 6, b.y + 12, b.width - 12, b.height - 18);
        g.setColor(new Color(168, 172, 182));
        g.fillRect(cx - 2, b.y + 10, 4, b.height - 16);
        g.setColor(new Color(128, 96, 44));
        g.fillRect(b.x + 2, b.y + b.height - 6, b.width - 4, 5);
    }

    // --- объекты ------------------------------------------------------------

    private void drawTanks(Graphics2D g, World world) {
        PlayerTank player = world.getPlayer();
        if (player != null && player.isAlive()) {
            drawTank(g, player, PLAYER_BODY, PLAYER_TRIM, PLAYER_HIGHLIGHT);
            if (player.hasShield()) {
                drawShield(g, player);
            }
        }
        for (EnemyTank enemy : world.getEnemies()) {
            if (enemy.isAppearing()) {
                drawSpawnStar(g, enemy);
                continue;
            }
            Color body = enemyColor(enemy);
            drawTank(g, enemy, body, body.darker().darker(), body.brighter());
        }
    }

    private Color enemyColor(EnemyTank enemy) {
        if (enemy.isBonus() && (frame / 8) % 2 == 0) {
            return new Color(224, 74, 74);
        }
        return switch (enemy.getType()) {
            case BASIC -> new Color(150, 150, 158);
            case FAST -> new Color(206, 236, 246);
            case POWER -> new Color(176, 144, 230);
            case ARMOR -> switch (Math.max(1, enemy.getHealth())) {
                case 4 -> new Color(226, 206, 96);
                case 3 -> new Color(216, 164, 66);
                case 2 -> new Color(204, 116, 52);
                default -> new Color(190, 72, 52);
            };
        };
    }

    private void drawTank(Graphics2D g, Tank tank, Color body, Color trim, Color highlight) {
        double size = tank.getWidth();
        double x = Math.round(tank.getX());
        double y = Math.round(tank.getY());
        AffineTransform saved = g.getTransform();
        g.rotate(tank.getDirection().angle, x + size / 2, y + size / 2);

        double tread = size * 0.22;
        g.setColor(trim);
        g.fill(new Rectangle2D.Double(x, y + 1, tread, size - 2));
        g.fill(new Rectangle2D.Double(x + size - tread, y + 1, tread, size - 2));
        g.setColor(trim.darker());
        for (double i = y + 3; i < y + size - 4; i += 5) {
            g.fill(new Rectangle2D.Double(x, i, tread, 2));
            g.fill(new Rectangle2D.Double(x + size - tread, i, tread, 2));
        }

        double hullX = x + tread + 1;
        double hullW = size - 2 * tread - 2;
        g.setColor(body);
        g.fill(new Rectangle2D.Double(hullX, y + size * 0.18, hullW, size * 0.78));
        g.setColor(trim);
        g.draw(new Rectangle2D.Double(hullX, y + size * 0.18, hullW - 1, size * 0.78 - 1));

        double cx = x + size / 2;
        double turret = size * 0.36;
        g.setColor(highlight);
        g.fill(new Ellipse2D.Double(cx - turret / 2, y + size * 0.44 - turret / 2, turret, turret));
        g.setColor(trim);
        g.draw(new Ellipse2D.Double(cx - turret / 2, y + size * 0.44 - turret / 2, turret, turret));

        double barrel = Math.max(3, size * 0.12);
        g.setColor(trim);
        g.fill(new Rectangle2D.Double(cx - barrel / 2, y, barrel, size * 0.46));
        g.setTransform(saved);
    }

    private void drawShield(Graphics2D g, Tank tank) {
        Rectangle b = tank.bounds();
        g.setStroke(new BasicStroke(2f));
        g.setColor((frame / 4) % 2 == 0 ? new Color(120, 200, 255) : Color.WHITE);
        g.drawOval(b.x - 3, b.y - 3, b.width + 6, b.height + 6);
        g.setStroke(new BasicStroke(1f));
    }

    private void drawSpawnStar(Graphics2D g, EnemyTank enemy) {
        Rectangle b = enemy.bounds();
        double p = enemy.appearProgress();
        int r = (int) (b.width / 2 * (0.3 + 0.7 * p));
        int cx = b.x + b.width / 2;
        int cy = b.y + b.height / 2;
        AffineTransform saved = g.getTransform();
        g.rotate(p * Math.PI * 4, cx, cy);
        g.setColor((frame / 3) % 2 == 0 ? Color.WHITE : new Color(120, 200, 255));
        g.fillPolygon(
                new int[]{cx, cx + r / 3, cx + r, cx + r / 3, cx, cx - r / 3, cx - r, cx - r / 3},
                new int[]{cy - r, cy - r / 3, cy, cy + r / 3, cy + r, cy + r / 3, cy, cy - r / 3}, 8);
        g.setTransform(saved);
    }

    private void drawBullets(Graphics2D g, World world) {
        for (Bullet bullet : world.getBullets()) {
            Rectangle b = bullet.bounds();
            g.setColor(new Color(120, 122, 130));
            g.fillRect(b.x, b.y, b.width, b.height);
            g.setColor(new Color(250, 250, 240));
            if (bullet.getDirection().isHorizontal()) {
                g.fillRect(b.x + 1, b.y, b.width - 2, b.height);
            } else {
                g.fillRect(b.x, b.y + 1, b.width, b.height - 2);
            }
        }
    }

    private void drawExplosions(Graphics2D g, World world) {
        for (Explosion explosion : world.getExplosions()) {
            Rectangle b = explosion.bounds();
            double p = explosion.progress();
            int size = (int) (b.width * (0.4 + 0.6 * p));
            int cx = b.x + b.width / 2;
            int cy = b.y + b.height / 2;
            g.setColor(new Color(255, 190, 40, (int) (210 * (1 - p))));
            g.fillOval(cx - size / 2, cy - size / 2, size, size);
            g.setColor(new Color(250, 88, 24, (int) (200 * (1 - p))));
            g.fillOval(cx - size / 3, cy - size / 3, size * 2 / 3, size * 2 / 3);
            g.setColor(new Color(255, 252, 224, (int) (200 * (1 - p))));
            g.fillOval(cx - size / 6, cy - size / 6, size / 3, size / 3);
        }
    }

    private void drawPopups(Graphics2D g, World world) {
        g.setFont(FONT_POPUP);
        FontMetrics fm = g.getFontMetrics();
        for (ScorePopup popup : world.getPopups()) {
            int x = (int) popup.getX() - fm.stringWidth(popup.getText()) / 2;
            int y = (int) popup.getY();
            int a = (int) (popup.alpha() * 255);
            g.setColor(new Color(0, 0, 0, a));
            g.drawString(popup.getText(), x + 1, y + 1);
            g.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), a));
            g.drawString(popup.getText(), x, y);
        }
    }

    private void drawPowerUps(Graphics2D g, World world) {
        for (PowerUp powerUp : world.getPowerUps()) {
            if (!powerUp.isVisible()) {
                continue;
            }
            Rectangle b = powerUp.bounds();
            g.setColor(new Color(250, 250, 252));
            g.fillRoundRect(b.x, b.y, b.width, b.height, 7, 7);
            g.setColor(new Color(30, 32, 38));
            g.drawRoundRect(b.x, b.y, b.width - 1, b.height - 1, 7, 7);
            drawPowerUpIcon(g, powerUp.getType(), b);
        }
    }

    private void drawPowerUpIcon(Graphics2D g, PowerUp.Type type, Rectangle b) {
        int cx = b.x + b.width / 2;
        int cy = b.y + b.height / 2;
        switch (type) {
            case STAR -> {
                g.setColor(new Color(236, 164, 26));
                g.fillPolygon(
                        new int[]{cx, cx + 3, cx + 9, cx + 4, cx + 6, cx, cx - 6, cx - 4, cx - 9, cx - 3},
                        new int[]{cy - 9, cy - 3, cy - 3, cy + 1, cy + 8, cy + 4, cy + 8, cy + 1, cy - 3, cy - 3}, 10);
            }
            case HELMET -> {
                g.setColor(new Color(52, 118, 220));
                g.fillArc(cx - 9, cy - 8, 18, 16, 0, 180);
                g.fillRect(cx - 10, cy, 20, 4);
            }
            case GRENADE -> {
                g.setColor(new Color(46, 48, 54));
                g.fillOval(cx - 7, cy - 5, 14, 14);
                g.fillRect(cx - 2, cy - 10, 5, 6);
            }
            case LIFE -> {
                g.setColor(new Color(196, 56, 56));
                g.fillRect(cx - 9, cy - 3, 18, 9);
                g.fillRect(cx - 4, cy - 8, 9, 6);
            }
            case SHOVEL -> {
                g.setColor(new Color(132, 84, 38));
                g.fillRect(cx - 2, cy - 10, 4, 12);
                g.setColor(new Color(150, 154, 166));
                g.fillPolygon(new int[]{cx - 7, cx + 7, cx}, new int[]{cy, cy, cy + 10}, 3);
            }
            case CLOCK -> {
                g.setColor(new Color(60, 62, 80));
                g.fillOval(cx - 9, cy - 9, 18, 18);
                g.setColor(Color.WHITE);
                g.fillOval(cx - 7, cy - 7, 14, 14);
                g.setColor(Color.BLACK);
                g.drawLine(cx, cy, cx, cy - 5);
                g.drawLine(cx, cy, cx + 4, cy + 1);
            }
            case TRIPLE -> {
                // Три снаряда рядом — ровно то, что уходит из стволов за выстрел.
                g.setColor(new Color(214, 68, 52));
                for (int i = -1; i <= 1; i++) {
                    g.fillRoundRect(cx + i * 7 - 2, cy - 8, 5, 15, 5, 5);
                }
            }
            case BOOST -> {
                g.setColor(new Color(240, 196, 32));
                g.fillPolygon(
                        new int[]{cx + 4, cx - 8, cx - 1, cx - 5, cx + 8, cx + 1},
                        new int[]{cy - 10, cy + 2, cy + 2, cy + 10, cy - 2, cy - 2}, 6);
            }
        }
    }

    // --- панель -------------------------------------------------------------

    private void drawHud(Graphics2D g, World world) {
        int x0 = Level.WIDTH;
        g.setPaint(new GradientPaint(x0, 0, PANEL_TOP, x0, SCREEN_HEIGHT, PANEL_BOTTOM));
        g.fillRect(x0, 0, HUD_WIDTH, SCREEN_HEIGHT);
        g.setPaint(ACCENT);
        g.fillRect(x0, 0, 2, SCREEN_HEIGHT);

        int left = x0 + 16;
        int y = 26;

        label(g, "ВРАГИ", left, y);
        int remaining = world.getEnemiesLeft();
        for (int i = 0; i < remaining; i++) {
            drawEnemyIcon(g, left + (i % 5) * 18, y + 10 + (i / 5) * 17);
        }

        y = 118;
        label(g, "ЖИЗНИ", left, y);
        int shown = Math.min(world.getLives(), 5);
        for (int i = 0; i < shown; i++) {
            drawLifeIcon(g, left + i * 18, y + 10);
        }
        if (world.getLives() > shown) {
            g.setFont(FONT_SMALL);
            g.setColor(LABEL);
            g.drawString("+" + (world.getLives() - shown), left + shown * 18 + 2, y + 22);
        }

        y = 158;
        label(g, "УРОВЕНЬ", left, y);
        card(g, left, y + 8, 52, 38);
        g.setFont(FONT_BADGE);
        g.setColor(ACCENT);
        centerIn(g, String.valueOf(world.getLevelNumber()), left, y + 8, 52, 38);

        drawScoreCard(g, left, 226, HUD_WIDTH - 32, world);

        y = 344;
        PlayerTank player = world.getPlayer();
        if (player != null && player.getUpgradeLevel() > 0) {
            label(g, "ОРУЖИЕ", left, y);
            for (int i = 0; i < player.getUpgradeLevel(); i++) {
                drawStar(g, left + 6 + i * 16, y + 16, 6);
            }
            y += 42;
        }
        if (world.isFreezeActive()) {
            y = status(g, "ВРАГИ ЗАМЕРЛИ", new Color(120, 200, 255), left, y);
        }
        if (player != null && player.hasTripleShot()) {
            y = status(g, "ТРОЙНОЙ ЗАЛП", new Color(240, 120, 96), left, y);
        }
        if (player != null && player.hasSpeedBoost()) {
            status(g, "УСКОРЕНИЕ", new Color(240, 196, 32), left, y);
        }

        g.setFont(FONT_LABEL);
        g.setColor(new Color(84, 90, 104));
        g.drawString("P — пауза", left, SCREEN_HEIGHT - 48);
        g.drawString("M — звук", left, SCREEN_HEIGHT - 34);
        g.drawString("N — музыка", left, SCREEN_HEIGHT - 20);
    }

    /** Строка активного бонуса в панели; возвращает координату для следующей такой строки. */
    private int status(Graphics2D g, String text, Color color, int left, int y) {
        g.setFont(FONT_SMALL);
        g.setColor(color);
        g.drawString(text, left, y + 12);
        return y + 18;
    }

    /** Карточка очков: крупное золотое число и рекорд под разделителем. */
    private void drawScoreCard(Graphics2D g, int x, int y, int w, World world) {
        int h = 92;
        card(g, x, y, w, h);
        g.setFont(FONT_LABEL);
        g.setColor(LABEL);
        g.drawString("ОЧКИ", x + 10, y + 18);

        g.setFont(FONT_VALUE);
        String score = "%06d".formatted(world.getScore());
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(0, 0, 0, 120));
        g.drawString(score, x + w - 10 - fm.stringWidth(score) + 1, y + 43);
        g.setColor(ACCENT);
        g.drawString(score, x + w - 10 - fm.stringWidth(score), y + 42);

        g.setColor(CARD_BORDER);
        g.drawLine(x + 10, y + 54, x + w - 10, y + 54);

        g.setFont(FONT_LABEL);
        g.setColor(LABEL);
        g.drawString("РЕКОРД", x + 10, y + 72);
        g.setFont(FONT_SMALL);
        String best = "%06d".formatted(world.getHighScore());
        fm = g.getFontMetrics();
        g.setColor(new Color(196, 202, 216));
        g.drawString(best, x + w - 10 - fm.stringWidth(best), y + 73);
    }

    private void card(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(CARD_BG);
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(CARD_BORDER);
        g.drawRoundRect(x, y, w - 1, h - 1, 8, 8);
    }

    private void label(Graphics2D g, String text, int x, int y) {
        g.setFont(FONT_LABEL);
        g.setColor(LABEL);
        g.drawString(text, x, y);
    }

    private void centerIn(Graphics2D g, String text, int x, int y, int w, int h) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, x + (w - fm.stringWidth(text)) / 2,
                y + (h - fm.getHeight()) / 2 + fm.getAscent());
    }

    private void drawStar(Graphics2D g, int cx, int cy, int r) {
        g.setColor(ACCENT);
        g.fillPolygon(
                new int[]{cx, cx + r / 3, cx + r, cx + r / 3, cx, cx - r / 3, cx - r, cx - r / 3},
                new int[]{cy - r, cy - r / 3, cy, cy + r / 3, cy + r, cy + r / 3, cy, cy - r / 3}, 8);
    }

    private void drawEnemyIcon(Graphics2D g, int x, int y) {
        g.setColor(new Color(196, 202, 216));
        g.fillRect(x + 1, y, 3, 12);
        g.fillRect(x + 9, y, 3, 12);
        g.fillRect(x + 4, y + 3, 5, 8);
        g.fillRect(x + 6, y - 2, 2, 6);
    }

    private void drawLifeIcon(Graphics2D g, int x, int y) {
        g.setColor(PLAYER_TRIM);
        g.fillRect(x + 1, y, 3, 12);
        g.fillRect(x + 9, y, 3, 12);
        g.setColor(PLAYER_HIGHLIGHT);
        g.fillRect(x + 4, y + 3, 5, 8);
        g.fillRect(x + 6, y - 2, 2, 6);
    }

    // --- заставки -----------------------------------------------------------

    private void drawOverlay(Graphics2D g, World world) {
        String title = null;
        String hint = null;
        switch (world.getState()) {
            case READY -> {
                title = "УРОВЕНЬ " + world.getLevelNumber();
                hint = "защищай базу";
            }
            case PAUSED -> {
                title = "ПАУЗА";
                hint = "P — продолжить";
            }
            case LEVEL_COMPLETE -> {
                title = "УРОВЕНЬ ПРОЙДЕН";
                hint = "очки: " + world.getScore();
            }
            case GAME_OVER -> {
                title = "ИГРА ОКОНЧЕНА";
                hint = "ENTER — начать заново";
            }
            case PLAYING -> {
            }
        }
        if (title == null) {
            return;
        }
        int bandY = Level.HEIGHT / 2 - 52;
        g.setColor(new Color(0, 0, 0, 190));
        g.fillRect(0, bandY, Level.WIDTH, 104);
        g.setColor(ACCENT);
        g.fillRect(0, bandY, Level.WIDTH, 2);
        g.fillRect(0, bandY + 102, Level.WIDTH, 2);

        g.setFont(FONT_BIG);
        drawCentered(g, title, Level.HEIGHT / 2 + 2, ACCENT);
        if (hint != null) {
            g.setFont(FONT_SMALL);
            drawCentered(g, hint, Level.HEIGHT / 2 + 32, new Color(196, 202, 216));
        }
    }

    private void drawCentered(Graphics2D g, String text, int y, Color color) {
        FontMetrics fm = g.getFontMetrics();
        g.setColor(color);
        g.drawString(text, (Level.WIDTH - fm.stringWidth(text)) / 2, y);
    }
}
