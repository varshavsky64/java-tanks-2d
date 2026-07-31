package io.github.varshavsky64.tanks;

/** Фазы игрового цикла. */
public enum GameState {
    /** Заставка перед уровнем. */
    READY,
    PLAYING,
    PAUSED,
    LEVEL_COMPLETE,
    GAME_OVER
}
