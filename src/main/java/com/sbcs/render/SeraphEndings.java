package com.sbcs.render;

import com.sbcs.anim.SeraphTimeline;
import com.sbcs.anim.SeraphTimeline.Ending;
import com.sbcs.render.art.ChopArt;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.sbcs.anim.SeraphTimeline.ease;
import static com.sbcs.render.SeraphScene.BX;
import static com.sbcs.render.SeraphScene.BZ;
import static com.sbcs.render.SeraphScene.FEET;
import static com.sbcs.render.SeraphScene.unitFor;

/**
 * The Seraph's item endings (2026-10-26, Matej: every drop's animation should be about its item). Each is drawn
 * with {@link SeraphScene}'s machinery and only after the reveal, so SeraphTest's honesty holds:
 * <ul>
 *   <li><b>The Summoning</b> (Summoning Eye) - the eyes are what summon the Ender Dragon at the altar in the
 *   Dragon's Nest. Eight End Portal frames rise in a ring, seven eyes click in, the drop sets into the eighth,
 *   the portal opens for a breath with the dragon's roar far off, and the beam takes the eye back out.</li>
 *   <li><b>Etherwarp</b> (Etherwarp Merger) - the ability's own look: a target block lit, then the item is on
 *   it, four times round the arena.</li>
 *   <li><b>The Runecraft</b> (Enchant Rune) - an enchanting table rises where the glyph stood and the table's own
 *   glyphs stream into it off the fallen Seraph and the pillars, as they fly from bookshelves; the rune is
 *   written out of them.</li>
 *   <li><b>Paragon</b> (Paragon Shard) - both End Crystals crack and burst, and their shards spiral in along
 *   where the beams ran and are pressed into one shard.</li>
 *   <li><b>Set in Stone</b> (End Stone Idol) - the Seraph does not fall: it turns to end stone from the feet up
 *   (texel by texel, through the gild), stands as a statue, cracks with light, and bursts; the idol is what was
 *   inside it.</li>
 * </ul>
 */
final class SeraphEndings {
    private SeraphEndings() {}

    // ---- textures (adopted from the game at runtime, like the rest of the scene)
    static final String TEX_FRAME_TOP = "seraph_frame_top", TEX_FRAME_SIDE = "seraph_frame_side", TEX_FRAME_EYE = "seraph_frame_eye",
            TEX_PORTAL = "seraph_end_portal", TEX_TABLE_TOP = "seraph_table_top", TEX_TABLE_SIDE = "seraph_table_side",
            TEX_TABLE_BOTTOM = "seraph_table_bottom";

    private static final int[] FULL = {0, 0, 16, 16};
    /** The frame block: 13 px tall, its side texture's lower 13 rows (models/block/end_portal_frame.json). */
    private static final int[][] FRAME_UV = {{0, 3, 16, 13}, {0, 3, 16, 13}, FULL, null, {0, 3, 16, 13}, {0, 3, 16, 13}};
    private static final String[] FRAME_TEX = {TEX_FRAME_SIDE, TEX_FRAME_SIDE, TEX_FRAME_TOP, TEX_FRAME_TOP, TEX_FRAME_SIDE, TEX_FRAME_SIDE};
    /** The eye: 8 x 3 x 8 px on top (end_portal_frame_filled.json: up uv 4,4,12,12; sides 4,0,12,3). */
    private static final int[][] EYE_UV = {{4, 0, 8, 3}, {4, 0, 8, 3}, {4, 4, 8, 8}, null, {4, 0, 8, 3}, {4, 0, 8, 3}};
    /** The enchanting table: 12 px tall, the side's lower 12 rows. */
    private static final int[][] TABLE_UV = {{0, 4, 16, 12}, {0, 4, 16, 12}, FULL, null, {0, 4, 16, 12}, {0, 4, 16, 12}};
    private static final String[] TABLE_TEX = {TEX_TABLE_SIDE, TEX_TABLE_SIDE, TEX_TABLE_TOP, TEX_TABLE_BOTTOM, TEX_TABLE_SIDE, TEX_TABLE_SIDE};

    /** Where every one of these drops comes to rest - the showcase, over the glyph's spot. */
    static final float SHOW_Y = FEET + 30f;

    // ================================================================== called from SeraphScene

    /** The 3D parts, after the ground glyph. */
    static void models(ChopScene.Canvas c, CrystalVeins.Fill layer, SeraphTimeline tl, long t, int h) {
        switch (tl.ending()) {
            case ALTAR -> altar(c, layer, tl, t, h);
            case WARP -> warpAim(c, tl, t);
            case RUNE -> table(c, tl, t);
            case SHARD -> shards(c, layer, tl, t, h);
            case IDOL -> rubble(c, tl, t);
            default -> { }
        }
    }

    /** The pixel effects, after the scene's own. */
    static void effects(ChopScene.Canvas c, CrystalVeins.Fill layer, SeraphTimeline tl, long t, int w, int h) {
        float g = unitFor(h);
        switch (tl.ending()) {
            case ALTAR -> portalMotes(c, layer, tl, t, g);
            case WARP -> warpTrails(c, layer, tl, t, g, h);
            case RUNE -> glyphs(c, tl, t, h);
            case IDOL -> statueFx(c, layer, tl, t, g, h);
            default -> { }
        }
    }

