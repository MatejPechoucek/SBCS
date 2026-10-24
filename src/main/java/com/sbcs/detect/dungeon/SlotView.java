package com.sbcs.detect.dungeon;

import java.util.List;
import java.util.Map;

/**
 * One slot of an open menu, as the parsers see it. Pure: the game's side fills it from the ItemStack - plain name,
 * vanilla id, the SkyBlock data (id, a book's enchantments, a pet's type and rarity), plain lore lines, the count -
 * and keeps the stack itself in {@code stack} so the roll can draw the real thing.
 */
public record SlotView(int slot, String name, String item, String sbId, Map<String, Integer> enchants,
                       String petType, String petTier, List<String> lore, int count, Object stack) {

    public boolean loreHas(String s) {
        for (String l : lore) if (l.contains(s)) return true;
        return false;
    }
}
