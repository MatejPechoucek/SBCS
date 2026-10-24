package com.sbcs.detect.dungeon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the two menus the Ossuary plays on. Pure. Strings are from Skyblocker (ChestValue, CroesusProfit), SkyOcean
 * (DungeonGambling, CroesusImpl) and SkyHanni (CroesusChestTracker) - used as facts - until our own records
 * ({@code dungeon_chest}, written by DungeonRecorder) confirm them.
 *
 * <ul>
 *   <li>A reward chest - in the boss room, or a chest's own page at Croesus: titled with its tier ("Obsidian Chest",
 *   or just "Obsidian"), the contents in its slots, "Open Reward Chest" (slot 31) with the cost in its lore
 *   ("1,000,000 Coins", "FREE", "Dungeon Chest Key"), "Reroll Chest" (Kismet) beside it. At Croesus a back arrow's
 *   lore names the floor: "To Catacombs - Floor VII".</li>
 *   <li>A run's page at Croesus, "Catacombs - Floor VII" / "Master Catacombs - Floor VII": a head per chest named
 *   just its tier, whose lore lists "Contents" (one line each, "Wither Essence x20", "Enchanted Book (Bank I)") down
 *   to a blank line, then "Cost" and the price.</li>
 * </ul>
 */
public final class ChestReader {
    private ChestReader() {}

    private static final Pattern COINS = Pattern.compile("^([0-9][0-9,]*) Coins$");

    /** Buttons and decoration in a chest menu, never contents. */
    static boolean control(SlotView s) {
        String n = s.name();
        if (n.isEmpty()) return true;
        if (s.item() != null && (s.item().contains("glass_pane") || s.item().endsWith(":barrier"))) return true;
        if (n.contains("Open Reward Chest") || n.contains("Reroll Chest") || n.equals("Close") || n.equals("Go Back")
                || n.startsWith("Go Back")) return true;
        return s.item() != null && s.item().endsWith(":arrow") && s.loreHas("To ");
    }

    /**
     * A reward chest's own menu, or null: not a chest's title, or not loaded yet (the "Open Reward Chest" button is
     * sent last - Skyblocker waits for it too). {@code floorHint} is the sidebar's, used when no back arrow names one.
     */
    public static RewardChest readChest(String title, List<SlotView> slots, DungeonFloor floorHint, Map<String, String> names) {
        ChestTier tier = ChestTier.fromTitle(title);
        if (tier == null) return null;
        SlotView open = null;
        DungeonFloor floor = null;
        for (SlotView s : slots) {
            if (s.name().contains("Open Reward Chest")) open = s;
            if (s.item() != null && s.item().endsWith(":arrow")) {
                for (String l : s.lore()) {
                    if (l.startsWith("To ")) {
                        DungeonFloor f = DungeonFloor.fromTitle(l.substring(3));
                        if (f != null) floor = f;
                    }
                }
            }
        }
        if (open == null) return null;
        if (floor == null) floor = floorHint;
        List<Loot> items = new ArrayList<>();
        for (SlotView s : slots) {
            if (control(s)) continue;
            items.add(loot(s, names));
        }
        if (items.isEmpty()) return null;
        long cost = 0;
        boolean key = false, opened = false;
        for (String l : open.lore()) {
            Matcher m = COINS.matcher(l.strip());
            if (m.matches()) cost = Long.parseLong(m.group(1).replace(",", ""));
            if (l.contains("Dungeon Chest Key")) key = true;
            String low = l.toLowerCase(java.util.Locale.ROOT);
            if (low.contains("already opened") || low.contains("already claimed")) opened = true;
        }
        return new RewardChest(floor, tier, items, cost, key, opened, -1);
    }

    /** One slot's item: priced by its data, else by its name. */
    static Loot loot(SlotView s, Map<String, String> names) {
        Object[] nc = PriceKeys.splitCount(s.name());
        String name = (String) nc[0];
        int count = Math.max(s.count(), (Integer) nc[1]);
        String key = PriceKeys.fromData(s.sbId(), s.enchants(), s.petType(), s.petTier());
        if (key == null) key = PriceKeys.fromName(name, names);
        // A book shows as "Enchanted Book"; its own name is the enchantment's.
        if (key != null && key.startsWith("ENCHANTMENT_") && name.equals("Enchanted Book")) {
            String b = PriceKeys.bookName(key);
            if (b != null) name = b;
        }
        boolean shiny = name.startsWith("Shiny ");
        return new Loot(key, name, count, shiny, s.item(), null, null, false, -1, s.stack());
    }

    /** A run's page at Croesus: its floor, or null if this is not one. */
    public static DungeonFloor runFloor(String title) {
        if (title == null) return null;
        String t = title.strip();
        if (!(t.startsWith("Catacombs - ") || t.startsWith("Master Catacombs - "))) return null;
        return DungeonFloor.fromTitle(t);
    }

    /** The chests on a run's page, from each head's lore. Empty if none could be read. */
    public static List<RewardChest> readRun(String title, List<SlotView> slots, Map<String, String> names) {
        DungeonFloor floor = runFloor(title);
        List<RewardChest> out = new ArrayList<>();
        if (floor == null) return out;
        for (SlotView s : slots) {
            ChestTier tier = ChestTier.fromTitle(s.name());
            if (tier == null || !s.name().strip().equals(tier.title) && !s.name().strip().equals(tier.title + " Chest")) continue;
            List<Loot> items = new ArrayList<>();
            long cost = 0;
            boolean key = false, opened = false, contents = false, costNext = false;
            for (String raw : s.lore()) {
                String l = raw.strip();
                String low = l.toLowerCase(java.util.Locale.ROOT);
                if (low.contains("already opened") || low.contains("already claimed") || low.equals("opened!")) opened = true;
                if (l.equals("Contents")) {
                    contents = true;
                    continue;
                }
                if (contents) {
                    if (l.isEmpty()) {
                        contents = false;
                        continue;
                    }
                    Object[] nc = PriceKeys.splitCount(l);
                    String name = (String) nc[0];
                    String k = PriceKeys.fromName(name, names);
                    // "Enchanted Book (Bank I)" is shown by its enchantment.
                    if (k != null && k.startsWith("ENCHANTMENT_") && name.startsWith("Enchanted Book")) {
                        String b = PriceKeys.bookName(k);
                        if (b != null) name = b;
                    }
                    items.add(new Loot(k, name, (Integer) nc[1], name.startsWith("Shiny "), null, null, null, false, -1, null));
                    continue;
                }
                if (l.equals("Cost")) {
                    costNext = true;
                    continue;
                }
                Matcher m = COINS.matcher(l);
                if (m.matches()) cost = Long.parseLong(m.group(1).replace(",", ""));
                if (l.contains("Dungeon Chest Key")) key = true;
                costNext = false;
            }
            if (!items.isEmpty()) out.add(new RewardChest(floor, tier, items, cost, key, opened, s.slot()));
        }
        return out;
    }
}
