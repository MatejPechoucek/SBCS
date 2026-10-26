package com.sbcs.gui;

import com.sbcs.SbcsClient;
import com.sbcs.anim.SlayerTimeline;
import com.sbcs.anim.SlayerTimeline.Drop;
import com.sbcs.anim.SlayerTimeline.Source;
import com.sbcs.audio.SbcsAudio;
import com.sbcs.audio.SlayerSound;
import com.sbcs.render.ChopScene;
import com.sbcs.render.HeadSkins;
import com.sbcs.render.MeshRenderer;
import com.sbcs.render.PixelLayer;
import com.sbcs.render.RasterImage;
import com.sbcs.render.SlayerRigs;
import com.sbcs.render.SlayerScene;
import com.sbcs.render.TextureBank;
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

import java.awt.image.BufferedImage;

/**
 * "Exhumed" (the Revenant Horror) and "The Brood" (the Tarantula Broodfather) - a good slayer drop's cutscene, in the
 * HUD like the others. Thin: {@link SlayerScene} draws every frame (the same code as tools/SlayerPreview); this
 * supplies the GUI primitives, the game's textures, the clock, the sounds and the price.
 *
 * It opens on the drop line, so the outcome is always known: there is no pending state and no tell.
 */
public class SlayerScreen extends Screen implements ChopScene.Canvas, Cutscene {

    private final SlayerTimeline tl;
    private final Drop drop;
    private final boolean test;
    private final String tier;
    private final com.sbcs.anim.SceneClock clock;
    private boolean active = true;

    private GuiGraphicsExtractor g;
    private final Matrix3x2f matrix = new Matrix3x2f();
    private final MeshRenderer meshes = new MeshRenderer();
    private static final Mesh HEAD = PlayerHeadMesh.head();
    private static final float HEAD_SPAN = PlayerHeadMesh.HEAD + 2 * PlayerHeadMesh.HAT_INFLATE;
    private static final PixelLayer BACK = new PixelLayer("slayer_back");
    private static final PixelLayer FRONT = new PixelLayer("slayer_front");
    /** One upload a frame each: the boss and its ending's models, and what is seen inside a body. */
    private static final RasterImage MODELS = new RasterImage("slayer_models");
    private static final RasterImage INSIDE = new RasterImage("slayer_inside");
    private static Source texturesFor;
    private Boolean packIcon;

    private final java.util.List<SlayerSound.Cue> cues;
    private int cueAt;
    private boolean priced, landed;
    private int coinTicks;
    private String nth;

    public SlayerScreen(Drop drop, boolean test, String tier) {
        super(Component.literal("SBCS"));
        this.drop = drop;
        this.test = test;
        this.tier = tier;
        this.clock = new com.sbcs.anim.SceneClock(speed(drop.source));
        long seed = System.nanoTime() ^ clock.openedAt();
        this.tl = new SlayerTimeline(drop, seed);
        this.cues = SlayerSound.schedule(tl);
        if (drop.priceId != null && (SbcsClient.config == null || SbcsClient.config.livePrices)) {
            com.sbcs.market.Bazaar.request();
            com.sbcs.market.LivePrice.request(drop.priceId);
        }
        if (drop.head != null) HeadSkins.SLAYER.texture(drop.head);
        count();
    }

    private static int speed(Source s) {
        if (SbcsClient.config == null) return 100;
        return s == Source.TARANTULA ? SbcsClient.config.spiderSpeed : SbcsClient.config.zombieSpeed;
    }

    private void count() {
        if (test) return;
        com.sbcs.config.SbcsStats st = com.sbcs.config.SbcsStats.get();
        st.onSource(drop.source.key, drop.title);
        nth = "your " + ChopScreen.ordinal(st.sourceDropCount(drop.source.key, drop.title));
    }

