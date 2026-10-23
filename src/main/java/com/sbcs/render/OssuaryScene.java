package com.sbcs.render;

import com.sbcs.anim.OssuaryTimeline;
import com.sbcs.anim.OssuaryTimeline.Band;
import com.sbcs.anim.OssuaryTimeline.Ending;
import com.sbcs.detect.dungeon.ChestValue;
import com.sbcs.detect.dungeon.Loot;
import com.sbcs.render.art.ChopArt;

import java.util.List;
import java.util.Locale;
import java.util.Random;

import static com.sbcs.anim.OssuaryTimeline.ease;

/**
 * Draws a frame of "The Ossuary" (a dungeon reward chest's roll) onto a {@link Canvas} - the same routine for the game
 * and the offline preview, on the Chopping Block's box renderer (its camera, boxes and ink outline).
 *
 * <b>The wheel (2026-10-23).</b> Matej's friends found the drums and the F7 levers "not it" and the chest models off,
 * and asked for "one big wheel that looks super cool with loads of light and detail". So every floor is now one big
 * prize wheel on a blackstone stand, standing on a plinth of the chest's own block, the floor's boss head on its hub -
 * the dealer. Sixteen segments, three of them veiled under cobwebs; a ring of bulbs round the rim that chase while it
 * idles, run rainbow while it spins, flash the tell's colours and land on the truth; a gold flapper at the top that
 * ticks off every peg. Master Mode's wheel is red, barred with wax seals. M7's five dragon gems crown it.
 *
 * The wheel's face is one image composed on the CPU ({@link #face}) and drawn turned through the camera's affine map -
 * a flat disc under an orthographic camera is a parallelogram of its square, as Eruption's lava floor is. Every other
 * texture is the game's, adopted at runtime, never bundled; the dealers' heads are NEU's. Effects are pixel squares on
 * a PixelLayer.
 *
 * Where the player can click (the wheel, the seals, the Livids) is worked out while drawing and left in the
 * {@code hit*} arrays for the host - the scene is the only thing that knows where they are.
 */
public final class OssuaryScene {
    private OssuaryScene() {}

    /** ChopScene's canvas, plus what only the Ossuary draws: any item of the chest or the wheel, and a dealer's head. */
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
    public static String chestTex(com.sbcs.detect.dungeon.ChestTier tier) {
        return "oss_chest_" + tier.name().toLowerCase(Locale.ROOT);
    }

    static final String TEX_WHITE = SeraphScene.TEX_WHITE;
    /** The wheel's face image, as the host's raster knows it. */
    public static final String WHEEL_IMAGE = "oss_wheel";

    // ------------------------------------------------------------------ set by the host each frame

    /** The wheel being dragged, GUI px down (0 when not). */
    public static float dragY;
    /** The price's source line under it ("bazaar instant sell"), "your 3rd", the dry streak. Null for none. */
    public static String priceNote, nthNote, dryNote;
    /** Draws the pixel layer at once, so the showcase stands over it. Set by the host. */
    public static Runnable flushLayer;
    /** The menu's banner switch. */
    public static boolean showBanner = true;

    // ------------------------------------------------------------------ hit boxes, for the host: {x, y, w, h}

    /** The wheel: drag it down to spin it. */
    public static final float[] hitChain = new float[4];
    public static final float[][] hitSeals = new float[8][4];
    public static final float[][] hitLivids = new float[9][4];

    public static int unitFor(int h) {
        return Math.max(2, Math.round(h / 120f));
    }

    // ------------------------------------------------------------------ layout, world units

    static final int SEG = OssuaryTimeline.SEGMENTS;
    static final float STEP = (float) (2 * Math.PI / SEG);
    /** The hub's height, the wheel's radius (to the rim's outer edge), and the plane of its face. */
    static final float WY = 2f, R = 13f, WZ = 0f;
    /** Where on the wheel the items sit, and the bulbs, as a share of the radius. */
    static final float ITEM_R = 0.64f, BULB_R = 0.93f;
    /** The face image, texels across. */
    static final int TEX_N = 128;
    /** The plinth: the chest's own block under the wheel. */
    static final float PLINTH_Y = WY - R - 2.6f, PLINTH_HY = 1.6f;
    /** Master Mode's sealed bar, across the wheel's lower half. */
    static final float BAR_Y = WY - 6.2f, BAR_Z = 4.2f;

    static int unit = 2;
    static float shade = 1f;
    static float grey = 0f;
    static long pt;
    /** The drag's wind-up as drawn, niches: eased back after the wheel is let go. */
    static float windShown;

    // ================================================================== the frame

    public static void draw(Canvas c, CrystalVeins.Fill layer, OssuaryTimeline tl, long t, int w, int h) {
        float master = tl.master(t);
        c.fill(0, 0, w, h, ChopScene.alpha(tl.dim(t)) | 0x07060A);
        java.util.Arrays.fill(hitChain, 0f);
        for (float[] a : hitSeals) java.util.Arrays.fill(a, 0f);
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
        float target = Math.min(0.6f, dragY / Math.max(1f, R * ChopScene.size * STEP));
        windShown = target > windShown ? target : windShown * 0.55f;

        backLight(c, tl, t);
        if (tl.dragons()) crown(c, layer, tl, t);
        stand(c, tl);
        wheel(c, layer, tl, t);
        pointer(c, tl, t);
        dealer(c, tl, t);
        if (tl.seals() > 0) bar(c, layer, tl, t);
        hand(c, tl, t, w);
        ChopScene.sceneAlpha = master;
        if (tl.livid()) livids(c, tl, t);
        effects(c, layer, tl, t, w, h);
        hints(c, tl, t, w, h);
        header(c, tl, t, w, h);

        ChopScene.sceneAlpha = master;
        shade = 1f;
        if (flushLayer != null) flushLayer.run();
        // Behind the showcase the wheel goes back into the dark, so the drop and its words stand alone.
        if (win) c.fill(0, 0, w, h, ChopScene.alpha(0.45f * retreat(tl, t) * master) | 0x07060A);
        if (r >= 0 && t >= r) {
            if (win) showcase(c, layer, tl, t, w, h);
            else bustWords(c, tl, t, w, h);
            strip(c, tl, t, w, h);
        }
        ChopScene.sceneAlpha = 1f;
        ChopScene.lightCount = 0;
    }

    // ================================================================== camera

    static void camera(OssuaryTimeline tl, long t, int w, int h) {
        double yaw = Math.toRadians(-9f + 3f * (float) Math.sin(pt / 1900.0));
        double pitch = Math.toRadians(-7f);
        ChopScene.cy = (float) Math.cos(yaw);
        ChopScene.sy = (float) Math.sin(yaw);
        ChopScene.cp = (float) Math.cos(pitch);
        ChopScene.sp = (float) Math.sin(pitch);
        float arrive = 0.88f + 0.12f * ChopScene.backOut01(t / (float) OssuaryTimeline.ARRIVE);
        // Leaning in through the tell.
        float lean = 0f;
        if (tl.tellAt() >= 0) lean = 0.07f * ease((t - tl.tellAt()) / (float) OssuaryTimeline.TELL_MS);
        if (tl.reveal() >= 0 && t >= tl.reveal()) lean *= 1f - ease((t - tl.reveal()) / 700f);
        float tall = tl.dragons() ? 42f : 37f;
        float fit = Math.min(h * 0.86f / tall, w * 0.9f / 40f);
        ChopScene.size = fit * arrive * (1f + lean);
        ChopScene.fx = 0f;
        ChopScene.fy = tl.dragons() ? WY + 1.6f : WY - 0.6f;
        ChopScene.fz = 0f;
        // Stepping back for the showcase and the strip, up and away from the row of items along the bottom.
        float back = retreat(tl, t);
        ChopScene.size *= 1f - 0.16f * back;
        float gone = going(tl, t);
        ChopScene.size *= 1f - 0.97f * gone * gone;
        float s = ChopScene.calm ? 0f : tl.shake(t) * h;
        ChopScene.sx0 = w / 2f + s * (float) Math.sin(t * 0.13);
        ChopScene.sy0 = h * (0.5f - 0.07f * back) + s * (float) Math.cos(t * 0.17);
    }

