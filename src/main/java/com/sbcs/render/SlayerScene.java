package com.sbcs.render;

import com.sbcs.anim.SlayerTimeline;
import com.sbcs.anim.SlayerTimeline.Drop;
import com.sbcs.anim.SlayerTimeline.Source;

import java.util.ArrayList;
import java.util.List;

/**
 * "Exhumed" (the Revenant Horror) and "The Brood" (the Tarantula Broodfather): one routine for the game
 * ({@code gui/SlayerScreen}) and the offline preview ({@code tools/SlayerPreview}), over ChopScene's Canvas.
 *
 * <h2>The stage</h2>
 * Over the dimmed game, a patch of the boss's own ground (coarse dirt and podzol for the graveyard, cobblestone and
 * webs for the den) drawn as GUI quads, fading out at its edges. On it the boss - the game's own zombie in diamond
 * armour, the game's own spider - is rasterised whole with a depth buffer ({@link ModelRaster}), as Eruption's
 * creatures are; so is everything else with a model (the Warden, the mosquito, the maggots, bricks, a scythe).
 * A second raster ({@link #INSIDE}) is drawn over the first for what is seen through something (the mosquito in the
 * spider's belly). Blood, slime, webs, sculk, sparks and flies are squares on two PixelLayers, under and over the
 * models. The drop is the screen's own item ({@code Canvas.item}: its NEU head in 3D, or its icon) from the moment it
 * appears, so it never jumps when it rises into the showcase.
 *
 * <h2>The run</h2>
 * The kill first, the same every time: the nametag counts down over three blows (a slash, a crit number, the red
 * hurt tint) and the boss dies the vanilla way, tipping over (a spider rolls onto its back). Then the drop's own ending
 * ({@link SlayerEndings}), then the showcase, centred: the banner in the drop line's tier colour, the name stamped,
 * "1 in ~N · your Nth", the price counting up.
 */
public final class SlayerScene {
    private SlayerScene() {}

    // ================================================================== the models

    /** The game's own models (SlayerRigs in game and preview; null in the pure tests, which draw none). */
    public interface Rigs {
        /** The Revenant: walk phase, yaw, how far its arms are raised past the zombie's reach, death tip 0..1, parts to leave out. */
        List<float[]> zombie(float walk, float yaw, float armsUp, float tip, int lose);

        /** One of its parts alone (LOSE_HEAD, LOSE_RIGHT_ARM...), about its own middle, turned. */
        List<float[]> zombiePart(int part, float yaw, float pitch, float roll);

        /** The Tarantula (or a cave spider, {@code cave} 1), its eyes over it. */
        List<float[]> spider(float walk, float yaw, float tip, float cave);

        /** The Warden, at {@code emergeMs} into its emerge or else {@code roarMs} into its roar (-1: neither). */
        List<float[]> warden(float yaw, float emergeMs, float roarMs, float tip, boolean heart);

        List<float[]> bee(float age, float yaw, float pitch);

        List<float[]> silverfish(float age, float yaw);

        /** A player-head skull in texture {@code slot}. */
        List<float[]> skull(float yaw, float pitch, int slot);
    }

    public static Rigs rigs;

    /** The 3D layer, and the one drawn over it. Their slots are set by the screen or the preview: see SLOT_*. */
    public static final ModelRaster RASTER = new ModelRaster(), INSIDE = new ModelRaster();
    /**
     * Half the usual pixel cap (0.4.0): the bosses stand large and their faces overlap, so at 600k a frame the dyes'
     * endings spent 20-28 ms a frame rasterising at a 960 x 540 GUI (FrameBudgetTest's timings). The image drops below
     * 2 px a GUI pixel only while a model fills most of the screen, and never below 1 (ModelRaster.begin).
     */
    static {
        RASTER.budget = INSIDE.budget = 300_000;
    }
    public static final int SLOT_BOSS = 0, SLOT_ARMOUR = 1, SLOT_WARDEN = 2, SLOT_HEART = 3, SLOT_BEE = 4,
            SLOT_SKULL = 5, SLOT_FLOOR = 6, SLOT_EYES = 7, SLOT_SILVERFISH = 8, SLOT_COBWEB = 9, SLOT_BRICKS = 10,
            SLOT_SCULK = 11, SLOT_CAVE_SPIDER = 12, SLOT_WHITE = 13;
    /** Parts of the zombie to leave out (and {@link Rigs#zombiePart}'s part). */
    public static final int LOSE_HEAD = 1, LOSE_RIGHT_ARM = 2, LOSE_LEFT_ARM = 4, LOSE_BODY = 8, LOSE_LEGS = 16, LOSE_HAT = 32;

    /** The ground's textures, GUI quads (adopted by the screen, read from files by the preview). */
    public static final String TEX_FLOOR_A = "slayer_floor_a", TEX_FLOOR_B = "slayer_floor_b";
    public static final String TEX_WHITE = SeraphScene.TEX_WHITE;
    public static final String TEX_RASTER = "slayer_raster", TEX_INSIDE = "slayer_inside";

