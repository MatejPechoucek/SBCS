package com.sbcs.render;

import com.sbcs.anim.WaterTimeline;
import com.sbcs.anim.WaterTimeline.Creature;
import com.sbcs.anim.WaterTimeline.Drop;
import com.sbcs.anim.WaterTimeline.Ending;

import java.util.ArrayList;
import java.util.List;

/**
 * "The Coil" (the Titanoboa) and "The Totem" (the Wiki Tiki): one routine for the game ({@code gui/WaterScreen}) and
 * the offline preview ({@code tools/WaterPreview}), over ChopScene's Canvas.
 *
 * <h2>How it is drawn</h2>
 * Three kinds of drawing, chosen by cost, as Eruption's:
 * <ul>
 *   <li><b>The water</b> is composed on the CPU into one image, a texel a world unit (the game's own animated water,
 *       tinted for the biome), and drawn with one affine blit - a flat plane under an orthographic camera is a
 *       parallelogram. Ripples, the tell's reflection, the dark under the coil and the dye's flood are in that image.</li>
 *   <li><b>Every model</b> - the Titanoboa's segments (boxes wearing its NEU bestiary skin), the Wiki Tiki's three
 *       heads, its Laser Totems, the compacting ring of terracotta, the lily pad - is rasterised whole with a depth
 *       buffer ({@link ModelRaster}) into one image. Under the surface ({@code cutBelow} 0) nothing is drawn, so the
 *       snake's loops break the water by themselves.</li>
 *   <li><b>Effects</b> are squares on two PixelLayers (splashes, the lasers, the eyes, the bubble, droplets).</li>
 * </ul>
 *
 * <h2>The tell</h2>
 * A colour, the same on every run until the reveal: the snake's scales (a wave of it running head to tail) or the
 * ring's terracotta (a sweep round it) and the Laser Totems' beams. See {@link WaterTimeline}.
 */
public final class WaterScene {
    private WaterScene() {}

    // ================================================================== textures

    /** The 3D layer. Slots are set by the screen or the preview. */
    public static final ModelRaster RASTER = new ModelRaster();
    public static final int SLOT_SKIN = 0, SLOT_WHITE = 1, SLOT_LILY = 2, SLOT_TC = 3, SLOT_LOG = 8, SLOT_LOG_TOP = 9;
    /** The ring's five terracottas, in the tell's order (SLOT_TC + level). */
    public static final String[] RING_TEXTURES = {"terracotta", "lime_terracotta", "yellow_terracotta", "red_terracotta", "light_blue_terracotta"};
    public static final String TEX_RASTER = "water_models", TEX_FLOOR = "water_floor";

    /** The game's still water, grey, 16 wide, {@link #waterFrames} frames of 16 down (null: a flat stand-in). */
    public static int[] waterPixels;
    public static int waterFrames = 1;

    public static String nthNote;
    public static boolean showBanner = true;
    public static Runnable flushBack, flushFront;
    public static float rasterScale = 2f;

    public static int unitFor(int h) {
        return Math.max(2, Math.round(h / 120f));
    }

    // ================================================================== the frame

    static WaterTimeline TL;
    static long T, P;
    static int W, H;
    static float G;
    static final List<SlayerScene.Poly> POLYS = new ArrayList<>();
    /** Where the drop comes from on the screen before it rises (set by the endings). */
    static float originX, originY, originSize;

    public static void draw(ChopScene.Canvas c, CrystalVeins.Fill back, CrystalVeins.Fill front, WaterTimeline tl, long t, int w, int h) {
        TL = tl;
        T = t;
        P = tl.phys(t);
        W = w;
        H = h;
        G = unitFor(h);
        float master = tl.master(t);
        c.fill(0, 0, w, h, ChopScene.alpha(tl.dim(t)) | 0x050A0C);
        if (master <= 0.01f) return;
        ChopScene.sceneAlpha = master;
        ChopScene.lightCount = 0;
        ChopScene.gild = null;
        ChopScene.glintHook = null;
        ChopScene.insideOut = false;
        ChopScene.yShift = 0f;
        camera(tl, t, w, h);
        POLYS.clear();
        originX = w / 2f;
        originY = h * 0.5f;
        originSize = h * 0.08f;

        floor(c, tl, t);
        if (tl.creature() == Creature.TITANOBOA) coil(tl, t, back);
        else totem(tl, t, back);
        if (flushBack != null) flushBack.run();
        rasterise(c);
        if (tl.creature() == Creature.TITANOBOA) coilFront(c, front, tl, t);
        else totemFront(c, front, tl, t);
        flood(c, front, tl, t);
        if (flushFront != null) flushFront.run();
        showcase(c, tl, t);
        ChopScene.sceneAlpha = 1f;
    }

    // ================================================================== the camera

    static final float CAM_YAW = -20f;

    static void camera(WaterTimeline tl, long t, int w, int h) {
        float sway = 2.5f * (float) Math.sin(t / 2600.0);
        float rear = tl.rear(t);
        double yaw = Math.toRadians(CAM_YAW + sway);
        double pitch = Math.toRadians(-27f + 6f * rear);
        ChopScene.cy = (float) Math.cos(yaw);
        ChopScene.sy = (float) Math.sin(yaw);
        ChopScene.cp = (float) Math.cos(pitch);
        ChopScene.sp = (float) Math.sin(pitch);
        // A slow push in through the build-up, the same every run; the dye pulls back for the column.
        float push = WaterTimeline.ease(t / (float) WaterTimeline.REVEAL);
        boolean boa = tl.creature() == Creature.TITANOBOA;
        ChopScene.size = h / (boa ? 78f : 100f) * (1f + 0.12f * push) * (1f - (boa ? 0.3f : 0.2f) * rear);
        ChopScene.fx = 0f;
        ChopScene.fy = (boa ? 6f : 22f) + (boa ? 58f : 30f) * rear;
        ChopScene.fz = 0f;
        ChopScene.sx0 = w / 2f;
        ChopScene.sy0 = h * 0.56f;
        // From the drop's rise the camera eases its focus up, so the showcase sits mid-screen.
        float up = tl.rise(t);
        ChopScene.sy0 += h * 0.05f * up;
        float shake = 2.2f * WaterTimeline.pop(t) + 2.6f * WaterTimeline.tremble(t) + endingShake(tl, t);
        shake *= Punch.shake();
        if (ChopScene.calm) shake = 0f;
        if (shake > 0f) {
            ChopScene.sx0 += shake * (float) Math.sin(t * 0.13);
            ChopScene.sy0 += shake * (float) Math.cos(t * 0.17);
        }
    }

    static float endingShake(WaterTimeline tl, long t) {
        long R = WaterTimeline.R;
        return switch (tl.ending()) {
            case BUST -> tl.creature() == Creature.WIKI_TIKI ? 3f * pulse(t, WaterTimeline.B_TOPPLE + 500, 160) + 3f * pulse(t, WaterTimeline.B_TOPPLE + 850, 160) : 0f;
            case FLOOD -> 4f * tl.heart(t) + 3f * pulse(t, WaterTimeline.F_RESUME, 300) + 2f * tl.rear(t) * (1f - tl.rear(t)) * 4f;
            case MASK -> 2f * pulse(t, WaterTimeline.M_EYES, 200);
            case EYES -> 2.5f * pulse(t, WaterTimeline.E_OPEN, 220);
            case SHED -> 1.5f * pulse(t, WaterTimeline.S_SLIDE, 300);
            case BUBBLE -> 2f * pulse(t, WaterTimeline.U_FREE, 200);
            default -> 1.5f * pulse(t, R, 200);
        };
    }

    static float pulse(long t, long at, float ms) {
        return t < at ? 0f : (float) Math.exp(-(t - at) / ms);
    }

    // ================================================================== the water

    /** The water plane, world units: x and z extents. One texel a unit. */
    static final int FX0 = -128, FX1 = 128, FZ0 = -96, FZ1 = 72;
    static final int FW = FX1 - FX0, FH = FZ1 - FZ0;
    static int[] floorPx = new int[FW * FH];
    /** Where the snake's body breaks the surface this frame: x, z, radius (for the ripples). */
    static final float[] CROSS = new float[3 * 64];
    static int crossings;