    /** 0 -> 1 over the last FADE_MS: the wheel going. */
    static float going(OssuaryTimeline tl, long t) {
        long end = tl.total();
        if (end == Long.MAX_VALUE) return 0f;
        return ease((t - (end - OssuaryTimeline.FADE_MS)) / (float) OssuaryTimeline.FADE_MS);
    }

    /** 0 -> 1 as the wheel steps back: into a win's showcase, or for a bust's strip. */
    static float retreat(OssuaryTimeline tl, long t) {
        long r = tl.reveal();
        if (r < 0 || t < r) return 0f;
        long from = tl.ending() == Ending.BUST ? tl.stripAt() - 200 : tl.arrive() - 400;
        return ease((t - from) / 600f);
    }

    static final int[] FULL = {0, 0, 16, 16};
    static final int[][] FULL6 = {FULL, FULL, FULL, FULL, FULL, FULL};

    // ================================================================== the wheel's state

    /** Which way the wheel turns: Master Mode's runs backwards. */
    static float dir(OssuaryTimeline tl) {
        return tl.red() ? -1f : 1f;
    }

    /** The wheel's position, niches, as drawn: frozen in a dye's stopped world, wound back while it is dragged. */
    static float wheelPos(OssuaryTimeline tl, long t) {
        long tt = tl.frozen(t) ? pt : t;
        return tl.pos(0, tt) - windShown;
    }

    /** The wheel's turn, radians, counter-clockwise. Segment k sits under the pointer when this is k * STEP. */
    static float theta(OssuaryTimeline tl, long t) {
        return dir(tl) * wheelPos(tl, t) * STEP;
    }

    /** Segment {@code k}'s middle on the face, in the wheel's own (unturned) frame. */
    static float segAngle(OssuaryTimeline tl, float k) {
        return (float) (Math.PI / 2) - dir(tl) * k * STEP;
    }

    /** The segment index (unbounded, as the timeline counts) showing in slot {@code k} nearest the pointer now. */
    static long indexOf(int k, float pos) {
        return k + SEG * (long) Math.round((pos - k) / SEG);
    }

    /** How fast it is spinning, 0..1 - the bulbs race and the items smear past it. */
    static float speed(OssuaryTimeline tl, long t) {
        return tl.blur(0, tl.frozen(t) ? pt : t);
    }

    /** The band's strength after the reveal: 0 a bust, 1 blue, 2 purple, 3 gold, 4 a jackpot or a dye. */
    static int power(OssuaryTimeline tl) {
        return switch (tl.band()) {
            case BUST -> 0;
            case BLUE -> 1;
            case PURPLE -> 2;
            case GOLD -> 3;
            case JACKPOT -> 4;
        };
    }

    /** 0 -> 1 as a bust's wheel dies: its bulbs going out one by one and its colour draining. */
    static float dying(OssuaryTimeline tl, long t) {
        long r = tl.reveal();
        if (r < 0 || t < r || tl.band() != Band.BUST) return 0f;
        return ease((t - r - 150) / 1300f);
    }

    // ================================================================== behind it

    /** The tell's light washing the wall behind, and after a win rays turning behind the wheel. */
    static void backLight(Canvas c, OssuaryTimeline tl, long t) {
        long r = tl.reveal();
        ChopScene.project(0f, WY, WZ - 4f);
        float x = ChopScene.P[0], y = ChopScene.P[1];
        float rad = R * ChopScene.size;
        if (tl.tellAt() >= 0 && t >= tl.tellAt()) {
            int tell = tint(tl.tellRgb(t));
            float a = r >= 0 && t >= r ? (tl.band() == Band.BUST ? 0.08f : 0.42f) : 0.16f + 0.2f * (1f - tl.stepPulse(t));
            ChopScene.glow(c, x, y, rad * 4.2f, tell, a);
        } else {
            ChopScene.glow(c, x, y, rad * 3.4f, 0xFFD890, 0.10f + 0.10f * speed(tl, t));
        }
        if (r < 0 || t < r || tl.band() == Band.BUST) return;
        // A win: rays of the outcome's colour wheeling round behind it, more and longer the bigger it is.
        int p = power(tl);
        float in = ease((t - r) / 450f) * (1f - retreat(tl, t) * 0.6f);
        int n = 6 + 4 * p;
        int col = tint(tl.outcomeRgb());
        for (int i = 0; i < n; i++) {
            double ang = i * Math.PI * 2 / n + (t - r) / (2600.0 - 300.0 * p) * (i % 2 == 0 ? 1 : -1) * 0.6;
            float len = rad * (1.25f + 0.35f * p) * in;
            c.line(x, y, x + (float) Math.cos(ang) * len, y + (float) Math.sin(ang) * len, Math.max(2f, rad * (0.05f + 0.02f * p)),
                    ChopScene.alpha(0.13f * in * ChopScene.sceneAlpha) | col);
        }
    }

    /** The stand: two blackstone legs from the axle to the plinth, the plinth of the chest's own block, a bone trim. */
    static void stand(Canvas c, OssuaryTimeline tl) {
        float k = shade * (1f - 0.35f * grey);
        String block = chestTex(tl.tier());
        float top = PLINTH_Y + PLINTH_HY;
        for (int s = -1; s <= 1; s += 2) {
            float x0 = 0f, y0 = WY, x1 = s * 9.5f, y1 = top + 0.4f;
            float cx = (x0 + x1) / 2f, cyw = (y0 + y1) / 2f;
            float len = (float) Math.hypot(x1 - x0, y1 - y0) / 2f;
            float az = (float) Math.atan2(x1 - x0, -(y1 - y0)) * -1f;
            ChopScene.inkWidth = Math.max(1.2f, ChopScene.size * 0.2f);
            ChopScene.box(cx, cyw, WZ - 2.2f, 0f, 0f, az, 1.0f, len, 0.9f, ChopScene.all(TEX_CHEEK), FULL6, 1, 1, 1, 0, 1, c);
            ChopScene.inkWidth = 0f;
            ChopScene.box(cx, cyw, WZ - 2.2f, 0f, 0f, az, 1.0f, len, 0.9f, ChopScene.all(TEX_CHEEK), FULL6,
                    0.55f * k, 0.53f * k, 0.6f * k, 0f, 1, c);
        }
        boolean red = tl.red();
        float rr = red ? 1f : 1f, gg = red ? 0.55f : 1f, bb = red ? 0.52f : 1f;
        ChopScene.inkWidth = Math.max(1.2f, ChopScene.size * 0.2f);
        ChopScene.box(0f, PLINTH_Y, WZ, 0f, 0f, 0f, R + 0.6f, PLINTH_HY, 5.2f, ChopScene.all(block), FULL6, 1, 1, 1, 0, 1, c);
        ChopScene.inkWidth = 0f;
        ChopScene.box(0f, PLINTH_Y, WZ, 0f, 0f, 0f, R + 0.6f, PLINTH_HY, 5.2f, ChopScene.all(block), FULL6,
                rr * k * 0.92f, gg * k * 0.92f, bb * k * 0.92f, 0f, 1, c);
        float[] bone = {0.86f * k, 0.82f * k, 0.72f * k};
        ChopScene.box(0f, top + 0.3f, WZ, 0f, 0f, 0f, R + 1.1f, 0.3f, 5.6f, ChopScene.all(TEX_BONE), FULL6,
                bone[0], bone[1], bone[2], 0f, 1, c);
    }

    // ================================================================== the wheel

