package com.sbcs.render;

import com.sbcs.anim.SlayerTimeline;
import com.sbcs.anim.SlayerTimeline.Ending;
import com.sbcs.anim.SlayerTimeline.Source;

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
import static com.sbcs.render.SlayerScene.square;

/**
 * Every drop's own ending, between the kill and the showcase - each one about its item, as Matej asked
 * (2026-10-25: "if it's a digested mosquito, then it should have a mosquito being digested ... a warden and the heart
 * is getting ripped out of it ... it can be a little bit gory"). The lore behind each is the item's own (NEU's text):
 *
 * <ul>
 *   <li><b>Beheaded Horror</b>: the killing blow takes the head clean off; it rolls, and turns into the trophy.</li>
 *   <li><b>Snake Rune</b>: snakes pour out of the corpse, circle it, and braid into the rune.</li>
 *   <li><b>Festering Maggot</b> - "a large, inflamed maggot, gorged on the flesh of the Revenant Horror ... no strength
 *       to drag itself": maggots burst out of its chest, and one swells until it can barely move.</li>
 *   <li><b>Scythe Blade</b>: three sweeps of a scythe, then its blade comes down and sticks in the ground.</li>
 *   <li><b>Shredded Sinew</b> (the Atoned Horror's): it is shredded, and one glowing sinew is drawn out until it snaps.</li>
 *   <li><b>Severed Hand</b>: its arm is torn off, and the hand crawls - and leaps at you.</li>
 *   <li><b>Warden Heart</b> (the Atoned Horror's, RNGesus Incarnate): the corpse sinks, sculk spreads, the Warden
 *       digs its way out - the game's own emerge and roar - its heart beats brighter, and is torn out of its chest.</li>
 *   <li><b>Matcha Dye</b>: time stops and the world goes grey; matcha drips, floods out from the corpse and colours
 *       everything it reaches; the corpse dissolves into it and the dye rises out of the pool.</li>
 *   <li>The catalysts (a burst of their colour), the books (glyphs drawn in) and the Paragon Shard (crystals out of the
 *       ground) - the last two the Tarantula's as well.</li>
 * </ul>
 * The Tarantula Broodfather's own endings are {@link BroodEndings}: every hook here hands a Tarantula drop over to it
 * first, except the shared BOOK and PARAGON.
 */
final class SlayerEndings {
    private SlayerEndings() {}

    // ================================================================== the hooks the stage asks

    /** A Tarantula drop with an ending of its own (BroodEndings) - the shared BOOK and PARAGON stay here. */
    static boolean brood(SlayerTimeline tl) {
        return tl.drop().source == Source.TARANTULA && tl.ending() != Ending.BOOK && tl.ending() != Ending.PARAGON;
    }

    /** The corpse's visibility: 1 lying there, 0 gone (it sinks into the ground between). */
    static float corpse(SlayerTimeline tl, long t) {
        if (brood(tl)) return BroodEndings.corpse(tl, t);
        long u = t - ENDING;
        return switch (tl.ending()) {
            case PLAIN, BOOK -> 1f - span(t, ENDING + 200, 500);
            case PARAGON -> 1f - span(t, ENDING + 900, 500);
            case WARDEN -> 1f - span(t, ENDING, 700);
            case MATCHA -> 1f - span(t, ENDING + 5600, 1000);
            default -> 1f - span(t, tl.arrive() - 600, 500);
        };
    }

    /** How far the death tip goes (1 vanilla); an ending that has its own plan for the body may stop it. */
    static float tipScale(SlayerTimeline tl, long t) {
        if (brood(tl)) return BroodEndings.tipScale(tl, t);
        return 1f;
    }

    static int lose(SlayerTimeline tl, long t) {
        if (brood(tl)) return 0;
        return switch (tl.ending()) {
            case BEHEAD -> t >= KILLED ? SlayerScene.LOSE_HEAD : 0;
            case HAND -> t >= ENDING + 250 ? SlayerScene.LOSE_RIGHT_ARM : 0;
            default -> 0;
        };
    }

    static float bossYaw(SlayerTimeline tl, long t) {
        if (brood(tl)) return BroodEndings.bossYaw(tl, t);
        return 0f;
    }

    static final float[] ZERO = {0f, 0f, 0f};

    static float[] bossOffset(SlayerTimeline tl, long t) {
        if (brood(tl)) return BroodEndings.bossOffset(tl, t);
        long u = t - ENDING;
        switch (tl.ending()) {
            case MAGGOT: {
                // The chest heaves as they push out.
                if (u < 0 || u > 1400) return ZERO;
                float k = (float) Math.abs(Math.sin(u / 90.0)) * (1f - u / 1400f);
                return new float[]{0f, 1.6f * k, 0f};
            }
            case SINEW:
            case HAND: {
                // Jerked with each slash.
                if (u < 0 || u > 1500) return ZERO;
                float k = (float) Math.sin(u / 40.0) * Math.max(0f, 1f - (u % 300) / 120f);
                return new float[]{0.8f * k, 0f, 0f};
            }
            default:
                return ZERO;
        }
    }

    /** The boss's own colour this frame (multiplied): drained, darkened, x-rayed. */
    static int bossTint(SlayerTimeline tl, long t) {
        if (brood(tl)) return BroodEndings.bossTint(tl, t);
        long u = t - ENDING;
        switch (tl.ending()) {
            default:
                return 0xFFFFFF;
        }
    }

    static float floorAlpha(SlayerTimeline tl, long t) {
        if (brood(tl)) return BroodEndings.floorAlpha(tl, t);
        float a = Math.min(1f, t / 250f);
        // The showcase stands alone: the ground goes as the drop rises.
        a *= 1f - 0.85f * tl.rise(t);
        return a;
    }

    static int floorTint(SlayerTimeline tl, long t) {
        if (brood(tl)) return BroodEndings.floorTint(tl, t);
        if (tl.drop().dye()) {
            float g = frozen(tl, t);
            return SlayerScene.mixRgb(0xFFFFFF, 0xA8A8B0, g);
        }
        return 0xFFFFFF;
    }

    static float extraDim(SlayerTimeline tl, long t) {
        if (brood(tl)) return BroodEndings.extraDim(tl, t);
        float d = 0f;
        if (tl.drop().dye()) d = 0.18f * frozen(tl, t);
        if (tl.ending() == Ending.WARDEN) d = 0.06f * span(t, ENDING, 800) * (1f - span(t, tl.arrive() - 400, 600));
        return d * tl.fadeOut(t);
    }

