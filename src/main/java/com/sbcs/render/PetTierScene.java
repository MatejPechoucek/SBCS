package com.sbcs.render;

import com.sbcs.anim.PetTierTimeline;
import com.sbcs.anim.PetTierTimeline.Tier;
import com.sbcs.render.art.ChopArt;

import java.util.Random;

import static com.sbcs.anim.PetTierTimeline.*;

/**
 * Draws the Scatha pet's reveal ({@link PetTierTimeline}) onto a {@link ChopScene.Canvas} - one routine for the
 * cleaver cutscene (ScopeMinigameScreen) and the offline preview (tools/PetTierPreview). Pure: no Minecraft.
 *
 * The head and the worm's body segments are boxes through ChopScene's renderer with the worm's own skins
 * ({@code c.dropSkin()} is the head's, {@code c.bestiarySkin(BODY_SKIN)} the body's - captured off a live Scatha in
 * game, ScathaArt's placeholders offline), base + hat as every head is, outlined by an ink pass. Every effect is
 * pixel squares on the {@code px} layer (one image in game, PixelLayer), flushed by {@code flush} before the boxes
 * so they sit behind the head.
 *
 * Nothing drawn before {@link PetTierTimeline#LAND} reads the tier: PetTierTest hashes it for all three.
 */
public final class PetTierScene {
    private PetTierScene() {}

    /** The key the body's skin is asked for under ({@link ChopScene.Canvas#bestiarySkin}). */
    public static final String BODY_SKIN = "scatha_body";

    /** How tall the head ends up, as a share of the screen's height. */
    public static final float HEAD_FRAC = 0.34f;
    /** The camera looks down on the head a little. */
    private static final float PITCH = -12f;

    /**
     * One frame, {@code t} ms after the head was cut free.
     *
     * @param ox     where the head was on screen when the last cut landed
     * @param oy     the same
     * @param from   its size there, in GUI pixels
     * @param flush  draws the pixel layer (null offline, where {@code px} draws straight onto the canvas)
     */
    public static void draw(ChopScene.Canvas c, CrystalVeins.Fill px, Runnable flush, PetTierTimeline tl, long t,
                            int w, int h, float ox, float oy, float from) {
        ChopScene.lightCount = 0;
        ChopScene.gild = null;
        ChopScene.glintHook = null;
        ChopScene.insideOut = false;
        ChopScene.yShift = 0f;
        ChopScene.sceneAlpha = 1f;
        ChopScene.inkWidth = 0f;

        float cx = w / 2f, cy = h * 0.46f;
        int cell = CrystalVeins.unitFor(h);
        float full = h * HEAD_FRAC;
        float travel = easeOutCubic(t / (float) RISE);
        float shrink = easeIn(tl.shrink(t));
        float hx = lerp(ox, cx, travel), hy = lerp(oy, cy, travel) - (float) Math.sin(travel * Math.PI) * h * 0.06f;
        float hs = lerp(from, full, travel);

        // The tremble through the cycle - the same every run - and, for a Legendary, harder up to the burst.
        float shake = tremble(t);
        Tier tier = t >= LAND ? tl.tier() : null;
        long e = t - LAND;
        if (tier == Tier.LEGENDARY && e >= 0 && e < L_BURST) {
            shake = 0.15f + 0.85f * clamp((e - L_CRACK) / (float) (L_BURST - L_CRACK));
            // The coil: the head draws in a little so the ring has room.
            hs *= 1f - 0.18f * ease(clamp((e - L_COIL) / 600f)) * (1f - ease(clamp((e - L_BURST + 200) / 300f)));
        }
        hx += shake * h * 0.007f * (float) Math.sin(t / 23.0);
        hy += shake * h * 0.005f * (float) Math.cos(t / 31.0);
        hs *= 1f - shrink;

        // ---- behind the head: glow, rays, every pixel effect
        int col = tl.colour(t);
        float steps = stepPulse(t);
        float glowA = travel * (0.45f + 0.35f * shake + 0.25f * steps);
        if (tier != null) glowA = afterGlow(tl, e);
        ChopScene.glow(c, hx, hy, hs * (2.5f + 0.35f * steps), col, glowA * (1f - shrink));

        if (tier != null) rays(c, tl, e, hx, hy, hs, shrink);

        orbit(px, tl, t, hx, hy, hs, cell, col);
        if (tier == Tier.RARE) burst(px, e, hx, hy, hs, cell, tl.tier().rgb, 1);
        if (tier == Tier.EPIC) {
            burst(px, e, hx, hy, hs, cell, Tier.EPIC.rgb, 2);
            shards(px, tl, e, hx, hy, w, h, cell);
        }
        if (tier == Tier.LEGENDARY) hollows(c, px, tl, e, hx, hy, hs, w, h, cell);
        if (flush != null) flush.run();

        // ---- the worm's body, coiled round the head (Legendary)
        if (tier == Tier.LEGENDARY) coil(c, tl, e, hx, hy, full, w);

        // ---- the head
        float yaw = headYaw(tl, t);
        if (hs >= 1f) {
            float flash = tier == null ? 0.18f * steps * travel : landFlash(tl, e);
            head(c, c.dropSkin(), hx, hy, hs, yaw, 0f, flash, 1f, 1f, 1f, w);
        }

        // ---- the card
        if (t >= tl.cardFrom()) card(c, tl, t, cx, cy, full, shrink, h);
        ChopScene.sceneAlpha = 1f;
    }

