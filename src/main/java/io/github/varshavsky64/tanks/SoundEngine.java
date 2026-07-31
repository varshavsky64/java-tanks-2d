package io.github.varshavsky64.tanks;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Синтезатор эффектов в стиле 8 бит: все сэмплы считаются в память при старте,
 * а фоновый поток подмешивает активные голоса в одну звуковую линию.
 * Это позволяет накладывать звуки друг на друга, чего не умеет одиночный Clip.
 */
public final class SoundEngine implements Audio, AutoCloseable {

    private static final float SAMPLE_RATE = 44100f;
    private static final int CHUNK_FRAMES = 512;
    private static final AudioFormat FORMAT =
            new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    private final Map<Sound, float[]> samples = new EnumMap<>(Sound.class);
    private final List<Voice> voices = new ArrayList<>();
    private final Random random = new Random();

    private SourceDataLine line;
    private Thread mixer;
    private volatile boolean running;
    private volatile boolean muted;

    public SoundEngine() {
        for (Sound sound : Sound.values()) {
            samples.put(sound, synthesize(sound));
        }
        try {
            line = AudioSystem.getSourceDataLine(FORMAT);
            line.open(FORMAT, CHUNK_FRAMES * 2 * 4);
            line.start();
        } catch (LineUnavailableException | IllegalArgumentException e) {
            line = null;
            return;
        }
        running = true;
        mixer = new Thread(this::mixLoop, "sound-mixer");
        mixer.setDaemon(true);
        mixer.start();
    }

    @Override
    public void play(Sound sound) {
        if (line == null || muted) {
            return;
        }
        float[] data = samples.get(sound);
        // Небольшая случайная расстройка громкости, чтобы повторы не звучали механически.
        float gain = 0.85f + random.nextFloat() * 0.3f;
        synchronized (voices) {
            if (voices.size() >= 16) {
                voices.remove(0);
            }
            voices.add(new Voice(data, gain));
        }
    }

    public void toggleMute() {
        muted = !muted;
        if (muted) {
            synchronized (voices) {
                voices.clear();
            }
        }
    }

    public boolean isMuted() {
        return muted;
    }

    public boolean isAvailable() {
        return line != null;
    }

    @Override
    public void close() {
        running = false;
        if (mixer != null) {
            mixer.interrupt();
        }
        if (line != null) {
            line.stop();
            line.close();
        }
    }

    private void mixLoop() {
        float[] mix = new float[CHUNK_FRAMES];
        byte[] out = new byte[CHUNK_FRAMES * 2];
        while (running) {
            java.util.Arrays.fill(mix, 0f);
            synchronized (voices) {
                voices.removeIf(voice -> voice.mixInto(mix));
            }
            for (int i = 0; i < CHUNK_FRAMES; i++) {
                int value = Math.round(Math.max(-1f, Math.min(1f, mix[i])) * Short.MAX_VALUE);
                out[i * 2] = (byte) (value & 0xFF);
                out[i * 2 + 1] = (byte) ((value >> 8) & 0xFF);
            }
            line.write(out, 0, out.length);
        }
    }

    /** Один проигрываемый сэмпл. */
    private static final class Voice {
        private final float[] data;
        private final float gain;
        private int position;

        Voice(float[] data, float gain) {
            this.data = data;
            this.gain = gain;
        }

        /** @return true, когда голос доиграл и его можно убрать. */
        boolean mixInto(float[] mix) {
            int n = Math.min(mix.length, data.length - position);
            for (int i = 0; i < n; i++) {
                mix[i] += data[position + i] * gain;
            }
            position += n;
            return position >= data.length;
        }
    }

    // --- синтез -------------------------------------------------------------

