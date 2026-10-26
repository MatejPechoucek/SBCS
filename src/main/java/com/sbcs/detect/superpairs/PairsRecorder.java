package com.sbcs.detect.superpairs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sbcs.detect.DropRecorder;
import com.sbcs.detect.dungeon.Sidebar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Records, for the Hunt, every Experimentation Table menu verbatim as {@code superpairs_menu}: title, every slot of the
 * menu's own part (name, item, SkyBlock data, lore). Written each time the grid has held still for a couple of ticks
 * and differs from the last record, so every flip of a game is there - which is what the theory in
 * {@link PairsGame} (titles, how a matched pair looks, the clicks left) and {@link PairsReward} (a revealed tile's
 * name and lore) has to be checked against. Only with "Drop records" on. Reads only; nothing is clicked or sent.
 */
public final class PairsRecorder {
    private PairsRecorder() {}

    private static final Pattern TITLE = Pattern.compile("(?i)superpairs|experiment|chronomatron|ultrasequencer|add-?ons?|rewards?");
    private static final int SETTLE_TICKS = 2;

    private static String pending, written;
    private static int still;

    public static void tick(Minecraft client) {
        if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            pending = null;
            written = null;
            return;
        }
        String title = Sidebar.plain(screen.getTitle().getString());
        if (!TITLE.matcher(title).find()) return;
        List<Slot> slots = screen.getMenu().slots;
        int own = Math.max(0, slots.size() - 36);
        StringBuilder sig = new StringBuilder(title);
        for (int i = 0; i < own; i++) {
            ItemStack s = slots.get(i).getItem();
            if (s.isEmpty()) continue;
            sig.append('|').append(i).append(':').append(s.getHoverName().getString()).append('x').append(s.getCount());
        }
        String now = sig.toString();
        if (!now.equals(pending)) {
            pending = now;
            still = 0;
            return;
        }
        if (++still < SETTLE_TICKS || now.equals(written)) return;
        written = now;

        JsonObject o = new JsonObject();
        o.addProperty("title", title);
        o.addProperty("rows", own / 9);
        JsonArray items = new JsonArray();
        for (int i = 0; i < own; i++) {
            ItemStack s = slots.get(i).getItem();
            if (s.isEmpty()) continue;
            JsonObject it = new JsonObject();
            it.addProperty("slot", i);
            it.addProperty("name", Sidebar.plain(s.getHoverName().getString()));
            it.addProperty("item", String.valueOf(BuiltInRegistries.ITEM.getKey(s.getItem())));
            it.addProperty("count", s.getCount());
            CustomData data = s.get(DataComponents.CUSTOM_DATA);
            if (data != null) it.addProperty("data", data.copyTag().toString());
            ItemLore lore = s.get(DataComponents.LORE);
            if (lore != null) {
                JsonArray lines = new JsonArray();
                for (Component c : lore.lines()) lines.add(Sidebar.plain(c.getString()));
                it.add("lore", lines);
            }
            // What the Hunt made of it, so a wrong reading shows in the record next to the truth.
            PairsReward r = PairsWatcher.read(s);
            if (r.covered()) it.addProperty("sbcs", r.tier() + " " + r.key());
            items.add(it);
        }
        o.add("items", items);
        DropRecorder.record("superpairs_menu", o);
    }
}