    // ================================================================== the head

    /** The head's turn: spun out of the cut, a slow sway through the cycle, then per tier. */
    static float headYaw(PetTierTimeline tl, long t) {
        float yaw = 25f + 720f * (1f - easeOutCubic(t / (float) RISE));
        yaw += 10f * (float) Math.sin(Math.max(0, t - RISE) / 420.0);
        if (t < LAND) return yaw;
        long e = t - LAND;
        return switch (tl.tier()) {
            case RARE -> yaw;
            case EPIC -> yaw + 360f * easeOutCubic(e / (float) E_SPIN);
            case LEGENDARY -> yaw + 720f * easeOutCubic((e - L_BURST) / 900f);
        };
    }

    /** A box head with base and hat, outlined; {@code size} is its height on screen. */
    static void head(ChopScene.Canvas c, String skin, float x, float y, float size, float yawDeg, float rollDeg,
                     float glow, float r, float g, float b, int w) {
        if (skin == null || size < 1f) return;
        setCamera(x, y, size / 9f);
        float ay = (float) Math.toRadians(yawDeg), az = (float) Math.toRadians(rollDeg);
        String[] tex = {skin, skin, skin, skin, skin, skin};
        for (int pass = 0; pass < 2; pass++) {
            ChopScene.inkWidth = pass == 0 ? Math.max(1.5f, w / 640f) : 0f;
            ChopScene.box(0f, 0f, 0f, 0f, ay, az, 4f, 4f, 4f, tex, ChopScene.HEAD, r, g, b, glow, 1f, c);
            ChopScene.box(0f, 0f, 0f, 0f, ay, az, 4.5f, 4.5f, 4.5f, tex, ChopScene.HAT, r, g, b, glow, 1f, c);
        }
        ChopScene.inkWidth = 0f;
    }

    /** An orthographic camera centred on (x, y): yaw 0, a little from above, {@code perUnit} GUI px a unit. */
    static void setCamera(float x, float y, float perUnit) {
        double pitch = Math.toRadians(PITCH);
        ChopScene.cy = 1f;
        ChopScene.sy = 0f;
        ChopScene.cp = (float) Math.cos(pitch);
        ChopScene.sp = (float) Math.sin(pitch);
        ChopScene.size = perUnit;
        ChopScene.fx = 0f;
        ChopScene.fy = 0f;
        ChopScene.fz = 0f;
        ChopScene.sx0 = x;
        ChopScene.sy0 = y;
    }

