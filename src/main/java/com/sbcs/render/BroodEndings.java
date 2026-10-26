package com.sbcs.render;

import com.sbcs.anim.SlayerTimeline;
import com.sbcs.anim.SlayerTimeline.Ending;

import java.util.List;

import static com.sbcs.anim.SlayerTimeline.ENDING;
import static com.sbcs.anim.SlayerTimeline.KILLED;
import static com.sbcs.anim.SlayerTimeline.clamp;
import static com.sbcs.anim.SlayerTimeline.ease;
import static com.sbcs.anim.SlayerTimeline.span;
import static com.sbcs.render.SlayerScene.CHEST;
import static com.sbcs.render.SlayerScene.G;
import static com.sbcs.render.SlayerScene.H;
import static com.sbcs.render.SlayerScene.HEAD;
import static com.sbcs.render.SlayerScene.IN;
import static com.sbcs.render.SlayerScene.POLYS;
import static com.sbcs.render.SlayerScene.W;
import static com.sbcs.render.SlayerScene.rigs;

/**
 * "The Brood" - the Tarantula Broodfather's own endings, between the kill and the showcase, each about its item as
 * Matej asked (2026-10-25: "if it's a digested mosquito, then it should have ... a mosquito being digested"). The
 * stage, the kill and the showcase are {@link SlayerScene}'s; this answers its hooks for every Tarantula drop but the
 * shared book and Paragon Shard (SlayerEndings hands them over).
 *
 * The lore is the items' own (NEU's text, the wiki):
 * <ul>
 *   <li><b>Digested Mosquito</b>: a mosquito lands on the dead spider's belly and drinks until it is swollen with
 *       blood; the corpse jerks and reels it into its fangs; the belly turns see-through and the mosquito sinks into
 *       bubbling acid - wings off, legs off, shrivelling brown; the spider heaves and spits the remains out.</li>
 *   <li><b>Primordial Eye</b> - "a single, all-seeing eye, forcefully removed from the Primordial Broodfather. Though
 *       expressionless, you can sense the rage": the camera closes on the slumped spider's face; its eyes go out but
 *       one, which bulges out and looks round; it is torn out on its nerve, snaps free, and stares at you, raging.</li>
 *   <li><b>Brick Red Dye</b> ("The Kiln"), the grandest: time stops and the world goes grey; bricks - the only colour
 *       left - fall out of the sky and build a kiln over the corpse, course by course, and close its dome; it fires,
 *       glowing through every joint; it blows apart, time comes back, and the dye stands where the spider lay.</li>
 *   <li><b>Shriveled Wasp</b> - "a notably large wasp carcass, drowned in the web and drained of blood. Its stinger
 *       remains intact": a big wasp flies into the web strung between the corpse's legs, struggles, is drained grey
 *       and shrivels; its stinger glints.</li>
 *   <li><b>Ensnared Snail</b> - "tightly wrapped in webbing by a fiend of Eldritch proportions. Maybe it will have its
 *       revenge - in the next life": a snail creeps past, silk shoots out and wraps it; its ghost rises, glares and
 *       dives into the corpse.</li>
 *   <li><b>Vial of Venom</b> - "the damage to your internal organs": the fangs drip venom into a vial; it fills,
 *       bubbles, fumes, and is corked.</li>
 *   <li><b>Tarantula Talisman</b> - "every 10th melee hit on the same enemy deals +10% damage": ten ghost hits
 *       count up on the corpse; the tenth crits, and the web over it is drawn tight into the pendant.</li>
 *   <li><b>Fly Swatter</b>: flies gather over the corpse - SWAT.</li>
 *   <li><b>Darkness Within Rune</b>: the lights go out; the eight red eyes go out one by one; the dark itself swirls
 *       into the rune.</li>
 *   <li><b>Spider / Tarantula Catalyst</b>: silk wraps the corpse into a cocoon that throbs in the catalyst's colour
 *       and splits.</li>
 * </ul>
 * Everything is pure, a function of the timeline, the time and the seed: the same frames offline and in game.
 * The public beat constants are what the soundtrack keys on.
 */
public final class BroodEndings {
    private BroodEndings() {}

    // ================================================================== the beats (ms after the ending starts)

    /** Digested Mosquito: lands, the snap, the belly turns see-through, it spits. */
    public static final long MQ_LAND = 1250, MQ_SNAP = 2300, MQ_XRAY = 2650, MQ_WINGS = 3150, MQ_SPIT = 4950, MQ_SPLAT = 5450;
    /** Primordial Eye: the eyes go out, it bulges, the pull, the snap. */
    public static final long E_DARK = 900, E_BULGE = 1500, E_PULL = 3000, E_SNAP = 4250;
    /** Brick Red Dye: the freeze, the first brick, the dome closed, the firing, the blast. */
    public static final long B_FREEZE = 150, B_RAIN = 900, B_CLOSED = 4300, B_FIRE = 4700, B_BLOW = 6500;
    /** Shriveled Wasp: caught, drained, the glint. */
    public static final long WS_CAUGHT = 1000, WS_DRAIN = 1500, WS_GLINT = 2900;
    /** Ensnared Snail: the silk shot, wrapped, the ghost, its dive. */
    public static final long SN_SHOT = 1300, SN_WRAPPED = 2100, SN_GHOST = 2300, SN_DIVE = 3200;
    /** Vial of Venom: the first drop, corked. */
    public static final long V_DRIP = 450, V_CORK = 2500;
    /** Tarantula Talisman: the first of the ten hits; the tenth. */
    public static final long TL_FIRST = 300, TL_STEP = 150, TL_TENTH = TL_FIRST + 9 * TL_STEP + 250;
    /** Fly Swatter: the swat. */
    public static final long SWAT = 1650;
    /** Darkness Within Rune: dark, the eyes start going out, the rune lights. */
    public static final long D_DARK = 250, D_EYES = 900, D_IGNITE = 2700;
    /** The catalysts: the cocoon splits. */
    public static final long C_SPLIT = 1500;

    /** Each ending's length, from ENDING to the showcase (SlayerTimeline.endingMs asks). */
    public static long endingMs(Ending e) {
        return switch (e) {
            case MOSQUITO -> 6300;
            case EYE -> 6700;
            case BRICK -> 8600;
            case WASP -> 3700;
            case SNAIL -> 4300;
            case VENOM -> 3300;
            case TALISMAN -> 3100;
            case SWATTER -> 2900;
            case DARKNESS -> 3700;
            case COCOON -> 2100;
            default -> 1500;
        };
    }

    // ================================================================== the corpse

    /** Lying on its back: how far a spider rolled over must be lifted for its back to rest on the ground. */
    static final float LIFT = 26f;

    /** Whether this ending lets the spider roll onto its back (the Eye wants its face, so it slumps instead). */
    static boolean rolls(SlayerTimeline tl) {
        return tl.ending() != Ending.EYE && tl.ending() != Ending.DARKNESS;
    }

    static float corpse(SlayerTimeline tl, long t) {
        long u = t - ENDING;
        return switch (tl.ending()) {
            case COCOON -> 1f - span(u, 700, 1);
            case BRICK -> u < B_BLOW ? 1f : 0f;
            case SNAIL -> 1f - span(u, SN_DIVE + 250, 500);
            default -> 1f - span(t, tl.arrive() - 550, 450);
        };
    }

    static float tipScale(SlayerTimeline tl, long t) {
        return rolls(tl) ? 1f : 0f;
    }

    static float bossYaw(SlayerTimeline tl, long t) {
        long u = t - ENDING;
        if (tl.ending() == Ending.MOSQUITO && u > MQ_SNAP - 60 && u < MQ_SNAP + 400) return 9f * (float) Math.sin((u - MQ_SNAP) / 45.0) * (1f - span(u, MQ_SNAP, 400));
        if (tl.ending() == Ending.SNAIL && u > SN_DIVE + 150) return 6f * (float) Math.sin(u / 35.0) * (1f - span(u, SN_DIVE + 150, 600));
        return 0f;
    }

    static float[] bossOffset(SlayerTimeline tl, long t) {
        long u = t - ENDING;
        float tip = SlayerTimeline.deathTip(t);
        float x = 0f, y = 0f, z = 0f;
        if (rolls(tl)) {
            // Rolled over about its feet it would lie half under the ground: lifted onto its back as it goes.
            y += LIFT * tip;
        } else if (t > KILLED) {
            // Slumped: the legs give and the body drops to the ground.
            y -= 7f * ease((t - KILLED) / 500f);
        }
        switch (tl.ending()) {
            case MOSQUITO -> {
                // The jerk as it snaps the mosquito up, then the heaves before it spits.
                y += 5f * bump(u, MQ_SNAP - 40, 220);
                for (int i = 0; i < 3; i++) y += 3.2f * bump(u, MQ_SPIT - 620 + i * 210, 170);
            }
            case EYE -> {
                // Dragged towards the eye being pulled out of it, then dropped as the nerve snaps.
                float pull = u < E_PULL ? 0f : (u < E_SNAP ? ease((u - E_PULL) / (float) (E_SNAP - E_PULL)) : 1f - ease((u - E_SNAP) / 220f));
                float jit = u > E_PULL && u < E_SNAP ? 0.7f * (float) Math.sin(u / 21.0) * pull : 0f;
                x += 2.4f * pull + jit;
                y += 2.0f * pull;
                z += 5.5f * pull;
            }
            case SWATTER -> y -= 4f * squash(u);
            case TALISMAN -> {
                // Jolted by each of the ten hits.
                int k = talismanHit(u);
                if (k >= 0) {
                    float j = 1f - span(u, TL_FIRST + k * TL_STEP + (k == 9 ? 250 : 0), 110);
                    x += (k % 2 == 0 ? 1.4f : -1.4f) * j * (k == 9 ? 2.5f : 1f);
                }
            }
            case COCOON -> { }
            default -> { }
        }
        return new float[]{x, y, z};
    }

    /** A quick up-and-down, 0..1..0 over {@code ms} from {@code at}. */
    static float bump(long u, long at, long ms) {
        if (u < at || u > at + ms) return 0f;
        return (float) Math.sin((u - at) / (double) ms * Math.PI);
    }

    static int bossTint(SlayerTimeline tl, long t) {
        long u = t - ENDING;
        switch (tl.ending()) {
            case DARKNESS:
                return SlayerScene.shade(0xFFFFFF, 1f - 0.92f * darkness(u));
            case MOSQUITO:
                // The belly shown through it: the rest of the body darkened, so the stomach reads as the inside.
                return SlayerScene.mixRgb(0xFFFFFF, 0x2A2632, 0.7f * xray(u));
            case BRICK:
                return SlayerScene.mixRgb(0xFFFFFF, 0xA4A4AC, frozen(u));
            case WASP:
            case SNAIL:
                return 0xFFFFFF;
            case EYE:
                // Its rage reddens it as the eye comes out.
                return SlayerScene.mixRgb(0xFFFFFF, 0xFF9A90, 0.35f * span(u, E_BULGE, 1500) * (1f - span(u, E_SNAP, 600)));
            default:
                return 0xFFFFFF;
        }
    }

    static float floorAlpha(SlayerTimeline tl, long t) {
        long u = t - ENDING;
        float a = Math.min(1f, t / 250f);
        if (tl.ending() == Ending.DARKNESS) a *= 1f - darkness(u);
        if (tl.ending() == Ending.EYE) a *= 1f - 0.55f * eyeZoom(u);
        return a * (1f - 0.85f * tl.rise(t));
    }

    static int floorTint(SlayerTimeline tl, long t) {
        if (tl.ending() == Ending.BRICK) return SlayerScene.mixRgb(0xFFFFFF, 0x9A9AA2, frozen(t - ENDING));
        return 0xFFFFFF;
    }

    static float extraDim(SlayerTimeline tl, long t) {
        long u = t - ENDING;
        float d = switch (tl.ending()) {
            case DARKNESS -> 0.34f * darkness(u);
            case BRICK -> 0.22f * frozen(u);
            case EYE -> 0.16f * eyeZoom(u);
            case MOSQUITO -> 0.12f * xray(u);
            default -> 0f;
        };
        return d * tl.fadeOut(t);
    }

    static float shake(SlayerTimeline tl, long t) {
        long u = t - ENDING;
        float s = 0f;
        switch (tl.ending()) {
            case MOSQUITO -> s += 3f * impulse(u, MQ_SNAP) + 2f * impulse(u, MQ_SPIT);
            case EYE -> {
                if (u > E_PULL && u < E_SNAP) s += 2.6f * span(u, E_PULL, E_SNAP - E_PULL);
                s += 7f * impulse(u, E_SNAP);
            }
            case BRICK -> {
                for (int i = 0; i < 6; i++) s += 0.9f * impulse(u, courseLanded(i));
                if (u > B_FIRE && u < B_BLOW) s += 1.6f * span(u, B_FIRE, B_BLOW - B_FIRE);
                s += 9f * impulse(u, B_BLOW);
            }
            case SWATTER -> s += 7f * impulse(u, SWAT);
            case TALISMAN -> s += 5f * impulse(u, TL_TENTH);
            case SNAIL -> s += 2.5f * impulse(u, SN_DIVE + 150);
            case COCOON -> s += 2.5f * impulse(u, C_SPLIT);
            case DARKNESS -> s += 3f * impulse(u, D_IGNITE);
            default -> { }
        }
        return s;
    }

