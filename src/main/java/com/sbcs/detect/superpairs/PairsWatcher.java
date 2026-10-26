package com.sbcs.detect.superpairs;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sbcs.SbcsClient;
import com.sbcs.anim.PairsTimeline;
import com.sbcs.audio.PairsVoices;
import com.sbcs.audio.SbcsAudio;
import com.sbcs.audio.Voices;
import com.sbcs.config.PatternConfig;
import com.sbcs.config.SbcsStats;
import com.sbcs.detect.DecisionLog;
import com.sbcs.detect.dungeon.DungeonRolls;
import com.sbcs.detect.dungeon.Sidebar;
import com.sbcs.detect.dungeon.SlotView;
import com.sbcs.render.PairsHunt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Hunt's eyes on the Experimentation Table: every tick it reads the open Superpairs menu into a {@link PairsGame},
 * and every frame the cover mixin asks it whether a tile is hidden ({@link #coverSlot}). When the game's menus are gone
 * and no screen is open, the boxes the player saw are revealed in a {@link com.sbcs.gui.PairsScreen}.
 *
 * <b>Read only.</b> Nothing here clicks a tile, sends a packet or touches the menu: the player plays Superpairs
 * exactly as always; only what is drawn on a rare tile changes - a black box with its letter instead of the item, and
 * no tooltip over it. The box is decided while drawing, from the stack being drawn, so the item never shows for even
 * one frame before the tick catches up.
 *
 * Every box, match and the game's end are narrated into the DecisionLog.
 */
public final class PairsWatcher {
    private PairsWatcher() {}

    /** No Superpairs menu for this long and the game is over. */
    static final long GONE_MS = 1500;
    /** A reveal waits this long at most for every screen to close. */
    static final long WAIT_MS = 10 * 60_000;

    private static PairsGame game;
    private static Screen gameOn;
    private static long lastMenuAt;
    private static List<PairsTimeline.Box> pending;
    private static long pendingAt;
    private static long beatAt;
    private static int beatN;
    /** Found pairs for the crack of light between them: the two slots and when. */
    private static final List<long[]> matches = new ArrayList<>();
    /** What each stack reads as, by identity: the cover asks every frame and the reading is not free. */
    private static final Map<ItemStack, PairsReward> readings = new IdentityHashMap<>();

    // ================================================================== every tick

    public static void tick(Minecraft mc, DecisionLog log) {
        PatternConfig c = SbcsClient.config;
        long now = System.currentTimeMillis();
        if (c == null || !c.pairsAnimation) {
            if (game != null) log.note("§8The Hunt: switched off mid-game - forgotten");
            reset();
            pending = null;
            return;
        }
        Screen screen = mc.gui.screen();
        String title = screen instanceof AbstractContainerScreen<?> m ? Sidebar.plain(m.getTitle().getString()) : null;
        if (title != null && PairsGame.isSuperpairs(title)) {
            AbstractContainerScreen<?> menu = (AbstractContainerScreen<?>) screen;
            String stake = PairsGame.gameStake(title);
            boolean rewardsMenu = stake == null && PairsGame.isRewards(title);
            if (game == null && stake != null) start(stake, screen, now, log);
            if (game != null) {
                if (stake != null) gameOn = screen;
                lastMenuAt = now;
                List<SlotView> slots = DungeonRolls.slots(menu);
                Map<Integer, PairsReward> rewards = new HashMap<>();
                for (SlotView s : slots) rewards.put(s.slot(), read(s));
                for (PairsGame.Event e : game.update(slots, rewards, rewardsMenu, now)) narrate(e, log, now);
                heartbeat(now);
            }
        } else if (game != null && now - lastMenuAt > GONE_MS) {
            finish(log, now);
        }
        if (pending != null) {
            if (now - pendingAt > WAIT_MS) {
                log.note("§8The Hunt: the reveal waited " + WAIT_MS / 60_000 + " min for the screen - dropped");
                pending = null;
            } else if (screen == null && !SbcsClient.cutscenePlaying()) {
                List<PairsTimeline.Box> boxes = pending;
                pending = null;
                SbcsClient.openPairs(boxes, false);
            }
        }
    }

    private static void start(String stake, Screen screen, long now, DecisionLog log) {
        reset();
        game = new PairsGame(stake, now);
        gameOn = screen;
        lastMenuAt = now;
        log.note("§7The Hunt: a Superpairs game (" + stake + ") - watching its tiles");
    }

    private static void narrate(PairsGame.Event e, DecisionLog log, long now) {
        switch (e.kind()) {
            case BOX -> {
                log.note("§dThe Hunt: box " + e.box().mark + " over slot " + e.slot() + " - " + e.box().reward.tier()
                        + " (" + e.box().reward.key() + ")");
                SbcsAudio.playPairs(PairsVoices.boxLand(now));
            }
            case MATCH -> {
                log.note("§aThe Hunt: box " + e.box().mark + " matched"
                        + (e.slot() < 0 ? " (the rewards menu lists it)" : " in slots " + e.slot() + " and " + e.other()));
                if (e.slot() >= 0) matches.add(new long[]{e.slot(), e.other(), now});
                SbcsAudio.playPairs(PairsVoices.match(now));
            }
            case TURN -> { }
        }
    }

    /** While a box waits for its pair, a heartbeat that quickens with every turn the hunt goes on. */
    private static void heartbeat(long now) {
        if (!game.hunting()) return;
        long gap = Math.max(450, 1100 - 60L * game.turnsHunting());
        if (now - beatAt < gap) return;
        beatAt = now;
        SbcsAudio.playPairs(Voices.heartbeat(beatN++));
    }

    /** The game's menus are gone: count it, and queue the reveal of what the player saw. */
    private static void finish(DecisionLog log, long now) {
        PairsGame g = game;
        reset();
        List<PairsGame.Box> seen = g.boxes();
        PatternConfig c = SbcsClient.config;
        List<PairsTimeline.Box> show = new ArrayList<>();
        List<String> won = new ArrayList<>();
        for (PairsGame.Box b : seen) {
            if (b.matched) won.add(b.reward.title());
            if (b.matched || c.pairsShowMissed) show.add(new PairsTimeline.Box(b.mark, b.reward, b.matched));
        }
        // Stats: one run per game, its first find as the drop, the rest tallied on their own.
        SbcsStats st = SbcsStats.get();
        st.onSource(com.sbcs.gui.PairsScreen.SOURCE, won.isEmpty() ? null : won.get(0));
        for (int i = 1; i < won.size(); i++) st.onSourceDrop(com.sbcs.gui.PairsScreen.SOURCE, won.get(i));
        if (seen.isEmpty()) {
            log.note("§8The Hunt: the game (" + g.stake() + ") ended with no rare tile seen - nothing to reveal");
            return;
        }
        if (show.isEmpty()) {
            log.note("§8The Hunt: " + seen.size() + " box(es), none matched, and missed boxes are not shown");
            return;
        }
        if (won.isEmpty() && SbcsClient.bustStyle(com.sbcs.gui.PairsScreen.SOURCE) == 2) {
            log.note("§8The Hunt: only missed boxes, and busts are off - not played");
            return;
        }
        log.note("§aThe Hunt: the game (" + g.stake() + ") is over - " + won.size() + " found of " + seen.size()
                + "; the reveal waits for the screen");
        pending = show;
        pendingAt = now;
    }

    private static void reset() {
        game = null;
        gameOn = null;
        matches.clear();
        readings.clear();
        beatN = 0;
    }

    /** Everything stops (/sbcs stop, the fail-safes): the game is forgotten, nothing is revealed. */
    public static void stop() {
        reset();
        pending = null;
    }

    /** Chat is held from the first box until the reveal is over, so a "RARE REWARD!" line cannot give it away. */
    public static boolean holdsChat() {
        return pending != null || game != null && !game.boxes().isEmpty();
    }

    // ================================================================== the cover (every frame)

    private static boolean on() {
        PatternConfig c = SbcsClient.config;
        return c != null && c.pairsAnimation && c.pairsCover && com.sbcs.Safety.on("superpairs");
    }

    /**
     * Called for every slot the menu draws (its pose already at the menu's corner). A rare tile of a Superpairs game
     * gets its box drawn instead of the item - returns true, and the item is not drawn.
     */
    public static boolean coverSlot(Object screen, GuiGraphicsExtractor g, Slot slot) {
        char mark = mark(screen, slot);
        if (mark == 0) return false;
        PairsGame.Box b = null;
        for (PairsGame.Box x : game.boxes()) if (x.mark == mark) b = x;
        long now = System.currentTimeMillis();
        PairsHunt.cover(paint(g), slot.x, slot.y, mark, b == null ? -1 : now - b.firstSeen, now);
        return true;
    }

    /** Whether this slot is covered now (its tooltip goes too). */
    public static boolean covered(Object screen, Slot slot) {
        return mark(screen, slot) != 0;
    }

    private static char mark(Object screen, Slot slot) {
        if (slot == null || slot.container instanceof Inventory || !on()) return 0;
        if (!(screen instanceof AbstractContainerScreen<?> menu)) return 0;
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return 0;
        if (game == null || gameOn != screen) {
            // The first frame of a game can come before the tick that starts it.
            String stake = PairsGame.gameStake(Sidebar.plain(menu.getTitle().getString()));
            if (stake == null) return 0;
            if (game == null) start(stake, (Screen) screen, System.currentTimeMillis(), SbcsClient.decisions);
            else gameOn = (Screen) screen;
        }
        if (readings.size() > 512) readings.clear();
        PairsReward r = readings.computeIfAbsent(stack, PairsWatcher::read);
        PairsGame.Box b = game.box(r, System.currentTimeMillis());
        return b == null ? 0 : b.mark;
    }

    /** Drawn after the screen: embers and the clicks round the menu while a box waits, a crack between a found pair. */
    public static void drawOver(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (game == null || mc.gui.screen() != gameOn || !on() || !(gameOn instanceof AbstractContainerScreen<?> menu)) return;
        if (rectOf != menu) return;
        int[] rect = PairsWatcher.rect;
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        long now = System.currentTimeMillis();
        List<PairsHunt.Match> ms = new ArrayList<>();
        List<Slot> slots = menu.getMenu().slots;
        for (long[] m : matches) {
            if (m[0] >= slots.size() || m[1] >= slots.size()) continue;
            Slot a = slots.get((int) m[0]), b = slots.get((int) m[1]);
            ms.add(new PairsHunt.Match(rect[0] + a.x, rect[1] + a.y, rect[0] + b.x, rect[1] + b.y, m[2]));
        }
        g.nextStratum();
        PairsHunt.overlay(paint(g), w, h, rect[0], rect[1], rect[2], rect[3], game.hunting(), game.turnsHunting(),
                game.clicksLeft(), game.clicksMax(), ms, now);
    }

    private static Object rectOf;
    private static final int[] rect = new int[4];

    /** The menu's own rect in GUI pixels, handed over by the cover mixin every frame (its fields are protected). */
    public static void rect(Object screen, int left, int top, int w, int h) {
        rectOf = screen;
        rect[0] = left;
        rect[1] = top;
        rect[2] = w;
        rect[3] = h;
    }

    private static PairsHunt.Paint paint(GuiGraphicsExtractor g) {
        Font font = Minecraft.getInstance().font;
        return new PairsHunt.Paint() {
            @Override
            public void fill(float x, float y, float w, float h, int argb) {
                int x0 = Math.round(x), y0 = Math.round(y);
                g.fill(x0, y0, x0 + Math.max(1, Math.round(w)), y0 + Math.max(1, Math.round(h)), argb);
            }

            @Override
            public void text(String text, float cx, float cy, float scale, int argb) {
                if ((argb >>> 24) < 5) return;
                g.text(font, text, Math.round(cx - font.width(text) / 2f), Math.round(cy - 4f), argb, true);
            }
        };
    }

    // ================================================================== reading a tile

    /** A slot as the parsers see it, read through {@link PairsReward#of}. */
    static PairsReward read(SlotView s) {
        return PairsReward.of(s.name(), s.item(), s.sbId(), s.enchants(), s.petType(), s.petTier(), s.lore());
    }

    /** The same, straight from a stack (the cover's path, every frame - cached by identity). */
    static PairsReward read(ItemStack s) {
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
                    // Read by its name instead.
                }
            }
        }
        List<String> lore = new ArrayList<>();
        ItemLore l = s.get(DataComponents.LORE);
        if (l != null) for (Component line : l.lines()) lore.add(Sidebar.plain(line.getString()));
        return PairsReward.of(name, item, id, ench, petType, petTier, lore);
    }
}