    /** The drop line's own tier word for the banner (null: the drop's table tier), "your 3rd", the banner switch. */
    public static String bannerWord, nthNote;
    public static boolean showBanner = true;
    /** Run between the two layers and the models, in game; offline the layers are the canvas. */
    public static Runnable flushBack, flushFront;
    /** Image pixels a GUI pixel for the 3D layer. */
    public static float rasterScale = 2f;

    // ================================================================== geometry (world units, 16 a block)

    /** How big each boss is drawn (blocks to world units times this). */
    static float bossScale(Source s) {
        return s == Source.REVENANT ? 1.5f : 2.1f;
    }

    /** The boss stands at the origin, turned three-quarters to the camera. */
    static final float BOSS_YAW = -38f;

    public static int unitFor(int h) {
        return Math.max(2, Math.round(h / 120f));
    }

    // ================================================================== the frame

    /** This frame's boss points in world units, worked out from its own polygons: the head and the chest (or abdomen). */
    static final float[] HEAD = new float[3], CHEST = new float[3];
    static SlayerTimeline TL;
    static long T;
    static int W, H;
    /** The grid of the pixel layers, GUI pixels. */
    static float G;

    public static void draw(ChopScene.Canvas c, CrystalVeins.Fill back, CrystalVeins.Fill front, SlayerTimeline tl, long t, int w, int h) {
        TL = tl;
        T = t;
        W = w;
        H = h;
        G = unitFor(h);
        float master = tl.fadeOut(t);
        c.fill(0, 0, w, h, ChopScene.alpha(tl.dim(t) + SlayerEndings.extraDim(tl, t)) | 0x07060A);
        if (master <= 0.01f) return;
        ChopScene.sceneAlpha = master;
        ChopScene.lightCount = 0;
        ChopScene.gild = null;
        ChopScene.glintHook = null;
        ChopScene.insideOut = false;
        ChopScene.yShift = 0f;
        camera(tl, t, w, h);
        POLYS.clear();
        IN.clear();
        grey = 0f;
        paintR = 0f;

        floor(c, tl, t);
        boss(tl, t);
        SlayerEndings.models(tl, t);
        SlayerEndings.back(c, back, tl, t);
        if (flushBack != null) flushBack.run();
        rasterise(c, RASTER, POLYS, TEX_RASTER, true);
        rasterise(c, INSIDE, IN, TEX_INSIDE, false);
        SlayerEndings.front(c, front, tl, t);
        intro(c, front, tl, t);
        if (flushFront != null) flushFront.run();
        item(c, tl, t);
        ChopScene.sceneAlpha = 1f;
    }

    // ================================================================== the camera

    static void camera(SlayerTimeline tl, long t, int w, int h) {
        float sway = 2f * (float) Math.sin(t / 2300.0);
        double yaw = Math.toRadians(-18f + sway);
        double pitch = Math.toRadians(-17f);
        ChopScene.cy = (float) Math.cos(yaw);
        ChopScene.sy = (float) Math.sin(yaw);
        ChopScene.cp = (float) Math.cos(pitch);
        ChopScene.sp = (float) Math.sin(pitch);
        // Closer once it is down: the endings happen on and round a corpse lying on the ground.
        float down = SlayerTimeline.ease((t - SlayerTimeline.KILLED) / 900f) * (1f - tl.rise(t));
        ChopScene.size = h * 1.45f / 128f * (1f + 0.18f * down);
        ChopScene.fx = 0f;
        ChopScene.fy = 18f - 8f * down;
        ChopScene.fz = 0f;
        ChopScene.sx0 = w / 2f;
        ChopScene.sy0 = h * 0.6f;
        SlayerEndings.camera(tl, t, w, h);
        float shake = SlayerEndings.shake(tl, t) + 2.5f * SlayerTimeline.hurt(t) * (t < SlayerTimeline.ENDING ? 1f : 0f);
        shake *= Punch.shake();
        if (ChopScene.calm) shake = 0f;
        if (shake > 0f) {
            ChopScene.sx0 += shake * (float) Math.sin(t * 0.13);
            ChopScene.sy0 += shake * (float) Math.cos(t * 0.17);
        }
    }

    // ================================================================== the ground