    static void floor(ChopScene.Canvas c, WaterTimeline tl, long t) {
        boolean boa = tl.creature() == Creature.TITANOBOA;
        // The bayou's swamp water, a hotspot's clear blue.
        int tint = boa ? 0x5A7E60 : 0x3F78C8;
        int lv = tl.colour(t);
        float pop = WaterTimeline.pop(t);
        float glowR = boa ? 58f : 52f;
        float reflect = 0.12f + 0.22f * pop + 0.18f * WaterTimeline.tremble(t);
        if (t >= WaterTimeline.REVEAL) reflect = 0.2f * (1f - WaterTimeline.span(t, WaterTimeline.REVEAL + 400, 900)) + (tl.drop().win() ? 0.14f : 0f);
        float grey = tl.grey(t), flood = tl.flood(t);
        float dark = tl.ending() == Ending.EYES ? WaterTimeline.span(t, WaterTimeline.E_DARK, WaterTimeline.E_DARK_TO - WaterTimeline.E_DARK) : 0f;
        int frame = waterPixels == null ? 0 : (int) ((P / 110) % Math.max(1, waterFrames));
        float ringR = (P % 2400) / 2400f * 90f, ringR2 = ((P + 1200) % 2400) / 2400f * 90f;
        float floodR = flood * 120f;
        float a0 = ChopScene.sceneAlpha;
        for (int zz = 0; zz < FH; zz++) {
            int wz = FZ0 + zz;
            for (int xx = 0; xx < FW; xx++) {
                int wx = FX0 + xx;
                // Fades out with distance, so the dimmed game shows round it.
                float ex = wx / 128f, ez = wz < 0 ? wz / 96f : wz / 72f;
                float edge = 1f - (ex * ex + ez * ez);
                if (edge <= 0f) {
                    floorPx[zz * FW + xx] = 0;
                    continue;
                }
                float alpha = Math.min(1f, edge * 2.4f) * 0.93f;
                int g = 150;
                if (waterPixels != null) {
                    int px = waterPixels[(frame * 16 + Math.floorMod(wz, 16)) * 16 + Math.floorMod(wx, 16)];
                    g = (px >> 16 & 255) * 77 + (px >> 8 & 255) * 150 + (px & 255) * 29 >> 8;
                }
                float k = g / 255f;
                int r = (int) ((tint >> 16 & 255) * k), gg = (int) ((tint >> 8 & 255) * k), b = (int) ((tint & 255) * k);
                float d = (float) Math.sqrt(wx * wx + wz * wz);
                // The rings the creature sends out, and the tell's colour on the water round it.
                float ring = Math.max(ringBand(d, ringR), ringBand(d, ringR2)) * 0.28f * Math.max(0f, 1f - d / 110f);
                for (int i = 0; i < crossings; i++) {
                    float cx = wx - CROSS[i * 3], cz = wz - CROSS[i * 3 + 1];
                    float cr = CROSS[i * 3 + 2];
                    float dd = cx * cx + cz * cz;
                    // Foam round where the body breaks the surface.
                    if (dd < cr * cr * 2.0f) ring = Math.max(ring, 0.16f * (1f - dd / (cr * cr * 2.0f)) + 0.06f);
                }
                if (ring > 0f) {
                    r += (int) ((235 - r) * ring);
                    gg += (int) ((245 - gg) * ring);
                    b += (int) ((240 - b) * ring);
                }
                float rf = reflect * Math.max(0f, 1f - d / glowR);
                if (rf > 0f) {
                    r += (int) (((lv >> 16 & 255) - r) * rf);
                    gg += (int) (((lv >> 8 & 255) - gg) * rf);
                    b += (int) (((lv & 255) - b) * rf);
                }
                if (dark > 0f) {
                    float dk = dark * Math.max(0f, 1f - d / 34f) * 0.9f;
                    r = (int) (r * (1f - dk));
                    gg = (int) (gg * (1f - dk));
                    b = (int) (b * (1f - dk));
                }
                if (grey > 0f) {
                    int l = (r * 77 + gg * 150 + b * 29) >> 8;
                    r += (int) ((l - r) * grey);
                    gg += (int) ((l - gg) * grey);
                    b += (int) ((l + 4 - b) * grey);
                }
                if (floodR > 0f) {
                    // The aquamarine spreads texel by texel from the middle, a ragged front.
                    float wob = 1f + 0.16f * (float) Math.sin(Math.atan2(wz, wx) * 7 + wz * 0.08f) * (float) Math.cos(wx * 0.11f);
                    if (d < floodR * wob) {
                        float kk = (0.45f + k * 0.85f) * 0.6f;
                        r = Math.min(255, (int) ((WaterTimeline.AQUAMARINE >> 16 & 255) * kk * 0.85f));
                        gg = Math.min(255, (int) ((WaterTimeline.AQUAMARINE >> 8 & 255) * kk));
                        b = Math.min(255, (int) ((WaterTimeline.AQUAMARINE & 255) * kk));
                        alpha = Math.max(alpha, Math.min(1f, edge * 3f));
                    }
                }
                int a = Math.round(alpha * a0 * 255);
                floorPx[zz * FW + xx] = a << 24 | clamp(r) << 16 | clamp(gg) << 8 | clamp(b);
            }
        }
        ChopScene.project(FX0, 0f, FZ0);
        float ox = ChopScene.P[0], oy = ChopScene.P[1];
        ChopScene.project(FX1, 0f, FZ0);
        float ux = ChopScene.P[0] - ox, uy = ChopScene.P[1] - oy;
        ChopScene.project(FX0, 0f, FZ1);
        float vx = ChopScene.P[0] - ox, vy = ChopScene.P[1] - oy;
        c.imageAffine(TEX_FLOOR, floorPx, FW, FH, ox, oy, ux, uy, vx, vy);
        crossings = 0;
    }

    static float ringBand(float d, float r) {
        float x = Math.abs(d - r);
        return x < 1.2f ? 1f - x / 1.2f : 0f;
    }

    // ================================================================== the Titanoboa

    static final int SEGMENTS = 24;
    static final float SPACING = 8.4f;
    /** The dye's column: height a segment as the snake rears. */
    static final float COLUMN_STEP = 3.1f;

    /** One segment this frame: x, y, z, yaw (radians), half size. */
    static final float[][] SEG = new float[SEGMENTS][5];

    /** The coil at scene time {@code p} (the physical clock): every segment's place. */
    static void coilAt(long p, long t, float[][] out) {
        float tight = WaterTimeline.tighten(t);
        float rHead = 31f - 12f * tight;
        float b = 4.6f - 1.4f * tight;
        double th = p * 0.00105 + 3.1 * tight * tight;
        double theta = th;
        for (int i = 0; i < SEGMENTS; i++) {
            float r = (float) (rHead + b * (th - theta));
            float hs = i == 0 ? 6.4f : 5.6f - 2.9f * (float) Math.pow(i / (float) (SEGMENTS - 1), 1.3);
            float x = (float) (r * Math.cos(theta)), z = (float) (r * Math.sin(theta));
            // Rising out of the water, head first; then loops: the body rolls up and down through the surface.
            float em = WaterTimeline.ease((t - WaterTimeline.EMERGE_FROM - i * 34f) / 700f);
            float base = hs * 0.35f + hs * 0.95f * (float) Math.sin(p * 0.0042 - i * 0.78);
            if (i == 0) base = 13f + 2.2f * (float) Math.sin(p * 0.003);
            if (i == 1) base = Math.max(base, 7f);
            float y = -hs * 2.4f - 10f + (base + hs * 2.4f + 10f) * em;
            out[i][0] = x;
            out[i][1] = y;
            out[i][2] = z;
            out[i][3] = (float) -theta;
            out[i][4] = hs;
            theta -= SPACING / Math.max(4f, r);
        }
        // The head looks out of the coil at the camera's side.
        out[0][3] += 0.9f;
    }