    static float shake(SlayerTimeline tl, long t) {
        if (brood(tl)) return BroodEndings.shake(tl, t);
        long u = t - ENDING;
        float s = 0f;
        switch (tl.ending()) {
            case WARDEN -> {
                if (u > 300 && u < 3300) s += 1.6f;
                if (u > 3300 && u < 5000) s += 2.4f * (float) Math.abs(Math.sin((u - 3300) / 140.0));
                s += 7f * impulse(u, RIP);
            }
            case SCYTHE -> s += 5f * impulse(u, SCYTHE_HIT);
            case MATCHA -> s += 3f * impulse(u, M_SPLASH);
            case HAND -> s += 4f * impulse(u, H_LEAP + 260);
            case PARAGON -> s += 3f * impulse(u, 500);
            default -> { }
        }
        return s;
    }

    /** 1 at {@code at}, decaying over 400 ms. */
    static float impulse(long u, long at) {
        if (u < at) return 0f;
        return (float) Math.exp(-(u - at) / 140.0) * (u - at < 600 ? 1f : 0f);
    }

    static void camera(SlayerTimeline tl, long t, int w, int h) {
        if (brood(tl)) {
            BroodEndings.camera(tl, t, w, h);
            return;
        }
        float rise = tl.rise(t);
        if (tl.ending() == Ending.WARDEN) {
            // Back and up as the Warden comes out of the ground (it is three blocks tall).
            float k = ease((t - ENDING - 300) / 1800f) * (1f - rise);
            ChopScene.size *= 1f - 0.22f * k;
            ChopScene.fy += 20f * k;
        }
    }

    static int nameColour(SlayerTimeline tl, long t) {
        if (brood(tl)) return BroodEndings.nameColour(tl, t);
        int c = tl.drop().colour;
        if (tl.drop().dye()) {
            float f = 0.5f + 0.5f * (float) Math.sin(t / 200.0);
            return SlayerScene.mixRgb(c, SlayerScene.mixRgb(c, 0xFFFFFF, 0.45f), f);
        }
        return c;
    }

    // ================================================================== the models each ending adds

    static void models(SlayerTimeline tl, long t) {
        if (brood(tl)) {
            BroodEndings.models(tl, t);
            return;
        }
        if (rigs == null) {
            grey(tl, t);
            return;
        }
        long u = t - ENDING;
        switch (tl.ending()) {
            case BEHEAD -> flyingHead(tl, t);
            case HAND -> flyingArm(tl, t, u);
            case MAGGOT -> maggots(tl, u);
            case SCYTHE -> scythe(tl, u);
            case WARDEN -> warden(tl, u);
            case PARAGON -> crystals(tl, u);
            case SNAKES -> snakes(tl, u);
            default -> { }
        }
        grey(tl, t);
    }

    // ------------------------------------------------------------------ Beheaded Horror

    /** Where the head lands, beside the body. */
    static final float[] HEAD_REST = {-30f, 4.5f, 30f};

    static void flyingHead(SlayerTimeline tl, long t) {
        if (t < KILLED || t > tl.arrive() - 200) return;
        float s = SlayerScene.bossScale(Source.REVENANT);
        float[] p = headPath(t);
        float spin = (float) Math.min(t - KILLED, 900) * 0.9f;
        // It turns into the trophy: the zombie's head shrinks away as the Beheaded Horror grows over it.
        float morph = span(t, ENDING + 1500, 500);
        if (morph >= 1f) return;
        SlayerScene.add(rigs.zombiePart(SlayerScene.LOSE_HEAD, 40f + spin * 0.4f, spin, spin * 0.25f), s * (1f - morph),
                p[0], p[1], p[2], -1, 0xFFFFFF, 0, POLYS);
    }

    /** The head's arc: off the neck at the blow, two bounces, rolling to a stop. */
    static float[] headPath(long t) {
        float k = clamp((t - KILLED) / 900f);
        float x = HEAD_REST[0] * ease(k), z = HEAD_REST[2] * ease(k);
        float y0 = 42f;
        float y;
        if (k < 0.55f) {
            float a = k / 0.55f;
            y = y0 + 22f * a - (y0 + 22f - HEAD_REST[1]) * a * a;
        } else {
            float a = (k - 0.55f) / 0.45f;
            y = HEAD_REST[1] + 8f * (float) Math.sin(a * Math.PI) * (1f - a);
        }
        return new float[]{x, y, z};
    }

    // ------------------------------------------------------------------ Severed Hand

    static final long H_TEAR = 250, H_LAND = 950, H_LEAP = 2200;

    static float[] armPath(SlayerTimeline tl, long u) {
        // Torn off at the shoulder (where the corpse's right arm was), up, down, then three hops at the camera.
        float sx = CHEST[0] - 6f, sy = CHEST[1] + 6f, sz = CHEST[2] + 6f;
        if (u < H_LAND) {
            float k = clamp((u - H_TEAR) / (float) (H_LAND - H_TEAR));
            return new float[]{sx - 30f * k, sy + 30f * k - (sy + 24f) * k * k + 3f * k, sz + 18f * k};
        }
        float bx = sx - 30f, bz = sz + 18f;
        if (u < H_LEAP) {
            float k = (u - H_LAND) / 1250f;
            float hop = (float) Math.abs(Math.sin(k * Math.PI * 3)) * 5f;
            return new float[]{bx - 4f * k, 3f + hop, bz + 16f * k};
        }
        // The leap: at the camera, growing.
        float k = clamp((u - H_LEAP) / 300f);
        return new float[]{bx - 4f, 3f + 30f * k, bz + 16f + 60f * k};
    }

    static void flyingArm(SlayerTimeline tl, long t, long u) {
        if (u < H_TEAR || u > H_LEAP + 260) return;
        float s = SlayerScene.bossScale(Source.REVENANT);
        float[] p = armPath(tl, u);
        // Tumbling through the air, then flat on the ground, hand first towards the camera, scrabbling.
        float roll = u < H_LAND ? (u - H_TEAR) * 0.7f : 12f * (float) Math.sin(u / 60.0);
        float pitch = u < H_LAND ? (u - H_TEAR) * 0.4f : -90f + 8f * (float) Math.sin(u / 45.0);
        float grow = u > H_LEAP ? 1f + 1.4f * clamp((u - H_LEAP) / 300f) : 1f;
        SlayerScene.add(rigs.zombiePart(SlayerScene.LOSE_RIGHT_ARM, u < H_LAND ? 70f : 150f, pitch, roll), s * 1.25f * grow, p[0], p[1], p[2], -1, 0xFFFFFF, 0, POLYS);
    }

    // ------------------------------------------------------------------ Festering Maggot

    static final long MG_BIG = 1500, MG_POP = 2600;

