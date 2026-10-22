package com.sbcs.detect.dungeon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * A chest priced: every item's worth, the best one, the total, and the profit after its cost. Pure - prices come
 * through {@code price} (key -> coins a piece, or null if not known yet), so the game's side can use the bazaar and
 * Coflnet and a test its own table.
 */
public record ChestValue(RewardChest chest, List<Long> worth, int best, long total, long profit, boolean incomplete) {

    /** The Dungeon Chest Key's own key, for a chest that needs one. */
    public static final String KEY_ITEM = "DUNGEON_CHEST_KEY";

    public static ChestValue of(RewardChest chest, Function<String, Long> price) {
        List<Long> worth = new ArrayList<>();
        long total = 0;
        boolean incomplete = false;
        int best = -1;
        long bestWorth = -1;
        for (int i = 0; i < chest.items().size(); i++) {
            Loot l = chest.items().get(i);
            Long p = l.key() == null ? null : price.apply(l.key());
            if (p == null) incomplete = true;
            long w = p == null ? 0 : p * l.count();
            worth.add(w);
            total += w;
            // The headline is the best thing that is not essence - essence only if there is nothing else.
            long rank = l.essence() ? w / 1000 : w;
            if (best < 0 || rank > bestWorth) {
                best = i;
                bestWorth = rank;
            }
        }
        long cost = chest.cost();
        if (chest.key()) {
            Long k = price.apply(KEY_ITEM);
            if (k == null) incomplete = true;
            else cost += k;
        }
        return new ChestValue(chest, worth, best, total, total - cost, incomplete);
    }

    public Loot bestItem() {
        return best < 0 ? null : chest.items().get(best);
    }

    public long bestWorth() {
        return best < 0 ? 0 : worth.get(best);
    }

    /** Item indices, most valuable first. */
    public List<Integer> ranked() {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < worth.size(); i++) idx.add(i);
        idx.sort(Comparator.comparingLong((Integer i) -> -worth.get(i)));
        return idx;
    }

    /**
     * The chest a Croesus run rolls: the most profitable of those that may roll, the least negative if every one
     * loses (Matej, 2026-10-22). Null if none may.
     */
    public static ChestValue bestOf(List<ChestValue> chests) {
        ChestValue best = null;
        for (ChestValue v : chests) if (best == null || v.profit > best.profit) best = v;
        return best;
    }
}
