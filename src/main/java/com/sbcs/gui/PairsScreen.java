package com.sbcs.gui;

import com.sbcs.SbcsClient;
import com.sbcs.anim.PairsTimeline;
import com.sbcs.anim.PairsTimeline.Box;
import com.sbcs.anim.PairsTimeline.Ending;
import com.sbcs.audio.PairsSound;
import com.sbcs.audio.SbcsAudio;
import com.sbcs.render.ChopScene;
import com.sbcs.render.HeadSkins;
import com.sbcs.render.MeshRenderer;
import com.sbcs.render.PairsScene;
import com.sbcs.render.PixelLayer;
import com.sbcs.render.TextureBank;
import com.sbcs.render.model.Mesh;
import com.sbcs.render.model.ModelProjection;
import com.sbcs.render.model.PlayerHeadMesh;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

import java.util.List;

/**
 * "The Hunt" - Superpairs' reveal, in the HUD like the other cutscenes, once the game is over and its menu shut. Thin,
 * as VaultScreen is: {@link PairsScene} draws every frame (the same code as the offline preview); this supplies the GUI
 * primitives, the clock, the sounds and the prices.
 *
 * Every box's contents are known when it opens - the game showed them and the cover hid them - so there is no pending
 * state; the sounds follow the clock and never what is in a box before its reveal. Counting is the watcher's (one run per
 * game, boxes or not), so this only reads the counts back for "your Nth".
 */
public class PairsScreen extends Screen implements ChopScene.Canvas, Cutscene {

    private final PairsTimeline tl;
    private final boolean test;
    private final com.sbcs.anim.SceneClock clock = new com.sbcs.anim.SceneClock(
            SbcsClient.config == null ? 100 : SbcsClient.config.pairsSpeed);
    private boolean active = true;

    private GuiGraphicsExtractor g;
    private final Matrix3x2f matrix = new Matrix3x2f();
    private final MeshRenderer meshes = new MeshRenderer();
    private static final Mesh HEAD = PlayerHeadMesh.head();
    private static final float HEAD_SPAN = PlayerHeadMesh.HEAD + 2 * PlayerHeadMesh.HAT_INFLATE;
    private static final PixelLayer LAYER = new PixelLayer("pairs_fx");

    private final List<PairsSound.Cue> cues;
    private int cueAt;
    private final boolean[] priced;
    private final String[] nth;
    private int coinTicks = -1, coinBox = -1;

    public static final String SOURCE = "superpairs";

    public PairsScreen(List<Box> boxes, boolean test) {
        super(Component.literal("SBCS"));
        this.test = test;
        long seed = System.nanoTime() ^ clock.openedAt();
        this.tl = new PairsTimeline(boxes, seed);
        this.cues = PairsSound.schedule(tl);
        this.priced = new boolean[tl.count()];
        this.nth = new String[tl.count()];
        boolean live = SbcsClient.config == null || SbcsClient.config.livePrices;
        if (live) com.sbcs.market.Bazaar.request();
        for (int i = 0; i < tl.count(); i++) {
            Box b = tl.box(i);
            String id = b.reward().priceId();
            if (live && b.matched() && id != null) com.sbcs.market.LivePrice.request(id);
            if (!test && b.matched()) {
                nth[i] = "your " + ChopScreen.ordinal(Math.max(1, com.sbcs.config.SbcsStats.get().sourceDropCount(SOURCE, b.title())));
            }
        }
    }

    public boolean isActive() { return active; }
    @Override public boolean isTest() { return test; }
    @Override public boolean pending() { return false; }

