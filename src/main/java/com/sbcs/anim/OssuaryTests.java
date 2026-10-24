package com.sbcs.anim;

import com.sbcs.detect.dungeon.ChestTier;
import com.sbcs.detect.dungeon.DungeonFloor;
import com.sbcs.detect.dungeon.DungeonPool;
import com.sbcs.detect.dungeon.Loot;
import com.sbcs.detect.dungeon.RewardChest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Made-up chests for "Try it" and {@code /sbcs test dungeon}, filled from the floor's real pool for that chest and
 * priced by the market: the outcome only decides which of the pool's items is the best one. An item with no market
 * price yet (or offline) falls back to the wiki's added cost - Hypixel's own idea of its worth - so an outcome the
 * floor cannot reach at real prices plays what its nearest item really is, never a made-up number. Pure.
 */
public final class OssuaryTests {
    private OssuaryTests() {}

    /** The outcomes a test can ask for, in the order the menu offers them. */
    public static final String[] OUTCOMES = {"bust", "blue", "purple", "gold", "jackpot", "dye"};

    /**
     * @param prices the fallbacks for items the market has not priced (the roll asks the market first)
     */
    public record Test(RewardChest chest, Map<String, Long> prices) {}

    /** What an item with neither a market price nor an added cost is taken to be worth: a cheap book's worth. */
    static final long CHEAP = 60_000;
    /** The dyes when the market has not answered: roughly what they sold for in October 2026. */
    static final Map<String, Long> DYE_GUESS = Map.of("DYE_LIVID", 600_000_000L, "DYE_NECRON", 900_000_000L);
    static final long WITHER_ESSENCE = 2_500;

    /** The chest a floor's test uses when none is named: Bedrock where it exists (Floor V up), else Obsidian. */
    public static ChestTier defaultTier(DungeonFloor floor) {
        return floor != null && floor.number() >= 5 ? ChestTier.BEDROCK : ChestTier.OBSIDIAN;
    }

    /** The wiki's base price of a chest: F1 and F2 are cheaper, Bedrock is 2M everywhere it exists. */
    public static long baseCost(DungeonFloor floor, ChestTier tier) {
        int n = floor == null ? 7 : Math.max(1, floor.number());
        long[] f1 = {0, 25_000, 50_000, 100_000, 250_000}, f2 = {0, 50_000, 100_000, 200_000, 500_000},
                rest = {0, 100_000, 250_000, 500_000, 1_000_000};
        if (tier == ChestTier.BEDROCK) return 2_000_000;
        return (n == 1 ? f1 : n == 2 ? f2 : rest)[tier.ordinal()];
    }

