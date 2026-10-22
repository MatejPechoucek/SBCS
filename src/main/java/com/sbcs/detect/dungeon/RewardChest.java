package com.sbcs.detect.dungeon;

import java.util.ArrayList;
import java.util.List;

/**
 * A dungeon reward chest whose contents are known. Pure. {@code cost} is its coin price (0 for the free Wood chest
 * or one already paid), {@code key} whether a Dungeon Chest Key is also needed, {@code opened} whether it was already
 * claimed (a Croesus page shows those too), {@code slot} the head it sits on at Croesus (-1 in a run).
 */
public record RewardChest(DungeonFloor floor, ChestTier tier, List<Loot> items, long cost, boolean key,
                          boolean opened, int slot) {

    /**
     * What makes a chest the same chest, in the boss room and later at Croesus: floor, tier and the sorted contents
     * (each by its price key where known, else its name). A Kismet reroll changes the contents, so it rolls again.
     */
    public String fingerprint() {
        List<String> parts = new ArrayList<>();
        for (Loot l : items) parts.add((l.key() != null ? l.key() : PriceKeys.clean(l.name())) + "x" + l.count());
        parts.sort(null);
        return (floor == null ? "?" : floor.label()) + "|" + tier.name() + "|" + String.join(",", parts);
    }
}
