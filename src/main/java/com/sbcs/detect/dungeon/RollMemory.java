package com.sbcs.detect.dungeon;

import java.util.HashMap;
import java.util.Map;

/**
 * Which chests have already rolled, by {@link RewardChest#fingerprint()}, so a chest rolled in the boss room shows
 * straight away at Croesus and no chest rolls twice. Croesus keeps a run's chests for 72 hours; so does this. Pure:
 * the game's side saves {@link #entries()} and loads it back.
 */
public final class RollMemory {
    public static final long KEEP_MS = 72L * 3600 * 1000;

    private final Map<String, Long> rolled = new HashMap<>();

    public synchronized boolean rolled(String fingerprint, long now) {
        Long at = rolled.get(fingerprint);
        return at != null && now - at < KEEP_MS;
    }

    public synchronized void remember(String fingerprint, long now) {
        rolled.put(fingerprint, now);
        rolled.values().removeIf(at -> now - at >= KEEP_MS);
    }

    public synchronized void forget(String fingerprint) {
        rolled.remove(fingerprint);
    }

    public synchronized Map<String, Long> entries() {
        return new HashMap<>(rolled);
    }

    public synchronized void load(Map<String, Long> from, long now) {
        rolled.clear();
        from.forEach((k, v) -> {
            if (k != null && v != null && now - v < KEEP_MS) rolled.put(k, v);
        });
    }
}
