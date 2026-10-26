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
 *   <li><b>Darkness Within Rune</b>: the lights go out; only the spider's eight eyes are left, and they go out one by
 *       one; then the rune lights in the dark.</li>
 *   <li><b>Fly Swatter</b>: flies over the corpse - SWAT.</li>
 *   <li><b>Tarantula Talisman</b>: a web is spun over the corpse and drawn tight into the pendant.</li>
 *   <li><b>Vial of Venom</b> - "damage to internal organs": the fangs drip venom into a vial until it is full.</li>
 *   <li><b>Digested Mosquito</b>: a mosquito lands on the dying spider and drinks; the fangs snap it up; through the
 *       spider's belly it is seen digested in bubbling acid, and spat out.</li>
 *   <li><b>Shriveled Wasp</b> - "drowned in the web and drained of blood; stinger intact": caught in a web, it is
 *       drained grey and shrivels, its stinger glinting.</li>
 *   <li><b>Ensnared Snail</b> - "tightly wrapped in webbing ... revenge in the next life": wrapped, and its ghost rises.</li>
 *   <li><b>Primordial Eye</b> - "forcefully removed from the Primordial Broodfather": the camera closes in on the dead
 *       spider's face; one eye swells, looks round, and is torn out on its nerve.</li>
 *   <li><b>Brick Red Dye</b>: time stops and greys; bricks rain, the only colour left, and build a wall round the corpse;
 *       it glows, cracks and blows apart, and the dye stands where the corpse was.</li>
 *   <li>The catalysts (a burst of their colour, or a cocoon for the Tarantula's), the books (glyphs drawn in) and the
 *       Paragon Shard (crystals out of the ground).</li>
 * </ul>
 */
final class SlayerEndings {
    private SlayerEndings() {}

    // ================================================================== the hooks the stage asks

    /** The corpse's visibility: 1 lying there, 0 gone (it sinks into the ground between). */
    static float corpse(SlayerTimeline tl, long t) {
        long u = t - ENDING;
        return switch (tl.ending()) {
            case PLAIN, BOOK, COCOON -> 1f - span(t, ENDING + 200, 500);
            case PARAGON -> 1f - span(t, ENDING + 900, 500);
            case WARDEN -> 1f - span(t, ENDING, 700);
            case MATCHA -> 1f - span(t, ENDING + 5600, 1000);
            case BRICK -> t < ENDING + 5900 ? 1f : 0f;
            case SNAIL, TALISMAN, VENOM, WASP, MOSQUITO, EYE, DARKNESS, SWATTER -> 1f - span(t, tl.arrive() - 500, 400);
            default -> 1f - span(t, tl.arrive() - 600, 500);
        };
    }

    /** How far the death tip goes (1 vanilla); an ending that has its own plan for the body may stop it. */
    static float tipScale(SlayerTimeline tl, long t) {
        return 1f;
    }

    static int lose(SlayerTimeline tl, long t) {
        return switch (tl.ending()) {
            case BEHEAD -> t >= KILLED ? SlayerScene.LOSE_HEAD : 0;
            case HAND -> t >= ENDING + 250 ? SlayerScene.LOSE_RIGHT_ARM : 0;
            default -> 0;
        };
    }

    static float bossYaw(SlayerTimeline tl, long t) {
        return 0f;
    }

    static final float[] ZERO = {0f, 0f, 0f};

    static float[] bossOffset(SlayerTimeline tl, long t) {
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
            case EYE: {
                // The pull: the head dragged towards the eye leaving it, then snapping back.
                long pull = u - 3000;
                if (pull < 0 || pull > 1300) return ZERO;
                float k = pull < 1000 ? ease(pull / 1000f) : 1f - ease((pull - 1000) / 300f);
                return new float[]{1.6f * k, 0.6f * k, 2.2f * k};
            }
            default:
                return ZERO;
        }
    }

    /** The boss's own colour this frame (multiplied): drained, darkened, x-rayed. */
    static int bossTint(SlayerTimeline tl, long t) {
        long u = t - ENDING;
        switch (tl.ending()) {
            case DARKNESS:
                return SlayerScene.shade(0xFFFFFF, 1f - 0.9f * darkness(t));
            case MOSQUITO: {
                // Seen through: the body dark while the belly shows.
                float k = xray(t);
                return SlayerScene.mixRgb(0xFFFFFF, 0x3A3A48, k * 0.75f);
            }
            default:
                return 0xFFFFFF;
        }
    }

    static float floorAlpha(SlayerTimeline tl, long t) {
        float a = Math.min(1f, t / 250f);
        if (tl.ending() == Ending.DARKNESS) a *= 1f - darkness(t);
        if (tl.ending() == Ending.EYE) a *= 1f - 0.6f * zoom(t);
        // The showcase stands alone: the ground goes as the drop rises.
        a *= 1f - 0.85f * tl.rise(t);
        return a;
    }

    static int floorTint(SlayerTimeline tl, long t) {
        if (tl.drop().dye()) {
            float g = frozen(tl, t);
            return SlayerScene.mixRgb(0xFFFFFF, 0xA8A8B0, g);
        }
        return 0xFFFFFF;
    }

    static float extraDim(SlayerTimeline tl, long t) {
        float d = 0f;
        if (tl.ending() == Ending.DARKNESS) d = 0.33f * darkness(t);
        if (tl.drop().dye()) d = 0.18f * frozen(tl, t);
        if (tl.ending() == Ending.WARDEN) d = 0.06f * span(t, ENDING, 800) * (1f - span(t, tl.arrive() - 400, 600));
        return d * tl.fadeOut(t);
    }

    static float shake(SlayerTimeline tl, long t) {
        long u = t - ENDING;
        float s = 0f;
        switch (tl.ending()) {
            case WARDEN -> {
                if (u > 300 && u < 3300) s += 1.6f;
                if (u > 3300 && u < 5000) s += 2.4f * (float) Math.abs(Math.sin((u - 3300) / 140.0));
                s += 7f * impulse(u, RIP);
            }
            case SWATTER -> s += 6f * impulse(u, SWAT);
            case SCYTHE -> s += 5f * impulse(u, SCYTHE_HIT);
            case BRICK -> {
                for (int i = 0; i < 4; i++) s += 1.2f * impulse(u, 2300 + i * 600);
                s += 8f * impulse(u, B_BLOW);
            }
            case MATCHA -> s += 3f * impulse(u, M_SPLASH);
            case EYE -> s += 6f * impulse(u, E_SNAP);
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
        float rise = tl.rise(t);
        if (tl.ending() == Ending.EYE) {
            // In on the dead spider's face, and back out for the showcase.
            float z = zoom(t);
            ChopScene.size *= 1f + 1.25f * z;
            ChopScene.fx += (HEAD[0] - ChopScene.fx) * z;
            ChopScene.fy += (HEAD[1] - ChopScene.fy) * z;
            ChopScene.fz += (HEAD[2] - ChopScene.fz) * z;
            ChopScene.sy0 += (h * 0.5f - ChopScene.sy0) * z;
        }
        if (tl.ending() == Ending.WARDEN) {
            // Back and up as the Warden comes out of the ground (it is three blocks tall).
            float k = ease((t - ENDING - 300) / 1800f) * (1f - rise);
            ChopScene.size *= 1f - 0.22f * k;
            ChopScene.fy += 20f * k;
        }
        if (tl.ending() == Ending.MOSQUITO) {
            float z = ease((t - ENDING - 1900) / 700f) * (1f - ease((t - ENDING - 4300) / 600f));
            ChopScene.size *= 1f + 0.45f * z;
            ChopScene.fx += (CHEST[0] - ChopScene.fx) * z;
            ChopScene.fy += (CHEST[1] - ChopScene.fy) * z;
            ChopScene.fz += (CHEST[2] - ChopScene.fz) * z;
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

    // ================================================================== the models each ending adds

    static void models(SlayerTimeline tl, long t) {
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
            case MOSQUITO -> mosquito(tl, t, u);
            case WASP -> wasp(tl, u);
            case SNAIL -> snail(tl, u);
            case SWATTER -> swatter(tl, u);
            case SNAKES -> snakes(tl, u);
            case BRICK -> bricks(tl, u);
            case COCOON -> cocoon(tl, u);
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

    // ------------------------------------------------------------------ the Digested Mosquito

    static final long MQ_LAND = 1200, MQ_SNAP = 1900, MQ_XRAY = 2100, MQ_SPIT = 4500;

    /** How see-through the spider is: its belly shown while the mosquito digests. */
    static float xray(long t) {
        long u = t - ENDING;
        return span(t, ENDING + MQ_XRAY, 400) * (1f - span(t, ENDING + MQ_SPIT - 300, 300));
    }

    static void mosquito(SlayerTimeline tl, long t, long u) {
        int body = 0x6A5444;
        if (u < MQ_SNAP) {
            // Flies in on a wavy line from the left, lands on the corpse's head and drinks, swelling red.
            float k = clamp(u / (float) MQ_LAND);
            float x = -110f + (HEAD[0] - 110f * 0f + 110f) * ease(k);
            float y = HEAD[1] + 30f * (1f - ease(k)) + 4f * (float) Math.sin(u / 90.0) * (1f - k) + 8f;
            float z = HEAD[2] + 20f * (1f - k);
            float drink = span(u, MQ_LAND, 600);
            int col = SlayerScene.mixRgb(body, 0xB02018, drink);
            float sc = 0.9f + 0.25f * drink;
            SlayerScene.add(rigs.bee(u / 50f * (u < MQ_LAND ? 1f : 0.4f), 100f, 25f * drink), sc, x, y, z, -1, col, 0, POLYS);
            // The proboscis: a thin red-brown needle into the head.
            if (u > MQ_LAND - 200) {
                float[][] a = SlayerScene.axes(0f, -0.9f, 0f);
                SlayerScene.solid(x + 3f, y - 2f, z + 3f, 0.5f, 4f, 0.5f, a[0], a[1], a[2], 0x7A2A20, 0, POLYS);
            }
            return;
        }
        if (u < MQ_SPIT) {
            // In the belly: drawn over the spider (the second layer), shrinking and browning as the acid works.
            float d = span(u, MQ_XRAY, MQ_SPIT - MQ_XRAY - 300);
            int col = SlayerScene.mixRgb(0xB02018, 0x5C3A1E, d);
            float sc = 1.1f * (1f - 0.55f * d);
            float roll = 0.6f * (float) Math.sin(u / 240.0);
            SlayerScene.addTurned(rigs.bee(u / 50f * (1f - d), 60f + u * 0.05f, 30f * (float) Math.sin(u / 300.0)), sc,
                    CHEST[0], CHEST[1] + 1f + 1.5f * (float) Math.sin(u / 170.0), CHEST[2] + 2f, roll, -1, col, 0, IN);
        }
    }

    // ------------------------------------------------------------------ the Shriveled Wasp

    static float[] waspAt(long u) {
        return new float[]{-6f, 34f, 22f};
    }

    static void wasp(SlayerTimeline tl, long u) {
        if (u > tl.arrive() - ENDING - 250) return;
        // Caught in the web over the corpse, struggling, then drained grey and shrinking, wings still.
        float drain = span(u, 900, 1300);
        float[] p = waspAt(u);
        float jit = (1f - drain) * 1.2f;
        int col = SlayerScene.mixRgb(0xFFFFFF, 0x7A7468, drain);
        float sc = 1.25f * (1f - 0.38f * drain);
        float wings = (1f - drain) * u / 18f;
        SlayerScene.addTurned(rigs.bee(wings, 15f + 8f * (float) Math.sin(u / 70.0) * (1f - drain), -10f), sc,
                p[0] + jit * (float) Math.sin(u / 23.0), p[1] + jit * (float) Math.cos(u / 31.0), p[2], 0.25f * drain, -1, col, 0, POLYS);
    }

    // ------------------------------------------------------------------ the Ensnared Snail

    static float[] snailAt(long u) {
        float k = clamp(u / 1100f);
        return new float[]{-48f + 22f * k, 0f, 22f};
    }

    static void snail(SlayerTimeline tl, long u) {
        if (u > tl.arrive() - ENDING - 300) return;
        float[] p = snailAt(u);
        float wrap = span(u, 1100, 900);
        int foot = SlayerScene.mixRgb(0xC4B07A, 0xF0F0F0, wrap);
        int shell = SlayerScene.mixRgb(0x8E5A2C, 0xF4F4F4, wrap);
        float creep = (float) Math.sin(u / 120.0) * (1f - wrap) * 0.5f;
        float[][] a = SlayerScene.axes(0.1f, 0f, 0f);
        SlayerScene.solid(p[0] + creep, 1.6f, p[2], 7f, 1.6f, 2.6f, a[0], a[1], a[2], foot, 0, POLYS);
        SlayerScene.solid(p[0] - 1.5f, 6.5f, p[2], 4.6f, 4.6f, 3.2f, a[0], a[1], a[2], shell, 0, POLYS);
        SlayerScene.solid(p[0] - 1.5f, 6.5f, p[2] + 3.3f, 2.4f, 2.4f, 0.2f, a[0], a[1], a[2], SlayerScene.mixRgb(0x5A3418, 0xDADADA, wrap), 0, POLYS);
        if (wrap < 0.9f) {
            float[][] st = SlayerScene.axes(0.1f, 0f, -0.35f);
            SlayerScene.solid(p[0] + 6.5f, 5f, p[2] - 1f, 0.5f, 3f, 0.5f, st[0], st[1], st[2], foot, 0, POLYS);
            SlayerScene.solid(p[0] + 6.5f, 5f, p[2] + 1f, 0.5f, 3f, 0.5f, st[0], st[1], st[2], foot, 0, POLYS);
        }
    }

    // ------------------------------------------------------------------ the Fly Swatter

    static final long SWAT = 1500;

    static void swatter(SlayerTimeline tl, long u) {
        if (u < 700 || u > tl.arrive() - ENDING - 200) return;
        // Raised high, then down flat onto the corpse; then lifted away.
        float down = u < SWAT ? ease((u - 700) / 800f) * 0.25f + (u > SWAT - 160 ? ease((u - SWAT + 160) / 160f) * 0.75f : 0f) : 1f;
        float lift = span(u, SWAT + 500, 500);
        float ang = 1.4f * (1f - down) + 1.1f * lift;
        float[][] a = SlayerScene.axes(-0.4f, ang, 0f);
        float hx = 4f, hz = 30f;
        float[] pivot = {CHEST[0] - 4f, 2f, CHEST[2] + 46f};
        float[] centre = {pivot[0] - a[2][0] * hz, pivot[1] - a[2][1] * hz + 2f, pivot[2] - a[2][2] * hz};
        // The pad: a mesh of green, then the handle.
        SlayerScene.box(centre[0], centre[1], centre[2], 15f, 0.8f, 12f, a[0], a[1], a[2], SlayerScene.ALL, SlayerScene.SLOT_COBWEB,
                0x6AA84A, 0, POLYS);
        SlayerScene.solid(centre[0], centre[1], centre[2], 15.6f, 0.5f, 12.6f, a[0], a[1], a[2], 0x3E6A2A, 0, POLYS);
        float hx0 = pivot[0] - a[2][0] * 8f, hy0 = pivot[1] - a[2][1] * 8f, hz0 = pivot[2] - a[2][2] * 8f;
        SlayerScene.solid(hx0, hy0, hz0, 1f, 1f, 9f, a[0], a[1], a[2], 0x8A5A2E, 0, POLYS);
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

    // ------------------------------------------------------------------ the Brick Red Dye

    static final long B_FREEZE = 200, B_RAIN = 2000, B_GLOW = 4800, B_BLOW = 5900;
    static final int BRICK = 0xCB4154;

    /** Each brick's slot in the wall: 7 along, 4 high, in a curve round the corpse's front. */
    static float[] brickSlot(int i) {
        int col = i % 7, row = i / 7;
        float ang = (col - 3) * 0.28f;
        float r = 34f;
        float off = row % 2 == 0 ? 0f : 0.14f;
        float x = CHEST[0] * 0.5f + (float) Math.sin(ang + off) * r;
        float z = CHEST[2] + (float) Math.cos(ang + off) * r - 12f;
        return new float[]{x, 4f + row * 8f, z, ang + off};
    }

    static void bricks(SlayerTimeline tl, long u) {
        if (u < B_RAIN) return;
        int n = 28;
        for (int i = 0; i < n; i++) {
            long land = B_RAIN + 300 + i * 85;
            float[] s = brickSlot(i);
            float x = s[0], y = s[1], z = s[2], ang = s[3], spin = 0f;
            if (u < land - 500) continue;
            if (u < land) {
                float k = (u - land + 500) / 500f;
                y = s[1] + 150f * (1f - k * k);
                spin = (1f - k) * 3f;
            } else if (u > B_BLOW) {
                // Blown out at the camera, tumbling.
                float b = (u - B_BLOW) / 1000f;
                float dx = x - CHEST[0] * 0.5f, dz = z - CHEST[2] + 30f;
                float dl = (float) Math.sqrt(dx * dx + dz * dz) + 1e-3f;
                x += dx / dl * 160f * b;
                z += dz / dl * 160f * b + 60f * b;
                y += 70f * b - 90f * b * b + 20f * tl.rand(80 + i) * b;
                spin = b * (6f + 4f * tl.rand(90 + i));
                if (b > 0.9f) continue;
            }
            float glow = u > B_GLOW && u < B_BLOW ? span(u, B_GLOW, 800) : 0f;
            float[][] a = SlayerScene.axes(ang, spin * 0.7f, spin);
            int add = glow > 0f ? ChopScene.alpha(0.5f * glow * (0.7f + 0.3f * (float) Math.sin(u / 60.0 + i))) | 0xFF4020 : 0;
            SlayerScene.box(x, y, z, 7.6f, 3.8f, 3.4f, a[0], a[1], a[2], BRICK_UV, SlayerScene.SLOT_BRICKS, 0xFFFFFF, add, POLYS);
        }
    }

    /** One brick of bricks.png on each face: the texture's top-left brick, 8 x 4 texels of 16. */
    static final float[] ONE_BRICK = {0f, 0f, 0.5f, 0.25f};
    static final float[][] BRICK_UV = {ONE_BRICK, ONE_BRICK, ONE_BRICK, ONE_BRICK, ONE_BRICK, ONE_BRICK};

    // ------------------------------------------------------------------ the Spider Catalyst / Tarantula Catalyst

    static void cocoon(SlayerTimeline tl, long u) {
        // Silk wraps a cocoon where the corpse was, it twitches, and splits open.
        float grow = span(u, 150, 600);
        if (grow <= 0f || u > 1500) return;
        float shake = u > 800 ? 1.2f * (float) Math.sin(u / 30.0) : 0f;
        float[][] a = SlayerScene.axes(0.4f, 0f, 0.1f * shake);
        SlayerScene.box(CHEST[0], 9f * grow, CHEST[2], 9f * grow, 9f * grow, 12f * grow, a[0], a[1], a[2], SlayerScene.ALL,
                SlayerScene.SLOT_COBWEB, 0xF4F4F4, ChopScene.alpha(0.2f) | 0xFFFFFF, POLYS);
        SlayerScene.solid(CHEST[0], 9f * grow, CHEST[2], 8.6f * grow, 8.6f * grow, 11.6f * grow, a[0], a[1], a[2], 0xD8D8D8, 0, POLYS);
    }

    // ================================================================== time stopping

    /** How stopped the world is, 0..1 (the dyes). */
    static float frozen(SlayerTimeline tl, long t) {
        if (!tl.drop().dye()) return 0f;
        long u = t - ENDING;
        long free = tl.ending() == Ending.MATCHA ? M_THAW : B_BLOW;
        return span(u, tl.ending() == Ending.MATCHA ? M_FREEZE : B_FREEZE, 450) * (1f - span(u, free, 500));
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

    // ================================================================== darkness

    static final long D_DARK = 300, D_EYES = 1000, D_IGNITE = 2600;

    static float darkness(long t) {
        long u = t - ENDING;
        return span(u, D_DARK, 500) * (1f - span(u, D_IGNITE + 900, 500));
    }

    static float zoom(long t) {
        long u = t - ENDING;
        return ease(u / 1300f) * (1f - ease((u - 4700) / 900f));
    }

    // ================================================================== the layers

    /** Under the models: pools, sculk, shadows, the web under a wasp. */
    static void back(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long t) {
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
            case BEHEAD, HAND, SINEW, MAGGOT, EYE -> bloodPool(layer, tl, t, u, a);
            case SWATTER -> {
                if (u > SWAT) splat(layer, CHEST[0], CHEST[2], 26f, 0x5A7A1E, u - SWAT, a, tl, 0);
            }
            default -> { }
        }
    }

    /** Over the models: blood, slime, webs, sparks, glyphs, the darkness's eyes. */
    static void front(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long t) {
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
            case BRICK -> brickFront(c, layer, tl, u, a);
            case DARKNESS -> darknessFront(c, layer, tl, t, u, a);
            case SWATTER -> flies(layer, tl, u, a);
            case TALISMAN -> web(c, layer, tl, u, a);
            case VENOM -> venom(c, layer, tl, u, a);
            case MOSQUITO -> belly(c, layer, tl, u, a);
            case WASP -> waspWeb(c, layer, tl, u, a);
            case SNAIL -> snailWeb(c, layer, tl, u, a);
            case EYE -> eyeFront(c, layer, tl, u, a);
            case COCOON -> {
                if (u > 1500 && u < 2200) shards(layer, tl, u - 1500, 0xF0F0F0, a);
                silk(c, tl, u, a);
            }
            case SNAKES -> {
                if (u > 2700 && u < 3300) shards(layer, tl, u - 2700, 0x55FF55, a);
            }
        }
    }

    // ================================================================== where the drop is before it rises

    /** {screen x, y, size px, alpha, yaw}, or null while it is not there yet. */
    static float[] itemAt(SlayerTimeline tl, long t) {
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
            case EYE: {
                if (u < 1400) return null;
                float[] s = SlayerScene.screen(HEAD[0] + 2f, HEAD[1] + 2f, HEAD[2] + 8f);
                float swell = ease((u - 1400) / 700f);
                float pull = u > 2900 ? ease((u - 2900) / 1100f) : 0f;
                float look = u < 2900 ? 70f * (float) Math.sin((u - 1400) / 260.0) : 0f;
                float x = s[0] + (W * 0.5f - s[0]) * pull, y = s[1] + (H * 0.38f - s[1]) * pull;
                return new float[]{x, y, (5f + 6f * swell) * sz + H * 0.12f * pull, Math.min(1f, swell * 2f), look};
            }
            case MATCHA:
            case BRICK: {
                long at = tl.ending() == Ending.MATCHA ? 6400 : B_BLOW;
                if (u < at) return null;
                float k = span(u, at, 400);
                float[] s = SlayerScene.screen(CHEST[0], 14f, CHEST[2]);
                return new float[]{s[0], s[1], 18f * sz * SlayerTimeline.backOut(k), k, 30f};
            }
            case MOSQUITO: {
                if (u < MQ_SPIT) return null;
                // Spat out of the mouth, onto the ground in front.
                float k = clamp((u - MQ_SPIT) / 500f);
                float x = HEAD[0] + 20f * k, y = HEAD[1] + 14f * k - 20f * k * k + 2f, z = HEAD[2] + 26f * k;
                float[] s = SlayerScene.screen(x, Math.max(4f, y), z);
                return new float[]{s[0], s[1], 12f * sz, 1f, 0f};
            }
            case WASP: {
                long at = 2300;
                if (u < at) return null;
                float k = span(u, at, 300);
                float[] p = waspAt(u);
                float[] s = SlayerScene.screen(p[0], p[1], p[2]);
                return new float[]{s[0], s[1], 14f * sz * k, k, 30f};
            }
            case SNAIL: {
                long at = 2700;
                if (u < at) return null;
                float k = span(u, at, 300);
                float[] p = snailAt(u);
                float[] s = SlayerScene.screen(p[0], 7f, p[2]);
                return new float[]{s[0], s[1], 14f * sz * k, k, 30f};
            }
            case SWATTER: {
                long at = SWAT + 900;
                if (u < at) return null;
                float k = span(u, at, 300);
                float[] s = SlayerScene.screen(CHEST[0], 8f, CHEST[2] + 6f);
                return new float[]{s[0], s[1], 16f * sz * k, k, 0f};
            }
            case VENOM: {
                if (u < V_CORK) return null;
                float k = span(u, V_CORK, 250);
                float[] v = vialScreen();
                return new float[]{v[0], v[1], v[2] * 1.4f, k, 30f};
            }
            case TALISMAN: {
                long at = 2000;
                if (u < at) return null;
                float k = span(u, at, 300);
                float[] s = SlayerScene.screen(CHEST[0], 26f, CHEST[2] + 20f);
                return new float[]{s[0], s[1], 16f * sz * SlayerTimeline.backOut(k), k, 30f};
            }
            case DARKNESS: {
                if (u < D_IGNITE) return null;
                float k = span(u, D_IGNITE, 400);
                float[] s = SlayerScene.screen(HEAD[0], HEAD[1] + 20f, HEAD[2]);
                return new float[]{s[0], s[1], 16f * sz * k, k, 30f};
            }
            default: {
                long at = switch (tl.ending()) {
                    case PLAIN -> 350;
                    case BOOK -> 700;
                    case COCOON -> 1500;
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

    // ------------------------------------------------------------------ the Brick Red Dye

    static void brickFront(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        float[] at = SlayerScene.screen(CHEST[0], 6f, CHEST[2]);
        for (int b = 0; b < 3; b++) {
            long s = u - (500 + b * 520);
            if (s < 0 || s > 700) continue;
            float k = s / 700f;
            ring(layer, at[0], at[1], H * 0.06f + H * 0.32f * k, ChopScene.alpha(a * (1f - k)) | BRICK);
        }
        // Dust as each brick lands.
        for (int i = 0; i < 28; i++) {
            long land = B_RAIN + 300 + i * 85;
            long s = u - land;
            if (s < 0 || s > 300 || u > B_BLOW) continue;
            float[] p = brickSlot(i);
            float[] sc = SlayerScene.screen(p[0], p[1] - 3f, p[2]);
            for (int j = 0; j < 4; j++) square(layer, sc[0] + (j - 1.5f) * G * 3 * (s / 300f + 0.3f), sc[1] + G, G, ChopScene.alpha(a * (1f - s / 300f)) | 0xB0A090);
        }
        // Cracks of red light across the wall before it goes.
        if (u > B_GLOW + 400 && u < B_BLOW) {
            float k = span(u, B_GLOW + 400, 600);
            float[] mid = SlayerScene.screen(CHEST[0] * 0.5f, 16f, CHEST[2] + 20f);
            for (int r = 0; r < 6; r++) {
                float ang = r * 1.05f + 0.3f;
                for (int j = 0; j < (int) (14 * k); j++) {
                    float wob = (float) Math.sin(j * 1.7f + r) * G * 1.5f;
                    square(layer, mid[0] + (float) Math.cos(ang) * j * G * 2 + wob, mid[1] + (float) Math.sin(ang) * j * G * 1.4f, G, ChopScene.alpha(a) | 0xFFD0A0);
                }
            }
            ChopScene.glow(c, mid[0], mid[1], 120f * ChopScene.size * k, 0xFF4020, 0.6f * a * k);
        }
        if (u > B_BLOW && u < B_BLOW + 300) ChopScene.glow(c, at[0], at[1] - H * 0.05f, 200f * ChopScene.size, 0xFF7050, 0.9f * a * (1f - (u - B_BLOW) / 300f));
        if (u > B_BLOW) gore(layer, tl, new float[]{CHEST[0], 12f, CHEST[2] + 20f}, u - B_BLOW, 70, a, 60, BRICK, 0x8A2A2A);
    }

    // ------------------------------------------------------------------ Darkness Within

    static void darknessFront(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long t, long u, float a) {
        float dark = darkness(t);
        if (dark <= 0.01f) return;
        // The eight eyes, red in the dark, going out one by one from the outside in.
        float[] e = SlayerScene.screen(HEAD[0], HEAD[1], HEAD[2] + 6f);
        float span = 5.5f * ChopScene.size;
        for (int i = 0; i < 8; i++) {
            long out = D_EYES + (long) i * 190;
            float on = u < out ? 1f : Math.max(0f, 1f - (u - out) / 120f);
            if (u > D_IGNITE) on = 0f;
            if (on <= 0f) continue;
            float x = e[0] + (i % 4 - 1.5f) * span * 0.55f, y = e[1] + (i / 4 - 0.5f) * span * 0.45f;
            square(layer, x, y, G * 2, ChopScene.alpha(a * dark * on) | 0xFF2020);
            ChopScene.glow(c, x, y, 14f * ChopScene.size, 0xFF2020, 0.5f * dark * on * a);
        }
        // The rune lights: a dark violet flame climbing round where it will be.
        if (u > D_IGNITE - 300 && u < D_IGNITE + 900) {
            float k = (u - D_IGNITE + 300) / 1200f;
            float[] r = SlayerScene.screen(HEAD[0], HEAD[1] + 20f, HEAD[2]);
            for (int i = 0; i < 36; i++) {
                float ang = i * 6.283f / 36f + k * 4f;
                float rr = H * 0.11f * (1f - 0.5f * k);
                float hgt = (float) Math.abs(Math.sin(i * 1.3f + u / 80.0)) * G * 4;
                square(layer, r[0] + (float) Math.cos(ang) * rr, r[1] + (float) Math.sin(ang) * rr * 0.8f - hgt, G * 2,
                        ChopScene.alpha(a * (1f - k)) | (i % 3 == 0 ? 0x8A3AD0 : 0x2A1A3A));
            }
            ChopScene.glow(c, r[0], r[1], 90f * ChopScene.size, 0x7A2AC0, 0.6f * a * (1f - k));
        }
    }

    // ------------------------------------------------------------------ the Fly Swatter

    static void flies(CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        for (int i = 0; i < 9; i++) {
            float ph = u / 1000f * (2.4f + tl.rand(1700 + i)) + i;
            float x = CHEST[0] + 16f * (float) Math.sin(ph * 2.1f) + (tl.rand(1720 + i) - 0.5f) * 10f;
            float y = 14f + 8f * (float) Math.cos(ph * 3.3f) + 6f * tl.rand(1740 + i);
            float z = CHEST[2] + 12f * (float) Math.cos(ph * 1.7f);
            if (u > SWAT) {
                // Splatted flat where they were.
                y = 0.4f;
                if (u > SWAT + 900) continue;
            }
            float[] s = SlayerScene.screen(x, y, z);
            square(layer, s[0], s[1], G * (u > SWAT ? 2 : 1), ChopScene.alpha(a) | (u > SWAT ? 0x2A3A0E : 0x101010));
            if (u < SWAT && (u / 40 + i) % 2 == 0) square(layer, s[0] + G, s[1] - G, G, ChopScene.alpha(a * 0.5f) | 0xD8E8FF);
        }
        if (u > SWAT && u < SWAT + 500) gore(layer, tl, new float[]{CHEST[0], 2f, CHEST[2]}, u - SWAT, 40, a, 70, 0x5A7A1E, 0x2A3A0E);
    }

    // ------------------------------------------------------------------ the Tarantula Talisman

    static void web(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        float[] at = SlayerScene.screen(CHEST[0], 26f, CHEST[2] + 20f);
        float spin = ease(u / 1200f);
        float tighten = ease((u - 1500) / 500f);
        float r = H * 0.26f * (1f - 0.85f * tighten);
        int col = ChopScene.alpha(a * 0.85f * (1f - span(u, 2000, 200))) | 0xF0F0F0;
        float lw = Math.max(1f, G * 0.5f);
        // Eight spokes, then the spiral wound round them.
        for (int i = 0; i < 8; i++) {
            float ang = i * 0.785f + u / 3000f;
            float len = r * Math.min(1f, spin * 2f);
            c.line(at[0], at[1], at[0] + (float) Math.cos(ang) * len, at[1] + (float) Math.sin(ang) * len * 0.85f, lw, col);
        }
        int turns = (int) (spin * 40);
        for (int j = 0; j < turns; j++) {
            float a0 = j * 0.785f + u / 3000f, a1 = (j + 1) * 0.785f + u / 3000f;
            float r0 = r * (1f - j / 44f), r1 = r * (1f - (j + 1) / 44f);
            c.line(at[0] + (float) Math.cos(a0) * r0, at[1] + (float) Math.sin(a0) * r0 * 0.85f,
                    at[0] + (float) Math.cos(a1) * r1, at[1] + (float) Math.sin(a1) * r1 * 0.85f, lw, col);
        }
        if (u > 1900 && u < 2300) ChopScene.glow(c, at[0], at[1], 80f * ChopScene.size, 0xFF55FF, 0.6f * a * (1f - (u - 1900) / 400f));
    }

    // ------------------------------------------------------------------ the Vial of Venom

    static final long V_FILL = 600, V_CORK = 2300;

    /** The vial on the screen: x, y of its middle, its height. */
    static float[] vialScreen() {
        float[] s = SlayerScene.screen(HEAD[0] + 14f, 8f, HEAD[2] + 18f);
        return new float[]{s[0], s[1], 26f * ChopScene.size * 0.5f};
    }

    static void venom(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        float[] v = vialScreen();
        float hh = v[2], ww = hh * 0.45f;
        float fill = span(u, V_FILL, V_CORK - V_FILL - 200);
        float fade = 1f - span(u, V_CORK, 250);
        int green = 0x7CFF40;
        // Drops off the fangs into the vial's neck.
        float[] f = SlayerScene.screen(HEAD[0] + 4f, HEAD[1] - 2f, HEAD[2] + 8f);
        if (u > 200 && u < V_CORK - 100) {
            for (int i = 0; i < 6; i++) {
                float k = ((u - 200) / 330f + i / 6f) % 1f;
                float x = f[0] + (v[0] - f[0]) * k, y = f[1] + (v[1] - hh / 2f - f[1]) * k + (float) Math.sin(k * Math.PI) * -H * 0.03f;
                square(layer, x, y, G * 2, ChopScene.alpha(a * fade) | green);
            }
        }
        if (fade <= 0f) return;
        // The glass: an outline, the venom rising in it, bubbles, then the cork.
        int glass = ChopScene.alpha(a * fade) | 0xD8F0FF;
        float lw = Math.max(1f, G);
        c.fill(v[0] - ww / 2f, v[1] + hh / 2f - hh * 0.85f * fill, ww, hh * 0.85f * fill, ChopScene.alpha(a * fade * 0.85f) | 0x4AC020);
        c.line(v[0] - ww / 2f, v[1] - hh / 2f, v[0] - ww / 2f, v[1] + hh / 2f, lw, glass);
        c.line(v[0] + ww / 2f, v[1] - hh / 2f, v[0] + ww / 2f, v[1] + hh / 2f, lw, glass);
        c.line(v[0] - ww / 2f, v[1] + hh / 2f, v[0] + ww / 2f, v[1] + hh / 2f, lw, glass);
        for (int i = 0; i < 5; i++) {
            float k = ((u / 500f) + i * 0.2f) % 1f;
            float y = v[1] + hh / 2f - hh * 0.85f * fill * k;
            if (fill > 0.05f) square(layer, v[0] + (tl.rand(1800 + i) - 0.5f) * ww * 0.7f, y, G, ChopScene.alpha(a * fade) | 0xC8FFA0);
        }
        if (u > V_CORK - 300) {
            float k = span(u, V_CORK - 300, 200);
            float cy = v[1] - hh / 2f - (1f - k) * hh * 0.6f;
            c.fill(v[0] - ww * 0.4f, cy - hh * 0.12f, ww * 0.8f, hh * 0.14f, ChopScene.alpha(a * fade) | 0x8A5A2E);
        }
        ChopScene.glow(c, v[0], v[1], hh * 2f, green, 0.25f * a * fill * fade);
    }

    // ------------------------------------------------------------------ the Digested Mosquito

    static void belly(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        float k = xray(ENDING + u);
        if (u > MQ_SNAP - 150 && u < MQ_SNAP + 250) {
            // The fangs snapping shut: a white flash and a spray.
            gore(layer, tl, HEAD, u - MQ_SNAP + 150, 20, a, 80, 0xA0141A, 0x6A5444);
        }
        if (k <= 0f) return;
        // The belly seen through: an oval of acid, bubbling, behind the mosquito (which is drawn over this layer).
        float[] b = SlayerScene.screen(CHEST[0], CHEST[1] + 1f, CHEST[2] + 2f);
        float rx = 15f * ChopScene.size, ry = 11f * ChopScene.size;
        float step = G;
        for (float dy = -ry; dy <= ry; dy += step) {
            for (float dx = -rx; dx <= rx; dx += step) {
                float d = dx * dx / (rx * rx) + dy * dy / (ry * ry);
                if (d > 1f) continue;
                int hsh = Math.round((b[0] + dx) / step) * 73856093 ^ Math.round((b[1] + dy) / step) * 19349663;
                float surf = (float) Math.sin(dx / rx * 6 + u / 160.0) * 0.08f;
                boolean acid = dy / ry > -0.25f + surf;
                int col = d > 0.85f ? 0x4A2A3A : (acid ? (((hsh >>> 5) & 7) == 0 ? 0xC8FF60 : 0x7ADC2A) : 0x2A1A22);
                square(layer, b[0] + dx, b[1] + dy, G, ChopScene.alpha(a * k * 0.92f) | col);
            }
        }
        for (int i = 0; i < 14; i++) {
            float ph = ((u - MQ_XRAY) / 700f + tl.rand(1900 + i)) % 1f;
            float x = b[0] + (tl.rand(1930 + i) - 0.5f) * rx * 1.4f, y = b[1] + ry * 0.8f - ph * ry * 1.4f;
            square(layer, x, y, G * (i % 3 == 0 ? 2 : 1), ChopScene.alpha(a * k * (1f - ph)) | 0xE8FFB0);
        }
        // The spit: a brown glob and slime as it comes out.
        if (u > MQ_SPIT - 100 && u < MQ_SPIT + 600) gore(layer, tl, HEAD, u - MQ_SPIT + 100, 30, a, 90, 0x7ADC2A, 0x5C3A1E);
    }

    // ------------------------------------------------------------------ the Shriveled Wasp

    static void waspWeb(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        float[] p = waspAt(u);
        float[] s = SlayerScene.screen(p[0], p[1], p[2]);
        float r = H * 0.16f;
        float fade = 1f - span(u, 2300, 300);
        int col = ChopScene.alpha(a * 0.8f * fade) | 0xF0F0F0;
        float lw = Math.max(1f, G * 0.5f);
        for (int i = 0; i < 8; i++) {
            float ang = i * 0.785f + 0.2f;
            c.line(s[0], s[1], s[0] + (float) Math.cos(ang) * r, s[1] + (float) Math.sin(ang) * r * 0.9f, lw, col);
        }
        for (int ringI = 1; ringI <= 4; ringI++) {
            float rr = r * ringI / 4.5f;
            for (int i = 0; i < 8; i++) {
                float a0 = i * 0.785f + 0.2f, a1 = (i + 1) * 0.785f + 0.2f;
                c.line(s[0] + (float) Math.cos(a0) * rr, s[1] + (float) Math.sin(a0) * rr * 0.9f,
                        s[0] + (float) Math.cos(a1) * rr, s[1] + (float) Math.sin(a1) * rr * 0.9f, lw, col);
            }
        }
        // Drained: red drops pulled off it into the web, then the stinger's glint.
        if (u > 900 && u < 2200) {
            for (int i = 0; i < 8; i++) {
                float k = ((u - 900) / 600f + i / 8f) % 1f;
                float ang = i * 0.785f + 0.2f;
                square(layer, s[0] + (float) Math.cos(ang) * r * k, s[1] + (float) Math.sin(ang) * r * 0.9f * k, G, ChopScene.alpha(a * (1f - k)) | 0xA0141A);
            }
        }
        if (u > 2050 && u < 2350) {
            float k = (u - 2050) / 300f;
            float gx = s[0] - 6f * ChopScene.size, gy = s[1] + 2f * ChopScene.size;
            for (int i = -3; i <= 3; i++) {
                square(layer, gx + i * G, gy, G, ChopScene.alpha(a * (1f - k) * (1f - Math.abs(i) / 4f)) | 0xFFFFFF);
                square(layer, gx, gy + i * G, G, ChopScene.alpha(a * (1f - k) * (1f - Math.abs(i) / 4f)) | 0xFFFFFF);
            }
        }
    }

    // ------------------------------------------------------------------ the Ensnared Snail

    static void snailWeb(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        float[] p = snailAt(u);
        float[] s = SlayerScene.screen(p[0], 5f, p[2]);
        // Strands shot from the spider's spinnerets onto it, then wound round.
        float[] from = SlayerScene.screen(CHEST[0], CHEST[1], CHEST[2]);
        float lw = Math.max(1f, G * 0.5f);
        float fade = 1f - span(u, 2700, 300);
        for (int i = 0; i < 5; i++) {
            long at = 900 + i * 90L;
            if (u < at) continue;
            float k = Math.min(1f, (u - at) / 200f);
            float tx = s[0] + (i - 2) * G * 2, ty = s[1] + (i % 2) * G * 2;
            c.line(from[0], from[1], from[0] + (tx - from[0]) * k, from[1] + (ty - from[1]) * k, lw, ChopScene.alpha(a * 0.8f * fade * (1f - span(u, 1900, 400))) | 0xF0F0F0);
        }
        float wrap = span(u, 1100, 900);
        for (int j = 0; j < (int) (wrap * 14); j++) {
            float y = s[1] - 6f * ChopScene.size + j * ChopScene.size;
            c.line(s[0] - 8f * ChopScene.size, y + ChopScene.size, s[0] + 8f * ChopScene.size, y - ChopScene.size, lw, ChopScene.alpha(a * fade) | 0xFFFFFF);
        }
        // The ghost: a pale snail shape rising out of the cocoon, swaying, fading - its revenge in the next life.
        if (u > 2000 && u < 3200) {
            float k = (u - 2000) / 1200f;
            float gx = s[0] + (float) Math.sin(k * 6) * G * 3, gy = s[1] - H * 0.25f * k;
            String[] ghost = {"..####..", ".######.", "##.##.##", "########", "########", "#.#..#.#"};
            for (int r = 0; r < ghost.length; r++) {
                for (int q = 0; q < ghost[r].length(); q++) {
                    if (ghost[r].charAt(q) != '#') continue;
                    square(layer, gx + (q - 4) * G * 1.5f, gy + (r - 3) * G * 1.5f, G * 1.5f, ChopScene.alpha(a * 0.6f * (1f - k)) | 0xD8F0FF);
                }
            }
        }
    }

    // ------------------------------------------------------------------ the Primordial Eye

    static final long E_SNAP = 4000;

    static void eyeFront(ChopScene.Canvas c, CrystalVeins.Fill layer, SlayerTimeline tl, long u, float a) {
        if (u < 1400) return;
        float[] socket = SlayerScene.screen(HEAD[0] + 2f, HEAD[1] + 2f, HEAD[2] + 8f);
        float[] eye = itemAt(tl, ENDING + u);
        if (eye == null) return;
        // The nerve: from the socket to the eye, stretching thin as it is pulled, pulsing; it snaps at E_SNAP.
        if (u > 2600 && u < E_SNAP + 500) {
            boolean snapped = u > E_SNAP;
            int n = 26;
            float tension = clamp((u - 2900) / 1100f);
            for (int i = 0; i < n; i++) {
                float k = i / (float) (n - 1);
                if (snapped) {
                    float back = ease((u - E_SNAP) / 300f);
                    if (k > 0.5f) continue;
                    k *= 1f - back;
                }
                float x = socket[0] + (eye[0] - socket[0]) * k, y = socket[1] + (eye[1] - socket[1]) * k;
                float wob = (float) Math.sin(k * Math.PI * 3 + u / 50.0) * G * (1f - tension) * 2f;
                int col = (i % 4 == 0) ? 0xFF8A9A : 0xC81E2E;
                square(layer, x + wob, y, G * (tension > 0.7f ? 1 : 2), ChopScene.alpha(a) | col);
            }
        }
        if (u > E_SNAP && u < E_SNAP + 1200) {
            gore(layer, tl, new float[]{HEAD[0] + 2f, HEAD[1] + 2f, HEAD[2] + 8f}, u - E_SNAP, 90, a, 100, 0xA0141A, 0x6AA02A);
            if (u < E_SNAP + 200) ChopScene.glow(c, socket[0], socket[1], 100f * ChopScene.size, 0xFF3030, 0.7f * a * (1f - (u - E_SNAP) / 200f));
        }
        // Its stare: red rings pulsing out of it once it is free.
        if (u > E_SNAP + 300) {
            for (int r = 0; r < 3; r++) {
                float k = (((u - E_SNAP - 300) / 900f) + r / 3f) % 1f;
                ring(layer, eye[0], eye[1], eye[2] * 0.6f + H * 0.3f * k, ChopScene.alpha(a * (1f - k) * 0.6f * (1f - tl.rise(ENDING + u))) | 0xFF2A2A);
            }
        }
        ChopScene.glow(c, eye[0], eye[1], eye[2] * 3f, 0xFF3020, 0.35f * a);
    }

    // ------------------------------------------------------------------ catalysts' silk

    static void silk(ChopScene.Canvas c, SlayerTimeline tl, long u, float a) {
        if (u < 100 || u > 900) return;
        float[] at = SlayerScene.screen(CHEST[0], 9f, CHEST[2]);
        float lw = Math.max(1f, G * 0.5f);
        for (int i = 0; i < 10; i++) {
            float ang = i * 0.628f;
            float k = clamp((u - 100 - i * 30) / 300f);
            float r = H * 0.3f * (1f - k);
            c.line(at[0] + (float) Math.cos(ang) * r, at[1] + (float) Math.sin(ang) * r, at[0], at[1], lw, ChopScene.alpha(a * 0.7f * (1f - span(u, 700, 200))) | 0xF0F0F0);
        }
    }

    static final float BOSS_YAW = SlayerScene.BOSS_YAW;
}