    static void coil(WaterTimeline tl, long t, CrystalVeins.Fill back) {
        Ending e = tl.ending();
        long R = WaterTimeline.R;
        coilAt(P, Math.min(t, e == Ending.FLOOD && t > WaterTimeline.F_FREEZE ? WaterTimeline.F_FREEZE : t), SEG);
        float tremble = WaterTimeline.tremble(t);
        if (tremble > 0f) {
            for (int i = 0; i < SEGMENTS; i++) {
                SEG[i][0] += 0.9f * tremble * (float) Math.sin(t * 0.09 + i * 1.7);
                SEG[i][2] += 0.9f * tremble * (float) Math.cos(t * 0.11 + i * 2.3);
            }
        }
        lilyPad(tl, t);
        boolean drawReal = true;
        // ---- the endings move the body
        switch (e) {
            case BUST, BOOK -> {
                long from = e == Ending.BUST ? WaterTimeline.B_SINK : WaterTimeline.K_UNDER;
                long to = e == Ending.BUST ? WaterTimeline.B_SINK_TO : WaterTimeline.K_UNDER_TO;
                for (int i = 0; i < SEGMENTS; i++) {
                    float k = WaterTimeline.ease((t - from - i * 18f) / (float) (to - from));
                    SEG[i][0] *= 1f + 0.35f * k;
                    SEG[i][2] *= 1f + 0.35f * k;
                    SEG[i][1] -= k * (SEG[i][1] + SEG[i][4] * 3f + 6f);
                }
            }
            case SHED -> {
                // The skin stays where it was; the snake slides out of it outwards and under.
                float[][] skin = new float[SEGMENTS][5];
                coilAt(WaterTimeline.S_SLIDE, WaterTimeline.S_SLIDE, skin);
                hollow(skin, t);
                for (int i = 0; i < SEGMENTS; i++) {
                    float q = WaterTimeline.ease(((t - WaterTimeline.S_SLIDE) / (float) (WaterTimeline.S_SLIDE_TO - WaterTimeline.S_SLIDE)) * 1.5f - i / (float) SEGMENTS * 0.5f);
                    if (t < WaterTimeline.S_SLIDE) break;
                    System.arraycopy(skin[i], 0, SEG[i], 0, 5);
                    SEG[i][0] *= 1f + 0.9f * q;
                    SEG[i][2] *= 1f + 0.9f * q;
                    SEG[i][1] -= q * (SEG[i][1] + SEG[i][4] * 3f + 8f);
                }
            }
            case FLOOD -> {
                float rear = tl.rear(t);
                if (rear > 0f) {
                    for (int i = 0; i < SEGMENTS; i++) {
                        float k = WaterTimeline.ease(rear * 1.6f - i / (float) SEGMENTS * 0.6f);
                        double a = 0.6 + i * 0.62 + P * 0.002;
                        float cx = (float) (7.5 * Math.cos(a)), cz = (float) (7.5 * Math.sin(a));
                        float cyy = 6f + (SEGMENTS - 1 - i) * COLUMN_STEP + (i == 0 ? 6f : 0f);
                        SEG[i][0] += (cx - SEG[i][0]) * k;
                        SEG[i][1] += (cyy - SEG[i][1]) * k;
                        SEG[i][2] += (cz - SEG[i][2]) * k;
                        if (i == 0) SEG[i][3] += (float) ((Math.toRadians(-CAM_YAW) - SEG[i][3]) * k);
                    }
                }
            }
            default -> { }
        }
        // ---- the body: each segment a box of the skin, the tell's colour running down it
        float dark = e == Ending.EYES ? WaterTimeline.span(t, WaterTimeline.E_DARK, WaterTimeline.E_DARK_TO - WaterTimeline.E_DARK) : 0f;
        float green = e == Ending.BOOK ? WaterTimeline.span(t, WaterTimeline.K_GLOW, 300) : 0f;
        for (int i = drawReal ? 0 : SEGMENTS; i < SEGMENTS; i++) {
            float[] s = SEG[i];
            long at = t - i * 30L;
            float lvl = tl.level(at);
            int col = WaterTimeline.colourAt(tl.creature(), lvl);
            float pop = WaterTimeline.pop(at);
            int tint = WaterTimeline.mix(0xFFFFFF, col, 0.62f);
            float landed = at >= WaterTimeline.REVEAL ? 0.3f * pulse(at, WaterTimeline.REVEAL, 500) : 0f;
            float strength = 0.26f + 0.3f * pop + landed;
            int add = ChopScene.alpha(strength) | col;
            if (dark > 0f) {
                tint = WaterTimeline.mix(tint, 0x1A1610, dark * 0.85f);
                add = ChopScene.alpha(strength * (1f - dark)) | col;
            }
            if (green > 0f) add = ChopScene.alpha(0.3f * green) | 0x55FF55;
            segment(s, i == 0, tint, add);
            if (s[1] < s[4] && s[1] > -s[4] && crossings < 64) {
                CROSS[crossings * 3] = s[0];
                CROSS[crossings * 3 + 1] = s[2];
                CROSS[crossings * 3 + 2] = s[4] * 1.15f;
                crossings++;
            }
        }
        if (e == Ending.EYES) {
            originX = screenX(0f, 9f, 0f);
            originY = screenY(0f, 9f, 0f);
        } else if (e == Ending.SHED) {
            originX = screenX(0f, 4f, 0f);
            originY = screenY(0f, 4f, 0f);
        } else if (e == Ending.FLOOD) {
            originX = screenX(SEG[0][0], SEG[0][1] + 14f, SEG[0][2]);
            originY = screenY(SEG[0][0], SEG[0][1] + 14f, SEG[0][2]);
        } else {
            originX = screenX(0f, 2f, 0f);
            originY = screenY(0f, 2f, 0f);
        }
    }

    /** One of the Titanoboa's segments: a cube wearing its skin, the head with its hat layer. */
    static void segment(float[] s, boolean head, int tint, int add) {
        float[][] ax = SlayerScene.axes(s[3], head ? -0.25f : 0f, 0f);
        SlayerScene.box(s[0], s[1], s[2], s[4], s[4], s[4], ax[0], ax[1], ax[2], headUv(false), SLOT_SKIN, tint, add, POLYS);
        if (head) {
            float hh = s[4] * 1.125f;
            SlayerScene.box(s[0], s[1], s[2], hh, hh, hh, ax[0], ax[1], ax[2], headUv(true), SLOT_SKIN, tint, add, POLYS);
        }
    }

    /** The Shed's empty skin: pale, a little bigger than the snake, crumpling into the middle tail first. */
    static void hollow(float[][] skin, long t) {
        if (t < WaterTimeline.S_SLIDE) return;
        for (int i = 0; i < SEGMENTS; i++) {
            float[] s = skin[i].clone();
            float k = WaterTimeline.ease(((t - WaterTimeline.S_CRUMPLE) / (float) (WaterTimeline.S_CRUMPLE_TO - WaterTimeline.S_CRUMPLE)) * 1.6f
                    - (SEGMENTS - 1 - i) / (float) SEGMENTS * 0.6f);
            if (k >= 1f) continue;
            s[0] *= 1f - k;
            s[2] *= 1f - k;
            s[1] += (4f - s[1]) * k;
            s[4] *= 1.08f * (1f - k);
            if (s[4] < 0.4f) continue;
            float in = WaterTimeline.span(t, WaterTimeline.S_SLIDE, 500);
            int tint = WaterTimeline.mix(0xFFFFFF, 0xF2EAD2, in);
            int add = ChopScene.alpha(0.55f * in) | 0xF4EEDC;
            segment(s, i == 0, tint, add);
        }
    }

    static void lilyPad(WaterTimeline tl, long t) {
        float bob = 0.4f * (float) Math.sin(P * 0.004);
        float up = tl.ending() == Ending.BUST ? 2.5f * WaterTimeline.span(t, WaterTimeline.B_SINK + 400, 500) : 0f;
        float y = 0.3f + bob + up;
        float[][] uv = {null, null, {0f, 0f, 1f, 1f}, null, null, null};
        float[][] ax = SlayerScene.axes(0.4f + 0.0002f * P, 0f, 0f);
        SlayerScene.box(0f, y, 0f, 11f, 0.25f, 11f, ax[0], ax[1], ax[2], uv, SLOT_LILY, 0x2E8A30, 0, POLYS);
    }