    /** A kick of light on every cycle step, fading over 120 ms. */
    static float stepPulse(long t) {
        int i = step(t);
        if (i < 0 || i >= STEPS.length) return 0f;
        return 1f - clamp((t - STEPS[i]) / 120f);
    }

    /** The landing's white-hot flash on the head itself, per tier. */
    static float landFlash(PetTierTimeline tl, long e) {
        float land = 0.55f * (1f - clamp(e / 260f));
        if (tl.tier() != Tier.LEGENDARY) return land;
        float burst = e >= L_BURST ? 0.8f * (1f - clamp((e - L_BURST) / 420f)) : 0f;
        // The pours light it up as they arrive.
        float pour = 0f;
        for (int i = 0; i < 6; i++) {
            long arrive = L_CRACK + i * L_CRACK_STEP + L_POUR_AFTER + L_POUR_MS;
            if (e >= arrive && e < L_RING) pour = Math.max(pour, 0.22f * (1f - clamp((e - arrive) / 200f)));
        }
        return Math.max(land, Math.max(burst, pour));
    }

    // ================================================================== light

    static float afterGlow(PetTierTimeline tl, long e) {
        return switch (tl.tier()) {
            case RARE -> 0.75f + 0.25f * (1f - clamp(e / 300f));
            case EPIC -> 0.85f + 0.4f * (1f - clamp(e / 400f));
            case LEGENDARY -> {
                // The heartbeat hold: the light throbs, sinks while the cracks pour, and blazes at the burst.
                float beat = beat(e) + beat(e - (L_BEAT2 - 0));
                float base = e < L_BURST ? 0.55f : 1f + 0.6f * (1f - clamp((e - L_BURST) / 600f));
                yield base + 0.4f * beat;
            }
        };
    }

    /** A lub-dub of light at e = 0. */
    static float beat(long e) {
        if (e < 0 || e > 600) return 0f;
        return (1f - clamp(e / 180f)) + 0.6f * (e >= 210 ? 1f - clamp((e - 210) / 180f) : 0f);
    }

    /** The game's rays texture behind the head, turning, in the pet's colour; bigger for the better pets. */
    static void rays(ChopScene.Canvas c, PetTierTimeline tl, long e, float x, float y, float hs, float shrink) {
        Tier tier = tl.tier();
        long from = tier == Tier.LEGENDARY ? L_BURST : 0;
        float a = clamp((e - from) / 300f) * (1f - clamp(shrink * 2f));
        if (a <= 0.02f) return;
        float big = tier == Tier.RARE ? 3.0f : tier == Tier.EPIC ? 3.8f : 4.8f;
        float speed = tier == Tier.LEGENDARY ? 45f : 60f;
        for (int layer = 0; layer < 2; layer++) {
            float size = hs * (layer == 0 ? big : big * 0.66f);
            float turn = (layer == 0 ? 1f : -1.6f) * (e - from) / speed;
            int rgb = layer == 0 ? tier.rgb : 0xFFFFFF;
            float la = a * (layer == 0 ? 0.6f : 0.3f);
            c.sprite(ChopArt.TEX_RAYS, x, y, size, size, turn, ChopScene.alpha(la) | rgb);
        }
    }

    // ================================================================== pixel effects

