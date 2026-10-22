package com.sbcs.detect.dungeon;

/**
 * The six dungeon reward chests, cheapest first. Pure. The order is load-bearing: the menu's bitmask
 * ({@code dungeonTiers}) uses {@link #ordinal()}. Each draws as a chest of its own block, the game's own texture.
 */
public enum ChestTier {
    WOOD("Wood", "oak_planks", 0xB08850),
    GOLD("Gold", "gold_block", 0xF4C430),
    DIAMOND("Diamond", "diamond_block", 0x5FE3E0),
    EMERALD("Emerald", "emerald_block", 0x3AD06A),
    OBSIDIAN("Obsidian", "obsidian", 0x8A55D8),
    BEDROCK("Bedrock", "bedrock", 0xB8B8B8);

    public final String title, block;
    public final int rgb;

    ChestTier(String title, String block, int rgb) {
        this.title = title;
        this.block = block;
        this.rgb = rgb;
    }

    public int bit() {
        return 1 << ordinal();
    }

    /** The default: Obsidian and Bedrock (Matej, 2026-10-22). */
    public static final int DEFAULT_BITS = OBSIDIAN.bit() | BEDROCK.bit();

    /**
     * A screen title or a Croesus head's name: "Obsidian Chest", "Obsidian", and nothing longer - Skyblocker's
     * ChestValue knows both shapes ("Hypixel broke dungeon chest titles"). Null otherwise.
     */
    public static ChestTier fromTitle(String s) {
        if (s == null) return null;
        String t = s.strip();
        if (t.endsWith(" Chest")) t = t.substring(0, t.length() - 6).strip();
        for (ChestTier c : values()) if (c.title.equals(t)) return c;
        return null;
    }
}
