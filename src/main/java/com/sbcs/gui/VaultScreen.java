package com.sbcs.gui;

import com.sbcs.SbcsClient;
import com.sbcs.anim.VaultTimeline;
import com.sbcs.anim.VaultTimeline.Prize;
import com.sbcs.audio.SbcsAudio;
import com.sbcs.audio.VaultSound;
import com.sbcs.render.ChopScene;
import com.sbcs.render.HeadSkins;
import com.sbcs.render.MeshRenderer;
import com.sbcs.render.PixelLayer;
import com.sbcs.render.TextureBank;
import com.sbcs.render.VaultScene;
import com.sbcs.render.model.Mesh;
import com.sbcs.render.model.ModelProjection;
import com.sbcs.render.model.PlayerHeadMesh;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

/**
 * "The Vault" - the Crystal Nucleus loot bundle's animation, in the HUD like the other cutscenes. Thin, as
 * HellionScreen is: {@link VaultScene} draws every frame (the same code as the offline preview); this supplies the
 * GUI primitives, the clock, the sounds and the price.
 *
 * The haul is printed whole before anything is drawn, so the outcome is known when it opens - there is no pending
 * state. The sounds follow the clock and never the prize before {@link VaultTimeline#REVEAL}.
 */
public class VaultScreen extends Screen implements ChopScene.Canvas, Cutscene {

    private final VaultTimeline tl;
    private final Prize prize;
    private final boolean test;
    /** Short busts apply: every real run, and a test from the menu's "Try it" (not /sbcs test). */
    private final boolean followsBusts = SbcsClient.tryingIt;
    private final com.sbcs.anim.SceneClock clock = new com.sbcs.anim.SceneClock(
            SbcsClient.config == null ? 100 : SbcsClient.config.nucleusSpeed);
    private boolean active = true;

    private GuiGraphicsExtractor g;
    private final Matrix3x2f matrix = new Matrix3x2f();
    private final MeshRenderer meshes = new MeshRenderer();
    private static final Mesh HEAD = PlayerHeadMesh.head();
    private static final float HEAD_SPAN = PlayerHeadMesh.HEAD + 2 * PlayerHeadMesh.HAT_INFLATE;
    private static final PixelLayer LAYER = new PixelLayer("vault_fx");

    private final java.util.List<VaultSound.Cue> cues;
    private int cueAt;
    private boolean priced, landed;
    private int coinTicks;

    static final String SOURCE = "nucleus";

    public VaultScreen(Prize prize, boolean test) {
        super(Component.literal("SBCS"));
        this.prize = prize == null ? Prize.BUST : prize;
        this.test = test;
        long seed = System.nanoTime() ^ clock.openedAt();
        this.tl = new VaultTimeline(this.prize, seed);
        this.cues = VaultSound.schedule(tl);
        if (this.prize.win() && this.prize.id() != null && (SbcsClient.config == null || SbcsClient.config.livePrices)) {
            com.sbcs.market.Bazaar.request();
            com.sbcs.market.LivePrice.request(this.prize.id());
        }
        count();
        shortBust();
    }

    /** "your 3rd" for a win, "12 bundles dry" for a bust; null for a test. */
    private String nth, dry;
    private boolean counted;

    private void count() {
        if (counted || test) return;
        counted = true;
        com.sbcs.config.SbcsStats st = com.sbcs.config.SbcsStats.get();
        st.onSource(SOURCE, prize.win() ? prize.title() : null);
        if (prize.win()) nth = "your " + ChopScreen.ordinal(st.sourceDropCount(SOURCE, prize.title()));
        else {
            int d = st.source(SOURCE).dry;
            dry = d + (d == 1 ? " bundle" : " bundles") + " without a rare roll";
        }
    }

