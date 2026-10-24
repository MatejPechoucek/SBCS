package com.sbcs.detect.dungeon;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sbcs.detect.DropRecorder;
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
 * Records, for the Ossuary, every dungeon reward chest and Croesus page the player opens, verbatim: title,
 * every slot of the chest's own part (name, item, SkyBlock data, lore), and the sidebar. A page is written once
 * its contents have held still for a few ticks and differ from the last one written, so a Kismet reroll (new
 * contents, same screen) is written again. Only with "Drop records" on. Reads only; nothing is clicked or sent.
 */
public final class DungeonRecorder {
    private DungeonRecorder() {}

    private static final Pattern TITLE = Pattern.compile(
            "(?i)\\b(wood|gold|diamond|emerald|obsidian|bedrock)\\b.*chest|croesus|catacombs|reward|kismet");
    private static final int SETTLE_TICKS = 4;

    private static String pending;
    private static int still;
    private static String written;
    private static String lastSidebar;

    public static void tick(Minecraft client) {
        if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            pending = null;
            written = null;
            return;
        }
        String title = Sidebar.plain(screen.getTitle().getString());
        List<Slot> slots = screen.getMenu().slots;
        int own = Math.max(0, slots.size() - 36);
        boolean dungeonish = TITLE.matcher(title).find();
        StringBuilder sig = new StringBuilder(title);
        for (int i = 0; i < own; i++) {
            ItemStack s = slots.get(i).getItem();
            if (s.isEmpty()) continue;
            String name = Sidebar.plain(s.getHoverName().getString());
            if (name.contains("Open Reward Chest") || name.contains("Reroll Chest")) dungeonish = true;
            sig.append('|').append(i).append(':').append(name).append('x').append(s.getCount());
            ItemLore lore = s.get(DataComponents.LORE);
            if (lore != null) sig.append('#').append(lore.lines().size());
        }
        if (!dungeonish) return;
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
            String name = Sidebar.plain(s.getHoverName().getString());
            // The grey glass round the edges is the same on every page.
            if (name.isEmpty() && String.valueOf(BuiltInRegistries.ITEM.getKey(s.getItem())).contains("glass_pane")) continue;
            JsonObject it = new JsonObject();
            it.addProperty("slot", i);
            it.addProperty("name", name);
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
            items.add(it);
        }
        o.add("items", items);
        JsonArray side = new JsonArray();
        for (String l : Sidebar.lines(client)) side.add(l);
        o.add("sidebar", side);
        DropRecorder.record("dungeon_chest", o);
    }

    /** The sidebar whenever it names a dungeon floor and has changed - the floor, the run's time, the score. */
    public static void sidebarTick(Minecraft client) {
        List<String> lines = Sidebar.lines(client);
        String joined = String.join("\n", lines);
        if (!joined.contains("Catacombs") || joined.equals(lastSidebar)) return;
        // The clock ticks every second; keep one record per change of anything but digits.
        String shape = joined.replaceAll("\\d", "");
        if (lastSidebar != null && shape.equals(lastSidebar.replaceAll("\\d", ""))) return;
        lastSidebar = joined;
        JsonObject o = new JsonObject();
        JsonArray side = new JsonArray();
        for (String l : lines) side.add(l);
        o.add("sidebar", side);
        DropRecorder.record("dungeon_sidebar", o);
    }
}