    /** Ten sparks circling the head in the cycle's colour; at the landing they fly out (or freeze, for a Legendary). */
    static void orbit(CrystalVeins.Fill px, PetTierTimeline tl, long t, float x, float y, float hs, int cell, int col) {
        float in = clamp((t - RISE) / 250f);
        if (in <= 0f) return;
        long held = Math.min(t, LAND);
        float out = 0f, a = in;
        if (t >= LAND) {
            long e = t - LAND;
            if (tl.tier() == Tier.LEGENDARY) a *= 1f - clamp(e / 500f);
            else {
                out = easeOutCubic(e / 700f);
                a *= 1f - clamp(e / 700f);
                held = LAND;
            }
            if (tl.tier() == Tier.LEGENDARY) held = LAND;
        }
        if (a <= 0.02f) return;
        for (int i = 0; i < 10; i++) {
            double ang = held / 380.0 + i * Math.PI * 2 / 10;
            float r = hs * (0.85f + 0.05f * (float) Math.sin(held / 90.0 + i)) + out * hs * 2.4f;
            float sx = x + (float) Math.cos(ang) * r, sy = y + (float) Math.sin(ang) * r * 0.8f;
            sq(px, sx, sy, cell * 2, ChopScene.alpha(a) | col);
            sq(px, sx - (float) Math.sin(ang) * cell * 2, sy + (float) Math.cos(ang) * cell * 1.6f, cell, ChopScene.alpha(a * 0.5f) | col);
        }
    }

    /** {@code rings} shockwaves of touching squares, out from the head. */
    static void burst(CrystalVeins.Fill px, long e, float x, float y, float hs, int cell, int rgb, int rings) {
        for (int k = 0; k < rings; k++) {
            float p = clamp((e - k * E_RINGS) / 520f);
            if (p <= 0f || p >= 1f) continue;
            ring(px, x, y, hs * (0.55f + 2.2f * easeOutCubic(p)), cell, ChopScene.alpha(0.9f * (1f - p)) | mixWhite(rgb, 0.35f * (1f - p)));
        }
    }