    /** A patch of the boss's own ground, 9 x 7 blocks, each block a GUI quad, fading out with distance. */
    static void floor(ChopScene.Canvas c, SlayerTimeline tl, long t) {
        float a = ChopScene.sceneAlpha * SlayerEndings.floorAlpha(tl, t);
        if (a <= 0.02f) return;
        boolean rev = tl.drop().source == Source.REVENANT;
        int tint = SlayerEndings.floorTint(tl, t);
        for (int j = -3; j <= 3; j++) {
            for (int i = -4; i <= 4; i++) {
                float d = (float) Math.sqrt(i * i * 0.6f + j * j * 1.1f);
                float fa = a * Math.max(0f, 1f - d / 4.2f);
                if (fa <= 0.02f) continue;
                int hsh = (i * 73856093) ^ (j * 19349663);
                String tex = ((hsh >>> 4) & 3) == 0 ? TEX_FLOOR_B : TEX_FLOOR_A;
                float x0 = i * 16f - 8f, z0 = j * 16f - 8f;
                ChopScene.project(x0, 0f, z0);
                float ox = ChopScene.P[0], oy = ChopScene.P[1];
                ChopScene.project(x0 + 16f, 0f, z0);
                float ux = ChopScene.P[0] - ox, uy = ChopScene.P[1] - oy;
                ChopScene.project(x0, 0f, z0 + 16f);
                float vx = ChopScene.P[0] - ox, vy = ChopScene.P[1] - oy;
                // Darker towards the back; a block in three a shade off, as worn ground is.
                float lit = (rev ? 0.62f : 0.55f) * (0.85f + 0.15f * (j + 3) / 6f) * ((hsh >>> 9 & 3) == 0 ? 0.88f : 1f);
                c.quad(tex, ox, oy, ux, uy, vx, vy, 0, 0, 16, 16, ChopScene.alpha(fa) | shade(tint, lit));
            }
        }
    }

    // ================================================================== the boss

    /** The boss through the kill, then lying dead until its ending is done with it. */
    static void boss(SlayerTimeline tl, long t) {
        if (rigs == null) return;
        Source src = tl.drop().source;
        float s = bossScale(src);
        float corpse = SlayerEndings.corpse(tl, t);
        float tip = SlayerTimeline.deathTip(t) * SlayerEndings.tipScale(tl, t);
        float hurt = SlayerTimeline.hurt(t);
        // Vanilla's hurt overlay: red, over the whole model.
        int tint = hurt > 0f ? mixRgb(0xFFFFFF, 0xFF8080, hurt) : 0xFFFFFF;
        int add = hurt > 0f ? ChopScene.alpha(0.22f * hurt) | 0xFF2010 : 0;
        int extraTint = SlayerEndings.bossTint(tl, t);
        tint = mul(tint, extraTint);
        float k = t / 1000f;
        float yaw = BOSS_YAW + SlayerEndings.bossYaw(tl, t);
        float[] off = SlayerEndings.bossOffset(tl, t);
        int from = POLYS.size();
        if (src == Source.REVENANT) {
            // Walking at the player until the blow that kills it; recoiling from each hit.
            float walk = t < SlayerTimeline.KILLED ? k * 6f : SlayerTimeline.KILLED / 1000f * 6f;
            float recoil = 0.3f * hurt;
            int lose = SlayerEndings.lose(tl, t);
            if ((lose & LOSE_HEAD) != 0) {
                // Where the head would be, for what comes out of the neck.
                int at = POLYS.size();
                add(rigs.zombie(walk, yaw, recoil, tip, 0), s, off[0], off[1], off[2], -1, tint, add, POLYS);
                centroid(at, 1, HEAD);
                while (POLYS.size() > at) POLYS.remove(POLYS.size() - 1);
            }
            add(rigs.zombie(walk, yaw, recoil, tip, lose), s, off[0], off[1], off[2], -1, tint, add, POLYS);
        } else {
            float walk = t < SlayerTimeline.KILLED ? k * 9f : SlayerTimeline.KILLED / 1000f * 9f;
            // A dead spider's legs curl in: the walk frozen mid-step, twitching once or twice.
            if (t > SlayerTimeline.KILLED + 600) walk += 0.25f * (float) Math.sin(k * 23f) * Math.max(0f, 1f - (t - SlayerTimeline.KILLED - 600) / 900f);
            add(rigs.spider(walk, yaw, tip, 0f), s, off[0], off[1], off[2], -1, tint, add, POLYS);
        }
        if (src != Source.REVENANT || (SlayerEndings.lose(tl, t) & LOSE_HEAD) == 0) centroid(from, 1, HEAD);
        centroid(from, src == Source.REVENANT ? 0 : 2, CHEST);
        // Worked out every frame, so the ending can use where it lies even once it is gone.
        if (corpse <= 0f) {
            while (POLYS.size() > from) POLYS.remove(POLYS.size() - 1);
            return;
        }
        // A corpse fading away at its end sinks into the ground instead (a raster cannot fade one model).
        if (corpse < 1f) {
            float drop = (1f - corpse) * 30f * s;
            for (int i = from; i < POLYS.size(); i++) {
                Poly p = POLYS.get(i);
                p.p[1] -= drop;
                p.p[4] -= drop;
                p.p[7] -= drop;
                p.cut = 0f;
            }
        }
    }