    static void maggots(SlayerTimeline tl, long u) {
        if (u < 150) return;
        int maggot = 0xF2E4B4;
        // Seven small ones crawl off the body, wriggling; the big one swells where it came out.
        for (int i = 0; i < 7; i++) {
            long b = 150 + i * 90;
            if (u < b) continue;
            float k = (u - b) / 1000f;
            float ang = i * 0.9f + tl.rand(i) * 0.6f;
            float r = 6f + 26f * Math.min(1f, k);
            float y = CHEST[1] + Math.max(-CHEST[1] + 1.5f, 10f * (1f - k * 3f));
            float x = CHEST[0] + (float) Math.cos(ang) * r, z = CHEST[2] + (float) Math.sin(ang) * r;
            if (u > tl.arrive() - ENDING - 400) continue;
            SlayerScene.add(rigs.silverfish(u / 50f + i * 7, (float) Math.toDegrees(ang) + 90f), 0.7f, x, y, z, -1, maggot, 0, POLYS);
        }
        if (u < MG_BIG || u > MG_POP) return;
        float swell = ease((u - MG_BIG) / (float) (MG_POP - MG_BIG));
        float throb = 1f + 0.08f * (float) Math.sin(u / 55.0) * swell;
        int col = SlayerScene.mixRgb(maggot, 0xD87A6A, swell * 0.6f);
        SlayerScene.add(rigs.silverfish(u / 160f, 60f), (0.9f + 2.4f * swell) * throb, CHEST[0], CHEST[1] + 2f, CHEST[2] + 4f, -1, col,
                ChopScene.alpha(0.2f * swell) | 0xFF3010, POLYS);
    }

    // ------------------------------------------------------------------ Scythe Blade

    static final long[] SWEEPS = {0, 380, 760};
    static final long SCYTHE_HIT = 1500;

    static void scythe(SlayerTimeline tl, long u) {
        // The blade itself: a long diamond-blue edge falling point first, then stuck in the ground, quivering.
        if (u < 1050 || u > tl.arrive() - ENDING - 250) return;
        float k = clamp((u - 1050) / (float) (SCYTHE_HIT - 1050));
        float y = 140f - 140f * k * k;
        float wob = u > SCYTHE_HIT ? 0.12f * (float) Math.exp(-(u - SCYTHE_HIT) / 220.0) * (float) Math.sin((u - SCYTHE_HIT) / 25.0) : 0f;
        float[][] a = SlayerScene.axes(0.6f, 0.15f + wob, 0.35f);
        int blue = 0x7FE8E0;
        float cx = 26f, cz = 8f;
        // A curved blade: three segments, each turned a little more.
        for (int s = 0; s < 3; s++) {
            float[][] b = SlayerScene.axes(0.6f, 0.15f + wob, 0.35f + s * 0.38f);
            float off = s * 11f;
            SlayerScene.solid(cx + b[1][0] * off, y + 14f + b[1][1] * off, cz + b[1][2] * off, 1.2f, 6f, 4f - s * 0.8f, b[0], b[1], b[2],
                    s == 0 ? 0xC8FFFA : blue, ChopScene.alpha(0.25f) | 0x40FFF0, POLYS);
        }
        SlayerScene.solid(cx, y + 2f, cz, 1.6f, 2.5f, 1.6f, a[0], a[1], a[2], 0x5A3A22, 0, POLYS);
    }

    // ------------------------------------------------------------------ the Warden

    static final long W_EMERGE = 500, W_ROAR = 3600, RIP = 5600, W_FALL = 5800;

    static void warden(SlayerTimeline tl, long u) {
        if (u < W_EMERGE) return;
        float emerge = u < W_ROAR ? (u - W_EMERGE) * 2.1f : -1f;
        float roar = u >= W_ROAR && u < RIP ? (u - W_ROAR) * 1.15f : (u >= RIP ? 1900f : -1f);
        float tip = u > W_FALL ? Math.min(1f, (float) Math.sqrt((u - W_FALL) / 1000f * 1.6f)) : 0f;
        boolean heart = u < RIP;
        float s = 1.55f;
        java.util.List<float[]> rig = rigs.warden(BOSS_YAW_W, emerge, roar, tip, heart);
        int from = POLYS.size();
        // The heart beats brighter and faster as the roar goes on (an emissive layer, so it lights itself).
        float beat = heartBeat(u);
        int add = 0;
        SlayerScene.add(rig, s, 0f, 0f, -6f, -1, 0xFFFFFF, add, POLYS);
        for (int i = from; i < POLYS.size(); i++) {
            SlayerScene.Poly p = POLYS.get(i);
            p.cut = 0f;
            if (p.slot == SlayerScene.SLOT_HEART) p.add = ChopScene.alpha(0.55f * beat) | 0xFF8A50;
            // It dissolves into the sculk as it falls: sinking after it lands.
            if (u > W_FALL + 700) {
                float d = (u - W_FALL - 700) * 0.035f;
                p.p[1] -= d;
                p.p[4] -= d;
                p.p[7] -= d;
            }
        }
        // Where its chest is, for the rip.
        SlayerScene.centroid(from, 0, WARDEN_CHEST);
    }

    static final float BOSS_YAW_W = -20f;
    static final float[] WARDEN_CHEST = new float[3];

    static float heartBeat(long u) {
        if (u < W_ROAR - 600) return 0.2f;
        float rate = 520f - 260f * clamp((u - W_ROAR) / 2000f);
        float ph = ((u - W_ROAR + 600) % (long) rate) / rate;
        float b = (float) Math.exp(-ph * 6f) + 0.6f * (float) Math.exp(-Math.abs(ph - 0.28f) * 12f);
        return Math.min(1f, 0.3f + b * (0.4f + 0.6f * clamp((u - W_ROAR) / 1600f)));
    }

    // ------------------------------------------------------------------ Paragon

    static void crystals(SlayerTimeline tl, long u) {
        // Gold crystal spikes punch out of the ground in a ring, then shatter as the shard comes up.
        long out = 500, shatter = 1500;
        if (u < out - 200 || u > shatter + 100) return;
        for (int i = 0; i < 9; i++) {
            float k = backOutK(clamp((u - out - i * 40) / 260f));
            if (k <= 0f) continue;
            float ang = i * (float) Math.PI * 2f / 9f + 0.3f;
            float r = 22f + 10f * tl.rand(30 + i);
            float hgt = (16f + 22f * tl.rand(40 + i)) * k;
            float[][] a = SlayerScene.axes(ang, 0.35f * (float) Math.cos(ang), 0.35f * (float) Math.sin(ang));
            int col = i % 3 == 0 ? 0xFFF2B0 : 0xFFC83A;
            SlayerScene.solid((float) Math.cos(ang) * r, hgt / 2f - 2f, (float) Math.sin(ang) * r, 2.6f, hgt / 2f, 2.6f, a[0], a[1], a[2], col,
                    ChopScene.alpha(0.35f) | 0xFFE070, POLYS);
        }
    }

