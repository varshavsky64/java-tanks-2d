package io.github.varshavsky64.tanks;

/** Типы клеток игрового поля. */
public enum TileType {
    /** Пустота — можно ехать и стрелять сквозь. */
    EMPTY,
    /** Кирпич — разрушается любым снарядом. */
    BRICK,
    /** Броня — разрушается только улучшенным снарядом. */
    STEEL,
    /** Вода — танки не проезжают, снаряды пролетают. */
    WATER,
    /** Кусты — рисуются поверх танков, ничего не блокируют. */
    TREES,
    /** Лёд — танки скользят (движение не блокирует). */
    ICE,
    /** База (орёл) — попадание означает поражение. */
    BASE;

    public boolean blocksTank() {
        return this == BRICK || this == STEEL || this == WATER || this == BASE;
    }

    public boolean blocksBullet() {
        return this == BRICK || this == STEEL || this == BASE;
    }
}
