package com.sbcs.render;

import com.sbcs.anim.OssuaryTimeline;
import com.sbcs.anim.OssuaryTimeline.Band;
import com.sbcs.anim.OssuaryTimeline.Ending;
import com.sbcs.detect.dungeon.ChestTier;
import com.sbcs.detect.dungeon.ChestValue;
import com.sbcs.detect.dungeon.Loot;
import com.sbcs.render.art.ChopArt;

import java.util.List;
import java.util.Random;

import static com.sbcs.anim.OssuaryTimeline.ease;

/**
 * Draws a frame of "The Ossuary" (a dungeon reward chest's roll) onto a {@link Canvas} - the same routine for the game
 * and the offline preview, on the Chopping Block's box renderer (its camera, boxes and ink outline).
 *
 * A drum of stone niches turns between two blackstone cheeks under a bone lintel, the floor's boss head on top -
 * the dealer. A gold bar marks the window. The chest you opened sits in front in its own block; on Master Mode it is
 * sealed with wax. An iron chain hangs on the right. On F7/M7 four drums stand side by side, one per Wither Lord,
 * each with a lever, Necron's the big one; M7 puts the five dragon statues behind them. Every texture is the game's,
 * adopted at runtime, never bundled; the dealers' heads are NEU's. Effects are pixel squares on a PixelLayer.
 *
 * Where the player can click (the chain, the seals, the levers, the Livids) is worked out while drawing and left in
 * the {@code hit*} arrays for the host - the scene is the only thing that knows where they are.
 */
public final class OssuaryScene {
    private OssuaryScene() {}

    /** ChopScene's canvas, plus what only the Ossuary draws: any item of the chest or the reel, and a dealer's head. */
    public interface Canvas extends ChopScene.Canvas {
        void loot(Loot loot, float cx, float cy, float size, float alpha);

        /** A dealer's head skin (HeadSkins.DUNGEON key: BONZO ... NECRON), or null while it is not ready. */
        String headSkin(String key);
    }

    public static final String TEX_NICHE = "oss_niche", TEX_NICHE_RED = "oss_niche_red", TEX_CHEEK = "oss_cheek",
            TEX_BONE = "oss_bone", TEX_GOLD = "oss_gold", TEX_CHAIN = "oss_chain", TEX_WEB = "oss_web",
            TEX_LANTERN = "oss_lantern", TEX_SOUL_LANTERN = "oss_soul_lantern", TEX_LEVER = "oss_lever",
            TEX_COBBLE = "oss_cobble", TEX_IRON = "oss_iron", TEX_BONE_ITEM = "oss_bone_item", TEX_WALL = "oss_wall",
            TEX_OBSIDIAN = "oss_obsidian";
    /** The game's own textures each is adopted from: {name, path under textures/}. */
    public static final String[][] VANILLA = {
            {TEX_NICHE, "block/deepslate_tiles"}, {TEX_NICHE_RED, "block/red_nether_bricks"},
            {TEX_CHEEK, "block/polished_blackstone_bricks"}, {TEX_BONE, "block/bone_block_side"},
            {TEX_GOLD, "block/gold_block"}, {TEX_CHAIN, "block/iron_chain"}, {TEX_WEB, "block/cobweb"},
            {TEX_LANTERN, "item/lantern"}, {TEX_SOUL_LANTERN, "item/soul_lantern"}, {TEX_LEVER, "block/lever"},
            {TEX_COBBLE, "block/cobblestone"}, {TEX_IRON, "block/iron_block"}, {TEX_BONE_ITEM, "item/bone"},
            {TEX_WALL, "block/chiseled_deepslate"}, {TEX_OBSIDIAN, "block/obsidian"},
    };
    /** Each chest tier's block texture: "oss_chest_<tier>". */
    public static String chestTex(ChestTier tier) {
        return "oss_chest_" + tier.name().toLowerCase(java.util.Locale.ROOT);
    }

    static final String TEX_WHITE = SeraphScene.TEX_WHITE;

    // ------------------------------------------------------------------ set by the host each frame

    /** The chain being dragged, GUI px down (0 when not). */
    public static float dragY;
    /** The price's source line under it ("bazaar instant sell"), "your 3rd", the dry streak. Null for none. */
    public static String priceNote, nthNote, dryNote;
    /** Draws the pixel layer at once, so the showcase stands over it. Set by the host. */
    public static Runnable flushLayer;
    /** The menu's banner switch. */
    public static boolean showBanner = true;

    // ------------------------------------------------------------------ hit boxes, for the host: {x, y, w, h}

    public static final float[] hitChain = new float[4];
    public static final float[][] hitSeals = new float[8][4];
    public static final float[][] hitLevers = new float[4][4];
    public static final float[][] hitLivids = new float[9][4];
    /** Where the chain's handle hangs at rest, screen px - the drag's origin. */
    public static float chainRestY;

    public static int unitFor(int h) {
        return Math.max(2, Math.round(h / 120f));
    }

    // ------------------------------------------------------------------ layout, world units

    /** A housing: where a drum sits, how big. */
    record Housing(float x, float r, float hx) {}

    static final int NICHES = 12;
    static final float STEP = (float) (2 * Math.PI / NICHES);
    static final float DRUM_Y = 3f;
    static final float CHEST_Y = -9.4f, CHEST_Z = 9f, CHEST_HX = 3.4f, CHEST_HY = 2.1f, CHEST_HZ = 2.6f;

    static Housing[] housings(OssuaryTimeline tl) {
        if (!tl.throne()) return new Housing[]{new Housing(0f, 7f, 6f)};
        return new Housing[]{new Housing(-18.5f, 4.6f, 3.2f), new Housing(-9.6f, 4.6f, 3.2f),
                new Housing(-0.7f, 4.6f, 3.2f), new Housing(11.2f, 6.6f, 5.2f)};
    }

    static int unit = 2;
    static float shade = 1f;
    static float grey = 0f;
    static long pt;

    // ================================================================== the frame