    static float backOutK(float v) {
        return SlayerTimeline.backOut(v);
    }



    // ------------------------------------------------------------------ the Snake Rune

    static void snakes(SlayerTimeline tl, long u) {
        if (u < 100) return;
        long coil = 1500, braid = 2300;
        for (int i = 0; i < 5; i++) {
            long b = 100 + i * 110;
            if (u < b) continue;
            float k = (u - b) / 1000f;
            float base = i * (float) Math.PI * 2f / 5f + tl.rand(50 + i);
            int col = i % 2 == 0 ? 0x4C9A34 : 0x3A7A2A;
            int segs = 9;
            float pull = span(u, braid, 500);
            for (int s = 0; s < segs; s++) {
                float along = Math.max(0f, k - s * 0.06f);
                // Out from the corpse, then round it in a ring, then up into the rune's spot.
                float r, ang, y;
                if (u < coil) {
                    r = 6f + 30f * Math.min(1f, along * 1.4f);
                    ang = base + 0.9f * along + 0.25f * (float) Math.sin(along * 12f + s);
                    y = 1.4f;
                } else {
                    float c = (u - coil) / 1000f;
                    r = 34f - 4f * Math.min(1f, c);
                    ang = base + 0.9f + c * 2.4f - s * 0.12f;
                    y = 1.4f;
                }
                r *= 1f - pull;
                y += 50f * pull * pull;
                float x = CHEST[0] * (1f - pull) + (float) Math.cos(ang) * r, z = CHEST[2] * (1f - pull) + (float) Math.sin(ang) * r;
                float sz = s == 0 ? 1.7f : 1.3f - s * 0.06f;
                SlayerScene.solid(x, y + sz, z, sz, sz, sz, SlayerScene.AX, SlayerScene.AY, SlayerScene.AZ,
                        s == 0 ? 0x2E6A1E : col, 0, POLYS);
            }
        }
    }



    // ================================================================== time stopping

    /** How stopped the world is, 0..1 (the dyes). */
    static float frozen(SlayerTimeline tl, long t) {
        if (!tl.drop().dye()) return 0f;
        long u = t - ENDING;
        return span(u, M_FREEZE, 450) * (1f - span(u, M_THAW, 500));
    }

    static final long M_FREEZE = 200, M_DRIP = 2400, M_SPLASH = 2700, M_FLOOD = 2800, M_THAW = 6700;

    static void grey(SlayerTimeline tl, long t) {
        if (!tl.drop().dye()) return;
        SlayerScene.grey = frozen(tl, t);
        long u = t - ENDING;
        if (tl.ending() == Ending.MATCHA && u > M_FLOOD) {
            float[] p = SlayerScene.screen(CHEST[0], 2f, CHEST[2]);
            SlayerScene.paintX = p[0];
            SlayerScene.paintY = p[1];
            SlayerScene.paintR = H * 0.6f * ease((u - M_FLOOD) / 1600f);
            SlayerScene.paintRgb = 0x8AC23A;
        }
    }


    // ================================================================== the layers

    /** Under the models: pools, sculk, shadows, the web under a wasp. */
    static void back(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long t) {
        if (brood(tl)) {
            BroodEndings.back(c, layer, tl, t);
            return;
        }
        long u = t - ENDING;
        float a = ChopScene.sceneAlpha;
        // A soft shadow under the boss.
        float[] sh = SlayerScene.screen(CHEST[0], 0f, CHEST[2]);
        if (SlayerEndings.corpse(tl, t) > 0.1f) {
            c.sprite(com.sbcs.render.art.ChopArt.TEX_GLOW, sh[0], sh[1], 70f * ChopScene.size, 22f * ChopScene.size, 0f, ChopScene.alpha(0.45f * a) | 0x000000);
        }
        switch (tl.ending()) {
            case WARDEN -> sculk(layer, tl, u, a);
            case MATCHA -> matchaPool(layer, tl, u, a);
            case BEHEAD, HAND, SINEW, MAGGOT -> bloodPool(layer, tl, t, u, a);
            default -> { }
        }
    }

    /** Over the models: blood, slime, webs, sparks, glyphs, the darkness's eyes. */
    static void front(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long t) {
        if (brood(tl)) {
            BroodEndings.front(c, layer, tl, t);
            return;
        }
        long u = t - ENDING;
        float a = ChopScene.sceneAlpha;
        // The vanilla death poof, for the endings that are done with the corpse at once.
        Ending e = tl.ending();
        if (e == Ending.PLAIN || e == Ending.BOOK || e == Ending.PARAGON) poof(layer, tl, u - (e == Ending.PARAGON ? 900 : 200), a);
        switch (e) {
            case PLAIN -> burstUp(layer, tl, u, tl.drop().colour, a);
            case BOOK -> glyphs(layer, tl, u, a);
            case PARAGON -> {
                if (u > 1500) shards(layer, tl, u - 1500, 0xFFD24A, a);
            }
            case BEHEAD -> {
                // The neck spurting: from where the head came off, in pulses.
                long since = t - KILLED;
                float[] neck = {HEAD[0], HEAD[1], HEAD[2]};
                spurt(layer, tl, neck, since, 1600, a, 0);
                if (u > 1500 && u < 2100) shards(layer, tl, u - 1500, 0xAA00AA, a);
            }
            case HAND -> {
                spurt(layer, tl, new float[]{CHEST[0] - 6f, CHEST[1] + 6f, CHEST[2] + 6f}, u - H_TEAR, 1300, a, 1);
                if (u > H_LEAP + 250 && u < H_LEAP + 900) bloodSplash(layer, tl, u - H_LEAP - 250, a);
            }
            case SINEW -> sinew(c, layer, tl, u, a);
            case SCYTHE -> sweeps(layer, tl, u, a);
            case MAGGOT -> {
                if (u < 900) gore(layer, tl, CHEST, u - 150, 40, a, 2, 0x9A1A16, 0xB8C26A);
                if (u > MG_POP && u < MG_POP + 600) gore(layer, tl, new float[]{CHEST[0], CHEST[1] + 4f, CHEST[2] + 4f}, u - MG_POP, 50, a, 3, 0xD8C890, 0x8A1A16);
            }
            case WARDEN -> wardenFront(c, layer, tl, u, a);
            case MATCHA -> matchaFront(c, layer, tl, u, a);
            case SNAKES -> {
                if (u > 2700 && u < 3300) shards(layer, tl, u - 2700, 0x55FF55, a);
            }
        }
    }

    // ================================================================== where the drop is before it rises