    /** The face's fixed geometry, per texel: radius (share of R), angle in the wheel's own frame. */
    static final float[] TEX_RHO = new float[TEX_N * TEX_N], TEX_PHI = new float[TEX_N * TEX_N];
    /** Per-texel noise, fixed, for the pixel-art grain. */
    static final float[] TEX_NOISE = new float[TEX_N * TEX_N];
    static final int[] FACE = new int[TEX_N * TEX_N];

    static {
        Random n = new Random(0x0551);
        for (int j = 0; j < TEX_N; j++) {
            for (int i = 0; i < TEX_N; i++) {
                float x = (i + 0.5f - TEX_N / 2f) / (TEX_N / 2f), y = (TEX_N / 2f - j - 0.5f) / (TEX_N / 2f);
                TEX_RHO[j * TEX_N + i] = (float) Math.hypot(x, y);
                TEX_PHI[j * TEX_N + i] = (float) Math.atan2(y, x);
                TEX_NOISE[j * TEX_N + i] = n.nextFloat();
            }
        }
    }

    static void wheel(Canvas c, CrystalVeins.Fill layer, OssuaryTimeline tl, long t) {
        float pos = wheelPos(tl, t), th = theta(tl, t);
        // The plane's affine map: the hub, and a world unit along x and y.
        ChopScene.project(0f, WY, WZ);
        float ox = ChopScene.P[0], oy = ChopScene.P[1];
        ChopScene.project(1f, WY, WZ);
        float exx = ChopScene.P[0] - ox, exy = ChopScene.P[1] - oy;
        ChopScene.project(0f, WY + 1f, WZ);
        float eyx = ChopScene.P[0] - ox, eyy = ChopScene.P[1] - oy;

        // The outline: the disc stamped in black a little larger behind the face.
        float cs = (float) Math.cos(th), sn = (float) Math.sin(th);
        face(tl, t, pos);
        float ink = Math.max(1.5f, ChopScene.size * 0.25f);
        // The silhouette: every opaque texel black.
        for (int q = 0; q < FACE.length; q++) INK_FACE[q] = (FACE[q] >>> 24) > 0 ? 0xFF000000 : 0;
        for (float[] o : ChopScene.INK) {
            drawFace(c, ox + o[0] * ink, oy + o[1] * ink, exx, exy, eyx, eyy, cs, sn, true);
        }
        drawFace(c, ox, oy, exx, exy, eyx, eyy, cs, sn, false);

        // Hit box: the whole disc, for dragging it round.
        float rad = R * ChopScene.size;
        hitChain[0] = ox - rad;
        hitChain[1] = oy - rad;
        hitChain[2] = hitChain[3] = 2 * rad;

        // What each segment holds, upright, on the turned face.
        long r = tl.reveal();
        float blur = speed(tl, t);
        float k = shade * (1f - 0.35f * grey);
        for (int s = 0; s < SEG; s++) {
            long i = indexOf(s, pos);
            float a = segAngle(tl, s) + th;
            float wx = (float) Math.cos(a) * ITEM_R * R, wy = WY + (float) Math.sin(a) * ITEM_R * R;
            ChopScene.project(wx, wy, WZ + 0.3f);
            float sx = ChopScene.P[0], sy = ChopScene.P[1];
            float size = 2f * ITEM_R * R * (float) Math.sin(STEP / 2f) * ChopScene.size * 0.86f;
            int n = tl.niche(0, i, t);
            if (n == -2 || (OssuaryTimeline.veiled(i) && n >= 0 && r >= 0 && t < r + 420)) {
                web(c, layer, tl, t, i, sx, sy, size, a, k);
                if (n == -2) continue;
            }
            if (n >= 0 && tl.value() != null && n < tl.value().chest().items().size()) {
                float in = ease((t - r - 200) / 300f);
                // It is lifted off the wheel by the showcase once that begins.
                if (tl.ending() != Ending.BUST && t >= tl.arrive() - 350) continue;
                c.loot(tl.value().chest().items().get(n), sx, sy, size * (0.6f + 0.4f * in), in);
            } else {
                c.loot(tl.fillerAt(0, i), sx, sy, size, (1f - 0.7f * blur) * (1f - 0.5f * dying(tl, t)));
            }
        }
        bulbs(c, tl, t, pos, th);
    }

    static void drawFace(Canvas c, float ox, float oy, float exx, float exy, float eyx, float eyy, float cs, float sn, boolean ink) {
        // The image's top-left corner and edges in the wheel's own frame, turned by the wheel, then mapped.
        float[] a = turn(-R, R, cs, sn), u = turn(2 * R, 0f, cs, sn), v = turn(0f, -2 * R, cs, sn);
        float px = ox + a[0] * exx + a[1] * eyx, py = oy + a[0] * exy + a[1] * eyy;
        float ux = u[0] * exx + u[1] * eyx, uy = u[0] * exy + u[1] * eyy;
        float vx = v[0] * exx + v[1] * eyx, vy = v[0] * exy + v[1] * eyy;
        if (ink) {
            c.imageAffine(WHEEL_IMAGE + "_ink", INK_FACE, TEX_N, TEX_N, px, py, ux, uy, vx, vy);
        } else {
            c.imageAffine(WHEEL_IMAGE, FACE, TEX_N, TEX_N, px, py, ux, uy, vx, vy);
        }
    }

    static final int[] INK_FACE = new int[TEX_N * TEX_N];

    static float[] turn(float x, float y, float cs, float sn) {
        return new float[]{x * cs - y * sn, x * sn + y * cs};
    }

    /** The segments' colours: the floor's own, dark, alternating with near-black and the chest tier's. */
    static int segColour(OssuaryTimeline tl, int k) {
        if (OssuaryTimeline.veiled(k)) return tl.red() ? 0x1A0606 : 0x0E0A14;
        int floor = tl.dealer().rgb, tier = tl.tier().rgb;
        if (tl.red()) {
            return switch (k % 4) {
                case 0 -> 0x7A1414;
                case 1 -> 0x2A0A0A;
                case 2 -> 0x5A1010;
                default -> ChopScene.mix(tier, 0x200808, 0.62f);
            };
        }
        return switch (k % 4) {
            case 0 -> ChopScene.mix(floor, 0x141018, 0.55f);
            case 1 -> 0x221C2C;
            case 2 -> ChopScene.mix(floor, 0x141018, 0.70f);
            default -> ChopScene.mix(tier, 0x141018, 0.62f);
        };
    }

