package com.sbcs.detect.dungeon;

/**
 * One thing in a reward chest. Pure. {@code key} is what it is priced by (the bazaar's or Coflnet's id,
 * "ENCHANTMENT_ULTIMATE_WISE_2", "ESSENCE_WITHER", "PET_SPIRIT?Rarity=LEGENDARY"), null when it could not be worked
 * out; {@code name} is what the player sees. The look, for drawing: {@code item} a vanilla id, {@code model} Hypixel's
 * item model, {@code skin} a head's texture blob, {@code colour} a leather tint (-1 none). {@code stack} is the real
 * ItemStack the chest showed, when there is one - opaque here, drawn by the game's side.
 */
public record Loot(String key, String name, int count, boolean shiny, String item, String model, String skin,
                   boolean glint, int colour, Object stack) {

    public Loot {
        count = Math.max(1, count);
    }

    public static Loot named(String key, String name, int count) {
        return new Loot(key, name, count, false, null, null, null, false, -1, null);
    }

    public Loot withStack(Object s) {
        return new Loot(key, name, count, shiny, item, model, skin, glint, colour, s);
    }

    public Loot withLook(Loot look) {
        if (look == null) return this;
        return new Loot(key, name, count, shiny, look.item, look.model, look.skin, look.glint, look.colour, stack);
    }

    /** Essence and the like: priced by the piece and never the headline unless nothing else is worth anything. */
    public boolean essence() {
        return key != null && key.startsWith("ESSENCE_");
    }

    /** The name as it is shown, "Wither Essence x20". */
    public String label() {
        return count > 1 ? name + " x" + count : name;
    }
}