    /** {screen x, y, size px, alpha, yaw}, or null while it is not there yet. */
    static float[] itemAt(SlayerTimeline tl, long t) {
        if (brood(tl)) return BroodEndings.itemAt(tl, t);
        long u = t - ENDING;
        float sz = ChopScene.size;
        switch (tl.ending()) {
            case BEHEAD: {
                // The trophy grows over the zombie's head where it lies.
                float morph = span(t, ENDING + 1500, 500);
                if (morph <= 0f) return null;
                float[] p = headPath(t);
                float[] s = SlayerScene.screen(p[0], p[1], p[2]);
                return new float[]{s[0], s[1], 14f * sz * morph, morph, 40f};
            }
            case HAND: {
                if (u < H_LEAP + 250) return null;
                float k = span(u, H_LEAP + 250, 350);
                return new float[]{W / 2f, H * 0.45f, H * (0.5f - 0.32f * k), 1f, 10f};
            }
            case WARDEN: {
                if (u < RIP) return null;
                // Out of the chest, at the camera, then hanging there beating.
                float k = ease((u - RIP) / 500f);
                float[] s = SlayerScene.screen(WARDEN_CHEST[0], WARDEN_CHEST[1], WARDEN_CHEST[2] + 6f);
                float beat = 1f + 0.12f * (float) Math.max(0, Math.sin((u - RIP) / 140.0)) * (1f - tl.rise(t));
                float x = s[0] + (W * 0.5f - s[0]) * k, y = s[1] + (H * 0.42f - s[1]) * k;
                return new float[]{x, y, (10f * sz + 18f * sz * k) * beat, 1f, 20f};
            }
            case MATCHA: {
                long at = 6400;
                if (u < at) return null;
                float k = span(u, at, 400);
                float[] s = SlayerScene.screen(CHEST[0], 14f, CHEST[2]);
                return new float[]{s[0], s[1], 18f * sz * SlayerTimeline.backOut(k), k, 30f};
            }
            default: {
                long at = switch (tl.ending()) {
                    case PLAIN -> 350;
                    case BOOK -> 700;
                    case PARAGON -> 1500;
                    case SCYTHE -> SCYTHE_HIT + 400;
                    case SINEW -> SN_SNAP;
                    case MAGGOT -> MG_POP;
                    case SNAKES -> 2700;
                    default -> 500;
                };
                if (u < at) return null;
                float k = span(u, at, 300);
                float y = tl.ending() == Ending.SNAKES ? 50f : (tl.ending() == Ending.SINEW ? 46f : 14f);
                float[] s = tl.ending() == Ending.SCYTHE ? SlayerScene.screen(26f, 18f, 8f) : SlayerScene.screen(CHEST[0], y, CHEST[2]);
                return new float[]{s[0], s[1], 16f * sz * SlayerTimeline.backOut(k), k, 30f};
            }
        }
    }

    /** Behind the drop in its showcase: rays in its colour, the dyes' and the RNGesus drops' bigger. */
    static void showcaseBehind(ChopScene.Canvas c, SlayerTimeline tl, long t, float x, float y, float size, float show) {
        if (brood(tl)) {
            BroodEndings.showcaseBehind(c, tl, t, x, y, size, show);
            return;
        }
        if (show <= 0f) return;
        int col = tl.drop().colour;
        boolean big = tl.drop().tier == SlayerTimeline.Tier.RNGESUS;
        ChopScene.glow(c, x, y, size * (big ? 3.4f : 2.6f), col, 0.5f * show);
        int n = big ? 14 : 10;
        float u = Math.max(1f, Math.round(G));
        float turn = t / 2800f;
        float len = H * (big ? 0.5f : 0.36f);
        for (int i = 0; i < n; i++) {
            float ang = turn + i * (float) Math.PI * 2f / n;
            float cs = (float) Math.cos(ang), sn = (float) Math.sin(ang);
            for (float r = size * 0.7f; r < len; r += u * 1.5f) {
                float fall = 1f - r / len;
                int cc = i % 2 == 0 ? col : SlayerScene.mixRgb(col, 0xFFFFFF, 0.4f);
                float s = u * (1 + (fall > 0.6f ? 1 : 0));
                c.fill(Math.round((x + cs * r) / u) * u - s / 2f, Math.round((y + sn * r * 0.8f) / u) * u - s / 2f, s, s,
                        ChopScene.alpha(show * ChopScene.sceneAlpha * fall * fall * 0.7f) | cc);
            }
        }
    }

    // ================================================================== effects

    /** Squares of one world burst: out from {@code at}, falling, bouncing on the ground, fading. */
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

    /** A jet of blood from a wound, in pulses, arcing over and landing. */
    static void spurt(CrystalVeins.Fill layer, SlayerTimeline tl, float[] from, long since, long ms, float a, int seed) {
        if (since < 0 || since > ms + 700) return;
        for (int i = 0; i < 46; i++) {
            long born = (long) (i * ms / 46f);
            long age = since - born;
            if (age < 0 || age > 650) continue;
            float pulse = 0.6f + 0.4f * (float) Math.abs(Math.sin(born / 160.0));
            float k = age / 1000f;
            float r = tl.rand(seed * 501 + i);
            float vx = (16f + 26f * r) * pulse, vy = (40f + 30f * tl.rand(seed * 503 + i)) * pulse, vz = 14f * (tl.rand(seed * 507 + i) - 0.3f);
            float x = from[0] + vx * k, y = from[1] + vy * k - 160f * k * k, z = from[2] + vz * k;
            if (y < 0.5f) {
                y = 0.5f;
            }
            float[] s = SlayerScene.screen(x, y, z);
            square(layer, s[0], s[1], G * (r > 0.6f ? 2 : 1), ChopScene.alpha(a * (1f - age / 650f * 0.6f)) | (r > 0.3f ? 0xA0141A : 0x6A0A10));
        }
    }

    static void bloodSplash(CrystalVeins.Fill layer, SlayerTimeline tl, long since, float a) {
        // On the "lens": big squares of blood thrown across the screen as the hand hits it.
        float k = since / 650f;
        for (int i = 0; i < 40; i++) {
            float ang = tl.rand(700 + i) * 6.283f, r = (0.1f + 0.5f * tl.rand(740 + i)) * H * Math.min(1f, k * 3f);
            float x = W / 2f + (float) Math.cos(ang) * r, y = H * 0.45f + (float) Math.sin(ang) * r * 0.7f + k * k * 30f;
            square(SlayerScene_layerFix(layer), x, y, G * (2 + (int) (3 * tl.rand(780 + i))), ChopScene.alpha(a * (1f - k)) | 0x8A0E14);
        }
    }

    private static CrystalVeins.Fill SlayerScene_layerFix(CrystalVeins.Fill l) {
        return l;
    }