    /**
     * Composes the face into {@link #FACE}: the gold rim with its bulb sockets, the groove, the segments with gold
     * dividers and a lighter lip, a ring of studs, and the hub. Lit per segment: the veiled ones glow in the tell's
     * colour through it, and after the reveal the stop takes the outcome's.
     */
    static void face(OssuaryTimeline tl, long t, float pos) {
        long r = tl.reveal();
        float die = dying(tl, t);
        int[] seg = new int[SEG];
        for (int k = 0; k < SEG; k++) seg[k] = segColour(tl, k);
        // The light on the stop: the tell's colour beating through the veils, then the truth.
        int stopSlot = tl.drum(0).target >= 0 ? (int) Math.floorMod(tl.drum(0).target, (long) SEG) : -1;
        float[] lit = new float[SEG];
        int litRgb = 0;
        if (tl.tellAt() >= 0 && t >= tl.tellAt() && stopSlot >= 0) {
            litRgb = tl.tellRgb(t);
            lit[stopSlot] = r >= 0 && t >= r ? (tl.band() == Band.BUST ? 0.25f : 0.75f) : 0.35f + 0.35f * (1f - tl.stepPulse(t));
        }
        float spin = speed(tl, t);
        float dirSign = dir(tl);
        for (int q = 0; q < FACE.length; q++) {
            float rho = TEX_RHO[q];
            if (rho > 1f) {
                FACE[q] = 0;
                continue;
            }
            float noise = TEX_NOISE[q];
            int col;
            if (rho > 0.975f) {
                col = 0x2A1A08;
            } else if (rho > 0.885f) {
                // The gold rim, lighter on its outer edge, with a dark socket for each bulb.
                float own = (float) (Math.PI / 2) - TEX_PHI[q];
                float b = own / (STEP / 2f) * dirSign;
                float near = Math.abs(b - Math.round(b));
                float dist = near * (STEP / 2f) * rho;
                if (dist < 0.03f && rho > 0.905f && rho < 0.955f) col = 0x3A2A12;
                else col = rho > 0.955f ? 0xF6CF62 : noise < 0.3f ? 0xB8842A : 0xDCA236;
            } else if (rho > 0.86f) {
                col = 0x1A1208;
            } else if (rho > 0.225f) {
                float own = ((float) (Math.PI / 2) - TEX_PHI[q]) * dirSign / STEP;
                int k = Math.floorMod(Math.round(own), SEG);
                float frac = own - Math.round(own);
                float edge = (0.5f - Math.abs(frac)) * STEP * rho;
                if (edge < 0.016f) {
                    col = edge < 0.007f ? 0xF0C04A : 0xA8741C;
                } else {
                    col = seg[k];
                    // A lighter lip round the outside, a darker band towards the hub.
                    if (rho > 0.80f) col = ChopScene.mix(col, 0xFFFFFF, 0.14f);
                    else if (rho < 0.34f) col = ChopScene.mix(col, 0x000000, 0.35f);
                    col = ChopScene.mix(col, noise < 0.5f ? 0x000000 : 0xFFFFFF, 0.06f * Math.abs(noise - 0.5f) * 2f);
                    if (lit[k] > 0f) col = ChopScene.mix(col, litRgb, lit[k] * (0.55f + 0.45f * rho));
                    // A ring of gold studs between the items and the hub.
                    if (rho > 0.355f && rho < 0.385f && Math.abs(frac) < 0.09f) col = 0xE8B23E;
                }
            } else if (rho > 0.19f) {
                col = noise < 0.4f ? 0xC08A2C : 0xE6AE40;
            } else {
                col = 0x231A14;
            }
            // Spinning hard, the face washes a little lighter, like a blur of light.
            if (spin > 0.05f && rho < 0.86f && rho > 0.225f) col = ChopScene.mix(col, 0xD8C8A8, 0.12f * spin);
            if (die > 0f) {
                int g = ((col >> 16 & 255) * 3 + (col >> 8 & 255) * 6 + (col & 255)) / 10;
                col = ChopScene.mix(col, g << 16 | g << 8 | g, 0.7f * die);
                col = ChopScene.mix(col, 0x000000, 0.25f * die);
            }
            col = tint(col);
            if (shade < 1f) col = ChopScene.mix(col, 0x000000, 1f - shade);
            FACE[q] = 0xFF000000 | (col & 0xFFFFFF);
        }
    }

    /** A veiled segment: a cobweb turning with the wheel; the stop's glows with the tell, then burns or crumbles. */
    static void web(Canvas c, CrystalVeins.Fill layer, OssuaryTimeline tl, long t, long i, float sx, float sy, float size,
                    float angle, float k) {
        long r = tl.reveal();
        boolean stop = i == tl.drum(0).target;
        float a = 1f;
        int col = 0xE8E8E8;
        if (stop && r >= 0 && t >= r) {
            float p = (t - r) / (tl.band() == Band.BUST ? 700f : 420f);
            a = 1f - ease(p);
            col = tl.band() == Band.BUST ? 0x7A7A7A : tint(tl.outcomeRgb());
            if (a <= 0.01f) return;
        } else if (stop && tl.tellAt() >= 0 && t >= tl.tellAt()) {
            col = ChopScene.mix(0xE8E8E8, tint(tl.tellRgb(t)), 0.35f + 0.3f * (1f - tl.stepPulse(t)));
        }
        int q = ChopScene.channel(k);
        int lit = ChopScene.alpha(a * 0.95f * ChopScene.sceneAlpha) | ((col >> 16 & 255) * q / 255) << 16
                | ((col >> 8 & 255) * q / 255) << 8 | (col & 255) * q / 255;
        float s = size * 1.15f;
        c.sprite(TEX_WEB, sx, sy, s, s, (float) Math.toDegrees(-(angle - Math.PI / 2)), lit);
        if (stop && tl.tellAt() >= 0 && t >= tl.tellAt() && (r < 0 || t < r)) {
            ChopScene.glow(c, sx, sy, size * 2.6f, tint(tl.tellRgb(t)), 0.35f + 0.35f * (1f - tl.stepPulse(t)));
        }
    }

    /** The bulbs round the rim: two to a segment, turning with the wheel. */
    static void bulbs(Canvas c, OssuaryTimeline tl, long t, float pos, float th) {
        int n = SEG * 2;
        long r = tl.reveal();
        float spin = speed(tl, t);
        float die = dying(tl, t);
        float bs = Math.max(2f, ChopScene.size * 0.62f);
        Random deaths = new Random(tl.seed() ^ 0xB01B);
        for (int b = 0; b < n; b++) {
            float deathAt = deaths.nextFloat();
            float a = segAngle(tl, b / 2f) + th;
            ChopScene.project((float) Math.cos(a) * BULB_R * R, WY + (float) Math.sin(a) * BULB_R * R, WZ + 0.3f);
            float x = ChopScene.P[0], y = ChopScene.P[1];
            boolean on;
            int col;
            float glow = 0.45f;
            if (r >= 0 && t >= r) {
                long since = t - r;
                if (tl.band() == Band.BUST) {
                    on = die < deathAt * 0.9f;
                    col = 0xB0A890;
                    glow = 0.25f;
                } else {
                    int p = power(tl);
                    if (p >= 3) {
                        // Gold and up: a rainbow racing round, all lit.
                        on = true;
                        col = p >= 4 ? hue((b / (float) n) + since / 600f) : ((b + since / 60) % 3 == 0 ? 0xFFFFFF : tint(tl.outcomeRgb()));
                        glow = 0.6f;
                    } else {
                        on = (b + since / (p == 2 ? 70 : 110)) % 2 == 0;
                        col = tint(tl.outcomeRgb());
                    }
                }
            } else if (tl.tellAt() >= 0 && t >= tl.tellAt()) {
                // The tell: every bulb in its colour, alternate halves flashing with each step.
                int step = tl.tellStep(t);
                on = step < 0 || (b + step) % 2 == 0 || tl.stepPulse(t) < 0.35f;
                col = tint(tl.tellRgb(t));
                glow = 0.55f;
            } else if (spin > 0.05f) {
                // Spinning: a rainbow chasing round the other way, faster the faster it goes.
                on = ((b + (int) (pos * 3)) % 3) != 0;
                col = hue(b / (float) n - pos * 0.05f);
            } else {
                // Idle: a marquee chase in warm white.
                on = (b + t / 120) % 3 == 0;
                col = 0xFFE6A0;
            }
            col = tint(col);
            int core = on ? col : ChopScene.mix(col, 0x1A1208, 0.78f);
            c.fill(x - bs / 2f, y - bs / 2f, bs, bs, ChopScene.alpha(ChopScene.sceneAlpha) | 0x0B0809);
            c.fill(x - bs / 2f + 1f, y - bs / 2f + 1f, bs - 2f, bs - 2f, ChopScene.alpha(ChopScene.sceneAlpha) | (shaded(core) & 0xFFFFFF));
            if (on) ChopScene.glow(c, x, y, bs * 5f, col, glow * shade);
        }
    }

    static int shaded(int rgb) {
        return shade < 1f ? ChopScene.mix(rgb, 0x000000, 1f - shade) : rgb;
    }

    /** A saturated colour round the hue circle, 0..1. */
    static int hue(float h) {
        h = h - (float) Math.floor(h);
        return java.awt.Color.HSBtoRGB(h, 0.75f, 1f) & 0xFFFFFF;
    }

