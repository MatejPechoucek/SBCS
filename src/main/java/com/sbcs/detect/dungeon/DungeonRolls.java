package com.sbcs.detect.dungeon;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.sbcs.SbcsClient;
import com.sbcs.config.PatternConfig;
import com.sbcs.detect.DecisionLog;
import com.sbcs.gui.OssuaryRoll;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Ossuary's eyes on the game: every tick it reads the open chest menu (the boss room's reward chest, or a run's
 * page at Croesus), asks {@link RollPicker} whether it rolls, and keeps the live {@link OssuaryRoll}. The cover mixins
 * ask {@link #covers} whether to hide the menu, and {@link #drawOver} draws the roll above it.
 *
 * <b>Read only.</b> Nothing here clicks a slot, sends a packet or opens a chest - the player still clicks "Open Reward
 * Chest" themself, after the roll. The menu is only hidden while the roll plays, and its own mouse and keys are
 * swallowed then, so a hover can neither show the contents early nor click anything by accident.
 *
 * A menu that might be ours is covered from its first frame (its contents arrive a tick or two after the title);
 * if it turns out not to roll, the cover goes at once - {@link #PRE_COVER_MS} at most.
 */
public final class DungeonRolls {
    private DungeonRolls() {}

    static final long PRE_COVER_MS = 1500;
    /** At Croesus, how long the page waits for the bazaar's prices before picking the best chest without them. */
    static final long PRICE_WAIT_MS = 1200;

    private static OssuaryRoll live;
    private static Screen liveOn;
    /** The menu the latest decision was about, when it was first seen, and whether it is done (rolled or not ours). */
    private static Screen seen;
    private static long seenAt;
    private static boolean settled;
    private static String lastWhy;
    /** When the current cover began, and when a frame last drew it. */
    private static long coverFrom, drawnAt;

    private static DungeonPool pool;
    private static final RollMemory MEMORY = new RollMemory();
    private static boolean loaded;
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("sbcs").resolve("dungeon_rolls.json");

    public static DungeonPool pool() {
        if (pool == null) {
            try (var in = DungeonRolls.class.getResourceAsStream("/assets/sbcs/dungeon_pool.json")) {
                pool = DungeonPool.parse(in == null ? "{}" : new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (Exception e) {
                SbcsClient.LOGGER.warn("[SBCS] could not read the dungeon pool", e);
                pool = DungeonPool.parse("{}");
            }
        }
        return pool;
    }

    // ================================================================== every tick

    public static void tick(Minecraft mc, DecisionLog log) {
        load();
        Screen screen = mc.gui.screen();
        if (live != null && (screen != liveOn || live.finished())) {
            if (!live.finished()) live.end();
            live = null;
            liveOn = null;
        }
        PatternConfig c = SbcsClient.config;
        if (c == null || !c.dungeonAnimation || !(screen instanceof AbstractContainerScreen<?> menu)) {
            seen = null;
            return;
        }
        if (screen != seen) {
            seen = screen;
            seenAt = System.currentTimeMillis();
            coverFrom = seenAt;
            settled = false;
            lastWhy = null;
        }
        if (live != null) return;
        String title = Sidebar.plain(menu.getTitle().getString());
        if (!RollPicker.candidate(title, c.dungeonTiers, c.dungeonCroesus)) {
            settled = true;
            return;
        }
        // Settled menus are looked at again: a Kismet reroll changes the chest in the same menu, and it rolls again.
        List<SlotView> slots = slots(menu);
        long now = System.currentTimeMillis();
        DungeonFloor hint = DungeonFloor.fromSidebar(Sidebar.lines(mc));
        boolean runPage = ChestReader.runFloor(title) != null;
        List<String> keys = runPage ? requestPrices(title, slots) : List.of();
        RollPicker.Decision d = RollPicker.decide(title, slots, hint, pool().names(), c.dungeonTiers, c.dungeonCroesus,
                MEMORY, now, DungeonRolls::price);
        if (d.kind() == RollPicker.Kind.WAIT) {
            if (now - seenAt > PRE_COVER_MS) settled = true;
            return;
        }
        // A run's page picks by profit: give the bazaar a moment the first time.
        if (d.kind() == RollPicker.Kind.ROLL && runPage && now - seenAt < PRICE_WAIT_MS
                && (com.sbcs.market.Bazaar.pending() || keys.stream().anyMatch(com.sbcs.market.LivePrice::pending))) {
            return;
        }
        if (d.kind() == RollPicker.Kind.NONE) {
            if (!d.why().equals(lastWhy)) log.note("§8The Ossuary: " + title + " - " + d.why());
            lastWhy = d.why();
            settled = true;
            return;
        }
        // ROLL.
        for (String fp : d.remember()) MEMORY.remember(fp, now);
        save();
        RewardChest chest = d.chest();
        OssuaryRoll roll = new OssuaryRoll(chest, pool(), false, null);
        if (!roll.win() && SbcsClient.bustStyle("dungeon") == 2 && roll.timeline().value() != null
                && !roll.timeline().value().incomplete()) {
            log.note("§7The Ossuary: " + chest.tier().title + " - a bust, and busts are off");
            settled = true;
            return;
        }
        log.note("§aThe Ossuary rolls: " + d.why());
        live = roll;
        liveOn = screen;
        coverFrom = Math.min(coverFrom, now);
        settled = true;
    }

    private static Screen requestedFor;

    /** A run's page is picked by profit: ask for every price on it, once per page. Returns the auction keys asked. */
    private static List<String> requestPrices(String title, List<SlotView> slots) {
        List<String> keys = new ArrayList<>();
        for (RewardChest ch : ChestReader.readRun(title, slots, pool().names())) {
            for (Loot l : ch.items()) {
                if (l.key() != null && !l.essence() && !l.key().startsWith("ENCHANTMENT_")) keys.add(l.key());
            }
        }
        if (requestedFor != seen) {
            requestedFor = seen;
            PatternConfig c = SbcsClient.config;
            if (c == null || c.livePrices) {
                com.sbcs.market.Bazaar.request();
                for (String k : keys) com.sbcs.market.LivePrice.request(k);
            }
        }
        return keys;
    }

    /** What the market says an item is worth now, or null (the same as the roll's). */
    static Long price(String key) {
        if (key == null) return null;
        PatternConfig c = SbcsClient.config;
        if (c != null && !c.livePrices) return null;
        if (key.equals(ChestValue.KEY_ITEM)) {
            Double buy = com.sbcs.market.Bazaar.buy(key);
            return buy == null ? null : Math.round(buy);
        }
        Double sell = com.sbcs.market.Bazaar.sell(key);
        if (sell != null && sell > 0) return Math.round(sell);
        return com.sbcs.market.LivePrice.cached(key);
    }

    // ================================================================== the cover

    /** Whether this menu is hidden now: a roll over it, or a candidate still loading. */
    public static boolean covers(Object screen) {
        if (screen == null || !com.sbcs.Safety.on("dungeon")) return false;
        // Never a cover with nothing drawn on it: if the drawing hook has not run, the menu is left alone.
        long now = System.currentTimeMillis();
        if (now - coverFrom > 1000 && drawnAt < coverFrom) return false;
        if (live != null && screen == liveOn && !live.finished()) return true;
        if (screen == seen && !settled && now - seenAt < PRE_COVER_MS) {
            PatternConfig c = SbcsClient.config;
            return c != null && c.dungeonAnimation;
        }
        return false;
    }

    /** Drawn after the screen (Hud.extractSavingIndicator's HEAD), on a stratum of its own above everything in it. */
    public static void drawOver(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.gui.screen();
        if (!covers(screen)) return;
        drawnAt = System.currentTimeMillis();
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        g.nextStratum();
        if (live != null && screen == liveOn) {
            live.draw(g, w, h);
        } else {
            g.fill(0, 0, w, h, 0xE607060A);
        }
    }

    public static boolean mousePressed(Object screen, double x, double y) {
        if (live != null && screen == liveOn && !live.finished()) return live.mousePressed(x, y);
        return covers(screen);
    }

    public static boolean mouseDragged(Object screen, double x, double y) {
        if (live != null && screen == liveOn && !live.finished()) live.mouseDragged(x, y);
        return covers(screen);
    }

    public static boolean mouseReleased(Object screen, double x, double y) {
        if (live != null && screen == liveOn && !live.finished()) live.mouseReleased(x, y);
        return covers(screen);
    }

    /**
     * A key while covered: Escape ends the roll and lets the menu close as usual; Space or Backspace skips; anything
     * else is swallowed (number keys would move items).
     */
    public static boolean keyPressed(Object screen, int key) {
        if (!covers(screen)) return false;
        if (key == 256) {
            if (live != null) live.end();
            return false;
        }
        if (live != null && (key == 32 || key == 259)) live.skip();
        return true;
    }

    /** Everything stops (/sbcs stop, the fail-safes). */
    public static void stop() {
        if (live != null) live.end();
        live = null;
        liveOn = null;
        settled = true;
    }

    public static boolean active() {
        return live != null && !live.finished();
    }

    // ================================================================== the menu's slots

    /** The chest's own slots (not the player's inventory) as the parsers see them. */
    public static List<SlotView> slots(AbstractContainerScreen<?> menu) {
        List<Slot> all = menu.getMenu().slots;
        int own = Math.max(0, all.size() - 36);
        List<SlotView> out = new ArrayList<>();
        for (int i = 0; i < own; i++) {
            ItemStack s = all.get(i).getItem();
            if (s.isEmpty()) continue;
            String name = Sidebar.plain(s.getHoverName().getString());
            String item = String.valueOf(BuiltInRegistries.ITEM.getKey(s.getItem()));
            String id = null, petType = null, petTier = null;
            Map<String, Integer> ench = null;
            CustomData data = s.get(DataComponents.CUSTOM_DATA);
            if (data != null) {
                CompoundTag tag = data.copyTag();
                id = tag.getString("id").orElse(null);
                CompoundTag e = tag.getCompoundOrEmpty("enchantments");
                if (!e.keySet().isEmpty()) {
                    ench = new HashMap<>();
                    for (String k : e.keySet()) ench.put(k, e.getIntOr(k, 0));
                }
                String pet = tag.getString("petInfo").orElse(null);
                if (pet != null) {
                    try {
                        JsonObject o = new Gson().fromJson(pet, JsonObject.class);
                        if (o != null && o.has("type")) petType = o.get("type").getAsString();
                        if (o != null && o.has("tier")) petTier = o.get("tier").getAsString();
                    } catch (RuntimeException ignored) {
                        // A pet we cannot read is priced by its name.
                    }
                }
            }
            List<String> lore = new ArrayList<>();
            ItemLore l = s.get(DataComponents.LORE);
            if (l != null) for (Component line : l.lines()) lore.add(Sidebar.plain(line.getString()));
            out.add(new SlotView(i, name, item, id, ench, petType, petTier, lore, s.getCount(), s.copy()));
        }
        return out;
    }

    // ================================================================== remembering

    private static void load() {
        if (loaded) return;
        loaded = true;
        try {
            if (!Files.exists(FILE)) return;
            Map<String, Long> m = new Gson().fromJson(Files.readString(FILE), new TypeToken<Map<String, Long>>() {}.getType());
            if (m != null) MEMORY.load(m, System.currentTimeMillis());
        } catch (Exception e) {
            SbcsClient.LOGGER.warn("[SBCS] could not read {}", FILE, e);
        }
    }

    private static void save() {
        Map<String, Long> m = MEMORY.entries();
        Thread t = new Thread(() -> {
            try {
                Files.createDirectories(FILE.getParent());
                Path tmp = FILE.resolveSibling("dungeon_rolls.json.tmp");
                Files.writeString(tmp, new Gson().toJson(m));
                Files.move(tmp, FILE, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception e) {
                SbcsClient.LOGGER.warn("[SBCS] could not save {}", FILE, e);
            }
        }, "SBCS dungeon rolls");
        t.setDaemon(true);
        t.start();
    }
}
