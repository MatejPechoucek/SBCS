package com.sbcs.detect.nucleus;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * What a Crystal Nucleus Loot Bundle can give up, and how often.
 *
 * <h2>Where these numbers come from</h2>
 *
 * The wiki's Crystal Nucleus page (read 2026-10-22), 65 weighted rows. It prints a total of
 * 311,212, but its rows add to {@link #TOTAL_WEIGHT} 307,212 and every percentage beside them
 * is against that, so the printed total is a typo. Rows that differ only by stack size
 * (Flawed Ruby x6 / x12 / x24 / x40) are one item here with their weights summed, because the
 * loot message names the item and the count, not the row. The Jade Dye is not in the weights:
 * each roll has its own 1 in 500,000 of becoming one.
 *
 * Names and colours are the NEU item repository's {@code displayname}s. The gemstones carry a
 * symbol there (and in chat), which {@link #key} throws away.
 *
 * <h2>Tiers</h2>
 *
 * Four, worst to best, and the order is load-bearing ({@link NucleusHaul#best()} compares on
 * ordinal). {@code RARE} is the Quick Claw and an extra Fine Jasper; {@code JACKPOT} the seven
 * Flawless gemstones and Divan's Alloy, 112 of 307,212 between them; {@code DYE} the Jade Dye.
 *
 * <h2>The guaranteed Fine gemstones</h2>
 *
 * Every bundle also gives one Fine gemstone of each Crystal Hollows colour - Ruby, Amethyst,
 * Jade, Amber, Sapphire, Topaz and Jasper (the wiki excludes only Opal, Onyx, Aquamarine,
 * Citrine and Peridot). So one Fine Jasper is always there and is no drop at all; only a
 * second one is the weight-200 roll. Whether Hypixel prints the two as one "x2" line or as two
 * lines is not known yet, so {@link NucleusHaul} counts the total either way.
 */
public final class NucleusLoot {
    private NucleusLoot() {}

    public enum Tier {
        COMMON(0xFFBFC6D4),
        RARE(0xFF55FFFF),
        JACKPOT(0xFFFFAA00),
        DYE(0xFF00AA00);

        private final int glow;

        Tier(int glow) { this.glow = glow; }

        /** The colour the reveal lights up in. Not the item's own rarity colour. */
        public int glow() { return glow; }
    }

    /** Where a drop sells. */
    public enum Market { BAZAAR, AUCTION, NONE }

    /**
     * One item of the table. {@code weight} is per roll against {@link #TOTAL_WEIGHT}, 0 for a
     * guaranteed reward or an item we have never heard of; the Jade Dye's is {@link #DYE_WEIGHT}.
     */
    public record Entry(String name, String id, int weight, Tier tier, int colour, Market market) {
        /** Chance this item is the result of any one roll. */
        public double perRoll() {
            return tier == Tier.DYE ? 1.0 / DYE_ONE_IN : weight / (double) TOTAL_WEIGHT;
        }
    }

    /** The weights below add up to this. */
    public static final int TOTAL_WEIGHT = 307_212;
    /** The Jade Dye's separate chance per roll. */
    public static final int DYE_ONE_IN = 500_000;
    /** Marks the Dye's row; it is not part of the weighted table. */
    public static final int DYE_WEIGHT = -1;

    /** Rolls per bundle: 14 to 19, up to 21 with the bonuses. */
    public static final int MIN_ROLLS = 14, MAX_ROLLS = 21;
    public static final double AVG_ROLLS = 16.5;

    public static final int UNKNOWN_COLOUR = 0xFFFFFFFF;

    /** The seven colours each bundle gives one Fine gemstone of. */
    public static final String[] GUARANTEED_FINE = {"Ruby", "Amethyst", "Jade", "Amber", "Sapphire", "Topaz", "Jasper"};

    private static final int DARK_PURPLE = 0xFFAA00AA, BLUE = 0xFF5555FF, GREEN = 0xFF55FF55, GOLD = 0xFFFFAA00,
            PINK = 0xFFFF55FF, WHITE = 0xFFFFFFFF, RED = 0xFFFF5555, DARK_GREEN = 0xFF00AA00;

    private static final Map<String, Entry> BY_NAME = new LinkedHashMap<>();

    private static void add(String name, String id, int weight, Tier tier, int colour, Market market) {
        BY_NAME.put(key(name), new Entry(name, id, weight, tier, colour, market));
    }

    private static final String[] COLOURS = {"Ruby", "Amethyst", "Jade", "Amber", "Sapphire", "Topaz"};

    static {
        add("Jade Dye", "DYE_JADE", DYE_WEIGHT, Tier.DYE, DARK_GREEN, Market.AUCTION);

        add("Flawless Jasper Gemstone", "FLAWLESS_JASPER_GEM", 5, Tier.JACKPOT, DARK_PURPLE, Market.BAZAAR);
        for (String c : COLOURS) {
            add("Flawless " + c + " Gemstone", "FLAWLESS_" + c.toUpperCase(Locale.ROOT) + "_GEM", 15,
                    Tier.JACKPOT, DARK_PURPLE, Market.BAZAAR);
        }
        add("Divan's Alloy", "DIVAN_ALLOY", 17, Tier.JACKPOT, GOLD, Market.AUCTION);

        add("Quick Claw", "PET_ITEM_QUICK_CLAW", 50, Tier.RARE, GOLD, Market.AUCTION);
        // RARE only beyond the guaranteed one - see the class comment and NucleusHaul.
        add("Fine Jasper Gemstone", "FINE_JASPER_GEM", 200, Tier.RARE, BLUE, Market.BAZAAR);

        add("Treasurite", "TREASURITE", 200 + 2_000, Tier.COMMON, DARK_PURPLE, Market.BAZAAR);
        add("Recall Potion", "RECALL_POTION", 250, Tier.COMMON, DARK_PURPLE, Market.AUCTION);
        add("Wishing Compass", "WISHING_COMPASS", 500, Tier.COMMON, GREEN, Market.BAZAAR);
        add("Precious Pearl", "PRECIOUS_PEARL", 500, Tier.COMMON, DARK_PURPLE, Market.BAZAAR);
        add("Claw Fossil", "CLAW_FOSSIL", 500, Tier.COMMON, GOLD, Market.AUCTION);
        add("Flawed Jasper Gemstone", "FLAWED_JASPER_GEM", 700 + 2_000 + 5_000 + 10_000, Tier.COMMON, GREEN, Market.BAZAAR);
        add("Gemstone Mixture", "GEMSTONE_MIXTURE", 700, Tier.COMMON, BLUE, Market.BAZAAR);
        add("Fortune IV", "ENCHANTMENT_FORTUNE_4", 700, Tier.COMMON, WHITE, Market.BAZAAR);
        add("Gold Essence", "ESSENCE_GOLD", 750, Tier.COMMON, PINK, Market.BAZAAR);
        add("Diamond Essence", "ESSENCE_DIAMOND", 750, Tier.COMMON, PINK, Market.BAZAAR);
        for (String c : COLOURS) {
            add("Fine " + c + " Gemstone", "FINE_" + c.toUpperCase(Locale.ROOT) + "_GEM", 900, Tier.COMMON, BLUE, Market.BAZAAR);
        }
        add("Jaderald", "JADERALD", 1_450, Tier.COMMON, DARK_PURPLE, Market.AUCTION);
        add("Dwarven O's Gemstone Grahams", "DWARVEN_OS_GEMSTONE_GRAHAMS", 1_450, Tier.COMMON, DARK_PURPLE, Market.BAZAAR);
        for (String c : COLOURS) {
            add("Flawed " + c + " Gemstone", "FLAWED_" + c.toUpperCase(Locale.ROOT) + "_GEM", 2_000 + 5_000 + 10_000 + 20_000,
                    Tier.COMMON, GREEN, Market.BAZAAR);
        }
        add("Ruby Crystal", "RUBY_CRYSTAL", 2_500, Tier.COMMON, RED, Market.NONE);
        add("Jasper Crystal", "JASPER_CRYSTAL", 2_500, Tier.COMMON, PINK, Market.NONE);
        add("Helix Fossil", "HELIX", 4_000, Tier.COMMON, GOLD, Market.AUCTION);
        add("Lapidary I", "ENCHANTMENT_LAPIDARY_1", 4_000, Tier.COMMON, WHITE, Market.BAZAAR);
        add("Prehistoric Egg", "PREHISTORIC_EGG", 5_000, Tier.COMMON, WHITE, Market.AUCTION);
        add("Divan Fragment", "DIVAN_FRAGMENT", 6_000, Tier.COMMON, DARK_PURPLE, Market.BAZAAR);
        add("Oil Barrel", "OIL_BARREL", 8_000, Tier.COMMON, GREEN, Market.BAZAAR);
        add("Pickonimbus 2000", "PICKONIMBUS", 10_000, Tier.COMMON, DARK_PURPLE, Market.AUCTION);
        add("Bob-omb", "BOB_OMB", 10_000, Tier.COMMON, BLUE, Market.BAZAAR);
    }

    /**
     * Lower-cased, letters, digits and spaces only, single-spaced. Drops the gemstones' symbols,
     * and turns "Enchanted Book (Lapidary I)" into the enchantment's own name.
     */
    static String key(String name) {
        String k = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", "").replaceAll("\\s+", " ").trim();
        if (k.startsWith("enchanted book ") && k.length() > "enchanted book ".length()) {
            k = k.substring("enchanted book ".length());
        }
        return k;
    }

    /** The table row for a chat name, or null when nothing matches. */
    public static Entry find(String chatName) {
        return BY_NAME.get(key(chatName));
    }

    /** The row for a chat name, falling back to a common of unknown weight. */
    public static Entry findOrCommon(String chatName) {
        Entry known = find(chatName);
        return known != null ? known : new Entry(chatName, null, 0, Tier.COMMON, UNKNOWN_COLOUR, Market.NONE);
    }

    /** Every row, rarest first. */
    public static Iterable<Entry> all() { return BY_NAME.values(); }

    /** Not a roll and not loot to show: powder, HotM experience, a bare unnamed book. */
    public static boolean ignored(String chatName) {
        String k = key(chatName);
        return k.endsWith(" powder") || k.contains("heart of the mountain") || k.contains("hotm")
                || k.endsWith(" experience") || k.endsWith(" exp") || k.endsWith(" xp");
    }

    /**
     * Chance that one bundle of {@code rolls} rolls carries at least one drop of this tier or
     * better - the "1 in ~N bundles" of the reveal is its inverse.
     */
    public static double atLeast(Tier tier, double rolls) {
        double perRoll = 0;
        for (Entry e : BY_NAME.values()) {
            if (e.tier().ordinal() >= tier.ordinal()) perRoll += e.perRoll();
        }
        return 1 - Math.pow(1 - perRoll, rolls);
    }

    /** Chance one bundle carries this item at least once. */
    public static double perBundle(Entry e, double rolls) {
        return 1 - Math.pow(1 - e.perRoll(), rolls);
    }
}