    public static void draw(Canvas c, CrystalVeins.Fill layer, OssuaryTimeline tl, long t, int w, int h) {
        float master = tl.master(t);
        c.fill(0, 0, w, h, ChopScene.alpha(tl.dim(t)) | 0x07060A);
        java.util.Arrays.fill(hitChain, 0f);
        for (float[] a : hitSeals) java.util.Arrays.fill(a, 0f);
        for (float[] a : hitLevers) java.util.Arrays.fill(a, 0f);
        for (float[] a : hitLivids) java.util.Arrays.fill(a, 0f);
        if (master <= 0.01f) return;
        ChopScene.lightCount = 0;
        ChopScene.gild = null;
        ChopScene.glintHook = null;
        ChopScene.insideOut = false;
        ChopScene.yShift = 0f;
        ChopScene.inkWidth = 0f;
        // The 3D never fades: a translucent machine shows its faces through each other. It pops in (camera) and,
        // going, darkens and shrinks away; only the 2D around it fades with the master.
        ChopScene.sceneAlpha = 1f;
        ChopScene.screenH = h;
        unit = unitFor(h);
        pt = tl.phys(t);
        grey = tl.grey(t);
        long r = tl.reveal();
        boolean win = r >= 0 && t >= r && tl.ending() != Ending.BUST;
        shade = win ? 1f - 0.5f * ease((t - tl.arrive() + 400) / 600f) : 1f;
        camera(tl, t, w, h);

        // The tell's light behind everything: the lantern's (the dragons') colour washing the back wall.
        int tell = tl.tellRgb(t);
        if (tl.tellAt() >= 0 && t >= tl.tellAt()) {
            ChopScene.project(housings(tl)[tl.drumCount() - 1].x(), DRUM_Y, -4f);
            float a = r >= 0 && t >= r ? (tl.band() == Band.BUST ? 0.10f : 0.38f) : 0.14f + 0.16f * (1f - tl.stepPulse(t));
            ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], 46f * ChopScene.size, tint(tell), a);
        }

        if (tl.dragons()) dragons(c, layer, tl, t);
        Housing[] hs = housings(tl);
        for (int d = 0; d < hs.length; d++) housing(c, layer, tl, t, d, hs[d]);
        chest(c, layer, tl, t);
        if (tl.throne()) {
            for (int d = 0; d < hs.length; d++) lever(c, tl, t, d, hs[d]);
        } else {
            chain(c, layer, tl, t, w, h);
        }
        lantern(c, tl, t);
        ChopScene.sceneAlpha = master;
        if (tl.livid()) livids(c, tl, t);
        effects(c, layer, tl, t, w, h);
        hints(c, tl, t, w, h);
        header(c, tl, t, w, h);

        ChopScene.sceneAlpha = master;
        shade = 1f;
        if (flushLayer != null) flushLayer.run();
        // Behind the showcase the machine goes back into the dark, so the drop and its words stand alone.
        if (win) c.fill(0, 0, w, h, ChopScene.alpha(0.45f * retreat(tl, t) * master) | 0x07060A);
        if (r >= 0 && t >= r) {
            if (win) showcase(c, tl, t, w, h);
            else bustWords(c, tl, t, w, h);
            strip(c, tl, t, w, h);
        }
        ChopScene.sceneAlpha = 1f;
        ChopScene.lightCount = 0;
    }

    // ================================================================== camera

    static void camera(OssuaryTimeline tl, long t, int w, int h) {
        double yaw = Math.toRadians(-14f + 3f * (float) Math.sin(pt / 1900.0));
        double pitch = Math.toRadians(-9f);
        ChopScene.cy = (float) Math.cos(yaw);
        ChopScene.sy = (float) Math.sin(yaw);
        ChopScene.cp = (float) Math.cos(pitch);
        ChopScene.sp = (float) Math.sin(pitch);
        float arrive = 0.88f + 0.12f * ChopScene.backOut01(t / (float) OssuaryTimeline.ARRIVE);
        // Leaning in through the tell.
        float lean = 0f;
        if (tl.tellAt() >= 0) lean = 0.07f * ease((t - tl.tellAt()) / (float) OssuaryTimeline.TELL_MS);
        if (tl.reveal() >= 0 && t >= tl.reveal()) lean *= 1f - ease((t - tl.reveal()) / 700f);
        float fit = tl.throne() ? Math.min(h * 0.74f / 33f, w * 0.92f / 50f) : Math.min(h * 0.74f / 33f, w * 0.9f / 30f);
        ChopScene.size = fit * arrive * (1f + lean);
        ChopScene.fx = tl.throne() ? -2.5f : 0.6f;
        ChopScene.fy = 1.8f;
        ChopScene.fz = 2f;
        // Stepping back for the showcase and the strip, up and away from the row of items along the bottom.
        float back = retreat(tl, t);
        ChopScene.size *= 1f - 0.16f * back;
        float gone = going(tl, t);
        ChopScene.size *= 1f - 0.97f * gone * gone;
        float s = ChopScene.calm ? 0f : tl.shake(t) * h;
        ChopScene.sx0 = w / 2f + s * (float) Math.sin(t * 0.13);
        ChopScene.sy0 = h * (0.5f - 0.07f * back) + s * (float) Math.cos(t * 0.17);
    }

    /** 0 -> 1 over the last FADE_MS: the machine going. */
    static float going(OssuaryTimeline tl, long t) {
        long end = tl.total();
        if (end == Long.MAX_VALUE) return 0f;
        return ease((t - (end - OssuaryTimeline.FADE_MS)) / (float) OssuaryTimeline.FADE_MS);
    }

    /** 0 -> 1 as the machine steps back: into a win's showcase, or for a bust's strip. */
    static float retreat(OssuaryTimeline tl, long t) {
        long r = tl.reveal();
        if (r < 0 || t < r) return 0f;
        long from = tl.ending() == Ending.BUST ? tl.stripAt() - 200 : tl.arrive() - 400;
        return ease((t - from) / 600f);
    }

    // ================================================================== a housing: cheeks, back, drum, lintel, dealer

    static final int[] FULL = {0, 0, 16, 16};
    static final int[][] FULL6 = {FULL, FULL, FULL, FULL, FULL, FULL};
    static final int[][] FRONTLESS = {FULL, FULL, FULL, FULL, null, FULL};

    static void housing(Canvas c, CrystalVeins.Fill layer, OssuaryTimeline tl, long t, int d, Housing hs) {
        float x = hs.x(), R = hs.r(), hx = hs.hx();
        float cheekHx = 1.1f, cheekHy = R + 2.2f, cheekHz = R + 1.4f;
        float top = DRUM_Y + R + 0.4f;
        float k = shade * (1f - 0.35f * grey);
        float[] cheek = {0.62f * k, 0.6f * k, 0.66f * k};
        // The outline round the whole housing.
        ChopScene.inkWidth = Math.max(1.2f, ChopScene.size * 0.2f);
        ChopScene.box(x, DRUM_Y, 0f, 0f, 0f, 0f, hx + 2 * cheekHx, cheekHy, cheekHz, ChopScene.all(TEX_CHEEK), FULL6, 1, 1, 1, 0, 1, c);
        ChopScene.inkWidth = 0f;
        // The back wall, dark: whatever shows between the niches.
        ChopScene.box(x, DRUM_Y, -R - 0.2f, 0f, 0f, 0f, hx, cheekHy - 0.2f, 0.6f, ChopScene.all(TEX_WALL), FULL6,
                0.25f * k, 0.24f * k, 0.28f * k, 0f, 1f, c);
        // The cheeks: the far one, the drum, then the near one.
        float left = x - hx - cheekHx, right = x + hx + cheekHx;
        boolean leftFar = ChopScene.depth(left, DRUM_Y, 0f) < ChopScene.depth(right, DRUM_Y, 0f);
        cheek(c, leftFar ? left : right, cheekHx, cheekHy, cheekHz, cheek);
        drum(c, layer, tl, t, d, hs);
        cheek(c, leftFar ? right : left, cheekHx, cheekHy, cheekHz, cheek);
        // The window: a gold bar above and below the front niche, and a tick on each cheek.
        float fh = R * (float) Math.sin(STEP / 2f) + 0.35f, fz = R * (float) Math.cos(STEP / 2f) + 0.45f;
        float[] gold = {k, 0.92f * k, 0.7f * k};
        float glowGold = tl.drum(d).stopAt >= 0 && t >= tl.drum(d).stopAt ? 0.3f * (1f - ease((t - tl.drum(d).stopAt) / 500f)) : 0f;
        for (int s = -1; s <= 1; s += 2) {
            ChopScene.inkWidth = Math.max(1f, ChopScene.size * 0.14f);
            ChopScene.box(x, DRUM_Y + s * fh, fz, 0f, 0f, 0f, hx + 0.5f, 0.32f, 0.4f, ChopScene.all(TEX_GOLD), FULL6, 1, 1, 1, 0, 1, c);
            ChopScene.inkWidth = 0f;
            ChopScene.box(x, DRUM_Y + s * fh, fz, 0f, 0f, 0f, hx + 0.5f, 0.32f, 0.4f, ChopScene.all(TEX_GOLD), FULL6,
                    gold[0], gold[1], gold[2], glowGold, 1, c);
        }
        // The lintel of bone, and the base.
        float[] bone = {0.86f * k, 0.82f * k, 0.72f * k};
        ChopScene.inkWidth = Math.max(1.2f, ChopScene.size * 0.18f);
        ChopScene.box(x, top + 1.1f, 0.6f, 0f, 0f, 0f, hx + 2 * cheekHx + 0.6f, 1.1f, cheekHz + 0.7f, ChopScene.all(TEX_BONE), FULL6, 1, 1, 1, 0, 1, c);
        ChopScene.inkWidth = 0f;
        ChopScene.box(x, top + 1.1f, 0.6f, 0f, 0f, 0f, hx + 2 * cheekHx + 0.6f, 1.1f, cheekHz + 0.7f, ChopScene.all(TEX_BONE), FULL6,
                bone[0], bone[1], bone[2], 0f, 1, c);
        if (!tl.throne() || d == 3) {
            ChopScene.inkWidth = Math.max(1.2f, ChopScene.size * 0.18f);
            ChopScene.box(x, DRUM_Y - R - 1.4f, 0.6f, 0f, 0f, 0f, hx + 2 * cheekHx + 0.6f, 1.0f, cheekHz + 0.7f, ChopScene.all(TEX_BONE), FULL6, 1, 1, 1, 0, 1, c);
            ChopScene.inkWidth = 0f;
            ChopScene.box(x, DRUM_Y - R - 1.4f, 0.6f, 0f, 0f, 0f, hx + 2 * cheekHx + 0.6f, 1.0f, cheekHz + 0.7f, ChopScene.all(TEX_BONE), FULL6,
                    bone[0] * 0.9f, bone[1] * 0.9f, bone[2] * 0.9f, 0f, 1, c);
        }
        dealer(c, tl, t, d, x, top + 2.2f, R);
    }

    static void cheek(Canvas c, float x, float hx, float hy, float hz, float[] k) {
        ChopScene.box(x, DRUM_Y, 0f, 0f, 0f, 0f, hx, hy, hz, ChopScene.all(TEX_CHEEK), FULL6, k[0], k[1], k[2], 0f, 1f, c);
    }

    /** The drum: twelve stone niches round a horizontal axle, each holding an item - or a cobweb at the stop. */
    static void drum(Canvas c, CrystalVeins.Fill layer, OssuaryTimeline tl, long t, int d, Housing hs) {
        float x = hs.x(), R = hs.r(), hx = hs.hx();
        long tt = tl.frozen(t) ? pt : t;
        float pos = tl.pos(d, tt);
        // Normal floors rise past the window; Master Mode's run backwards, falling.
        float dir = tl.red() ? -1f : 1f;
        float blur = tl.blur(d, tt);
        long base = (long) Math.floor(pos);
        float apo = R * (float) Math.cos(STEP / 2f), half = R * (float) Math.sin(STEP / 2f);
        String tex = tl.red() ? TEX_NICHE_RED : TEX_NICHE;
        float k = shade * (1f - 0.35f * grey);
        // Far niches first: the drum is convex, so drawing every face turned to the camera in depth order is exact.
        java.util.List<float[]> faces = new java.util.ArrayList<>();
        for (int j = -6; j <= 6; j++) {
            long i = base + j;
            float th = dir * (pos - i) * STEP;
            float ny = (float) Math.sin(th), nz = (float) Math.cos(th);
            float facing = -ny * ChopScene.sp + nz * ChopScene.cy * ChopScene.cp;
            if (facing <= 0.02f) continue;
            faces.add(new float[]{i, th, facing});
        }
        faces.sort((a, b) -> Float.compare(a[2], b[2]));
        for (float[] f : faces) {
            long i = (long) f[0];
            float th = f[1], facing = f[2];
            float ny = (float) Math.sin(th), nz = (float) Math.cos(th);
            // The face: a centre on the apothem, its height along the tangent.
            float cyw = DRUM_Y + ny * apo, czw = nz * apo;
            float ty = nz, tz = -ny;
            float light = 0.45f + 0.55f * Math.max(0f, ny * 0.5f + nz * 0.85f);
            int tint = 0xFF000000 | ChopScene.channel(light * k * 0.9f) << 16 | ChopScene.channel(light * k * 0.86f) << 8 | ChopScene.channel(light * k * 0.95f);
            ChopScene.worldQuad(tex, FULL, x - hx, cyw + ty * half, czw + tz * half, 2 * hx, 0f, 0f, 0f, -2 * ty * half, -2 * tz * half, tint, c);
            // The niche's dark recess, inset.
            float in = 0.78f;
            ChopScene.worldQuad(TEX_WALL, FULL, x - hx * in, cyw + ty * half * in, czw + tz * half * in, 2 * hx * in, 0f, 0f,
                    0f, -2 * ty * half * in, -2 * tz * half * in, ChopScene.alpha(0.88f) | darkTint(0.32f * light * k), c);
            // What it holds.
            ChopScene.project(x, cyw, czw);
            float sx = ChopScene.P[0], sy = ChopScene.P[1];
            float size = 2f * half * ChopScene.size * 0.82f * (0.35f + 0.65f * facing);
            int n = tl.niche(d, i, t);
            float a = Math.min(1f, facing * 1.6f) * (1f - 0.6f * blur);
            if (n == -2) {
                web(c, tl, t, x, cyw, czw, ty, tz, hx, half, light * k, i == tl.drum(d).target);
            } else if (n >= 0 && tl.value() != null && n < tl.value().chest().items().size()) {
                c.loot(tl.value().chest().items().get(n), sx, sy, size, a);
            } else {
                c.loot(tl.fillerAt(d, i), sx, sy, size, a * 0.9f);
            }
            // Speed: a smear of light down the niche when it whips past.
            if (blur > 0.05f) {
                c.fill(sx - size * 0.6f, sy - size * 0.1f, size * 1.2f, size * 0.2f, ChopScene.alpha(0.18f * blur * ChopScene.sceneAlpha) | 0xD8D0C0);
            }
        }
    }

    static int darkTint(float v) {
        int q = ChopScene.channel(v);
        return q << 16 | q << 8 | Math.min(255, q + 6);
    }

    /** The shroud: a cobweb hung over the stop niche and its neighbours; the stop's burns or crumbles at the reveal. */
    static void web(Canvas c, OssuaryTimeline tl, long t, float x, float cyw, float czw, float ty, float tz, float hx, float half,
                    float light, boolean stop) {
        long r = tl.reveal();
        float a = 1f;
        int col = 0xE8E8E8;
        if (stop && r >= 0 && t >= r) {
            float p = (t - r) / (tl.band() == Band.BUST ? 700f : 520f);
            a = 1f - ease(p);
            col = tl.band() == Band.BUST ? 0x7A7A7A : tint(tl.outcomeRgb());
            if (a <= 0.01f) return;
        } else if (stop && tl.tellAt() >= 0 && t >= tl.tellAt()) {
            col = ChopScene.mix(0xE8E8E8, tint(tl.tellRgb(t)), 0.35f + 0.3f * (1f - tl.stepPulse(t)));
        }
        float s = Math.min(2f * hx, 2f * half) * 1.05f;
        int q = ChopScene.channel(light);
        int lit = ChopScene.alpha(a * 0.95f) | ((col >> 16 & 255) * q / 255) << 16 | ((col >> 8 & 255) * q / 255) << 8 | (col & 255) * q / 255;
        // Square on the face, centred, as big as the niche is tall.
        ChopScene.worldQuad(TEX_WEB, FULL, x - s / 2f, cyw + ty * half, czw + tz * half, s, 0f, 0f, 0f, -2 * ty * half, -2 * tz * half, lit, c);
        if (stop && tl.tellAt() >= 0 && t >= tl.tellAt() && (r < 0 || t < r)) {
            ChopScene.project(x, cyw, czw + 0.4f);
            ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], 9f * ChopScene.size, tint(tl.tellRgb(t)), 0.35f + 0.35f * (1f - tl.stepPulse(t)));
        }
    }

    // ================================================================== the dealer

    static final String[] DEALER_KEYS = {"BONZO", "SCARF", "PROFESSOR", "THORN", "LIVID", "SADAN", "NECRON"};

    static void dealer(Canvas c, OssuaryTimeline tl, long t, int d, float x, float y, float R) {
        String key = tl.throne() ? OssuaryTimeline.LORDS[d] : DEALER_KEYS[tl.dealer().ordinal()];
        String skin = c.headSkin(key);
        float half = tl.throne() ? (d == 3 ? 2.6f : 2.0f) : 2.8f;
        float drop = (1f - ChopScene.backOut01((t - 200 - 70L * d) / 420f)) * 9f;
        float bob = 0.25f * (float) Math.sin(pt / 380.0 + d);
        float yaw = (float) Math.toRadians(-8f + 10f * (float) Math.sin(pt / 1300.0 + d * 0.7));
        float roll = 0f;
        long r = tl.reveal();
        if (r >= 0 && t >= r && (!tl.throne() || d == 3)) {
            float since = (t - r) / 1000f;
            if (tl.band() == Band.BUST) {
                // The boss laughs: a quick shake side to side, nodding.
                float laugh = Math.max(0f, 1f - since / 1.6f);
                roll = 0.16f * laugh * (float) Math.sin((t - r) / 45.0);
                bob += 0.5f * laugh * Math.abs((float) Math.sin((t - r) / 90.0));
            } else if (tl.ending() != Ending.WIN) {
                // A jackpot: the dealer spins round once, stunned.
                yaw += (float) (2 * Math.PI * ease(since / 1.1f));
            }
        }
        float hy = y + half + bob + drop;
        if (skin == null) {
            ChopScene.inkWidth = Math.max(1.2f, ChopScene.size * 0.2f);
            ChopScene.box(x, hy, 1f, 0f, yaw, roll, half, half, half, ChopScene.all(TEX_BONE), FULL6, 1, 1, 1, 0, 1, c);
            ChopScene.inkWidth = 0f;
            ChopScene.box(x, hy, 1f, 0f, yaw, roll, half, half, half, ChopScene.all(TEX_BONE), FULL6, 0.8f * shade, 0.78f * shade, 0.7f * shade, 0, 1, c);
            return;
        }
        float k = shade * (1f - 0.35f * grey);
        ChopScene.inkWidth = Math.max(1.5f, ChopScene.size / 3f);
        ChopScene.box(x, hy, 1f, 0f, yaw, roll, half * 1.0625f, half * 1.0625f, half * 1.0625f, ChopScene.all(skin), ChopScene.HEAD, 1, 1, 1, 0, 1, c);
        ChopScene.inkWidth = 0f;
        ChopScene.box(x, hy, 1f, 0f, yaw, roll, half, half, half, ChopScene.all(skin), ChopScene.HEAD, k, k, k, 0, 1, c);
        ChopScene.box(x, hy, 1f, 0f, yaw, roll, half * 1.0625f, half * 1.0625f, half * 1.0625f, ChopScene.all(skin), ChopScene.HAT, k, k, k, 0, 1, c);
    }

    // ================================================================== the chest

    static void chest(Canvas c, CrystalVeins.Fill layer, OssuaryTimeline tl, long t) {
        float x = tl.throne() ? -4.6f : 0f;
        float land = 1f - ChopScene.backOut01((t - 120) / 480f);
        float y = CHEST_Y + land * 14f;
        float k = shade * (1f - 0.35f * grey);
        String tex = chestTex(tl.tier());
        boolean bloodrock = tl.red() && tl.tier() == ChestTier.BEDROCK;
        float r = bloodrock ? 1f : 1f, g = bloodrock ? 0.26f : 1f, b = bloodrock ? 0.24f : 1f;
        // The lid opens a crack after the seals, wider on a win.
        float lid = 0.12f;
        if (tl.unlockAt() >= 0) lid = 0.12f + 0.16f * ease((t - tl.unlockAt()) / 400f);
        long rv = tl.reveal();
        if (rv >= 0 && t >= rv && tl.ending() != Ending.BUST) lid += 0.6f * ChopScene.backOut01((t - rv) / 500f);
        ChopScene.inkWidth = Math.max(1.2f, ChopScene.size * 0.2f);
        ChopScene.box(x, y, CHEST_Z, 0f, 0f, 0f, CHEST_HX, CHEST_HY, CHEST_HZ, ChopScene.all(tex), FULL6, 1, 1, 1, 0, 1, c);
        lidBox(c, tex, x, y, lid, 1, 1, 1, true);
        ChopScene.inkWidth = 0f;
        ChopScene.box(x, y, CHEST_Z, 0f, 0f, 0f, CHEST_HX, CHEST_HY, CHEST_HZ, ChopScene.all(tex), FULL6, r * k, g * k, b * k, 0f, 1, c);
        // The light inside, through the gap.
        ChopScene.project(x, y + CHEST_HY, CHEST_Z + CHEST_HZ * 0.4f);
        int light = tl.red() ? 0xFF2A20 : tl.tier().rgb;
        float glowA = 0.25f + 0.1f * (float) Math.sin(pt / 300.0);
        if (rv >= 0 && t >= rv) glowA = tl.band() == Band.BUST ? 0.1f : 0.6f;
        ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], 10f * ChopScene.size, tint(rv >= 0 && t >= rv && tl.band() != Band.BUST ? tl.outcomeRgb() : light), glowA);
        lidBox(c, tex, x, y, lid, r * k * 0.95f, g * k * 0.95f, b * k * 0.95f, false);
        // The latch.
        ChopScene.box(x, y + CHEST_HY - 0.5f, CHEST_Z + CHEST_HZ + 0.2f, 0f, 0f, 0f, 0.5f, 0.7f, 0.25f, ChopScene.all(TEX_IRON), FULL6,
                0.8f * k, 0.8f * k, 0.85f * k, 0f, 1, c);
        if (tl.red()) {
            // Master Mode: a red rim round every tier, and a seam that pulses along the lid.
            float pulse = 0.5f + 0.5f * (float) Math.sin(pt / 260.0);
            ChopScene.project(x - CHEST_HX, y + CHEST_HY, CHEST_Z + CHEST_HZ);
            float ax = ChopScene.P[0], ay = ChopScene.P[1];
            ChopScene.project(x + CHEST_HX, y + CHEST_HY, CHEST_Z + CHEST_HZ);
            c.line(ax, ay, ChopScene.P[0], ChopScene.P[1], Math.max(1f, ChopScene.size * 0.22f),
                    ChopScene.alpha((0.45f + 0.45f * pulse) * ChopScene.sceneAlpha) | 0xFF2418);
            if (!bloodrock) {
                for (int s = -1; s <= 1; s += 2) {
                    ChopScene.project(x + s * CHEST_HX, y - CHEST_HY, CHEST_Z + CHEST_HZ);
                    float bx = ChopScene.P[0], by = ChopScene.P[1];
                    ChopScene.project(x + s * CHEST_HX, y + CHEST_HY, CHEST_Z + CHEST_HZ);
                    c.line(bx, by, ChopScene.P[0], ChopScene.P[1], Math.max(1f, ChopScene.size * 0.3f), ChopScene.alpha(0.8f * ChopScene.sceneAlpha) | 0xA01010);
                }
            }
            seals(c, layer, tl, t, x, y);
        }
    }

    /** The lid, hinged along its back top edge and opened {@code angle} radians. */
    static void lidBox(Canvas c, String tex, float x, float y, float angle, float r, float g, float b, boolean ink) {
        float hy = 0.85f;
        float pivotY = y + CHEST_HY, pivotZ = CHEST_Z - CHEST_HZ;
        float ca = (float) Math.cos(angle), sa = (float) Math.sin(angle);
        // The lid's centre, relative to the hinge (0, hy, +HZ), turned back about X.
        float ly = hy, lz = CHEST_HZ;
        float cyw = pivotY + ly * ca + lz * sa, czw = pivotZ - ly * sa + lz * ca;
        ChopScene.box(x, cyw, czw, -angle, 0f, 0f, CHEST_HX + 0.08f, hy, CHEST_HZ + 0.08f, ChopScene.all(tex), FULL6, r, g, b, 0f, 1, c);
    }

    /** Master Mode's wax seals across the chest's front: click each, or watch them crack one by one. */
    static void seals(Canvas c, CrystalVeins.Fill layer, OssuaryTimeline tl, long t, float x, float y) {
        int n = tl.seals();
        float span = Math.min(2 * CHEST_HX - 0.8f, n * 1.5f);
        for (int i = 0; i < n; i++) {
            float sxw = n == 1 ? x : x - span / 2f + span * i / (n - 1);
            ChopScene.project(sxw, y - 0.2f, CHEST_Z + CHEST_HZ + 0.15f);
            float px = ChopScene.P[0], py = ChopScene.P[1];
            float rad = Math.max(3f, 0.62f * ChopScene.size);
            long at = tl.sealAt(i);
            float appear = ChopScene.backOut01((t - OssuaryTimeline.ARRIVE + 260 - 60L * i) / 260f);
            if (appear <= 0.01f) continue;
            if (at < 0) {
                float[] hit = hitSeals[Math.min(7, i)];
                hit[0] = px - rad * 1.3f;
                hit[1] = py - rad * 1.3f;
                hit[2] = hit[3] = rad * 2.6f;
                float wob = tl.sealHits(i) > 0 ? 0.12f * (float) Math.sin(t / 30.0) : 0f;
                waxDisc(c, px, py, rad * appear * (1f + wob), 1f, tl.sealHits(i), tl.hitsFor(i), i == tl.nextSeal() && tl.chain(), t);
            } else {
                // Cracked: two halves drop away, with flecks of wax.
                float f = (t - at) / (float) OssuaryTimeline.SEAL_FALL;
                if (f >= 1.6f) continue;
                float drop = f * f * 4f * ChopScene.size, a = Math.max(0f, 1f - f / 1.6f);
                waxHalf(c, px - f * rad * 0.8f, py + drop, rad, a, true);
                waxHalf(c, px + f * rad * 0.8f, py + drop * 1.2f, rad, a, false);
                Random rng = new Random(tl.seed() + 77L * i);
                for (int k = 0; k < 7; k++) {
                    float vx = (rng.nextFloat() - 0.5f) * 3f, vy = -1.5f - rng.nextFloat() * 2f;
                    float fx = px + vx * f * rad * 2f, fy = py + (vy * f + 5f * f * f) * rad;
                    sq(layer, fx, fy, 0xB01818, a);
                }
            }
        }
    }

    static void waxDisc(Canvas c, float cx, float cy, float rad, float a, int hits, int need, boolean next, long t) {
        int r = Math.max(2, Math.round(rad));
        int step = Math.max(1, Math.round(rad / 4f));
        float al = a * ChopScene.sceneAlpha;
        for (int yy = -r; yy <= r; yy += step) {
            for (int xx = -r; xx <= r; xx += step) {
                float dd = (float) Math.sqrt(xx * xx + yy * yy) / r;
                if (dd > 1f) continue;
                int col = dd > 0.78f ? 0x6E0C0C : (xx + yy < -r * 0.3f ? 0xD43030 : 0xA81616);
                if (dd < 0.45f && Math.abs(xx) < r * 0.12f + step) col = 0x5A0808;
                c.fill(cx + xx, cy + yy, step, step, ChopScene.alpha(al) | col);
            }
        }
        // Cracks for each click taken (M7's last seal needs three).
        for (int k = 0; k < hits && k < need; k++) {
            float ang = 0.7f + k * 2.1f;
            c.line(cx, cy, cx + (float) Math.cos(ang) * r, cy + (float) Math.sin(ang) * r, Math.max(1f, step), ChopScene.alpha(al) | 0x2A0404);
        }
        if (next) {
            float p = 0.5f + 0.5f * (float) Math.sin(t / 160.0);
            c.sprite(ChopArt.TEX_GLOW, cx, cy, r * 4.5f, r * 4.5f, 0f, ChopScene.alpha(0.25f * p * al) | 0xFF4030);
        }
    }

    static void waxHalf(Canvas c, float cx, float cy, float rad, float a, boolean left) {
        int r = Math.max(2, Math.round(rad));
        int step = Math.max(1, Math.round(rad / 4f));
        for (int yy = -r; yy <= r; yy += step) {
            for (int xx = left ? -r : 0; xx <= (left ? 0 : r); xx += step) {
                if (xx * xx + yy * yy > r * r) continue;
                c.fill(cx + xx, cy + yy, step, step, ChopScene.alpha(a * ChopScene.sceneAlpha) | 0x9A1414);
            }
        }
    }

    // ================================================================== the chain

    static void chain(Canvas c, CrystalVeins.Fill layer, OssuaryTimeline tl, long t, int w, int h) {
        Housing hs = housings(tl)[0];
        float x = hs.x() + hs.hx() + 3.6f, z = 5.5f;
        float topY = DRUM_Y + hs.r() + 3.0f, restY = -2.5f;
        ChopScene.project(x, restY, z);
        float hx0 = ChopScene.P[0], hy0 = ChopScene.P[1];
        ChopScene.project(x, topY, z);
        float tx = ChopScene.P[0], ty = ChopScene.P[1];
        chainRestY = hy0;
        // How far down the handle is: the player's drag, or the hand's stroke, then a spring back after a pull.
        float down = dragY;
        long hand = tl.handAt(0);
        if (hand >= 0 && t >= hand && t < hand + OssuaryTimeline.HAND_STROKE) {
            float p = (t - hand) / (float) OssuaryTimeline.HAND_STROKE;
            down = Math.max(down, (float) Math.sin(p * Math.PI) * 0.16f * h);
        }
        for (long[] p : tl.drum(0).pulls) {
            long dt = t - p[0];
            if (dt >= 0 && dt < 700) down = Math.max(down, 0.12f * h * (float) Math.exp(-dt / 160.0) * (float) Math.cos(dt / 70.0));
        }
        down = Math.max(0f, Math.min(0.3f * h, down));
        float hy = hy0 + down;
        // The links: the game's chain texture down a strip, tiled every 16 texels.
        float width = Math.max(3f, 0.9f * ChopScene.size);
        float len = hy - ty, tile = width * 16f / 3f;
        int tintC = 0xFF000000 | darkTint(0.85f * shade);
        float scroll = down % tile;
        for (float yy = -scroll; yy < len; yy += tile) {
            float y0 = Math.max(0f, yy), y1 = Math.min(len, yy + tile);
            if (y1 <= y0) continue;
            int v0 = Math.round((y0 - yy) / tile * 16f), vh = Math.max(1, Math.round((y1 - y0) / tile * 16f));
            if (v0 + vh > 16) vh = 16 - v0;
            if (vh <= 0) continue;
            ChopScene.emit(c, TEX_CHAIN, tx - width / 2f, ty + y0, width, 0f, 0f, y1 - y0, new int[]{6, v0, 3, vh}, tintC);
        }
        // The handle: a bone across the bottom.
        boolean waiting = tl.waitingForPlayer(t) && tl.drum(0).t0 < 0;
        float hw = Math.max(10f, 3.6f * ChopScene.size);
        if (waiting) {
            float p = 0.5f + 0.5f * (float) Math.sin(t / 180.0);
            c.sprite(ChopArt.TEX_GLOW, tx, hy, hw * 2.6f, hw * 2.6f, 0f, ChopScene.alpha(0.22f * p * ChopScene.sceneAlpha) | 0xFFE6A0);
        }
        c.sprite(TEX_BONE_ITEM, tx, hy + hw * 0.2f, hw, hw, 45f, ChopScene.alpha(ChopScene.sceneAlpha) | darkTint(0.95f * shade));
        hitChain[0] = tx - hw * 0.9f;
        hitChain[1] = hy - hw * 0.6f;
        hitChain[2] = hw * 1.8f;
        hitChain[3] = hw * 1.6f;
        // The skeleton hand, when it pulls for you: a bony claw reaching in from the right.
        if (hand >= 0 && t >= hand - 250 && t < hand + OssuaryTimeline.HAND_STROKE + 300) {
            float in = ease((t - hand + 250) / 250f) * (1f - ease((t - hand - OssuaryTimeline.HAND_STROKE) / 300f));
            claw(c, tx + hw * 0.4f + (1f - in) * 0.25f * w, hy + hw * 0.1f, Math.max(2f, ChopScene.size * 0.32f), in);
        }
        // Sparks off the chain while it moves fast.
        float v = tl.vel(0, t);
        if (v > 8f) {
            Random rng = new Random(t / 50 * 31 + tl.seed());
            for (int k = 0; k < 3; k++) {
                float sy = ty + rng.nextFloat() * (hy - ty);
                sq(layer, tx + (rng.nextFloat() - 0.5f) * width * 2f, sy, 0xFFD27A, 0.8f);
            }
        }
    }

    /** A skeleton's hand as pixel squares: a palm and four fingers curled round the handle. */
    static void claw(Canvas c, float x, float y, float px, float a) {
        int col = ChopScene.alpha(a * ChopScene.sceneAlpha) | 0xD8D2C0, dark = ChopScene.alpha(a * ChopScene.sceneAlpha) | 0x8A8270;
        String[] map = {
                "..........",
                ".#.#.#.#..",
                ".#.#.#.#..",
                ".#.#.#.#..",
                ".########.",
                ".#######..",
                "..######..",
                "...####...",
                "....##....",
                "....##....",
        };
        for (int yy = 0; yy < map.length; yy++) {
            for (int xx = 0; xx < map[yy].length(); xx++) {
                if (map[yy].charAt(xx) != '#') continue;
                c.fill(x + (xx - 1) * px - px, y + (yy - 3) * px - px, px + px, px + px, dark);
            }
        }
        for (int yy = 0; yy < map.length; yy++) {
            for (int xx = 0; xx < map[yy].length(); xx++) {
                if (map[yy].charAt(xx) != '#') continue;
                c.fill(x + (xx - 1) * px, y + (yy - 3) * px, px, px, col);
            }
        }
    }

    // ================================================================== F7/M7: the levers

    static void lever(Canvas c, OssuaryTimeline tl, long t, int d, Housing hs) {
        float x = hs.x(), y = DRUM_Y - hs.r() - 2.0f, z = hs.r() + 2.4f;
        float k = shade;
        ChopScene.inkWidth = Math.max(1f, ChopScene.size * 0.15f);
        ChopScene.box(x, y, z, 0f, 0f, 0f, 1.1f, 0.4f, 0.8f, ChopScene.all(TEX_COBBLE), FULL6, 1, 1, 1, 0, 1, c);
        ChopScene.inkWidth = 0f;
        ChopScene.box(x, y, z, 0f, 0f, 0f, 1.1f, 0.4f, 0.8f, ChopScene.all(TEX_COBBLE), FULL6, 0.8f * k, 0.8f * k, 0.8f * k, 0, 1, c);
        // The stick: up and back until it is thrown, then forward.
        var dr = tl.drum(d);
        float thrown = 0f;
        if (!dr.pulls.isEmpty()) thrown = ease((t - dr.pulls.get(0)[0]) / 160f);
        long hand = tl.handAt(d);
        if (hand >= 0 && t >= hand) thrown = Math.max(thrown, ease((t - hand) / (float) (OssuaryTimeline.HAND_STROKE / 2)));
        float ang = -0.7f + 1.4f * thrown;
        float ly = y + 0.4f + 1.4f * (float) Math.cos(ang), lz = z + 1.4f * (float) Math.sin(ang);
        ChopScene.box(x, ly, lz, ang, 0f, 0f, 0.22f, 1.4f, 0.22f, ChopScene.all(TEX_LEVER), new int[][]{
                {7, 6, 2, 10}, {7, 6, 2, 10}, {7, 6, 2, 2}, {7, 6, 2, 2}, {7, 6, 2, 10}, {7, 6, 2, 10}}, k, k, k, 0, 1, c);
        ChopScene.project(x, y + 1.2f, z + 0.5f);
        float px = ChopScene.P[0], py = ChopScene.P[1];
        float s = Math.max(14f, 3.2f * ChopScene.size);
        float[] hit = hitLevers[d];
        hit[0] = px - s / 2f;
        hit[1] = py - s / 2f;
        hit[2] = hit[3] = s;
        if (tl.chain() && dr.t0 < 0 && tl.canPull(d, t) && tl.handAt(d) < 0) {
            float p = 0.5f + 0.5f * (float) Math.sin(t / 170.0 + d);
            c.sprite(ChopArt.TEX_GLOW, px, py, s * 2.2f, s * 2.2f, 0f, ChopScene.alpha(0.22f * p * ChopScene.sceneAlpha) | 0xFFE6A0);
        }
    }

    // ================================================================== M7: the dragon statues

    static void dragons(Canvas c, CrystalVeins.Fill layer, OssuaryTimeline tl, long t) {
        long r = tl.reveal();
        int lit = tl.dragonLit(t);
        boolean bust = r >= 0 && t >= r && tl.band() == Band.BUST;
        for (int i = 0; i < 5; i++) {
            float x = -22f + i * 11f, y = DRUM_Y + 4f, z = -11f;
            float sink = bust ? ease((t - r - 150L * i) / 900f) * 6f : 0f;
            float k = shade * 0.75f;
            ChopScene.inkWidth = Math.max(1f, ChopScene.size * 0.16f);
            ChopScene.box(x, y - sink, z, 0f, 0f, 0f, 1.6f, 9f, 1.6f, ChopScene.all(TEX_OBSIDIAN), FULL6, 1, 1, 1, 0, 1, c);
            ChopScene.inkWidth = 0f;
            ChopScene.box(x, y - sink, z, 0f, 0f, 0f, 1.6f, 9f, 1.6f, ChopScene.all(TEX_OBSIDIAN), FULL6, k, k, k * 1.1f, 0, 1, c);
            int col = OssuaryTimeline.DRAGONS[i];
            boolean on = lit == i;
            float gy = y + 9f + 1.2f - sink;
            float gk = on ? 1f : 0.28f;
            int tcol = on ? tint(col) : 0x3A3A40;
            ChopScene.box(x, gy, z, 0.6f, (float) (pt / 900.0 + i), 0f, 1.1f, 1.1f, 1.1f, ChopScene.all(TEX_WHITE), FULL6,
                    (tcol >> 16 & 255) / 255f * gk + 0.1f, (tcol >> 8 & 255) / 255f * gk + 0.1f, (tcol & 255) / 255f * gk + 0.1f, on ? 0.2f : 0f, 1, c);
            if (on) {
                ChopScene.project(x, gy, z);
                float a = r >= 0 && t >= r ? 0.75f : 0.45f + 0.3f * (1f - tl.stepPulse(t));
                ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], 16f * ChopScene.size, tint(col), a);
            }
            if (bust && t >= r) {
                Random rng = new Random(tl.seed() + i);
                for (int k2 = 0; k2 < 5; k2++) {
                    float f = (t - r - 150L * i - rng.nextInt(300)) / 900f;
                    if (f < 0 || f > 1) continue;
                    ChopScene.project(x + (rng.nextFloat() - 0.5f) * 3f, gy - f * 8f, z + 1.7f);
                    sq(layer, ChopScene.P[0], ChopScene.P[1], 0x50505A, 1f - f);
                }
            }
        }
    }

    // ================================================================== the lantern (the tell)

    static void lantern(Canvas c, OssuaryTimeline tl, long t) {
        long ta = tl.tellAt();
        if (ta < 0 || t < ta - 500 || tl.dragons()) return;
        Housing hs = housings(tl)[tl.drumCount() - 1];
        float x = hs.x() - hs.hx() - 4.4f, z = 6f;
        float in = ChopScene.backOut01((t - ta + 500) / 500f);
        long r = tl.reveal();
        float swing = 0.35f * (float) Math.sin((t - ta) / 210.0) * (r >= 0 && t >= r ? Math.max(0f, 1f - (t - r) / 1200f) : 1f);
        float topY = DRUM_Y + hs.r() + 3.0f, len = 6.4f;
        float ly = topY + 10f * (1f - in) - len * (float) Math.cos(swing), lx = x + len * (float) Math.sin(swing);
        ChopScene.project(x, topY + 10f * (1f - in), z);
        float ax = ChopScene.P[0], ay = ChopScene.P[1];
        ChopScene.project(lx, ly, z);
        float bx = ChopScene.P[0], by = ChopScene.P[1];
        c.line(ax, ay, bx, by, Math.max(1f, ChopScene.size * 0.25f), ChopScene.alpha(ChopScene.sceneAlpha) | 0x3A3A44);
        int col = tint(tl.tellRgb(t));
        float pulse = 1f - tl.stepPulse(t);
        float s = 3.6f * ChopScene.size;
        ChopScene.glow(c, bx, by + s * 0.4f, s * (3.6f + 1.4f * pulse), col, 0.55f + 0.3f * pulse);
        c.sprite(tl.red() ? TEX_SOUL_LANTERN : TEX_LANTERN, bx, by + s * 0.45f, s, s, (float) Math.toDegrees(-swing),
                ChopScene.alpha(ChopScene.sceneAlpha) | ChopScene.mix(0xFFFFFF, col, 0.55f));
        ChopScene.light(lx, ly, z, 18f, col, 0.6f);
    }

    // ================================================================== F5: the shell game

    static void livids(Canvas c, OssuaryTimeline tl, long t) {
        long line = tl.liviLineAt();
        if (line < 0 || t < line) return;
        String skin = c.headSkin("LIVID");
        long pick = tl.liviPickAt();
        long r = tl.reveal();
        float end = r >= 0 ? 1f - ease((t - r) / 400f) : 1f;
        for (int i = 0; i < 9; i++) {
            float appear = ChopScene.backOut01((t - line - 45L * i) / 260f);
            if (appear <= 0.01f) continue;
            float x = -12f + i * 3f, y = -2.4f + 0.25f * (float) Math.sin(pt / 300.0 + i * 0.8), z = 12.5f;
            float a = appear * end;
            float half = 1.15f * appear;
            if (pick >= 0) {
                if (i == tl.liviPick()) {
                    y += 2.2f * ease((t - pick) / 300f);
                    half *= 1f + 0.3f * ease((t - pick) / 300f);
                } else {
                    float f = ease((t - pick) / 350f);
                    a *= 1f - f;
                    y -= 2f * f;
                }
            }
            if (a <= 0.02f) continue;
            float yaw = (float) Math.toRadians(-10f + 18f * (float) Math.sin(pt / 700.0 + i));
            String sk = skin == null ? TEX_BONE : skin;
            int[][] head = skin == null ? FULL6 : ChopScene.HEAD;
            ChopScene.inkWidth = Math.max(1f, ChopScene.size * 0.18f);
            ChopScene.box(x, y, z, 0f, yaw, 0f, half * 1.06f, half * 1.06f, half * 1.06f, ChopScene.all(sk), head, 1, 1, 1, 0, a, c);
            ChopScene.inkWidth = 0f;
            ChopScene.box(x, y, z, 0f, yaw, 0f, half, half, half, ChopScene.all(sk), head, shade, shade, shade, 0, a, c);
            if (skin != null) ChopScene.box(x, y, z, 0f, yaw, 0f, half * 1.06f, half * 1.06f, half * 1.06f, ChopScene.all(skin), ChopScene.HAT, shade, shade, shade, 0, a, c);
            ChopScene.project(x, y, z);
            float s = half * 2.4f * ChopScene.size;
            float[] hit = hitLivids[i];
            if (pick < 0) {
                hit[0] = ChopScene.P[0] - s / 2f;
                hit[1] = ChopScene.P[1] - s / 2f;
                hit[2] = hit[3] = s;
            }
            if (pick >= 0 && i == tl.liviPick()) {
                ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], s * 3f, OssuaryTimeline.Dealer.LIVID.rgb, 0.5f * a);
            }
        }
    }

    // ================================================================== effects

    static void effects(Canvas c, CrystalVeins.Fill layer, OssuaryTimeline tl, long t, int w, int h) {
        Housing[] hs = housings(tl);
        // Dust off each drum as it stops, and sparks off the window while it whips past.
        for (int d = 0; d < hs.length; d++) {
            var dr = tl.drum(d);
            float x = hs[d].x(), R = hs[d].r();
            float fh = R * (float) Math.sin(STEP / 2f) + 0.35f, fz = R * (float) Math.cos(STEP / 2f) + 0.45f;
            if (dr.stopAt >= 0 && t >= dr.stopAt && t < dr.stopAt + 900) {
                Random rng = new Random(tl.seed() + 31L * d);
                for (int k = 0; k < 18; k++) {
                    float f = (t - dr.stopAt) / 900f;
                    float vx = (rng.nextFloat() - 0.5f) * 2f * hs[d].hx(), vy = rng.nextFloat() * 2.4f;
                    ChopScene.project(x + vx * (0.6f + f), DRUM_Y - fh + vy * f - 3f * f * f, fz + 0.5f);
                    sq(layer, ChopScene.P[0], ChopScene.P[1], 0x8A8478, 1f - f);
                }
            }
            float v = tl.vel(d, tl.frozen(t) ? pt : t);
            if (v > 4f) {
                Random rng = new Random(t / 45 + 1000L * d + tl.seed());
                int n = Math.min(6, (int) (v / 4f));
                for (int k = 0; k < n; k++) {
                    float side = rng.nextBoolean() ? 1f : -1f;
                    ChopScene.project(x + (rng.nextFloat() - 0.5f) * 2f * hs[d].hx(), DRUM_Y + side * fh, fz + 0.4f);
                    sq(layer, ChopScene.P[0] + (rng.nextFloat() - 0.5f) * 6f, ChopScene.P[1] + side * rng.nextFloat() * 8f, 0xFFD27A, 0.9f);
                }
            }
        }
        long r = tl.reveal();
        if (r < 0 || t < r) return;
        Housing main = hs[hs.length - 1];
        ChopScene.project(main.x(), DRUM_Y, main.r() + 0.5f);
        float nx = ChopScene.P[0], ny = ChopScene.P[1];
        int col = tint(tl.outcomeRgb());
        long dt = t - r;
        Random rng = new Random(tl.seed() ^ 0x5EED);
        if (tl.band() == Band.BUST) {
            // The shroud crumbles to grey dust.
            for (int k = 0; k < 40; k++) {
                float f = (dt - rng.nextInt(250)) / 900f;
                float vx = (rng.nextFloat() - 0.5f) * 2.4f, vy = rng.nextFloat() * 0.6f;
                if (f < 0 || f > 1) continue;
                sq(layer, nx + vx * main.hx() * ChopScene.size * 0.5f * (0.4f + f), ny + (vy * f + 3.5f * f * f) * ChopScene.size * 2f, 0x8E8E8E, 1f - f);
            }
            return;
        }
        // The shroud burns off in the outcome's colour: embers rising off the niche.
        for (int k = 0; k < 46; k++) {
            float f = (dt - rng.nextInt(400)) / 1000f;
            if (f < 0 || f > 1) continue;
            float vx = (rng.nextFloat() - 0.5f) * 2f;
            sq(layer, nx + vx * main.hx() * ChopScene.size * (0.5f + 0.3f * f), ny - (f * 9f + rng.nextFloat()) * ChopScene.size, k % 3 == 0 ? 0xFFF4D0 : col, 1f - f);
        }
        if (tl.ending() == Ending.WIN) return;
        // A jackpot (and a dye, once it thaws): the floor's own confetti, and rays.
        long from = tl.ending() == Ending.DYE ? r + OssuaryTimeline.THAW_AFTER : r + 300;
        if (t < from) {
            if (tl.ending() == Ending.DYE) frozenDye(c, layer, tl, t, w, h);
            return;
        }
        long et = t - from;
        themed(layer, tl, et, w, h);
    }

    /** The jackpot's confetti, by floor: balloons, scarves, beams, spirits, Livid green, terracotta, wither purple. */
    static void themed(CrystalVeins.Fill layer, OssuaryTimeline tl, long et, int w, int h) {
        Random rng = new Random(tl.seed() * 13 + 5);
        int floor = Math.max(1, tl.floor().number());
        int col = tint(tl.outcomeRgb());
        int n = ChopScene.calm ? 30 : 64;
        for (int k = 0; k < n; k++) {
            float x0 = rng.nextFloat() * w, delay = rng.nextFloat() * 1600f, life = 2200f + rng.nextFloat() * 1600f;
            float f = (et - delay) / life;
            if (f < 0f || f > 1f) continue;
            int c2 = k % 4 == 0 ? 0xFFFFFF : col;
            float a = Math.min(1f, (1f - f) * 2f);
            switch (floor) {
                case 1 -> {
                    // Balloons: blobs rising with a string.
                    float y = h + 20 - f * (h + 60), x = x0 + 10f * (float) Math.sin(f * 9 + k);
                    int bc = new int[]{0xFF4F6E, 0xFFD23F, 0x4FC3FF, 0x7CFF6B, 0xC86BFF}[k % 5];
                    for (int yy = 0; yy < 3; yy++) for (int xx = 0; xx < 2; xx++) sq(layer, x + xx * unit, y + yy * unit, bc, a);
                    for (int yy = 3; yy < 7; yy++) sq(layer, x + unit * 0.5f, y + yy * unit, 0xE0E0E0, a * 0.6f);
                }
                case 2 -> {
                    // Ribbons of red scarf falling, waving.
                    float y = -20 + f * (h + 40);
                    for (int s = 0; s < 6; s++) sq(layer, x0 + 6f * (float) Math.sin(f * 12 + s * 0.6 + k), y - s * unit, 0xD62B2B, a);
                }
                case 3 -> {
                    // Guardian beams: teal lines crossing from the corners.
                    float y = h * rng.nextFloat();
                    float x = x0 + (f - 0.5f) * w * 0.4f;
                    sq(layer, x, y, col, a * 0.8f);
                }
                case 4 -> {
                    // Spirits: white wisps rising, drifting.
                    float y = h - f * h * 1.1f, x = x0 + 18f * (float) Math.sin(f * 6 + k);
                    sq(layer, x, y, 0xF2FAFF, a);
                    sq(layer, x, y + unit, 0xBFD8F0, a * 0.6f);
                }
                case 6 -> {
                    // Terracotta shards bursting out and falling.
                    float vx = (rng.nextFloat() - 0.5f) * w * 0.8f, vy = -h * (0.4f + rng.nextFloat() * 0.5f);
                    float x = w / 2f + vx * f, y = h * 0.45f + vy * f + h * 1.4f * f * f;
                    sq(layer, x, y, k % 2 == 0 ? 0xB0582C : 0x7A3A1C, a);
                }
                default -> {
                    // Livid green, wither purple: motes of the colour rising across the width.
                    float y = h - f * h, x = x0 + 8f * (float) Math.sin(f * 7 + k);
                    sq(layer, x, y, c2, a);
                }
            }
        }
    }

    /** A dye's freeze: the colour beating in from the edges three times, then flooding the screen. */
    static void frozenDye(Canvas c, CrystalVeins.Fill layer, OssuaryTimeline tl, long t, int w, int h) {
        long r = tl.reveal();
        int col = tint(tl.outcomeRgb());
        for (int i = 0; i < OssuaryTimeline.HEARTS.length; i++) {
            long b = r + OssuaryTimeline.HEARTS[i];
            float f = (t - b + 500) / 500f;
            if (f < 0f || f > 1.3f) continue;
            // A ring of squares pulled in towards the niche, as Hellion's cold ones are.
            Random rng = new Random(tl.seed() + 101L * i);
            int n = 36 + 12 * i;
            for (int k = 0; k < n; k++) {
                double ang = rng.nextDouble() * Math.PI * 2;
                float rad = (1f - Math.min(1f, f)) * Math.max(w, h) * 0.6f + 8f;
                sq(layer, w / 2f + (float) Math.cos(ang) * rad, h * 0.45f + (float) Math.sin(ang) * rad * 0.7f, col, Math.min(1f, f));
            }
        }
    }

    static void sq(CrystalVeins.Fill layer, float x, float y, int rgb, float a) {
        if (a <= 0.02f) return;
        float u = unit;
        layer.fill(Math.round(x / u) * u, Math.round(y / u) * u, u, u, ChopScene.alpha(Math.min(1f, a) * ChopScene.sceneAlpha) | (rgb & 0xFFFFFF));
    }

    /** Greyed while a dye stops the world. */
    static int tint(int rgb) {
        if (grey <= 0.01f) return rgb;
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        int l = (r * 3 + g * 6 + b) / 10;
        return ChopScene.mix(rgb, l << 16 | l << 8 | l, grey);
    }

    // ================================================================== words

    static void header(Canvas c, OssuaryTimeline tl, long t, int w, int h) {
        float a = ease((t - 250) / 400f) * ChopScene.sceneAlpha;
        long r = tl.reveal();
        if (r >= 0 && t >= r) a *= 1f - ease((t - r) / 400f);
        if (a <= 0.02f) return;
        String s = tl.floor().title() + " · " + tl.tier().title + " Chest";
        c.text(s, w / 2f, h * 0.045f + 6, 1f, ChopScene.alpha(a) | (tl.red() ? 0xFF5050 : 0xE8DCC0));
    }

    /** What the player can do, while they are being waited on (chain mode). */
    static void hints(Canvas c, OssuaryTimeline tl, long t, int w, int h) {
        if (!tl.waitingForPlayer(t)) return;
        String s;
        if (tl.seals() > 0 && tl.unlockAt() < 0) s = tl.nextSeal() == tl.seals() - 1 && tl.hitsFor(tl.nextSeal()) > 1
                ? "Break the last seal (" + (tl.hitsFor(tl.nextSeal()) - tl.sealHits(tl.nextSeal())) + " more)" : "Click the seals";
        else if (tl.livid() && tl.liviLineAt() >= 0) s = "Pick the real Livid";
        else if (tl.throne()) s = tl.drum(3).t0 < 0 && tl.canPull(3, t) ? "Throw Necron's lever" : "Throw the levers";
        else s = "Pull the chain";
        float p = 0.65f + 0.35f * (float) Math.sin(t / 220.0);
        c.text(s, w / 2f, h * 0.93f, 1f, ChopScene.alpha(p * ChopScene.sceneAlpha) | 0xFFE6A0);
    }

    /** The drop, forward: out of the niche to the middle of the screen, its name, its worth. */
    static void showcase(Canvas c, OssuaryTimeline tl, long t, int w, int h) {
        ChestValue v = tl.value();
        if (v == null || v.bestItem() == null) return;
        long arrive = tl.arrive();
        Housing main = housings(tl)[tl.drumCount() - 1];
        ChopScene.project(main.x(), DRUM_Y, main.r() + 0.4f);
        float nx = ChopScene.P[0], ny = ChopScene.P[1];
        float niche = 2f * main.r() * (float) Math.sin(STEP / 2f) * ChopScene.size * 0.82f;
        float f = ease((t - arrive + 350) / 650f);
        float strip = ease((t - tl.stripAt()) / 500f);
        float cx = nx + (w / 2f - nx) * f, cy = ny + (h * (0.38f - 0.06f * strip) - ny) * f;
        float size = niche + (Math.min(h * 0.22f, w * 0.2f) - niche) * f;
        if (tl.ending() == Ending.JACKPOT || tl.ending() == Ending.DYE) size *= 1.15f;
        int col = tl.outcomeRgb();
        // Rays and a glow behind it, in its colour.
        float a = ChopScene.sceneAlpha;
        if (t >= arrive - 300) {
            ChopScene.glow(c, cx, cy, size * 3.2f, col, 0.55f * f);
            if (tl.band() == Band.GOLD || tl.band() == Band.JACKPOT) rays(c, cx, cy, size, col, t, f);
        }
        if (t < arrive - 350 && tl.ending() == Ending.DYE) {
            // Frozen in the niche, the dye's colour beating round it.
            c.loot(v.bestItem(), cx, cy, size, a);
            return;
        }
        c.loot(v.bestItem(), cx, cy + (float) Math.sin(t / 420.0) * 2f * f, size, a);
        // The words come in under it once it has arrived.
        float wa = ease((t - arrive - 300) / 300f) * a;
        if (wa <= 0.02f) return;
        float scale = Math.min(1.35f, h / 300f);
        float y = cy + size * 0.62f + 8f * scale;
        int name = nameColour(tl);
        String title = v.bestItem().label();
        int letters = (int) ((t - arrive - 300) / 38);
        c.text(title.substring(0, Math.max(0, Math.min(title.length(), letters))), w / 2f, y, scale * 1.15f, ChopScene.alpha(wa) | name);
        y += 13f * scale;
        long worth = v.bestWorth();
        float count = ease((t - arrive - 500) / 900f);
        String price = worth > 0 ? String.format("%,d coins", Math.round(worth * count)) : "price unknown";
        c.text(price, w / 2f, y, scale, ChopScene.alpha(wa) | 0xFFD75E);
        y += 11f * scale;
        String note = nthNote != null ? nthNote : priceNote;
        if (note != null) c.text(note, w / 2f, y, scale * 0.8f, ChopScene.alpha(wa * 0.85f) | 0xB8B0A0);
        // The banner, Hypixel's own words, for the big ones.
        if (showBanner && (tl.band() == Band.GOLD || tl.band() == Band.JACKPOT)) {
            float ba = ChopScene.backOut01((t - arrive) / 260f);
            float bs = scale * (1.5f + 0.8f * (1f - Math.min(1f, ba)));
            String banner = tl.ending() == Ending.DYE ? "WOW!" : tl.band() == Band.JACKPOT ? "RARE REWARD!" : "RARE REWARD!";
            c.text("§l" + banner, w / 2f, cy - size * 0.62f - 10f * scale, bs, ChopScene.alpha(Math.min(1f, ba) * a) | (tl.ending() == Ending.DYE ? 0xFF55FF : col));
        }
    }

    /** The name's colour: the band's, a jackpot's own. */
    static int nameColour(OssuaryTimeline tl) {
        return switch (tl.band()) {
            case BUST -> 0xAAAAAA;
            case BLUE -> 0x55AAFF;
            case PURPLE -> 0xC77DFF;
            case GOLD -> 0xFFAA00;
            case JACKPOT -> ChopScene.mix(tl.outcomeRgb(), 0xFFFFFF, 0.25f);
        };
    }

    static void rays(Canvas c, float cx, float cy, float size, int col, long t, float f) {
        int n = 10;
        float len = size * 2.6f * f;
        for (int i = 0; i < n; i++) {
            double ang = i * Math.PI * 2 / n + t / 2200.0;
            float x1 = cx + (float) Math.cos(ang) * len, y1 = cy + (float) Math.sin(ang) * len;
            c.line(cx, cy, x1, y1, Math.max(2f, size * 0.09f), ChopScene.alpha(0.16f * f * ChopScene.sceneAlpha) | col);
        }
    }

    static void bustWords(Canvas c, OssuaryTimeline tl, long t, int w, int h) {
        long r = tl.reveal();
        float a = ease((t - r - 200) / 300f) * ChopScene.sceneAlpha;
        if (a <= 0.02f) return;
        float scale = Math.min(1.35f, h / 300f);
        // Up where the header was, clear of the machine.
        c.text("Nothing rare", w / 2f, h * 0.06f, scale * 1.1f, ChopScene.alpha(a) | 0x9A9A9A);
        if (dryNote != null) c.text(dryNote, w / 2f, h * 0.06f + 13f * scale, scale * 0.8f, ChopScene.alpha(a * 0.85f) | 0x8A8478);
    }

    /** The whole chest in a row along the bottom, best first, and what it comes to after its cost. */
    static void strip(Canvas c, OssuaryTimeline tl, long t, int w, int h) {
        ChestValue v = tl.value();
        if (v == null) return;
        float f = ease((t - tl.stripAt()) / 450f);
        if (f <= 0.01f) return;
        float a = f * ChopScene.sceneAlpha;
        List<Integer> order = v.ranked();
        int n = order.size();
        float scale = Math.min(1.35f, h / 300f);
        float icon = Math.min(h * 0.075f, (w * 0.86f) / Math.max(1, n) * 0.7f);
        float gap = icon * 1.35f;
        float y = h * 0.78f + (1f - f) * 20f;
        float x0 = w / 2f - gap * (n - 1) / 2f;
        c.fill(x0 - gap * 0.6f, y - icon * 0.75f, gap * (n - 1) + gap * 1.2f, icon * 1.5f, ChopScene.alpha(0.45f * a) | 0x0B0709);
        for (int k = 0; k < n; k++) {
            int i = order.get(k);
            float x = x0 + k * gap;
            float pop = ChopScene.backOut01((t - tl.stripAt() - 60L * k) / 260f);
            if (pop <= 0.01f) continue;
            if (i == v.best() && tl.band() != Band.BUST) ChopScene.glow(c, x, y, icon * 2.2f, tl.outcomeRgb(), 0.4f * a);
            c.loot(v.chest().items().get(i), x, y, icon * pop, a);
            long wv = v.worth().get(i);
            if (wv > 0) c.text(short_(wv), x, y + icon * 0.62f, scale * 0.6f, ChopScene.alpha(a * 0.9f) | 0xD8CFB8);
        }
        long cost = v.total() - v.profit();
        String line = "Worth " + short_(v.total()) + (cost > 0 ? "  ·  cost " + short_(cost) : "") + "  ·  "
                + (v.profit() >= 0 ? "profit +" : "loss -") + short_(Math.abs(v.profit())) + (v.incomplete() ? " (some prices unknown)" : "");
        c.text(line, w / 2f, y + icon * 0.75f + 13f * scale, scale * 0.85f, ChopScene.alpha(a) | (v.profit() >= 0 ? 0x7CE07C : 0xE07C7C));
    }

    /** 12.3M, 450k, 900. */
    public static String short_(long coins) {
        if (coins >= 1_000_000_000L) return String.format("%.2fB", coins / 1e9);
        if (coins >= 1_000_000L) return String.format("%.1fM", coins / 1e6);
        if (coins >= 1_000L) return String.format("%.0fk", coins / 1e3);
        return String.valueOf(coins);
    }
}