    /** A pool spreading under the corpse. */
    static void bloodPool(CrystalVeins.Fill layer, SlayerTimeline tl, long t, long u, float a) {
        long from = tl.ending() == Ending.BEHEAD ? KILLED + 300 - ENDING : 300;
        float k = ease((u - from) / 2000f);
        if (k <= 0f) return;
        float[] at = tl.ending() == Ending.BEHEAD ? new float[]{HEAD[0], 0f, HEAD[2]} : new float[]{CHEST[0], 0f, CHEST[2]};
        pool(layer, at[0], at[2], 22f * k, 0x6A0A10, 0x8A1218, a * (1f - tl.rise(t)) * 0.9f, tl, 10);
    }

    /** A pool on the ground: an ellipse of grid squares, ragged at its edge. */
    static void pool(CrystalVeins.Fill layer, float cx, float cz, float r, int c1, int c2, float a, SlayerTimeline tl, int seed) {
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
        pool(layer, cx, cz, r * k, col, 0x2A3A0E, a * (1f - span(since, 1200, 600)), tl, seed);
        for (int i = 0; i < 10; i++) {
            float ang = i * 0.628f + tl.rand(820 + i);
            float rr = r * (1.2f + 0.8f * tl.rand(840 + i)) * k;
            float[] s = SlayerScene.screen(cx + (float) Math.cos(ang) * rr, 0.2f, cz + (float) Math.sin(ang) * rr);
            square(layer, s[0], s[1], G * 2, ChopScene.alpha(a * (1f - span(since, 1200, 600))) | col);
        }
    }

    /** Vanilla's death poof: grey-white smoke squares puffing out and up. */
    static void poof(CrystalVeins.Fill layer, SlayerTimeline tl, long since, float a) {
        if (since < 0 || since > 900) return;
        float k = since / 900f;
        for (int i = 0; i < 26; i++) {
            float r1 = tl.rand(900 + i), r2 = tl.rand(940 + i), r3 = tl.rand(980 + i);
            float x = CHEST[0] + (r1 - 0.5f) * 30f * (0.5f + k), z = CHEST[2] + (r2 - 0.5f) * 20f * (0.5f + k);
            float y = 2f + r3 * 20f + 18f * k;
            float[] s = SlayerScene.screen(x, y, z);
            int col = r1 > 0.5f ? 0xE8E8E8 : 0xB0B0B0;
            square(layer, s[0], s[1], G * (r3 > 0.5f ? 3 : 2) * (1f - 0.5f * k), ChopScene.alpha(a * (1f - k)) | col);
        }
    }

    /** A catalyst's burst: its colour going up in a column of squares, then a ring. */
    static void burstUp(CrystalVeins.Fill layer, SlayerTimeline tl, long u, int col, float a) {
        if (u < 250 || u > 1700) return;
        float k = (u - 250) / 1000f;
        for (int i = 0; i < 40; i++) {
            float r1 = tl.rand(1000 + i), r2 = tl.rand(1040 + i);
            float ang = r1 * 6.283f;
            float rr = 4f + 20f * k * r2;
            float y = 4f + 60f * k * (0.4f + r2);
            float[] s = SlayerScene.screen(CHEST[0] + (float) Math.cos(ang) * rr, y, CHEST[2] + (float) Math.sin(ang) * rr);
            square(layer, s[0], s[1], G * (r2 > 0.7f ? 2 : 1), ChopScene.alpha(a * (1f - k / 1.45f)) | (r2 > 0.5f ? col : 0xFFFFFF));
        }
    }

    /** The books: enchanting glyphs drawn in from all round to where the book appears. */
    static void glyphs(CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        if (u > 1100) return;
        float[] at = SlayerScene.screen(CHEST[0], 14f, CHEST[2]);
        for (int i = 0; i < 24; i++) {
            float k = clamp((u - i * 18) / 700f);
            if (k <= 0f || k >= 1f) continue;
            float ang = tl.rand(1100 + i) * 6.283f + k * 2f;
            float r = H * 0.35f * (1f - k);
            float x = at[0] + (float) Math.cos(ang) * r, y = at[1] + (float) Math.sin(ang) * r * 0.7f;
            square(layer, x, y, G * 2, ChopScene.alpha(a * Math.min(1f, k * 3f)) | (i % 3 == 0 ? 0xFF55FF : 0xB070FF));
        }
    }

    /** Pieces flying out from where the drop appears. */
    static void shards(CrystalVeins.Fill layer, SlayerTimeline tl, long since, int col, float a) {
        if (since < 0 || since > 700) return;
        float k = since / 700f;
        float[] at = SlayerScene.screen(CHEST[0], 14f, CHEST[2]);
        for (int i = 0; i < 30; i++) {
            float ang = tl.rand(1200 + i) * 6.283f, sp = 0.2f + 0.5f * tl.rand(1240 + i);
            float r = H * sp * ease(k);
            float x = at[0] + (float) Math.cos(ang) * r, y = at[1] + (float) Math.sin(ang) * r * 0.8f + 40f * k * k;
            square(layer, x, y, G * (i % 4 == 0 ? 2 : 1), ChopScene.alpha(a * (1f - k)) | (i % 3 == 0 ? 0xFFFFFF : col));
        }
    }

    // ------------------------------------------------------------------ Shredded Sinew

    static final long SN_PULL = 1300, SN_SNAP = 2500;

    static void sinew(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        // Five slashes across the corpse, each throwing blood.
        for (int i = 0; i < 5; i++) {
            long at = i * 220L;
            long s = u - at;
            if (s < 0 || s > 600) continue;
            float[] p = SlayerScene.screen(CHEST[0] + (i - 2) * 6f, CHEST[1], CHEST[2]);
            float ang = (i % 2 == 0 ? 0.7f : -0.7f) + 0.2f * i;
            slash(layer, p[0], p[1], 46f * ChopScene.size * 0.5f, ang, s / 140f, a, 0xFFE8E0);
            gore(layer, tl, new float[]{CHEST[0] + (i - 2) * 6f, CHEST[1], CHEST[2]}, s, 14, a, 20 + i, 0xA0141A, 0x5A0A0E);
        }
        if (u < SN_PULL - 200 || u > SN_SNAP + 500) return;
        // The sinew: drawn up out of the chest towards the drop's spot, taut and humming, then it snaps.
        float[] base = SlayerScene.screen(CHEST[0], CHEST[1], CHEST[2] + 4f);
        float[] top = SlayerScene.screen(CHEST[0], 46f, CHEST[2]);
        float pull = ease((u - SN_PULL + 200) / 900f);
        boolean snapped = u > SN_SNAP;
        float hum = (float) Math.sin(u / 18.0) * (u > SN_PULL + 700 ? 2.5f : 0.8f) * G * 0.5f;
        int n = 24;
        for (int i = 0; i < n; i++) {
            float k = i / (float) (n - 1);
            if (k > pull) break;
            if (snapped) {
                // The two ends whip back and fall.
                float back = ease((u - SN_SNAP) / 400f);
                if (k > 0.5f) continue;
                k *= 1f - back;
            }
            float x = base[0] + (top[0] - base[0]) * k + hum * (float) Math.sin(k * Math.PI);
            float y = base[1] + (top[1] - base[1]) * k;
            int col = (i % 3 == 0) ? 0xFFB070 : 0xE04A2A;
            square(layer, x, y, G * 2, ChopScene.alpha(a * (snapped ? 1f - span(u, SN_SNAP, 500) : 1f)) | col);
        }
        if (u > SN_PULL + 600 && !snapped) ChopScene.glow(c, top[0], top[1] + (base[1] - top[1]) * 0.4f, 40f * ChopScene.size, 0xFF6030, 0.35f * a);
        if (snapped && u < SN_SNAP + 300) ChopScene.glow(c, top[0], top[1], 70f * ChopScene.size, 0xFFD080, 0.7f * a * (1f - (u - SN_SNAP) / 300f));
    }