    /** The coil's effects over the models: splashes where it breaks the surface, the eyes, the shed's flakes. */
    static void coilFront(ChopScene.Canvas c, CrystalVeins.Fill layer, WaterTimeline tl, long t) {
        // Drips off the loops as they come up, the same every run.
        for (int i = 2; i < SEGMENTS; i += 3) {
            float[] s = SEG[i];
            if (s[1] < 0f || t > WaterTimeline.REVEAL + 600) continue;
            long ph = (P + i * 211L) % 900;
            float k = ph / 900f;
            float x = screenX(s[0], s[1] - s[4] - k * 9f, s[2]), y = screenY(s[0], s[1] - s[4] - k * 9f, s[2]);
            square(layer, x, y, G, ChopScene.alpha(0.7f * (1f - k) * ChopScene.sceneAlpha) | 0xBFE8E0);
        }
        Ending e = tl.ending();
        if (e == Ending.EYES) eyes(c, layer, tl, t);
        if (e == Ending.SHED && t >= WaterTimeline.S_CRUMPLE) {
            long since = t - WaterTimeline.S_CRUMPLE;
            for (int i = 0; i < 26; i++) {
                float a = (float) (i * 2.399 + tl.seed() % 7);
                float k = Math.min(1f, since / 1300f);
                float rr = 6f + 34f * (1f - k) * ((i * 37) % 10 / 10f + 0.4f);
                float x = screenX((float) Math.cos(a) * rr, 6f + 10f * k + (i % 5), (float) Math.sin(a) * rr);
                float y = screenY((float) Math.cos(a) * rr, 6f + 10f * k + (i % 5), (float) Math.sin(a) * rr);
                square(layer, x, y, G, ChopScene.alpha(0.8f * (1f - k) * ChopScene.sceneAlpha) | 0xF2EAD2);
            }
        }
        if (e == Ending.BUST && t >= WaterTimeline.B_SINK) {
            // The lily pads bob up round the big one: the guaranteed drop.
            for (int i = 0; i < 4; i++) {
                float a = 0.9f + i * 1.6f;
                float k = WaterTimeline.span(t, WaterTimeline.B_SINK + 300 + i * 120, 400);
                float x = screenX((float) Math.cos(a) * 16f, 0.5f, (float) Math.sin(a) * 16f);
                float y = screenY((float) Math.cos(a) * 16f, 0.5f, (float) Math.sin(a) * 16f) + (1f - k) * 6f;
                float sz = 4f * G;
                layer.fill(snap(x - sz / 2f), snap(y - sz / 4f), sz, sz / 2f, ChopScene.alpha(k * ChopScene.sceneAlpha) | 0x2E8A30);
                layer.fill(snap(x - sz / 2f), snap(y - sz / 4f), G, G, ChopScene.alpha(k * ChopScene.sceneAlpha) | 0x58C04A);
            }
        }
    }

    /** Snake Eyes: two gold slits open in the dark middle of the coil, blink once, and come together into the drop. */
    static void eyes(ChopScene.Canvas c, CrystalVeins.Fill layer, WaterTimeline tl, long t) {
        if (t < WaterTimeline.E_OPEN) return;
        float open = WaterTimeline.span(t, WaterTimeline.E_OPEN, 320);
        long b = t - WaterTimeline.E_BLINK;
        if (b >= 0 && b < 260) open *= b < 110 ? 1f - b / 110f : (b - 110) / 150f;
        float fuse = WaterTimeline.span(t, WaterTimeline.E_FUSE, 400);
        if (fuse >= 1f) return;
        float cx = screenX(0f, 9f, 0f), cy = screenY(0f, 9f, 0f);
        float gap = 14f * G * (1f - fuse);
        float E = 2f * G;
        float a = (1f - fuse) * ChopScene.sceneAlpha;
        ChopScene.glow(c, cx, cy, 44f * G * (0.6f + 0.4f * open), 0xF2C230, 0.45f * a * open);
        for (int side = -1; side <= 1; side += 2) {
            float ex = cx + side * gap / 2f;
            // An almond 7 wide, 5 tall at most, opened by rows from the middle; a black slit down it.
            int rows = Math.round(2.5f * open);
            for (int ry = -rows; ry <= rows; ry++) {
                int half = 3 - Math.abs(ry);
                for (int rx = -half; rx <= half; rx++) {
                    int col = rx == 0 ? 0x120A02 : (Math.abs(ry) == rows && rows > 0 ? 0xB8860A : 0xFFD43A);
                    if (rx == 0 && Math.abs(ry) == rows) col = 0xB8860A;
                    layer.fill(snap(ex + rx * E - E / 2f), snap(cy + ry * E - E / 2f), E, E, ChopScene.alpha(a) | col);
                }
            }
        }
    }

    // ================================================================== the Wiki Tiki

    static final float HEAD = 9f;
    static final int RING = 16;
    static final float RING_R = 36f;
    /** The three heads' turns off the camera, bottom up. */
    static final float[] HEAD_YAW = {-0.16f, 0.12f, -0.05f};
    /** The two Laser Totems: x, z. */
    static final float[][] LASER = {{-54f, -10f}, {54f, -10f}};
    /** Where the lasers start (the totems' eyes) and end this frame, world; and the top face's mouth. */
    static final float[][] BEAM = new float[2][6];
    static final float[] MOUTH = new float[3];

    static float headYaw(int k) {
        return (float) Math.toRadians(-CAM_YAW) + HEAD_YAW[k];
    }

