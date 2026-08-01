package io.github.varshavsky64.tanks;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/** Игровой мир: состояние уровня, все объекты и правила их взаимодействия. */
public final class World {

    private static final int MAX_ENEMIES_ON_FIELD = 5;
    private static final int ENEMIES_PER_LEVEL = 20;
    private static final int SPAWN_INTERVAL = 150;
    private static final int RESPAWN_DELAY = 90;
    private static final int FREEZE_FRAMES = 60 * 8;
    private static final int SHOVEL_FRAMES = 60 * 15;
    private static final int TRIPLE_FRAMES = 60 * 15;
    private static final int BOOST_FRAMES = 60 * 15;
    private static final int POWERUP_SCORE = 500;
    private static final int START_LIVES = 3;
    /** Каждый n-й враг в волне — «бонусный»: при уничтожении оставляет бонус. */
    private static final int BONUS_ENEMY_EVERY = 3;
    /** Сколько бонусов может лежать на поле одновременно. */
    private static final int MAX_POWERUPS_ON_FIELD = 2;

    private final Random random = new Random();
    private final Input input = new Input();
    private final Audio audio;

    private final List<EnemyTank> enemies = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Explosion> explosions = new ArrayList<>();
    private final List<PowerUp> powerUps = new ArrayList<>();
    private final List<ScorePopup> popups = new ArrayList<>();
    private final Deque<EnemyTank> spawnQueue = new ArrayDeque<>();

    private Level level;
    private PlayerTank player;
    private GameState state = GameState.READY;

    private int levelIndex;
    private int score;
    private int highScore;
    private int lives;
    private int stateTimer;
    private int spawnTimer;
    private int spawnPointIndex;
    private int respawnTimer;
    private int freezeTimer;
    private int shovelTimer;

    public World() {
        this(Audio.SILENT);
    }

    public World(Audio audio) {
        this.audio = audio;
        restart();
    }

    public void restart() {
        score = 0;
        lives = START_LIVES;
        levelIndex = 0;
        startLevel(0);
    }

    private void startLevel(int index) {
        levelIndex = index;
        level = Level.load(index);
        enemies.clear();
        bullets.clear();
        explosions.clear();
        powerUps.clear();
        popups.clear();
        spawnQueue.clear();
        freezeTimer = 0;
        shovelTimer = 0;
        spawnTimer = 0;
        spawnPointIndex = 0;
        respawnTimer = 0;
        fillSpawnQueue();
        spawnPlayer();
        state = GameState.READY;
        stateTimer = 100;
        play(Sound.LEVEL_START);
    }

    private void fillSpawnQueue() {
        int difficulty = Math.min(levelIndex, 6);
        for (int i = 0; i < ENEMIES_PER_LEVEL; i++) {
            int roll = random.nextInt(100) + difficulty * 6;
            EnemyTank.Type type;
            if (roll < 45) {
                type = EnemyTank.Type.BASIC;
            } else if (roll < 70) {
                type = EnemyTank.Type.FAST;
            } else if (roll < 90) {
                type = EnemyTank.Type.POWER;
            } else {
                type = EnemyTank.Type.ARMOR;
            }
            boolean bonus = i % BONUS_ENEMY_EVERY == BONUS_ENEMY_EVERY - 1;
            spawnQueue.add(new EnemyTank(0, 0, type, bonus, random));
        }
    }

    private void spawnPlayer() {
        Point spawn = level.playerSpawn();
        player = new PlayerTank(spawn.x, spawn.y);
    }

    public void update() {
        switch (state) {
            case READY -> {
                if (--stateTimer <= 0) {
                    state = GameState.PLAYING;
                }
            }
            case PLAYING -> updatePlaying();
            case LEVEL_COMPLETE -> {
                if (--stateTimer <= 0) {
                    startLevel(levelIndex + 1);
                }
            }
            case PAUSED, GAME_OVER -> {
            }
        }
    }

    private void updatePlaying() {
        updateTimers();
        updateSpawning();

        if (player != null) {
            player.update(this);
            collectPowerUps();
        } else if (--respawnTimer <= 0) {
            if (lives > 0) {
                lives--;
                spawnPlayer();
            } else {
                gameOver();
                return;
            }
        }

        for (EnemyTank enemy : enemies) {
            enemy.update(this);
        }
        for (Bullet bullet : new ArrayList<>(bullets)) {
            bullet.update(this);
            resolveBullet(bullet);
        }
        for (Explosion explosion : new ArrayList<>(explosions)) {
            explosion.update(this);
        }
        for (PowerUp powerUp : new ArrayList<>(powerUps)) {
            powerUp.update(this);
        }
        for (ScorePopup popup : new ArrayList<>(popups)) {
            popup.update(this);
        }

        enemies.removeIf(e -> !e.isAlive());
        bullets.removeIf(b -> !b.isAlive());
        explosions.removeIf(e -> !e.isAlive());
        powerUps.removeIf(p -> !p.isAlive());
        popups.removeIf(p -> !p.isAlive());

        if (spawnQueue.isEmpty() && enemies.isEmpty() && state == GameState.PLAYING) {
            state = GameState.LEVEL_COMPLETE;
            stateTimer = 180;
            play(Sound.LEVEL_CLEAR);
        }
    }