    /** The drop, its banner and words. True if this ending drew it (or it is not to be drawn yet). */
    static boolean drop(ChopScene.Canvas c, SeraphTimeline tl, long t, int h) {
        Ending e = tl.ending();
        float[] at;
        float size;
        switch (e) {
            case ALTAR -> {
                at = altarEyePos(tl, t);
                if (at == null) return true;
                size = h * altarEyeSize(tl, t);
            }
            case WARP -> {
                at = warpPos(tl, t);
                if (at == null) return true;
                size = h * (t >= SeraphTimeline.warpAt(SeraphTimeline.W_JUMPS - 1) ? 0.15f : 0.11f);
                // Gone between the aim's end and the landing: it is in the warp.
                if (warpHidden(tl, t)) return true;
            }
            case RUNE -> {
                float f = tl.runeForm(t);
                if (f <= 0f) return true;
                float rise = SeraphTimeline.backOut(Math.min(1f, (t - SeraphTimeline.R_FORM) / (float) (tl.arrive() - SeraphTimeline.R_FORM)));
                at = new float[]{BX, FEET + 12f + (SHOW_Y - FEET - 12f) * rise, BZ};
                size = h * 0.15f * Math.min(1f, 0.3f + rise);
            }
            case SHARD -> {
                if (t < SeraphTimeline.S_FUSE) return true;
                float k = SeraphTimeline.backOut(Math.min(1f, (t - SeraphTimeline.S_FUSE) / 600f));
                at = new float[]{BX, SHOW_Y, BZ};
                size = h * 0.15f * Math.max(0.05f, k);
            }
            case IDOL -> {
                float r = tl.idolRise(t);
                if (t < SeraphTimeline.I_SHATTER) return true;
                float x0 = SeraphTimeline.seraphX(SeraphTimeline.REVEAL);
                at = new float[]{x0, FEET + 24f + (SHOW_Y - FEET - 24f) * r, 0f};
                size = h * 0.17f * Math.min(1f, 0.35f + r);
            }
            default -> {
                return false;
            }
        }
        ChopScene.project(at[0], at[1], at[2]);
        float x = ChopScene.P[0], y = ChopScene.P[1];
        float ts = Math.min(1.35f, h / 300f);
        float show = ease((t - tl.arrive() + 300) / 500f);
        int rgb = switch (e) {
            case ALTAR -> 0x3FBF8A;
            case WARP -> 0xB866F0;
            case RUNE -> 0xC8A0FF;
            case SHARD -> 0x7FE0F0;
            default -> 0xE8E2A0;
        };
        if (e == Ending.IDOL && show > 0f) SeraphScene.rays(c, x, y, h, t, show, 0xC74EBD);
        if (show > 0f && e != Ending.IDOL) ChopScene.glow(c, x, y, size * 1.3f, rgb, 0.45f * show);
        if (e == Ending.IDOL) ChopScene.glow(c, x, y, size * 1.6f, rgb, 0.55f);
        float yaw = 32f + 10f * (float) Math.sin(t / 700.0);
        c.item(x, y, size, ChopScene.sceneAlpha, yaw);
        SeraphScene.banner(c, tl, t, x, y - size * 0.62f - 12f * ts, h);
        SeraphScene.labels(c, tl, t, x, y + size * 0.62f, h);
        return true;
    }

    /** Where the camera pans to as these endings play (x, z), or null for the glyph's spot. */
    static float[] panTarget(SeraphTimeline tl) {
        if (tl.ending() == Ending.IDOL) return new float[]{SeraphTimeline.seraphX(SeraphTimeline.REVEAL), 0f};
        return null;
    }

    static float[] showTarget(SeraphTimeline tl) {
        if (tl.ending() == Ending.IDOL) return new float[]{SeraphTimeline.seraphX(SeraphTimeline.REVEAL), SHOW_Y, 0f};
        return null;
    }

    // ================================================================== the Summoning (the Summoning Eye)

    static final float RING_R = 25f;
    /** The frames at three quarters of a block: full size, eight of them shouldered into one another. */
    static final float FS = 0.72f, FRAME_TOP = FEET + 13f * FS;

    static float[] framePos(int k) {
        double ang = (k + 1) * Math.PI / 4;
        return new float[]{BX + RING_R * (float) Math.sin(ang), BZ + RING_R * (float) Math.cos(ang), (float) ang};
    }

    /** The drop's eye: up the beam, hovering, set into the front frame, then lifted out to be shown. Null: hidden. */
    static float[] altarEyePos(SeraphTimeline tl, long t) {
        long r = SeraphTimeline.REVEAL;
        if (t < r + 350) return null;
        float bob = 1.2f * (float) Math.sin(t / 260.0);
        float[] hover = {BX, SHOW_Y + bob, BZ};
        if (t < SeraphTimeline.A_FRAMES) {
            float k = SeraphTimeline.backOut(Math.min(1f, (t - r - 350) / (float) (SeraphTimeline.A_FRAMES - r - 350)));
            return new float[]{BX, FEET + 8f + (SHOW_Y - FEET - 8f) * k, BZ};
        }
        float[] seat = framePos(SeraphTimeline.A_FRAMES_N - 1);
        float sx = seat[0], sy = FRAME_TOP + 3f, sz = seat[1];
        if (t < SeraphTimeline.A_SEAT) return hover;
        if (t < SeraphTimeline.A_SEATED) {
            float k = ease((t - SeraphTimeline.A_SEAT) / (float) (SeraphTimeline.A_SEATED - SeraphTimeline.A_SEAT));
            return new float[]{hover[0] + (sx - hover[0]) * k, hover[1] + (sy - hover[1]) * k + 10f * (float) Math.sin(Math.PI * k),
                    hover[2] + (sz - hover[2]) * k};
        }
        if (t < SeraphTimeline.A_LIFT) return null;
        float k = SeraphTimeline.backOut(Math.min(1f, (t - SeraphTimeline.A_LIFT) / (float) (tl.arrive() - SeraphTimeline.A_LIFT)));
        return new float[]{sx + (BX - sx) * k, sy + (SHOW_Y - sy) * k, sz + (BZ - sz) * k};
    }

