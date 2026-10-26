package com.sbcs.render;

import com.sbcs.anim.PairsTimeline;
import com.sbcs.anim.PairsTimeline.Box;
import com.sbcs.anim.PairsTimeline.Ending;
import com.sbcs.render.art.ChopArt;

import java.util.Random;

import static com.sbcs.anim.PairsTimeline.ease;

/**
 * Draws a frame of "The Hunt" (Superpairs' reveal) onto a {@link ChopScene.Canvas} - the same routine for the game and
 * the offline preview, on the Chopping Block's box renderer (its camera, boxes and ink outline).
 *
 * The box is the cover from the game made solid: an obsidian cube (the game's own texture, adopted at runtime) with a
 * lid and its gold letter on the front. Everything about it before its reveal is the same whatever is inside. Effects
 * are pixel art on a PixelLayer: cracks of light, shards, dust, glyph streams (the game's own enchanting glyphs) and the
 * Nadeshiko Dye's petals.
 */
public final class PairsScene {
    private PairsScene() {}

    public static final String TEX_BOX = "pairs_box";
    /** The game's own textures each one is adopted from (textures/block/...). */
    public static final String[][] VANILLA = {{TEX_BOX, "obsidian"}};
    static final String TEX_WHITE = SeraphScene.TEX_WHITE;

    /** "your 3rd" after the odds, per box; set by the screen. */
    public static String[] nthNotes;
    /** The menu's banner switch, set by the screen each frame. */
    public static boolean showBanner = true;
    /** Draws the pixel layer at once, before the showcase, so the reward and its words stand over it. */
    public static Runnable flushLayer;
    /** The box whose reward {@link ChopScene.Canvas#item} draws now, and whether it got away (drawn grey). */
    public static int current;
    public static boolean greyItem;

    public static int unitFor(int h) {
        return Math.max(2, Math.round(h / 120f));
    }

    // ------------------------------------------------------------------ the box, world units

    /** Half the box; the lid's thickness. */
    static final float B = 6f, LID = 1.1f;
    static final int[] FULL = {0, 0, 16, 16};
    static final int[][] FULL6 = {FULL, FULL, FULL, FULL, FULL, FULL};
    /** Where a box flies in from: up and to the left, where the menu was. */
    static final float[] FROM = {-34f, 17f, -6f};
    static final int GOLD = 0xE8C46A;

    static int unit = 2;
    static float grey;
    static float shade = 1f;
    /** The box's place and turn this frame. */
    static float bx, by, bz, bYaw, bPitch;