    static void totem(WaterTimeline tl, long t, CrystalVeins.Fill back) {
        Ending e = tl.ending();
        float em = WaterTimeline.emerge(t);
        float rise = -64f * (1f - em);
        float tremble = WaterTimeline.tremble(t);
        float sink = 0f;
        if (e == Ending.BOOK) sink = 70f * WaterTimeline.span(t, WaterTimeline.K_UNDER, WaterTimeline.K_UNDER_TO - WaterTimeline.K_UNDER);
        if (e == Ending.FLOOD) sink = 64f * WaterTimeline.span(t, WaterTimeline.F_RESUME + 200, 1800);
        if (e == Ending.MASK) sink = 70f * WaterTimeline.span(t, tl.riseFrom(), 1400);
        float green = e == Ending.BOOK ? WaterTimeline.span(t, WaterTimeline.K_GLOW, 300) : 0f;
        // ---- the three heads
        for (int k = 0; k < 3; k++) {
            float y = HEAD + k * HEAD * 2f + rise - sink;
            float x = tremble * 0.8f * (float) Math.sin(t * 0.1 + k * 2.1);
            float yaw = headYaw(k), roll = 0f, pitch = 0f;
            float px = x, py = y, pz = 0f;
            if (e == Ending.BUST && t >= WaterTimeline.B_TOPPLE && k > 0) {
                // The top falls first, then the middle: each tips over its bottom edge and drops into the water.
                long from = WaterTimeline.B_TOPPLE + (2 - k) * 320L;
                float f = WaterTimeline.ease((t - from) / 650f);
                float dir = k == 2 ? 1f : -1f;
                roll = dir * 1.55f * f;
                float drop = f * f * (y + HEAD * 2f);
                px = x + dir * (HEAD * 1.3f + 10f * k) * f;
                py = y - drop;
                pz = 8f * f;
            }
            boolean peeled = e == Ending.MASK && k == 2 && t >= WaterTimeline.M_PEEL;
            int lit = faceLit(tl, t, k);
            int add = lit;
            if (green > 0f) add = ChopScene.alpha(0.3f * green) | 0x55FF55;
            // The Mask: once its face is off, the totem steps back into the dark behind it.
            int tint = 0xFFFFFF;
            if (e == Ending.MASK) tint = WaterTimeline.mix(0xFFFFFF, 0x202430, 0.7f * WaterTimeline.span(t, WaterTimeline.M_PEEL, 900));
            float[][] ax = SlayerScene.axes(yaw, pitch, roll);
            float[][] uv = headUv(false);
            if (peeled) uv[4] = null;
            SlayerScene.box(px, py, pz, HEAD, HEAD, HEAD, ax[0], ax[1], ax[2], uv, SLOT_SKIN, tint, add, POLYS);
            float hh = HEAD * 1.125f;
            float[][] hat = headUv(true);
            if (peeled) hat[4] = null;
            SlayerScene.box(px, py, pz, hh, hh, hh, ax[0], ax[1], ax[2], hat, SLOT_SKIN, tint, add, POLYS);
            if (k == 2) {
                MOUTH[0] = px + ax[2][0] * HEAD - ax[1][0] * HEAD * 0.45f;
                MOUTH[1] = py + ax[2][1] * HEAD - ax[1][1] * HEAD * 0.45f;
                MOUTH[2] = pz + ax[2][2] * HEAD - ax[1][2] * HEAD * 0.45f;
            }
        }
        if (e == Ending.MASK) mask(tl, t, rise);
        // ---- the compacting ring
        for (int j = 0; j < RING; j++) {
            double a = j * Math.PI * 2 / RING;
            float x = (float) (RING_R * Math.cos(a)), z = (float) (RING_R * Math.sin(a));
            float lv = tl.level(t - j * 24L);
            int slot = SLOT_TC + Math.max(0, Math.min(4, Math.round(lv)));
            float pop = WaterTimeline.pop(t - j * 24L);
            float y = -3f + (-30f) * (1f - WaterTimeline.ease((t - 250 - j * 40f) / 600f)) + 1.5f * pop;
            if (e == Ending.FLOOD) y -= 26f * WaterTimeline.span(t, WaterTimeline.F_RESUME, 1500);
            float[][] ax = SlayerScene.axes((float) -a, 0f, 0f);
            float[][] uv = {SlayerScene.WHOLE, SlayerScene.WHOLE, SlayerScene.WHOLE, null, SlayerScene.WHOLE, SlayerScene.WHOLE};
            SlayerScene.box(x, y, z, 7.5f, 7.5f, 7.5f, ax[0], ax[1], ax[2], uv, slot, 0xFFFFFF, ChopScene.alpha(0.25f * pop) | 0xFFFFFF, POLYS);
        }
        // ---- the Laser Totems
        float lu = WaterTimeline.ease((t - WaterTimeline.LASERS_FROM) / 500f);
        for (int s = 0; s < 2; s++) {
            float x = LASER[s][0], z = LASER[s][1];
            float down = -40f * (1f - lu);
            if (e == Ending.BUST) down -= 40f * WaterTimeline.span(t, WaterTimeline.B_CRACK + 200, 900);
            if (e == Ending.FLOOD) down -= 40f * WaterTimeline.span(t, WaterTimeline.F_RESUME, 1200);
            float[][] ax = SlayerScene.axes((float) Math.toRadians(-CAM_YAW) + (s == 0 ? 0.5f : -0.5f), 0f, 0f);
            float[][] side = {SlayerScene.WHOLE, SlayerScene.WHOLE, null, null, SlayerScene.WHOLE, SlayerScene.WHOLE};
            SlayerScene.box(x, 6f + down, z, 4.5f, 6f, 4.5f, ax[0], ax[1], ax[2], side, SLOT_LOG, 0xFFFFFF, 0, POLYS);
            SlayerScene.box(x, 18f + down, z, 4.5f, 6f, 4.5f, ax[0], ax[1], ax[2], side, SLOT_LOG, 0xFFFFFF, 0, POLYS);
            float hs = 5f;
            SlayerScene.box(x, 24f + hs + down, z, hs, hs, hs, ax[0], ax[1], ax[2], headUv(false), SLOT_SKIN, 0xC8C8C8, ChopScene.alpha(0.2f) | tl.colour(t), POLYS);
            BEAM[s][0] = x + ax[2][0] * hs;
            BEAM[s][1] = 24f + hs + down + 1f;
            BEAM[s][2] = z + ax[2][2] * hs;
        }
        originX = screenX(MOUTH[0], MOUTH[1], MOUTH[2]);
        originY = screenY(MOUTH[0], MOUTH[1], MOUTH[2]);
        if (e == Ending.BOOK || e == Ending.FLOOD) {
            originX = screenX(0f, 4f, 0f);
            originY = screenY(0f, 4f, 0f);
        }
    }

    /** Face {@code k}'s glow: off until it lights (FACES), then the tell's colour, pulsing on each step. ARGB, alpha = strength. */
    static int faceLit(WaterTimeline tl, long t, int k) {
        if (t < WaterTimeline.FACES[k]) return 0;
        float on = WaterTimeline.span(t, WaterTimeline.FACES[k], 250);
        float a = on * (0.08f + 0.16f * WaterTimeline.pop(t) + 0.1f * WaterTimeline.tremble(t));
        if (t >= WaterTimeline.REVEAL) a = on * (tl.drop().win() ? 0.16f : 0.1f * (1f - WaterTimeline.span(t, WaterTimeline.REVEAL, 500)));
        if (tl.ending() == Ending.MASK && t > WaterTimeline.M_PEEL) a *= 1f - WaterTimeline.span(t, WaterTimeline.M_PEEL, 600);
        return ChopScene.alpha(a) | tl.colour(t);
    }

    /** The Tiki Mask: the top face comes off the totem, turns square to the camera and grows. */
    static void mask(WaterTimeline tl, long t, float rise) {
        if (t < WaterTimeline.M_PEEL || t >= tl.riseFrom()) return;
        float peel = WaterTimeline.span(t, WaterTimeline.M_PEEL, WaterTimeline.M_PEEL_TO - WaterTimeline.M_PEEL);
        float turn = WaterTimeline.span(t, WaterTimeline.M_TURN, WaterTimeline.M_TURN_TO - WaterTimeline.M_TURN);
        float yaw = headYaw(2) + ((float) Math.toRadians(-CAM_YAW) - headYaw(2)) * turn;
        float pitch = 0.35f * turn;
        float[][] ax = SlayerScene.axes(yaw, pitch, 0.25f * (float) Math.sin(peel * Math.PI) * (1f - turn));
        float y = HEAD * 5f + rise + 14f * peel + 20f * turn;
        float out = HEAD + 16f * peel + 14f * turn;
        float x = ax[2][0] * out + 8f * turn, z = ax[2][2] * out;
        float s = HEAD * (1f + 0.9f * turn);
        float[][] uv = {null, null, null, null, faceRect(8, 8, false), null};
        SlayerScene.box(x, y, z, s, s, 0.9f, ax[0], ax[1], ax[2], uv, SLOT_SKIN, 0xFFFFFF, ChopScene.alpha(0.12f) | 0xFFD43A, POLYS);
        float[][] hat = {null, null, null, null, faceRect(40, 8, false), null};
        SlayerScene.box(x + ax[2][0] * 1.0f, y, z + ax[2][2] * 1.0f, s * 1.125f, s * 1.125f, 0.9f, ax[0], ax[1], ax[2], hat, SLOT_SKIN, 0xFFFFFF, 0, POLYS);
        MASK[0] = x + ax[2][0] * 2f;
        MASK[1] = y;
        MASK[2] = z + ax[2][2] * 2f;
        MASK[3] = s;
        System.arraycopy(ax[0], 0, MASK_X, 0, 3);
        System.arraycopy(ax[1], 0, MASK_Y, 0, 3);
    }

    static final float[] MASK = new float[4], MASK_X = new float[3], MASK_Y = new float[3];