    /** 1 at {@code at}, dying away over half a second. */
    static float impulse(long u, long at) {
        if (u < at) return 0f;
        return (float) Math.exp(-(u - at) / 140.0) * (u - at < 600 ? 1f : 0f);
    }

    // ================================================================== the camera

    static void camera(SlayerTimeline tl, long t, int w, int h) {
        long u = t - ENDING;
        float rise = tl.rise(t);
        // Every ending happens on and round the corpse: once it is down the shot settles on it, with room above.
        float on = ease((t - KILLED) / 800f) * (1f - rise);
        ChopScene.fx += (CHEST[0] - ChopScene.fx) * on;
        ChopScene.fz += (CHEST[2] - ChopScene.fz) * on;
        ChopScene.fy += (CHEST[1] + 6f - ChopScene.fy) * on;
        ChopScene.sy0 += (h * 0.56f - ChopScene.sy0) * on;
        switch (tl.ending()) {
            case EYE -> {
                // In on its face, and back out for the showcase.
                float z = eyeZoom(u);
                ChopScene.size *= 1f + 1.35f * z;
                ChopScene.fx += (HEAD[0] - ChopScene.fx) * z;
                ChopScene.fy += (HEAD[1] - ChopScene.fy) * z;
                ChopScene.fz += (HEAD[2] - ChopScene.fz) * z;
                ChopScene.sy0 += (h * 0.5f - ChopScene.sy0) * z;
            }
            case MOSQUITO -> {
                // In on the belly while it digests.
                float z = ease((u - MQ_SNAP - 150) / 600f) * (1f - ease((u - MQ_SPIT + 250) / 500f));
                ChopScene.size *= 1f + 0.55f * z;
                ChopScene.fx += (BELLY[0] - ChopScene.fx) * z;
                ChopScene.fy += (BELLY[1] - ChopScene.fy) * z;
                ChopScene.fz += (BELLY[2] - ChopScene.fz) * z;
                ChopScene.sy0 += (h * 0.52f - ChopScene.sy0) * z;
            }
            case BRICK -> {
                // Back and up as the kiln rises over it, in close as it fires.
                float back = ease((u - B_RAIN) / 1600f) * (1f - rise);
                float fire = ease((u - B_FIRE) / 1500f) * (1f - ease((u - B_BLOW) / 300f));
                ChopScene.size *= (1f - 0.2f * back) * (1f + 0.12f * fire);
                ChopScene.fy += 14f * back;
            }
            case SNAIL -> {
                float g = ease((u - SN_GHOST) / 500f) * (1f - rise);
                ChopScene.fy += 8f * g;
            }
            default -> { }
        }
    }

    static int nameColour(SlayerTimeline tl, long t) {
        int c = tl.drop().colour;
        if (tl.drop().dye()) {
            float f = 0.5f + 0.5f * (float) Math.sin(t / 200.0);
            return SlayerScene.mixRgb(c, SlayerScene.mixRgb(c, 0xFFFFFF, 0.45f), f);
        }
        return c;
    }

    // ================================================================== this frame's corpse

    /** The belly's top middle (the abdomen, lying on its back), the abdomen's middle, and its size on screen. */
    static final float[] BELLY = new float[3], ABDOMEN = new float[3];
    /** The abdomen's half-extents along the screen's right and up (world units). */
    static float abdomenRx = 10f, abdomenRy = 8f;
    /** The camera's right, up and towards-it vectors in the world, this frame. */
    static final float[] CR = new float[3], CU = new float[3], CD = new float[3];

    static void frame() {
        CR[0] = ChopScene.cy;
        CR[1] = 0f;
        CR[2] = ChopScene.sy;
        CU[0] = -ChopScene.sy * ChopScene.sp;
        CU[1] = ChopScene.cp;
        CU[2] = ChopScene.cy * ChopScene.sp;
        CD[0] = -ChopScene.sy * ChopScene.cp;
        CD[1] = -ChopScene.sp;
        CD[2] = ChopScene.cy * ChopScene.cp;
        // The abdomen from the boss's own polygons (flag 2, the spider's body1), the eyes layer left out.
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE, sx = 0, sy = 0, sz = 0;
        float r0 = Float.MAX_VALUE, r1 = -Float.MAX_VALUE, v0 = Float.MAX_VALUE, v1 = -Float.MAX_VALUE;
        int n = 0;
        for (SlayerScene.Poly p : POLYS) {
            if ((p.flags & 2) == 0 || (p.flags & 4) != 0) continue;
            for (int k = 0; k < 4; k++) {
                float x = p.p[0], y = p.p[1], z = p.p[2];
                if (k == 1) { x = p.p[3]; y = p.p[4]; z = p.p[5]; }
                if (k == 2) { x = p.p[6]; y = p.p[7]; z = p.p[8]; }
                if (k == 3) { x = p.p[3] + p.p[6] - p.p[0]; y = p.p[4] + p.p[7] - p.p[1]; z = p.p[5] + p.p[8] - p.p[2]; }
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
                sx += x;
                sy += y;
                sz += z;
                float rr = x * CR[0] + z * CR[2], vv = x * CU[0] + y * CU[1] + z * CU[2];
                r0 = Math.min(r0, rr);
                r1 = Math.max(r1, rr);
                v0 = Math.min(v0, vv);
                v1 = Math.max(v1, vv);
                n++;
            }
        }
        if (n == 0) {
            System.arraycopy(CHEST, 0, ABDOMEN, 0, 3);
            System.arraycopy(CHEST, 0, BELLY, 0, 3);
            return;
        }
        ABDOMEN[0] = sx / n;
        ABDOMEN[1] = sy / n;
        ABDOMEN[2] = sz / n;
        BELLY[0] = ABDOMEN[0];
        BELLY[1] = maxY;
        BELLY[2] = ABDOMEN[2];
        abdomenRx = (r1 - r0) / 2f;
        abdomenRy = (v1 - v0) / 2f;
    }

    // ================================================================== the models each ending adds

    static void models(SlayerTimeline tl, long t) {
        frame();
        if (rigs == null) return;
        long u = t - ENDING;
        switch (tl.ending()) {
            case MOSQUITO -> mosquitoModels(tl, u);
            case EYE -> eyeModels(tl, u);
            case BRICK -> kiln(tl, u);
            case WASP -> wasp(tl, u);
            case SNAIL -> snail(tl, u);
            case SWATTER -> swatter(tl, u);
            case COCOON -> cocoon(tl, u);
            case DARKNESS -> {
                // The eyes are the last light: gone from the model, drawn as squares that go out one by one.
                if (u > D_DARK) dropEyes();
            }
            default -> { }
        }
    }

    /** Takes the spider's eyes layer out of this frame's polygons. */
    static void dropEyes() {
        POLYS.removeIf(p -> p.slot == SlayerScene.SLOT_EYES);
    }

    // ------------------------------------------------------------------ building blocks