    public static void draw(ChopScene.Canvas c, CrystalVeins.Fill layer, PairsTimeline tl, long t, int w, int h) {
        float master = tl.master(t);
        c.fill(0, 0, w, h, ChopScene.alpha(tl.dim(t)) | 0x07060B);
        if (master <= 0.01f || tl.count() == 0) return;
        ChopScene.lightCount = 0;
        ChopScene.gild = null;
        ChopScene.glintHook = null;
        ChopScene.insideOut = false;
        ChopScene.yShift = 0f;
        ChopScene.inkWidth = 0f;
        unit = unitFor(h);
        ChopScene.screenH = h;

        int i = tl.index(t);
        long u = t - tl.start(i);
        Box box = tl.box(i);
        Ending e = box.ending();
        current = i;
        greyItem = e == Ending.MISSED;
        camera(e, u, w, h);
        grey = PairsTimeline.frozen(e, u);
        float a = master * tl.boxAlpha(i, u);
        ChopScene.sceneAlpha = a;
        long r = PairsTimeline.reveal(e);
        float back = ease((u - PairsTimeline.arrive(e) + 500) / 600f);
        shade = 1f - 0.6f * back;

        // A faint light behind the box in its cracks' colour, so it stands off the dimmed game.
        ChopScene.project(0f, 0f, 0f);
        int crack = PairsTimeline.crackRgb(e, u);
        if (u >= PairsTimeline.WIND) {
            float ga = u >= r ? (e == Ending.MISSED ? 0.1f : 0.4f) : 0.12f + 0.2f * PairsTimeline.flash(u);
            ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], 46f * ChopScene.size, greyed(crack), ga * (1f - back));
        }

        boxPlace(e, u);
        switch (e) {
            case RARE -> {
                if (u < r) closedBox(c, layer, box, e, u, 0f);
                else shards(layer, tl, i, e, u, PairsTimeline.burst(e, u), crack, false);
            }
            case DYE -> {
                if (u < PairsTimeline.THAW) {
                    closedBox(c, layer, box, e, u, 0f);
                    if (u >= r) pinkSeep(c, layer, e, u);
                } else {
                    shards(layer, tl, i, e, u, PairsTimeline.burst(e, u), crack, true);
                }
                petals(c, layer, tl, i, e, u, w, h);
            }
            case ULTRA -> {
                if (u < r) closedBox(c, layer, box, e, u, 0f);
                else openBox(c, layer, tl, i, e, u, w, h);
            }
            case MISSED -> {
                closedBox(c, layer, box, e, u, PairsTimeline.lid(e, u));
                if (u >= r) dust(layer, tl, i, u - r);
            }
        }

        shade = 1f;
        grey = 0f;
        ChopScene.sceneAlpha = a;
        showcase(c, layer, tl, i, e, u, w, h);
        queue(c, tl, i, u, w, h);
        if (flushLayer != null) flushLayer.run();
        ChopScene.sceneAlpha = 1f;
        ChopScene.lightCount = 0;
    }

    // ================================================================== camera and the box's place

    static void camera(Ending e, long u, int w, int h) {
        double yaw = Math.toRadians(22f);
        double pitch = Math.toRadians(-14f);
        ChopScene.cy = (float) Math.cos(yaw);
        ChopScene.sy = (float) Math.sin(yaw);
        ChopScene.cp = (float) Math.cos(pitch);
        ChopScene.sp = (float) Math.sin(pitch);
        long r = PairsTimeline.reveal(e);
        float push = 1f + 0.12f * ease(u / (float) r) + 0.03f * PairsTimeline.flash(u);
        if (e == Ending.DYE && u >= PairsTimeline.FREEZE) push += 0.05f * PairsTimeline.heart(e, u);
        ChopScene.size = h * 0.6f / 30f * push;
        ChopScene.fx = 0f;
        ChopScene.fy = 0f;
        ChopScene.fz = 0f;
        float s = ChopScene.calm ? 0f : PairsTimeline.shake(e, u) * h * Punch.shake();
        ChopScene.sx0 = w / 2f + s * (float) Math.sin(u * 0.13);
        ChopScene.sy0 = h * 0.5f + s * (float) Math.cos(u * 0.17);
    }

    static void boxPlace(Ending e, long u) {
        float f = PairsTimeline.fly(u);
        long p = PairsTimeline.phys(e, u);
        float tr = PairsTimeline.tremble(e, u);
        bx = FROM[0] + (0f - FROM[0]) * f + tr * 0.35f * (float) Math.sin(p * 0.9);
        by = FROM[1] + (0f - FROM[1]) * f + 1.2f * (float) Math.sin(p / 520.0) * f + tr * 0.25f * (float) Math.cos(p * 1.3);
        bz = FROM[2] + (0f - FROM[2]) * f;
        bYaw = (float) Math.toRadians(PairsTimeline.yaw(p)) + (1f - f) * 2.6f;
        bPitch = (1f - f) * 0.6f;
    }

    /** A point on the box, in its own frame, to the world. */
    static float[] onBox(float lx, float ly, float lz) {
        float cb = (float) Math.cos(bYaw), sb = (float) Math.sin(bYaw);
        float ca = (float) Math.cos(bPitch), sa = (float) Math.sin(bPitch);
        // X turn (pitch) first, then Y (yaw) - as ChopScene.box does.
        float y1 = ly * ca - lz * sa, z1 = ly * sa + lz * ca;
        float x2 = lx * cb + z1 * sb, z2 = -lx * sb + z1 * cb;
        return new float[]{bx + x2, by + y1, bz + z2};
    }

    /** A direction on the box to the world. */
    static float[] dirBox(float lx, float ly, float lz) {
        float[] p = onBox(lx, ly, lz), o = onBox(0f, 0f, 0f);
        return new float[]{p[0] - o[0], p[1] - o[1], p[2] - o[2]};
    }

    static boolean facing(float[] n) {
        return -n[1] * ChopScene.sp + (-n[0] * ChopScene.sy + n[2] * ChopScene.cy) * ChopScene.cp > 0.02f;
    }

    // ================================================================== the closed box

    static void closedBox(ChopScene.Canvas c, CrystalVeins.Fill layer, Box box, Ending e, long u, float lidAngle) {
        float[] X = dirBox(1, 0, 0), Y = dirBox(0, 1, 0), Z = dirBox(0, 0, 1);
        float[] body = onBox(0f, -LID / 2f, 0f);
        // The lid hinges on its back edge: a missed box's lifts a crack.
        float lc = (float) Math.cos(lidAngle), ls = (float) Math.sin(lidAngle);
        float[] LY = {Y[0] * lc + Z[0] * ls, Y[1] * lc + Z[1] * ls, Y[2] * lc + Z[2] * ls};
        float[] LZ = {Z[0] * lc - Y[0] * ls, Z[1] * lc - Y[1] * ls, Z[2] * lc - Y[2] * ls};
        float[] hinge = onBox(0f, B - LID, -B - 0.15f);
        float hz = B + 0.15f, hy = LID / 2f;
        float[] lid = {hinge[0] + LY[0] * hy + LZ[0] * hz, hinge[1] + LY[1] * hy + LZ[1] * hz, hinge[2] + LY[2] * hy + LZ[2] * hz};

        ChopScene.inkWidth = Math.max(1.2f, ChopScene.size * 0.2f);
        ChopScene.boxAxes(body[0], body[1], body[2], X, Y, Z, B, B - LID / 2f, B, ChopScene.all(TEX_BOX), FULL6, 1, 1, 1, 0, 1, c);
        ChopScene.boxAxes(lid[0], lid[1], lid[2], X, LY, LZ, B + 0.15f, hy, hz, ChopScene.all(TEX_BOX), FULL6, 1, 1, 1, 0, 1, c);
        ChopScene.inkWidth = 0f;

        float flash = PairsTimeline.flash(u);
        float glow = 0.04f + 0.10f * flash + (u >= PairsTimeline.reveal(e) && e != Ending.MISSED ? 0.12f : 0f);
        float[] k = greyed(0.78f, 0.72f, 0.92f);
        boolean lidFirst = lidAngle > 0.05f;
        if (lidFirst) {
            // Lifted, the lid's underside is behind the body's top edge.
            ChopScene.boxAxes(lid[0], lid[1], lid[2], X, LY, LZ, B + 0.15f, hy, hz, ChopScene.all(TEX_BOX), FULL6,
                    k[0] * 1.05f, k[1] * 1.05f, k[2] * 1.05f, glow, 1f, c);
        }
        ChopScene.boxAxes(body[0], body[1], body[2], X, Y, Z, B, B - LID / 2f, B, ChopScene.all(TEX_BOX), FULL6,
                k[0], k[1], k[2], glow, 1f, c);
        if (!lidFirst) {
            ChopScene.boxAxes(lid[0], lid[1], lid[2], X, LY, LZ, B + 0.15f, hy, hz, ChopScene.all(TEX_BOX), FULL6,
                    k[0] * 1.05f, k[1] * 1.05f, k[2] * 1.05f, glow, 1f, c);
        }
        // A missed box: a grey breath of light out of the crack.
        if (lidAngle > 0.02f) {
            float[] gap = onBox(0f, B - LID, B * 0.6f);
            ChopScene.project(gap[0], gap[1], gap[2]);
            ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], 16f * ChopScene.size, 0x9A9AA8, 0.35f * lidAngle / 0.32f);
        }

        shimmer(c, layer, u);
        cracks(c, layer, e, u);
        mark(c, box, e, u);
    }

    /** A faint band of light sweeping across the box's front, the same on every box - the cover's shimmer. */
    static void shimmer(ChopScene.Canvas c, CrystalVeins.Fill layer, long u) {
        float[] n = dirBox(0, 0, 1);
        if (!facing(n)) return;
        float ph = ((u + 400) % 2200) / 2200f;
        float x0 = -B + ph * 4f * B;
        float a = ChopScene.sceneAlpha * 0.28f * (1f - grey);
        for (float y = -B + 0.5f; y <= B - LID; y += 0.9f) {
            float x = x0 - (y + B) * 0.6f;
            if (x < -B || x > B) continue;
            float[] p = onBox(x, y, B + 0.05f);
            ChopScene.project(p[0], p[1], p[2]);
            square(layer, ChopScene.P[0], ChopScene.P[1], unit, ChopScene.alpha(a) | 0x8A78B8);
        }
    }

    /** The letter on the box's front, gold - the same mark the tiles carried in the game. */
    static void mark(ChopScene.Canvas c, Box box, Ending e, long u) {
        float[] n = dirBox(0, 0, 1);
        float face = -n[1] * ChopScene.sp + (-n[0] * ChopScene.sy + n[2] * ChopScene.cy) * ChopScene.cp;
        if (face < 0.2f) return;
        float[] p = onBox(0f, -LID / 2f, B + 0.1f);
        ChopScene.project(p[0], p[1], p[2]);
        float pulse = 0.6f + 0.4f * (float) Math.sin(u / 260.0);
        int col = greyed(e == Ending.MISSED ? 0x8C8478 : GOLD);
        ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], 12f * ChopScene.size, col, 0.25f * pulse * face);
        float a = ChopScene.sceneAlpha * Math.min(1f, (face - 0.2f) * 3f);
        c.text(String.valueOf(box.mark()), ChopScene.P[0], ChopScene.P[1], Math.max(1f, ChopScene.size * 0.42f) * face,
                ChopScene.alpha(a) | col);
    }

    // ------------------------------------------------------------------ cracks of light

    /** The crack paths: three on the front, three on the right, three on the top, in the faces' own (u, v) -1..1. */
    static final float[][][] CRACK_PATHS = crackPaths();
    /** Which face each crack is on: 0 front (+Z), 1 right (+X), 2 top (+Y). */
    static final int[] CRACK_FACE = {0, 1, 2, 0, 1, 2, 0, 1, 2};

    static float[][][] crackPaths() {
        Random r = new Random(0x5A1EL);
        float[][][] out = new float[PairsTimeline.CRACKS][][];
        for (int k = 0; k < out.length; k++) {
            int n = 7;
            float[][] p = new float[n][2];
            // From an edge inward, with kinks.
            int edge = r.nextInt(4);
            float s = r.nextFloat() * 1.4f - 0.7f;
            float x = edge == 0 ? -1f : edge == 1 ? 1f : s, y = edge == 2 ? -1f : edge == 3 ? 1f : s;
            float dx = -x * 0.22f + (r.nextFloat() - 0.5f) * 0.18f, dy = -y * 0.22f + (r.nextFloat() - 0.5f) * 0.18f;
            for (int j = 0; j < n; j++) {
                p[j][0] = Math.max(-0.95f, Math.min(0.95f, x));
                p[j][1] = Math.max(-0.95f, Math.min(0.95f, y));
                x += dx + (r.nextFloat() - 0.5f) * 0.2f;
                y += dy + (r.nextFloat() - 0.5f) * 0.2f;
            }
            out[k] = p;
        }
        return out;
    }

    static float[] facePoint(int face, float fu, float fv) {
        float top = B - LID;
        return switch (face) {
            case 0 -> onBox(fu * B, -LID / 2f + fv * (B - LID / 2f), B + 0.06f);
            case 1 -> onBox(B + 0.06f, -LID / 2f + fv * (B - LID / 2f), -fu * B);
            default -> onBox(fu * B, top + LID + 0.06f, -fv * B);
        };
    }

    static void cracks(ChopScene.Canvas c, CrystalVeins.Fill layer, Ending e, long u) {
        int n = PairsTimeline.cracks(e, u);
        if (n <= 0) return;
        int rgb = greyed(PairsTimeline.crackRgb(e, u));
        long r = PairsTimeline.reveal(e);
        float bright = u >= r ? (e == Ending.MISSED ? 0.55f : 1f) : 0.6f + 0.4f * PairsTimeline.flash(u);
        if (u >= PairsTimeline.HANG && u < r) bright = 0.8f + 0.2f * (float) Math.sin(u / 30.0);
        float[][] normals = {dirBox(0, 0, 1), dirBox(1, 0, 0), dirBox(0, 1, 0)};
        for (int k = 0; k < n; k++) {
            int face = CRACK_FACE[k];
            if (!facing(normals[face])) continue;
            float[][] path = CRACK_PATHS[k];
            // The newest crack runs in over 120 ms.
            float grow = k == n - 1 && u < PairsTimeline.HANG && e != Ending.MISSED
                    ? Math.min(1f, (u - PairsTimeline.STEPS[Math.max(0, PairsTimeline.step(u))]) / 120f) : 1f;
            int segs = path.length - 1;
            float upto = segs * grow;
            for (int s = 0; s < segs && s < upto; s++) {
                float[] p0 = facePoint(face, path[s][0], path[s][1]), p1 = facePoint(face, path[s + 1][0], path[s + 1][1]);
                ChopScene.project(p0[0], p0[1], p0[2]);
                float x0 = ChopScene.P[0], y0 = ChopScene.P[1];
                ChopScene.project(p1[0], p1[1], p1[2]);
                float x1 = ChopScene.P[0], y1 = ChopScene.P[1];
                float len = (float) Math.hypot(x1 - x0, y1 - y0);
                int steps = Math.max(1, Math.round(len / unit));
                float part = Math.min(1f, upto - s);
                for (int q = 0; q <= steps * part; q++) {
                    float f = q / (float) steps;
                    int col = (q + s) % 4 == 0 ? ChopScene.mix(rgb, 0xFFFFFF, 0.55f) : rgb;
                    square(layer, x0 + (x1 - x0) * f, y0 + (y1 - y0) * f, unit, ChopScene.alpha(ChopScene.sceneAlpha * bright) | col);
                }
            }
        }
    }

    /** The Dye's stopped moment: pink light seeping from every crack, swelling on each heartbeat. */
    static void pinkSeep(ChopScene.Canvas c, CrystalVeins.Fill layer, Ending e, long u) {
        float beat = PairsTimeline.heart(e, u);
        float[] p = onBox(0f, 0f, 0f);
        ChopScene.project(p[0], p[1], p[2]);
        // Never greyed: in the stopped world it is the only colour left.
        ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], (26f + 14f * beat) * ChopScene.size, Ending.DYE.tell, 0.3f + 0.4f * beat);
        if (beat < 0.1f) return;
        float[][] normals = {dirBox(0, 0, 1), dirBox(1, 0, 0), dirBox(0, 1, 0)};
        for (int k = 0; k < PairsTimeline.CRACKS; k++) {
            if (!facing(normals[CRACK_FACE[k]])) continue;
            for (float[] q : CRACK_PATHS[k]) {
                float[] w = facePoint(CRACK_FACE[k], q[0], q[1]);
                ChopScene.project(w[0], w[1], w[2]);
                square(layer, ChopScene.P[0] - unit, ChopScene.P[1] - unit, unit * 2, ChopScene.alpha(ChopScene.sceneAlpha * 0.5f * beat) | 0xFFC2DE);
            }
        }
    }

    // ================================================================== the ultra-rare: the box opens, the book

    static void openBox(ChopScene.Canvas c, CrystalVeins.Fill layer, PairsTimeline tl, int i, Ending e, long u, int w, int h) {
        float unf = PairsTimeline.unfold(e, u);
        float[] Y0 = {0f, 1f, 0f};
        float[] k = greyed(0.78f, 0.72f, 0.92f);
        // The bottom, and four sides hinged on its edges, falling outward.
        float[][] outs = {dirBox(0, 0, 1), dirBox(1, 0, 0), dirBox(0, 0, -1), dirBox(-1, 0, 0)};
        float cs = (float) Math.cos(unf), sn = (float) Math.sin(unf);
        float[][][] panels = new float[4][][];
        float[] depths = new float[4];
        for (int s = 0; s < 4; s++) {
            float[] n = outs[s];
            float[] Yp = {Y0[0] * cs + n[0] * sn, Y0[1] * cs + n[1] * sn, Y0[2] * cs + n[2] * sn};
            float[] Zp = {n[0] * cs - Y0[0] * sn, n[1] * cs - Y0[1] * sn, n[2] * cs - Y0[2] * sn};
            float[] Xp = {Yp[1] * Zp[2] - Yp[2] * Zp[1], Yp[2] * Zp[0] - Yp[0] * Zp[2], Yp[0] * Zp[1] - Yp[1] * Zp[0]};
            float[] hinge = {bx + n[0] * B, by - B, bz + n[2] * B};
            float half = B - LID / 2f;
            float[] ctr = {hinge[0] + Yp[0] * half - Zp[0] * 0.3f, hinge[1] + Yp[1] * half - Zp[1] * 0.3f, hinge[2] + Yp[2] * half - Zp[2] * 0.3f};
            panels[s] = new float[][]{ctr, Xp, Yp, Zp, {half}};
            depths[s] = ChopScene.depth(ctr[0], ctr[1], ctr[2]);
        }
        Integer[] order = {0, 1, 2, 3};
        java.util.Arrays.sort(order, (p, q) -> Float.compare(depths[p], depths[q]));
        float[] X = dirBox(1, 0, 0), Z = dirBox(0, 0, 1);
        // Ink first, all of it.
        ChopScene.inkWidth = Math.max(1.2f, ChopScene.size * 0.2f);
        ChopScene.boxAxes(bx, by - B + 0.3f, bz, X, Y0, Z, B, 0.3f, B, ChopScene.all(TEX_BOX), FULL6, 1, 1, 1, 0, 1, c);
        for (int s : order) {
            float[][] p = panels[s];
            ChopScene.boxAxes(p[0][0], p[0][1], p[0][2], p[1], p[2], p[3], B, p[4][0], 0.3f, ChopScene.all(TEX_BOX), FULL6, 1, 1, 1, 0, 1, c);
        }
        ChopScene.inkWidth = 0f;
        // Far sides, the floor, the book, then the near sides.
        int drawn = 0;
        for (int s : order) {
            if (depths[s] > ChopScene.depth(bx, by - B, bz)) break;
            float[][] p = panels[s];
            ChopScene.boxAxes(p[0][0], p[0][1], p[0][2], p[1], p[2], p[3], B, p[4][0], 0.3f, ChopScene.all(TEX_BOX), FULL6, k[0], k[1], k[2], 0.05f, 1f, c);
            drawn++;
        }
        ChopScene.boxAxes(bx, by - B + 0.3f, bz, X, Y0, Z, B, 0.3f, B, ChopScene.all(TEX_BOX), FULL6, k[0], k[1], k[2], 0.05f, 1f, c);
        // The light inside, released.
        ChopScene.project(bx, by - B + 1f, bz);
        ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], 36f * ChopScene.size, Ending.ULTRA.tell, 0.55f * (1f - ease((u - PairsTimeline.BOOK_FLASH) / 600f)));
        book(c, layer, tl, i, e, u);
        for (int n = drawn; n < 4; n++) {
            float[][] p = panels[order[n]];
            ChopScene.boxAxes(p[0][0], p[0][1], p[0][2], p[1], p[2], p[3], B, p[4][0], 0.3f, ChopScene.all(TEX_BOX), FULL6, k[0], k[1], k[2], 0.05f, 1f, c);
        }
        lidFlight(c, e, u);
        glyphs(c, layer, tl, i, e, u, h);
        bookFlash(c, layer, e, u);
    }

    /** The lid blown off: up and back, tumbling, gone. */
    static void lidFlight(ChopScene.Canvas c, Ending e, long u) {
        float s = PairsTimeline.lid(e, u);
        if (s < 0f || s > 1.2f) return;
        float x = bx - 6f * s, y = by + B + 26f * s - 30f * s * s, z = bz - 10f * s;
        float a = 1f - ease((s - 0.6f) / 0.5f);
        float[] k = greyed(0.78f, 0.72f, 0.92f);
        ChopScene.inkWidth = Math.max(1.2f, ChopScene.size * 0.2f);
        ChopScene.box(x, y, z, -3.2f * s, bYaw + 1.5f * s, 0.9f * s, B + 0.15f, LID / 2f, B + 0.15f, ChopScene.all(TEX_BOX), FULL6, 1, 1, 1, 0, a, c);
        ChopScene.inkWidth = 0f;
        ChopScene.box(x, y, z, -3.2f * s, bYaw + 1.5f * s, 0.9f * s, B + 0.15f, LID / 2f, B + 0.15f, ChopScene.all(TEX_BOX), FULL6, k[0], k[1], k[2], 0.1f, a, c);
    }

    /** Where the book stands: risen out of the box, facing the camera. */
    static float[] bookAt(Ending e, long u) {
        float rise = PairsTimeline.bookRise(e, u);
        return new float[]{bx, by - B + 1f + 10f * rise, bz + 2f * rise};
    }

    static final int LEATHER = 0x5C2A86, PAGES = 0xEDE3C4, GILT = 0xD9B451;

    /** The enchanted book: leather covers in the glint's violet, gilt edges, cream pages; opens and riffles. */
    static void book(ChopScene.Canvas c, CrystalVeins.Fill layer, PairsTimeline tl, int i, Ending e, long u) {
        float left = PairsTimeline.bookLeft(e, u);
        float rise = PairsTimeline.bookRise(e, u);
        if (rise <= 0f || left <= 0.01f) return;
        float[] at = bookAt(e, u);
        float s = 0.6f + 0.6f * rise;
        float W = 2.6f * s, H = 3.6f * s, T = 0.9f * s;
        // Turned a little towards the camera; the spine on the left.
        float yaw = -0.38f + 0.12f * (float) Math.sin(u / 900.0);
        float cyw = (float) Math.cos(yaw), syw = (float) Math.sin(yaw);
        float[] X = {cyw, 0f, -syw}, Y = {0f, 1f, 0f}, Z = {syw, 0f, cyw};
        float open = PairsTimeline.bookOpen(e, u);
        float[] lc = rgbF(LEATHER), pc = rgbF(PAGES), gc = rgbF(GILT);
        float glow = 0.1f + 0.5f * PairsTimeline.bookFlash(e, u);
        // Back cover, pages, spine.
        float[] back = add(at, X, 0f, Z, -T - 0.12f * s);
        float[] pages = add(at, X, 0f, Z, 0f);
        float[] spine = add(at, X, -W, Z, 0f);
        ChopScene.inkWidth = Math.max(1f, ChopScene.size * 0.16f);
        ChopScene.boxAxes(back[0], back[1], back[2], X, Y, Z, W, H, 0.12f * s, ChopScene.all(TEX_WHITE), FULL6, 1, 1, 1, 0, left, c);
        ChopScene.boxAxes(spine[0], spine[1], spine[2], X, Y, Z, 0.14f * s, H, T + 0.12f * s, ChopScene.all(TEX_WHITE), FULL6, 1, 1, 1, 0, left, c);
        ChopScene.inkWidth = 0f;
        ChopScene.boxAxes(back[0], back[1], back[2], X, Y, Z, W, H, 0.12f * s, ChopScene.all(TEX_WHITE), FULL6, lc[0], lc[1], lc[2], glow, left, c);
        ChopScene.boxAxes(pages[0], pages[1], pages[2], X, Y, Z, W - 0.15f * s, H - 0.2f * s, T, ChopScene.all(TEX_WHITE), FULL6, pc[0], pc[1], pc[2], glow, left, c);
        ChopScene.boxAxes(spine[0], spine[1], spine[2], X, Y, Z, 0.14f * s, H, T + 0.12f * s, ChopScene.all(TEX_WHITE), FULL6, gc[0], gc[1], gc[2], glow, left, c);
        // Ruled lines on the open page.
        if (open > 0.3f) {
            for (int r = 0; r < 7; r++) {
                float y = H * 0.7f - r * H * 0.2f;
                float[] p0 = add(at, X, -W * 0.7f, Z, T + 0.02f), p1 = add(at, X, W * (r % 3 == 2 ? 0.2f : 0.7f), Z, T + 0.02f);
                ChopScene.project(p0[0], p0[1] + y, p0[2]);
                float x0 = ChopScene.P[0], y0 = ChopScene.P[1];
                ChopScene.project(p1[0], p1[1] + y, p1[2]);
                c.line(x0, y0, ChopScene.P[0], ChopScene.P[1], Math.max(1f, unit * 0.5f), ChopScene.alpha(ChopScene.sceneAlpha * left * 0.6f * open) | 0x7A6A86);
            }
        }
        // Pages turning, right to left about the spine.
        float rf = PairsTimeline.riffle(e, u);
        if (rf > 0f && u < PairsTimeline.BOOK_FLASH) {
            for (int q = 0; q < 3; q++) {
                float f = rf - q * 0.37f;
                if (f < 0f) continue;
                float ang = (float) Math.PI * 0.92f * (f - (float) Math.floor(f));
                pageQuad(c, at, X, Z, W, H, T, ang, left);
            }
        }
        // The front cover, swinging open about the spine.
        float ang = (float) Math.PI * 0.93f * open;
        float ca = (float) Math.cos(ang), sa = (float) Math.sin(ang);
        float[] CX = {X[0] * ca + Z[0] * sa, X[1] * ca + Z[1] * sa, X[2] * ca + Z[2] * sa};
        float[] CZ = {Z[0] * ca - X[0] * sa, Z[1] * ca - X[1] * sa, Z[2] * ca - X[2] * sa};
        float[] hinge = add(at, X, -W, Z, T + 0.12f * s);
        float[] cov = {hinge[0] + CX[0] * W, hinge[1] + CX[1] * W, hinge[2] + CX[2] * W};
        ChopScene.inkWidth = Math.max(1f, ChopScene.size * 0.16f);
        ChopScene.boxAxes(cov[0], cov[1], cov[2], CX, Y, CZ, W, H, 0.12f * s, ChopScene.all(TEX_WHITE), FULL6, 1, 1, 1, 0, left, c);
        ChopScene.inkWidth = 0f;
        ChopScene.boxAxes(cov[0], cov[1], cov[2], CX, Y, CZ, W, H, 0.12f * s, ChopScene.all(TEX_WHITE), FULL6, lc[0], lc[1], lc[2], glow, left, c);
        // Gilt corners on the cover while it is shut.
        if (open < 0.5f) {
            float[] cn = {cov[0] + CZ[0] * 0.14f * s, cov[1], cov[2] + CZ[2] * 0.14f * s};
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sy = -1; sy <= 1; sy += 2) {
                    ChopScene.project(cn[0] + CX[0] * W * 0.8f * sx, cn[1] + H * 0.85f * sy, cn[2] + CX[2] * W * 0.8f * sx);
                    square(layer, ChopScene.P[0], ChopScene.P[1], unit, ChopScene.alpha(ChopScene.sceneAlpha * left * (1f - open * 2f)) | GILT);
                }
            }
        }
        ChopScene.project(at[0], at[1], at[2] + T);
        ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], (14f + 10f * open) * ChopScene.size, Ending.ULTRA.tell, (0.15f + 0.35f * open) * left);
    }

    static void pageQuad(ChopScene.Canvas c, float[] at, float[] X, float[] Z, float W, float H, float T, float ang, float alpha) {
        float ca = (float) Math.cos(ang), sa = (float) Math.sin(ang);
        float[] PX = {X[0] * ca + Z[0] * sa, X[1] * ca + Z[1] * sa, X[2] * ca + Z[2] * sa};
        float[] o = add(at, X, -W, Z, T + 0.03f);
        float lw = (W * 2f - 0.3f);
        int tint = ChopScene.alpha(alpha * 0.95f) | ChopScene.mix(PAGES, 0x9A8E70, 0.35f * sa);
        ChopScene.worldQuad(TEX_WHITE, FULL, o[0], o[1] + H - 0.2f, o[2], PX[0] * lw, PX[1] * lw, PX[2] * lw, 0f, -(H - 0.2f) * 2f, 0f, tint, c);
    }

    /** The enchanting table's glyphs streaming in from all round into the open pages. */
    static void glyphs(ChopScene.Canvas c, CrystalVeins.Fill layer, PairsTimeline tl, int i, Ending e, long u, int h) {
        if (u < PairsTimeline.RIFFLE - 200 || u > PairsTimeline.BOOK_FLASH + 100) return;
        float[] at = bookAt(e, u);
        ChopScene.project(at[0], at[1], at[2] + 1f);
        float tx = ChopScene.P[0], ty = ChopScene.P[1];
        Random r = new Random(tl.seed() * 13 + i * 7 + 11);
        long from = PairsTimeline.RIFFLE - 200, to = PairsTimeline.BOOK_FLASH;
        int n = 44;
        for (int k = 0; k < n; k++) {
            long born = from + (long) (r.nextFloat() * (to - from - 500));
            float life = 650f + r.nextFloat() * 300f;
            double ang = r.nextDouble() * Math.PI * 2;
            float rad = h * (0.28f + 0.14f * r.nextFloat());
            int letter = r.nextInt(26);
            float s = (u - born) / life;
            if (s < 0f || s > 1f) continue;
            float pull = s * s;
            float curl = (1f - s) * 1.4f;
            float x = tx + (float) Math.cos(ang + curl) * rad * (1f - pull), y = ty + (float) Math.sin(ang + curl) * rad * 0.75f * (1f - pull);
            float a = ChopScene.sceneAlpha * Math.min(1f, s * 4f) * (1f - ease((s - 0.85f) / 0.15f));
            int col = k % 3 == 0 ? 0xFFFFFF : k % 3 == 1 ? 0xD9B8FF : 0xA97BFF;
            c.sprite(ChopArt.glyphTex(letter), Math.round(x / unit) * unit, Math.round(y / unit) * unit, unit * 4, unit * 4, 0f, ChopScene.alpha(a) | col);
        }
    }

    /** The book becoming the reward: a ring of violet squares racing out and a burst. */
    static void bookFlash(ChopScene.Canvas c, CrystalVeins.Fill layer, Ending e, long u) {
        float f = PairsTimeline.bookFlash(e, u);
        if (f <= 0.02f) return;
        float[] at = bookAt(e, u);
        ChopScene.project(at[0], at[1], at[2]);
        float since = (u - PairsTimeline.BOOK_FLASH) / 600f;
        ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], 40f * ChopScene.size, 0xF0DCFF, 0.7f * f);
        if (since < 1f) ring(layer, ChopScene.P[0], ChopScene.P[1], since * 34f * ChopScene.size, 48, Ending.ULTRA.tell, 1f - since);
    }

    // ================================================================== shards, petals, dust

    /** The box bursting into black shards edged in the answer's colour, and a ring racing out. */
    static void shards(CrystalVeins.Fill layer, PairsTimeline tl, int i, Ending e, long u, float since, int rgb, boolean pink) {
        if (since < 0f) return;
        ChopScene.project(bx, by, bz);
        float cx = ChopScene.P[0], cy = ChopScene.P[1];
        Random r = new Random(tl.seed() * 31 + i * 5 + 3);
        int n = Punch.count(90);
        for (int k = 0; k < n; k++) {
            float vx = (r.nextFloat() * 2f - 1f) * 26f, vy = 6f + r.nextFloat() * 26f, vz = (r.nextFloat() * 2f - 1f) * 18f;
            float life = 0.8f + r.nextFloat() * 0.7f;
            float s = since;
            if (s > life) continue;
            float x = vx * s, y = vy * s - 34f * s * s, z = vz * s;
            ChopScene.project(bx + x, by + y, bz + z);
            float fade = 1f - s / life;
            int col = k % 4 == 0 ? rgb : k % 4 == 1 ? 0x241A33 : 0x120D1A;
            if (pink && k % 3 == 0) col = 0xFFC2DE;
            square(layer, ChopScene.P[0], ChopScene.P[1], unit * (k % 5 == 0 ? 3 : k % 2 == 0 ? 2 : 1), ChopScene.alpha(ChopScene.sceneAlpha * fade) | col);
        }
        if (since < 0.6f) ring(layer, cx, cy, since / 0.6f * 32f * ChopScene.size, 44, rgb, 1f - since / 0.6f);
    }

    static void ring(CrystalVeins.Fill layer, float cx, float cy, float rad, int n, int rgb, float a) {
        for (int k = 0; k < n; k++) {
            double ang = k * Math.PI * 2 / n;
            square(layer, cx + (float) Math.cos(ang) * rad, cy + (float) Math.sin(ang) * rad * 0.8f, unit,
                    ChopScene.alpha(ChopScene.sceneAlpha * a) | rgb);
        }
    }

    static final int[] PETAL = {0xF7B9DC, 0xEFA7CD, 0xFFD6EA, 0xE58AB8, 0xFFFFFF};

    /**
     * The Nadeshiko Dye's petals: at the thaw they burst out of the box and storm the whole screen in a spiral, then
     * gather into the dye at its showcase; afterwards a light fall of them across the screen until it ends.
     */
    static void petals(ChopScene.Canvas c, CrystalVeins.Fill layer, PairsTimeline tl, int i, Ending e, long u, int w, int h) {
        float since = PairsTimeline.burst(e, u);
        if (since < 0f) return;
        ChopScene.project(bx, by, bz);
        float ox = ChopScene.P[0], oy = ChopScene.P[1];
        float dx = w / 2f, dy = h * 0.42f;
        float gather = (PairsTimeline.arrive(e) - PairsTimeline.THAW) / 1000f;
        Random r = new Random(tl.seed() * 17 + i * 3 + 1);
        int n = Math.min(560, Punch.count(360));
        float diag = (float) Math.hypot(w, h) * 0.55f;
        for (int k = 0; k < n; k++) {
            double a0 = r.nextDouble() * Math.PI * 2;
            float reach = diag * (0.35f + 0.65f * r.nextFloat());
            float spin = 1.4f + r.nextFloat() * 1.6f;
            float delay = r.nextFloat() * 0.25f;
            int col = PETAL[k % PETAL.length];
            float s = since - delay;
            if (s < 0f) continue;
            float x, y, a = 1f;
            if (since < gather) {
                // Out from the box, spiralling, then pulled into the dye.
                float out = Math.min(1f, s / (gather * 0.55f));
                float back = ease((since - gather * 0.55f) / (gather * 0.45f));
                double ang = a0 + spin * s;
                float rad = reach * (1f - (1f - out) * (1f - out)) * (1f - back);
                float cxp = ox + (dx - ox) * ease(since / gather), cyp = oy + (dy - oy) * ease(since / gather);
                x = cxp + (float) Math.cos(ang) * rad;
                y = cyp + (float) Math.sin(ang) * rad * 0.7f;
                a = back > 0.92f ? (1f - back) / 0.08f : 1f;
            } else {
                // Falling across the screen, swaying.
                if (k % 3 != 0) continue;
                float fall = since - gather;
                float x0 = r.nextFloat() * w, period = 3.2f + r.nextFloat() * 2f;
                float ph = (fall / period + r.nextFloat()) % 1f;
                x = x0 + (float) Math.sin(ph * 9 + k) * w * 0.03f;
                y = -unit * 4 + ph * (h + unit * 8);
                a = 0.75f * Math.min(1f, fall * 2f);
            }
            // A petal is two cells, turned by its own flutter.
            boolean flip = ((u / 90 + k) & 1) == 0;
            int argb = ChopScene.alpha(ChopScene.sceneAlpha * a) | col;
            square(layer, x, y, unit, argb);
            square(layer, x + (flip ? unit : 0), y + (flip ? 0 : unit), unit, ChopScene.alpha(ChopScene.sceneAlpha * a) | ChopScene.mix(col, 0xC2508A, 0.35f));
        }
        if (since < 0.7f) ring(layer, ox, oy, since / 0.7f * 40f * ChopScene.size, 56, Ending.DYE.tell, 1f - since / 0.7f);
        // The dye forming where the petals gather.
        float form = ease((since - gather * 0.6f) / (gather * 0.4f));
        if (form > 0f) ChopScene.glow(c, dx, dy, h * 0.5f * form, Ending.DYE.tell, 0.5f * form);
    }

    /** A missed box: grey dust breathing out of the crack under the lid. */
    static void dust(CrystalVeins.Fill layer, PairsTimeline tl, int i, long since) {
        Random r = new Random(tl.seed() * 7 + i);
        for (int k = 0; k < 26; k++) {
            float delay = r.nextFloat() * 500f, life = 900f + r.nextFloat() * 600f;
            float s = (since - 100 - delay) / life;
            if (s < 0f || s > 1f) continue;
            float drift = 1f - (1f - s) * (1f - s);
            float[] p = onBox((r.nextFloat() * 2f - 1f) * B, B - LID, B * 0.8f);
            ChopScene.project(p[0] + (r.nextFloat() - 0.5f) * 6f * drift, p[1] + 5f * drift, p[2] + 4f * drift);
            int col = k % 3 == 0 ? 0x8A8692 : 0x6A6672;
            square(layer, ChopScene.P[0], ChopScene.P[1], unit * (s < 0.4f ? 2 : 1), ChopScene.alpha(ChopScene.sceneAlpha * 0.7f * (1f - s)) | col);
        }
    }

    static void square(CrystalVeins.Fill layer, float x, float y, float s, int argb) {
        if ((argb >>> 24) < 3) return;
        layer.fill(Math.round(x / unit) * unit, Math.round(y / unit) * unit, s, s, argb);
    }

    // ================================================================== the showcase

    static void showcase(ChopScene.Canvas c, CrystalVeins.Fill layer, PairsTimeline tl, int i, Ending e, long u, int w, int h) {
        float rise = PairsTimeline.itemRise(e, u);
        if (rise <= 0f) {
            return;
        }
        float sx, sy;
        if (e == Ending.ULTRA) {
            float[] at = bookAt(e, u);
            ChopScene.project(at[0], at[1], at[2]);
        } else {
            ChopScene.project(bx, by, bz);
        }
        sx = ChopScene.P[0];
        sy = ChopScene.P[1];
        float rx = w / 2f, ry = h * 0.42f;
        float x = sx + (rx - sx) * rise, y = sy + (ry - sy) * rise;
        float big = switch (e) {
            case MISSED -> 0.12f;
            case RARE -> 0.15f;
            case ULTRA -> 0.18f;
            case DYE -> 0.21f;
        };
        float size = h * big * Math.min(1f, 0.35f + rise);
        int light = e == Ending.MISSED ? 0x6A6A72 : e.tell;
        float show = ease((u - PairsTimeline.arrive(e) + 400) / 500f);
        if (show > 0f && e != Ending.MISSED) rays(layer, e, x, y, h, u, show);
        ChopScene.glow(c, x, y, size * 2.6f, light, (e == Ending.MISSED ? 0.2f : 0.45f) * Math.min(1f, rise));
        if (flushLayer != null) flushLayer.run();
        float yaw = 32f + 10f * (float) Math.sin(u / 700.0);
        ChopScene.showItem(c, x, y, size, ChopScene.sceneAlpha * Math.min(1f, rise * 2f), yaw);
        float ts = Math.min(1.35f, h / 300f);
        banner(c, e, u, x, y - size * 0.62f - 12f * ts, h);
        labels(c, tl, i, e, u, x, y + size * 0.62f, h);
    }

    static void rays(CrystalVeins.Fill layer, Ending e, float x, float y, int h, long u, float alpha) {
        boolean big = PairsTimeline.banner(e);
        int n = big ? 12 : 8;
        float turn = u / 2600f;
        float len = h * (big ? 0.5f : 0.33f);
        for (int k = 0; k < n; k++) {
            float ang = turn + k * (float) Math.PI * 2f / n;
            float cs = (float) Math.cos(ang), sn = (float) Math.sin(ang);
            for (float r = h * 0.08f; r < len; r += unit * 1.5f) {
                float fall = 1f - r / len;
                int col = k % 2 == 0 ? e.tell : ChopScene.mix(e.tell, 0xFFFFFF, 0.45f);
                square(layer, x + cs * r, y + sn * r * 0.8f, unit * (fall > 0.6f ? 2 : 1),
                        ChopScene.alpha(alpha * ChopScene.sceneAlpha * fall * fall * (big ? 0.7f : 0.5f)) | col);
            }
        }
    }

    static void banner(ChopScene.Canvas c, Ending e, long u, float x, float y, int h) {
        if (!showBanner) return;
        float k = PairsTimeline.bannerStamp(e, u);
        if (k <= 0f) return;
        float ts = Math.min(1.35f, h / 300f);
        float scale = ts * (1.5f + 1.4f * (1f - ease(k)));
        float a = Math.min(1f, k * 2.5f) * ChopScene.sceneAlpha;
        int rgb = PairsTimeline.bannerRgb(e);
        if (k < 1f) ChopScene.glow(c, x, y, 160f * ts, rgb, 0.6f * (1f - k));
        c.text(PairsTimeline.bannerWord(e), x, y, scale, ChopScene.alpha(a) | rgb);
    }

    static void labels(ChopScene.Canvas c, PairsTimeline tl, int i, Ending e, long u, float x, float below, int h) {
        float ts = Math.min(1.35f, h / 300f);
        float na = PairsTimeline.nameAlpha(e, u) * ChopScene.sceneAlpha;
        Box box = tl.box(i);
        int colour = e == Ending.MISSED ? 0x8A8A8A : box.colour();
        if (na > 0.02f) c.name(x, below + 8f * ts, 1.6f * ts, ChopScene.alpha(na) | colour, PairsTimeline.letters(e, u));
        float la = PairsTimeline.lineAlpha(e, u) * ChopScene.sceneAlpha;
        if (la <= 0.02f) return;
        if (e == Ending.MISSED) {
            c.text("Got away - its pair was never found", x, below + 22f * ts, ts, ChopScene.alpha(la) | 0x9A9A9A);
            return;
        }
        long oneIn = box.reward().oneIn();
        String nth = nthNotes != null && i < nthNotes.length ? nthNotes[i] : null;
        if (oneIn > 0 || nth != null) {
            String line = (oneIn > 0 ? "1 in ~" + String.format("%,d", oneIn) + " experiments" : "") + (oneIn > 0 && nth != null ? " · " : "")
                    + (nth == null ? "" : nth);
            c.text(line, x, below + 22f * ts, ts, ChopScene.alpha(la) | 0xC8C8C8);
        }
        long price = tl.price(i);
        long start = PairsTimeline.countStart(e);
        if (price > 0 && u >= start) {
            float k = Math.min(1f, (u - start) / (float) PairsTimeline.COUNT_MS);
            long shown = Math.round(price * (1f - (1f - k) * (1f - k) * (1f - k)));
            c.text(String.format("%,d coins", shown), x, below + 35f * ts, 1.25f * ts, ChopScene.alpha(la) | 0xFFD84A);
            if (tl.priceSource(i) != null) c.text(tl.priceSource(i), x, below + 45f * ts, 0.7f * ts, ChopScene.alpha(la) | 0x9A9A9A);
        }
    }

    /** With more than one box: their letters along the top, the one opening now in gold. */
    static void queue(ChopScene.Canvas c, PairsTimeline tl, int i, long u, int w, int h) {
        if (tl.count() < 2) return;
        float ts = Math.min(1.35f, h / 300f);
        float gap = 16f * ts;
        float x0 = w / 2f - gap * (tl.count() - 1) / 2f;
        for (int k = 0; k < tl.count(); k++) {
            int col = k == i ? GOLD : k < i ? 0x6A6A6A : 0xB8B8C8;
            if (!tl.box(k).matched() && k != i) col = 0x5A5A5A;
            c.text(String.valueOf(tl.box(k).mark()), x0 + gap * k, 14f * ts, ts, ChopScene.alpha(ChopScene.sceneAlpha * 0.9f) | col);
        }
    }

    // ================================================================== colour

    static int greyed(int rgb) {
        if (shade < 1f) rgb = ChopScene.mix(rgb, 0, 1f - shade);
        if (grey <= 0f) return rgb & 0xFFFFFF;
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        int l = Math.round(0.3f * r + 0.59f * g + 0.11f * b);
        return ChopScene.mix(rgb, l << 16 | l << 8 | l, grey) & 0xFFFFFF;
    }

    static float[] greyed(float r, float g, float b) {
        float[] c = {r * shade, g * shade, b * shade};
        if (grey <= 0f) return c;
        float l = 0.3f * c[0] + 0.59f * c[1] + 0.11f * c[2];
        return new float[]{c[0] + (l - c[0]) * grey, c[1] + (l - c[1]) * grey, c[2] + (l - c[2]) * grey};
    }

    static float[] rgbF(int rgb) {
        return greyed((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f);
    }

    static float[] add(float[] p, float[] a, float ka, float[] b, float kb) {
        return new float[]{p[0] + a[0] * ka + b[0] * kb, p[1] + a[1] * ka + b[1] * kb, p[2] + a[2] * ka + b[2] * kb};
    }
}
