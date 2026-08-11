package io.github.varshavsky64.tanks;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.Timer;

/** Панель игры: игровой цикл на Swing-таймере, ввод и вызов отрисовки. */
public final class GamePanel extends JPanel {

    private static final int FRAME_MS = 16;

    /**
     * Раскладка ЙЦУКЕН: символы кириллицы и латинские буквы на тех же физических клавишах.
     * При русской раскладке система отдаёт код кириллической буквы (или вовсе ничего),
     * поэтому клавиша ищется по напечатанному символу и переводится в латинский аналог.
     */
    private static final String CYRILLIC = "йцукенгшщзхъфывапролджэячсмитьбюё";
    private static final String LATIN = "qwertyuiop[]asdfghjkl;'zxcvbnm,.`";

    private final SoundEngine sound = new SoundEngine();
    private final World world = new World(sound);
    private final Renderer renderer = new Renderer();
    private final Set<Integer> pressed = new HashSet<>();
    private final Timer timer = new Timer(FRAME_MS, e -> tick());

    public GamePanel() {
        setPreferredSize(new Dimension(Renderer.SCREEN_WIDTH, Renderer.SCREEN_HEIGHT));
        setFocusable(true);
        setDoubleBuffered(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = keyCode(e);
                pressed.add(code);
                handleCommand(code);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                pressed.remove(keyCode(e));
            }
        });
    }

    public void start() {
        requestFocusInWindow();
        timer.start();
    }

    /** Код клавиши, не зависящий от раскладки: кириллица приводится к латинице по позиции. */
    private static int keyCode(KeyEvent e) {
        int index = CYRILLIC.indexOf(Character.toLowerCase(e.getKeyChar()));
        return index < 0 ? e.getKeyCode() : KeyEvent.getExtendedKeyCodeForChar(LATIN.charAt(index));
    }

    private void handleCommand(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_P -> world.togglePause();
            case KeyEvent.VK_M -> sound.toggleMute();
            case KeyEvent.VK_N -> sound.toggleMusic();
            case KeyEvent.VK_ENTER -> {
                if (world.getState() == GameState.GAME_OVER) {
                    world.restart();
                }
            }
            case KeyEvent.VK_ESCAPE -> {
                sound.close();
                System.exit(0);
            }
            default -> {
            }
        }
    }

    private void tick() {
        readInput();
        world.update();
        repaint();
    }

    private void readInput() {
        Input input = world.getInput();
        input.up = pressed.contains(KeyEvent.VK_UP) || pressed.contains(KeyEvent.VK_W);
        input.down = pressed.contains(KeyEvent.VK_DOWN) || pressed.contains(KeyEvent.VK_S);
        input.left = pressed.contains(KeyEvent.VK_LEFT) || pressed.contains(KeyEvent.VK_A);
        input.right = pressed.contains(KeyEvent.VK_RIGHT) || pressed.contains(KeyEvent.VK_D);
        input.fire = pressed.contains(KeyEvent.VK_SPACE) || pressed.contains(KeyEvent.VK_CONTROL);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        renderer.render((Graphics2D) g.create(), world);
    }
}