    /** The gold flapper at the top: kicked aside by every peg that passes it, with the tell's gem on it. */
    static void pointer(Canvas c, OssuaryTimeline tl, long t) {
        float pos = wheelPos(tl, t);
        // A peg sits between every two segments; one passes under the flapper at every half niche.
        float q = pos + 0.5f;
        float f = q - (float) Math.floor(q);
        float kick = f > 0.86f ? (f - 0.86f) / 0.14f : f < 0.10f ? 1f - f / 0.10f : 0f;
        float ang = -dir(tl) * 0.5f * kick;
        float px = 0f, py = WY + R + 2.4f, pz = WZ + 1.4f, hy = 1.9f;
        float cxw = px + (float) Math.sin(ang) * hy, cyw = py - (float) Math.cos(ang) * hy;
        float k = shade * (1f - 0.35f * grey);
        ChopScene.inkWidth = Math.max(1.2f, ChopScene.size * 0.18f);
        ChopScene.box(cxw, cyw, pz, 0f, 0f, ang, 0.75f, hy, 0.55f, ChopScene.all(TEX_GOLD), FULL6, 1, 1, 1, 0, 1, c);
        ChopScene.box(px, py + 0.4f, pz - 0.6f, 0f, 0f, 0f, 1.5f, 1.0f, 1.0f, ChopScene.all(TEX_CHEEK), FULL6, 1, 1, 1, 0, 1, c);
        ChopScene.inkWidth = 0f;
        ChopScene.box(px, py + 0.4f, pz - 0.6f, 0f, 0f, 0f, 1.5f, 1.0f, 1.0f, ChopScene.all(TEX_CHEEK), FULL6,
                0.55f * k, 0.53f * k, 0.6f * k, 0f, 1, c);
        ChopScene.box(cxw, cyw, pz, 0f, 0f, ang, 0.75f, hy, 0.55f, ChopScene.all(TEX_GOLD), FULL6, k, 0.95f * k, 0.8f * k, 0f, 1, c);
        // The gem on its face: the tell's colour (dark before it starts).
        long r = tl.reveal();
        int gem = tl.tellAt() >= 0 && t >= tl.tellAt() ? tint(tl.tellRgb(t)) : 0x6A5A40;
        float gx = px + (float) Math.sin(ang) * 0.9f, gy = py - (float) Math.cos(ang) * 0.9f;
        ChopScene.box(gx, gy, pz + 0.65f, 0f, 0f, (float) Math.PI / 4 + ang, 0.5f, 0.5f, 0.2f, ChopScene.all(TEX_WHITE), FULL6,
                (gem >> 16 & 255) / 255f, (gem >> 8 & 255) / 255f, (gem & 255) / 255f, 0.3f, 1, c);
        if (tl.tellAt() >= 0 && t >= tl.tellAt()) {
            ChopScene.project(gx, gy, pz + 0.8f);
            float a = r >= 0 && t >= r ? (tl.band() == Band.BUST ? 0.15f : 0.7f) : 0.4f + 0.35f * (1f - tl.stepPulse(t));
            ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], 7f * ChopScene.size, gem, a);
        }
    }

    // ================================================================== the dealer, on the hub

    static final String[] DEALER_KEYS = {"BONZO", "SCARF", "PROFESSOR", "THORN", "LIVID", "SADAN", "NECRON"};

    static void dealer(Canvas c, OssuaryTimeline tl, long t) {
        String key = DEALER_KEYS[tl.dealer().ordinal()];
        String skin = c.headSkin(key);
        float half = 2.5f;
        float drop = (1f - ChopScene.backOut01((t - 200) / 420f)) * 9f;
        float bob = 0.2f * (float) Math.sin(pt / 380.0);
        float yaw = (float) Math.toRadians(-6f + 8f * (float) Math.sin(pt / 1300.0));
        float roll = 0f;
        long r = tl.reveal();
        if (r >= 0 && t >= r) {
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
        float hy = WY + bob + drop, hz = WZ + half + 0.4f;
        if (skin == null) {
            ChopScene.inkWidth = Math.max(1.2f, ChopScene.size * 0.2f);
            ChopScene.box(0f, hy, hz, 0f, yaw, roll, half, half, half, ChopScene.all(TEX_BONE), FULL6, 1, 1, 1, 0, 1, c);
            ChopScene.inkWidth = 0f;
            ChopScene.box(0f, hy, hz, 0f, yaw, roll, half, half, half, ChopScene.all(TEX_BONE), FULL6, 0.8f * shade, 0.78f * shade, 0.7f * shade, 0, 1, c);
            return;
        }
        float k = shade * (1f - 0.35f * grey);
        ChopScene.inkWidth = Math.max(1.5f, ChopScene.size / 3f);
        ChopScene.box(0f, hy, hz, 0f, yaw, roll, half * 1.0625f, half * 1.0625f, half * 1.0625f, ChopScene.all(skin), ChopScene.HEAD, 1, 1, 1, 0, 1, c);
        ChopScene.inkWidth = 0f;
        ChopScene.box(0f, hy, hz, 0f, yaw, roll, half, half, half, ChopScene.all(skin), ChopScene.HEAD, k, k, k, 0, 1, c);
        ChopScene.box(0f, hy, hz, 0f, yaw, roll, half * 1.0625f, half * 1.0625f, half * 1.0625f, ChopScene.all(skin), ChopScene.HAT, k, k, k, 0, 1, c);
    }

    // ================================================================== Master Mode: the sealed bar

    /** An iron bar across the wheel, held by wax seals: click each, or watch them crack; it drops once they are gone. */
    static void bar(Canvas c, CrystalVeins.Fill layer, OssuaryTimeline tl, long t) {
        long un = tl.unlockAt();
        float fall = un >= 0 && t >= un ? (t - un) / 1000f : 0f;
        float y = BAR_Y - 30f * fall * fall - 2f * fall, rot = 0.9f * fall;
        float k = shade * (1f - 0.35f * grey);
        if (fall < 0.9f) {
            ChopScene.inkWidth = Math.max(1.2f, ChopScene.size * 0.18f);
            ChopScene.box(0f, y, BAR_Z, 0f, 0f, rot, R + 1.2f, 0.75f, 0.5f, ChopScene.all(TEX_IRON), FULL6, 1, 1, 1, 0, 1, c);
            ChopScene.inkWidth = 0f;
            ChopScene.box(0f, y, BAR_Z, 0f, 0f, rot, R + 1.2f, 0.75f, 0.5f, ChopScene.all(TEX_IRON), FULL6,
                    0.6f * k, 0.6f * k, 0.66f * k, 0f, 1, c);
        }
        seals(c, layer, tl, t);
    }

    /** Master Mode's wax seals along the bar: click each, or watch them crack one by one. */
    static void seals(Canvas c, CrystalVeins.Fill layer, OssuaryTimeline tl, long t) {
        int n = tl.seals();
        float span = Math.min(2 * R - 4f, n * 3.2f);
        for (int i = 0; i < n; i++) {
            float sxw = n == 1 ? 0f : -span / 2f + span * i / (n - 1);
            ChopScene.project(sxw, BAR_Y, BAR_Z + 0.6f);
            float px = ChopScene.P[0], py = ChopScene.P[1];
            float rad = Math.max(3f, 0.95f * ChopScene.size);
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


    // ================================================================== the hand that spins it for you

    /** The skeleton hand: reaches in from the right, grips the rim and hauls it down. A glow on the rim while it waits. */
    static void hand(Canvas c, OssuaryTimeline tl, long t, int w) {
        long hand = tl.handAt(0);
        boolean waiting = tl.waitingForPlayer(t) && tl.drum(0).t0 < 0 && tl.unlockAt() >= 0 && t >= tl.unlockAt();
        if (waiting) {
            // Where to grab it: the right of the rim, pulsing, with arrows pointing down.
            ChopScene.project(R * 0.97f, WY, WZ + 0.5f);
            float x = ChopScene.P[0], y = ChopScene.P[1];
            float p = 0.5f + 0.5f * (float) Math.sin(t / 180.0);
            ChopScene.glow(c, x, y, 9f * ChopScene.size, 0xFFE6A0, 0.25f + 0.25f * p);
            float u = Math.max(2f, ChopScene.size * 0.4f);
            for (int k = 0; k < 3; k++) {
                float yy = y + (2.2f + k * 1.6f + 0.6f * ((t / 90) % 6) / 6f) * ChopScene.size;
                float a = (0.4f + 0.6f * p) * (1f - k * 0.25f) * ChopScene.sceneAlpha;
                for (int s = -2; s <= 2; s++) {
                    c.fill(x + 2.4f * ChopScene.size + s * u - u / 2f, yy - Math.abs(s) * u, u, u, ChopScene.alpha(a) | 0xFFE6A0);
                }
            }
        }
        if (hand < 0 || t < hand - 250 || t >= hand + OssuaryTimeline.HAND_STROKE + 300) return;
        float in = ease((t - hand + 250) / 250f) * (1f - ease((t - hand - OssuaryTimeline.HAND_STROKE) / 300f));
        float p = Math.max(0f, Math.min(1f, (t - hand) / (float) OssuaryTimeline.HAND_STROKE));
        // Down the right of the rim, from a little above the axle to well below it.
        double ang = 0.35 - 0.9 * Math.sin(p * Math.PI / 2);
        ChopScene.project((float) Math.cos(ang) * R * 1.02f, WY + (float) Math.sin(ang) * R * 1.02f, WZ + 0.8f);
        claw(c, ChopScene.P[0] + (1f - in) * 0.25f * w, ChopScene.P[1], Math.max(2f, ChopScene.size * 0.36f), in);
    }

    // ================================================================== M7: the dragons' crown

    /** Five dragon gems on gold stalks over the wheel, lighting in turn through the tell; a bust drops them grey. */
    static void crown(Canvas c, CrystalVeins.Fill layer, OssuaryTimeline tl, long t) {
        long r = tl.reveal();
        int lit = tl.dragonLit(t);
        boolean bust = r >= 0 && t >= r && tl.band() == Band.BUST;
        float k = shade * (1f - 0.35f * grey);
        for (int i = 0; i < 5; i++) {
            double a = Math.toRadians(150 - 30 * i);
            float rr = R + 5.6f;
            float x = (float) Math.cos(a) * rr, y = WY + (float) Math.sin(a) * rr, z = WZ - 0.6f;
            float sink = bust ? ease((t - r - 150L * i) / 900f) * 7f : 0f;
            // The stalk from the rim.
            ChopScene.project((float) Math.cos(a) * R, WY + (float) Math.sin(a) * R, z);
            float sx = ChopScene.P[0], sy = ChopScene.P[1];
            ChopScene.project(x, y - sink, z);
            float gx = ChopScene.P[0], gy = ChopScene.P[1];
            c.line(sx, sy, gx, gy, Math.max(2f, ChopScene.size * 0.5f), ChopScene.alpha(ChopScene.sceneAlpha) | 0x1A1208);
            c.line(sx, sy, gx, gy, Math.max(1f, ChopScene.size * 0.28f), ChopScene.alpha(ChopScene.sceneAlpha) | shaded(0xC8922E));
            int col = OssuaryTimeline.DRAGONS[i];
            boolean on = lit == i;
            float gk = on ? 1f : 0.3f;
            int tcol = on ? tint(col) : ChopScene.mix(col, 0x2A2A30, 0.75f);
            float half = on ? 1.45f : 1.15f;
            ChopScene.inkWidth = Math.max(1f, ChopScene.size * 0.18f);
            ChopScene.box(x, y - sink, z, 0.6f, (float) (pt / 900.0 + i), 0f, half, half, half, ChopScene.all(TEX_WHITE), FULL6, 1, 1, 1, 0, 1, c);
            ChopScene.inkWidth = 0f;
            ChopScene.box(x, y - sink, z, 0.6f, (float) (pt / 900.0 + i), 0f, half, half, half, ChopScene.all(TEX_WHITE), FULL6,
                    (tcol >> 16 & 255) / 255f * gk * k + 0.08f, (tcol >> 8 & 255) / 255f * gk * k + 0.08f, (tcol & 255) / 255f * gk * k + 0.08f, on ? 0.25f : 0f, 1, c);
            if (on) {
                float ga = r >= 0 && t >= r ? 0.8f : 0.45f + 0.35f * (1f - tl.stepPulse(t));
                ChopScene.glow(c, gx, gy, 16f * ChopScene.size, tint(col), ga);
            }
            if (bust && t >= r) {
                Random rng = new Random(tl.seed() + i);
                for (int q = 0; q < 6; q++) {
                    float f = (t - r - 150L * i - rng.nextInt(300)) / 900f;
                    if (f < 0 || f > 1) continue;
                    ChopScene.project(x + (rng.nextFloat() - 0.5f) * 3f, y - f * 9f, z + 1.7f);
                    sq(layer, ChopScene.P[0], ChopScene.P[1], 0x50505A, 1f - f);
                }
            }
        }
    }

    // ================================================================== F5: the shell game

    /** Nine Livids line up in front of the wheel's lower half; one is picked (any pick is the real one). */
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
            float x = -12f + i * 3f, y = WY - 6.5f + 0.25f * (float) Math.sin(pt / 300.0 + i * 0.8), z = WZ + 8f;
            float a = appear * end;
            float half = 1.25f * appear;
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

    /** The stop slot on screen: the segment under the pointer once the wheel has stopped. */
    static float[] stopSlot() {
        ChopScene.project(0f, WY + ITEM_R * R, WZ + 0.4f);
        return new float[]{ChopScene.P[0], ChopScene.P[1]};
    }

    static void effects(Canvas c, CrystalVeins.Fill layer, OssuaryTimeline tl, long t, int w, int h) {
        var dr = tl.drum(0);
        ChopScene.project(0f, WY + R + 0.6f, WZ + 1.4f);
        float px = ChopScene.P[0], py = ChopScene.P[1];
        long tt = tl.frozen(t) ? pt : t;
        float v = tl.vel(0, tt);
        // Sparks off the flapper while the pegs whip past it, more the faster it goes.
        if (v > 3f) {
            Random rng = new Random(tt / 40 + tl.seed());
            int n = Math.min(9, (int) (v / 3f));
            for (int k = 0; k < n; k++) {
                float vx = (rng.nextFloat() - 0.5f) * 2f, f = rng.nextFloat();
                sq(layer, px + vx * (4f + 10f * f) * ChopScene.size * 0.4f, py - f * 4f * ChopScene.size + f * f * 6f * ChopScene.size,
                        k % 3 == 0 ? 0xFFFFFF : 0xFFD27A, 1f - f * 0.6f);
            }
        }
        // The rim's light trail while it spins hard: streaks of the bulbs' rainbow along the rim.
        float sp = speed(tl, t);
        if (sp > 0.1f) {
            float th = theta(tl, t);
            int n = 24;
            for (int k = 0; k < n; k++) {
                float a = segAngle(tl, k * SEG / (float) n) + th;
                for (int s = 1; s <= 3; s++) {
                    float b = a + dir(tl) * s * 0.035f * sp;
                    ChopScene.project((float) Math.cos(b) * BULB_R * R, WY + (float) Math.sin(b) * BULB_R * R, WZ + 0.3f);
                    sq(layer, ChopScene.P[0], ChopScene.P[1], tint(hue(k / (float) n - wheelPos(tl, t) * 0.05f)), sp * (0.5f - s * 0.13f));
                }
            }
        }
        // Dust and a ring of sparks as it stops.
        if (dr.stopAt >= 0 && t >= dr.stopAt && t < dr.stopAt + 900) {
            Random rng = new Random(tl.seed() + 31L);
            float f = (t - dr.stopAt) / 900f;
            for (int k = 0; k < 22; k++) {
                double a = rng.nextDouble() * Math.PI;
                float sp2 = 3f + rng.nextFloat() * 6f;
                sq(layer, px + (float) Math.cos(a) * sp2 * f * ChopScene.size, py - (float) Math.sin(a) * sp2 * f * ChopScene.size + 4f * f * f * ChopScene.size,
                        0xFFE6A0, 1f - f);
            }
        }
        long r = tl.reveal();
        if (r < 0 || t < r) return;
        float[] s = stopSlot();
        float nx = s[0], ny = s[1];
        int col = tint(tl.outcomeRgb());
        long dt = t - r;
        Random rng = new Random(tl.seed() ^ 0x5EED);
        float niche = 2f * ITEM_R * R * (float) Math.sin(STEP / 2f) * ChopScene.size;
        if (tl.band() == Band.BUST) {
            // The web crumbles to grey dust.
            for (int k = 0; k < 40; k++) {
                float f = (dt - rng.nextInt(250)) / 900f;
                float vx = (rng.nextFloat() - 0.5f) * 2.4f, vy = rng.nextFloat() * 0.6f;
                if (f < 0 || f > 1) continue;
                sq(layer, nx + vx * niche * 0.5f * (0.4f + f), ny + (vy * f + 3.5f * f * f) * niche, 0x8E8E8E, 1f - f);
            }
            return;
        }
        int p = power(tl);
        // The web burns off in the outcome's colour: embers rising off the slot, more the bigger it is.
        int embers = 30 + 16 * p;
        for (int k = 0; k < embers; k++) {
            float f = (dt - rng.nextInt(400)) / 1000f;
            if (f < 0 || f > 1) continue;
            float vx = (rng.nextFloat() - 0.5f) * 2f;
            sq(layer, nx + vx * niche * (0.5f + 0.3f * f), ny - (f * 9f + rng.nextFloat()) * ChopScene.size, k % 3 == 0 ? 0xFFF4D0 : col, 1f - f);
        }
        // Shockwaves out of the slot: one per band, each a ring of squares.
        for (int q = 0; q < p; q++) {
            float f = (dt - 110L * q) / 700f;
            if (f < 0 || f > 1) continue;
            float rad = (2f + 28f * ease(f)) * ChopScene.size;
            int n = 28 + 6 * p;
            for (int k = 0; k < n; k++) {
                double a = k * Math.PI * 2 / n + q * 0.3;
                sq(layer, nx + (float) Math.cos(a) * rad, ny + (float) Math.sin(a) * rad * 0.8f, q % 2 == 0 ? col : 0xFFFFFF, (1f - f) * 0.9f);
            }
        }
        // Gold and up: a fountain of coins and sparks out of the slot.
        if (p >= 3 && !tl.frozen(t)) {
            long et = tl.phys(t) - r;
            int n = ChopScene.calm ? 24 : 60;
            Random cr = new Random(tl.seed() * 3 + 1);
            for (int k = 0; k < n; k++) {
                float delay = cr.nextFloat() * 1400f, life = 900f + cr.nextFloat() * 700f;
                float f = (et - delay) / life;
                if (f < 0f || f > 1f) continue;
                float vx = (cr.nextFloat() - 0.5f) * 30f, vy = -22f - cr.nextFloat() * 18f;
                float x = nx + vx * f * ChopScene.size, y = ny + (vy * f + 40f * f * f) * ChopScene.size;
                int cc = k % 4 == 0 ? 0xFFFFFF : k % 2 == 0 ? 0xFFD23F : col;
                sq(layer, x, y, cc, 1f - f * f);
                if (k % 3 == 0) sq(layer, x + unit, y, ChopScene.mix(cc, 0x000000, 0.35f), 1f - f * f);
            }
        }
        if (tl.ending() == Ending.WIN) return;
        // A jackpot (and a dye, once it thaws): the floor's own confetti.
        long from = tl.ending() == Ending.DYE ? r + OssuaryTimeline.THAW_AFTER : r + 300;
        if (t < from) {
            if (tl.ending() == Ending.DYE) frozenDye(c, layer, tl, t, w, h);
            return;
        }
        themed(layer, tl, t - from, w, h);
    }

    // ================================================================== each drop's own touch

    enum Fx { GLYPHS, RECOMB, IMPLOSION, WARP, SHIELD, SPLASH, SLASH, ARMOUR, STARS, MOTES, SPARKLE }

    /** What kind of thing the drop is, for its own small flourish round the showcase. */
    static Fx fxOf(Loot l) {
        String k = l == null || l.key() == null ? "" : l.key();
        String item = l == null || l.item() == null ? "" : l.item();
        if (k.startsWith("ENCHANTMENT_") || item.endsWith("enchanted_book")) return Fx.GLYPHS;
        if (k.contains("RECOMBOBULATOR")) return Fx.RECOMB;
        if (k.equals("IMPLOSION_SCROLL")) return Fx.IMPLOSION;
        if (k.equals("SHADOW_WARP_SCROLL")) return Fx.WARP;
        if (k.equals("WITHER_SHIELD_SCROLL")) return Fx.SHIELD;
        if (k.endsWith("_FISH")) return Fx.SPLASH;
        if (k.matches(".*(SWORD|CLAYMORE|STAFF|BOW|FURY|HANDLE|BREATH|DAGGER|SCYTHE).*")) return Fx.SLASH;
        if (k.matches(".*(CHESTPLATE|LEGGINGS|BOOTS|HELMET).*")) return Fx.ARMOUR;
        if (k.startsWith("ESSENCE_")) return Fx.MOTES;
        if (k.matches(".*(PET_|RING|EYE|SKULL|STAR|SCARF).*")) return Fx.STARS;
        return Fx.SPARKLE;
    }

    /** The drop's own flourish, round where it is shown: pixel squares straight onto the canvas (after the layer). */
    static void itemFx(Canvas c, OssuaryTimeline tl, Loot l, float cx, float cy, float size, long dt) {
        if (dt < 0) return;
        Fx fx = fxOf(l);
        int col = l != null && l.colour() >= 0 ? l.colour() & 0xFFFFFF : tl.outcomeRgb();
        int p = Math.max(1, power(tl));
        float u = unit;
        Random rng = new Random(tl.seed() * 7 + fx.ordinal());
        float a0 = Math.min(1f, dt / 300f) * ChopScene.sceneAlpha;
        switch (fx) {
            case GLYPHS -> {
                // The enchanting table's glyphs drawn into the book, round and in.
                for (int k = 0; k < 10 + 4 * p; k++) {
                    float f = ((dt + rng.nextInt(1400)) % 1400) / 1400f;
                    double a = rng.nextDouble() * Math.PI * 2 + f * 2.5;
                    float rad = size * (1.4f - 1.1f * f);
                    glyph(c, cx + (float) Math.cos(a) * rad, cy + (float) Math.sin(a) * rad * 0.7f, u, k % 2 == 0 ? 0xB98CFF : 0x7FD8FF, a0 * (1f - f * 0.6f), k);
                }
            }
            case RECOMB -> {
                // A ring of gold ticks turning round it, a rarity being added.
                int n = 16;
                float rad = size * 0.9f;
                for (int k = 0; k < n; k++) {
                    double a = k * Math.PI * 2 / n + dt / 900.0;
                    boolean on = (k + dt / 90) % 4 != 0;
                    px(c, cx + (float) Math.cos(a) * rad, cy + (float) Math.sin(a) * rad, u * 1.5f, on ? 0xFFD23F : 0x8A6A1C, a0);
                }
                if ((dt / 600) % 2 == 0) ChopScene.glow(c, cx, cy, size * 2f, 0xFFD23F, 0.2f * a0);
            }
            case IMPLOSION -> {
                // Rings of fire squares falling inwards, again and again.
                for (int q = 0; q < 3; q++) {
                    float f = ((dt + q * 300) % 900) / 900f;
                    float rad = size * (1.6f - 1.4f * f);
                    for (int k = 0; k < 18; k++) {
                        double a = k * Math.PI * 2 / 18;
                        px(c, cx + (float) Math.cos(a) * rad, cy + (float) Math.sin(a) * rad, u, k % 2 == 0 ? 0xFF8A1F : 0xFFE060, a0 * f);
                    }
                }
            }
            case WARP -> {
                // Teleport flecks blinking in and out round it.
                for (int k = 0; k < 24; k++) {
                    long ph = (dt / 120 + k * 7) % 9;
                    if (ph > 3) continue;
                    float x = cx + (rng.nextFloat() - 0.5f) * size * 2.6f, y = cy + (rng.nextFloat() - 0.5f) * size * 1.8f;
                    px(c, x, y, u, k % 3 == 0 ? 0xFFFFFF : 0x9A3BFF, a0 * (1f - ph / 4f));
                }
            }
            case SHIELD, ARMOUR -> {
                // A hexagon of light pulsing out from it.
                float f = (dt % 1100) / 1100f;
                float rad = size * (0.7f + 0.7f * f);
                int shieldCol = fx == Fx.SHIELD ? 0xFFD23F : ChopScene.mix(col, 0xFFFFFF, 0.3f);
                for (int e = 0; e < 6; e++) {
                    double a0r = e * Math.PI / 3, a1r = (e + 1) * Math.PI / 3;
                    for (int s = 0; s < 6; s++) {
                        float m = s / 6f;
                        float x = cx + (float) ((1 - m) * Math.cos(a0r) + m * Math.cos(a1r)) * rad;
                        float y = cy + (float) ((1 - m) * Math.sin(a0r) + m * Math.sin(a1r)) * rad;
                        px(c, x, y, u, shieldCol, a0 * (1f - f));
                    }
                }
            }
            case SPLASH -> {
                // Water: drops thrown up and falling round it, a ripple under it.
                for (int k = 0; k < 18; k++) {
                    float f = ((dt + rng.nextInt(1000)) % 1000) / 1000f;
                    float vx = (rng.nextFloat() - 0.5f) * size * 2.4f, vy = -size * (1f + rng.nextFloat());
                    px(c, cx + vx * f, cy + size * 0.4f + vy * f + size * 2.2f * f * f, u, k % 3 == 0 ? 0xE0F6FF : 0x3FA8FF, a0 * (1f - f));
                }
            }
            case SLASH -> {
                // Two blade strokes crossing it, every so often.
                float f = (dt % 1300) / 260f;
                if (f < 1f) {
                    for (int s = 0; s < 2; s++) {
                        for (int k = 0; k < 14; k++) {
                            float m = k / 13f;
                            if (m > f) break;
                            float x = cx + (m - 0.5f) * size * 2.2f * (s == 0 ? 1 : -1), y = cy + (m - 0.5f) * size * 1.6f;
                            px(c, x, y, u * 1.4f, k == (int) (f * 13) ? 0xFFFFFF : ChopScene.mix(col, 0xFFFFFF, 0.5f), a0 * (0.4f + 0.6f * m));
                        }
                    }
                }
            }
            case STARS -> {
                // Twinkling four-point stars round it.
                for (int k = 0; k < 8 + 2 * p; k++) {
                    float ph = ((dt + rng.nextInt(900)) % 900) / 900f;
                    float tw = (float) Math.sin(ph * Math.PI);
                    float x = cx + (rng.nextFloat() - 0.5f) * size * 2.6f, y = cy + (rng.nextFloat() - 0.5f) * size * 2f;
                    int cc = k % 2 == 0 ? 0xFFFFFF : ChopScene.mix(col, 0xFFFFFF, 0.4f);
                    px(c, x, y, u, cc, a0 * tw);
                    if (tw > 0.5f) {
                        px(c, x - u, y, u, cc, a0 * tw * 0.6f);
                        px(c, x + u, y, u, cc, a0 * tw * 0.6f);
                        px(c, x, y - u, u, cc, a0 * tw * 0.6f);
                        px(c, x, y + u, u, cc, a0 * tw * 0.6f);
                    }
                }
            }
            case MOTES -> {
                for (int k = 0; k < 14; k++) {
                    float f = ((dt + rng.nextInt(1600)) % 1600) / 1600f;
                    float x = cx + (rng.nextFloat() - 0.5f) * size * 1.8f + 4f * (float) Math.sin(f * 6 + k);
                    px(c, x, cy + size * 0.6f - f * size * 2f, u, k % 2 == 0 ? 0x3A3A3A : 0x9A9A9A, a0 * (1f - f));
                }
            }
            case SPARKLE -> {
                for (int k = 0; k < 10; k++) {
                    float f = ((dt + rng.nextInt(1200)) % 1200) / 1200f;
                    double a = rng.nextDouble() * Math.PI * 2;
                    float rad = size * (0.5f + 0.9f * f);
                    px(c, cx + (float) Math.cos(a) * rad, cy + (float) Math.sin(a) * rad, u, k % 2 == 0 ? 0xFFFFFF : col, a0 * (1f - f));
                }
            }
        }
    }

    /** A 3x3 enchanting glyph, one of four shapes. */
    static void glyph(Canvas c, float x, float y, float u, int rgb, float a, int which) {
        int[] masks = {0b101010101, 0b111001011, 0b010111010, 0b110011001};
        int m = masks[Math.floorMod(which, 4)];
        for (int b = 0; b < 9; b++) {
            if ((m >> b & 1) == 0) continue;
            px(c, x + (b % 3 - 1) * u, y + (b / 3 - 1) * u, u, rgb, a);
        }
    }

    /** A square straight onto the canvas, on the pixel grid. */
    static void px(Canvas c, float x, float y, float s, int rgb, float a) {
        if (a <= 0.02f) return;
        float u = unit;
        c.fill(Math.round(x / u) * u, Math.round(y / u) * u, s, s, ChopScene.alpha(Math.min(1f, a)) | (rgb & 0xFFFFFF));
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
        else s = "Drag the wheel down to spin it";
        float p = 0.65f + 0.35f * (float) Math.sin(t / 220.0);
        c.text(s, w / 2f, h * 0.93f, 1f, ChopScene.alpha(p * ChopScene.sceneAlpha) | 0xFFE6A0);
    }

    /** The drop, forward: out of the niche to the middle of the screen, its name, its worth. */
    static void showcase(Canvas c, CrystalVeins.Fill layer, OssuaryTimeline tl, long t, int w, int h) {
        ChestValue v = tl.value();
        if (v == null || v.bestItem() == null) return;
        long arrive = tl.arrive();
        float[] slot = stopSlot();
        float nx = slot[0], ny = slot[1];
        float niche = 2f * ITEM_R * R * (float) Math.sin(STEP / 2f) * ChopScene.size * 0.86f;
        float f = ease((t - arrive + 350) / 650f);
        float strip = ease((t - tl.stripAt()) / 500f);
        float cx = nx + (w / 2f - nx) * f, cy = ny + (h * (0.38f - 0.06f * strip) - ny) * f;
        // Bigger drops come further forward.
        float big = 0.86f + 0.07f * power(tl);
        float size = niche + (Math.min(h * 0.22f, w * 0.2f) * big - niche) * f;
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
        // The landing: a punch of scale as it arrives, harder the bigger it is.
        float punch = 1f + 0.08f * power(tl) * (float) Math.max(0.0, Math.sin(Math.PI * Math.min(1f, Math.max(0f, (t - arrive) / 260f))));
        float iy = cy + (float) Math.sin(t / 420.0) * 2f * f;
        c.loot(v.bestItem(), cx, iy, size * punch, a);
        if (t >= arrive) itemFx(c, tl, v.bestItem(), cx, iy, size, t - arrive);
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