    /**
     * @param outcome one of {@link #OUTCOMES}; "dye" on a floor without one (only M5 and M7 have dyes) is a jackpot
     * @param market  an item's market price, or null when it has none yet
     */
    public static Test make(DungeonFloor floor, ChestTier tier, String outcome, DungeonPool pool, Function<String, Long> market) {
        List<DungeonPool.Entry> entries = pool.pool(floor, tier);
        int set = Math.min(6, Math.max(1, floor.number()) - 1);
        Set<String> jackpots = OssuaryTimeline.JACKPOTS.get(set);
        Map<String, Long> fallback = new HashMap<>();
        Function<DungeonPool.Entry, Long> worth = e -> {
            String k = e.look().key();
            Long m = market == null ? null : market.apply(k);
            return m != null ? m : e.cost() > 0 ? e.cost() : CHEAP;
        };
        for (DungeonPool.Entry e : entries) {
            String k = e.look().key();
            if (k != null) fallback.put(k, e.cost() > 0 ? e.cost() : CHEAP);
        }

        // The best item: the pool's own, picked for the band asked for.
        DungeonPool.Entry best = null;
        String bestKey = null;
        switch (outcome) {
            case "bust" -> { }
            case "jackpot" -> best = jackpot(entries, jackpots, worth);
            case "dye" -> {
                bestKey = floor.master() && floor.number() == 7 ? "DYE_NECRON" : floor.master() && floor.number() == 5 ? "DYE_LIVID" : null;
                if (bestKey == null) best = jackpot(entries, jackpots, worth);
            }
            default -> {
                long lo = switch (outcome) {
                    case "blue" -> 1_000_000;
                    case "purple" -> OssuaryTimeline.PURPLE_AT;
                    default -> OssuaryTimeline.GOLD_AT;
                };
                long hi = switch (outcome) {
                    case "blue" -> OssuaryTimeline.PURPLE_AT;
                    case "purple" -> OssuaryTimeline.GOLD_AT;
                    default -> OssuaryTimeline.JACKPOT_AT;
                };
                best = nearest(entries, jackpots, worth, lo, hi);
            }
        }
        if (best != null) bestKey = best.look().key();
        long bestWorth = best != null ? worth.apply(best) : 0;

        // The rest: the pool's commonest items worth less than the best (and, for a bust, under 1M), most likely first.
        List<Loot> items = new ArrayList<>();
        long cap = bestKey == null ? 1_000_000 : Math.min(1_000_000, Math.max(1, bestWorth));
        List<DungeonPool.Entry> byWeight = new ArrayList<>(entries);
        byWeight.sort(Comparator.comparingInt(DungeonPool.Entry::weight).reversed());
        int n = floor.number() >= 6 ? 4 : 3;
        long added = 0;
        for (DungeonPool.Entry e : byWeight) {
            if (items.size() >= n) break;
            String k = e.look().key();
            if (k == null || k.equals(bestKey) || jackpots.contains(k) || worth.apply(e) >= cap) continue;
            items.add(e.look());
            added += e.cost();
        }
        if (bestKey != null) {
            Loot look = best != null ? best.look() : pool.look(bestKey);
            items.add(0, look != null ? look : Loot.named(bestKey, bestKey, 1));
            if (best != null) added += best.cost();
            if (best == null) fallback.put(bestKey, DYE_GUESS.getOrDefault(bestKey, 900_000_000L));
        }
        items.add(new Loot("ESSENCE_WITHER", "Wither Essence", 12 + 4 * floor.number(), false, "minecraft:player_head",
                null, null, false, -1, null));
        fallback.put("ESSENCE_WITHER", WITHER_ESSENCE);
        fallback.put("DUNGEON_CHEST_KEY", 1_200_000L);
        long cost = baseCost(floor, tier) + added;
        return new Test(new RewardChest(floor, tier, items, cost, false, false, -1), fallback);
    }

    /** The floor's priciest jackpot in this pool (it is what "jackpot" is there to show), or null if it has none. */
    private static DungeonPool.Entry jackpot(List<DungeonPool.Entry> entries, Set<String> jackpots,
                                             Function<DungeonPool.Entry, Long> worth) {
        DungeonPool.Entry best = null;
        for (DungeonPool.Entry e : entries) {
            if (jackpots.contains(e.look().key()) && (best == null || worth.apply(e) > worth.apply(best))) best = e;
        }
        return best;
    }

    /**
     * The likeliest non-jackpot item worth between lo and hi; when none is, the one closest to that range (by ratio),
     * so the test plays what the floor really has.
     */
    private static DungeonPool.Entry nearest(List<DungeonPool.Entry> entries, Set<String> jackpots,
                                             Function<DungeonPool.Entry, Long> worth, long lo, long hi) {
        DungeonPool.Entry in = null, near = null;
        double nearBy = Double.MAX_VALUE;
        for (DungeonPool.Entry e : entries) {
            String k = e.look().key();
            if (k == null || jackpots.contains(k) || OssuaryTimeline.DYES.contains(k)) continue;
            long w = worth.apply(e);
            if (w >= lo && w < hi) {
                if (in == null || e.weight() > in.weight()) in = e;
                continue;
            }
            double by = w < lo ? (double) lo / Math.max(1, w) : (double) w / hi;
            if (by < nearBy) {
                nearBy = by;
                near = e;
            }
        }
        return in != null ? in : near;
    }
}