    static float altarEyeSize(SeraphTimeline tl, long t) {
        if (t < SeraphTimeline.A_SEAT) return 0.1f;
        if (t < SeraphTimeline.A_SEATED) return 0.1f - 0.06f * ease((t - SeraphTimeline.A_SEAT) / 450f);
        return 0.06f + 0.09f * ease((t - SeraphTimeline.A_LIFT) / (float) (tl.arrive() - SeraphTimeline.A_LIFT));
    }

    static void altar(ChopScene.Canvas c, CrystalVeins.Fill layer, SeraphTimeline tl, long t, int h) {
        if (t < SeraphTimeline.A_FRAMES) return;
        float portal = tl.altarPortal(t);
        // The portal under everything, inside the ring: the starfield, dark, at the frames' top.
        if (portal > 0.02f) {
            float y = FRAME_TOP - 0.5f, rad = RING_R - 6f * FS - 1f, cell = 2.5f;
            for (float x = -rad; x < rad; x += cell) {
                for (float z = -rad; z < rad; z += cell) {
                    float d = (float) Math.hypot(x + cell / 2, z + cell / 2);
                    if (d > rad * portal) continue;
                    int u = ((int) ((x + rad) * 7 + t / 40)) & 255, v = ((int) ((z + rad) * 7 + t / 70)) & 255;
                    ChopScene.worldQuad(SeraphScene.TEX_WHITE, new int[]{0, 0, 4, 4}, BX + x, y, BZ + z, cell, 0, 0, 0, 0, cell,
                            ChopScene.alpha(0.95f * ChopScene.sceneAlpha) | 0x050A10, c);
                    ChopScene.worldQuad(TEX_PORTAL, new int[]{u, v, 24, 24}, BX + x, y, BZ + z, cell, 0, 0, 0, 0, cell,
                            ChopScene.alpha(ChopScene.sceneAlpha) | 0x6CFFE6, c);
                }
            }
        }
        // The frames, back to front, the ink first.
        List<Integer> order = new ArrayList<>();
        for (int k = 0; k < SeraphTimeline.A_FRAMES_N; k++) order.add(k);
        order.sort((a, b) -> {
            float[] pa = framePos(a), pb = framePos(b);
            return Float.compare(ChopScene.depth(pa[0], FEET, pa[1]), ChopScene.depth(pb[0], FEET, pb[1]));
        });
        for (int pass = 0; pass < 2; pass++) {
            ChopScene.inkWidth = pass == 0 ? Math.max(1.5f, ChopScene.size / 2.5f) : 0f;
            for (int k : order) {
                float rise = tl.altarFrame(t, k);
                if (rise <= 0.01f) continue;
                float[] p = framePos(k);
                float hy = 6.5f * FS * rise;
                ChopScene.box(p[0], FEET + hy, p[1], 0, p[2], 0, 8f * FS, hy, 8f * FS, FRAME_TEX, FRAME_UV, 1, 1, 1, 0, 1f, c);
                if (tl.altarEye(t, k) && rise > 0.5f) {
                    long since = t - (k < SeraphTimeline.A_FRAMES_N - 1 ? SeraphTimeline.A_EYES + k * SeraphTimeline.A_EYE_STEP : SeraphTimeline.A_SEATED);
                    float pop = since < 120 ? 1f + 0.4f * (1f - since / 120f) : 1f;
                    float flare = portal > 0f ? 0.5f + 0.5f * portal : 0f;
                    float e = FS * pop;
                    ChopScene.box(p[0], FEET + 2f * hy + 1.5f * e, p[1], 0, p[2], 0, 4f * e, 1.5f * e, 4f * e,
                            ChopScene.all(TEX_FRAME_EYE), EYE_UV, 1, 1, 1, 0.2f + flare, 1f, c);
                }
            }
        }
        ChopScene.inkWidth = 0f;
        // A green flash on each eye as it clicks in, and all of them while the portal is open.
        for (int k = 0; k < SeraphTimeline.A_FRAMES_N; k++) {
            if (!tl.altarEye(t, k)) continue;
            float[] p = framePos(k);
            long since = t - (k < SeraphTimeline.A_FRAMES_N - 1 ? SeraphTimeline.A_EYES + k * SeraphTimeline.A_EYE_STEP : SeraphTimeline.A_SEATED);
            float a = Math.max((float) Math.exp(-since / 200.0), 0.7f * portal);
            if (a < 0.03f) continue;
            ChopScene.project(p[0], FRAME_TOP + 2f, p[1]);
            ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], ChopScene.size * 14f, 0x3FBF8A, a);
        }
        if (portal > 0f) {
            ChopScene.project(BX, FRAME_TOP, BZ);
            ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], ChopScene.size * 60f * portal, 0x1E7A6A, 0.5f * portal);
        }
    }

    /** The End portal's particles: black and purple motes rising off it while it is open. */
    static void portalMotes(ChopScene.Canvas c, CrystalVeins.Fill layer, SeraphTimeline tl, long t, float g) {
        float portal = tl.altarPortal(t);
        if (portal <= 0f) return;
        Random r = new Random(tl.seed() * 41 + 7);
        float sec = (t - SeraphTimeline.A_OPEN) / 1000f;
        for (int i = 0; i < 70; i++) {
            float ang = r.nextFloat() * 6.283f, rr = (RING_R - 7f) * (float) Math.sqrt(r.nextFloat());
            float life = (sec * (0.6f + 0.6f * r.nextFloat()) + r.nextFloat()) % 1f;
            ChopScene.project(BX + (float) Math.cos(ang) * rr, FRAME_TOP + life * 34f, BZ + (float) Math.sin(ang) * rr);
            float a = portal * ChopScene.sceneAlpha * (1f - life);
            int col = i % 3 == 0 ? 0x101018 : (i % 3 == 1 ? 0x6E2BB8 : 0x2FA890);
            layer.fill(Math.round(ChopScene.P[0] / g) * g, Math.round(ChopScene.P[1] / g) * g, g, g, (Math.round(a * 255) << 24) | col);
        }
    }

    // ================================================================== Etherwarp (the Etherwarp Merger)

    /** Where warp {@code k} puts the drop, and the block it lands on {x, y, z, bx, by, bz, half width}. */
    static float[] warpTarget(int k) {
        return switch (k) {
            // The pillars' inner top blocks, then the ground in front, then the showcase over the glyph's spot.
            case 0, 1 -> {
                int side = k == 0 ? -1 : 1;
                float[] p = SeraphScene.pillarAt(side);
                float x = p[0] - side * 8f, z = p[2] + 8f;
                yield new float[]{x, p[1] + 8f, z, x, p[1] - 8f, z, 8f};
            }
            case 2 -> new float[]{14f, FEET + 8f, 30f, 14f, FEET - 8f, 30f, 8f};
            default -> new float[]{BX, SHOW_Y, BZ, BX, FEET + 8f, BZ, 8f};
        };
    }

    /** The drop before the warps: up the beam to halfway. */
    static float[] warpPos(SeraphTimeline tl, long t) {
        long r = SeraphTimeline.REVEAL;
        if (t < r + 350) return null;
        int k = -1;
        for (int i = 0; i < SeraphTimeline.W_JUMPS; i++) if (t >= SeraphTimeline.warpAt(i)) k = i;
        if (k >= 0) {
            float[] p = warpTarget(k);
            float bob = 1f * (float) Math.sin(t / 240.0);
            return new float[]{p[0], p[1] + bob, p[2]};
        }
        float rise = SeraphTimeline.backOut(Math.min(1f, (t - r - 350) / 700f));
        return new float[]{BX, FEET + 8f + 14f * rise, BZ};
    }

    /** Between leaving and arriving it is nowhere: the last 80 ms of each aim. */
    static boolean warpHidden(SeraphTimeline tl, long t) {
        for (int i = 0; i < SeraphTimeline.W_JUMPS; i++) {
            long at = SeraphTimeline.warpAt(i);
            if (t >= at - 80 && t < at) return true;
        }
        return false;
    }

    /** The target block lit, as the ability shows it: the block washed purple with a bright edge. */
    static void warpAim(ChopScene.Canvas c, SeraphTimeline tl, long t) {
        for (int k = 0; k < SeraphTimeline.W_JUMPS; k++) {
            float a = tl.warpAim(t, k);
            if (a <= 0f) continue;
            float[] p = warpTarget(k);
            float hw = p[6] + 0.3f, flick = 0.85f + 0.15f * (float) Math.sin(t * 0.06);
            ChopScene.box(p[3], p[4], p[5], 0, 0, 0, hw, 8.3f, hw, ChopScene.all(SeraphScene.TEX_WHITE), new int[][]{{0, 0, 4, 4}, {0, 0, 4, 4},
                    {0, 0, 4, 4}, {0, 0, 4, 4}, {0, 0, 4, 4}, {0, 0, 4, 4}}, 0.72f, 0.4f, 0.95f, 0.5f, 0.38f * a * flick, c);
            // The edges.
            float[][] corners = new float[8][];
            for (int i = 0; i < 8; i++) {
                ChopScene.project(p[3] + ((i & 1) == 0 ? -hw : hw), p[4] + ((i & 2) == 0 ? -8.3f : 8.3f), p[5] + ((i & 4) == 0 ? -hw : hw));
                corners[i] = new float[]{ChopScene.P[0], ChopScene.P[1]};
            }
            int[][] edges = {{0, 1}, {2, 3}, {4, 5}, {6, 7}, {0, 2}, {1, 3}, {4, 6}, {5, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
            float lw = Math.max(1f, ChopScene.size * 0.5f);
            for (int[] e : edges) {
                c.line(corners[e[0]][0], corners[e[0]][1], corners[e[1]][0], corners[e[1]][1], lw, ChopScene.alpha(a * ChopScene.sceneAlpha) | 0xE0B0FF);
            }
        }
    }

    /** Each warp: a puff where it was, a streak to where it lands, and a burst there. */
    static void warpTrails(ChopScene.Canvas c, CrystalVeins.Fill layer, SeraphTimeline tl, long t, float g, int h) {
        for (int k = 0; k < SeraphTimeline.W_JUMPS; k++) {
            long at = SeraphTimeline.warpAt(k);
            long since = t - at;
            if (since < -80 || since > 520) continue;
            float[] to = warpTarget(k);
            float[] from = k == 0 ? new float[]{BX, FEET + 22f, BZ} : warpTarget(k - 1);
            ChopScene.project(from[0], from[1], from[2]);
            float fx = ChopScene.P[0], fy = ChopScene.P[1];
            ChopScene.project(to[0], to[1], to[2]);
            float tx = ChopScene.P[0], ty = ChopScene.P[1];
            if (since >= 0 && since < 260) {
                // The streak: dotted, brightest at the far end, gone over a quarter second.
                float a = (1f - since / 260f) * ChopScene.sceneAlpha;
                float len = (float) Math.hypot(tx - fx, ty - fy);
                for (float d = 0; d < len; d += g * 2) {
                    float u = d / len;
                    layer.fill(Math.round((fx + (tx - fx) * u) / g) * g, Math.round((fy + (ty - fy) * u) / g) * g, g, g,
                            (Math.round(255 * a * (0.3f + 0.7f * u)) << 24) | (((int) (d / g)) % 4 < 2 ? 0xE0B0FF : 0x8A2BE2));
                }
            }
            puff(layer, since + 80, fx, fy, g, h, 90 + k, false);
            puff(layer, since, tx, ty, g, h, 120 + k, true);
            if (since >= 0 && since < 300) ChopScene.glow(c, tx, ty, ChopScene.size * 26f, 0xB866F0, 0.6f * (1f - since / 300f));
        }
    }

    /** A teleport puff at a screen point: squares flung out (or in, when {@code in}) over 450 ms. */
    static void puff(CrystalVeins.Fill layer, long since, float x, float y, float g, int h, int salt, boolean out) {
        if (since < 0 || since > 450) return;
        float k = since / 450f;
        int[] cols = {0x8A2BE2, 0xB866F0, 0xE0B0FF, 0x5A1E8C};
        Random r = new Random(salt * 7919L);
        for (int i = 0; i < 26; i++) {
            double ang = r.nextFloat() * Math.PI * 2;
            float sp = h * (0.03f + 0.07f * r.nextFloat());
            float d = out ? sp * ease(k) : sp * (1f - ease(k));
            float a = ChopScene.sceneAlpha * (1f - k);
            layer.fill(Math.round((x + (float) Math.cos(ang) * d) / g) * g, Math.round((y + (float) Math.sin(ang) * d - h * 0.03f * k) / g) * g,
                    g, g, (Math.round(a * 255) << 24) | cols[i % 4]);
        }
    }

    // ================================================================== the Runecraft (the Enchant Rune)

    static void table(ChopScene.Canvas c, SeraphTimeline tl, long t) {
        float rise = tl.runeTable(t);
        if (rise <= 0.01f) return;
        float hy = 6f * rise;
        for (int pass = 0; pass < 2; pass++) {
            ChopScene.inkWidth = pass == 0 ? Math.max(1.5f, ChopScene.size / 2.5f) : 0f;
            ChopScene.box(BX, FEET + hy, BZ, 0, (float) Math.toRadians(-12), 0, 8f, hy, 8f, TABLE_TEX, TABLE_UV, 1, 1, 1, 0.1f, 1f, c);
        }
        ChopScene.inkWidth = 0f;
        float stream = tl.runeStream(t), form = tl.runeForm(t);
        ChopScene.project(BX, FEET + 14f, BZ);
        float flash = t >= SeraphTimeline.R_FORM ? (float) Math.exp(-(t - SeraphTimeline.R_FORM) / 250.0) : 0f;
        ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], ChopScene.size * (14f + 26f * stream + 30f * flash), 0xB070FF,
                0.25f + 0.4f * stream + 0.5f * flash - 0.2f * form);
    }

    /** Where glyph {@code i} comes from: along the fallen Seraph, or off a pillar's top. */
    static float[] glyphSource(Random r, int i) {
        if (i % 3 == 2) {
            float[] p = SeraphScene.pillarAt(i % 2 == 0 ? -1 : 1);
            return new float[]{p[0] + (r.nextFloat() - 0.5f) * 20f, p[1] + r.nextFloat() * 8f, p[2] + (r.nextFloat() - 0.5f) * 20f};
        }
        float x0 = SeraphTimeline.seraphX(SeraphTimeline.REVEAL);
        return new float[]{x0 - r.nextFloat() * 44f, FEET + 2f + r.nextFloat() * 6f, (r.nextFloat() - 0.5f) * 8f};
    }

    /**
     * The glyphs - the game's own sga particles - streaming in to the table's top, thicker as it goes on, then
     * circling the rune once it is written.
     */
    static void glyphs(ChopScene.Canvas c, SeraphTimeline tl, long t, int h) {
        if (t < SeraphTimeline.R_STREAM) return;
        Random r = new Random(tl.seed() * 37 + 11);
        int n = 90;
        float gs = Math.max(4f, ChopScene.size * 3.2f);
        float tx = BX, ty = FEET + 14f, tz = BZ;
        long span = SeraphTimeline.R_STREAM_END - SeraphTimeline.R_STREAM;
        for (int i = 0; i < n; i++) {
            float[] src = glyphSource(r, i);
            long born = SeraphTimeline.R_STREAM + Math.round(span * Math.sqrt(i / (float) n));
            long fly = 800 + r.nextInt(300);
            int letter = r.nextInt(26);
            float lift = 6f + r.nextFloat() * 12f;
            int tint = i % 4 == 0 ? 0xFFFFFF : (i % 4 == 1 ? 0xD8C0FF : (i % 4 == 2 ? 0xB89CF0 : 0xE8E0FF));
            float k = (t - born) / (float) fly;
            if (k < 0f || k > 1f) continue;
            float e = ease(k);
            float x = src[0] + (tx - src[0]) * e, y = src[1] + (ty - src[1]) * e + lift * (float) Math.sin(Math.PI * k), z = src[2] + (tz - src[2]) * e;
            ChopScene.project(x, y, z);
            float a = ChopScene.sceneAlpha * Math.min(1f, k * 5f) * (1f - ease((k - 0.85f) / 0.15f));
            c.sprite(ChopArt.glyphTex(letter), ChopScene.P[0], ChopScene.P[1], gs, gs, 0f, ChopScene.alpha(a) | tint);
        }
        // Circling the rune once it is written.
        float form = tl.runeForm(t);
        if (form > 0f) {
            float rise = SeraphTimeline.backOut(Math.min(1f, (t - SeraphTimeline.R_FORM) / (float) (tl.arrive() - SeraphTimeline.R_FORM)));
            float cy = FEET + 12f + (SHOW_Y - FEET - 12f) * rise;
            for (int i = 0; i < 12; i++) {
                double ang = i / 12.0 * Math.PI * 2 + (t - SeraphTimeline.R_FORM) / 900.0;
                float rad = 15f * form;
                ChopScene.project(BX + (float) Math.cos(ang) * rad, cy + 2f * (float) Math.sin(ang * 3 + t / 300.0), BZ + (float) Math.sin(ang) * rad);
                c.sprite(ChopArt.glyphTex((i * 7) % 26), ChopScene.P[0], ChopScene.P[1], gs, gs, 0f,
                        ChopScene.alpha(0.85f * form * ChopScene.sceneAlpha) | (i % 2 == 0 ? 0xFFFFFF : 0xC8A0FF));
            }
        }
    }

    // ================================================================== Paragon (the Paragon Shard)

    /** How a crystal shakes while it cracks, added to its heart. */
    static float[] crystalShake(SeraphTimeline tl, long t, int side) {
        float k = tl.shardCrack(t);
        if (k <= 0f || tl.crystalsGone(t)) return null;
        float a = 2.2f * k * k;
        return new float[]{a * (float) Math.sin(t * 0.9 + side), a * (float) Math.cos(t * 1.3 + side * 2)};
    }

    /** The shards: thrown out from each crystal, then a tightening spiral into the shard. */
    static void shards(ChopScene.Canvas c, CrystalVeins.Fill layer, SeraphTimeline tl, long t, int h) {
        float g = unitFor(h);
        long pt = tl.phys(t);
        // The crack: a growing white glow on each crystal, flickering faster, chips spat off.
        float crack = tl.shardCrack(t);
        if (crack > 0f && !tl.crystalsGone(t)) {
            for (int side = -1; side <= 1; side += 2) {
                float[] ctr = SeraphScene.crystalAt(side, pt, 0f);
                ChopScene.project(ctr[0], ctr[1], ctr[2]);
                float fl = 0.7f + 0.3f * (float) Math.sin(t * (0.02 + 0.06 * crack));
                ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], ChopScene.size * (10f + 12f * crack), 0xE8FCFF, 0.3f + 0.4f * crack * fl);
                Random r = new Random(tl.seed() * 3 + side * 101 + (t / 90));
                for (int i = 0; i < (int) (8 * crack); i++) {
                    double ang = r.nextFloat() * Math.PI * 2;
                    float d = ChopScene.size * (6f + 10f * r.nextFloat());
                    layer.fill(Math.round((ChopScene.P[0] + (float) Math.cos(ang) * d) / g) * g, Math.round((ChopScene.P[1] + (float) Math.sin(ang) * d) / g) * g,
                            g, g, (Math.round(255 * ChopScene.sceneAlpha) << 24) | (i % 2 == 0 ? 0xFFFFFF : 0x9FE8F8));
                }
            }
        }
        float pull = tl.shardPull(t);
        if (pull < 0f) return;
        // The burst: a flash at each crystal.
        long since = t - SeraphTimeline.S_BURST;
        if (since < 400) {
            for (int side = -1; side <= 1; side += 2) {
                float[] ctr = SeraphScene.crystalAt(side, SeraphTimeline.S_BURST, 0f);
                ChopScene.project(ctr[0], ctr[1], ctr[2]);
                ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], ChopScene.size * 26f, 0xF0FCFF, 0.7f * (1f - since / 400f));
            }
        }
        if (pull >= 1f) {
            long fuse = t - SeraphTimeline.S_FUSE;
            if (fuse < 700) {
                ChopScene.project(BX, SHOW_Y, BZ);
                float ox = ChopScene.P[0], oy = ChopScene.P[1], k = fuse / 700f;
                ChopScene.glow(c, ox, oy, ChopScene.size * 70f * (0.5f + k), 0xE8FCFF, 0.9f * (1f - k));
                float reach = h * 0.35f * ease(k);
                for (int i = 0; i < 60; i++) {
                    double ang = i / 60.0 * Math.PI * 2;
                    layer.fill(Math.round((ox + (float) Math.cos(ang) * reach) / g) * g, Math.round((oy + (float) Math.sin(ang) * reach * 0.7f) / g) * g,
                            g * 2, g, (Math.round(255 * (1f - k) * ChopScene.sceneAlpha) << 24) | (i % 2 == 0 ? 0x7FE0F0 : 0xFFFFFF));
                }
            }
            return;
        }
        List<float[]> boxes = new ArrayList<>();
        for (int side = -1; side <= 1; side += 2) {
            float[] c0 = SeraphScene.crystalAt(side, SeraphTimeline.S_BURST, 0f);
            Random r = new Random(tl.seed() * 53 + side * 7 + 3);
            for (int i = 0; i < 22; i++) {
                float dx = r.nextFloat() - 0.5f, dy = r.nextFloat() - 0.3f, dz = r.nextFloat() - 0.5f;
                float dl = (float) Math.sqrt(dx * dx + dy * dy + dz * dz) + 1e-4f, dist = 10f + 16f * r.nextFloat();
                float[] out = {c0[0] + dx / dl * dist, c0[1] + dy / dl * dist, c0[2] + dz / dl * dist};
                float lag = r.nextFloat() * 0.18f;
                float k = Math.max(0f, Math.min(1f, (pull - lag) / (1f - lag)));
                float[] p = shardAt(out, c0, k, side, i);
                float sz = (1.4f + 1.8f * r.nextFloat()) * (1f - 0.65f * ease(k));
                boxes.add(new float[]{p[0], p[1], p[2], sz, r.nextFloat() * 6f, r.nextFloat() * 6f, k});
                // A short trail of light behind it.
                for (int j = 1; j <= 3; j++) {
                    float[] q = shardAt(out, c0, Math.max(0f, k - j * 0.025f), side, i);
                    ChopScene.project(q[0], q[1], q[2]);
                    layer.fill(Math.round(ChopScene.P[0] / g) * g, Math.round(ChopScene.P[1] / g) * g, g, g,
                            (Math.round(200 * (1f - j / 4f) * ChopScene.sceneAlpha) << 24) | (j == 1 ? 0xE8FCFF : 0x7FD8F0));
                }
            }
        }
        boxes.sort((a, b) -> Float.compare(ChopScene.depth(a[0], a[1], a[2]), ChopScene.depth(b[0], b[1], b[2])));
        float spin = (t - SeraphTimeline.S_BURST) / 160f;
        // The crystal's own cube, the solid part of it (its glass is see-through), outlined so it reads.
        for (int pass = 0; pass < 2; pass++) {
            ChopScene.inkWidth = pass == 0 ? Math.max(1f, ChopScene.size / 3.5f) : 0f;
            for (float[] b : boxes) {
                ChopScene.box(b[0], b[1], b[2], b[4] + spin, b[5] + spin * 0.7f, 0, b[3], b[3], b[3], ChopScene.all(SeraphScene.TEX_CRYSTAL),
                        SeraphScene.CORE_CUBE_UV, 1, 1, 1, 0.25f + 0.45f * b[6], 1f, c);
            }
        }
        ChopScene.inkWidth = 0f;
        ChopScene.project(BX, SHOW_Y, BZ);
        ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], ChopScene.size * (10f + 30f * pull), 0x9FE8F8, 0.3f + 0.5f * pull);
    }

    /** A shard's path: out with the burst for the first fifth, then spiralling in to the showcase point. */
    static float[] shardAt(float[] out, float[] c0, float k, int side, int i) {
        if (k < 0.2f) {
            float e = ease(k / 0.2f);
            return new float[]{c0[0] + (out[0] - c0[0]) * e, c0[1] + (out[1] - c0[1]) * e, c0[2] + (out[2] - c0[2]) * e};
        }
        float u = ease((k - 0.2f) / 0.8f);
        float vx = out[0] - BX, vy = out[1] - SHOW_Y, vz = out[2] - BZ;
        double ang = u * Math.PI * 2.2 * side;
        float cs = (float) Math.cos(ang), sn = (float) Math.sin(ang), s = 1f - u;
        return new float[]{BX + (vx * cs - vz * sn) * s, SHOW_Y + vy * s * s + 8f * (float) Math.sin(Math.PI * u) * ((i % 2) * 2 - 1), BZ + (vx * sn + vz * cs) * s};
    }

    // ================================================================== Set in Stone (the End Stone Idol)

    /** End stone, as the block's texture averages. */
    static final int END_STONE = 0xDCDCA2;
    /** The light inside the statue, showing through its cracks. */
    static final int CRACK_LIGHT = 0xFFE9A8;

    /** The cracks: a few jagged lines down the statue {x at the top, sway, phase, y from, y to}. */
    private static float[][] cracks(long seed) {
        Random r = new Random(seed * 61 + 5);
        float[][] out = new float[7][];
        for (int i = 0; i < out.length; i++) {
            float top = FEET + 10f + r.nextFloat() * 36f;
            out[i] = new float[]{(r.nextFloat() - 0.5f) * 10f, 0.8f + r.nextFloat() * 1.6f, r.nextFloat() * 6f, top, top - 8f - r.nextFloat() * 20f};
        }
        return out;
    }

    /** The stone creeping up the Seraph texel by texel, and the cracks of light once it has set. */
    static ChopScene.Gild stoneGild(SeraphTimeline tl, long t) {
        float stone = tl.stone(t), crack = tl.statueCrack(t);
        float[][] cr = cracks(tl.seed());
        float x0 = SeraphTimeline.seraphX(SeraphTimeline.REVEAL);
        return (x, y, z, cell, now, sx, sy) -> {
            float hgt = (y - FEET) / 46f;
            float n = ((cell * 2654435761L) >>> 16 & 255) / 255f * 0.07f;
            if (hgt + n > stone) return 0;
            if (crack > 0f) {
                for (int i = 0; i < cr.length; i++) {
                    float[] q = cr[i];
                    // Each crack runs down from its top as the crack grows; the later ones start later.
                    float grown = Math.max(0f, crack * 1.4f - i * 0.1f);
                    float bottom = q[3] + (q[4] - q[3]) * Math.min(1f, grown);
                    if (grown <= 0f || y > q[3] || y < bottom) continue;
                    float lineX = x0 + q[0] + q[1] * (float) Math.sin(y * 0.55f + q[2]) + 0.6f * (float) Math.sin(y * 2.1f + q[2] * 3);
                    if (Math.abs(x - lineX) < 0.7f || Math.abs(z - (q[0] * 0.3f) - q[1] * (float) Math.sin(y * 0.7f + q[2])) < 0.45f && Math.abs(x - x0) < 4.5f) {
                        return 0x18 << 24 | CRACK_LIGHT;
                    }
                }
            }
            return 0x58 << 24 | END_STONE;
        };
    }

    /** Light through the cracks, and the dust and the burst at the shatter. */
    static void statueFx(ChopScene.Canvas c, CrystalVeins.Fill layer, SeraphTimeline tl, long t, float g, int h) {
        float x0 = SeraphTimeline.seraphX(SeraphTimeline.REVEAL);
        float crack = tl.statueCrack(t);
        if (crack > 0f && t < SeraphTimeline.I_SHATTER) {
            ChopScene.project(x0, FEET + 26f, 0f);
            float throb = 0.6f + 0.4f * (float) Math.sin(t * (0.008 + 0.03 * crack));
            ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], ChopScene.size * (20f + 40f * crack), CRACK_LIGHT, 0.2f + 0.45f * crack * throb);
            // Dust trickling off it.
            Random r = new Random(tl.seed() * 71 + (t / 120));
            for (int i = 0; i < (int) (10 * crack); i++) {
                ChopScene.project(x0 + (r.nextFloat() - 0.5f) * 10f, FEET + 4f + r.nextFloat() * 40f - ((t % 120) / 120f) * 4f, 3f);
                layer.fill(Math.round(ChopScene.P[0] / g) * g, Math.round(ChopScene.P[1] / g) * g, g, g,
                        (Math.round(220 * ChopScene.sceneAlpha) << 24) | (i % 2 == 0 ? END_STONE : 0xB8B888));
            }
        }
        long since = t - SeraphTimeline.I_SHATTER;
        if (since >= 0 && since < 900) {
            float k = since / 900f;
            ChopScene.project(x0, FEET + 24f, 0f);
            float ox = ChopScene.P[0], oy = ChopScene.P[1];
            ChopScene.glow(c, ox, oy, ChopScene.size * 90f * (0.5f + k), CRACK_LIGHT, 0.95f * (1f - k));
            float reach = (float) Math.hypot(h * 1.7f, h) * 0.5f * ease(k);
            for (int i = 0; i < 80; i++) {
                double ang = i / 80.0 * Math.PI * 2;
                layer.fill(Math.round((ox + (float) Math.cos(ang) * reach) / g) * g, Math.round((oy + (float) Math.sin(ang) * reach * 0.6f) / g) * g,
                        g * 2, g, (Math.round(255 * (1f - k) * ChopScene.sceneAlpha) << 24) | (i % 2 == 0 ? 0xC74EBD : END_STONE));
            }
            Random r = new Random(tl.seed() * 83 + 1);
            for (int i = 0; i < 50; i++) {
                double ang = r.nextFloat() * Math.PI * 2;
                float d = h * (0.05f + 0.2f * r.nextFloat()) * ease(k);
                float s = g * (2 + r.nextInt(3)) * (1f - 0.5f * k);
                layer.fill(Math.round((ox + (float) Math.cos(ang) * d) / g) * g, Math.round((oy + (float) Math.sin(ang) * d * 0.7f + h * 0.08f * k) / g) * g,
                        s, s, (Math.round(200 * (1f - k) * ChopScene.sceneAlpha) << 24) | (i % 3 == 0 ? 0xF4F2D0 : (i % 3 == 1 ? END_STONE : 0xA8A880)));
            }
        }
    }

    /** The statue in pieces: end stone chunks flung out from its chest, tumbling, landing and fading. */
    static void rubble(ChopScene.Canvas c, SeraphTimeline tl, long t) {
        if (t < SeraphTimeline.I_SHATTER) return;
        float sec = (t - SeraphTimeline.I_SHATTER) / 1000f;
        if (sec > 2.6f) return;
        float x0 = SeraphTimeline.seraphX(SeraphTimeline.REVEAL);
        Random r = new Random(tl.seed() * 97 + 13);
        List<float[]> chunks = new ArrayList<>();
        for (int i = 0; i < 46; i++) {
            float px = x0 + (r.nextFloat() - 0.5f) * (i < 30 ? 9f : 16f), py = FEET + 2f + r.nextFloat() * 44f, pz = (r.nextFloat() - 0.5f) * 4f;
            float dx = px - x0, dy = py - (FEET + 24f), dz = pz + 2f;
            float dl = (float) Math.sqrt(dx * dx + dy * dy + dz * dz) + 0.5f, sp = 12f + 18f * r.nextFloat();
            float vx = dx / dl * sp, vy = dy / dl * sp + 14f, vz = dz / dl * sp * 0.4f + 2f;
            float s = 1.2f + 2.2f * r.nextFloat();
            float x = px + vx * sec, z = pz + vz * sec, y = py + vy * sec - 60f * sec * sec;
            boolean landed = y < FEET + s;
            if (landed) y = FEET + s;
            float a = 1f - ease((sec - 1.6f) / 1f);
            if (a < 0.02f) continue;
            float spin = landed ? 0f : sec;
            chunks.add(new float[]{x, y, z, s, r.nextFloat() * 3f + spin * (2f + r.nextFloat() * 4f), r.nextFloat() * 3f + spin * 3f, a});
        }
        chunks.sort((a, b) -> Float.compare(ChopScene.depth(a[0], a[1], a[2]), ChopScene.depth(b[0], b[1], b[2])));
        for (int pass = 0; pass < 2; pass++) {
            ChopScene.inkWidth = pass == 0 ? Math.max(1.5f, ChopScene.size / 2.5f) : 0f;
            for (float[] k : chunks) {
                if (pass == 0 && k[6] < 0.6f) continue;
                ChopScene.box(k[0], k[1], k[2], k[4], k[5], 0, k[3], k[3], k[3], ChopScene.all(SeraphScene.TEX_END_STONE),
                        new int[][]{{0, 0, 8, 8}, {4, 4, 8, 8}, {8, 0, 8, 8}, {0, 8, 8, 8}, {8, 8, 8, 8}, {2, 6, 8, 8}}, 1, 1, 1, 0, k[6], c);
            }
        }
        ChopScene.inkWidth = 0f;
    }
}
