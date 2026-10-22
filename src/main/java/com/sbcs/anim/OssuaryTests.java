package com.sbcs.anim;

import com.sbcs.detect.dungeon.ChestTier;
import com.sbcs.detect.dungeon.DungeonFloor;
import com.sbcs.detect.dungeon.DungeonPool;
import com.sbcs.detect.dungeon.Loot;
import com.sbcs.detect.dungeon.RewardChest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Made-up chests for "Try it" and {@code /sbcs test dungeon}: one per outcome, filled from the floor's own pool and
 * priced with fixed numbers, so a test plays exactly the band it names whatever the market says. Pure.
 */
public final class OssuaryTests {
    private OssuaryTests() {}

    /** The outcomes a test can ask for, in the order the menu offers them. */
    public static final String[] OUTCOMES = {"bust", "blue", "purple", "gold", "jackpot", "dye"};

    public record Test(RewardChest chest, Map<String, Long> prices) {}

    /**
     * @param outcome one of {@link #OUTCOMES}; "dye" on a floor without one (only F5 and F7 have dyes) is a jackpot
     */
    public static Test make(DungeonFloor floor, ChestTier tier, String outcome, DungeonPool pool) {
        List<DungeonPool.Entry> entries = pool.pool(floor, tier);
        Map<String, Long> prices = new HashMap<>();
        List<Loot> items = new ArrayList<>();
        int n = floor.number() >= 6 ? 5 : 4;
        // The small things: the first few of the pool that are not jackpots, at small prices.
        int set = Math.max(1, floor.number()) - 1;
        for (DungeonPool.Entry e : entries) {
            if (items.size() >= n - 1) break;
            String k = e.look().key();
            if (k == null || OssuaryTimeline.JACKPOTS.get(Math.min(6, set)).contains(k) || OssuaryTimeline.DYES.contains(k)) continue;
            items.add(e.look());
            prices.put(k, 40_000L + 70_000L * items.size());
        }
        items.add(new Loot("ESSENCE_WITHER", "Wither Essence", 12 + 4 * floor.number(), false, "minecraft:player_head",
                null, null, false, -1, null));
        prices.put("ESSENCE_WITHER", 2_500L);
        String best;
        long worth;
        switch (outcome) {
            case "bust" -> {
                best = null;
                worth = 0;
            }
            case "blue" -> {
                best = "RECOMBOBULATOR_3000";
                worth = 2_400_000;
            }
            case "purple" -> {
                best = firstOf(entries, set, false);
                worth = 9_000_000;
            }
            case "gold" -> {
                best = firstOf(entries, set, false);
                worth = 40_000_000;
            }
            case "dye" -> {
                best = floor.number() == 7 ? "DYE_NECRON" : floor.number() == 5 ? "DYE_LIVID" : firstOf(entries, set, true);
                worth = 900_000_000;
            }
            default -> {
                best = firstOf(entries, set, true);
                worth = 220_000_000;
            }
        }
        if (best != null) {
            Loot look = pool.look(best);
            items.add(0, look != null ? look : Loot.named(best, best, 1));
            prices.put(best, worth);
        }
        long cost = switch (tier) {
            case WOOD -> 0;
            case GOLD -> 100_000;
            case DIAMOND -> 250_000;
            case EMERALD -> 500_000;
            case OBSIDIAN -> 2_000_000;
            case BEDROCK -> 4_000_000;
        };
        prices.put("DUNGEON_CHEST_KEY", 1_200_000L);
        return new Test(new RewardChest(floor, tier, items, cost, false, false, -1), prices);
    }

    /** The first item of the pool that is (or is not) one of the floor's jackpots. */
    private static String firstOf(List<DungeonPool.Entry> entries, int set, boolean jackpot) {
        for (DungeonPool.Entry e : entries) {
            String k = e.look().key();
            if (k == null || OssuaryTimeline.DYES.contains(k)) continue;
            if (OssuaryTimeline.JACKPOTS.get(Math.min(6, set)).contains(k) == jackpot) return k;
        }
        return jackpot ? OssuaryTimeline.JACKPOTS.get(Math.min(6, set)).iterator().next() : "RECOMBOBULATOR_3000";
    }
}