    /** A solid-colour box from {@code a} to {@code b} (world), {@code thick} across: a leg, a strand, a stalk. */
    static void rod(float[] a, float[] b, float thick, int rgb, int add, List<SlayerScene.Poly> to) {
        float dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-3f) return;
        float[] Z = {dx / len, dy / len, dz / len};
        float[] up = Math.abs(Z[1]) > 0.9f ? new float[]{1, 0, 0} : new float[]{0, 1, 0};
        float[] X = norm(cross(up, Z));
        float[] Y = cross(Z, X);
        SlayerScene.solid((a[0] + b[0]) / 2f, (a[1] + b[1]) / 2f, (a[2] + b[2]) / 2f, thick / 2f, thick / 2f, len / 2f, X, Y, Z, rgb, add, to);
    }

    /** A point {@code (x, y, z)} in the frame {@code ax} (three axes) about {@code o}. */
    static float[] at(float[] o, float[][] ax, float x, float y, float z) {
        return new float[]{o[0] + ax[0][0] * x + ax[1][0] * y + ax[2][0] * z,
                o[1] + ax[0][1] * x + ax[1][1] * y + ax[2][1] * z,
                o[2] + ax[0][2] * x + ax[1][2] * y + ax[2][2] * z};
    }

    static float[] cross(float[] a, float[] b) {
        return new float[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static float[] norm(float[] v) {
        float l = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return l < 1e-6f ? new float[]{1, 0, 0} : new float[]{v[0] / l, v[1] / l, v[2] / l};
    }

    /** A box in the frame {@code ax}, its middle at local {@code (x, y, z)}, half sizes along the frame's axes. */
    static void part(float[] o, float[][] ax, float x, float y, float z, float hx, float hy, float hz, int rgb, int add, List<SlayerScene.Poly> to) {
        float[] c = at(o, ax, x, y, z);
        SlayerScene.solid(c[0], c[1], c[2], hx, hy, hz, ax[0], ax[1], ax[2], rgb, add, to);
    }

    /** A camera-facing plate (a box flat towards the camera) at world {@code c}, {@code hw} x {@code hh}. */
    static void plate(float[] c, float hw, float hh, float depth, int rgb, int add, List<SlayerScene.Poly> to) {
        SlayerScene.solid(c[0], c[1], c[2], hw, hh, Math.max(0.2f, depth), CR, CU, CD, rgb, add, to);
    }

    /** {@code c} moved by {@code r} along the screen's right, {@code v} up and {@code d} towards the camera. */
    static float[] scr(float[] c, float r, float v, float d) {
        return new float[]{c[0] + CR[0] * r + CU[0] * v + CD[0] * d, c[1] + CR[1] * r + CU[1] * v + CD[1] * d, c[2] + CR[2] * r + CU[2] * v + CD[2] * d};
    }

    // ================================================================== the Digested Mosquito

    /**
     * A mosquito of boxes: a small dark head with a needle, a humped thorax, a long banded abdomen (swelling red with
     * blood by {@code bloat}), six long thin legs, two clear wings. {@code o} its thorax; facing {@code yaw} (radians,
     * 0 = +Z), pitched. {@code digest} 0..1 browns and shrivels it; {@code legs} how many are left; {@code wings} on.
     */
    static void mosquito(float[] o, float yaw, float pitch, float roll, float s, float flap, float bloat, float digest,
                         int legs, boolean wings, float probe, List<SlayerScene.Poly> to) {
        float[][] ax = SlayerScene.axes(yaw, pitch, roll);
        float sh = 1f - 0.4f * digest;
        s *= sh;
        int body = SlayerScene.mixRgb(0x3B342E, 0x5A3E22, digest);
        int band = SlayerScene.mixRgb(0xCFC6B4, 0x7A5A34, digest);
        int blood = SlayerScene.mixRgb(0xB0141C, 0x6A2E18, digest);
        // Head, eyes and the needle (pointing down and forward, or straight down while it drinks).
        part(o, ax, 0, 0.6f * s, 4.2f * s, 1.3f * s, 1.2f * s, 1.2f * s, body, 0, to);
        part(o, ax, 1.0f * s, 0.9f * s, 4.9f * s, 0.5f * s, 0.6f * s, 0.5f * s, 0x701010, 0, to);
        part(o, ax, -1.0f * s, 0.9f * s, 4.9f * s, 0.5f * s, 0.6f * s, 0.5f * s, 0x701010, 0, to);
        float pa = 0.5f + 0.9f * probe;
        float[] n0 = at(o, ax, 0, 0.1f * s, 5.2f * s);
        float[] n1 = at(o, ax, 0, (0.1f - 5.2f * (float) Math.sin(pa)) * s, (5.2f + 5.2f * (float) Math.cos(pa)) * s);
        rod(n0, n1, 0.45f * s, SlayerScene.mixRgb(0x2A1E18, 0x4A3018, digest), 0, to);
        // The thorax, humped.
        part(o, ax, 0, 0.9f * s, 1.4f * s, 1.7f * s, 1.6f * s, 2.0f * s, body, 0, to);
        // The abdomen: five segments tapering back, banded; each swells and reddens with the blood.
        for (int i = 0; i < 5; i++) {
            float zz = -1.7f - i * 1.75f;
            float w = (1.35f - 0.14f * i) * (1f + 1.1f * bloat * (1f - Math.abs(i - 1.6f) / 4.5f));
            int col = i % 2 == 0 ? body : band;
            col = SlayerScene.mixRgb(col, blood, bloat * (i % 2 == 0 ? 0.85f : 0.6f));
            part(o, ax, 0, (0.6f - 0.25f * i) * s - 0.35f * bloat * s, zz * s, w * s, w * 0.9f * s, 0.95f * s, col,
                    bloat > 0.4f ? ChopScene.alpha(0.18f * bloat) | 0xFF2020 : 0, to);
        }
        // Six legs: long, thin, bent at the knee, splayed.
        for (int i = 0; i < 6 && i < legs; i++) {
            int side = i % 2 == 0 ? 1 : -1;
            int pair = i / 2;
            float zz = (2.2f - pair * 1.1f) * s;
            float[] hip = at(o, ax, side * 1.2f * s, 0.1f * s, zz);
            float spread = (pair - 1) * 0.75f;
            float kx = side * 4.8f, ky = 2.6f, kz = (float) Math.sin(spread) * 4.5f;
            float[] knee = at(o, ax, side * 1.2f * s + kx * s, 0.1f * s + ky * s, zz + kz * s);
            float[] foot = at(o, ax, side * 1.2f * s + kx * 1.5f * s, 0.1f * s - 4.6f * s, zz + kz * 1.9f * s);
            int lc = SlayerScene.mixRgb(0x2E2722, 0x4A3220, digest);
            rod(hip, knee, 0.42f * s, lc, 0, to);
            rod(knee, foot, 0.34f * s, lc, 0, to);
        }
        if (wings) {
            // Two clear wings off the thorax, back along the body, beating.
            for (int side = -1; side <= 1; side += 2) {
                float ang = 0.35f + 0.55f * (float) Math.sin(flap) * side * 0 + 0.55f * (float) Math.sin(flap);
                float[][] wa = SlayerScene.axes(yaw + side * 0.32f, pitch, roll + side * ang);
                float[] base = at(o, ax, side * 1.0f * s, 2.3f * s, 0.6f * s);
                float[] mid = at(base, wa, side * 1.2f * s, 0.15f * s, -3.0f * s);
                SlayerScene.solid(mid[0], mid[1], mid[2], 1.25f * s, 0.12f * s, 3.4f * s, wa[0], wa[1], wa[2],
                        SlayerScene.mixRgb(0xDCE8F4, 0x9A9080, digest), ChopScene.alpha(0.25f) | 0xFFFFFF, to);
            }
        }
    }

    /** How see-through the belly is, 0..1. */
    static float xray(long u) {
        return span(u, MQ_XRAY, 350) * (1f - span(u, MQ_SPIT - 350, 300));
    }

    /** How digested it is, 0..1. */
    static float digest(long u) {
        return span(u, MQ_XRAY + 200, MQ_SPIT - MQ_XRAY - 700);
    }

    /** The mosquito's flight in: a buzzing, looping line from off the left onto the belly. */
    static float[] mosquitoFlight(long u) {
        float k = clamp(u / (float) MQ_LAND);
        float e = 1f - (1f - k) * (1f - k);
        float sx = -150f, sy = 70f, sz = 30f;
        float lx = BELLY[0] + 1f, ly = BELLY[1] + 6.5f, lz = BELLY[2] + 1f;
        float x = sx + (lx - sx) * e, y = sy + (ly - sy) * e, z = sz + (lz - sz) * e;
        float wob = (1f - k);
        x += 9f * (float) Math.sin(u / 95.0) * wob;
        y += 7f * (float) Math.sin(u / 61.0) * wob + 5f * (float) Math.sin(u / 23.0) * wob * 0.4f;
        z += 6f * (float) Math.cos(u / 83.0) * wob;
        return new float[]{x, y, z};
    }

    static void mosquitoModels(SlayerTimeline tl, long u) {
        if (u < 0) return;
        float s = 1.25f;
        if (u < MQ_SNAP) {
            float[] p = mosquitoFlight(u);
            float drink = span(u, MQ_LAND + 150, MQ_SNAP - MQ_LAND - 300);
            float landed = span(u, MQ_LAND - 150, 150);
            // Hovering it beats its wings fast and banks; landed, they fold and it pumps.
            float flap = u < MQ_LAND ? u / 9f : (float) Math.sin(u / 300.0) * 0.2f;
            float yaw = (float) Math.toRadians(80f - 50f * landed);
            float pitch = (1f - landed) * 0.25f * (float) Math.sin(u / 140.0) + landed * -0.08f;
            float pump = landed * 0.06f * (float) Math.sin(u / 70.0);
            mosquito(p, yaw, pitch, 0f, s * (1f + pump), flap, drink, 0f, 6, true, landed, POLYS);
            return;
        }
        if (u < MQ_SNAP + 220) {
            // Reeled into the fangs on a strand of silk, tumbling.
            float k = ease((u - MQ_SNAP) / 220f);
            float[] p0 = mosquitoFlight(MQ_LAND);
            float[] p = {p0[0] + (HEAD[0] - p0[0]) * k, p0[1] + (HEAD[1] + 3f - p0[1]) * k, p0[2] + (HEAD[2] - p0[2]) * k};
            mosquito(p, 1.2f + 6f * k, 1.4f * k, 2f * k, s * (1f - 0.4f * k), 0f, 1f, 0f, 6, true, 1f, POLYS);
            return;
        }
        float x = xray(u);
        if (x <= 0.02f || u > MQ_SPIT) return;
        stomach(tl, u, x);
    }

    /**
     * The belly seen through, drawn over the corpse ({@link SlayerScene#IN}): a window the shape of the abdomen - its
     * wall, the dark inside, acid in the bottom half - and the mosquito sinking into it, losing its wings, then its
     * legs one by one, browning and shrivelling. The acid in front of it is drawn in front, so it is really in it.
     */
    static void stomach(SlayerTimeline tl, long u, float open) {
        float rx = abdomenRx * 0.92f * (0.55f + 0.45f * open), ry = abdomenRy * 0.85f * (0.55f + 0.45f * open);
        float[] c = scr(ABDOMEN, 0f, 0f, 14f);
        float d = digest(u);
        int rows = 12;
        float level = -0.05f - 0.25f * d;
        int acidA = SlayerScene.mixRgb(0x76D02A, 0x9A9A2A, d * 0.6f), acidB = SlayerScene.mixRgb(0x4FA81E, 0x6E6A1E, d * 0.6f);
        // The wall and the dark inside, row by row of an ellipse (pixel steps); the acid's back over them.
        for (int i = 0; i < rows; i++) {
            float v0 = -1f + i * 2f / rows, v1 = v0 + 2f / rows, vm = (v0 + v1) / 2f;
            float hw = (float) Math.sqrt(Math.max(0f, 1f - vm * vm));
            float yc = vm * ry;
            boolean acid = vm < level;
            int wall = 0x5A1C2C;
            plate(scr(c, 0f, yc, -6f), hw * rx + 1.6f, ry / rows + 0.05f, 0.3f, wall, 0, IN);
            int inside = acid ? (i % 3 == 0 ? acidB : acidA) : (i % 2 == 0 ? 0x2A0E18 : 0x331220);
            plate(scr(c, 0f, yc, -5.4f), Math.max(0.3f, hw * rx - 0.2f), ry / rows + 0.05f, 0.3f, inside,
                    acid ? ChopScene.alpha(0.25f) | 0x80FF40 : 0, IN);
        }
        // The mosquito: at the surface, then down into it.
        float sink = ease(d);
        float[] mp = scr(c, rx * 0.06f * (float) Math.sin(u / 300.0), level * ry + 2.4f - 5.5f * sink, -2.2f);
        int legs = 6 - (int) Math.min(6, Math.max(0, (u - (MQ_WINGS + 250)) / 170));
        boolean wings = u < MQ_WINGS;
        mosquito(mp, (float) Math.toRadians(70f + u * 0.02f), 0.25f * (float) Math.sin(u / 260.0), 0.35f * (float) Math.sin(u / 330.0),
                0.95f, 0f, 1f - span(u, MQ_XRAY + 400, 1200), d, legs, wings, 0f, IN);
        // Its wings, torn off, floating up and away; its legs, sinking.
        if (u >= MQ_WINGS && u < MQ_WINGS + 900) {
            float k = (u - MQ_WINGS) / 900f;
            for (int side = -1; side <= 1; side += 2) {
                float[] wp = scr(mp, side * (3f + 4f * k), 4f + 10f * k, 0.5f);
                float[][] wa = SlayerScene.axes(side * 0.6f + k * 3f, 0.4f, side * 0.9f);
                if (k < 0.85f) SlayerScene.solid(wp[0], wp[1], wp[2], 1.0f, 0.12f, 2.8f * (1f - 0.5f * k), wa[0], wa[1], wa[2], 0xC8D4E0, 0, IN);
            }
        }
        for (int i = 0; i < 6; i++) {
            long off = MQ_WINGS + 250 + i * 170L;
            if (u < off || u > off + 700) continue;
            float k = (u - off) / 700f;
            float[] a = scr(mp, (i % 2 == 0 ? 1 : -1) * (2.5f + 1.5f * k), -1.5f - 5f * k, 0.6f);
            float[] b = scr(a, (i % 2 == 0 ? 1 : -1) * 2.5f, -2.5f, 0f);
            rod(a, b, 0.35f, 0x3A2A1E, 0, IN);
        }
        // The acid in front of it: the rows below the surface again, nearer.
        for (int i = 0; i < rows; i++) {
            float v0 = -1f + i * 2f / rows, v1 = v0 + 2f / rows, vm = (v0 + v1) / 2f;
            if (vm >= level) continue;
            float hw = (float) Math.sqrt(Math.max(0f, 1f - vm * vm));
            boolean surface = vm + 2f / rows >= level;
            int col = surface ? SlayerScene.mixRgb(acidA, 0xE8FFB0, 0.6f) : (i % 3 == 1 ? acidB : acidA);
            plate(scr(c, 0f, vm * ry, 2.4f), Math.max(0.3f, hw * rx - 0.2f), ry / rows + 0.05f, 0.3f, col, ChopScene.alpha(0.3f) | 0x80FF40, IN);
        }
        // Bubbles rising through the acid, popping at the top.
        for (int i = 0; i < 9; i++) {
            float ph = ((u - MQ_XRAY) / (520f + 140f * tl.rand(1930 + i)) + tl.rand(1900 + i)) % 1f;
            float bx = (tl.rand(1960 + i) - 0.5f) * 1.5f * rx;
            float by = -ry * 0.9f + (level * ry + ry * 0.9f) * ph;
            float sz = 0.6f + 0.6f * tl.rand(1990 + i);
            plate(scr(c, bx, by, 3.2f), sz, sz, 0.3f, 0xE8FFB8, ChopScene.alpha(0.4f) | 0xFFFFFF, IN);
        }
    }

    /** The drop, spat out of the mouth in an arc onto the ground in front. */
    static float[] spitPath(long u) {
        float k = clamp((u - MQ_SPIT) / (float) (MQ_SPLAT - MQ_SPIT));
        float x = HEAD[0] + 26f * k, z = HEAD[2] + 30f * k;
        float y = HEAD[1] + 2f + 34f * k - 40f * k * k;
        return new float[]{x, Math.max(5f, y), z};
    }

    static void mosquitoFront(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        // Its needle in: drops of blood drawn up as it drinks.
        if (u > MQ_LAND + 100 && u < MQ_SNAP - 50) {
            float[] p = SlayerScene.screen(BELLY[0] + 1f, BELLY[1] + 1f, BELLY[2] + 1f);
            for (int i = 0; i < 3; i++) {
                float k = ((u - MQ_LAND) / 260f + i / 3f) % 1f;
                square(layer, p[0] + G * (i - 1), p[1] - k * G * 3, G, ChopScene.alpha(a * (1f - k)) | 0xC0181E);
            }
        }
        // The strand that reels it in, and the crunch.
        if (u > MQ_SNAP - 70 && u < MQ_SNAP + 260) {
            float[] h = SlayerScene.screen(HEAD[0], HEAD[1] + 2f, HEAD[2]);
            float[] m = SlayerScene.screen(BELLY[0] + 1f, BELLY[1] + 6f, BELLY[2] + 1f);
            float k = clamp((u - MQ_SNAP + 70) / 70f);
            float back = ease((u - MQ_SNAP) / 220f);
            float ex = m[0] + (h[0] - m[0]) * back, ey = m[1] + (h[1] - m[1]) * back;
            c.line(h[0], h[1], h[0] + (ex - h[0]) * k, h[1] + (ey - h[1]) * k, Math.max(1f, G * 0.6f), ChopScene.alpha(a * 0.9f) | 0xF4F4F4);
        }
        if (u > MQ_SNAP + 200) gore(layer, tl, new float[]{HEAD[0], HEAD[1] + 3f, HEAD[2]}, u - MQ_SNAP - 200, 26, a, 81, 0xB0141C, 0x5A1010);
        float x = xray(u);
        if (x > 0.02f) {
            // A green glow through the hide.
            float[] b = SlayerScene.screen(ABDOMEN[0], ABDOMEN[1], ABDOMEN[2]);
            ChopScene.glow(c, b[0], b[1], 90f * ChopScene.size, 0x7ADC2A, 0.32f * x * a);
        }
        // The heave and the spit: slime out of the mouth along the drop's arc, then a splat where it lands.
        if (u > MQ_SPIT - 60 && u < MQ_SPIT + 700) gore(layer, tl, new float[]{HEAD[0] + 2f, HEAD[1] + 2f, HEAD[2] + 3f}, u - MQ_SPIT + 60, 34, a, 90, 0x86DC2E, 0x4E3A1E);
        if (u > MQ_SPIT && u < MQ_SPLAT) {
            for (int i = 0; i < 7; i++) {
                float[] p = spitPath(u - i * 28L);
                float[] s = SlayerScene.screen(p[0], p[1], p[2]);
                square(layer, s[0], s[1], G * (i < 2 ? 2 : 1), ChopScene.alpha(a * (1f - i / 7f)) | (i % 2 == 0 ? 0x86DC2E : 0xC8F07A));
            }
        }
    }

    static void mosquitoBack(CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        if (u > MQ_SPLAT) {
            float[] p = spitPath(MQ_SPLAT);
            splat(layer, p[0], p[2], 12f, 0x6ABE24, u - MQ_SPLAT, a * (1f - tl.rise(ENDING + u)), tl, 7);
        }
    }

    // ================================================================== the Primordial Eye

    /** How far the camera is in on the face, 0..1. */
    static float eyeZoom(long u) {
        return ease(u / 1300f) * (1f - ease((u - 5400) / 800f));
    }

    static void eyeModels(SlayerTimeline tl, long u) {
        // The eyes go out as it dies; one stays lit - the one that comes out.
        if (u > E_DARK) dropEyes();
    }

    /** The eye's place: in the socket, bulging out of it, dragged out towards the camera, then free. */
    static float[] eyeAt(SlayerTimeline tl, long u) {
        float sz = ChopScene.size;
        float[] sock = socket();
        float[] s = SlayerScene.screen(sock[0], sock[1], sock[2]);
        float bulge = ease((u - E_BULGE) / 900f);
        float pull = u < E_PULL ? 0f : (u < E_SNAP ? ease((u - E_PULL) / (float) (E_SNAP - E_PULL)) * 0.35f : 0.35f + 0.65f * SlayerTimeline.backOut(clamp((u - E_SNAP) / 450f)));
        // Looking round, side to side, faster as the rage grows; then fixed on you.
        float look = u < E_PULL ? 55f * (float) Math.sin((u - E_BULGE) / (220.0 - 60.0 * span(u, E_BULGE, 1400))) * bulge : 0f;
        float tx = W * 0.5f, ty = H * 0.42f;
        float x = s[0] + (tx - s[0]) * pull, y = s[1] + (ty - s[1]) * pull;
        float size = (3.5f + 6.5f * bulge) * sz * (1f + 0.25f * pull) + H * 0.1f * Math.max(0f, pull - 0.35f);
        if (u > E_SNAP) {
            // Trembling with rage once it is free.
            float tr = 1.6f * (1f - span(u, E_SNAP + 600, 1200));
            x += tr * (float) Math.sin(u / 17.0);
            y += tr * (float) Math.cos(u / 23.0);
        }
        return new float[]{x, y, size, Math.min(1f, (u - E_BULGE) / 250f), look};
    }

    /** Where the eye sits on the face: the spider's big front eye, a little off the middle. */
    static float[] socket() {
        return new float[]{HEAD[0] + 1.5f * CR[0], HEAD[1] + 2.5f, HEAD[2] + 1.5f * CR[2] + 0f};
    }

    static void eyeFront(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        float[] head = SlayerScene.screen(HEAD[0], HEAD[1], HEAD[2]);
        float[] sock = socket();
        float[] so = SlayerScene.screen(sock[0], sock[1], sock[2]);
        // The other seven eyes going out, red to black, one by one (drawn as the model's own eyes go).
        if (u < E_DARK + 900) {
            float span8 = 6f * ChopScene.size;
            for (int i = 0; i < 8; i++) {
                long out = E_DARK + i * 95L;
                float on = u < out ? 1f : Math.max(0f, 1f - (u - out) / 140f);
                if (on <= 0f || u < E_DARK) continue;
                float x = head[0] + (i % 4 - 1.5f) * span8 * 0.5f, y = head[1] + (i / 4 - 0.5f) * span8 * 0.38f - ChopScene.size;
                square(layer, x, y, G * 2, ChopScene.alpha(a * on) | 0xE01818);
            }
        }
        if (u < E_BULGE - 300) return;
        float[] eye = eyeAt(tl, u);
        // The rage: red veins creeping out of the socket over the face, pulsing with its heartbeat.
        float beat = heartbeat(u);
        if (u < E_SNAP + 300) {
            float grow = span(u, E_BULGE - 200, 1600) * (1f - span(u, E_SNAP, 300));
            for (int r = 0; r < 9; r++) {
                float ang = r * 0.698f + tl.rand(2100 + r) * 0.4f;
                int len = (int) (grow * (6 + 6 * tl.rand(2120 + r)));
                float x = so[0], y = so[1];
                for (int j = 0; j < len; j++) {
                    ang += (tl.rand(2200 + r * 31 + j) - 0.5f) * 0.9f;
                    x += (float) Math.cos(ang) * G;
                    y += (float) Math.sin(ang) * G;
                    square(layer, x, y, G, ChopScene.alpha(a * (0.6f + 0.4f * beat) * (1f - j / (float) (len + 3))) | (j < 3 ? 0xFF3030 : 0xB01020));
                }
            }
        }
        // The nerve: from the socket to the eye, thick and pulsing, stretching thin; it snaps at E_SNAP.
        if (u > E_PULL - 200 && u < E_SNAP + 450) {
            boolean snapped = u > E_SNAP;
            int n = 30;
            float tension = clamp((u - E_PULL) / (float) (E_SNAP - E_PULL));
            for (int i = 0; i < n; i++) {
                float k = i / (float) (n - 1);
                float x0 = so[0], y0 = so[1], x1 = eye[0], y1 = eye[1];
                if (snapped) {
                    // The two ends whip back to where they hang from.
                    float back = ease((u - E_SNAP) / 280f);
                    if (k > 0.5f) {
                        float kk = (1f - k) * 2f;
                        float xx = x1 + (so[0] - x1) * 0f, yy = y1;
                        float x = xx + (float) Math.sin(kk * 3 + u / 40.0) * G * 2 * (1f - back);
                        float y = yy + eye[2] * 0.4f + kk * eye[2] * 0.6f * (1f - back) + back * kk * G * 4;
                        if (back < 1f) square(layer, x, y, G, ChopScene.alpha(a * (1f - back)) | 0xC81E2E);
                        continue;
                    }
                    k *= 1f - back;
                }
                float sag = (float) Math.sin(k * Math.PI) * (1f - tension) * G * 6;
                float wob = (float) Math.sin(k * Math.PI * 4 + u / 35.0) * G * (0.5f + 1.5f * tension);
                float x = x0 + (x1 - x0) * k + wob, y = y0 + (y1 - y0) * k + sag;
                int col = (i % 5 == 0) ? 0xFF8A9A : ((i % 5 == 2) ? 0x8A0E1A : 0xC81E2E);
                square(layer, x, y, G * (tension > 0.75f ? 1 : 2), ChopScene.alpha(a) | col);
            }
        }
        if (u > E_SNAP && u < E_SNAP + 1300) {
            gore(layer, tl, sock, u - E_SNAP, 110, a, 100, 0xA0141A, 0x5A0A10);
            if (u < E_SNAP + 220) ChopScene.glow(c, so[0], so[1], 130f * ChopScene.size, 0xFF2020, 0.8f * a * (1f - (u - E_SNAP) / 220f));
        }
        // Its stare once it is free: rings of red pulsing out with each heartbeat, and the edges of the screen reddening.
        float free = span(u, E_SNAP + 200, 400) * (1f - tl.rise(ENDING + u));
        if (free > 0f) {
            for (int r = 0; r < 3; r++) {
                float k = (((u - E_SNAP - 200) / 950f) + r / 3f) % 1f;
                ring(layer, eye[0], eye[1], eye[2] * 0.55f + H * 0.35f * k, ChopScene.alpha(a * free * (1f - k) * 0.6f) | 0xFF2A2A);
            }
            vignette(layer, 0xA00A10, a * free * (0.35f + 0.25f * beat));
        }
        ChopScene.glow(c, eye[0], eye[1], eye[2] * 3f, 0xFF3020, (0.25f + 0.25f * beat) * a * span(u, E_BULGE, 400));
    }

    /** A heartbeat, 0..1, quickening from the bulge to the snap and slowing after. */
    static float heartbeat(long u) {
        float rate = u < E_SNAP ? 700f - 380f * span(u, E_BULGE, E_SNAP - E_BULGE) : 420f;
        float ph = (u % (long) rate) / rate;
        return (float) Math.exp(-ph * 7f) + 0.55f * (float) Math.exp(-Math.abs(ph - 0.3f) * 14f);
    }

    // ================================================================== the Brick Red Dye: the Kiln

    static final int BRICK = 0xCB4154;
    /** Bricks a course, courses of wall, then the dome's courses. */
    static final int PER_COURSE = 16, WALL = 5, DOME = 3;
    static final float KILN_R = 44f, BRICK_H = 7.2f;

    /** How stopped the world is, 0..1. */
    static float frozen(long u) {
        return span(u, B_FREEZE, 450) * (1f - span(u, B_BLOW, 600));
    }

    /** Brick {@code i}'s course. */
    static int course(int i) {
        return i / PER_COURSE;
    }

    static int bricks() {
        return (WALL + DOME) * PER_COURSE + 1;
    }

    /** When brick {@code i} lands: a course at a time, faster and faster. */
    static long landAt(int i) {
        int n = bricks();
        float k = i / (float) (n - 1);
        // Front of each course first is not how a kiln is built, but the eye reads it: round from the back.
        return B_RAIN + Math.round((B_CLOSED - B_RAIN) * (1f - (1f - k) * (1f - k) * 0.55f - 0.45f * (1f - k)) );
    }

    static long courseLanded(int course) {
        int last = Math.min(bricks() - 1, (course + 1) * PER_COURSE - 1);
        return landAt(last) + 40;
    }

    /** Brick {@code i}'s resting place: {x, y, z, yaw, scale} - a ring of courses, the dome drawing in, a cap stone. */
    static float[] brickRest(int i) {
        int n = bricks();
        if (i == n - 1) return new float[]{CHEST[0], (WALL + DOME) * BRICK_H + 3f, CHEST[2], 0f, 0.9f};
        int cs = course(i), j = i % PER_COURSE;
        float r = KILN_R;
        float sc = 1f;
        float yy = cs * BRICK_H + BRICK_H / 2f;
        if (cs >= WALL) {
            int d = cs - WALL + 1;
            r = KILN_R * (1f - 0.27f * d);
            sc = 1f - 0.2f * d;
            yy = WALL * BRICK_H + (d - 0.5f) * BRICK_H * 0.85f;
        }
        float ang = (j + (cs % 2 == 0 ? 0f : 0.5f)) * 6.2832f / PER_COURSE + 0.2f;
        return new float[]{CHEST[0] + (float) Math.cos(ang) * r, yy, CHEST[2] + (float) Math.sin(ang) * r, ang, sc};
    }

    /** How hot the kiln is, 0..1: from the firing to the blast, ramping, flickering. */
    static float heat(long u) {
        return span(u, B_FIRE, B_BLOW - B_FIRE - 200);
    }

    static void kiln(SlayerTimeline tl, long u) {
        if (u < B_RAIN - 500) return;
        int n = bricks();
        float h = heat(u);
        for (int i = 0; i < n; i++) {
            long land = landAt(i);
            if (u < land - 450) continue;
            float[] r = brickRest(i);
            float x = r[0], y = r[1], z = r[2], yaw = r[3], sc = r[4];
            float tumble = 0f, pitch = 0f;
            if (u < land) {
                // Falling out of the sky, turning.
                float k = (u - land + 450) / 450f;
                y += 170f * (1f - k * k);
                tumble = (1f - k) * (2f + 2f * tl.rand(2400 + i));
            } else if (u > B_BLOW) {
                // Blown out, the front ones at the camera.
                float b = (u - B_BLOW) / 1000f;
                float dx = x - CHEST[0], dz = z - CHEST[2];
                float dl = (float) Math.sqrt(dx * dx + dz * dz) + 1e-3f;
                float sp = 190f + 120f * tl.rand(2500 + i);
                x += dx / dl * sp * b;
                z += dz / dl * sp * b + 70f * b;
                y += (110f + 60f * tl.rand(2550 + i)) * b - 260f * b * b;
                tumble = b * (7f + 6f * tl.rand(2600 + i));
                pitch = b * (4f + 3f * tl.rand(2650 + i));
                if (b > 1.1f || y < -40f) continue;
            } else if (u < land + 120) {
                // A little settle as it lands.
                y += 1.5f * (1f - (u - land) / 120f);
            }
            float[][] a = SlayerScene.axes(-yaw + 1.5708f + tumble * 0.4f, pitch, tumble);
            // Heat: towards the glow of a firing kiln, each brick its own flicker.
            int add = 0;
            if (h > 0f && u < B_BLOW) {
                float fl = 0.75f + 0.25f * (float) Math.sin(u / (55.0 + 20 * tl.rand(2700 + i)) + i);
                add = ChopScene.alpha(Math.min(0.85f, h * 0.65f * fl)) | SlayerScene.mixRgb(0xFF4018, 0xFFD070, h * h);
            }
            SlayerScene.box(x, y, z, 9.4f * sc, BRICK_H * 0.5f * sc, 4.6f * sc, a[0], a[1], a[2], BRICK_UV, SlayerScene.SLOT_BRICKS, 0xFFFFFF, add, POLYS);
        }
    }

    /** One brick of bricks.png on each face: its top-left brick, 8 x 4 texels of 16. */
    static final float[] ONE_BRICK = {0f, 0f, 0.5f, 0.25f};
    static final float[][] BRICK_UV = {ONE_BRICK, ONE_BRICK, ONE_BRICK, ONE_BRICK, ONE_BRICK, ONE_BRICK};

    static void kilnFront(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        float fz = frozen(u);
        // The freeze: a grey ring sweeping out from the corpse, and a hush - the heartbeats are the sound's.
        if (u > B_FREEZE && u < B_FREEZE + 700) {
            float k = (u - B_FREEZE) / 700f;
            float[] at = SlayerScene.screen(CHEST[0], 4f, CHEST[2]);
            ring(layer, at[0], at[1], H * (0.05f + 0.9f * k), ChopScene.alpha(a * (1f - k) * 0.7f) | 0xD8D8E0);
        }
        // Dust as each brick lands.
        int n = bricks();
        for (int i = 0; i < n; i++) {
            long s = u - landAt(i);
            if (s < 0 || s > 260 || u > B_BLOW) continue;
            float[] p = brickRest(i);
            float[] sc = SlayerScene.screen(p[0], p[1] - 3f, p[2]);
            float k = s / 260f;
            for (int j = 0; j < 4; j++) {
                square(layer, sc[0] + (j - 1.5f) * G * 2.5f * (0.4f + k), sc[1] - k * G * 2, G, ChopScene.alpha(a * (1f - k) * 0.8f) | 0xB8ACA0);
            }
        }
        float h = heat(u);
        float[] mid = SlayerScene.screen(CHEST[0], WALL * BRICK_H * 0.6f, CHEST[2]);
        if (h > 0f && u < B_BLOW) {
            // Firing: the glow through the joints, embers and heat rising off the dome.
            ChopScene.glow(c, mid[0], mid[1], (110f + 60f * h) * ChopScene.size, 0xFF5020, a * (0.25f + 0.45f * h) * (0.85f + 0.15f * (float) Math.sin(u / 70.0)));
            int embers = (int) (40 * h);
            for (int i = 0; i < embers; i++) {
                float ph = ((u - B_FIRE) / (700f + 400f * tl.rand(2800 + i)) + tl.rand(2850 + i)) % 1f;
                float ang = tl.rand(2900 + i) * 6.283f;
                float rr = KILN_R * (0.2f + 0.8f * tl.rand(2950 + i)) * (1f - 0.5f * ph);
                float[] p = SlayerScene.screen(CHEST[0] + (float) Math.cos(ang) * rr, (WALL + DOME) * BRICK_H * (0.6f + 1.4f * ph), CHEST[2] + (float) Math.sin(ang) * rr);
                square(layer, p[0] + (float) Math.sin(ph * 9 + i) * G * 2, p[1], G, ChopScene.alpha(a * (1f - ph)) | (i % 3 == 0 ? 0xFFE080 : 0xFF6A20));
            }
            // Cracks of white light across the kiln's face in the last of it.
            float cr = span(u, B_BLOW - 900, 800);
            if (cr > 0f) {
                for (int r = 0; r < 7; r++) {
                    float ang = -2.6f + r * 0.75f + tl.rand(3000 + r) * 0.3f;
                    int len = (int) (cr * (10 + 8 * tl.rand(3020 + r)));
                    float x = mid[0], y = mid[1];
                    for (int j = 0; j < len; j++) {
                        ang += (tl.rand(3100 + r * 37 + j) - 0.5f) * 0.7f;
                        x += (float) Math.cos(ang) * G * 1.5f;
                        y += (float) Math.sin(ang) * G * 1.2f;
                        square(layer, x, y, G * (j < 4 ? 2 : 1), ChopScene.alpha(a) | (j < 5 ? 0xFFFFF0 : 0xFFD080));
                    }
                }
            }
        }
        // The blast: a flash, a shockwave, brick dust and grit; colour comes back from the middle out.
        if (u > B_BLOW && u < B_BLOW + 350) ChopScene.glow(c, mid[0], mid[1], 260f * ChopScene.size, 0xFFB080, 0.95f * a * (1f - (u - B_BLOW) / 350f));
        if (u > B_BLOW && u < B_BLOW + 800) {
            float k = (u - B_BLOW) / 800f;
            ring(layer, mid[0], mid[1], H * (0.08f + 0.75f * k), ChopScene.alpha(a * (1f - k)) | 0xFFD0A0);
            ring(layer, mid[0], mid[1], H * (0.04f + 0.55f * k), ChopScene.alpha(a * (1f - k)) | BRICK);
        }
        if (u > B_BLOW) gore(layer, tl, new float[]{CHEST[0], 20f, CHEST[2] + 10f}, u - B_BLOW, 90, a, 60, BRICK, 0x7A2A2A);
        // The world's colour returning: a brick-red wave out from where it blew.
        float back = span(u, B_BLOW, 600);
        if (back > 0f && back < 1f) ring(layer, mid[0], mid[1], H * 1.1f * back, ChopScene.alpha(a * 0.6f * (1f - back)) | 0xFF8070);
        if (fz > 0f && u < B_BLOW) frost(layer, tl, u, a * fz);
    }

    /** The stopped world's edges: grey flecks hanging still in the air at the sides of the screen. */
    static void frost(CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        for (int i = 0; i < 46; i++) {
            float side = tl.rand(3200 + i) < 0.5f ? 0.03f + 0.17f * tl.rand(3250 + i) : 0.8f + 0.17f * tl.rand(3250 + i);
            float x = W * side, y = H * (0.08f + 0.84f * tl.rand(3300 + i));
            square(layer, x, y, G * (i % 4 == 0 ? 2 : 1), ChopScene.alpha(a * 0.5f) | 0xC8C8D0);
        }
    }

    // ================================================================== the Shriveled Wasp

    /** The web strung across the corpse's raised legs, and where the wasp hangs in it. */
    static float[] webCentre() {
        return new float[]{BELLY[0] - 4f, BELLY[1] + 26f, BELLY[2] + 8f};
    }

    static float[] waspPos(long u) {
        float[] w = webCentre();
        if (u >= WS_CAUGHT) return w;
        float k = clamp(u / (float) WS_CAUGHT);
        float e = k * k * (3f - 2f * k);
        float sx = 150f, sy = 80f, sz = 10f;
        float x = sx + (w[0] - sx) * e, y = sy + (w[1] - sy) * e, z = sz + (w[2] - sz) * e;
        y += 8f * (float) Math.sin(u / 70.0) * (1f - k);
        return new float[]{x, y, z};
    }

    static void wasp(SlayerTimeline tl, long u) {
        if (u < 0 || u > tl.arrive() - ENDING - 300) return;
        float drain = span(u, WS_DRAIN, WS_GLINT - WS_DRAIN - 200);
        float[] p = waspPos(u);
        float struggle = u < WS_CAUGHT ? 0f : (1f - drain) * (1.4f + 0.8f * (float) Math.sin(u / 47.0));
        // Notably large: half as big again as a bee. Drained, it greys and shrinks, and curls.
        int col = SlayerScene.mixRgb(0xFFFFFF, 0x6E6A60, drain);
        float sc = 1.6f * (1f - 0.35f * drain);
        float wings = u < WS_CAUGHT ? u / 6f : (1f - drain) * u / 14f;
        float yaw = u < WS_CAUGHT ? -95f : -60f + 25f * (float) Math.sin(u / 90.0) * (1f - drain);
        SlayerScene.addTurned(rigs.bee(wings, yaw, -15f + 30f * drain), sc,
                p[0] + struggle * (float) Math.sin(u / 23.0), p[1] + struggle * (float) Math.cos(u / 31.0), p[2], 0.5f * drain, -1, col, 0, POLYS);
    }

    static void waspFront(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        float[] w = webCentre();
        float[] s = SlayerScene.screen(w[0], w[1], w[2]);
        float fade = 1f - span(u, tl.arrive() - ENDING - 500, 300);
        // The web: spokes out to the legs' tips and the ground, its spiral; it flashes white where the wasp hits.
        float hit = impulse(u, WS_CAUGHT);
        float shiver = u > WS_CAUGHT ? (1f - span(u, WS_DRAIN, 1200)) * 1.2f : 0f;
        web(c, layer, s[0], s[1], H * 0.2f, 9, 5, u, shiver, ChopScene.alpha(a * fade * (0.55f + 0.45f * hit)) | 0xF0F0F0, tl, 0);
        // Drained: red pulled out of it along the strands, to the edge of the web and down to the corpse's fangs.
        if (u > WS_DRAIN && u < WS_GLINT) {
            float[] f = SlayerScene.screen(HEAD[0], HEAD[1], HEAD[2]);
            for (int i = 0; i < 10; i++) {
                float k = ((u - WS_DRAIN) / 520f + i / 10f) % 1f;
                float x = s[0] + (f[0] - s[0]) * k + (float) Math.sin(k * 9 + i) * G;
                float y = s[1] + (f[1] - s[1]) * k;
                square(layer, x, y, G * (i % 3 == 0 ? 2 : 1), ChopScene.alpha(a * (1f - k * 0.5f)) | (i % 2 == 0 ? 0xB0141C : 0x7A0A10));
            }
        }
        // The stinger's glint, intact.
        if (u > WS_GLINT && u < WS_GLINT + 450) {
            float k = (u - WS_GLINT) / 450f;
            float gx = s[0] - 9f * ChopScene.size, gy = s[1] + 4f * ChopScene.size;
            star(layer, gx, gy, 5, a * (1f - k), 0xFFFFFF);
        }
    }

    // ================================================================== the Ensnared Snail

    static float[] snailPos(long u) {
        float k = clamp(u / (float) SN_SHOT);
        return new float[]{CHEST[0] - 62f + 18f * k, 0f, CHEST[2] + 30f};
    }

    static void snail(SlayerTimeline tl, long u) {
        if (u < 0 || u > tl.arrive() - ENDING - 250) return;
        float[] p = snailPos(u);
        float wrap = span(u, SN_SHOT + 120, SN_WRAPPED - SN_SHOT - 120);
        float spin = u > SN_SHOT ? ease((u - SN_SHOT) / 700f) * 6.283f * 1.5f : 0f;
        int foot = SlayerScene.mixRgb(0xC9B486, 0xEDEDED, wrap);
        int shell = SlayerScene.mixRgb(0x9A6232, 0xF2F2F2, wrap);
        int dark = SlayerScene.mixRgb(0x5E3418, 0xD8D8D8, wrap);
        float creep = (float) Math.sin(u / 110.0) * (1f - wrap) * 0.6f;
        float[][] ax = SlayerScene.axes(spin + 0.2f, 0f, 0f);
        float[] o = {p[0], 0f, p[2]};
        float s = 1.3f;
        if (wrap < 0.95f) {
            part(o, ax, 0, 1.4f * s, creep * s, 2.6f * s, 1.4f * s, 7.5f * s, foot, 0, POLYS);
            part(o, ax, 0, 2.6f * s, 6.2f * s, 2.2f * s, 1.6f * s, 1.6f * s, foot, 0, POLYS);
            // Eye stalks, with dark tips.
            for (int side = -1; side <= 1; side += 2) {
                float[] base = at(o, ax, side * 1.2f * s, 3.6f * s, 6.6f * s);
                float[] tip = at(o, ax, side * 2.2f * s, 7.4f * s, 8.4f * s);
                rod(base, tip, 0.6f * s, foot, 0, POLYS);
                SlayerScene.solid(tip[0], tip[1], tip[2], 0.7f * s, 0.7f * s, 0.7f * s, ax[0], ax[1], ax[2], 0x2A1A10, 0, POLYS);
            }
        }
        // The shell: a coil of boxes, the outer whorl big, the inner smaller and higher.
        for (int i = 0; i < 4; i++) {
            float r = (3.6f - i * 0.8f) * s;
            part(o, ax, 0, (5.4f + i * 0.9f) * s, (-1.6f + i * 0.7f) * s, 2.6f * s * (1f - 0.12f * i), r, r, i % 2 == 0 ? shell : dark, 0, POLYS);
        }
        // Wrapped: a cocoon of silk round the whole of it.
        if (wrap > 0f) {
            float g = ease(wrap);
            part(o, ax, 0, 5f * s, 0f, 3.6f * s * g, 5.2f * s * g, 8.6f * s * g, 0xF4F4F4, ChopScene.alpha(0.15f) | 0xFFFFFF, POLYS);
        }
    }

    static void snailFront(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        float[] p = snailPos(u);
        float[] s = SlayerScene.screen(p[0], 7f, p[2]);
        float lw = Math.max(1f, G * 0.5f);
        float fade = 1f - span(u, tl.arrive() - ENDING - 500, 300);
        // Its trail of slime, glinting.
        if (u < SN_SHOT + 400) {
            for (int i = 0; i < 14; i++) {
                float[] q = SlayerScene.screen(CHEST[0] - 62f - 6f + i * 1.4f * Math.min(1f, u / (float) SN_SHOT) * 1.4f, 0.3f, CHEST[2] + 30f);
                square(layer, q[0], q[1], G, ChopScene.alpha(a * 0.5f * (1f - span(u, SN_SHOT, 400))) | ((i + u / 120) % 5 == 0 ? 0xF0FFF0 : 0x9AB0A0));
            }
        }
        // Silk shot from the spinnerets at the end of the corpse's belly; then wound round and round.
        float[] from = SlayerScene.screen(BELLY[0], BELLY[1], BELLY[2] - 6f);
        for (int i = 0; i < 6; i++) {
            long at = SN_SHOT - 180 + i * 60L;
            if (u < at || u > SN_WRAPPED + 300) continue;
            float k = Math.min(1f, (u - at) / 140f);
            float tx = s[0] + (i - 2.5f) * G * 1.5f, ty = s[1] + (i % 2) * G * 2;
            c.line(from[0], from[1], from[0] + (tx - from[0]) * k, from[1] + (ty - from[1]) * k, lw,
                    ChopScene.alpha(a * 0.85f * (1f - span(u, SN_WRAPPED, 300))) | 0xF4F4F4);
        }
        float wrap = span(u, SN_SHOT + 120, SN_WRAPPED - SN_SHOT - 120);
        if (wrap > 0f && wrap < 1f) {
            for (int j = 0; j < 10; j++) {
                float ang = j * 1.3f + u / 50f;
                square(layer, s[0] + (float) Math.cos(ang) * G * 6, s[1] + (float) Math.sin(ang) * G * 3, G, ChopScene.alpha(a) | 0xFFFFFF);
            }
        }
        // Its revenge in the next life: the ghost rises out of the cocoon, turns to the corpse with red eyes, dives in.
        if (u > SN_GHOST && u < SN_DIVE + 500) {
            float rise = ease((u - SN_GHOST) / 500f);
            float dive = ease((u - SN_DIVE) / 380f);
            float[] gc = SlayerScene.screen(p[0], 7f + 34f * rise, p[2]);
            float[] tgt = SlayerScene.screen(CHEST[0], CHEST[1], CHEST[2]);
            float gx = gc[0] + (tgt[0] - gc[0]) * dive + (float) Math.sin(u / 160.0) * G * 2 * (1f - dive);
            float gy = gc[1] + (tgt[1] - gc[1]) * dive;
            float ga = a * Math.min(1f, (u - SN_GHOST) / 250f) * (1f - span(u, SN_DIVE + 300, 200));
            ghostSnail(layer, gx, gy, G * 1.6f * (1f + 0.3f * dive), ga, u > SN_DIVE - 450, dive > 0f);
            ChopScene.glow(c, gx, gy, 60f * ChopScene.size, 0x9AF0E0, 0.35f * ga);
        }
        if (u > SN_DIVE + 150 && u < SN_DIVE + 900) {
            float[] tgt = SlayerScene.screen(CHEST[0], CHEST[1], CHEST[2]);
            ring(layer, tgt[0], tgt[1], H * (0.03f + 0.3f * span(u, SN_DIVE + 150, 700)), ChopScene.alpha(a * (1f - span(u, SN_DIVE + 150, 700))) | 0xA8F8E8);
            gore(layer, tl, CHEST, u - SN_DIVE - 150, 40, a, 120, 0xA8F8E8, 0x3A3632);
        }
    }

    /** A pixel ghost of a snail, facing left (towards the corpse), red-eyed when it means it. */
    static void ghostSnail(CrystalVeins.Fill layer, float x, float y, float cell, float a, boolean angry, boolean diving) {
        String[] g = {
                ".#...#......",
                ".#...#......",
                ".##.##..###.",
                "..###..#####",
                "..####.##.##",
                ".######.####",
                "############",
                ".##########."};
        for (int r = 0; r < g.length; r++) {
            for (int q = 0; q < g[r].length(); q++) {
                if (g[r].charAt(q) != '#') continue;
                boolean eye = r <= 1;
                int col = eye && angry ? 0xFF2020 : (r >= 6 ? 0xB8F0E8 : 0xE0FFF8);
                square(layer, x + (q - 6) * cell, y + (r - 4) * cell, cell, ChopScene.alpha(a * (eye && angry ? 1f : 0.62f)) | col);
            }
        }
    }

    // ================================================================== the Vial of Venom

    /** The vial on the screen: x, y of its middle, its height. */
    static float[] vialScreen() {
        float[] s = SlayerScene.screen(HEAD[0] + 10f * CR[0], 2f, HEAD[2] + 10f * CR[2] + 14f);
        return new float[]{s[0], s[1] - H * 0.075f, H * 0.15f};
    }

    static void venomFront(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        float[] v = vialScreen();
        float hh = v[2], ww = hh * 0.5f;
        float in = SlayerTimeline.backOut(clamp(u / 300f));
        float fill = span(u, V_DRIP + 200, V_CORK - V_DRIP - 450);
        float fade = 1f - span(u, V_CORK + 100, 250);
        int green = 0x7CFF40;
        float cell = Math.max(1f, Math.round(G));
        // Drops off the fangs, one after another, faster, into the vial's mouth.
        float[] f = SlayerScene.screen(HEAD[0] + 2f, HEAD[1] - 1f, HEAD[2] + 6f);
        float mx = v[0], my = v[1] - hh / 2f;
        if (u > V_DRIP && u < V_CORK - 150) {
            for (int i = 0; i < 9; i++) {
                float period = 420f - 240f * span(u, V_DRIP, 1200);
                float k = ((u - V_DRIP) / period + i / 9f) % 1f;
                float x = f[0] + (mx - f[0]) * k, y = f[1] + (my - f[1]) * k * k;
                square(layer, x, y, G * (i % 3 == 0 ? 2 : 1), ChopScene.alpha(a) | (i % 2 == 0 ? green : 0xC8FFA0));
            }
            // A thread of venom hanging from the fang.
            for (int j = 0; j < 3; j++) square(layer, f[0], f[1] + j * G, G, ChopScene.alpha(a) | green);
        }
        if (fade <= 0f || in <= 0f) return;
        float va = a * fade;
        // The vial: drawn as pixel art over everything, its glass, the venom rising, its meniscus, bubbles, the cork.
        float w2 = ww * in, h2 = hh * in;
        float x0 = Math.round((mx - w2 / 2f) / cell) * cell, y0 = Math.round((v[1] - h2 / 2f) / cell) * cell;
        int cols = Math.max(4, Math.round(w2 / cell)), rws = Math.max(6, Math.round(h2 / cell));
        int neck = Math.max(2, cols / 3);
        int liquidTop = rws - Math.round((rws - 3) * fill);
        for (int r = 0; r < rws; r++) {
            boolean neckRow = r < 3;
            int lo = neckRow ? (cols - neck) / 2 : 0, hi = neckRow ? lo + neck - 1 : cols - 1;
            for (int q = lo; q <= hi; q++) {
                boolean edge = q == lo || q == hi || r == rws - 1 || (r == 3 && (q < (cols - neck) / 2 || q > (cols - neck) / 2 + neck - 1));
                int col;
                float al;
                if (edge) {
                    col = q == lo ? 0xF4FCFF : 0xA8C0D0;
                    al = 0.95f;
                } else if (r >= liquidTop) {
                    boolean top = r == liquidTop;
                    col = top ? 0xC8FF90 : (((q + r) % 4 == 0) ? 0x5ADC2A : 0x3EAA1A);
                    al = 0.92f;
                } else {
                    col = q == lo + 1 ? 0xFFFFFF : 0xB8D8E8;
                    al = q == lo + 1 ? 0.35f : 0.12f;
                }
                c.fill(x0 + q * cell, y0 + r * cell, cell, cell, ChopScene.alpha(va * al) | col);
            }
        }
        // Bubbles in it.
        if (fill > 0.1f) {
            for (int i = 0; i < 5; i++) {
                float k = ((u / 480f) + i * 0.2f) % 1f;
                int r = rws - 2 - Math.round((rws - 2 - liquidTop) * k);
                int q = 1 + Math.round((cols - 3) * tl.rand(1800 + i));
                if (r > liquidTop) c.fill(x0 + q * cell, y0 + r * cell, cell, cell, ChopScene.alpha(va * 0.9f) | 0xE8FFC8);
            }
        }
        // Fumes over its mouth: green skulls of steam, the poison's own particle swirl.
        if (fill > 0.4f) {
            for (int i = 0; i < 8; i++) {
                float k = ((u / 900f) + i / 8f) % 1f;
                float x = mx + (float) Math.sin(k * 7 + i) * cell * 3, y = y0 - k * hh * 0.8f;
                square(layer, x, y, G, ChopScene.alpha(va * (1f - k) * 0.7f) | (i % 2 == 0 ? 0x4E9A2A : 0x8ACC5A));
            }
        }
        // The cork pressed in.
        if (u > V_CORK - 320) {
            float k = ease((u - V_CORK + 320) / 320f);
            float cyy = y0 - (1f - k) * hh * 0.5f - cell * 1.5f + cell;
            int cw = neck + 2;
            for (int q = 0; q < cw; q++) {
                for (int r = 0; r < 2; r++) {
                    c.fill(x0 + ((cols - cw) / 2 + q) * cell, cyy + r * cell, cell, cell, ChopScene.alpha(va) | (r == 0 ? 0xB07A44 : 0x8A5A2E));
                }
            }
        }
        ChopScene.glow(c, mx, v[1], hh * 1.8f, green, 0.3f * va * fill);
    }

    // ================================================================== the Tarantula Talisman

    /** Which of the ten hits has landed (-1 none). */
    static int talismanHit(long u) {
        if (u < TL_FIRST) return -1;
        int k = (int) ((u - TL_FIRST) / TL_STEP);
        if (k >= 9) return u >= TL_TENTH ? 9 : 8;
        return k;
    }

    static void talismanFront(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        float ts = Math.min(1.35f, H / 300f);
        float[] mid = SlayerScene.screen(BELLY[0], BELLY[1] + 6f, BELLY[2]);
        // Ten hits, the count above the corpse; the tenth crits pink and big.
        for (int k = 0; k < 10; k++) {
            long at = TL_FIRST + k * TL_STEP + (k == 9 ? 250 : 0);
            long s = u - at;
            if (s < 0 || s > 500) continue;
            float f = s / 500f;
            boolean tenth = k == 9;
            float ang = (k % 3 - 1) * 0.7f + (tenth ? 0.2f : 0f);
            if (s < 130) slash(layer, mid[0], mid[1], H * (tenth ? 0.34f : 0.2f), ang, s / 130f, a, tenth ? 0xFF55FF : 0xFFFFFF);
            float x = mid[0] + (k % 2 == 0 ? 1 : -1) * H * (0.08f + 0.02f * k), y = mid[1] - H * 0.14f - f * H * 0.08f;
            float pop = 1f + 0.6f * Math.max(0f, 1f - s / 100f);
            float alpha = (f < 0.7f ? 1f : 1f - (f - 0.7f) / 0.3f) * a;
            if (tenth) c.text("✧" + (k + 1) + "! +10%✧", W / 2f, mid[1] - H * 0.2f - f * H * 0.05f, ts * 1.6f * pop, ChopScene.alpha(alpha) | 0xFF55FF);
            else c.text("" + (k + 1), x, y, ts * 1.1f * pop, ChopScene.alpha(alpha) | 0xFFFF55);
        }
        // The web: spun over the corpse as the hits land, drawn tight into a pendant after the tenth.
        float spin = span(u, 0, TL_TENTH);
        float tight = ease((u - TL_TENTH - 100) / 450f);
        float[] at = SlayerScene.screen(BELLY[0], BELLY[1] + 14f, BELLY[2] + 6f);
        float r = H * 0.28f * (1f - 0.88f * tight);
        float fade = 1f - span(u, TL_TENTH + 550, 150);
        web(c, layer, at[0], at[1], r * Math.min(1f, spin * 1.6f), 8, Math.max(1, (int) (spin * 6)), u, 0f,
                ChopScene.alpha(a * 0.8f * fade) | 0xF0F0F0, tl, 1);
        if (u > TL_TENTH + 450 && u < TL_TENTH + 850) ChopScene.glow(c, at[0], at[1], 90f * ChopScene.size, 0xAA00AA, 0.7f * a * (1f - (u - TL_TENTH - 450) / 400f));
        if (u > TL_TENTH && u < TL_TENTH + 700) gore(layer, tl, BELLY, u - TL_TENTH, 30, a, 130, 0x8AC23A, 0x3A3A2A);
    }

    // ================================================================== the Fly Swatter

    /** How flat the corpse is pressed by the swat, 0..1. */
    static float squash(long u) {
        if (u < SWAT) return 0f;
        return (float) Math.exp(-(u - SWAT) / 260.0);
    }

    static void swatter(SlayerTimeline tl, long u) {
        if (u < 600 || u > SWAT + 1100) return;
        // Raised behind, wound back, then down flat onto the corpse - and lifted away.
        float wind = ease((u - 600) / 700f);
        float down = u < SWAT - 150 ? 0f : ease((u - SWAT + 150) / 150f);
        float lift = span(u, SWAT + 450, 500);
        float ang = 1.25f * wind * (1f - down) + (1f - wind) * 0.6f + 1.2f * lift;
        float[][] a = SlayerScene.axes(-0.5f, ang, 0f);
        float hz = 34f;
        float[] pivot = {CHEST[0] - 10f, 3f, CHEST[2] + 54f};
        float[] centre = {pivot[0] - a[2][0] * hz, pivot[1] - a[2][1] * hz + 2f, pivot[2] - a[2][2] * hz};
        // The pad: a grid of holes over a red rim, then the handle.
        SlayerScene.box(centre[0], centre[1], centre[2], 18f, 0.9f, 15f, a[0], a[1], a[2], SlayerScene.ALL, SlayerScene.SLOT_COBWEB, 0xC8302A, 0, POLYS);
        SlayerScene.solid(centre[0], centre[1], centre[2], 18.8f, 0.6f, 15.8f, a[0], a[1], a[2], 0x8A1E1A, 0, POLYS);
        float[] h0 = {pivot[0] - a[2][0] * 10f, pivot[1] - a[2][1] * 10f, pivot[2] - a[2][2] * 10f};
        SlayerScene.solid(h0[0], h0[1], h0[2], 1.1f, 1.1f, 11f, a[0], a[1], a[2], 0xC89A5A, 0, POLYS);
        SlayerScene.solid(pivot[0], pivot[1], pivot[2], 1.6f, 1.6f, 2.4f, a[0], a[1], a[2], 0x3A3A3A, 0, POLYS);
    }

    static void swatterFront(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        int n = 6 + (int) (8 * span(u, 0, 1200));
        for (int i = 0; i < n; i++) {
            float ph = u / 1000f * (2.4f + tl.rand(1700 + i)) + i;
            float x = BELLY[0] + 22f * (float) Math.sin(ph * 2.1f) + (tl.rand(1720 + i) - 0.5f) * 14f;
            float y = BELLY[1] + 6f + 10f * (float) Math.cos(ph * 3.3f) + 8f * tl.rand(1740 + i);
            float z = BELLY[2] + 16f * (float) Math.cos(ph * 1.7f);
            if (u > SWAT) {
                if (u > SWAT + 1000) continue;
                y = BELLY[1] + 0.5f;
            }
            float[] s = SlayerScene.screen(x, y, z);
            square(layer, s[0], s[1], G * (u > SWAT ? 2 : 1), ChopScene.alpha(a) | (u > SWAT ? 0x3A4A12 : 0x101010));
            if (u < SWAT && (u / 40 + i) % 2 == 0) {
                square(layer, s[0] + G, s[1] - G, G, ChopScene.alpha(a * 0.55f) | 0xD8E8FF);
                square(layer, s[0] - G, s[1] - G, G, ChopScene.alpha(a * 0.55f) | 0xD8E8FF);
            }
        }
        if (u > SWAT - 120 && u < SWAT) {
            // The swing's smear.
            float[] p = SlayerScene.screen(BELLY[0] - 6f, BELLY[1] + 30f, BELLY[2] + 30f);
            float k = (u - SWAT + 120) / 120f;
            slash(layer, p[0], p[1] + k * H * 0.1f, H * 0.4f, 0.15f, k, a * 0.6f, 0xFFFFFF);
        }
        if (u > SWAT && u < SWAT + 700) {
            gore(layer, tl, new float[]{BELLY[0], BELLY[1], BELLY[2]}, u - SWAT, 50, a, 70, 0x6A8A1E, 0x2A3A0E);
            ChopScene.glow(c, SlayerScene.screen(BELLY[0], BELLY[1], BELLY[2])[0], SlayerScene.screen(BELLY[0], BELLY[1], BELLY[2])[1],
                    80f * ChopScene.size, 0xFFFFFF, 0.5f * a * (1f - (u - SWAT) / 700f));
        }
        if (u > SWAT) {
            float[] s = SlayerScene.screen(BELLY[0], BELLY[1] + 24f, BELLY[2]);
            float k = span(u, SWAT, 650);
            if (k < 1f) c.text("SWAT!", s[0], s[1] - k * H * 0.05f, Math.min(1.35f, H / 300f) * (2.2f + 0.8f * (1f - k)), ChopScene.alpha(a * (1f - k * k)) | 0xFFFFFF);
        }
    }

    // ================================================================== Darkness Within

    static float darkness(long u) {
        return span(u, D_DARK, 500) * (1f - span(u, D_IGNITE + 700, 500));
    }

    static void darknessFront(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long t, long u, float a) {
        float dark = darkness(u);
        if (dark <= 0.01f) return;
        // The dark closing in from the edges.
        vignette(layer, 0x000000, a * dark * 0.85f);
        // The eight eyes, red in the dark, going out one by one from the outside in.
        float[] e = SlayerScene.screen(HEAD[0], HEAD[1], HEAD[2] + 6f);
        float span8 = 7f * ChopScene.size;
        int[] order = {0, 7, 3, 4, 1, 6, 2, 5};
        for (int i = 0; i < 8; i++) {
            long out = D_EYES + (long) i * 180;
            float on = u < out ? 1f : Math.max(0f, 1f - (u - out) / 110f);
            if (u > D_IGNITE) on = 0f;
            if (on <= 0f) continue;
            int slot = order[i];
            float x = e[0] + (slot % 4 - 1.5f) * span8 * 0.5f, y = e[1] + (slot / 4 - 0.5f) * span8 * 0.38f;
            square(layer, x, y, G * 2, ChopScene.alpha(a * dark * on) | 0xFF2020);
            ChopScene.glow(c, x, y, 16f * ChopScene.size, 0xFF2020, 0.45f * dark * on * a);
        }
        // The dark itself: a swirl of black smoke drawn in round the spot, then the rune lights violet in its middle.
        float[] r = SlayerScene.screen(HEAD[0], HEAD[1] + 22f, HEAD[2]);
        if (u > D_EYES + 1200 && u < D_IGNITE + 900) {
            float k = clamp((u - D_EYES - 1200) / (float) (D_IGNITE - D_EYES - 1200));
            for (int i = 0; i < 48; i++) {
                float ang = i * 6.283f / 48f * 3f + u / 160f + tl.rand(3500 + i);
                float rr = H * (0.42f - 0.32f * k) * (0.5f + 0.5f * tl.rand(3550 + i)) * (u > D_IGNITE ? 1f + 2f * span(u, D_IGNITE, 600) : 1f);
                float al = a * (u > D_IGNITE ? 1f - span(u, D_IGNITE, 600) : Math.min(1f, k * 3f));
                square(layer, r[0] + (float) Math.cos(ang) * rr, r[1] + (float) Math.sin(ang) * rr * 0.7f, G * (i % 5 == 0 ? 3 : 2),
                        ChopScene.alpha(al * 0.9f) | (i % 4 == 0 ? 0x2A1A3A : 0x0A0810));
            }
        }
        if (u > D_IGNITE - 100 && u < D_IGNITE + 500) {
            float k = (u - D_IGNITE + 100) / 600f;
            ChopScene.glow(c, r[0], r[1], 160f * ChopScene.size * (0.5f + k), 0x7A2AC0, 0.8f * a * (1f - k));
            ring(layer, r[0], r[1], H * (0.03f + 0.4f * k), ChopScene.alpha(a * (1f - k)) | 0x9A4AE0);
        }
    }

    // ================================================================== the catalysts: a cocoon

    static void cocoon(SlayerTimeline tl, long u) {
        float grow = span(u, 150, 550);
        if (grow <= 0f || u > C_SPLIT + 60) return;
        float throb = u > 800 ? 0.06f * (float) Math.max(0, Math.sin((u - 800) / 85.0)) : 0f;
        float shake = u > 1100 ? 1.0f * (float) Math.sin(u / 26.0) * span(u, 1100, 400) : 0f;
        float[][] a = SlayerScene.axes(0.4f, 0f, 0.08f * shake);
        float g = grow * (1f + throb);
        int glow = ChopScene.alpha(0.35f * span(u, 700, 700) * (0.6f + 0.4f * (float) Math.sin(u / 85.0))) | tl.drop().colour;
        SlayerScene.box(CHEST[0], 12f * g, CHEST[2], 12f * g, 12f * g, 17f * g, a[0], a[1], a[2], SlayerScene.ALL,
                SlayerScene.SLOT_COBWEB, 0xF4F4F4, glow, POLYS);
        SlayerScene.solid(CHEST[0], 12f * g, CHEST[2], 11.6f * g, 11.6f * g, 16.6f * g, a[0], a[1], a[2], 0xDADADA, glow, POLYS);
    }

    static void cocoonFront(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        float[] at = SlayerScene.screen(CHEST[0], 12f, CHEST[2]);
        float lw = Math.max(1f, G * 0.5f);
        if (u > 50 && u < 800) {
            // Strands drawn in from all round, wrapping it.
            for (int i = 0; i < 12; i++) {
                float ang = i * 0.524f;
                float k = clamp((u - 50 - i * 25) / 320f);
                float r = H * 0.34f * (1f - k);
                c.line(at[0] + (float) Math.cos(ang) * r, at[1] + (float) Math.sin(ang) * r * 0.8f, at[0], at[1], lw,
                        ChopScene.alpha(a * 0.7f * (1f - span(u, 600, 200))) | 0xF0F0F0);
            }
        }
        if (u > 700 && u < C_SPLIT) ChopScene.glow(c, at[0], at[1], 70f * ChopScene.size, tl.drop().colour, 0.3f * a * span(u, 700, 600));
        if (u > C_SPLIT && u < C_SPLIT + 700) {
            float k = (u - C_SPLIT) / 700f;
            ChopScene.glow(c, at[0], at[1], 140f * ChopScene.size, tl.drop().colour, 0.8f * a * (1f - k));
            ring(layer, at[0], at[1], H * (0.04f + 0.4f * k), ChopScene.alpha(a * (1f - k)) | tl.drop().colour);
            gore(layer, tl, new float[]{CHEST[0], 12f, CHEST[2]}, u - C_SPLIT, 50, a, 140, 0xF4F4F4, 0xC8C8C8);
        }
    }

    // ================================================================== the layers

    /** Under the models: the shadow, pools, the slime. */
    static void back(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long t) {
        long u = t - ENDING;
        float a = ChopScene.sceneAlpha;
        float[] sh = SlayerScene.screen(CHEST[0], 0f, CHEST[2]);
        if (corpse(tl, t) > 0.1f && tl.ending() != Ending.DARKNESS) {
            c.sprite(com.sbcs.render.art.ChopArt.TEX_GLOW, sh[0], sh[1], 80f * ChopScene.size, 24f * ChopScene.size, 0f, ChopScene.alpha(0.45f * a) | 0x000000);
        }
        switch (tl.ending()) {
            case MOSQUITO -> mosquitoBack(layer, tl, u, a);
            case EYE -> {
                if (u > E_SNAP) pool(layer, HEAD[0], HEAD[2] + 6f, 20f * ease((u - E_SNAP) / 1600f), 0x6A0A10, 0x8A1218, a * (1f - tl.rise(t)) * 0.9f, 21);
            }
            case SWATTER -> {
                if (u > SWAT) splat(layer, BELLY[0], BELLY[2], 30f, 0x5A7A1E, u - SWAT, a, tl, 0);
            }
            case VENOM -> {
                // Venom spilt on the ground under the fang, hissing.
                float k = span(u, V_DRIP + 300, 1500);
                if (k > 0f) pool(layer, HEAD[0] + 6f, HEAD[2] + 10f, 9f * k, 0x3E8A1A, 0x6ACC2A, a * 0.8f * (1f - tl.rise(t)), 33);
            }
            default -> { }
        }
    }

    /** Over the models: webs, slime, blood, the vial, the glyphs. */
    static void front(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long t) {
        long u = t - ENDING;
        float a = ChopScene.sceneAlpha;
        switch (tl.ending()) {
            case MOSQUITO -> mosquitoFront(c, layer, tl, u, a);
            case EYE -> eyeFront(c, layer, tl, u, a);
            case BRICK -> kilnFront(c, layer, tl, u, a);
            case WASP -> waspFront(c, layer, tl, u, a);
            case SNAIL -> snailFront(c, layer, tl, u, a);
            case VENOM -> venomFront(c, layer, tl, u, a);
            case TALISMAN -> talismanFront(c, layer, tl, u, a);
            case SWATTER -> swatterFront(c, layer, tl, u, a);
            case DARKNESS -> darknessFront(c, layer, tl, t, u, a);
            case COCOON -> cocoonFront(c, layer, tl, u, a);
            default -> { }
        }
    }

    // ================================================================== where the drop is before it rises

    /** {screen x, y, size px, alpha, yaw}, or null while it is not there yet. */
    static float[] itemAt(SlayerTimeline tl, long t) {
        long u = t - ENDING;
        float sz = ChopScene.size;
        switch (tl.ending()) {
            case MOSQUITO: {
                if (u < MQ_SPIT) return null;
                float[] p = spitPath(u);
                float[] s = SlayerScene.screen(p[0], p[1], p[2]);
                float spin = u < MQ_SPLAT ? (u - MQ_SPIT) * 0.9f : 0f;
                float squish = u > MQ_SPLAT && u < MQ_SPLAT + 160 ? 1f + 0.25f * (1f - (u - MQ_SPLAT) / 160f) : 1f;
                return new float[]{s[0], s[1], 13f * sz * span(u, MQ_SPIT, 120) * squish, 1f, spin};
            }
            case EYE: {
                if (u < E_BULGE) return null;
                return eyeAt(tl, u);
            }
            case BRICK: {
                if (u < B_BLOW + 80) return null;
                float k = span(u, B_BLOW + 80, 500);
                float[] s = SlayerScene.screen(CHEST[0], 20f, CHEST[2]);
                return new float[]{s[0], s[1], 20f * sz * SlayerTimeline.backOut(k), k, 30f + 200f * (1f - k)};
            }
            case WASP: {
                // The carcass itself becomes the drop where it hangs in the web.
                if (u < WS_GLINT - 150) return null;
                float k = span(u, WS_GLINT - 150, 300);
                float[] p = waspPos(u);
                float[] s = SlayerScene.screen(p[0], p[1], p[2]);
                return new float[]{s[0], s[1], 15f * sz * k, k, 30f};
            }
            case SNAIL: {
                if (u < SN_WRAPPED) return null;
                float k = span(u, SN_WRAPPED, 250);
                float[] p = snailPos(u);
                float[] s = SlayerScene.screen(p[0], 7f, p[2]);
                return new float[]{s[0], s[1], 14f * sz * k, k, 30f};
            }
            case SWATTER: {
                long at = SWAT + 650;
                if (u < at) return null;
                float k = span(u, at, 300);
                float[] s = SlayerScene.screen(BELLY[0], BELLY[1] + 10f, BELLY[2] + 6f);
                return new float[]{s[0], s[1], 16f * sz * SlayerTimeline.backOut(k), k, 0f};
            }
            case VENOM: {
                if (u < V_CORK + 100) return null;
                float k = span(u, V_CORK + 100, 250);
                float[] v = vialScreen();
                return new float[]{v[0], v[1], v[2] * 1.3f, k, 30f};
            }
            case TALISMAN: {
                long at = TL_TENTH + 450;
                if (u < at) return null;
                float k = span(u, at, 300);
                float[] s = SlayerScene.screen(BELLY[0], BELLY[1] + 14f, BELLY[2] + 6f);
                return new float[]{s[0], s[1], 16f * sz * SlayerTimeline.backOut(k), k, 30f};
            }
            case DARKNESS: {
                if (u < D_IGNITE) return null;
                float k = span(u, D_IGNITE, 400);
                float[] s = SlayerScene.screen(HEAD[0], HEAD[1] + 22f, HEAD[2]);
                return new float[]{s[0], s[1], 16f * sz * SlayerTimeline.backOut(k), k, 30f};
            }
            case COCOON: {
                if (u < C_SPLIT) return null;
                float k = span(u, C_SPLIT, 300);
                float[] s = SlayerScene.screen(CHEST[0], 12f + 10f * k, CHEST[2]);
                return new float[]{s[0], s[1], 16f * sz * SlayerTimeline.backOut(k), k, 30f};
            }
            default:
                return null;
        }
    }

    /** Behind the drop in its showcase: rays in its colour, bigger for the RNGesus drops and the dye. */
    static void showcaseBehind(ChopScene.Canvas c, SlayerTimeline tl, long t, float x, float y, float size, float show) {
        if (show <= 0f) return;
        int col = tl.drop().colour;
        boolean big = tl.drop().tier == SlayerTimeline.Tier.RNGESUS;
        ChopScene.glow(c, x, y, size * (big ? 3.4f : 2.6f), col, 0.5f * show);
        int n = big ? 14 : 10;
        float g = Math.max(1f, Math.round(G));
        float turn = t / 2800f;
        float len = H * (big ? 0.5f : 0.36f);
        for (int i = 0; i < n; i++) {
            float ang = turn + i * (float) Math.PI * 2f / n;
            float cs = (float) Math.cos(ang), sn = (float) Math.sin(ang);
            for (float r = size * 0.7f; r < len; r += g * 1.5f) {
                float fall = 1f - r / len;
                int cc = i % 2 == 0 ? col : SlayerScene.mixRgb(col, 0xFFFFFF, 0.4f);
                float s = g * (1 + (fall > 0.6f ? 1 : 0));
                c.fill(Math.round((x + cs * r) / g) * g - s / 2f, Math.round((y + sn * r * 0.8f) / g) * g - s / 2f, s, s,
                        ChopScene.alpha(show * ChopScene.sceneAlpha * fall * fall * 0.7f) | cc);
            }
        }
        // The Eye keeps staring: a slow red pulse round it.
        if (tl.ending() == Ending.EYE) ChopScene.glow(c, x, y, size * 2.2f, 0xFF2020, 0.25f * show * heartbeat(t - ENDING));
    }

    // ================================================================== effects

    static void square(CrystalVeins.Fill layer, float x, float y, float s, int argb) {
        SlayerScene.square(layer, x, y, s, argb);
    }

    /** A web on the screen: {@code spokes} strands out from (x, y), {@code rings} turns of spiral, swaying. */
    static void web(ChopScene.Canvas c, CrystalVeins.Fill layer, float x, float y, float r, int spokes, int rings, long u,
                    float shiver, int argb, SlayerTimeline tl, int seed) {
        if ((argb >>> 24) < 4 || r < 2f) return;
        float lw = Math.max(1f, G * 0.5f);
        float[] ex = new float[spokes], ey = new float[spokes];
        for (int i = 0; i < spokes; i++) {
            float ang = i * 6.2832f / spokes + 0.2f + 0.15f * tl.rand(3600 + seed * 50 + i);
            float len = r * (0.85f + 0.3f * tl.rand(3700 + seed * 50 + i));
            ex[i] = (float) Math.cos(ang) * len;
            ey[i] = (float) Math.sin(ang) * len * 0.85f;
            float sx = shiver * G * (float) Math.sin(u / 30.0 + i);
            c.line(x + sx, y, x + ex[i] + sx, y + ey[i], lw, argb);
        }
        for (int ringI = 1; ringI <= rings; ringI++) {
            float k = ringI / (rings + 0.7f);
            for (int i = 0; i < spokes; i++) {
                int j = (i + 1) % spokes;
                float sx = shiver * G * (float) Math.sin(u / 30.0 + i + ringI);
                // Each thread sags a little between the spokes.
                float mx = (ex[i] + ex[j]) / 2f * k * 0.92f, my = (ey[i] + ey[j]) / 2f * k * 0.92f;
                c.line(x + ex[i] * k + sx, y + ey[i] * k, x + mx + sx, y + my, lw, argb);
                c.line(x + mx + sx, y + my, x + ex[j] * k + sx, y + ey[j] * k, lw, argb);
            }
        }
    }

    /** A four-pointed star of squares (a glint). */
    static void star(CrystalVeins.Fill layer, float x, float y, int arm, float a, int rgb) {
        for (int i = -arm; i <= arm; i++) {
            float f = 1f - Math.abs(i) / (arm + 1f);
            square(layer, x + i * G, y, G, ChopScene.alpha(a * f) | rgb);
            if (i != 0) square(layer, x, y + i * G, G, ChopScene.alpha(a * f) | rgb);
        }
    }

    /** A slash of squares along a line through (x, y), growing along its length then gone. */
    static void slash(CrystalVeins.Fill layer, float x, float y, float len, float ang, float k, float a, int rgb) {
        float dx = (float) Math.cos(ang) * len, dy = (float) Math.sin(ang) * len;
        int n = 20;
        for (int j = 0; j < n; j++) {
            float f = j / (float) (n - 1);
            if (f > k * 1.6f || f < k * 1.6f - 0.9f) continue;
            float w = (float) Math.sin(f * Math.PI);
            square(layer, x - dx / 2f + dx * f, y - dy / 2f + dy * f, G * (1 + Math.round(w * 1.5f)), ChopScene.alpha(a * (1f - k)) | (w > 0.6f ? 0xFFFFFF : rgb));
        }
    }

    /** A ring of squares. */
    static void ring(CrystalVeins.Fill layer, float x, float y, float r, int argb) {
        int n = Math.max(12, (int) (r * 6.283f / (G * 1.6f)));
        for (int i = 0; i < n; i++) {
            float ang = i * 6.283f / n;
            square(layer, x + (float) Math.cos(ang) * r, y + (float) Math.sin(ang) * r * 0.8f, G, argb);
        }
    }

    /** The screen's edges darkened in {@code rgb}, stepped in from the sides in pixel bands. */
    static void vignette(CrystalVeins.Fill layer, int rgb, float a) {
        if (a <= 0.02f) return;
        float g = Math.max(1f, Math.round(G)) * 2;
        int bands = 6;
        for (int b = 0; b < bands; b++) {
            float al = a * (1f - b / (float) bands) * 0.7f;
            int argb = ChopScene.alpha(al) | rgb;
            float in = b * g;
            layer.fill(in, in, W - 2 * in, g, argb);
            layer.fill(in, H - in - g, W - 2 * in, g, argb);
            layer.fill(in, in + g, g, H - 2 * in - 2 * g, argb);
            layer.fill(W - in - g, in + g, g, H - 2 * in - 2 * g, argb);
        }
    }

    /** One burst of squares in the world: out from {@code at}, falling, stopping on the ground, fading. */
    static void gore(CrystalVeins.Fill layer, SlayerTimeline tl, float[] at, long since, int n, float a, int seedBase, int c1, int c2) {
        if (since < 0 || since > 1400) return;
        float k = since / 1000f;
        for (int i = 0; i < n; i++) {
            float r1 = tl.rand(seedBase * 997 + i), r2 = tl.rand(seedBase * 991 + i + 300), r3 = tl.rand(seedBase * 983 + i + 600);
            float ang = r1 * 6.283f;
            float sp = 30f + 60f * r2;
            float vx = (float) Math.cos(ang) * sp, vz = (float) Math.sin(ang) * sp * 0.7f + 20f;
            float vy = 40f + 70f * r3;
            float x = at[0] + vx * k, z = at[2] + vz * k;
            float y = at[1] + vy * k - 180f * k * k;
            if (y < 0.5f) y = 0.5f;
            float life = 1f - k / 1.4f;
            float[] s = SlayerScene.screen(x, y, z);
            square(layer, s[0], s[1], G * (r2 > 0.7f ? 2 : 1), ChopScene.alpha(a * life) | (r3 > 0.35f ? c1 : c2));
        }
    }

    /** A pool on the ground: an ellipse of grid squares, ragged at its edge. */
    static void pool(CrystalVeins.Fill layer, float cx, float cz, float r, int c1, int c2, float a, int seed) {
        if (r <= 0.5f || a <= 0.02f) return;
        float step = G / ChopScene.size;
        for (float dz = -r; dz <= r; dz += step) {
            for (float dx = -r * 1.3f; dx <= r * 1.3f; dx += step) {
                float d = (dx * dx) / (r * r * 1.69f) + (dz * dz) / (r * r);
                int hx = Math.round((cx + dx) / step), hz = Math.round((cz + dz) / step);
                int hsh = hx * 73856093 ^ hz * 19349663 ^ seed;
                float edge = 0.8f + ((hsh >>> 8) & 255) / 255f * 0.3f;
                if (d > edge) continue;
                float[] s = SlayerScene.screen(cx + dx, 0.2f, cz + dz);
                square(layer, s[0], s[1], G, ChopScene.alpha(a) | (((hsh >>> 3) & 3) == 0 ? c2 : c1));
            }
        }
    }

    static void splat(CrystalVeins.Fill layer, float cx, float cz, float r, int col, long since, float a, SlayerTimeline tl, int seed) {
        float k = Math.min(1f, since / 120f);
        pool(layer, cx, cz, r * k, col, 0x2A3A0E, a * (1f - span(since, 1200, 600)), seed);
        for (int i = 0; i < 10; i++) {
            float ang = i * 0.628f + tl.rand(820 + i);
            float rr = r * (1.2f + 0.8f * tl.rand(840 + i)) * k;
            float[] s = SlayerScene.screen(cx + (float) Math.cos(ang) * rr, 0.2f, cz + (float) Math.sin(ang) * rr);
            square(layer, s[0], s[1], G * 2, ChopScene.alpha(a * (1f - span(since, 1200, 600))) | col);
        }
    }
}
