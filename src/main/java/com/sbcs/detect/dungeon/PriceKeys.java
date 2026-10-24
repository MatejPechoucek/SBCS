package com.sbcs.detect.dungeon;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What a chest's item is priced by. Pure. From the item's own data in a chest menu (its SkyBlock id, a book's one
 * enchantment, a pet's type and rarity), or from its name alone - all a Croesus head's "Contents" list gives.
 * The id shapes are the ones the bazaar and Coflnet use, and Skyblocker's CroesusProfit prices the same ones.
 */
public final class PriceKeys {
    private PriceKeys() {}

    /** Ultimate enchantments whose names drop the "Ultimate" (all but Wise and Jerry). */
    static final Set<String> ULTIMATE = Set.of("bank", "chimera", "combo", "duplex", "fatal_tempo", "flash",
            "habanero_tactics", "inferno", "last_stand", "legion", "no_pain_no_gain", "one_for_all", "refrigerate",
            "rend", "soul_eater", "swarm", "the_one", "wisdom");
    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private static final Pattern COUNT = Pattern.compile("^(.*?)\\s+x(\\d+)$");
    private static final Pattern ESSENCE = Pattern.compile("^([A-Za-z]+) Essence$");
    private static final Pattern BOOK = Pattern.compile("^Enchanted Book \\((.+)\\)$");
    private static final Pattern LEVEL = Pattern.compile("^(.+?) ([IVX]+)$");

    /** The key from an item's data; null when it has none worth pricing. */
    public static String fromData(String id, Map<String, Integer> enchants, String petType, String petTier) {
        if (id == null || id.isEmpty()) return null;
        if (id.equals("ENCHANTED_BOOK") && enchants != null && enchants.size() == 1) {
            var e = enchants.entrySet().iterator().next();
            return "ENCHANTMENT_" + e.getKey().toUpperCase(Locale.ROOT) + "_" + e.getValue();
        }
        if (id.equals("PET") && petType != null) {
            return "PET_" + petType.toUpperCase(Locale.ROOT) + (petTier == null ? "" : "?Rarity=" + petTier.toUpperCase(Locale.ROOT));
        }
        return id;
    }

    /** "Wither Essence x20" -> {"Wither Essence", 20}. */
    public static Object[] splitCount(String name) {
        String n = clean(name);
        Matcher m = COUNT.matcher(n);
        if (m.matches()) return new Object[]{m.group(1).strip(), Integer.parseInt(m.group(2))};
        return new Object[]{n, 1};
    }

    /** Stars, colour codes and padding off a name. */
    public static String clean(String name) {
        return name == null ? "" : name.replaceAll("§.", "").replace("✪", "").replaceAll("[➊➋➌➍➎]", "").strip();
    }

    /**
     * The key from a name ("Enchanted Book (Ultimate Wise II)", "Wither Essence", "Recombobulator 3000"), using
     * {@code names} (display name -> key, the pool's) for everything that is not a book or essence. Null if unknown.
     */
    public static String fromName(String name, Map<String, String> names) {
        String n = clean(name);
        Matcher m = ESSENCE.matcher(n);
        if (m.matches()) return "ESSENCE_" + m.group(1).toUpperCase(Locale.ROOT);
        m = BOOK.matcher(n);
        if (m.matches()) return book(m.group(1));
        if (names != null) {
            String k = names.get(n);
            if (k != null) return k;
            // "Shiny Necron's Handle" is the handle; a recombobulated name is the same item.
            if (n.startsWith("Shiny ")) return names.get(n.substring(6));
        }
        return null;
    }

    /** "Ultimate Wise II" -> ENCHANTMENT_ULTIMATE_WISE_2; "One For All I" -> ENCHANTMENT_ULTIMATE_ONE_FOR_ALL_1. */
    public static String book(String enchant) {
        Matcher m = LEVEL.matcher(enchant.strip());
        if (!m.matches()) return null;
        int lvl = 0;
        for (int i = 1; i < ROMAN.length; i++) if (ROMAN[i].equals(m.group(2))) lvl = i;
        if (lvl == 0) return null;
        String id = m.group(1).toLowerCase(Locale.ROOT).replace(' ', '_').replace("'", "").replace("-", "_");
        if (!id.startsWith("ultimate_") && ULTIMATE.contains(id)) id = "ultimate_" + id;
        return "ENCHANTMENT_" + id.toUpperCase(Locale.ROOT) + "_" + lvl;
    }

    /** The name a book's key is shown by: ENCHANTMENT_ULTIMATE_WISE_2 -> "Ultimate Wise II". */
    public static String bookName(String key) {
        if (key == null || !key.startsWith("ENCHANTMENT_")) return null;
        String rest = key.substring("ENCHANTMENT_".length());
        int us = rest.lastIndexOf('_');
        if (us < 0) return null;
        int lvl;
        try {
            lvl = Integer.parseInt(rest.substring(us + 1));
        } catch (NumberFormatException e) {
            return null;
        }
        String id = rest.substring(0, us).toLowerCase(Locale.ROOT);
        if (id.startsWith("ultimate_") && ULTIMATE.contains(id.substring(9))) id = id.substring(9);
        StringBuilder sb = new StringBuilder();
        for (String w : id.split("_")) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb + " " + (lvl < ROMAN.length ? ROMAN[lvl] : String.valueOf(lvl));
    }
}