    private void updateTimers() {
        if (freezeTimer > 0) {
            freezeTimer--;
        }
        if (shovelTimer > 0 && --shovelTimer == 0) {
            level.restoreBaseWall();
        }
    }

    private void updateSpawning() {
        if (spawnQueue.isEmpty() || enemies.size() >= MAX_ENEMIES_ON_FIELD) {
            return;
        }
        if (--spawnTimer > 0) {
            return;
        }
        List<Point> spawns = level.enemySpawns();
        for (int attempt = 0; attempt < spawns.size(); attempt++) {
            Point spawn = spawns.get(spawnPointIndex);
            spawnPointIndex = (spawnPointIndex + 1) % spawns.size();
            Rectangle area = new Rectangle(spawn.x, spawn.y, Tank.SIZE, Tank.SIZE);
            if (canTankOccupy(area, null)) {
                EnemyTank template = spawnQueue.poll();
                enemies.add(new EnemyTank(spawn.x, spawn.y, template.getType(), template.isBonus(), random));
                spawnTimer = SPAWN_INTERVAL;
                return;
            }
        }
        spawnTimer = 20;
    }

    private void resolveBullet(Bullet bullet) {
        Rectangle b = bullet.bounds();
        if (b.x < 0 || b.y < 0 || b.x + b.width > Level.WIDTH || b.y + b.height > Level.HEIGHT) {
            explodeSmall(bullet);
            play(Sound.HIT_STEEL);
            bullet.kill();
            return;
        }
        if (level.blocksBullet(b)) {
            if (level.hitsBase(b) && !level.isBaseDestroyed()) {
                level.destroyBase();
                Rectangle base = level.baseBounds();
                explosions.add(Explosion.big(base.x + base.width / 2, base.y + base.height / 2));
                play(Sound.BIG_EXPLOSION);
                bullet.kill();
                gameOver();
                return;
            }
            Rectangle damage = damageArea(bullet);
            boolean steel = level.hasSteel(damage);
            level.damage(damage, bullet.getPower());
            explodeSmall(bullet);
            play(steel && bullet.getPower() < 2 ? Sound.HIT_STEEL : Sound.HIT_BRICK);
            bullet.kill();
            return;
        }
        for (Bullet other : bullets) {
            if (other != bullet && other.isAlive() && other.isPlayerBullet() != bullet.isPlayerBullet()
                    && other.bounds().intersects(b)) {
                other.kill();
                bullet.kill();
                return;
            }
        }
        if (bullet.isPlayerBullet()) {
            for (EnemyTank enemy : enemies) {
                if (enemy.isAlive() && enemy.bounds().intersects(b)) {
                    bullet.kill();
                    if (enemy.takeHit(bullet.getPower())) {
                        destroyEnemy(enemy);
                    } else {
                        explodeSmall(bullet);
                        play(Sound.HIT_STEEL);
                    }
                    return;
                }
            }
        } else if (player != null && player.isAlive() && player.bounds().intersects(b)) {
            bullet.kill();
            if (player.takeHit(bullet.getPower())) {
                destroyPlayer();
            } else {
                explodeSmall(bullet);
            }
        }
    }

    /**
     * Зона разрушения: одна клетка в глубину от точки касания и две клетки поперёк —
     * ровно ширина танка, поэтому пробоина получается проезжей.
     */
    private Rectangle damageArea(Bullet bullet) {
        Rectangle b = bullet.bounds();
        Direction dir = bullet.getDirection();
        int tipX = switch (dir) {
            case LEFT -> b.x;
            case RIGHT -> b.x + b.width - 1;
            default -> b.x + b.width / 2;
        };
        int tipY = switch (dir) {
            case UP -> b.y;
            case DOWN -> b.y + b.height - 1;
            default -> b.y + b.height / 2;
        };
        Rectangle area = new Rectangle(tipX, tipY, 1, 1);
        if (dir.isHorizontal()) {
            area.grow(0, Level.CELL / 2 - 1);
        } else {
            area.grow(Level.CELL / 2 - 1, 0);
        }
        return area;
    }