    @Override protected void init() { }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        long t = clock.t();
        if (t >= tl.total()) {
            summary();
            close();
            return;
        }
        sounds(t);
        for (String[] v : PairsScene.VANILLA) {
            TextureBank.adopt(v[0], Identifier.withDefaultNamespace("textures/block/" + v[1] + ".png"), 16, 16);
        }
        for (int k = 0; k < 26; k++) {
            TextureBank.adopt(com.sbcs.render.art.ChopArt.glyphTex(k),
                    Identifier.withDefaultNamespace("textures/particle/sga_" + (char) ('a' + k) + ".png"), 8, 8);
        }
        PairsScene.nthNotes = nth;
        PairsScene.showBanner = SbcsClient.config == null || SbcsClient.config.pairsBanner;
        ChopScene.calm = (SbcsClient.config != null && SbcsClient.config.calmEffects) || com.sbcs.Safety.slow();
        int i = tl.index(t);
        long u = t - tl.start(i);
        Ending e = tl.box(i).ending();
        com.sbcs.render.Punch.set(u >= PairsTimeline.reveal(e), e != Ending.MISSED, tl.price(i));
        this.g = g;
        LAYER.begin(width, height, PairsScene.unitFor(height));
        boolean[] flushed = {false};
        PairsScene.flushLayer = () -> {
            if (flushed[0]) return;
            flushed[0] = true;
            LAYER.end(g);
        };
        try {
            PairsScene.draw(this, LAYER, tl, t, width, height);
        } finally {
            PairsScene.flushLayer = null;
        }
        if (!flushed[0]) LAYER.end(g);
        this.g = null;
        com.sbcs.render.GameplayView.draw(g, Math.min(1f, (System.currentTimeMillis() - clock.openedAt()) / 400f), 0xFFC77DFF);
    }

    // ================================================================== sound and prices

    private static final long STALE_CUE_MS = 1_000;

    private void sounds(long t) {
        while (cueAt < cues.size() && t >= cues.get(cueAt).at) {
            PairsSound.Cue cue = cues.get(cueAt++);
            if (cue.cut) SbcsAudio.silence();
            if (t - cue.at > STALE_CUE_MS) continue;
            SbcsAudio.playPairs(cue.voices);
        }
        int i = tl.index(t);
        long u = t - tl.start(i);
        Ending e = tl.box(i).ending();
        if (e == Ending.MISSED) return;
        long start = PairsTimeline.countStart(e);
        if (!priced[i] && u >= start) {
            priced[i] = true;
            fixPrice(i);
        }
        if (tl.price(i) > 0 && u >= start && u < start + PairsTimeline.COUNT_MS) {
            if (coinBox != i) {
                coinBox = i;
                coinTicks = 0;
            }
            int due = (int) ((u - start) / 70);
            while (coinTicks <= due) SbcsAudio.onThawCoin(coinTicks++);
        }
    }

    /** The bazaar's instant sell (books, the serum), else the lowest BIN (the dye, the fish, the pet). */
    private void fixPrice(int i) {
        String id = tl.box(i).reward().priceId();
        if (id == null) return;
        if (SbcsClient.config != null && (!SbcsClient.config.pairsPrice || !SbcsClient.config.livePrices)) return;
        Double sell = com.sbcs.market.Bazaar.sell(id);
        if (sell != null && sell > 0) {
            tl.price(i, Math.round(sell), "bazaar instant sell");
            return;
        }
        Long bin = com.sbcs.market.LivePrice.cached(id);
        if (bin != null && bin > 0) tl.price(i, bin, "lowest BIN on the auction house");
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

    @Override public String creatureSkin(com.sbcs.anim.ChopTimeline.Creature creature) { return null; }
    @Override public String dropSkin() { return null; }

    private static final float[][] INK = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-0.71f, -0.71f}, {0.71f, -0.71f}, {-0.71f, 0.71f}, {0.71f, 0.71f}};

    /**
     * The reward: the dye, the serum and the Guardian as real 3D heads with their NEU skins; a book as the game's
     * enchanted book icon; Experiment the Fish as a pufferfish (its item in vanilla). A box that got away draws grey.
     */
    @Override
    public void item(float cx, float cy, float size, float alpha, float yaw) {
        int a = Math.round(255 * Math.max(0f, Math.min(1f, alpha)));
        Box box = tl.box(Math.min(PairsScene.current, tl.count() - 1));
        String art = box.reward().art();
        if (a <= 3 || size < 2f || art == null) return;
        int tint = PairsScene.greyItem ? 0x7A7A80 : 0xFFFFFF;
        if (art.startsWith("head:")) {
            String skin = HeadSkins.PAIRS.texture(art.substring(5));
            if (skin == null) return;
            meshes.begin();
            meshes.camera(ModelProjection.Camera.of(yaw, -22f, size / (HEAD_SPAN * 1.4f)));
            meshes.add(HEAD, skin, 0f, 0f, 0f, 0f, (a << 24) | tint, true, false, true);
            meshes.flush(g, cx, cy, a << 24, Math.max(1.5f, size / 16f));
            return;
        }
        Identifier tex = Identifier.withDefaultNamespace("textures/item/" + (art.equals("fish") ? "pufferfish" : "enchanted_book") + ".png");
        float s = size / 16f, ink = Math.max(1.5f, size / 16f);
        int x = Math.round(cx - size / 2f), y = Math.round(cy - size / 2f);
        Matrix3x2fStack pose = g.pose();
        for (float[] o : INK) {
            pose.pushMatrix();
            pose.translate(x + o[0] * ink, y + o[1] * ink);
            pose.scale(s, s);
            g.blit(RenderPipelines.GUI_TEXTURED, tex, 0, 0, 0f, 0f, 16, 16, 16, 16, a << 24);
            pose.popMatrix();
        }
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(s, s);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, 0, 0, 0f, 0f, 16, 16, 16, 16, (a << 24) | tint);
        pose.popMatrix();
    }

    @Override
    public void name(float cx, float cy, float scale, int argb, int letters) {
        String full = tl.box(Math.min(PairsScene.current, tl.count() - 1)).title();
        int n = Math.min(letters, full.length());
        if (n <= 0) return;
        plate(cx, cy, scale, font.width(full), argb);
        if (n >= full.length()) {
            outlined(Component.literal(full), cx, cy, scale, argb);
            return;
        }
        Component part = Component.literal(full.substring(0, n));
        float shift = (font.width(part) - font.width(full)) / 2f * scale;
        outlined(part, cx + shift, cy, scale, argb);
    }

    @Override
    public void text(String text, float cx, float cy, float scale, int argb) {
        plate(cx, cy, scale, font.width(text), argb);
        outlined(Component.literal(text), cx, cy, scale, argb);
    }

    private void plate(float cx, float cy, float scale, int textW, int argb) {
        int a = Math.round((argb >>> 24) * 0.42f);
        if (a < 4) return;
        float w = (textW + 8) * scale, h = 12f * scale;
        fill(Math.round(cx - w / 2f), Math.round(cy - h / 2f), Math.round(w), Math.round(h), a << 24 | 0x0B0709);
    }

    private void outlined(Component text, float cx, float cy, float scale, int argb) {
        if ((argb >>> 24) < 6) return;
        int alpha = argb >>> 24;
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        pose.scale(scale, scale);
        int x = Math.round(-font.width(text) / 2f), y = -4;
        for (int ox = -1; ox <= 1; ox++) {
            for (int oy = -1; oy <= 1; oy++) {
                if (ox != 0 || oy != 0) g.text(font, text, x + ox, y + oy, alpha << 24, false);
            }
        }
        g.text(font, text, x, y, argb, false);
        pose.popMatrix();
    }

    // ================================================================== the summary, skipping

    private boolean summarised;

    private void summary() {
        if (summarised) return;
        summarised = true;
        StringBuilder found = new StringBuilder();
        long coins = 0;
        int lost = 0;
        for (int i = 0; i < tl.count(); i++) {
            Box b = tl.box(i);
            if (!b.matched()) {
                lost++;
                continue;
            }
            if (found.length() > 0) found.append(", ");
            found.append(b.title());
            if (tl.price(i) > 0) coins += tl.price(i);
        }
        if (!test && coins > 0) com.sbcs.config.SbcsStats.get().onCoins(coins);
        String what = found.length() > 0 ? found.toString() : "nothing rare found";
        String extra = (lost > 0 ? " - " + lost + (lost == 1 ? " box" : " boxes") + " got away" : "")
                + (coins > 0 ? " - " + String.format("%,d", coins) + " coins" : "") + (test ? " (test)" : "");
        SbcsClient.summary("Superpairs: " + what + extra);
    }

    @Override
    public boolean showsWin() {
        if (!active) return false;
        for (Box b : tl.boxes()) if (b.matched()) return true;
        return false;
    }

    /** The skip key: to the current box's showcase, price counted; from there, on to the next box or the end. */
    @Override
    public void skip() {
        if (!active) return;
        long t = clock.t();
        int i = tl.index(t);
        Ending e = tl.box(i).ending();
        long target = tl.start(i) + Math.max(PairsTimeline.arrive(e), PairsTimeline.countStart(e) + PairsTimeline.COUNT_MS);
        if (e == Ending.MISSED || t >= target) target = tl.start(i) + PairsTimeline.end(e);
        if (target >= tl.total()) {
            summary();
            interrupt();
            return;
        }
        long to = target;
        clock.jump(to);
        SbcsAudio.silence();
        SbcsAudio.muted(() -> sounds(to));
        SbcsAudio.onSkip();
    }

    @Override
    public void interrupt() {
        if (!active) return;
        SbcsAudio.silence();
        SbcsAudio.onSkip();
        close();
    }

    private void close() {
        com.sbcs.render.GameplayView.release();
        if (!active) return;
        active = false;
        SbcsAudio.letRing();
        com.sbcs.render.CutsceneHud.hide(this);
    }

    @Override
    public void onClose() {
        close();
    }
}