    /** A cut: a line of squares grown along its length, white in the middle. */
    static void slash(CrystalVeins.Fill layer, float x, float y, float len, float ang, float k, float a, int col) {
        if (k <= 0f || k >= 2f) return;
        float dx = (float) Math.cos(ang) * len, dy = (float) Math.sin(ang) * len;
        int n = 20;
        for (int j = 0; j < n; j++) {
            float u = j / (float) (n - 1);
            if (u > k * 1.4f || u < k * 1.4f - 1f) continue;
            float wdt = (float) Math.sin(u * Math.PI);
            square(layer, x - dx / 2f + dx * u, y - dy / 2f + dy * u, G * (1 + Math.round(wdt * 1.5f)),
                    ChopScene.alpha(a * (1f - Math.min(1f, k / 2f))) | (wdt > 0.6f ? 0xFFFFFF : col));
        }
    }

    // ------------------------------------------------------------------ Scythe sweeps

    static void sweeps(CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        float[] c0 = SlayerScene.screen(CHEST[0], CHEST[1] + 6f, CHEST[2]);
        for (int i = 0; i < SWEEPS.length; i++) {
            long s = u - SWEEPS[i];
            if (s < 0 || s > 450) continue;
            // A crescent of squares swept round, its trail fading behind it.
            float k = s / 300f;
            float r = H * (0.16f + 0.03f * i);
            float a0 = (i % 2 == 0 ? -2.6f : -0.5f), dir = i % 2 == 0 ? 1f : -1f;
            for (int j = 0; j < 30; j++) {
                float f = j / 29f;
                if (f > k) break;
                float age = k - f;
                if (age > 0.6f) continue;
                float ang = a0 + dir * 2.1f * f;
                float thick = (float) Math.sin(f * Math.PI);
                for (int w = 0; w < 3; w++) {
                    float rr = r - w * G * 1.2f * thick;
                    square(layer, c0[0] + (float) Math.cos(ang) * rr, c0[1] + (float) Math.sin(ang) * rr * 0.55f, G,
                            ChopScene.alpha(a * (1f - age / 0.6f)) | (w == 0 ? 0xFFFFFF : 0x7FE8E0));
                }
            }
            if (s < 300) gore(layer, tl, CHEST, s, 12, a, 30 + i, 0xA0141A, 0x5A0A0E);
        }
        if (u > SCYTHE_HIT && u < SCYTHE_HIT + 700) {
            // Where the blade lands: a ring of dust and a crack of light.
            float k = (u - SCYTHE_HIT) / 700f;
            for (int i = 0; i < 28; i++) {
                float ang = i * 6.283f / 28f;
                float[] s = SlayerScene.screen(26f + (float) Math.cos(ang) * 30f * k, 1f, 8f + (float) Math.sin(ang) * 22f * k);
                square(layer, s[0], s[1], G * 2, ChopScene.alpha(a * (1f - k)) | (i % 2 == 0 ? 0xB8A890 : 0x7FE8E0));
            }
        }
    }

    // ------------------------------------------------------------------ the Warden's

    static void sculk(CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        // Sculk spreading from where the corpse sank, the veins pulsing cyan with each heartbeat.
        float k = ease(u / 1600f) * (1f - span(u, tl.arrive() - ENDING - 600, 600));
        if (k <= 0f) return;
        float r = 48f * k;
        float beat = heartBeat(u);
        float step = G / ChopScene.size;
        for (float dz = -r; dz <= r; dz += step) {
            for (float dx = -r * 1.4f; dx <= r * 1.4f; dx += step) {
                float d = (dx * dx) / (r * r * 1.96f) + (dz * dz) / (r * r);
                int hx = Math.round(dx / step), hz = Math.round(dz / step);
                int hsh = hx * 73856093 ^ hz * 19349663;
                float edge = 0.75f + ((hsh >>> 8) & 255) / 255f * 0.35f;
                if (d > edge) continue;
                int pick = (hsh >>> 3) & 15;
                int col = pick < 2 ? SlayerScene.mixRgb(0x0E5A62, 0x3AE8F0, beat) : (pick < 9 ? 0x07212A : 0x0C2E38);
                float[] s = SlayerScene.screen(dx, 0.2f, dz - 6f);
                square(layer, s[0], s[1], G, ChopScene.alpha(a * 0.95f) | col);
            }
        }
    }

