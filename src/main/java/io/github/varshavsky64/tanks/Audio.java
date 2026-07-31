package io.github.varshavsky64.tanks;

/** Выход для звука. Позволяет гонять логику мира без звуковой карты. */
public interface Audio {

    Audio SILENT = sound -> {
    };

    void play(Sound sound);
}
