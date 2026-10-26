package com.sbcs.detect.superpairs;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What a Superpairs tile holds, as far as the reveal cares. Pure: read from what the menu shows - the tile's plain
 * name, its vanilla item, its SkyBlock data (id, a book's enchantments, a pet's type and tier) and its lore - so it can
 * be checked offline against recorded menus.
 *
 * <b>Theory until Matej's records:</b> the reward names below are the wiki's and the NEU repo's, not read off a live
 * Superpairs grid. What a revealed tile's name and lore look like there (the item's own name, or "Enchanted Book" with
 * the enchantment in the lore) is not known yet, so both are accepted.
 */
public record PairsReward(String key, String title, Tier tier, int colour, String priceId, String art) {

    /** How far up the table a reward is. Every tier from {@link #RARE} up is covered by a black box. */
    public enum Tier {
        /** Books VI and lower, bottles, XP: never covered, never revealed. */
        NORMAL,
        /** Experiment the Fish, the Metaphysical Serum, a Legendary Guardian: a short reveal. */
        RARE,
        /** A VII book (or higher), an endcap upgrade: the book rises and opens. */
        ULTRA,
        /** The Nadeshiko Dye: petals fill the screen. */
        DYE;

        public boolean covered() {
            return this != NORMAL;
        }
    }

    public boolean covered() {
        return tier.covered();
    }

    /** The NEU ids of the head rewards whose skins are bundled (superpairs_heads.json). */
    public static final String DYE_ID = "DYE_NADESHIKO", SERUM_ID = "METAPHYSICAL_SERUM", FISH_ID = "EXPERIMENT_THE_FISH",
            GUARDIAN_ID = "GUARDIAN;4";
    /** The Nadeshiko Dye's own pink (its name is §d in NEU; the dye itself is this). */
    public static final int NADESHIKO = 0xF6ADC6;

    private static final Pattern ROMAN = Pattern.compile("\\b(I{1,3}|IV|V|VI{0,3}|IX|X)\\b\\s*$");
    private static final Pattern ROMAN_ANY = Pattern.compile("([A-Z][A-Za-z' ]+?)\\s+(VII|VIII|IX|X)\\b");