    static void wardenFront(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        // Dirt thrown up as it digs out.
        if (u > W_EMERGE && u < W_ROAR) {
            for (int i = 0; i < 30; i++) {
                long born = W_EMERGE + (long) (tl.rand(1300 + i) * 2600);
                long age = u - born;
                if (age < 0 || age > 700) continue;
                float k = age / 1000f;
                float ang = tl.rand(1340 + i) * 6.283f;
                float x = (float) Math.cos(ang) * (8f + 30f * k), z = (float) Math.sin(ang) * (6f + 20f * k) - 6f;
                float y = 2f + 50f * k - 120f * k * k;
                if (y < 0f) continue;
                float[] s = SlayerScene.screen(x, y, z);
                square(layer, s[0], s[1], G * (i % 3 == 0 ? 2 : 1), ChopScene.alpha(a) | (i % 2 == 0 ? 0x07212A : 0x4A3424));
            }
        }
        // The roar: rings of sound off its mouth (the sonic boom's rings, at the camera).
        if (u > W_ROAR + 600 && u < RIP) {
            float[] mouth = SlayerScene.screen(WARDEN_CHEST[0], WARDEN_CHEST[1] + 22f, WARDEN_CHEST[2] + 10f);
            for (int r = 0; r < 4; r++) {
                float k = (((u - W_ROAR - 600) / 420f) + r * 0.25f) % 1f;
                ring(layer, mouth[0], mouth[1], H * 0.05f + H * 0.4f * k, ChopScene.alpha(a * (1f - k) * 0.7f) | (r % 2 == 0 ? 0x3AE8F0 : 0x0E8A96));
            }
        }
        // The heart: a glow through the chest, then the rip - gore and sculk thrown at the camera.
        float[] ch = SlayerScene.screen(WARDEN_CHEST[0], WARDEN_CHEST[1] + 2f, WARDEN_CHEST[2] + 6f);
        if (u > W_ROAR - 400 && u < RIP) ChopScene.glow(c, ch[0], ch[1], (30f + 30f * heartBeat(u)) * ChopScene.size, 0xFF6A3A, 0.5f * heartBeat(u) * a);
        if (u > RIP - 250 && u < RIP) {
            // The chest splitting: a crack of light.
            float k = (u - RIP + 250) / 250f;
            for (int i = 0; i < 12; i++) square(layer, ch[0] + (float) Math.sin(i * 2.1f) * G * 2 * k, ch[1] - G * 6 + i * G, G * 2, ChopScene.alpha(a * k) | 0xFFC080);
        }
        if (u > RIP && u < RIP + 1400) {
            gore(layer, tl, new float[]{WARDEN_CHEST[0], WARDEN_CHEST[1], WARDEN_CHEST[2] + 6f}, u - RIP, 80, a, 40, 0xA0141A, 0x0E5A62);
            if (u < RIP + 220) ChopScene.glow(c, ch[0], ch[1], 140f * ChopScene.size, 0xFFB080, 0.7f * a * (1f - (u - RIP) / 220f));
            // Drops off the heart.
            float[] hp = {W * 0.5f, H * 0.42f};
            for (int i = 0; i < 10; i++) {
                float k = ((u - RIP) / 700f + i * 0.1f) % 1f;
                square(layer, hp[0] + (tl.rand(1400 + i) - 0.5f) * H * 0.08f, hp[1] + H * 0.05f + k * H * 0.25f, G, ChopScene.alpha(a * (1f - k)) | 0x8A0E14);
            }
        }
        // It crumbles into sculk as it lies there.
        if (u > W_FALL + 600 && u < W_FALL + 2200) {
            float k = (u - W_FALL - 600) / 1600f;
            for (int i = 0; i < 50; i++) {
                float r1 = tl.rand(1500 + i), r2 = tl.rand(1550 + i);
                float x = -30f + 60f * r1, z = -10f + 14f * r2, y = 4f + 12f * r2 + 30f * k * r1;
                float[] s = SlayerScene.screen(x, y, z);
                square(layer, s[0], s[1], G * (r2 > 0.6f ? 2 : 1), ChopScene.alpha(a * (1f - k)) | (r1 > 0.6f ? 0x3AE8F0 : 0x0C2E38));
            }
        }
    }

    static void ring(CrystalVeins.Fill layer, float x, float y, float r, int argb) {
        int n = Math.max(12, (int) (r * 6.283f / (G * 1.6f)));
        for (int i = 0; i < n; i++) {
            float ang = i * 6.283f / n;
            square(layer, x + (float) Math.cos(ang) * r, y + (float) Math.sin(ang) * r * 0.75f, G, argb);
        }
    }

    // ------------------------------------------------------------------ the Matcha Dye

    static void matchaPool(CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        if (u < M_SPLASH) return;
        float k = ease((u - M_SPLASH) / 1800f) * (1f - span(u, tl.arrive() - ENDING - 300, 700));
        pool(layer, CHEST[0], CHEST[2], 34f * k, 0x6E9A2A, 0x8AC23A, a, tl, 77);
    }

    static void matchaFront(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        int matcha = 0x8AC23A;
        float[] at = SlayerScene.screen(CHEST[0], 6f, CHEST[2]);
        // Three heartbeats in the stopped world, each a ring of matcha green leaving the corpse.
        for (int b = 0; b < 3; b++) {
            long s = u - (700 + b * 560);
            if (s < 0 || s > 700) continue;
            float k = s / 700f;
            ring(layer, at[0], at[1], H * 0.06f + H * 0.32f * k, ChopScene.alpha(a * (1f - k)) | matcha);
        }
        // The drip: from the top of the screen down onto the corpse.
        if (u > M_DRIP && u < M_SPLASH) {
            float k = (u - M_DRIP) / (float) (M_SPLASH - M_DRIP);
            float y = -10f + (at[1] + 10f) * k * k;
            for (int i = 0; i < 5; i++) square(layer, at[0], y - i * G * 1.5f, G * (i == 0 ? 3 : 2), ChopScene.alpha(a * (1f - i * 0.18f)) | matcha);
        }
        if (u > M_SPLASH && u < M_SPLASH + 900) gore(layer, tl, new float[]{CHEST[0], 4f, CHEST[2]}, u - M_SPLASH, 60, a, 50, matcha, 0xC8E880);
        // Bubbles rising out of the pool as the corpse goes under.
        if (u > M_FLOOD + 600 && u < 6400) {
            for (int i = 0; i < 24; i++) {
                float ph = ((u - M_FLOOD) / 900f + tl.rand(1600 + i)) % 1f;
                float ang = tl.rand(1630 + i) * 6.283f, rr = 30f * tl.rand(1660 + i);
                float[] s = SlayerScene.screen(CHEST[0] + (float) Math.cos(ang) * rr, 1f + 30f * ph, CHEST[2] + (float) Math.sin(ang) * rr * 0.7f);
                square(layer, s[0], s[1], G * (i % 3 == 0 ? 2 : 1), ChopScene.alpha(a * (1f - ph)) | (i % 2 == 0 ? matcha : 0xD8F0A0));
            }
        }
        // Tea leaves whirling up into a column where the dye forms.
        if (u > 5200 && u < 6800) {
            float k = (u - 5200) / 1600f;
            for (int i = 0; i < 40; i++) {
                float h = (i / 40f + k * 0.8f) % 1f;
                float ang = i * 0.7f + k * 12f;
                float rr = H * 0.13f * (1f - h) * (1f - span(u, 6200, 500));
                float x = at[0] + (float) Math.cos(ang) * rr, y = at[1] - H * 0.3f * h;
                square(layer, x, y, G * 2, ChopScene.alpha(a * (1f - h)) | (i % 3 == 0 ? 0x4E7A1E : matcha));
            }
        }
        if (u > 6300 && u < 6700) ChopScene.glow(c, at[0], at[1] - H * 0.05f, 150f * ChopScene.size, matcha, 0.8f * a * (1f - (u - 6300) / 400f));
    }



    static final float BOSS_YAW = SlayerScene.BOSS_YAW;
}