    /** The centre of the polygons since {@code from} with flag bit {@code bit} (0: those with no head bit). */
    static void centroid(int from, int bit, float[] out) {
        float x = 0, y = 0, z = 0;
        int n = 0;
        for (int i = from; i < POLYS.size(); i++) {
            Poly p = POLYS.get(i);
            boolean take = bit == 0 ? (p.flags & 1) == 0 : (p.flags & bit) != 0;
            if (!take || (p.flags & 4) != 0) continue;
            x += p.p[0] + (p.p[3] - p.p[0] + p.p[6] - p.p[0]) / 2f;
            y += p.p[1] + (p.p[4] - p.p[1] + p.p[7] - p.p[1]) / 2f;
            z += p.p[2] + (p.p[5] - p.p[2] + p.p[8] - p.p[2]) / 2f;
            n++;
        }
        if (n == 0) return;
        out[0] = x / n;
        out[1] = y / n;
        out[2] = z / n;
    }

    // ================================================================== the 3D layer

    /** One rasterised polygon in world units: a corner and its two edges. */
    static final class Poly {
        final float[] p = new float[9];
        final float[] uv = new float[4];
        int slot;
        float lit = 1f;
        int tint = 0xFFFFFF, add, flags;
        boolean overlay;
        float[] normal;
        /** Pixels below this world height are not drawn (NaN: none) - sinking into the ground. */
        float cut = Float.NaN;
    }

    static final List<Poly> POLYS = new ArrayList<>(), IN = new ArrayList<>();

    /** Polygons from a rig, scaled and placed; slot -1 keeps the rig's own slots. */
    static void add(List<float[]> rig, float scale, float x, float y, float z, int slot, int tint, int add, List<Poly> to) {
        addTurned(rig, scale, x, y, z, 0f, slot, tint, add, to);
    }

    /** {@link #add}, the whole rig first turned {@code roll} radians about the camera's view axis-ish (Z). */
    static void addTurned(List<float[]> rig, float scale, float x, float y, float z, float roll, int slot, int tint, int add, List<Poly> to) {
        float s = 16f * scale;
        float cr = (float) Math.cos(roll), sr = (float) Math.sin(roll);
        for (float[] q : rig) {
            int org = -1, ua = -1, va = -1;
            float uMin = 9, uMax = -9, vMin = 9, vMax = -9;
            for (int k = 0; k < 4; k++) {
                uMin = Math.min(uMin, q[k * 5 + 3]);
                uMax = Math.max(uMax, q[k * 5 + 3]);
                vMin = Math.min(vMin, q[k * 5 + 4]);
                vMax = Math.max(vMax, q[k * 5 + 4]);
            }
            for (int k = 0; k < 4; k++) {
                boolean u0 = Math.abs(q[k * 5 + 3] - uMin) < 1e-5f, v0 = Math.abs(q[k * 5 + 4] - vMin) < 1e-5f;
                boolean u1 = Math.abs(q[k * 5 + 3] - uMax) < 1e-5f, v1 = Math.abs(q[k * 5 + 4] - vMax) < 1e-5f;
                if (u0 && v0) org = k;
                else if (u1 && v0) ua = k;
                else if (u0 && v1) va = k;
            }
            if (org < 0 || ua < 0 || va < 0 || uMax - uMin <= 0f || vMax - vMin <= 0f) continue;
            Poly p = new Poly();
            int[] ks = {org, ua, va};
            for (int i = 0; i < 3; i++) {
                float qx = q[ks[i] * 5] * s, qy = q[ks[i] * 5 + 1] * s, qz = q[ks[i] * 5 + 2] * s;
                p.p[i * 3] = x + qx * cr - qy * sr;
                p.p[i * 3 + 1] = y + qx * sr + qy * cr;
                p.p[i * 3 + 2] = z + qz;
            }
            p.uv[0] = uMin;
            p.uv[1] = vMin;
            p.uv[2] = uMax;
            p.uv[3] = vMax;
            p.slot = slot >= 0 ? slot : (int) q[20];
            p.flags = (int) q[21];
            p.overlay = (p.flags & 4) != 0;
            p.tint = tint;
            p.add = add;
            if (p.overlay) {
                // The eyes and the heart are emissive in vanilla: never darkened, never tinted by the hurt flash.
                p.tint = 0xFFFFFF;
                p.add = 0;
            }
            // Culled as the game culls its cutout entities - heads are drawn whole, as vanilla does.
            if (q.length >= 25 && (p.flags & 1) == 0) {
                float nx = q[22] * cr - q[23] * sr, ny = q[22] * sr + q[23] * cr;
                p.normal = new float[]{nx, ny, q[24]};
            }
            p.lit = p.overlay ? 1f : light(p);
            to.add(p);
        }
    }