    /**
     * Reads a tile. Never null: anything not recognised is {@link Tier#NORMAL} under its own name.
     *
     * @param name    the tile's plain name
     * @param item    the vanilla item id ("minecraft:enchanted_book")
     * @param sbId    the SkyBlock id from its custom data, or null
     * @param enchants a book's enchantments from its custom data, or null
     * @param petType a pet's type, or null
     * @param petTier a pet's tier ("LEGENDARY"), or null
     * @param lore    plain lore lines (never null)
     */
    public static PairsReward of(String name, String item, String sbId, Map<String, Integer> enchants, String petType,
                                 String petTier, List<String> lore) {
        String n = name == null ? "" : name.trim();
        String low = n.toLowerCase(Locale.ROOT);
        String id = sbId == null ? "" : sbId.toUpperCase(Locale.ROOT);

        if (id.equals(DYE_ID) || low.contains("nadeshiko dye")) {
            return new PairsReward("DYE_NADESHIKO", "Nadeshiko Dye", Tier.DYE, 0xFF55FF, DYE_ID, "head:" + DYE_ID);
        }
        if (id.equals(FISH_ID) || low.contains("experiment the fish")) {
            return new PairsReward(FISH_ID, "Experiment the Fish", Tier.RARE, 0xFF5555, FISH_ID, "fish");
        }
        if (id.equals(SERUM_ID) || low.contains("metaphysical serum")) {
            return new PairsReward(SERUM_ID, "Metaphysical Serum", Tier.RARE, 0xAA00AA, SERUM_ID, "head:" + SERUM_ID);
        }
        boolean guardian = "GUARDIAN".equalsIgnoreCase(petType) || low.matches(".*\\bguardian\\b.*") && (low.contains("lvl") || item != null && item.contains("head"));
        if (guardian) {
            boolean legendary = "LEGENDARY".equalsIgnoreCase(petTier) || "MYTHIC".equalsIgnoreCase(petTier)
                    || loreHas(lore, "LEGENDARY") || loreHas(lore, "MYTHIC");
            if (legendary) {
                return new PairsReward("PET_GUARDIAN_LEGENDARY", "Legendary Guardian", Tier.RARE, 0xFFAA00, "PET_GUARDIAN", "head:" + GUARDIAN_ID);
            }
            return normal(n);
        }

        // A book: its enchantments from the data when Hypixel puts them there, else a VII (or higher) in its name or lore.
        boolean book = item != null && item.contains("enchanted_book") || low.contains("enchanted book") || id.equals("ENCHANTED_BOOK");
        if (enchants != null) {
            for (Map.Entry<String, Integer> e : enchants.entrySet()) {
                if (e.getValue() != null && e.getValue() >= 7) {
                    String ench = e.getKey().toLowerCase(Locale.ROOT);
                    String title = pretty(ench) + " " + roman(e.getValue());
                    return new PairsReward("ENCHANTMENT_" + ench.toUpperCase(Locale.ROOT) + "_" + e.getValue(), title, Tier.ULTRA,
                            0xFF55FF, "ENCHANTMENT_" + ench.toUpperCase(Locale.ROOT) + "_" + e.getValue(), "book");
                }
            }
        }
        if (book || ROMAN_ANY.matcher(n).find()) {
            PairsReward r = bookFrom(n);
            if (r == null && book) {
                for (String l : lore) {
                    r = bookFrom(l.trim());
                    if (r != null) break;
                }
            }
            if (r != null) return r;
        }
        // An endcap upgrade, or anything else Hypixel marks ultra-rare in its lore (theory: the word on the tile).
        for (String l : lore) {
            String u = l.toUpperCase(Locale.ROOT);
            if (u.contains("ULTRA-RARE") || u.contains("ULTRA RARE")) {
                return new PairsReward("ULTRA:" + n, n, Tier.ULTRA, 0xFF55FF, sbId, "book");
            }
        }
        return normal(n);
    }

    private static PairsReward normal(String n) {
        return new PairsReward("NORMAL:" + n, n, Tier.NORMAL, 0xFFFFFF, null, null);
    }

    /** "Sharpness VII" (anything before VII or higher) as an ultra-rare book, or null. */
    static PairsReward bookFrom(String s) {
        Matcher m = ROMAN_ANY.matcher(s);
        if (!m.find()) return null;
        String ench = m.group(1).trim();
        int level = fromRoman(m.group(2));
        String keyName = ench.toUpperCase(Locale.ROOT).replace(' ', '_').replace("'", "");
        String id = "ENCHANTMENT_" + keyName + "_" + level;
        return new PairsReward(id, ench + " " + m.group(2), Tier.ULTRA, 0xFF55FF, id, "book");
    }

    static boolean loreHas(List<String> lore, String s) {
        for (String l : lore) if (l.toUpperCase(Locale.ROOT).contains(s)) return true;
        return false;
    }

    static String pretty(String ench) {
        StringBuilder b = new StringBuilder();
        for (String w : ench.split("_")) {
            if (w.isEmpty()) continue;
            if (b.length() > 0) b.append(' ');
            b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return b.toString();
    }

    static final String[] ROMANS = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    static String roman(int n) {
        return n > 0 && n < ROMANS.length ? ROMANS[n] : String.valueOf(n);
    }

    static int fromRoman(String r) {
        for (int i = 1; i < ROMANS.length; i++) if (ROMANS[i].equals(r)) return i;
        return 0;
    }

    /** Whether a name ends in a roman numeral (a tier word, for the narration). */
    static boolean hasLevel(String s) {
        return ROMAN.matcher(s).find();
    }

    /**
     * The odds the showcase prints, per experiment, or 0 for none. Only the VIIs are on the wiki (Superpairs page,
     * 2026-10-23: ~1 in 3,700 per particular book at Metaphysical); the rest are not given, so nothing is printed.
     */
    public long oneIn() {
        return tier == Tier.ULTRA && key.startsWith("ENCHANTMENT_") ? 3_700 : 0;
    }
}