    /** A ring of squares, one cell apart. */
    static void ring(CrystalVeins.Fill px, float x, float y, float r, int cell, int argb) {
        int n = Math.max(12, Math.round((float) (Math.PI * 2 * r) / (cell * 1.2f)));
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            sq(px, x + (float) Math.cos(a) * r, y + (float) Math.sin(a) * r * 0.85f, cell, argb);
        }
    }

    /** The Epic's crystal shards: out of the head, past the screen's edges. */
    static void shards(CrystalVeins.Fill px, PetTierTimeline tl, long e, float x, float y, int w, int h, int cell) {
        if (e < 0 || e > 1400) return;
        Random r = new Random(tl.seed() * 31 + 7);
        float reach = (float) Math.hypot(w, h) * 0.75f;
        for (int i = 0; i < 34; i++) {
            double ang = r.nextDouble() * Math.PI * 2;
            float speed = 0.7f + 0.6f * r.nextFloat();
            long delay = r.nextInt(160);
            float p = clamp((e - delay) / (1100f / speed));
            if (p <= 0f) continue;
            float d = reach * easeOutCubic(p) * speed;
            float dx = (float) Math.cos(ang), dy = (float) Math.sin(ang);
            float sx = x + dx * (h * 0.06f + d), sy = y + dy * (h * 0.06f + d);
            float a = 1f - clamp((p - 0.7f) / 0.3f);
            int len = 2 + r.nextInt(3);
            // A shard: a bright tip, a purple body, a dark tail, along its flight.
            for (int k = 0; k < len + 2; k++) {
                int rgb = k == 0 ? 0xF0D0FF : k <= len ? (k % 2 == 0 ? 0xC060E8 : 0xAA00AA) : 0x5A1070;
                float ka = k <= len ? a : a * 0.45f;
                sq(px, sx - dx * k * cell * 1.5f, sy - dy * k * cell * 1.5f, cell * (k == 0 ? 3 : 2), ChopScene.alpha(ka) | rgb);
            }
        }
    }

    /** The Legendary's cracks and pours, and the burst's coins and rings. */
    static void hollows(ChopScene.Canvas c, CrystalVeins.Fill px, PetTierTimeline tl, long e, float x, float y, float hs,
                        int w, int h, int cell) {
        Random r = new Random(tl.seed() * 17 + 3);
        for (int i = 0; i < 6; i++) {
            long open = L_CRACK + i * L_CRACK_STEP;
            float[] at = crackAt(i, w, h);
            float grown = easeOutCubic((e - open) / 160f);
            // Seeded zig-zag, fixed for the run.
            float[] zig = new float[9];
            for (int k = 0; k < zig.length; k++) zig[k] = (r.nextFloat() - 0.5f) * 2f;
            if (grown <= 0f) continue;
            float fade = e >= L_BURST ? 1f - clamp((e - L_BURST) / 700f) : 1f;
            if (fade <= 0.02f) continue;
            int gem = GEMS[i];
            int shown = e >= L_BURST ? mixWhite(0xFFAA00, 0.3f * fade) : gem;
            crack(px, at[0], at[1], at[2], h * 0.085f * grown, zig, cell, shown, fade);
            // The crack's own light, and its pour into the head.
            ChopScene.glow(c, at[0], at[1], h * 0.16f, gem, 0.45f * grown * fade);
            long pour = open + L_POUR_AFTER;
            if (e >= pour && e < L_RING + 200) pour(px, e - pour, at[0], at[1], x, y, cell, gem,
                    e >= L_RING ? 1f - clamp((e - L_RING) / 200f) : 1f, i);
        }
        if (e >= L_BURST) {
            for (int k = 0; k < 3; k++) {
                float p = clamp((e - L_BURST - k * 120) / 700f);
                if (p > 0f && p < 1f) ring(px, x, y, hs * (0.6f + 3.4f * easeOutCubic(p)), cell,
                        ChopScene.alpha(0.95f * (1f - p)) | mixWhite(0xFFAA00, 0.4f * (1f - p)));
            }
            coins(px, tl, e - L_BURST, x, y, h, cell);
        }
    }

    /** Where crack i opens: round the screen's edge, one per Hollows colour. Returns {x, y, angle of its run}. */
    static float[] crackAt(int i, int w, int h) {
        double a = Math.toRadians(-90 + 30 + i * 60);
        float x = w / 2f + (float) Math.cos(a) * w * 0.36f, y = h * 0.46f + (float) Math.sin(a) * h * 0.37f;
        return new float[]{x, y, (float) (a + Math.PI / 2)};
    }

    /** A jagged pixel crack through (x, y) along {@code ang}, {@code len} each way. */
    static void crack(CrystalVeins.Fill px, float x, float y, float ang, float len, float[] zig, int cell, int rgb, float a) {
        float dx = (float) Math.cos(ang), dy = (float) Math.sin(ang), nx = -dy, ny = dx;
        int n = Math.max(2, Math.round(2 * len / cell));
        for (int k = 0; k <= n; k++) {
            float s = -len + 2 * len * k / n;
            float u = (k / (float) n) * (zig.length - 1);
            int z0 = Math.min(zig.length - 2, (int) u);
            float off = (zig[z0] + (zig[z0 + 1] - zig[z0]) * (u - z0)) * cell * 2.2f;
            float edge = 1f - Math.abs(s) / Math.max(1f, len);
            float px0 = x + dx * s + nx * off, py0 = y + dy * s + ny * off;
            sq(px, px0 + nx * cell, py0 + ny * cell, cell, ChopScene.alpha(a * 0.7f) | 0x0A0610);
            sq(px, px0, py0, cell, ChopScene.alpha(a) | mixWhite(rgb, 0.45f * edge));
        }
    }

    /** A stream of squares from a crack into the head, {@code e} ms after it started. */
    static void pour(CrystalVeins.Fill px, long e, float x0, float y0, float x1, float y1, int cell, int rgb, float a, int i) {
        float front = clamp(e / (float) L_POUR_MS);
        float dx = x1 - x0, dy = y1 - y0, len = (float) Math.hypot(dx, dy);
        float nx = -dy / Math.max(1f, len), ny = dx / Math.max(1f, len);
        int n = Math.max(6, Math.round(len / (cell * 2.2f)));
        for (int k = 0; k < n; k++) {
            float s = ((k / (float) n) + e / 700f) % 1f;
            if (s > front) continue;
            float wob = (float) Math.sin(s * 9 + e / 90.0 + i) * cell * 1.2f;
            float sx = x0 + dx * s + nx * wob, sy = y0 + dy * s + ny * wob;
            int size = k % 3 == 0 ? cell * 2 : cell;
            sq(px, sx, sy, size, ChopScene.alpha(a * (0.55f + 0.45f * s)) | mixWhite(rgb, 0.3f * s));
        }
    }

    /** The burst's coins: thrown up out of the head, turning, falling off the bottom. */
    static void coins(CrystalVeins.Fill px, PetTierTimeline tl, long e, float x, float y, int h, int cell) {
        Random r = new Random(tl.seed() * 13 + 11);
        for (int i = 0; i < 44; i++) {
            float vx = (r.nextFloat() - 0.5f) * h * 1.1f, vy = -h * (0.7f + 0.6f * r.nextFloat());
            long delay = r.nextInt(500);
            float s = (e - delay) / 1000f;
            if (s <= 0f || s > 2.4f) continue;
            float cx = x + vx * s, cy = y + vy * s + 0.5f * h * 1.9f * s * s;
            float turn = Math.abs((float) Math.cos(s * 9 + i));
            int wc = Math.max(1, Math.round(3 * turn));
            float a = 1f - clamp((s - 1.9f) / 0.5f);
            for (int k = 0; k < wc; k++) {
                for (int j = 0; j < 3; j++) {
                    boolean rim = k == 0 || k == wc - 1 || j == 0 || j == 2;
                    sq(px, cx + (k - wc / 2f) * cell, cy + (j - 1) * cell, cell, ChopScene.alpha(a) | (rim ? 0xB8741A : 0xFFD84A));
                }
            }
        }
    }

    static void sq(CrystalVeins.Fill px, float x, float y, int size, int argb) {
        if ((argb >>> 24) < 3) return;
        px.fill(Math.round(x - size / 2f), Math.round(y - size / 2f), size, size, argb);
    }

    // ================================================================== the coil

    /** The worm's five body segments: up from below in a spiral, into a ring round the head, gold, then thrown out. */
    static void coil(ChopScene.Canvas c, PetTierTimeline tl, long e, float x, float y, float full, int w) {
        if (e < L_COIL) return;
        String skin = c.bestiarySkin(BODY_SKIN);
        float R = full * 0.98f, seg = full * 0.44f;
        for (int i = 0; i < 5; i++) {
            float u = ease(clamp((e - L_COIL - i * 110) / (float) (L_RING - L_COIL - 440)));
            if (u <= 0f) continue;
            double end = Math.toRadians(-90 + i * 72) + (e - L_RING) / 2600.0 * (e > L_RING ? 1 : 0);
            double ang = end - (1f - u) * Math.PI * 1.7;
            float r = R * (1f + 2.2f * (1f - u));
            if (e >= L_BEAT2) r *= 1f - 0.06f * (1f - clamp((e - L_BEAT2) / 300f)) * clamp((e - L_BEAT2) / 60f);
            float out = e >= L_BURST ? easeIn(clamp((e - L_BURST) / 650f)) : 0f;
            r += out * full * 3.2f;
            float sx = x + (float) Math.cos(ang) * r, sy = y + (float) Math.sin(ang) * r * 0.9f;
            float gold = ease(clamp((e - L_GOLD - i * L_GOLD_STEP) / 220f));
            float glow = 0.3f * gold * (1f - clamp((e - L_GOLD - i * L_GOLD_STEP - 220) / 400f)) + (e >= L_BURST ? 0.4f * (1f - out) : 0f);
            float roll = (float) Math.toDegrees(ang) + 90f;
            float yaw = 35f + (float) Math.toDegrees(ang) * 0.5f + out * 400f;
            // Gold rises up each segment texel by texel through its own skin (a tint can only darken, and the
            // worm is dark): the gild, as the Crown's, the skin showing through as relief.
            if (gold > 0f) {
                float fill = gold * 1.15f;
                ChopScene.gild = (gx, gy, gz, cell, now, gsx, gsy) -> (gy + 4.5f) / 9f <= fill ? 0x60F2B53A : 0;
                ChopScene.glow(c, sx, sy, seg * 2.2f, 0xFFAA00, 0.5f * gold * (1f - out));
            }
            head(c, skin, sx, sy, seg * (1f - 0.5f * out), yaw, roll, glow, 1f, 1f, 1f, w);
            ChopScene.gild = null;
        }
    }

    // ================================================================== the card

    static void card(ChopScene.Canvas c, PetTierTimeline tl, long t, float cx, float cy, float full, float shrink, int h) {
        long held = t - tl.cardFrom();
        float a = clamp(held / 220f) * (1f - clamp(shrink * 3f));
        if (a <= 0.03f) return;
        float ts = Math.max(1f, Math.min(2f, h / 240f));
        Tier tier = tl.tier();
        // The box head with its hat and the camera's tilt stands a little taller than {@code full}.
        float top = cy - full * 0.6f - 11f * ts, below = cy + full * 0.6f + 9f * ts;
        // The banner: stamped down from big for the better pets.
        float stamp = tier == Tier.RARE ? 1f : 1f + 1.6f * (1f - easeOutCubic(held / 170f));
        float bs = (tier == Tier.LEGENDARY ? 1.9f : 1.5f) * ts * stamp;
        if (tier == Tier.LEGENDARY) top -= 3f * ts;
        c.text("PET DROP!", cx, top, bs, ChopScene.alpha(a * (stamp > 1.5f ? 0.6f : 1f)) | 0xFFAA00);
        int letters = tl.letterMs() <= 0 ? 999 : (int) Math.max(0, (held - 200) / tl.letterMs());
        c.name(cx, below, 1.6f * ts, ChopScene.alpha(a) | tier.rgb, letters);
        float yy = below + 15f * ts;
        if (tl.price() > 0) {
            float k = clamp((t - tl.countStart()) / (float) COUNT_MS);
            if (k > 0f) {
                long shown = Math.round(tl.price() * (1f - (1f - k) * (1f - k) * (1f - k)));
                c.text(String.format("%,d coins", shown), cx, yy, 1.2f * ts, ChopScene.alpha(a) | 0xFFD84A);
                yy += 9f * ts;
                c.text(tl.priceSource(), cx, yy, 0.75f * ts, ChopScene.alpha(a) | 0x9A9A9A);
                yy += 11f * ts;
            }
        }
        if (held > 450) {
            String odds = "~1 in " + String.format("%,d", tier.oneIn) + " kills";
            if (!tl.note().isEmpty()) {
                c.text(tl.note(), cx, yy, 0.9f * ts, ChopScene.alpha(a * clamp((held - 450) / 200f)) | 0xDDDDDD);
                yy += 10f * ts;
            }
            c.text(odds, cx, yy, 0.75f * ts, ChopScene.alpha(a * clamp((held - 450) / 200f)) | 0x9A9A9A);
        }
    }

    // ================================================================== helpers

    static int mixWhite(int rgb, float k) {
        return PetTierTimeline.mix(rgb, 0xFFFFFF, k);
    }

    static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    static float ease(float t) { t = clamp(t); return t * t * (3 - 2 * t); }
    static float easeIn(float t) { t = clamp(t); return t * t; }
    static float easeOutCubic(float t) { t = clamp(t); float u = 1f - t; return 1f - u * u * u; }
}