    /** A textured box as polygons: centre, half sizes, axes; uv per face {u0, v0, u1, v1} normalised (null skips). */
    static void box(float cx, float cy, float cz, float hx, float hy, float hz, float[] X, float[] Y, float[] Z,
                    float[][] uv, int slot, int tint, int add, List<Poly> to) {
        float[][] axes = {X, Y, Z};
        float[] half = {hx, hy, hz};
        for (int axis = 0; axis < 3; axis++) {
            for (int sign = 1; sign >= -1; sign -= 2) {
                int fi = axis * 2 + (sign > 0 ? 0 : 1);
                if (uv[fi] == null) continue;
                int ua = axis == 1 ? 0 : (axis == 0 ? 2 : 0), va = axis == 1 ? 2 : 1;
                float[] n = axes[axis], U = axes[ua], V = axes[va];
                float hn = half[axis] * sign, hu = half[ua], hv = half[va];
                Poly p = new Poly();
                float ox = cx + n[0] * hn - U[0] * hu + V[0] * hv, oy = cy + n[1] * hn - U[1] * hu + V[1] * hv,
                        oz = cz + n[2] * hn - U[2] * hu + V[2] * hv;
                p.p[0] = ox;
                p.p[1] = oy;
                p.p[2] = oz;
                p.p[3] = ox + U[0] * 2 * hu;
                p.p[4] = oy + U[1] * 2 * hu;
                p.p[5] = oz + U[2] * 2 * hu;
                p.p[6] = ox - V[0] * 2 * hv;
                p.p[7] = oy - V[1] * 2 * hv;
                p.p[8] = oz - V[2] * 2 * hv;
                System.arraycopy(uv[fi], 0, p.uv, 0, 4);
                p.normal = new float[]{n[0] * sign, n[1] * sign, n[2] * sign};
                p.slot = slot;
                p.tint = tint;
                p.add = add;
                p.lit = light(p);
                to.add(p);
            }
        }
    }

    static final float[] AX = {1, 0, 0}, AY = {0, 1, 0}, AZ = {0, 0, 1};
    static final float[] WHOLE = {0f, 0f, 1f, 1f};
    static final float[][] ALL = {WHOLE, WHOLE, WHOLE, WHOLE, WHOLE, WHOLE};

    /** A plain box in one colour (the white texture, tinted). */
    static void solid(float cx, float cy, float cz, float hx, float hy, float hz, float[] X, float[] Y, float[] Z, int rgb, int add, List<Poly> to) {
        box(cx, cy, cz, hx, hy, hz, X, Y, Z, ALL, SLOT_WHITE, rgb, add, to);
    }

