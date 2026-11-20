package ru.kotmiks.holo.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Выдача id для пакетных сущностей. Диапазон выбран высоко, чтобы не пересекаться
 * со счётчиком id настоящих сущностей сервера.
 */
public final class EntityIds {

    private static final int START = 1_500_000_000;
    private static final int LIMIT = 2_100_000_000;
    private static final AtomicInteger NEXT = new AtomicInteger(START);

    private EntityIds() {
    }

    public static int next() {
        int id = NEXT.getAndIncrement();
        if (id >= LIMIT) {
            NEXT.set(START);
            return NEXT.getAndIncrement();
        }
        return id;
    }
}