    static void totemFront(ChopScene.Canvas c, CrystalVeins.Fill layer, WaterTimeline tl, long t) {
        Ending e = tl.ending();
        // ---- the lasers: from each totem's eye to a point sweeping up and down the totem, the tell's colour
        float on = WaterTimeline.ease((t - WaterTimeline.LASERS_FROM - 400) / 300f);
        float off = 1f;
        if (e == Ending.BUST) off = 1f - WaterTimeline.span(t, WaterTimeline.B_CRACK, 120);
        else if (e == Ending.BUBBLE) off = 1f - WaterTimeline.span(t, WaterTimeline.U_GROW, WaterTimeline.U_GROW_TO - WaterTimeline.U_GROW);
        else if (t >= WaterTimeline.REVEAL) off = 1f - WaterTimeline.span(t, WaterTimeline.REVEAL + 250, 300);
        if (e == Ending.FLOOD) off = Math.min(off, 1f - WaterTimeline.span(t, WaterTimeline.F_FREEZE, 1));
        float beam = on * off;
        if (beam > 0.02f) {
            int col = tl.colour(t);
            float sweep = 0.5f + 0.5f * (float) Math.sin(P * (0.0024 + 0.0035 * WaterTimeline.tighten(t)));
            for (int s = 0; s < 2; s++) {
                float ty = HEAD * 0.6f + sweep * HEAD * 4.6f, tx = 0f, tz = HEAD;
                if (s == 1) ty = HEAD * 5.2f - sweep * HEAD * 4.6f;
                // From the reveal the beams meet in the top face's mouth.
                float meet = t >= WaterTimeline.REVEAL ? WaterTimeline.span(t, WaterTimeline.REVEAL, 250) : 0f;
                tx += (MOUTH[0] - tx) * meet;
                ty += (MOUTH[1] - ty) * meet;
                tz += (MOUTH[2] - tz) * meet;
                laser(layer, BEAM[s][0], BEAM[s][1], BEAM[s][2], tx, ty, tz, col, beam, t);
            }
        }
        // ---- the bust's cracks, the bubble, the mask's eyes
        if (e == Ending.BUST && t >= WaterTimeline.B_CRACK) cracks(layer, tl, t);
        if (e == Ending.BUBBLE) bubble(c, layer, tl, t);
        if (e == Ending.MASK && t >= WaterTimeline.M_EYES && t < tl.riseFrom()) maskEyes(c, layer, t);
        if (e == Ending.BUST && t >= WaterTimeline.B_TOPPLE + 500) splash(layer, tl, t, WaterTimeline.B_TOPPLE + 500, 26f, 0f, 0xBFE8F4);
        if (e == Ending.BUST && t >= WaterTimeline.B_TOPPLE + 850) splash(layer, tl, t, WaterTimeline.B_TOPPLE + 850, -24f, 0f, 0xBFE8F4);
        if (e == Ending.BUST && t >= WaterTimeline.B_TOPPLE + 600) {
            // Clay balls and jungle wood bob up where the heads went in: the guaranteed drop.
            for (int i = 0; i < 9; i++) {
                float a = i * 0.7f;
                float r = 16f + (i * 13 % 7) * 3f;
                float k = WaterTimeline.span(t, WaterTimeline.B_TOPPLE + 600 + i * 60, 350);
                float wx = (float) Math.cos(a) * r, wz = (float) Math.sin(a) * r * 0.6f + 6f;
                float x = screenX(wx, 0.8f, wz), y = screenY(wx, 0.8f, wz) + (1f - k) * 5f + (float) Math.sin(P * 0.005 + i) * 0.8f;
                int col = i % 3 == 2 ? 0x6B4F2A : 0xA4A8B8;
                square(layer, x, y, 2 * G, ChopScene.alpha(k * ChopScene.sceneAlpha) | col);
            }
        }
    }

    /** A laser beam of squares, a bright core and a coloured edge, with sparks where it lands. */
    static void laser(CrystalVeins.Fill layer, float x0, float y0, float z0, float x1, float y1, float z1, int col, float a, long t) {
        float sx = screenX(x0, y0, z0), sy = screenY(x0, y0, z0), ex = screenX(x1, y1, z1), ey = screenY(x1, y1, z1);
        float len = (float) Math.hypot(ex - sx, ey - sy);
        int n = Math.max(2, Math.round(len / G));
        float alpha = a * ChopScene.sceneAlpha;
        int core = WaterTimeline.mix(col, 0xFFFFFF, 0.6f);
        for (int i = 0; i <= n; i++) {
            float k = i / (float) n;
            float x = sx + (ex - sx) * k, y = sy + (ey - sy) * k;
            float flick = 0.75f + 0.25f * (float) Math.sin(t * 0.05 + i * 0.9);
            square(layer, x, y + G, G, ChopScene.alpha(alpha * 0.55f * flick) | col);
            square(layer, x, y, G, ChopScene.alpha(alpha * flick) | core);
        }
        for (int i = 0; i < 4; i++) {
            long ph = (t + i * 83L) % 330;
            float k = ph / 330f;
            float ang = (float) (i * 1.9 + (t / 330) * 0.7);
            square(layer, ex + (float) Math.cos(ang) * k * 6f * G, ey + (float) Math.sin(ang) * k * 6f * G, G, ChopScene.alpha(alpha * (1f - k)) | core);
        }
    }

    /** A bust's cracks: jagged dark lines spreading over each head's front from the middle. */
    static void cracks(CrystalVeins.Fill layer, WaterTimeline tl, long t) {
        float k = WaterTimeline.span(t, WaterTimeline.B_CRACK, 260);
        for (int h = 0; h < 3; h++) {
            if (h > 0 && t >= WaterTimeline.B_TOPPLE + (2 - h) * 320L) continue;
            float yaw = headYaw(h);
            float fxw = (float) Math.sin(yaw) * HEAD, fzw = (float) Math.cos(yaw) * HEAD;
            float cx = screenX(fxw, HEAD + h * HEAD * 2f, fzw), cy = screenY(fxw, HEAD + h * HEAD * 2f, fzw);
            java.util.Random r = new java.util.Random(tl.seed() * 31 + h);
            for (int arm = 0; arm < 4; arm++) {
                float x = cx, y = cy;
                double dir = arm * Math.PI / 2 + r.nextDouble() * 0.8;
                int steps = Math.round(7 * k);
                for (int s = 0; s < steps; s++) {
                    dir += (r.nextDouble() - 0.5) * 1.1;
                    x += (float) Math.cos(dir) * G;
                    y += (float) Math.sin(dir) * G;
                    layer.fill(snap(x), snap(y), G, G, ChopScene.alpha(0.9f * ChopScene.sceneAlpha) | 0x1A0E08);
                }
            }
        }
    }

    /** The Troubled Bubble: forming in the top face's mouth as it swallows the beams, then floating free. */
    static void bubble(ChopScene.Canvas c, CrystalVeins.Fill layer, WaterTimeline tl, long t) {
        if (t < WaterTimeline.U_GROW || t >= tl.riseFrom() + 60) return;
        float grow = WaterTimeline.span(t, WaterTimeline.U_GROW, WaterTimeline.U_GROW_TO - WaterTimeline.U_GROW);
        float x = screenX(MOUTH[0], MOUTH[1], MOUTH[2] + 2f), y = screenY(MOUTH[0], MOUTH[1], MOUTH[2] + 2f);
        float r = (2f + 9f * grow) * G * (1f + 0.04f * (float) Math.sin(t * 0.03));
        float a = ChopScene.sceneAlpha;
        int col = tl.colour(t);
        ChopScene.glow(c, x, y, r * 4f, col, 0.35f * a);
        int n = Math.max(12, Math.round(r * 6.3f / G));
        for (int i = 0; i < n; i++) {
            double ang = i * Math.PI * 2 / n;
            square(layer, x + (float) Math.cos(ang) * r, y + (float) Math.sin(ang) * r, G, ChopScene.alpha(0.85f * a) | 0xE8F8FF);
        }
        // Inside: the swallowed light swirling.
        for (int i = 0; i < 10; i++) {
            double ang = i * 0.63 + t * 0.006;
            float rr = r * (0.25f + 0.5f * ((i * 7) % 5) / 5f);
            square(layer, x + (float) Math.cos(ang) * rr, y + (float) Math.sin(ang) * rr * 0.8f, G, ChopScene.alpha(0.6f * a) | col);
        }
        // The shine.
        square(layer, x - r * 0.45f, y - r * 0.5f, 2 * G, ChopScene.alpha(0.9f * a) | 0xFFFFFF);
        originX = x;
        originY = y;
        originSize = r * 1.6f;
    }

