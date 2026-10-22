package com.sbcs.gui;

import com.sbcs.SbcsClient;
import com.sbcs.anim.OssuaryTimeline;
import com.sbcs.anim.SceneClock;
import com.sbcs.audio.OssuarySound;
import com.sbcs.audio.SbcsAudio;
import com.sbcs.config.PatternConfig;
import com.sbcs.detect.dungeon.ChestTier;
import com.sbcs.detect.dungeon.ChestValue;
import com.sbcs.detect.dungeon.DungeonPool;
import com.sbcs.detect.dungeon.Loot;
import com.sbcs.detect.dungeon.RewardChest;
import com.sbcs.render.ChopScene;
import com.sbcs.render.HeadSkins;
import com.sbcs.render.OssuaryScene;
import com.sbcs.render.PixelLayer;
import com.sbcs.render.TextureBank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One roll of "The Ossuary": the timeline, its clock, the player's hands on the chain, the sounds, the prices and the
 * GUI primitives {@link OssuaryScene} draws through. Not a cutscene in the HUD - it is drawn over the reward chest's
 * own menu (or Croesus's page, or the test screen), which is why it is a plain object with {@link #draw} and the mouse
 * calls rather than a Screen. {@code DungeonRolls} owns the live one; {@code OssuaryTestScreen} hosts a test.
 *
 * Read-only towards the server: nothing here clicks a slot or sends anything. The player opens the chest themself.
 */
public final class OssuaryRoll implements OssuaryScene.Canvas {

    static final String SOURCE = "dungeon";

    private final RewardChest chest;
    private final OssuaryTimeline tl;
    private final boolean test;
    private final SceneClock clock = new SceneClock(SbcsClient.config == null ? 100 : SbcsClient.config.dungeonSpeed);
    private final Map<String, Long> fixed;
    private static final PixelLayer LAYER = new PixelLayer("ossuary_fx");

    private boolean finished, counted, summarised, ended;
    private long soundAt = -1;
    private final List<OssuarySound.Cue> cues = new ArrayList<>();
    private String nth, dry;

    private GuiGraphicsExtractor g;
    private final Font font = Minecraft.getInstance().font;
    private final Matrix3x2f matrix = new Matrix3x2f();

    /**
     * @param chest the chest as read off its menu (items carry their ItemStacks), or a made-up one for a test
     * @param fixed a test's prices for what the market has not priced (the market comes first), or null
     */
    public OssuaryRoll(RewardChest chest, DungeonPool pool, boolean test, Map<String, Long> fixed) {
        this.chest = withLooks(chest, pool);
        this.test = test;
        this.fixed = fixed;
        PatternConfig c = SbcsClient.config;
        boolean red = chest.floor().master() && (c == null || c.dungeonMasterRed);
        boolean chain = c == null || c.dungeonChain;
        List<Loot> looks = new ArrayList<>();
        List<DungeonPool.Entry> entries = pool == null ? List.of() : pool.pool(chest.floor(), chest.tier());
        int[] weights = new int[entries.size()];
        for (int i = 0; i < weights.length; i++) {
            looks.add(entries.get(i).look());
            weights[i] = entries.get(i).weight();
        }
        long seed = System.nanoTime() ^ clock.openedAt();
        this.tl = new OssuaryTimeline(chest.floor(), chest.tier(), red, chain, seed, looks, weights);
        if (c == null || c.livePrices) {
            com.sbcs.market.Bazaar.request();
            for (Loot l : this.chest.items()) {
                if (l.key() != null && !l.essence() && !l.key().startsWith("ENCHANTMENT_")) com.sbcs.market.LivePrice.request(l.key());
            }
        }
        HeadSkins.DUNGEON.prewarm();
        reprice();
        shortBust(test ? SbcsClient.tryingIt : true);
    }

    /**
     * Busts set to short: a chest known to hold nothing over the floor jumps, silently, to just before the reveal -
     * the hand does the pulls in no time. Only once every price is in: an unknown price is never taken for a bust.
     */
    private void shortBust(boolean follows) {
        if (!follows || SbcsClient.bustStyle(SOURCE) != 1 || tl.value() == null || tl.value().incomplete() || win()) return;
        long t = 0;
        while (tl.reveal() < 0 && t < 120_000) {
            tl.update(t);
            t += 16;
        }
        if (tl.reveal() < 0) return;
        long target = Math.max(0, tl.reveal() - 450);
        clock.jump(target);
        soundAt = target;
    }

    /** Every item given the pool's look (its icon, skin or model) where the pool knows it. */
    private static RewardChest withLooks(RewardChest c, DungeonPool pool) {
        if (pool == null) return c;
        List<Loot> items = new ArrayList<>();
        for (Loot l : c.items()) items.add(l.key() == null ? l : l.withLook(pool.look(l.key())));
        return new RewardChest(c.floor(), c.tier(), items, c.cost(), c.key(), c.opened(), c.slot());
    }

    public RewardChest chest() { return chest; }
    public OssuaryTimeline timeline() { return tl; }
    public boolean isTest() { return test; }

    /** Over: the cover lifts and the menu underneath is the player's again. */
    public boolean finished() { return finished; }

    // ================================================================== prices

    private Long price(String key) {
        if (key == null) return null;
        Long live = market(key);
        if (live != null) return live;
        return fixed == null ? null : fixed.get(key);
    }

    /** The market's price: the bazaar's instant sell (instant buy for the key), else the lowest BIN. */
    public static Long market(String key) {
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

    private void reprice() {
        if (tl.locked()) return;
        long floor = 0;
        PatternConfig c = SbcsClient.config;
        if (c != null && c.minWorth > 0) floor = PatternConfig.MIN_WORTH[Math.min(c.minWorth, PatternConfig.MIN_WORTH.length - 1)];
        tl.resolve(ChestValue.of(chest, this::price), floor);
    }

    /** Whether this chest is worth playing at all with busts off: priced (as far as is known yet) over the floor. */
    public boolean win() {
        return tl.band() != OssuaryTimeline.Band.BUST;
    }

    // ================================================================== the frame

    public void draw(GuiGraphicsExtractor g, int w, int h) {
        long t = clock.t();
        if (!finished && t >= tl.total()) {
            end();
            return;
        }
        reprice();
        tl.update(t);
        sounds(t);
        count(t);
        for (String[] v : OssuaryScene.VANILLA) {
            String path = v[1];
            TextureBank.adopt(v[0], Identifier.withDefaultNamespace("textures/" + path + ".png"), 16, 16);
        }
        for (ChestTier tier : ChestTier.values()) {
            TextureBank.adopt(OssuaryScene.chestTex(tier), Identifier.withDefaultNamespace("textures/block/" + tier.block + ".png"), 16, 16);
        }
        PatternConfig c = SbcsClient.config;
        OssuaryScene.nthNote = nth;
        OssuaryScene.dryNote = dry;
        OssuaryScene.showBanner = c == null || c.dungeonBanner;
        ChopScene.calm = (c != null && c.calmEffects) || com.sbcs.Safety.slow();
        this.g = g;
        // The menu underneath is covered whole until the very end: its items would give the roll away.
        float cover = 1f - OssuaryTimeline.ease((t - (tl.total() - OssuaryTimeline.FADE_MS)) / (float) OssuaryTimeline.FADE_MS);
        fill(0, 0, w, h, Math.round(255 * 0.9f * Math.max(0f, cover)) << 24 | 0x07060A);
        LAYER.begin(w, h, OssuaryScene.unitFor(h));
        boolean[] flushed = {false};
        OssuaryScene.flushLayer = () -> {
            if (flushed[0]) return;
            flushed[0] = true;
            LAYER.end(g);
        };
        try {
            OssuaryScene.draw(this, LAYER, tl, t, w, h);
        } finally {
            OssuaryScene.flushLayer = null;
        }
        if (!flushed[0]) LAYER.end(g);
        this.g = null;
    }

    private void sounds(long t) {
        if (t <= soundAt) return;
        cues.clear();
        OssuarySound.between(tl, soundAt, t, cues);
        soundAt = t;
        for (OssuarySound.Cue cue : cues) {
            if (cue.cut()) SbcsAudio.silence();
            SbcsAudio.playDungeon(cue.voices());
        }
    }

    /** Counted once the outcome shows; a test never. */
    private void count(long t) {
        if (counted || test || tl.reveal() < 0 || t < tl.reveal()) return;
        counted = true;
        com.sbcs.config.SbcsStats st = com.sbcs.config.SbcsStats.get();
        Loot best = tl.value() == null ? null : tl.value().bestItem();
        boolean win = win() && best != null;
        st.onSource(SOURCE, win ? best.name() : null);
        if (win) nth = "your " + ChopScreen.ordinal(st.sourceDropCount(SOURCE, best.name()));
        else {
            int d = st.source(SOURCE).dry;
            dry = d + (d == 1 ? " chest" : " chests") + " since a rare reward";
        }
    }

    // ================================================================== input

    private boolean dragging;
    private float dragFrom, dragLast;
    private long dragLastAt;
    private float dragSpeed;

    /** A click on the scene. Returns true: the menu underneath never sees it. */
    public boolean mousePressed(double x, double y) {
        long t = clock.t();
        if (tl.reveal() >= 0 && t >= tl.reveal()) return true;
        if (inside(OssuaryScene.hitChain, x, y) && tl.canPull(0, t)) {
            dragging = true;
            dragFrom = dragLast = (float) y;
            dragLastAt = System.currentTimeMillis();
            dragSpeed = 0f;
            tl.touch(t);
            return true;
        }
        for (int i = 0; i < tl.seals(); i++) {
            if (inside(OssuaryScene.hitSeals[i], x, y)) {
                tl.clickSeal(i, t);
                if (tl.sealAt(i) < 0) SbcsAudio.playDungeon(com.sbcs.audio.Voices.vaultKnock(0, tl.seed() + i));
                return true;
            }
        }
        for (int d = 0; d < tl.drumCount() && tl.throne(); d++) {
            if (inside(OssuaryScene.hitLevers[d], x, y) && tl.canPull(d, t)) {
                tl.pull(d, OssuaryTimeline.DEFAULT_PULL, t);
                return true;
            }
        }
        if (tl.livid()) {
            for (int i = 0; i < 9; i++) {
                if (inside(OssuaryScene.hitLivids[i], x, y)) {
                    tl.pickLivid(i, t);
                    return true;
                }
            }
        }
        return true;
    }

    public void mouseDragged(double x, double y) {
        if (!dragging) return;
        long now = System.currentTimeMillis();
        float dy = (float) y - dragLast;
        long dt = Math.max(1, now - dragLastAt);
        dragSpeed = 0.6f * dragSpeed + 0.4f * Math.max(0f, dy / dt);
        dragLast = (float) y;
        dragLastAt = now;
        OssuaryScene.dragY = Math.max(0f, (float) y - dragFrom);
        tl.touch(clock.t());
    }

    public void mouseReleased(double x, double y) {
        if (!dragging) return;
        dragging = false;
        int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        float down = OssuaryScene.dragY / (0.3f * h);
        OssuaryScene.dragY = 0f;
        if (down < 0.12f) return;
        // How far, and how hard at the end: a slow long pull and a short yank both spin it well.
        float strength = Math.min(1f, 0.6f * Math.min(1f, down) + 0.4f * Math.min(1f, dragSpeed / 1.2f));
        tl.pull(0, strength, clock.t());
    }

    private static boolean inside(float[] r, double x, double y) {
        return r[2] > 0 && x >= r[0] && y >= r[1] && x < r[0] + r[2] && y < r[1] + r[3];
    }

    /** Escape, a closed menu, or a stop: it ends now. A roll that has not shown its outcome is counted anyway. */
    public void end() {
        if (ended) return;
        ended = true;
        finished = true;
        dragging = false;
        OssuaryScene.dragY = 0f;
        long t = clock.t();
        if (!test && !counted && tl.value() != null) {
            // Over before the reveal: still a chest opened, its stats as they would have been.
            if (tl.reveal() >= 0) count(Math.max(t, tl.reveal()));
        }
        if (t < tl.total()) {
            SbcsAudio.silence();
            if (tl.reveal() < 0 || t < tl.reveal()) SbcsAudio.onSkip();
        }
        summary();
    }

    /** Skip: a win jumps to its showcase, anything else ends. */
    public void skip() {
        long t = clock.t();
        long r = tl.reveal();
        if (r < 0 || t < r || !win()) {
            end();
            return;
        }
        long target = tl.stripAt();
        if (t >= target) {
            end();
            return;
        }
        clock.jump(target);
        soundAt = target;
        SbcsAudio.silence();
        SbcsAudio.onSkip();
    }

    private void summary() {
        if (summarised || tl.value() == null) return;
        summarised = true;
        ChestValue v = tl.value();
        Loot best = v.bestItem();
        String what = win() && best != null ? best.label() : "nothing rare";
        String profit = (v.profit() >= 0 ? "profit " : "loss ") + OssuaryScene.short_(Math.abs(v.profit()));
        String extra = test ? " (test)" : nth != null ? " (" + nth + ")" : "";
        if (!test && win() && v.bestWorth() > 0) com.sbcs.config.SbcsStats.get().onCoins(v.bestWorth());
        SbcsClient.summary(chest.floor().label() + " " + chest.tier().title + ": " + what + " - " + profit + extra);
    }

    // ================================================================== the canvas

    @Override
    public void quad(String tex, float ox, float oy, float ux, float uy, float vx, float vy, int u0, int v0, int uw, int vh, int argb) {
        Identifier id = TextureBank.id(tex);
        if (id == null || uw <= 0 || vh <= 0) return;
        matrix.set(ux / uw, uy / uw, vx / vh, vy / vh, ox, oy);
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.mul(matrix);
        g.blit(RenderPipelines.GUI_TEXTURED, id, 0, 0, u0, v0, uw, vh, TextureBank.width(tex), TextureBank.height(tex), argb);
        pose.popMatrix();
    }

    @Override
    public void sprite(String tex, float cx, float cy, float w, float h, float rot, int argb) {
        if ((argb >>> 24) < 2 || w < 0.5f || h < 0.5f) return;
        Identifier id = TextureBank.id(tex);
        if (id == null) return;
        int tw = TextureBank.width(tex), th = TextureBank.height(tex);
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        if (rot != 0f) pose.rotate((float) Math.toRadians(rot));
        pose.scale(w / tw, h / th);
        pose.translate(-tw / 2f, -th / 2f);
        g.blit(RenderPipelines.GUI_TEXTURED, id, 0, 0, 0, 0, tw, th, tw, th, argb);
        pose.popMatrix();
    }

    @Override
    public void line(float x0, float y0, float x1, float y1, float width, int argb) {
        float dx = x1 - x0, dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.05f || (argb >>> 24) < 3) return;
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(x0, y0);
        pose.rotate((float) Math.atan2(dy, dx));
        pose.translate(-width / 2f, -width / 2f);
        pose.scale(len + width, width);
        g.fill(0, 0, 1, 1, argb);
        pose.popMatrix();
    }

    @Override
    public void fill(float x, float y, float w, float h, int argb) {
        if ((argb >>> 24) < 3) return;
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(w, h);
        g.fill(0, 0, 1, 1, argb);
        pose.popMatrix();
    }

    @Override public void item(float cx, float cy, float size, float alpha, float yaw) { }
    @Override public void name(float cx, float cy, float scale, int argb, int letters) { }
    @Override public String creatureSkin(com.sbcs.anim.ChopTimeline.Creature creature) { return null; }
    @Override public String dropSkin() { return null; }

    @Override
    public String headSkin(String key) {
        return HeadSkins.DUNGEON.texture(key);
    }

    @Override
    public void text(String text, float cx, float cy, float scale, int argb) {
        if ((argb >>> 24) < 6) return;
        Component c = Component.literal(text);
        int a = Math.round((argb >>> 24) * 0.42f);
        float tw = (font.width(c) + 8) * scale, th = 12f * scale;
        if (a >= 4) fill(Math.round(cx - tw / 2f), Math.round(cy - th / 2f), Math.round(tw), Math.round(th), a << 24 | 0x0B0709);
        int alpha = argb >>> 24;
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        pose.scale(scale, scale);
        int x = Math.round(-font.width(c) / 2f), y = -4;
        for (int ox = -1; ox <= 1; ox++) {
            for (int oy = -1; oy <= 1; oy++) {
                if (ox != 0 || oy != 0) g.text(font, c, x + ox, y + oy, alpha << 24, false);
            }
        }
        g.text(font, c, x, y, argb, false);
        pose.popMatrix();
    }

    // ================================================================== the items

    /** Whether Hypixel's pack has a texture, asked once per path. */
    private static final Map<Identifier, Boolean> PACK = new HashMap<>();
    private static final Map<String, ItemStack> STACKS = new HashMap<>();

    /**
     * An item of the chest or of the floor's pool. The chest's own are drawn as the very ItemStack its menu showed
     * (Hypixel's model and all); the pool's from their look: Hypixel's pack icon when it is loaded, else a vanilla
     * stack - a head with its NEU skin, leather in its colour, a glint where the item has one.
     */
    @Override
    public void loot(Loot l, float cx, float cy, float size, float alpha) {
        if (l == null || size < 2f) return;
        int a = Math.round(255 * Math.max(0f, Math.min(1f, alpha)));
        if (a <= 3) return;
        if (l.stack() instanceof ItemStack real && !real.isEmpty()) {
            stack(real, cx, cy, size, a);
            return;
        }
        Identifier icon = packIcon(l.model());
        if (icon != null) {
            float s = size / 16f;
            Matrix3x2fStack pose = g.pose();
            pose.pushMatrix();
            pose.translate(cx - size / 2f, cy - size / 2f);
            pose.scale(s, s);
            g.blit(RenderPipelines.GUI_TEXTURED, icon, 0, 0, 0f, 0f, 16, 16, 16, 16, (a << 24) | 0xFFFFFF);
            pose.popMatrix();
            return;
        }
        stack(STACKS.computeIfAbsent(String.valueOf(l.key()) + "|" + l.item(), k -> make(l)), cx, cy, size, a);
    }

    /** {@code hypixel_skyblock:item/uncategorized/x} -> its texture, if the pack is loaded. */
    private static Identifier packIcon(String model) {
        if (model == null || model.indexOf(':') < 0) return null;
        try {
            String ns = model.substring(0, model.indexOf(':')), path = model.substring(model.indexOf(':') + 1);
            Identifier tex = Identifier.fromNamespaceAndPath(ns, "textures/" + path + ".png");
            return PACK.computeIfAbsent(tex, p -> Minecraft.getInstance().getResourceManager().getResource(p).isPresent()) ? tex : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static ItemStack make(Loot l) {
        try {
            ItemStack stack;
            if (l.item() != null) {
                Identifier id = Identifier.parse(l.item());
                stack = BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElse(new ItemStack(Items.PAPER));
            } else stack = new ItemStack(Items.PAPER);
            DataComponentPatch.Builder patch = DataComponentPatch.builder();
            if (l.skin() != null && stack.is(Items.PLAYER_HEAD)) {
                com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(
                        java.util.UUID.nameUUIDFromBytes(l.skin().getBytes(java.nio.charset.StandardCharsets.UTF_8)), "SbcsItem",
                        new com.mojang.authlib.properties.PropertyMap(com.google.common.collect.ImmutableMultimap.of(
                                "textures", new com.mojang.authlib.properties.Property("textures", l.skin()))));
                patch.set(DataComponents.PROFILE, net.minecraft.world.item.component.ResolvableProfile.createResolved(profile));
            }
            if (l.colour() >= 0) patch.set(DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(l.colour()));
            if (l.glint()) patch.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            stack.applyComponents(patch.build());
            return stack;
        } catch (RuntimeException e) {
            return new ItemStack(Items.PAPER);
        }
    }

    /** An ItemStack at any size: the item renderer under a scaled pose (it cannot fade - hidden below half). */
    private void stack(ItemStack s, float cx, float cy, float size, int a) {
        if (a < 128) return;
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        float sc = size / 16f;
        pose.translate(cx - 8f * sc, cy - 8f * sc);
        pose.scale(sc, sc);
        g.item(s, 0, 0);
        pose.popMatrix();
    }
}