    private static float[] synthesize(Sound sound) {
        return switch (sound) {
            case PLAYER_SHOT -> {
                float[] buf = buffer(0.10);
                sweep(buf, 0, 0.10, 950, 260, 0.32, true, 2.5);
                yield buf;
            }
            case ENEMY_SHOT -> {
                float[] buf = buffer(0.09);
                sweep(buf, 0, 0.09, 520, 180, 0.14, true, 2.5);
                yield buf;
            }
            case HIT_BRICK -> {
                float[] buf = buffer(0.09);
                noise(buf, 0, 0.09, 0.28, 4.0);
                sweep(buf, 0, 0.05, 300, 120, 0.14, true, 3.0);
                yield buf;
            }
            case HIT_STEEL -> {
                float[] buf = buffer(0.16);
                sweep(buf, 0, 0.16, 2100, 1500, 0.16, false, 5.0);
                sweep(buf, 0, 0.12, 3300, 2600, 0.10, false, 6.0);
                noise(buf, 0, 0.04, 0.12, 5.0);
                yield buf;
            }
            case EXPLOSION -> {
                float[] buf = buffer(0.45);
                noise(buf, 0, 0.45, 0.42, 3.0);
                sweep(buf, 0, 0.35, 160, 55, 0.30, false, 3.0);
                yield buf;
            }
            case BIG_EXPLOSION -> {
                float[] buf = buffer(0.85);
                noise(buf, 0, 0.85, 0.5, 2.0);
                sweep(buf, 0, 0.7, 130, 38, 0.40, false, 2.0);
                sweep(buf, 0.05, 0.5, 90, 30, 0.25, true, 2.5);
                yield buf;
            }
            case POWERUP_APPEAR -> {
                float[] buf = buffer(0.30);
                sweep(buf, 0.00, 0.12, 880, 880, 0.18, true, 1.5);
                sweep(buf, 0.15, 0.12, 1320, 1320, 0.18, true, 1.5);
                yield buf;
            }
            case POWERUP_TAKE -> {
                float[] buf = buffer(0.42);
                double[] notes = {659, 784, 988, 1319};
                for (int i = 0; i < notes.length; i++) {
                    sweep(buf, i * 0.09, 0.10, notes[i], notes[i], 0.20, true, 2.0);
                }
                yield buf;
            }
            case LEVEL_START -> {
                float[] buf = buffer(0.75);
                double[] notes = {523, 659, 784, 1047};
                for (int i = 0; i < notes.length; i++) {
                    sweep(buf, i * 0.15, 0.17, notes[i], notes[i], 0.20, true, 1.6);
                }
                yield buf;
            }
            case LEVEL_CLEAR -> {
                float[] buf = buffer(1.15);
                double[] notes = {523, 659, 784, 1047, 1319};
                for (int i = 0; i < notes.length; i++) {
                    sweep(buf, i * 0.13, 0.16, notes[i], notes[i], 0.20, true, 1.4);
                }
                sweep(buf, 0.70, 0.42, 1568, 1568, 0.22, true, 1.2);
                yield buf;
            }
            case LIFE_LOST -> {
                float[] buf = buffer(0.55);
                sweep(buf, 0, 0.55, 620, 120, 0.26, true, 2.0);
                yield buf;
            }
            case GAME_OVER -> {
                float[] buf = buffer(1.6);
                double[] notes = {523, 494, 440, 392, 330, 262};
                for (int i = 0; i < notes.length; i++) {
                    sweep(buf, i * 0.22, 0.24, notes[i], notes[i] * 0.98, 0.22, true, 1.5);
                }
                yield buf;
            }
        };
    }

    private static float[] buffer(double seconds) {
        return new float[(int) (seconds * SAMPLE_RATE)];
    }

    /**
     * Подмешивает тон со скольжением частоты и экспоненциальным затуханием.
     *
     * @param square true — меандр (характерный «чиптюн»), false — синус
     * @param decay  чем больше, тем резче спад громкости
     */
    private static void sweep(float[] buf, double start, double duration,
                              double fromHz, double toHz, double gain, boolean square, double decay) {
        int from = (int) (start * SAMPLE_RATE);
        int count = (int) (duration * SAMPLE_RATE);
        double phase = 0;
        for (int i = 0; i < count && from + i < buf.length; i++) {
            double t = (double) i / count;
            double freq = fromHz + (toHz - fromHz) * t;
            phase += 2 * Math.PI * freq / SAMPLE_RATE;
            double wave = square ? (Math.sin(phase) >= 0 ? 1 : -1) : Math.sin(phase);
            buf[from + i] += (float) (wave * gain * Math.exp(-decay * t));
        }
    }

    private static void noise(float[] buf, double start, double duration,
                              double gain, double decay) {
        int from = (int) (start * SAMPLE_RATE);
        int count = (int) (duration * SAMPLE_RATE);
        Random rnd = new Random(12345);
        double smoothed = 0;
        for (int i = 0; i < count && from + i < buf.length; i++) {
            double t = (double) i / count;
            // Простое сглаживание убирает шипение и делает шум похожим на взрыв.
            smoothed = smoothed * 0.6 + (rnd.nextDouble() * 2 - 1) * 0.4;
            buf[from + i] += (float) (smoothed * gain * Math.exp(-decay * t));
        }
    }
}