    /** The mask's eyes lighting gold. */
    static void maskEyes(ChopScene.Canvas c, CrystalVeins.Fill layer, long t) {
        float k = WaterTimeline.span(t, WaterTimeline.M_EYES, 200);
        for (int side = -1; side <= 1; side += 2) {
            float ox = MASK[0] + MASK_X[0] * side * MASK[3] * 0.45f + MASK_Y[0] * MASK[3] * 0.1f;
            float oy = MASK[1] + MASK_X[1] * side * MASK[3] * 0.45f + MASK_Y[1] * MASK[3] * 0.1f;
            float oz = MASK[2] + MASK_X[2] * side * MASK[3] * 0.45f + MASK_Y[2] * MASK[3] * 0.1f;
            float x = screenX(ox, oy, oz), y = screenY(ox, oy, oz);
            ChopScene.glow(c, x, y, 34f * G, 0xFFC830, 0.6f * k);
            layer.fill(snap(x - 2 * G), snap(y - G), 4 * G, 2 * G, ChopScene.alpha(k * ChopScene.sceneAlpha) | 0xFFE680);
            layer.fill(snap(x - G), snap(y - G / 2f), 2 * G, G, ChopScene.alpha(k * ChopScene.sceneAlpha) | 0xFFFFFF);
        }
        originX = screenX(MASK[0], MASK[1], MASK[2]);
        originY = screenY(MASK[0], MASK[1], MASK[2]);
        originSize = MASK[3] * 2f * ChopScene.size;
    }

    /** A splash: squares thrown up and out from a point on the water, falling back. */
    static void splash(CrystalVeins.Fill layer, WaterTimeline tl, long t, long at, float wx, float wz, int col) {
        long since = t - at;
        if (since < 0 || since > 900) return;
        float k = since / 900f;
        int n = Math.round(18 * Punch.now);
        for (int i = 0; i < n; i++) {
            double a = i * 2.399;
            float sp = 8f + (i * 17 % 9) * 2.2f;
            float h = 26f + (i * 11 % 7) * 4f;
            float x = wx + (float) Math.cos(a) * sp * k, z = wz + (float) Math.sin(a) * sp * k * 0.6f;
            float y = h * k - 50f * k * k;
            if (y < -1f) continue;
            square(layer, screenX(x, y, z), screenY(x, y, z), G, ChopScene.alpha(0.9f * (1f - k) * ChopScene.sceneAlpha) | col);
        }
    }

    // ================================================================== the dye: "The Bayou Floods"

    /** The dye's water climbing what is left standing; the geyser; drops streaming off the column. */
    static void flood(ChopScene.Canvas c, CrystalVeins.Fill layer, WaterTimeline tl, long t) {
        if (tl.ending() != Ending.FLOOD) return;
        float a = ChopScene.sceneAlpha;
        // The heartbeats in the stopped world: a ring of light from the middle each time.
        for (long at : WaterTimeline.F_HEARTS) {
            long since = t - at;
            if (since < 0 || since > 700) continue;
            float k = since / 700f;
            float r = (10f + 70f * k);
            int n = Math.round(r * 0.9f);
            for (int i = 0; i < n; i++) {
                double ang = i * Math.PI * 2 / n;
                float x = (float) Math.cos(ang) * r, z = (float) Math.sin(ang) * r;
                square(layer, screenX(x, 0.5f, z), screenY(x, 0.5f, z), G, ChopScene.alpha(0.7f * (1f - k) * a) | WaterTimeline.AQUAMARINE);
            }
        }
        float rear = tl.rear(t);
        if (rear <= 0f) return;
        // Water pouring off the column, and (the totem's) a geyser up the middle.
        boolean boa = tl.creature() == Creature.TITANOBOA;
        float top = boa ? 6f + SEGMENTS * COLUMN_STEP * rear : 120f * rear;
        int n = Math.round(60 * Punch.now);
        for (int i = 0; i < n; i++) {
            long ph = (P + i * 97L) % 1100;
            float k = ph / 1100f;
            double ang = i * 2.399;
            float rr = boa ? 9f + 4f * k : 4f + 10f * k;
            float y = boa ? top * (1f - k) : top * k * (1f - 0.15f * k);
            float x = (float) Math.cos(ang) * rr, z = (float) Math.sin(ang) * rr;
            int col = (i & 3) == 0 ? 0xFFFFFF : WaterTimeline.AQUAMARINE;
            square(layer, screenX(x, y, z), screenY(x, y, z), (i % 5 == 0 ? 2 : 1) * G, ChopScene.alpha(0.85f * a * Math.min(1f, rear * 2f)) | col);
        }
        if (!boa) {
            float x = screenX(0f, top, 0f), y = screenY(0f, top, 0f);
            originX = x;
            originY = y;
            ChopScene.glow(c, x, y, 70f * G, WaterTimeline.AQUAMARINE, 0.5f * rear * a);
        } else {
            ChopScene.glow(c, originX, originY, 60f * G, WaterTimeline.AQUAMARINE, 0.45f * rear * a);
        }
    }

    // ================================================================== the 3D layer