    private void destroyEnemy(EnemyTank enemy) {
        Point c = enemy.center();
        explosions.add(Explosion.big(c.x, c.y));
        play(Sound.EXPLOSION);
        addScore(enemy.getType().getScore(), c.x, c.y);
        if (enemy.isBonus()) {
            // Раньше новый бонус стирал предыдущий; теперь на поле их может лежать два,
            // иначе при частых выпадениях игрок не успевал подобрать первый.
            while (powerUps.size() >= MAX_POWERUPS_ON_FIELD) {
                powerUps.remove(0);
            }
            powerUps.add(PowerUp.randomAt(random));
            play(Sound.POWERUP_APPEAR);
        }
    }

    private void destroyPlayer() {
        Point c = player.center();
        explosions.add(Explosion.big(c.x, c.y));
        play(Sound.BIG_EXPLOSION);
        play(Sound.LIFE_LOST);
        player = null;
        respawnTimer = RESPAWN_DELAY;
    }

    private void explodeSmall(Bullet bullet) {
        Point c = bullet.center();
        explosions.add(Explosion.small(c.x, c.y));
    }

    private void addScore(int amount, int x, int y) {
        score += amount;
        highScore = Math.max(highScore, score);
        popups.add(new ScorePopup(x, y, amount));
    }

    private void collectPowerUps() {
        Rectangle playerBounds = player.bounds();
        for (PowerUp powerUp : new ArrayList<>(powerUps)) {
            if (!powerUp.isAlive() || !powerUp.bounds().intersects(playerBounds)) {
                continue;
            }
            powerUp.kill();
            Point c = powerUp.center();
            addScore(POWERUP_SCORE, c.x, c.y);
            play(Sound.POWERUP_TAKE);
            applyPowerUp(powerUp.getType());
        }
    }

    private void applyPowerUp(PowerUp.Type type) {
        switch (type) {
            case STAR -> player.upgrade();
            case HELMET -> player.giveShield(60 * 10);
            case LIFE -> lives++;
            case CLOCK -> freezeTimer = FREEZE_FRAMES;
            case TRIPLE -> player.giveTripleShot(TRIPLE_FRAMES);
            case BOOST -> player.giveSpeedBoost(BOOST_FRAMES);
            case SHOVEL -> {
                level.fortifyBase();
                shovelTimer = SHOVEL_FRAMES;
            }
            case GRENADE -> {
                for (EnemyTank enemy : new ArrayList<>(enemies)) {
                    if (enemy.isAlive()) {
                        enemy.kill();
                        destroyEnemy(enemy);
                    }
                }
            }
        }
    }

    private void gameOver() {
        state = GameState.GAME_OVER;
        play(Sound.GAME_OVER);
    }

    /** Проверяет, помещается ли танк в прямоугольник: поле, стены и другие танки. */
    public boolean canTankOccupy(Rectangle r, Tank self) {
        if (r.x < 0 || r.y < 0 || r.x + r.width > Level.WIDTH || r.y + r.height > Level.HEIGHT) {
            return false;
        }
        if (level.blocksTank(r)) {
            return false;
        }
        if (player != null && player != self && player.isAlive() && player.bounds().intersects(r)) {
            return false;
        }
        for (EnemyTank enemy : enemies) {
            if (enemy != self && enemy.isAlive() && enemy.bounds().intersects(r)) {
                return false;
            }
        }
        return true;
    }

    public void addBullet(Bullet bullet) {
        bullets.add(bullet);
    }

    public void play(Sound sound) {
        audio.play(sound);
    }

    public void togglePause() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
        } else if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
        }
    }

    public Input getInput() {
        return input;
    }

    public Level getLevel() {
        return level;
    }

    public PlayerTank getPlayer() {
        return player;
    }

    public List<EnemyTank> getEnemies() {
        return enemies;
    }

    public List<Bullet> getBullets() {
        return bullets;
    }

    public List<Explosion> getExplosions() {
        return explosions;
    }

    public List<PowerUp> getPowerUps() {
        return powerUps;
    }

    public List<ScorePopup> getPopups() {
        return popups;
    }

    public GameState getState() {
        return state;
    }

    public int getScore() {
        return score;
    }

    public int getHighScore() {
        return highScore;
    }

    public int getLives() {
        return lives;
    }

    public int getLevelNumber() {
        return levelIndex + 1;
    }

    public int getEnemiesLeft() {
        return spawnQueue.size() + enemies.size();
    }

    public boolean isFreezeActive() {
        return freezeTimer > 0;
    }
}
