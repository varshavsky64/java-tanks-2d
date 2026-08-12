package io.github.varshavsky64.tanks;

/** Выход для звука. Позволяет гонять логику мира без звуковой карты. */
@FunctionalInterface
public interface Audio {

    Audio SILENT = sound -> {
    };

    void play(Sound sound);
}