    /** Axes turned by yaw (about Y) then pitch (about the turned X), radians. */
    static float[][] axes(float yaw, float pitch, float roll) {
        float cy = (float) Math.cos(yaw), sy = (float) Math.sin(yaw), cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch);
        float cr = (float) Math.cos(roll), sr = (float) Math.sin(roll);
        float[] X = {cy, 0, -sy}, Y = {0, 1, 0}, Z = {sy, 0, cy};
        float[] Y2 = {Y[0] * cp + Z[0] * sp, Y[1] * cp + Z[1] * sp, Y[2] * cp + Z[2] * sp};
        float[] Z2 = {Z[0] * cp - Y[0] * sp, Z[1] * cp - Y[1] * sp, Z[2] * cp - Y[2] * sp};
        float[] X3 = {X[0] * cr + Y2[0] * sr, X[1] * cr + Y2[1] * sr, X[2] * cr + Y2[2] * sr};
        float[] Y3 = {Y2[0] * cr - X[0] * sr, Y2[1] * cr - X[1] * sr, Y2[2] * cr - X[2] * sr};
        return new float[][]{X3, Y3, Z2};
    }

    /** Lit from above and a little from the camera's side, as vanilla's two lights roughly do. */
    static float light(Poly p) {
        float ax = p.p[3] - p.p[0], ay = p.p[4] - p.p[1], az = p.p[5] - p.p[2];
        float bx = p.p[6] - p.p[0], by = p.p[7] - p.p[1], bz = p.p[8] - p.p[2];
        float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        float nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (nl < 1e-6f) return 0.8f;
        return 0.6f + 0.4f * Math.max(Math.abs(ny / nl) * 0.85f, Math.abs((nx * 0.3f + nz * 0.7f) / nl) * 0.7f);
    }

    static boolean facesAway(Poly p) {
        float cx = p.p[0], cy = p.p[1], cz = p.p[2];
        return ChopScene.depth(cx + p.normal[0], cy + p.normal[1], cz + p.normal[2]) - ChopScene.depth(cx, cy, cz) < -1e-4f;
    }

    /** How grey the main layer is this frame (the dyes stop time), and the dye's paint coming back over it. */
    static float grey;
    static float paintX, paintY, paintR;
    static int paintRgb;

    static void rasterise(ChopScene.Canvas c, ModelRaster r, List<Poly> polys, String key, boolean post) {
        if (polys.isEmpty()) return;
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        float[][] scr = new float[polys.size()][];
        for (int i = 0; i < polys.size(); i++) {
            Poly p = polys.get(i);
            float[] o = new float[12];
            for (int k = 0; k < 3; k++) {
                ChopScene.project(p.p[k * 3], p.p[k * 3 + 1], p.p[k * 3 + 2]);
                o[k * 2] = ChopScene.P[0];
                o[k * 2 + 1] = ChopScene.P[1];
                o[6 + k] = ChopScene.depth(p.p[k * 3], p.p[k * 3 + 1], p.p[k * 3 + 2]);
                o[9 + k] = p.p[k * 3 + 1];
            }
            float fx = o[2] + o[4] - o[0], fy = o[3] + o[5] - o[1];
            minX = Math.min(minX, Math.min(Math.min(o[0], o[2]), Math.min(o[4], fx)));
            maxX = Math.max(maxX, Math.max(Math.max(o[0], o[2]), Math.max(o[4], fx)));
            minY = Math.min(minY, Math.min(Math.min(o[1], o[3]), Math.min(o[5], fy)));
            maxY = Math.max(maxY, Math.max(Math.max(o[1], o[3]), Math.max(o[5], fy)));
            scr[i] = o;
        }
        float ink = Math.max(1.5f, ChopScene.size * 0.55f);
        float pad = ink + 2f;
        if (!r.begin(Math.max(0f, minX - pad), Math.max(0f, minY - pad), Math.min(W, maxX + pad), Math.min(H, maxY + pad), rasterScale)) return;
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < polys.size(); i++) {
                Poly p = polys.get(i);
                if (p.overlay != (pass == 1)) continue;
                if (p.normal != null && facesAway(p)) continue;
                if (!r.has(p.slot)) continue;
                float[] o = scr[i];
                r.cutBelow = p.cut;
                r.face(o[0], o[1], o[2] - o[0], o[3] - o[1], o[4] - o[0], o[5] - o[1],
                        o[6], o[7] - o[6], o[8] - o[6], o[9], o[10] - o[9], o[11] - o[9],
                        p.uv[0], p.uv[1], p.uv[2], p.uv[3], p.slot, p.lit, p.tint, p.add, p.overlay);
            }
        }
        r.cutBelow = Float.NaN;
        if (post && (grey > 0f || paintR > 0f)) greyAndPaint(r);
        r.ink(Math.max(1, Math.round(ink * r.scale)));
        r.fade(ChopScene.sceneAlpha);
        c.image(key, r.px, r.w, r.h, r.x, r.y, r.w / r.scale, r.h / r.scale);
    }

    /**
     * The dyes' stopped world: every pixel towards its own brightness in grey, except inside the paint, where the
     * brightness is mapped onto the dye's colour - so the corpse keeps its shading, only in matcha or brick.
     */
    static void greyAndPaint(ModelRaster r) {
        int pr = paintRgb >> 16 & 255, pg = paintRgb >> 8 & 255, pb = paintRgb & 255;
        float rr = paintR * r.scale, px0 = (paintX - r.x) * r.scale, py0 = (paintY - r.y) * r.scale;
        for (int yy = 0; yy < r.h; yy++) {
            for (int xx = 0; xx < r.w; xx++) {
                int i = yy * r.w + xx;
                int c = r.px[i];
                if (c >>> 24 == 0) continue;
                int cr = c >> 16 & 255, cg = c >> 8 & 255, cb = c & 255;
                int lum = (cr * 77 + cg * 150 + cb * 29) >> 8;
                boolean painted = false;
                if (rr > 0f) {
                    float dx = xx - px0, dy = (yy - py0) * 1.3f;
                    // A ragged edge: the paint's front wobbles per column.
                    float wob = 1f + 0.12f * (float) Math.sin(xx * 0.37f + yy * 0.11f);
                    painted = dx * dx + dy * dy < rr * rr * wob * wob;
                }
                if (painted) {
                    float k = 0.35f + lum / 255f * 0.95f;
                    cr = Math.min(255, (int) (pr * k));
                    cg = Math.min(255, (int) (pg * k));
                    cb = Math.min(255, (int) (pb * k));
                } else if (grey > 0f) {
                    int gl = (int) (lum * 0.92f);
                    cr += (int) ((gl - cr) * grey);
                    cg += (int) ((gl - cg) * grey);
                    cb += (int) ((gl + 6 - cb) * grey);
                }
                r.px[i] = 0xFF000000 | Math.max(0, Math.min(255, cr)) << 16 | Math.max(0, Math.min(255, cg)) << 8 | Math.max(0, Math.min(255, cb));
            }
        }
    }

    // ================================================================== the kill

    /** The nametag counting down, a slash and a crit number per blow, the vanilla death poof. */
    static void intro(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long t) {
        float ts = Math.min(1.35f, H / 300f);
        Source src = tl.drop().source;
        int hit = SlayerTimeline.hitIndex(t);
        // The nametag over the boss until it dies, Hypixel's way: "☠ Revenant Horror IV 2.1M❤".
        if (t < SlayerTimeline.KILLED + 250 && rigs != null) {
            float a = (1f - SlayerTimeline.span(t, SlayerTimeline.KILLED, 250)) * Math.min(1f, t / 200f) * ChopScene.sceneAlpha;
            ChopScene.project(HEAD[0], HEAD[1] + 14f * bossScale(src) * (src == Source.REVENANT ? 1f : 0.8f), HEAD[2]);
            float max = src == Source.REVENANT ? 2.4f : 2.4f;
            float hp = hit < 0 ? max : (hit == 0 ? 1.37f : (hit == 1 ? 0.41f : 0f));
            String hpText = hp <= 0f ? "0" : String.format(java.util.Locale.ROOT, "%.1fM", hp);
            int hpCol = hp > 1.6f ? 0x55FF55 : (hp > 0.8f ? 0xFFFF55 : 0xFF5555);
            String name = "☠ " + src.boss + " IV ";
            c.text(name + hpText + "❤", ChopScene.P[0], ChopScene.P[1], 0.85f * ts, ChopScene.alpha(a) | 0xFF5555);
            if (hp > 0f) c.text(name + hpText + "❤", ChopScene.P[0], ChopScene.P[1], 0.85f * ts, ChopScene.alpha(a * 0.25f) | hpCol);
        }
        // Each blow: a white slash across the boss and a crit number floating off.
        for (int i = 0; i < SlayerTimeline.HITS.length; i++) {
            long since = t - SlayerTimeline.HITS[i];
            if (since < 0 || since > 700) continue;
            float k = since / 700f;
            if (since < 160 && rigs != null) {
                float sk = since / 160f;
                float cx = (HEAD[0] + CHEST[0]) / 2f, cyy = (HEAD[1] + CHEST[1]) / 2f, cz = (HEAD[2] + CHEST[2]) / 2f;
                ChopScene.project(cx, cyy, cz);
                float x = ChopScene.P[0], y = ChopScene.P[1];
                float len = 34f * ChopScene.size * 0.6f;
                float ang = (i % 2 == 0 ? -0.6f : 0.6f) + (i == 2 ? 0.2f : 0f);
                float dx = (float) Math.cos(ang) * len, dy = (float) Math.sin(ang) * len;
                // Drawn growing along its length, then gone: three rows of squares, white at the middle.
                int n = 18;
                for (int j = 0; j < n; j++) {
                    float u = j / (float) (n - 1);
                    if (u > sk * 1.6f || u < sk * 1.6f - 0.9f) continue;
                    float fx = x - dx / 2f + dx * u, fy = y - dy / 2f + dy * u;
                    float wdt = (float) Math.sin(u * Math.PI);
                    square(layer, fx, fy, G * (1 + Math.round(wdt * 1.5f)), ChopScene.alpha(0.9f * (1f - sk) * ChopScene.sceneAlpha) | (wdt > 0.6f ? 0xFFFFFF : 0xC8D8FF));
                }
            }
            if (rigs == null) continue;
            String[] crits = {"✧0,784,312✧", "✧1,116,048✧", "✧2,904,770✧"};
            ChopScene.project(HEAD[0] + 16f + i * 5f, HEAD[1] - 6f + 18f * k - i * 7f, HEAD[2] + 6f);
            float a = (k < 0.7f ? 1f : 1f - (k - 0.7f) / 0.3f) * ChopScene.sceneAlpha;
            float pop = 1f + 0.5f * Math.max(0f, 1f - since / 120f);
            c.text(crits[i].replace("0,784", "784"), ChopScene.P[0], ChopScene.P[1], ts * 0.9f * pop * (i == 2 ? 1.25f : 1f),
                    ChopScene.alpha(a) | (i == 2 ? 0xFFAA00 : 0xFFFF55));
        }
    }

    // ================================================================== the drop and the showcase

    /** Where the drop rests in the showcase: the screen's middle, a little high, so the words fit under it. */
    static float restX() {
        return W / 2f;
    }

    static float restY() {
        return H * 0.4f;
    }

    static float restSize() {
        return H * (TL.drop().dye() ? 0.3f : (TL.drop().tier == SlayerTimeline.Tier.RNGESUS ? 0.27f : 0.22f));
    }

    /** The drop: wherever its ending has it until it rises, then into the showcase. */
    static void item(ChopScene.Canvas c, SlayerTimeline tl, long t) {
        float[] at = SlayerEndings.itemAt(tl, t);
        float rise = tl.rise(t);
        if (at == null && rise <= 0f) return;
        float x, y, size, alpha, yaw;
        if (at == null) at = new float[]{restX(), restY(), 0f, 1f, 30f};
        // at: screen x, y, size (GUI px), alpha, yaw.
        x = at[0] + (restX() - at[0]) * rise;
        y = at[1] + (restY() - at[1]) * rise;
        size = at[2] + (restSize() - at[2]) * rise;
        alpha = at[3] + (1f - at[3]) * rise;
        yaw = at[4] + (30f + 12f * (float) Math.sin(t / 650.0) - at[4]) * rise;
        if (rise > 0f) {
            float show = SlayerTimeline.ease((t - tl.arrive() + 400) / 500f);
            SlayerEndings.showcaseBehind(c, tl, t, x, y, size, show);
            float bob = 2f * (float) Math.sin(t / 420.0) * show;
            y += bob;
        }
        if (alpha > 0.02f && size > 1f) c.item(x, y, size, ChopScene.sceneAlpha * Math.min(1f, alpha), yaw);
        if (rise <= 0f) return;
        float ts = Math.min(1.35f, H / 300f);
        banner(c, tl, t, x, y - size * 0.62f - 12f * ts);
        labels(c, tl, t, x, y + size * 0.62f);
    }

    static void banner(ChopScene.Canvas c, SlayerTimeline tl, long t, float x, float y) {
        if (!showBanner) return;
        float k = tl.bannerStamp(t);
        if (k <= 0f) return;
        float ts = Math.min(1.35f, H / 300f);
        float scale = ts * (1.4f + 1.3f * (1f - SlayerTimeline.ease(k)));
        float a = Math.min(1f, k * 2.5f) * ChopScene.sceneAlpha;
        String word = bannerWord != null ? bannerWord : tl.drop().tier.word;
        int rgb = com.sbcs.detect.HellionDrops.tierColour(word);
        if (k < 1f) ChopScene.glow(c, x, y, 160f * ts, rgb, 0.6f * (1f - k));
        c.text(word, x, y, scale, ChopScene.alpha(a) | rgb);
    }

    static void labels(ChopScene.Canvas c, SlayerTimeline tl, long t, float x, float below) {
        float na = tl.nameAlpha(t) * ChopScene.sceneAlpha;
        float ts = Math.min(1.35f, H / 300f);
        int colour = SlayerEndings.nameColour(tl, t);
        if (na > 0.02f) c.name(x, below + 8f * ts, 1.6f * ts, ChopScene.alpha(na) | colour, tl.letters(t));
        float la = tl.lineAlpha(t) * ChopScene.sceneAlpha;
        if (la <= 0.02f) return;
        Drop d = tl.drop();
        String odds = d.oneIn > 0 ? "1 in ~" + String.format("%,d", d.oneIn) + " " + (d.source == Source.REVENANT ? "Horrors" : "Broodfathers") : null;
        String line = odds == null ? nthNote : (nthNote == null ? odds : odds + " · " + nthNote);
        if (line != null) c.text(line, x, below + 22f * ts, ts, ChopScene.alpha(la) | 0xC8C8C8);
        long price = tl.price();
        if (price > 0 && t >= tl.countStart()) {
            float k = Math.min(1f, (t - tl.countStart()) / (float) SlayerTimeline.COUNT_MS);
            long shown = Math.round(price * (1f - (1f - k) * (1f - k) * (1f - k)));
            c.text(String.format("%,d coins", shown), x, below + 35f * ts, 1.25f * ts, ChopScene.alpha(la) | 0xFFD84A);
            if (tl.priceSource() != null) c.text(tl.priceSource(), x, below + 45f * ts, 0.7f * ts, ChopScene.alpha(la) | 0x9A9A9A);
        }
    }

    // ================================================================== small helpers

    /** A square on a pixel layer, snapped to its grid. */
    static void square(CrystalVeins.Fill layer, float x, float y, float s, int argb) {
        if ((argb >>> 24) < 3 || s <= 0f) return;
        float gx = Math.round(x / G) * G, gy = Math.round(y / G) * G;
        float sz = Math.max(G, Math.round(s / G) * G);
        layer.fill(gx - sz / 2f, gy - sz / 2f, sz, sz, argb);
    }

    /** The screen point of a world point, into {@code out} {x, y}. */
    static float[] screen(float x, float y, float z) {
        ChopScene.project(x, y, z);
        return new float[]{ChopScene.P[0], ChopScene.P[1]};
    }

    static int shade(int rgb, float k) {
        int r = Math.min(255, (int) ((rgb >> 16 & 255) * k)), g = Math.min(255, (int) ((rgb >> 8 & 255) * k)), b = Math.min(255, (int) ((rgb & 255) * k));
        return r << 16 | g << 8 | b;
    }

    static int mul(int a, int b) {
        return ((a >> 16 & 255) * (b >> 16 & 255) / 255) << 16 | ((a >> 8 & 255) * (b >> 8 & 255) / 255) << 8 | ((a & 255) * (b & 255) / 255);
    }

    static int mixRgb(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) ((a >> 16 & 255) + ((b >> 16 & 255) - (a >> 16 & 255)) * t);
        int g = (int) ((a >> 8 & 255) + ((b >> 8 & 255) - (a >> 8 & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return r << 16 | g << 8 | bl;
    }
}