    public boolean isActive() { return active; }
    public boolean isTest() { return test; }
    public boolean counted() { return counted; }
    /** The haul is known when it opens: never waiting for its outcome. */
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
        for (String[] v : VaultScene.VANILLA) {
            TextureBank.adopt(v[0], Identifier.withDefaultNamespace("textures/block/" + v[1] + ".png"), 16, 16);
        }
        VaultScene.nthNote = nth;
        VaultScene.bustNote = dry;
        VaultScene.showBanner = SbcsClient.config == null || SbcsClient.config.nucleusBanner;
        ChopScene.calm = (SbcsClient.config != null && SbcsClient.config.calmEffects) || com.sbcs.Safety.slow();
        this.g = g;
        LAYER.begin(width, height, VaultScene.unitFor(height));
        boolean[] flushed = {false};
        VaultScene.flushLayer = () -> {
            if (flushed[0]) return;
            flushed[0] = true;
            LAYER.end(g);
        };
        try {
            VaultScene.draw(this, LAYER, tl, t, width, height);
        } finally {
            VaultScene.flushLayer = null;
        }
        if (!flushed[0]) LAYER.end(g);
        this.g = null;
        com.sbcs.render.GameplayView.draw(g, Math.min(1f, (System.currentTimeMillis() - clock.openedAt()) / 400f), 0xFF55D8FF);
    }

    // ================================================================== sound

    private static final long STALE_CUE_MS = 1_000;

    private void sounds(long t) {
        while (cueAt < cues.size() && t >= cues.get(cueAt).at) {
            VaultSound.Cue cue = cues.get(cueAt++);
            if (cue.cut) SbcsAudio.silence();
            if (t - cue.at > STALE_CUE_MS) continue;
            SbcsAudio.playNucleus(cue.voices);
        }
        if (prize.win()) {
            if (!priced && t >= tl.countStart()) {
                priced = true;
                fixPrice();
            }
            if (tl.price() > 0 && t >= tl.countStart() && t < tl.countStart() + VaultTimeline.COUNT_MS) {
                int due = (int) ((t - tl.countStart()) / 70);
                while (coinTicks <= due) SbcsAudio.onThawCoin(coinTicks++);
            }
            if (!landed && tl.price() > 0 && t >= tl.countStart() + VaultTimeline.COUNT_MS) {
                landed = true;
                SbcsAudio.onThawCoinLand();
            }
        }
    }

    /** The bazaar's instant sell (the gemstones), else the lowest BIN (the claw, the alloy, the dye). */
    private void fixPrice() {
        if (prize.id() == null) return;
        if (SbcsClient.config != null && (!SbcsClient.config.nucleusPrice || !SbcsClient.config.livePrices)) return;
        // A stack is worth its count (two Fine Jaspers); everything else that wins comes one at a time.
        int n = Math.max(1, prize.amount());
        Double sell = com.sbcs.market.Bazaar.sell(prize.id());
        if (sell != null && sell > 0) {
            tl.price(Math.round(sell * n), "bazaar instant sell");
            return;
        }
        Long bin = com.sbcs.market.LivePrice.cached(prize.id());
        if (bin != null && bin > 0) tl.price(bin * n, "lowest BIN on the auction house");
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

    /** Whether Hypixel's pack has the icon, asked once per path. */
    private static final java.util.Map<String, Boolean> PACK = new java.util.HashMap<>();

    /**
     * The drop: the Jade Dye and the Quick Claw as real 3D heads with their NEU skins; the gemstones and the alloy as
     * Hypixel's own icons when its pack is loaded, else a vanilla stand-in (quartz in the gem's colour, a gold ingot).
     */
    @Override
    public void item(float cx, float cy, float size, float alpha, float yaw) {
        int a = Math.round(255 * Math.max(0f, Math.min(1f, alpha)));
        String art = prize.art();
        if (a <= 3 || size < 2f || art == null) return;
        if (art.startsWith("head:")) {
            String skin = HeadSkins.NUCLEUS.texture(art.substring(5));
            if (skin == null) return;
            meshes.begin();
            meshes.camera(ModelProjection.Camera.of(yaw, -22f, size / (HEAD_SPAN * 1.4f)));
            meshes.add(HEAD, skin, 0f, 0f, 0f, 0f, (a << 24) | 0xFFFFFF, true, false, true);
            meshes.flush(g, cx, cy, a << 24, Math.max(1.5f, size / 16f));
            return;
        }
        String packPath, fallback;
        int tint = 0xFFFFFF;
        if (art.startsWith("gem:") || art.startsWith("fine:")) {
            boolean fine = art.startsWith("fine:");
            String c = art.substring(art.indexOf(':') + 1);
            packPath = "textures/item/collections/gemstone/" + c + "/" + (fine ? "fine_" : "flawless_") + c + "_gem.png";
            fallback = "quartz";
            tint = VaultTimeline.gemRgb(c);
        } else {
            packPath = "textures/item/uncategorized/divans_alloy.png";
            fallback = "gold_ingot";
        }
        Identifier pack = Identifier.fromNamespaceAndPath("hypixel_skyblock", packPath);
        boolean inPack = PACK.computeIfAbsent(packPath, p -> Minecraft.getInstance().getResourceManager().getResource(pack).isPresent());
        Identifier tex = pack;
        if (inPack) tint = 0xFFFFFF;
        else tex = Identifier.withDefaultNamespace("textures/item/" + fallback + ".png");
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
        String full = prize.title() + (prize.amount() > 1 ? " x" + prize.amount() : "");
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

    // ================================================================== busts, short; the summary

    private boolean shortened;

    private void shortBust() {
        if (shortened || (test && !followsBusts) || prize.win() || SbcsClient.bustStyle(SOURCE) != 1) return;
        shortened = true;
        long t = clock.t(), target = VaultTimeline.REVEAL - 450;
        if (t >= target) return;
        clock.jump(target);
        SbcsAudio.silence();
        SbcsAudio.muted(() -> sounds(target));
    }

    private boolean summarised;

    private void summary() {
        if (summarised) return;
        summarised = true;
        if (!test && prize.win() && tl.price() > 0) com.sbcs.config.SbcsStats.get().onCoins(tl.price());
        String what = prize.win() ? prize.title() : "no rare roll";
        String price = prize.win() && tl.price() > 0 ? " - " + String.format("%,d", tl.price()) + " coins" : "";
        String extra = test ? " (test)" : nth != null ? " (" + nth + ")" : "";
        SbcsClient.summary("Crystal Nucleus: " + what + price + extra);
    }

    // ================================================================== skipping

    @Override
    public boolean showsWin() {
        return active && prize.win();
    }

    @Override
    public void skip() {
        if (!active) return;
        long t = clock.t();
        long target = Math.max(tl.arrive(), tl.countStart() + VaultTimeline.COUNT_MS);
        if (!prize.win() || t >= target || target >= tl.total()) {
            summary();
            interrupt();
            return;
        }
        clock.jump(target);
        SbcsAudio.silence();
        SbcsAudio.muted(() -> sounds(target));
        SbcsAudio.onSkip();
        summary();
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