    static void rasterise(ChopScene.Canvas c) {
        List<SlayerScene.Poly> polys = POLYS;
        if (polys.isEmpty()) return;
        ModelRaster r = RASTER;
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        float[][] scr = new float[polys.size()][];
        for (int i = 0; i < polys.size(); i++) {
            SlayerScene.Poly p = polys.get(i);
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
        float ink = Math.max(1.5f, ChopScene.size * 0.5f);
        float pad = ink + 2f;
        float grey = TL.grey(T), flood = TL.flood(T);
        r.heights = flood > 0f;
        if (!r.begin(Math.max(0f, minX - pad), Math.max(0f, minY - pad), Math.min(W, maxX + pad), Math.min(H, maxY + pad), rasterScale)) return;
        r.cutBelow = 0f;
        for (int i = 0; i < polys.size(); i++) {
            SlayerScene.Poly p = polys.get(i);
            if (p.normal != null && SlayerScene.facesAway(p)) continue;
            if (!r.has(p.slot)) continue;
            float[] o = scr[i];
            r.face(o[0], o[1], o[2] - o[0], o[3] - o[1], o[4] - o[0], o[5] - o[1],
                    o[6], o[7] - o[6], o[8] - o[6], o[9], o[10] - o[9], o[11] - o[9],
                    p.uv[0], p.uv[1], p.uv[2], p.uv[3], p.slot, p.lit, p.tint, p.add, false);
        }
        r.cutBelow = Float.NaN;
        if (grey > 0f || flood > 0f) greyAndFlood(r, grey, flood);
        r.ink(Math.max(1, Math.round(ink * r.scale)));
        r.fade(ChopScene.sceneAlpha);
        c.image(TEX_RASTER, r.px, r.w, r.h, r.x, r.y, r.w / r.scale, r.h / r.scale);
        r.heights = false;
    }

    /** The stopped world greys; under the dye's rising water every pixel takes aquamarine by its own brightness. */
    static void greyAndFlood(ModelRaster r, float grey, float flood) {
        boolean boa = TL.creature() == Creature.TITANOBOA;
        // Up to the top of what stands: the coil's head, the totem's top; the column as it rears.
        float topY = boa ? 20f + SEGMENTS * COLUMN_STEP * TL.rear(T) : HEAD * 6f + 4f;
        float level = -2f + flood * (topY + 6f);
        int ar = WaterTimeline.AQUAMARINE >> 16 & 255, ag = WaterTimeline.AQUAMARINE >> 8 & 255, ab = WaterTimeline.AQUAMARINE & 255;
        for (int yy = 0; yy < r.h; yy++) {
            for (int xx = 0; xx < r.w; xx++) {
                int i = yy * r.w + xx;
                int c = r.px[i];
                if (c >>> 24 == 0) continue;
                int cr = c >> 16 & 255, cg = c >> 8 & 255, cb = c & 255;
                int lum = (cr * 77 + cg * 150 + cb * 29) >> 8;
                float wy = r.heights && i < r.wy.length ? r.wy[i] : Float.NaN;
                // The front climbs texel by texel: a ragged edge, a column a texel wide.
                float wob = 1.6f * (float) Math.sin((xx / r.scale) * 0.9f) + 1.1f * (float) Math.cos((xx / r.scale) * 0.37f);
                if (!Float.isNaN(wy) && flood > 0f && wy < level + wob) {
                    float k = 0.3f + lum / 255f * 1.0f;
                    boolean rim = wy > level + wob - 1.2f;
                    cr = Math.min(255, (int) (ar * k * (rim ? 1.25f : 0.9f)));
                    cg = Math.min(255, (int) (ag * k * (rim ? 1.15f : 1f)));
                    cb = Math.min(255, (int) (ab * k * (rim ? 1.15f : 1f)));
                } else if (grey > 0f) {
                    int gl = (int) (lum * 0.9f);
                    cr += (int) ((gl - cr) * grey);
                    cg += (int) ((gl - cg) * grey);
                    cb += (int) ((gl + 6 - cb) * grey);
                }
                r.px[i] = 0xFF000000 | Math.max(0, Math.min(255, cr)) << 16 | Math.max(0, Math.min(255, cg)) << 8 | Math.max(0, Math.min(255, cb));
            }
        }
    }

    /** A head's six faces on a 64 x 64 skin, {+X, -X, +Y, -Y, +Z, -Z}, the front on +Z (base, or the hat at +32). */
    static float[][] headUv(boolean hat) {
        int o = hat ? 32 : 0;
        return new float[][]{
                faceRect(16 + o, 8, true),   // +X: the head's own left
                faceRect(0 + o, 8, false),   // -X: its right
                flipV(faceRect(8 + o, 0, false)), // top, its back edge away from the camera
                faceRect(16 + o, 0, false),  // bottom
                faceRect(8 + o, 8, false),   // front
                faceRect(24 + o, 8, true),   // back
        };
    }

    /** An 8 x 8 face of a 64 x 64 skin, normalised; {@code flip} runs it the other way across. */
    static float[] faceRect(int u, int v, boolean flip) {
        float e = 0.001f;
        return flip ? new float[]{(u + 8 - e) / 64f, v / 64f, u / 64f, (v + 8) / 64f} : new float[]{u / 64f, v / 64f, (u + 8) / 64f, (v + 8) / 64f};
    }

    static float[] flipV(float[] r) {
        return new float[]{r[0], r[3] - 0.001f / 64f, r[2], r[1]};
    }

    // ================================================================== the drop and the showcase

    static float restX() {
        return TL.drop().ending == Ending.FLOOD && TL.creature() == Creature.TITANOBOA ? originX : W / 2f;
    }

    static float restY() {
        return TL.drop().ending == Ending.FLOOD && TL.creature() == Creature.TITANOBOA ? Math.max(H * 0.36f, Math.min(originY, H * 0.42f)) : H * 0.4f;
    }

    static float restSize() {
        return H * (TL.drop().ending == Ending.FLOOD ? 0.3f : TL.banner() ? 0.24f : 0.2f);
    }

    static void showcase(ChopScene.Canvas c, WaterTimeline tl, long t) {
        float ts = Math.min(1.35f, H / 300f);
        Drop d = tl.drop();
        if (!d.win()) {
            // A bust names the guaranteed drop over where it floats: no rays, no banner, its price uncounted.
            float na = tl.nameAlpha(t) * ChopScene.sceneAlpha;
            if (na <= 0.02f) return;
            float x = screenX(0f, 4f, 0f), y = screenY(0f, 4f, 0f);
            if (tl.creature().guaranteedIcon != null) c.item(x, y - 26f * ts, 28f * ts, na, 0f);
            c.name(x, y + 14f * ts, 1.25f * ts, ChopScene.alpha(na) | 0x55FF55, 999);
            long price = tl.price();
            if (price > 0 && t >= tl.countStart()) c.text(String.format("%,d coins", price), x, y + 27f * ts, ts, ChopScene.alpha(na) | 0xFFD84A);
            return;
        }
        float rise = tl.rise(t);
        if (rise <= 0f && t < tl.riseFrom()) return;
        float x = originX + (restX() - originX) * rise;
        float y = originY + (restY() - originY) * rise;
        float size = originSize + (restSize() - originSize) * rise;
        float show = WaterTimeline.ease((t - tl.arrive() + 400) / 500f);
        if (show > 0f) rays(c, tl, t, x, y, size, show);
        y += 2f * (float) Math.sin(t / 420.0) * show;
        float yaw = 30f + 12f * (float) Math.sin(t / 650.0);
        c.item(x, y, size, ChopScene.sceneAlpha * Math.min(1f, 0.3f + rise), yaw);
        banner(c, tl, t, x, y - size * 0.62f - 12f * ts);
        labels(c, tl, t, x, y + size * 0.62f);
    }

    /** The win's light: stepped pixel rays in the drop's colour out of the item, and motes. */
    static void rays(ChopScene.Canvas c, WaterTimeline tl, long t, float x, float y, float size, float show) {
        int col = tl.drop().ending == Ending.FLOOD ? WaterTimeline.AQUAMARINE : tl.landed();
        ChopScene.glow(c, x, y, size * 3.2f, col, 0.4f * show);
        int n = 8;
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n + t * 0.0004;
            float len = size * (0.9f + 0.35f * (i % 2));
            for (int s = 0; s < 7; s++) {
                float k = s / 7f;
                float rr = size * 0.5f + len * k;
                float wdt = G * (1 + Math.round(2.5f * k));
                float px = x + (float) Math.cos(a) * rr, py = y + (float) Math.sin(a) * rr;
                c.fill(snap(px - wdt / 2f), snap(py - wdt / 2f), wdt, wdt, ChopScene.alpha(0.32f * show * (1f - k) * ChopScene.sceneAlpha) | col);
            }
        }
    }

    static void banner(ChopScene.Canvas c, WaterTimeline tl, long t, float x, float y) {
        if (!showBanner) return;
        float k = tl.bannerStamp(t);
        if (k <= 0f) return;
        float ts = Math.min(1.35f, H / 300f);
        float scale = ts * (1.4f + 1.3f * (1f - WaterTimeline.ease(k)));
        float a = Math.min(1f, k * 2.5f) * ChopScene.sceneAlpha;
        int rgb = tl.bannerColour();
        if (k < 1f) ChopScene.glow(c, x, y, 160f * ts, rgb, 0.6f * (1f - k));
        c.text(tl.bannerText(), x, y, scale, ChopScene.alpha(a) | rgb);
    }

    static void labels(ChopScene.Canvas c, WaterTimeline tl, long t, float x, float below) {
        float na = tl.nameAlpha(t) * ChopScene.sceneAlpha;
        float ts = Math.min(1.35f, H / 300f);
        Drop d = tl.drop();
        if (na > 0.02f) c.name(x, below + 8f * ts, 1.6f * ts, ChopScene.alpha(na) | d.colour, tl.letters(t));
        float la = tl.lineAlpha(t) * ChopScene.sceneAlpha;
        if (la <= 0.02f) return;
        String odds = "1 in ~" + String.format("%,d", d.oneIn) + " " + tl.creature().title + (tl.creature() == Creature.WIKI_TIKI ? "s" : "s");
        String line = nthNote == null ? odds : odds + " · " + nthNote;
        c.text(line, x, below + 22f * ts, ts, ChopScene.alpha(la) | 0xC8C8C8);
        long price = tl.price();
        if (price > 0 && t >= tl.countStart()) {
            float k = Math.min(1f, (t - tl.countStart()) / (float) WaterTimeline.COUNT_MS);
            long shown = Math.round(price * (1f - (1f - k) * (1f - k) * (1f - k)));
            c.text(String.format("%,d coins", shown), x, below + 35f * ts, 1.25f * ts, ChopScene.alpha(la) | 0xFFD84A);
            if (tl.priceSource() != null) c.text(tl.priceSource(), x, below + 45f * ts, 0.7f * ts, ChopScene.alpha(la) | 0x9A9A9A);
        }
    }

    // ================================================================== helpers

    static float screenX(float x, float y, float z) {
        ChopScene.project(x, y, z);
        return ChopScene.P[0];
    }

    static float screenY(float x, float y, float z) {
        ChopScene.project(x, y, z);
        return ChopScene.P[1];
    }

    static float snap(float v) {
        return Math.round(v / G) * G;
    }

    static void square(CrystalVeins.Fill layer, float x, float y, float s, int argb) {
        if ((argb >>> 24) < 3 || s <= 0f) return;
        float gx = Math.round(x / G) * G, gy = Math.round(y / G) * G;
        float sz = Math.max(G, Math.round(s / G) * G);
        layer.fill(gx - sz / 2f, gy - sz / 2f, sz, sz, argb);
    }

    static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