    public Drop drop() { return drop; }
    public boolean isActive() { return active; }
    public boolean isTest() { return test; }
    /** Always known: it opens on the drop line. */
    public boolean pending() { return false; }

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
        textures();
        SlayerScene.bannerWord = tier;
        SlayerScene.nthNote = nth;
        boolean zombie = drop.source == Source.REVENANT;
        SlayerScene.showBanner = SbcsClient.config == null || (zombie ? SbcsClient.config.zombieBanner : SbcsClient.config.spiderBanner);
        ChopScene.calm = (SbcsClient.config != null && SbcsClient.config.calmEffects) || com.sbcs.Safety.slow();
        com.sbcs.render.Punch.set(t >= tl.arrive(), true, tl.price());
        SlayerScene.rasterScale = Math.max(1, Math.min(2, Minecraft.getInstance().getWindow().getGuiScale()));
        this.g = g;
        int unit = SlayerScene.unitFor(height);
        BACK.begin(width, height, unit);
        FRONT.begin(width, height, unit);
        SlayerScene.flushBack = () -> BACK.end(g);
        SlayerScene.flushFront = () -> FRONT.end(g);
        try {
            SlayerScene.draw(this, BACK, FRONT, tl, t, width, height);
        } finally {
            SlayerScene.flushBack = null;
            SlayerScene.flushFront = null;
        }
        this.g = null;
        com.sbcs.render.GameplayView.draw(g, Math.min(1f, (System.currentTimeMillis() - clock.openedAt()) / 400f), 0xFF000000 | drop.source.colour);
    }

    // ================================================================== textures

    /** The game's own textures (a resource pack's when one is loaded), read once per slayer. */
    private void textures() {
        boolean zombie = drop.source == Source.REVENANT;
        TextureBank.adopt(SlayerScene.TEX_FLOOR_A, Identifier.withDefaultNamespace(zombie ? "textures/block/coarse_dirt.png" : "textures/block/cobblestone.png"), 16, 16);
        TextureBank.adopt(SlayerScene.TEX_FLOOR_B, Identifier.withDefaultNamespace(zombie ? "textures/block/podzol_top.png" : "textures/block/mossy_cobblestone.png"), 16, 16);
        if (texturesFor == drop.source) return;
        texturesFor = drop.source;
        if (SlayerScene.rigs == null) SlayerScene.rigs = new SlayerRigs();
        BufferedImage white = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) white.setRGB(x, y, 0xFFFFFFFF);
        both(SlayerScene.SLOT_WHITE, white);
        set(SlayerScene.SLOT_BOSS, zombie ? "textures/entity/zombie/zombie.png" : "textures/entity/spider/spider.png");
        set(SlayerScene.SLOT_ARMOUR, "textures/entity/equipment/humanoid/diamond.png");
        set(SlayerScene.SLOT_WARDEN, "textures/entity/warden/warden.png");
        set(SlayerScene.SLOT_HEART, "textures/entity/warden/warden_heart.png");
        set(SlayerScene.SLOT_BEE, "textures/entity/bee/bee.png");
        set(SlayerScene.SLOT_EYES, "textures/entity/spider/spider_eyes.png");
        set(SlayerScene.SLOT_SILVERFISH, "textures/entity/silverfish/silverfish.png");
        set(SlayerScene.SLOT_COBWEB, "textures/block/cobweb.png");
        set(SlayerScene.SLOT_BRICKS, "textures/block/bricks.png");
        set(SlayerScene.SLOT_CAVE_SPIDER, "textures/entity/spider/cave_spider.png");
    }

    private static void set(int slot, String path) {
        BufferedImage img = SbcsClient.readGameTexture(path);
        if (img != null) both(slot, img);
    }

    private static void both(int slot, BufferedImage img) {
        SlayerScene.RASTER.texture(slot, img);
        SlayerScene.INSIDE.texture(slot, img);
    }

    // ================================================================== sound and price

    private static final long STALE_CUE_MS = 1_000;

    private void sounds(long t) {
        while (cueAt < cues.size() && t >= cues.get(cueAt).at) {
            SlayerSound.Cue cue = cues.get(cueAt++);
            if (cue.cut) SbcsAudio.silence();
            if (t - cue.at > STALE_CUE_MS) continue;
            SbcsAudio.playSlayer(drop.source.key, cue.voices);
        }
        if (!priced && t >= tl.countStart()) {
            priced = true;
            fixPrice();
        }
        if (tl.price() > 0 && t >= tl.countStart() && t < tl.countStart() + SlayerTimeline.COUNT_MS) {
            int due = (int) ((t - tl.countStart()) / 70);
            while (coinTicks <= due) SbcsAudio.onThawCoin(coinTicks++);
        }
        if (!landed && tl.price() > 0 && t >= tl.countStart() + SlayerTimeline.COUNT_MS) {
            landed = true;
            SbcsAudio.onThawCoinLand();
        }
    }

    /** The bazaar's instant sell, else the lowest BIN - fixed when the count starts. */
    private void fixPrice() {
        if (drop.priceId == null || SbcsClient.config == null) return;
        boolean on = drop.source == Source.REVENANT ? SbcsClient.config.zombiePrice : SbcsClient.config.spiderPrice;
        if (!on || !SbcsClient.config.livePrices) return;
        Double sell = com.sbcs.market.Bazaar.sell(drop.priceId);
        if (sell != null && sell > 0) {
            tl.price(Math.round(sell), "bazaar instant sell");
            return;
        }
        Long bin = com.sbcs.market.LivePrice.cached(drop.priceId);
        if (bin != null && bin > 0) tl.price(bin, "lowest BIN on the auction house");
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
    public void image(String key, int[] argb, int iw, int ih, float x, float y, float w, float h) {
        (SlayerScene.TEX_INSIDE.equals(key) ? INSIDE : MODELS).draw(g, argb, iw, ih, x, y, w, h);
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
    @Override public String dropSkin() { return drop.head != null ? HeadSkins.SLAYER.texture(drop.head) : null; }

    private static final float[][] INK = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-0.71f, -0.71f}, {0.71f, -0.71f}, {-0.71f, 0.71f}, {0.71f, 0.71f}};

    /** The drop: a real 3D head with its NEU skin, else Hypixel's pack icon when the pack is loaded, else the vanilla icon. */
    @Override
    public void item(float cx, float cy, float size, float alpha, float yaw) {
        int a = Math.round(255 * Math.max(0f, Math.min(1f, alpha)));
        if (a <= 3 || size < 2f) return;
        if (drop.head != null) {
            String skin = HeadSkins.SLAYER.texture(drop.head);
            if (skin == null) return;
            meshes.begin();
            meshes.camera(ModelProjection.Camera.of(yaw, -22f, size / (HEAD_SPAN * 1.4f)));
            meshes.add(HEAD, skin, 0f, 0f, 0f, 0f, (a << 24) | 0xFFFFFF, true, false, true);
            meshes.flush(g, cx, cy, a << 24, Math.max(1.5f, size / 16f));
            return;
        }
        Identifier tex = null;
        if (drop.pack != null) {
            Identifier pack = Identifier.fromNamespaceAndPath("hypixel_skyblock", "textures/item/" + drop.pack + ".png");
            // Only from Hypixel's pack when it is loaded: a missing texture would draw the purple-black square.
            if (packIcon == null) packIcon = Minecraft.getInstance().getResourceManager().getResource(pack).isPresent();
            if (packIcon) tex = pack;
        }
        if (tex == null) tex = Identifier.withDefaultNamespace("textures/item/" + drop.icon + ".png");
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
        g.blit(RenderPipelines.GUI_TEXTURED, tex, 0, 0, 0f, 0f, 16, 16, 16, 16, (a << 24) | 0xFFFFFF);
        pose.popMatrix();
    }

    @Override
    public void name(float cx, float cy, float scale, int argb, int letters) {
        String full = drop.title;
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

    // ================================================================== the summary, skipping, leaving

    private boolean summarised;

    private void summary() {
        if (summarised) return;
        summarised = true;
        if (!test && tl.price() > 0) com.sbcs.config.SbcsStats.get().onCoins(tl.price());
        String price = tl.price() > 0 ? " - " + String.format("%,d", tl.price()) + " coins" : "";
        String extra = test ? " (test)" : nth != null ? " (" + nth + ")" : "";
        SbcsClient.summary(drop.source.boss + ": " + drop.title + price + extra);
    }

    @Override
    public boolean showsWin() {
        return active;
    }

    @Override
    public void skip() {
        if (!active) return;
        long t = clock.t();
        long target = Math.max(tl.arrive(), tl.countStart() + SlayerTimeline.COUNT_MS);
        if (t >= target || target >= tl.total()) {
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
