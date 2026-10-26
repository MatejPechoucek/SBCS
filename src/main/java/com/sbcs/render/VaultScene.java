package com.sbcs.render;

import com.sbcs.anim.VaultTimeline;
import com.sbcs.anim.VaultTimeline.Ending;

import java.util.Random;

import static com.sbcs.anim.VaultTimeline.ease;

/**
 * Draws a frame of "The Vault" (the Crystal Nucleus loot bundle) onto a {@link ChopScene.Canvas} - the same routine
 * for the game and the offline preview, on the Chopping Block's box renderer (its camera, boxes and ink outline).
 *
 * The safe: a steel body with a recess behind a door, the door's face the game's own vault texture, a dial (the
 * vault's top texture) and five sockets round it, five brass bolts along its free edge, hinges on the right. Every
 * texture is the game's, adopted at runtime, never bundled. Effects are pixel art: squares on a grid a GUI-scaled cell
 * across, on a PixelLayer.
 */
public final class VaultScene {
    private VaultScene() {}

    public static final String TEX_DOOR = "vault_door", TEX_DIAL = "vault_dial", TEX_STEEL = "vault_steel",
            TEX_DARK = "vault_dark";
    /** The game's own textures each one is adopted from (textures/block/...). */
    public static final String[][] VANILLA = {
            {TEX_DOOR, "vault_bottom"}, {TEX_DIAL, "vault_top"}, {TEX_STEEL, "iron_block"}, {TEX_DARK, "polished_blackstone"}};
    /** A plain white texture, tinted - the crystals. */
    static final String TEX_WHITE = SeraphScene.TEX_WHITE;

    /** "your 3rd" after the odds; the dry streak under a bust. Set by the screen each frame; null for none. */
    public static String nthNote, bustNote;
    /** The menu's banner switch, set by the screen each frame. */
    public static boolean showBanner = true;
    /** Draws the pixel layer at once, before the showcase, so the drop and its words stand over it. Set by the screen. */
    public static Runnable flushLayer;

    /** The cell grid the pixel effects sit on, in GUI pixels. */
    public static int unitFor(int h) {
        return Math.max(2, Math.round(h / 120f));
    }

    // ------------------------------------------------------------------ the safe, world units

    static final float BODY_X = 10.5f, BODY_Y = 11.5f, BODY_Z = 8f;
    static final float DOOR_X = 8.6f, DOOR_Y = 9.6f, DOOR_Z = 1.0f;
    static final float RECESS = 3.6f;
    /**
     * The sockets' ring. At 6 with a socket at the top, the one at upper left sat under the bolts (Matej, 2026-10-22:
     * "the locks clip the crystal"); at 5 the dial's corners swept through all five as it spun (Matej's friends,
     * 2026-10-23). Now the ring points its odd socket at the hinge, so the two on the bolts' side sit at +-144 degrees,
     * well in from the edge, and it is wide enough that the dial's corners ({@link #DIAL_H} * sqrt 2) clear every rim.
     */
    static final float SOCKET_R = 6.0f;
    /** The dial's half-size: its corners reach 3.5, the sockets' rims start at 4.15. */
    static final float DIAL_H = 2.45f;
    /** The bolts: half their length, where they sit across the door's free edge into the frame, how far they draw back. */
    static final float BOLT_HX = 1.1f, BOLT_X = -DOOR_X - 0.25f, BOLT_TRAVEL = 1.2f;
    static final float[] BOLT_Y = {7.2f, 3.6f, 0f, -3.6f, -7.2f};

    static final int[] FULL = {0, 0, 16, 16};
    static final int[][] FULL6 = {FULL, FULL, FULL, FULL, FULL, FULL};

    public static void draw(ChopScene.Canvas c, CrystalVeins.Fill layer, VaultTimeline tl, long t, int w, int h) {
        float master = tl.master(t);
        c.fill(0, 0, w, h, ChopScene.alpha(tl.dim(t)) | 0x07080C);
        if (master <= 0.01f) return;
        ChopScene.lightCount = 0;
        ChopScene.gild = null;
        ChopScene.glintHook = null;
        ChopScene.insideOut = false;
        ChopScene.yShift = 0f;
        ChopScene.inkWidth = 0f;
        camera(tl, t, w, h);
        unit = unitFor(h);

        // The safe goes dark once the drop is out, so the drop stands off it - darkened, not faded: faded, every face
        // showed through the one in front of it.
        float back = tl.prize().win() ? ease((t - tl.arrive() + 500) / 600f) : 0f;
        shade = 1f - 0.55f * back;
        ChopScene.sceneAlpha = master;
        grey = tl.frozen(t);

        // A faint light behind the safe in the dial's colour, so it stands off the dimmed game.
        ChopScene.project(0f, 0f, 0f);
        int dial = tl.dialRgb(t);
        if (t >= VaultTimeline.WIND) {
            float a = t >= VaultTimeline.REVEAL ? (tl.ending().win() ? 0.4f : 0.12f) : 0.16f + 0.18f * VaultTimeline.flash(t);
            ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], 44f * ChopScene.size, greyed(dial), a);
        }

        safe(c, tl, t);
        effects(c, layer, tl, t, w, h);

        ChopScene.sceneAlpha = master;
        shade = 1f;
        if (tl.prize().win()) showcase(c, layer, tl, t, w, h);
        else bustWords(c, tl, t, w, h);
        if (flushLayer != null) flushLayer.run();
        ChopScene.sceneAlpha = 1f;
        ChopScene.lightCount = 0;
    }

    static int unit = 2;
    /** How grey the frozen world is (the Jade Dye), 0..1. */
    static float grey;
    /** How lit the safe is: 1, darker behind the showcase. */
    static float shade = 1f;

    // ================================================================== camera

    static void camera(VaultTimeline tl, long t, int w, int h) {
        double yaw = Math.toRadians(24f + 4f * (float) Math.sin(t / 1700.0));
        double pitch = Math.toRadians(-12f);
        ChopScene.cy = (float) Math.cos(yaw);
        ChopScene.sy = (float) Math.sin(yaw);
        ChopScene.cp = (float) Math.cos(pitch);
        ChopScene.sp = (float) Math.sin(pitch);
        float push = 1f + 0.10f * ease(t / (float) VaultTimeline.REVEAL) + 0.03f * VaultTimeline.flash(t);
        ChopScene.size = h * 0.7f / 30f * Math.max(0.01f, VaultTimeline.doorIn(t)) * push;
        ChopScene.fx = 0f;
        ChopScene.fy = 0f;
        ChopScene.fz = 0f;
        float s = ChopScene.calm ? 0f : tl.shake(t) * h * Punch.shake();
        ChopScene.sx0 = w / 2f + s * (float) Math.sin(t * 0.13);
        ChopScene.sy0 = h * 0.5f + s * (float) Math.cos(t * 0.17);
    }

    // ================================================================== the safe

    /** The door's frame: centre and axes. */
    static final float[] DC = new float[3], DX = new float[3], DY = new float[3], DZ = new float[3];
    static float doorAlpha = 1f, doorHy = DOOR_Y, doorHz = DOOR_Z, doorHx = DOOR_X;

    static void safe(ChopScene.Canvas c, VaultTimeline tl, long t) {
        doorFrame(tl, t);
        float melt = tl.melt(t);

        // The outline: the body and the door, stamped in black behind everything.
        ChopScene.inkWidth = Math.max(1.2f, ChopScene.size * 0.22f);
        ChopScene.box(0f, 0f, 0f, 0f, 0f, 0f, BODY_X, BODY_Y, BODY_Z, ChopScene.all(TEX_STEEL), FULL6, 1, 1, 1, 0, 1, c);
        if (doorAlpha > 0.02f) {
            ChopScene.boxAxes(DC[0], DC[1], DC[2], DX, DY, DZ, doorHx, doorHy, doorHz, ChopScene.all(TEX_STEEL), FULL6,
                    1, 1, 1, 0, doorAlpha, c);
        }
        ChopScene.inkWidth = 0f;

        // The recess, seen through the open door: its far walls only.
        float inner = tl.inner(t);
        int light = tl.prize().light();
        ChopScene.insideOut = true;
        float[] k = greyed(0.30f, 0.30f, 0.34f);
        ChopScene.box(0f, 0f, BODY_Z - RECESS, 0f, 0f, 0f, DOOR_X - 0.1f, DOOR_Y - 0.1f, RECESS, ChopScene.all(TEX_DARK),
                new int[][]{FULL, FULL, FULL, FULL, null, FULL}, k[0], k[1], k[2], 0.25f * inner, 1f, c);
        ChopScene.insideOut = false;
        if (inner > 0.01f) {
            ChopScene.project(0f, 0f, BODY_Z - RECESS);
            ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], 17f * ChopScene.size, greyed(light), 0.85f * inner);
            ChopScene.light(0f, 0f, BODY_Z + 3f, 26f, greyed(light), 0.9f * inner);
        }

        // The body, but its front: that is the frame round the opening, drawn over the recess's edges.
        k = greyed(0.42f, 0.45f, 0.53f);
        ChopScene.box(0f, 0f, 0f, 0f, 0f, 0f, BODY_X, BODY_Y, BODY_Z, ChopScene.all(TEX_STEEL),
                new int[][]{FULL, FULL, FULL, FULL, null, FULL}, k[0], k[1], k[2], 0f, 1f, c);
        frame(c, k);
        hinges(c, tl, t);

        // The door, with what is on it: on its far side while it is turned away, so it covers them.
        boolean facing = -DZ[1] * ChopScene.sp + (-DZ[0] * ChopScene.sy + DZ[2] * ChopScene.cy) * ChopScene.cp > 0f;
        if (doorAlpha > 0.02f) {
            if (!facing) fittings(c, tl, t);
            door(c, tl, t);
            if (facing) fittings(c, tl, t);
        }

        // A win's light blooming out of the open vault, over the door's edge.
        if (inner > 0.01f && tl.ending().win() && tl.ending() != Ending.DYE || (tl.ending() == Ending.DYE && t >= VaultTimeline.THAW)) {
            ChopScene.project(0f, 0f, BODY_Z + 3f);
            ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], 30f * ChopScene.size, greyed(light), 0.35f * inner);
        }
        if (melt > 0f) {
            ChopScene.project(0f, -BODY_Y + 1f, BODY_Z + 4f);
            ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], 34f * ChopScene.size, 0xFFB030, 0.5f * melt);
        }
    }

    /** Works out where the door is: on its hinge, straining, blown off, or melting. */
    static void doorFrame(VaultTimeline tl, long t) {
        float ang = tl.doorAngle(t);
        float flight = tl.doorFlight(t);
        float strain = VaultTimeline.strain(t);
        float jx = strain * 0.22f * (float) Math.sin(t * 0.9), jy = strain * 0.18f * (float) Math.cos(t * 1.3);
        float ax = 0f, ay = ang, az = 0f;
        float lx = -DOOR_X, lz = DOOR_Z;
        float cb = (float) Math.cos(ay), sb = (float) Math.sin(ay);
        float cx = DOOR_X + lx * cb + lz * sb + jx, cyw = jy, cz = BODY_Z - lx * sb + lz * cb;
        // A Flawless gem's knocks from inside: the door jumps out at the camera.
        cz += 0.55f * tl.knock(t);
        doorAlpha = 1f;
        doorHx = DOOR_X;
        doorHy = DOOR_Y;
        doorHz = DOOR_Z;
        if (flight >= 0f) {
            // Blown off its hinges: up, out towards the camera and to the right, tumbling.
            cx += 15f * flight;
            cyw += 24f * flight - 34f * flight * flight;
            cz += 34f * flight;
            ay += 3.4f * flight;
            ax -= 2.3f * flight;
            az += 1.3f * flight;
            doorAlpha = 1f - ease((flight - 0.45f) / 0.35f);
        }
        float melt = tl.melt(t);
        if (melt > 0f) {
            // It slumps from the top down and spreads at its foot, then is gone into the pool.
            doorHy = DOOR_Y * (1f - 0.88f * melt);
            cyw = -DOOR_Y + doorHy;
            doorHx = DOOR_X * (1f + 0.18f * melt);
            doorHz = DOOR_Z * (1f + 1.6f * melt);
            doorAlpha = 1f - ease((melt - 0.7f) / 0.3f);
        }
        float ca = (float) Math.cos(ax), sa = (float) Math.sin(ax);
        cb = (float) Math.cos(ay);
        sb = (float) Math.sin(ay);
        float cc = (float) Math.cos(az), sc = (float) Math.sin(az);
        set(DX, cb * cc, cb * sc, -sb);
        set(DY, sa * sb * cc - ca * sc, sa * sb * sc + ca * cc, sa * cb);
        set(DZ, ca * sb * cc + sa * sc, ca * sb * sc - sa * cc, ca * cb);
        set(DC, cx, cyw, cz);
    }

    static void set(float[] v, float x, float y, float z) {
        v[0] = x;
        v[1] = y;
        v[2] = z;
    }

    /** A point on the door, in its own frame (z out of its face), to the world. */
    static float[] onDoor(float lx, float ly, float lz) {
        return new float[]{DC[0] + DX[0] * lx + DY[0] * ly + DZ[0] * lz, DC[1] + DX[1] * lx + DY[1] * ly + DZ[1] * lz,
                DC[2] + DX[2] * lx + DY[2] * ly + DZ[2] * lz};
    }

    static void door(ChopScene.Canvas c, VaultTimeline tl, long t) {
        float heat = tl.heat(t), melt = tl.melt(t);
        float[] steel = greyed(0.45f, 0.48f, 0.56f), face = greyed(0.92f, 0.95f, 1f);
        float glow = 0f;
        if (heat > 0f) {
            // Divan's Alloy: the steel heats through red to white gold.
            steel = mix3(steel, new float[]{1f, 0.55f, 0.16f}, heat);
            face = mix3(face, new float[]{1f, 0.62f, 0.2f}, heat);
            glow = 0.62f * heat * heat + 0.2f * melt;
        }
        String[] tex = {TEX_STEEL, TEX_STEEL, TEX_STEEL, TEX_STEEL, TEX_DOOR, TEX_STEEL};
        // Its face, then its edges and back - two calls, so each takes its own tint.
        ChopScene.boxAxes(DC[0], DC[1], DC[2], DX, DY, DZ, doorHx, doorHy, doorHz, tex,
                new int[][]{FULL, FULL, FULL, FULL, null, FULL}, steel[0], steel[1], steel[2], glow, doorAlpha, c);
        ChopScene.boxAxes(DC[0], DC[1], DC[2], DX, DY, DZ, doorHx, doorHy, doorHz, tex,
                new int[][]{null, null, null, null, FULL, null}, face[0], face[1], face[2], glow, doorAlpha, c);
        if (heat > 0.02f) {
            // A tint can only darken, and the door's face is dark: the heat goes over it, the face showing through
            // as relief - dull red, orange, then white gold, swelling as each crystal pours in.
            float pulse = 0f;
            for (long p : VaultTimeline.POURS) if (t >= p) pulse = Math.max(pulse, (float) Math.exp(-(t - p) / 180.0));
            float k = Math.min(1f, heat + 0.15f * pulse);
            int hot = k < 0.5f ? ChopScene.mix(0x7A1408, 0xFF6A14, k / 0.5f) : ChopScene.mix(0xFF6A14, 0xFFE9A8, (k - 0.5f) / 0.5f);
            float[] hc = rgbF(hot);
            ChopScene.boxAxes(DC[0], DC[1], DC[2], DX, DY, DZ, doorHx + 0.01f, doorHy + 0.01f, doorHz + 0.01f,
                    ChopScene.all(TEX_WHITE), FULL6, hc[0], hc[1], hc[2], 0.15f * k, doorAlpha * Math.min(0.82f, 0.1f + 0.8f * k), c);
            ChopScene.project(DC[0], DC[1], DC[2] + doorHz);
            ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], (22f + 10f * k) * ChopScene.size, hot, (0.25f + 0.3f * pulse) * k * doorAlpha);
        }
    }

    /** The dial, the sockets with their crystals, and the bolts. */
    static void fittings(ChopScene.Canvas c, VaultTimeline tl, long t) {
        float a = doorAlpha * (1f - ease(tl.melt(t) / 0.35f));
        if (a <= 0.02f) return;
        float heat = tl.heat(t);
        float front = doorHz;

        // The sockets: dark plates let into the face.
        for (int k = 0; k < 5; k++) {
            float[] p = socket(k);
            float[] s = onDoor(p[0], p[1], front + 0.2f);
            // A brass rim, then the dark plate, which takes a little of its crystal's colour once it is seated.
            float[] rim = greyed(0.62f, 0.5f, 0.3f);
            ChopScene.boxAxes(s[0], s[1], s[2], DX, DY, DZ, 1.4f, 1.85f, 0.12f, ChopScene.all(TEX_STEEL), FULL6,
                    rim[0], rim[1], rim[2], 0f, a, c);
            float seated = VaultTimeline.fly(k, t) >= 1f ? 0.35f : 0f;
            float[] g = greyed(mix3(new float[]{0.22f, 0.22f, 0.26f}, rgbF(VaultTimeline.CRYSTAL_RGB[k]), seated));
            float[] sp = onDoor(p[0], p[1], front + 0.3f);
            ChopScene.boxAxes(sp[0], sp[1], sp[2], DX, DY, DZ, 1.1f, 1.55f, 0.2f, ChopScene.all(TEX_DARK), FULL6,
                    g[0], g[1], g[2], 0.15f * seated, a, c);
        }
        // The crystals that have reached their sockets (the ones still flying are drawn by crystals()).
        for (int k = 0; k < 5; k++) {
            if (VaultTimeline.fly(k, t) < 1f) continue;
            float[] p = socket(k);
            float[] s = onDoor(p[0], p[1], front + 0.4f + 0.6f);
            crystal(c, tl, t, k, s, DX, DY, DZ, a);
        }

        // The dial, turning on the door's face, in the colour it is showing.
        float d = VaultTimeline.dial(t);
        float cd = (float) Math.cos(d), sd = (float) Math.sin(d);
        float[] X = {DX[0] * cd + DY[0] * sd, DX[1] * cd + DY[1] * sd, DX[2] * cd + DY[2] * sd};
        float[] Y = {-DX[0] * sd + DY[0] * cd, -DX[1] * sd + DY[1] * cd, -DX[2] * sd + DY[2] * cd};
        float[] at = onDoor(0f, 0f, front + 0.55f);
        int rgb = tl.dialRgb(t);
        float tint = t < VaultTimeline.WIND ? 0f : 0.35f + 0.35f * VaultTimeline.flash(t);
        if (t >= VaultTimeline.REVEAL) tint = tl.ending().win() ? 0.6f : 0.2f;
        float[] dc = greyed(mix3(new float[]{0.9f, 0.92f, 1f}, rgbF(rgb), tint));
        if (heat > 0f) dc = mix3(dc, new float[]{1f, 0.7f, 0.25f}, heat);
        float dglow = t >= VaultTimeline.WIND && t < VaultTimeline.REVEAL ? 0.25f * VaultTimeline.flash(t) : 0f;
        if (t >= VaultTimeline.REVEAL && tl.ending().win()) dglow = 0.3f * (float) Math.exp(-(t - VaultTimeline.REVEAL) / 300.0);
        dglow = Math.max(dglow, 0.7f * heat);
        String[] dt = {TEX_STEEL, TEX_STEEL, TEX_STEEL, TEX_STEEL, TEX_DIAL, TEX_STEEL};
        ChopScene.boxAxes(at[0], at[1], at[2], X, Y, DZ, DIAL_H, DIAL_H, 0.55f, dt, FULL6, dc[0], dc[1], dc[2], dglow, a, c);
        float[] hub = onDoor(0f, 0f, front + 1.1f + 0.6f);
        float[] hc = greyed(0.85f, 0.72f, 0.42f);
        ChopScene.boxAxes(hub[0], hub[1], hub[2], X, Y, DZ, 0.8f, 0.8f, 0.6f, ChopScene.all(TEX_STEEL), FULL6,
                hc[0], hc[1], hc[2], dglow * 0.5f, a, c);
        if (t >= VaultTimeline.WIND) {
            ChopScene.project(at[0], at[1], at[2]);
            float ga = t >= VaultTimeline.REVEAL ? (tl.ending().win() ? 0.75f : 0.18f) : 0.35f + 0.45f * VaultTimeline.flash(t);
            ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], 12f * ChopScene.size, greyed(rgb), ga * a);
        }

        // The bolts: brass bars across the door's free edge into the frame, pulled back one per crystal.
        float[] bc = greyed(0.86f, 0.72f, 0.42f);
        if (heat > 0f) bc = mix3(bc, new float[]{1f, 0.6f, 0.2f}, heat);
        for (int k = 0; k < 5; k++) {
            float open = tl.boltNow(k, t);
            float rattle = VaultTimeline.strain(t) * 0.12f * (float) Math.sin(t * 0.7 + k * 1.9);
            float[] b = onDoor(BOLT_X + BOLT_TRAVEL * open + rattle, BOLT_Y[k], front + 0.42f);
            ChopScene.boxAxes(b[0], b[1], b[2], DX, DY, DZ, BOLT_HX, 0.5f, 0.42f, ChopScene.all(TEX_STEEL), FULL6,
                    bc[0], bc[1], bc[2], 0.6f * heat, a, c);
        }
        // Last, so a crystal on its way in passes over everything on the door, never through it.
        if (t < VaultTimeline.SOCKETS[4] + 400) crystalsFlying(c, tl, t);
    }

    /** Socket {@code k}'s place on the door's face, round the dial from the top. */
    static float[] socket(int k) {
        double phi = socketAngle(k);
        return new float[]{(float) Math.cos(phi) * SOCKET_R, (float) Math.sin(phi) * SOCKET_R};
    }

    /**
     * The smallest gap, world units, between the door's fittings: the dial's corners and every socket's rim, and every
     * bolt (drawn back as far as it goes, rattling) and every rim. Negative is a clip. For VaultTest.
     */
    public static float clearance() {
        float rimX = 1.4f, rimY = 1.85f, best = Float.MAX_VALUE;
        for (int k = 0; k < 5; k++) {
            float[] p = socket(k);
            // The dial's corners sweep a circle of radius DIAL_H * sqrt 2; the rim's nearest point to the centre.
            float nx = Math.max(Math.abs(p[0]) - rimX, 0f), ny = Math.max(Math.abs(p[1]) - rimY, 0f);
            best = Math.min(best, (float) Math.hypot(nx, ny) - DIAL_H * (float) Math.sqrt(2));
            for (float by : BOLT_Y) {
                float right = BOLT_X + BOLT_TRAVEL + 0.12f + BOLT_HX;
                float gapX = p[0] - rimX - right, gapY = Math.abs(by - p[1]) - rimY - 0.5f;
                best = Math.min(best, Math.max(gapX, gapY));
            }
            // Every rim inside the door's face.
            best = Math.min(best, Math.min(DOOR_X - Math.abs(p[0]) - rimX, DOOR_Y - Math.abs(p[1]) - rimY));
        }
        return best;
    }

    /** Socket {@code k}'s angle on the ring, radians: clockwise from upper left, the fifth on the hinge side. */
    static double socketAngle(int k) {
        return Math.toRadians(144 - 72 * k);
    }

    static void crystal(ChopScene.Canvas c, VaultTimeline tl, long t, int k, float[] at, float[] X, float[] Y, float[] Z, float a) {
        int rgb = VaultTimeline.CRYSTAL_RGB[k];
        float click = VaultTimeline.click(k, t);
        float lit = 0.62f + 0.38f * click;
        if (VaultTimeline.cycle(t) == k && t < VaultTimeline.REVEAL) lit = Math.min(1f, lit + 0.3f + 0.4f * VaultTimeline.flash(t));
        float poured = tl.poured(k, t);
        if (t >= VaultTimeline.REVEAL) {
            float s = ease((t - VaultTimeline.REVEAL) / 400f);
            lit = tl.ending().win() ? lit + (1f - lit) * s : lit - (lit - 0.2f) * s;
            lit *= 1f - 0.75f * poured;
        }
        float[] col = greyed(mix3(new float[]{0.25f, 0.25f, 0.28f}, rgbF(rgb), lit));
        float g = 0.3f * click + (lit > 0.9f ? 0.15f : 0f);
        ChopScene.inkWidth = Math.max(1f, ChopScene.size * 0.18f);
        ChopScene.boxAxes(at[0], at[1], at[2], X, Y, Z, 0.75f, 1.25f, 0.6f, ChopScene.all(TEX_WHITE), FULL6, 1, 1, 1, 0, a, c);
        ChopScene.inkWidth = 0f;
        ChopScene.boxAxes(at[0], at[1], at[2], X, Y, Z, 0.75f, 1.25f, 0.6f, ChopScene.all(TEX_WHITE), FULL6,
                col[0], col[1], col[2], g, a, c);
        ChopScene.project(at[0], at[1], at[2]);
        ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], (5f + 4f * click) * ChopScene.size, greyed(rgb), (0.25f + 0.35f * click) * lit * a);
    }

    /** The crystals still on their way in, from off to the sides, spinning. */
    static void crystalsFlying(ChopScene.Canvas c, VaultTimeline tl, long t) {
        for (int k = 0; k < 5; k++) {
            float f = VaultTimeline.fly(k, t);
            if (f < 0f || f >= 1f) continue;
            float[] q = flightPos(k, f);
            float x = q[0], y = q[1], z = q[2];
            float spin = (1f - ease(f)) * 7f;
            float cs = (float) Math.cos(spin), sn = (float) Math.sin(spin);
            float[] X = {cs, 0f, -sn}, Y = {0f, 1f, 0f}, Z = {sn, 0f, cs};
            crystal(c, tl, t, k, new float[]{x, y, z}, X, Y, Z, Math.min(1f, f * 4f));
        }
    }

    /**
     * Where crystal {@code k} is, {@code f} of the way in: from out in front and a little outward of its socket,
     * coming in square to the door at the end - swept in from the side, it crossed the bolts and the other sockets.
     */
    static float[] flightPos(int k, float f) {
        float[] p = socket(k);
        float[] end = onDoor(p[0], p[1], doorHz + 1.0f);
        double phi = socketAngle(k);
        float sx = end[0] + (float) Math.cos(phi) * 16f, sy = end[1] + (float) Math.sin(phi) * 11f + 4f, sz = end[2] + 30f;
        float e = ease(f);
        float side = 1f - (1f - e) * (1f - e) * (1f - e);
        return new float[]{sx + (end[0] - sx) * side, sy + (end[1] - sy) * side + 3f * (float) Math.sin(f * Math.PI),
                sz + (end[2] - sz) * e};
    }

    /** The frame round the opening: four strips on the body's front, over the recess's edges. */
    static void frame(ChopScene.Canvas c, float[] k) {
        int tint = 0xFF000000 | ChopScene.channel(k[0] * 0.9f) << 16 | ChopScene.channel(k[1] * 0.9f) << 8 | ChopScene.channel(k[2] * 0.9f);
        float z = BODY_Z;
        // left, right, top, bottom
        strip(c, -BODY_X, BODY_Y, DOOR_X * -1f - -BODY_X, 2 * BODY_Y, z, tint);
        strip(c, DOOR_X, BODY_Y, BODY_X - DOOR_X, 2 * BODY_Y, z, tint);
        strip(c, -DOOR_X, BODY_Y, 2 * DOOR_X, BODY_Y - DOOR_Y, z, tint);
        strip(c, -DOOR_X, -DOOR_Y, 2 * DOOR_X, BODY_Y - DOOR_Y, z, tint);
    }

    static void strip(ChopScene.Canvas c, float x0, float y0, float wx, float hy, float z, int tint) {
        int[] rect = {0, 0, Math.max(1, Math.min(16, Math.round(wx * 1.4f))), Math.max(1, Math.min(16, Math.round(hy * 1.4f)))};
        ChopScene.worldQuad(TEX_STEEL, rect, x0, y0, z, wx, 0f, 0f, 0f, -hy, 0f, tint, c);
    }

    /** Two hinges on the frame's right, which stay when the door is blown off. */
    static void hinges(ChopScene.Canvas c, VaultTimeline tl, long t) {
        float[] k = greyed(0.30f, 0.31f, 0.36f);
        for (float y : new float[]{6.2f, -6.2f}) {
            ChopScene.box(DOOR_X + 0.6f, y, BODY_Z + 0.7f, 0f, 0f, 0f, 0.8f, 1.8f, 0.7f, ChopScene.all(TEX_STEEL), FULL6,
                    k[0], k[1], k[2], 0f, 1f, c);
        }
    }

    // ================================================================== effects: squares on the pixel layer

    static void effects(ChopScene.Canvas c, CrystalVeins.Fill layer, VaultTimeline tl, long t, int w, int h) {
        float u = unit;
        float a = ChopScene.sceneAlpha;
        Random r = new Random(tl.seed() * 31 + 5);
        // Each crystal's click: a ring of sparks in its colour.
        for (int k = 0; k < 5; k++) {
            long since = t - VaultTimeline.SOCKETS[k];
            if (since < 0 || since > 380) continue;
            float[] p = socket(k);
            float[] s = onDoor(p[0], p[1], doorHz + 1f);
            ChopScene.project(s[0], s[1], s[2]);
            float k01 = since / 380f;
            for (int i = 0; i < 9; i++) {
                double ang = i * Math.PI * 2 / 9 + k;
                float d = (3f + 9f * k01) * ChopScene.size;
                square(layer, ChopScene.P[0] + (float) Math.cos(ang) * d, ChopScene.P[1] + (float) Math.sin(ang) * d, u,
                        ChopScene.alpha(a * (1f - k01)) | greyed(VaultTimeline.CRYSTAL_RGB[k]));
            }
        }
        // The bolts giving: a spark off each as it slides.
        for (int k = 0; k < 5; k++) {
            long since = t - VaultTimeline.SOCKETS[k] - VaultTimeline.BOLT_DELAY;
            if (since < 0 || since > 260) continue;
            float[] b = onDoor(BOLT_X - BOLT_HX, BOLT_Y[k], doorHz + 0.4f);
            ChopScene.project(b[0], b[1], b[2]);
            float k01 = since / 260f;
            for (int i = 0; i < 4; i++) {
                float dx = -(2f + 6f * k01) * ChopScene.size * (0.6f + 0.3f * i), dy = (i - 1.5f) * 1.6f * ChopScene.size * k01;
                square(layer, ChopScene.P[0] + dx, ChopScene.P[1] + dy + 4f * k01 * k01 * ChopScene.size, u,
                        ChopScene.alpha(a * (1f - k01)) | (i % 2 == 0 ? 0xFFE7A0 : 0xFFB040));
            }
        }
        trails(layer, tl, t);
        switch (tl.ending()) {
            case BUST -> {
                switch (tl.bustStyle()) {
                    case CRACK -> dust(layer, tl, t, r);
                    case SWING -> {
                        dust(layer, tl, t, r);
                        moth(layer, tl, t);
                    }
                    case JAM -> jam(layer, tl, t, r);
                }
            }
            case OPEN -> {
                if (tl.openStyle() == VaultTimeline.OpenStyle.SLAM) slamSparks(layer, tl, t, r);
                // The held crack: the vault's light round the door's free edge until it swings.
                long from = VaultTimeline.REVEAL + VaultTimeline.HOLD;
                if (t >= from && t < VaultTimeline.SWING + 250) {
                    float on = ease((t - from) / 220f) * (1f - ease((t - VaultTimeline.SWING) / 250f));
                    seam(c, layer, t, on, 0f, tl.prize().light(), 0xFFFFFF);
                }
            }
            case BLOWN -> {
                // Each knock forces more light out round the door, until it goes.
                if (tl.knocked(t) > 0 && t < VaultTimeline.BLOW) {
                    float on = 0.35f + 0.2f * tl.knocked(t);
                    seam(c, layer, t, on, tl.knock(t), tl.prize().light(), 0xFFFFFF);
                }
                blast(layer, tl, t, r, false);
            }
            case ALLOY -> meltdown(layer, tl, t, r, w, h);
            case DYE -> {
                jadeSeam(c, layer, tl, t);
                blast(layer, tl, t, r, true);
            }
            default -> { }
        }
    }

    /** A bust that just swung open: a moth flutters out of the empty vault and away up to the left. */
    static void moth(CrystalVeins.Fill layer, VaultTimeline tl, long t) {
        float s = (t - VaultTimeline.REVEAL - 700) / 1600f;
        if (s < 0f || s > 1f) return;
        float x = -2f - 16f * s + 1.5f * (float) Math.sin(s * 19), y = -1f + 14f * s + 1.2f * (float) Math.sin(s * 31);
        ChopScene.project(x, y, BODY_Z + 3f + 6f * s);
        float a = ChopScene.sceneAlpha * Math.min(1f, (1f - s) * 3f);
        boolean up = ((t / 70) & 1) == 0;
        square(layer, ChopScene.P[0], ChopScene.P[1], unit, ChopScene.alpha(a) | 0xB8AE96);
        square(layer, ChopScene.P[0] - unit, ChopScene.P[1] - (up ? unit : 0), unit, ChopScene.alpha(a) | 0xD8CEB4);
        square(layer, ChopScene.P[0] + unit, ChopScene.P[1] - (up ? unit : 0), unit, ChopScene.alpha(a) | 0xD8CEB4);
    }

    /** A jammed bust: a spark off each bolt as it slams home, and grey smoke curling off the door's edge. */
    static void jam(CrystalVeins.Fill layer, VaultTimeline tl, long t, Random r) {
        for (int k = 0; k < 5; k++) {
            long since = t - VaultTimeline.jamBolt(k);
            if (since < 0 || since > 300) continue;
            float[] b = onDoor(BOLT_X - BOLT_HX, BOLT_Y[k], doorHz + 0.4f);
            ChopScene.project(b[0], b[1], b[2]);
            float k01 = since / 300f;
            for (int i = 0; i < 6; i++) {
                double ang = Math.PI * (0.6 + 0.8 * i / 5.0);
                float d = (1.5f + 7f * k01) * ChopScene.size;
                square(layer, ChopScene.P[0] + (float) Math.cos(ang) * d, ChopScene.P[1] - (float) Math.sin(ang) * d + 6f * k01 * k01 * ChopScene.size,
                        unit, ChopScene.alpha(ChopScene.sceneAlpha * (1f - k01)) | (i % 2 == 0 ? 0xFFE7A0 : 0xFF9A30));
            }
        }
        long since = t - VaultTimeline.jamBolt(0);
        if (since < 0) return;
        for (int i = 0; i < 24; i++) {
            float delay = r.nextFloat() * 700f, life = 1100f + r.nextFloat() * 600f;
            float y0 = (r.nextFloat() * 2f - 1f) * DOOR_Y * 0.9f;
            float s = (since - delay) / life;
            if (s < 0f || s > 1f) continue;
            float rise = 1f - (1f - s) * (1f - s);
            ChopScene.project(-DOOR_X - 0.3f - 2.5f * rise + 0.8f * (float) Math.sin(s * 9 + i), y0 + 6f * rise, BODY_Z + 1.5f + 2f * rise);
            int col = i % 2 == 0 ? 0x6A6A70 : 0x8A8A90;
            square(layer, ChopScene.P[0], ChopScene.P[1], unit * (s < 0.5f ? 2 : 1), ChopScene.alpha(ChopScene.sceneAlpha * 0.7f * (1f - s)) | col);
        }
    }

    /** A rare roll kicked open: sparks off the hinges and the safe's side as the door bangs into it. */
    static void slamSparks(CrystalVeins.Fill layer, VaultTimeline tl, long t, Random r) {
        long since = t - VaultTimeline.slamHit();
        if (since < 0 || since > 650) return;
        float k01 = since / 650f;
        int light = tl.prize().light();
        for (int i = 0; i < 30; i++) {
            float vx = 2f + r.nextFloat() * 10f, vy = (r.nextFloat() * 2f - 0.6f) * 9f, vz = r.nextFloat() * 8f;
            float y0 = (r.nextFloat() * 2f - 1f) * DOOR_Y;
            ChopScene.project(BODY_X + vx * k01, y0 + vy * k01 - 10f * k01 * k01, BODY_Z + 1f + vz * k01);
            int col = i % 3 == 0 ? 0xFFFFFF : i % 3 == 1 ? 0xFFD27A : light;
            square(layer, ChopScene.P[0], ChopScene.P[1], unit, ChopScene.alpha(ChopScene.sceneAlpha * (1f - k01)) | col);
        }
    }

    /** A bust: dust puffing out of the crack. */
    static void dust(CrystalVeins.Fill layer, VaultTimeline tl, long t, Random r) {
        long since = t - VaultTimeline.REVEAL - 200;
        if (since < 0) return;
        for (int i = 0; i < 28; i++) {
            float delay = r.nextFloat() * 500f, life = 900f + r.nextFloat() * 500f;
            float y0 = (r.nextFloat() * 2f - 1f) * DOOR_Y * 0.9f;
            float vx = -2f - r.nextFloat() * 5f, vy = 0.5f + r.nextFloat() * 2f, vz = 2f + r.nextFloat() * 4f;
            float s = (since - delay) / life;
            if (s < 0f || s > 1f) continue;
            float drift = 1f - (1f - s) * (1f - s);
            ChopScene.project(-DOOR_X + vx * drift, y0 + vy * drift, BODY_Z + 1.5f + vz * drift);
            int col = i % 3 == 0 ? 0x9A9282 : i % 3 == 1 ? 0x7A746A : 0xB4AC9A;
            square(layer, ChopScene.P[0], ChopScene.P[1], unit * (s < 0.4f ? 2 : 1), ChopScene.alpha(ChopScene.sceneAlpha * 0.8f * (1f - s)) | col);
        }
    }

    /**
     * The door blown off: sparks and splinters off its edges, and a fountain of coins (the jackpot) or a flood of jade
     * (the Dye) out of the vault.
     */
    static void blast(CrystalVeins.Fill layer, VaultTimeline tl, long t, Random r, boolean jade) {
        long b = tl.blowAt();
        float since = (t - b) / 1000f;
        if (since < 0f) return;
        int light = tl.prize().light();
        int n = jade ? 280 : 60;
        int[] jadeCols = {0x00AA00, 0x55FF55, 0x2E8B57, 0xA8FFB0};
        for (int i = 0; i < n; i++) {
            float vx = (r.nextFloat() * 2f - 1f) * (jade ? 30f : 22f), vy = 8f + r.nextFloat() * (jade ? 34f : 26f),
                    vz = 8f + r.nextFloat() * 26f;
            float life = 1.0f + r.nextFloat() * 0.9f, delay = r.nextFloat() * (jade ? 0.5f : 0.15f);
            float s = since - delay;
            if (s < 0f || s > life) continue;
            float x = vx * s, y = vy * s - 30f * s * s, z = BODY_Z + vz * s;
            ChopScene.project(x * 0.8f, y, z);
            float fade = 1f - s / life;
            int col = jade ? jadeCols[i % 4] : (i % 5 == 0 ? 0xFFFFFF : i % 2 == 0 ? light : 0xFFE08A);
            square(layer, ChopScene.P[0], ChopScene.P[1], unit * (i % 4 == 0 ? 2 : 1), ChopScene.alpha(ChopScene.sceneAlpha * fade) | col);
        }
        // A ring of light racing out from the opening.
        if (since < 0.6f) {
            ChopScene.project(0f, 0f, BODY_Z);
            float rad = since / 0.6f * 30f * ChopScene.size;
            int col = jade ? 0x55FF55 : light;
            for (int i = 0; i < 40; i++) {
                double ang = i * Math.PI * 2 / 40;
                square(layer, ChopScene.P[0] + (float) Math.cos(ang) * rad, ChopScene.P[1] + (float) Math.sin(ang) * rad * 0.8f,
                        unit, ChopScene.alpha(ChopScene.sceneAlpha * (1f - since / 0.6f)) | col);
            }
        }
    }

    /** The Jade Dye's stopped moment: jade light seeping out of the crack, swelling on each heartbeat. */
    static void jadeSeam(ChopScene.Canvas c, CrystalVeins.Fill layer, VaultTimeline tl, long t) {
        if (t < VaultTimeline.REVEAL || t >= VaultTimeline.THAW + 200) return;
        float on = ease((t - VaultTimeline.REVEAL) / 250f) * (1f - ease((t - VaultTimeline.THAW) / 200f));
        seam(c, layer, t, on, tl.heart(t), 0x55FF55, 0xA8FFB0);
    }

    /**
     * Light escaping round a door that is not quite shut: squares down its free edge, and along its top and bottom
     * once it swells ({@code beat}, 0..1). Never greyed: in the Dye's stopped world it is the only colour left.
     */
    static void seam(ChopScene.Canvas c, CrystalVeins.Fill layer, long t, float on, float beat, int rgb, int pale) {
        float a = ChopScene.sceneAlpha * on * (0.55f + 0.45f * beat);
        int dark = ChopScene.mix(rgb, 0x000000, 0.35f);
        for (float y = -DOOR_Y; y <= DOOR_Y; y += 0.8f) {
            float[] p = onDoor(-DOOR_X - 0.15f, y, 0f);
            ChopScene.project(p[0], p[1], p[2]);
            float wob = (float) Math.sin(y * 1.7f + t / 140.0) * 0.5f + 0.5f;
            square(layer, ChopScene.P[0], ChopScene.P[1], unit, ChopScene.alpha(a) | (wob > 0.5f ? rgb : pale));
            if (beat > 0.3f && wob > 0.7f) {
                square(layer, ChopScene.P[0] - unit * (1 + Math.round(3 * beat)), ChopScene.P[1], unit, ChopScene.alpha(a * 0.6f) | dark);
            }
        }
        if (beat > 0.05f) {
            for (float x = -DOOR_X; x <= DOOR_X; x += 1.1f) {
                for (float y : new float[]{DOOR_Y + 0.15f, -DOOR_Y - 0.15f}) {
                    float[] p = onDoor(x, y, 0f);
                    ChopScene.project(p[0], p[1], p[2]);
                    square(layer, ChopScene.P[0], ChopScene.P[1], unit, ChopScene.alpha(a * beat) | pale);
                }
            }
        }
        float[] mid = onDoor(-DOOR_X, 0f, 0f);
        ChopScene.project(mid[0], mid[1], mid[2]);
        ChopScene.glow(c, ChopScene.P[0], ChopScene.P[1], (14f + 10f * beat) * ChopScene.size, rgb, 0.45f * on + 0.35f * beat);
    }

    /** A crystal in flight leaves a short trail of squares in its colour, and a glint or two behind that. */
    static void trails(CrystalVeins.Fill layer, VaultTimeline tl, long t) {
        for (int k = 0; k < 5; k++) {
            float f = VaultTimeline.fly(k, t);
            if (f <= 0f || f >= 1f) continue;
            for (int i = 1; i <= 7; i++) {
                long back = t - i * 22L;
                float fb = VaultTimeline.fly(k, back);
                if (fb < 0f) break;
                float[] q = flightPos(k, fb);
                ChopScene.project(q[0], q[1], q[2]);
                float fade = 1f - i / 8f;
                int col = i % 3 == 0 ? 0xFFFFFF : VaultTimeline.CRYSTAL_RGB[k];
                square(layer, ChopScene.P[0], ChopScene.P[1], unit * (i < 3 ? 2 : 1), ChopScene.alpha(ChopScene.sceneAlpha * 0.8f * fade) | greyed(col));
            }
        }
    }

    /**
     * Divan's Alloy, "the Meltdown": each crystal pours a beam of its light into the dial; the door glows hotter and
     * hotter, sparks spitting off it; then it slumps into a pool of molten gold at the safe's foot, a fountain of
     * sparks going up, and the pool keeps rippling under the drop.
     */
    static void meltdown(CrystalVeins.Fill layer, VaultTimeline tl, long t, Random r, int w, int h) {
        float a = ChopScene.sceneAlpha;
        // The beams: a line of squares from each crystal to the dial, filling as it pours.
        float[] dial = onDoor(0f, 0f, doorHz + 1.2f);
        for (int k = 0; k < 5; k++) {
            long since = t - VaultTimeline.POURS[k];
            if (since < 0 || since > 900 || tl.melt(t) > 0.2f) continue;
            float[] p = socket(k);
            float[] s = onDoor(p[0], p[1], doorHz + 1.2f);
            ChopScene.project(s[0], s[1], s[2]);
            float x0 = ChopScene.P[0], y0 = ChopScene.P[1];
            ChopScene.project(dial[0], dial[1], dial[2]);
            float x1 = ChopScene.P[0], y1 = ChopScene.P[1];
            float reach = Math.min(1f, since / 240f), fade = 1f - ease((since - 500) / 400f);
            int n = 14;
            for (int i = 0; i <= n; i++) {
                float f = i / (float) n;
                if (f > reach) break;
                float pulse = 0.6f + 0.4f * (float) Math.sin(f * 12f - since / 40f);
                square(layer, x0 + (x1 - x0) * f, y0 + (y1 - y0) * f, unit,
                        ChopScene.alpha(a * fade * pulse) | VaultTimeline.CRYSTAL_RGB[k]);
            }
        }
        // Sparks spitting off the hot door.
        float heat = tl.heat(t), melt = tl.melt(t);
        if (heat > 0.25f && melt < 0.6f) {
            for (int i = 0; i < 40; i++) {
                float period = 0.5f + r.nextFloat() * 0.6f, off = r.nextFloat();
                float ph = ((t / 1000f) / period + off) % 1f;
                if (r.nextFloat() > heat) continue;
                float[] p = onDoor((r.nextFloat() * 2f - 1f) * DOOR_X, (r.nextFloat() * 2f - 1f) * doorHy, doorHz);
                ChopScene.project(p[0] + (r.nextFloat() - 0.5f) * 8f * ph, p[1] + 6f * ph - 9f * ph * ph, p[2] + 5f * ph);
                square(layer, ChopScene.P[0], ChopScene.P[1], unit, ChopScene.alpha(a * (1f - ph)) | (i % 3 == 0 ? 0xFFF4C0 : 0xFFB030));
            }
        }
        if (t < VaultTimeline.MELT) return;
        // The pool: an ellipse of molten gold at the foot, spreading as the door goes, rippling.
        float spread = ease((t - VaultTimeline.MELT) / 1100f);
        ChopScene.project(0f, -BODY_Y, BODY_Z + 5f);
        float cx = ChopScene.P[0], cy = ChopScene.P[1];
        float rx = (5f + 14f * spread) * ChopScene.size, ry = rx * 0.24f;
        int[] golds = {0x8A5A10, 0xC8861E, 0xFFB030, 0xFFD860, 0xFFF4C0};
        for (float y = -ry; y <= ry; y += unit) {
            float span = rx * (float) Math.sqrt(Math.max(0f, 1f - (y * y) / (ry * ry)));
            for (float x = -span; x <= span; x += unit) {
                float d = (float) Math.sqrt((x * x) / (rx * rx) + (y * y) / (ry * ry));
                // Molten, not a target: the bands broken up by a slow noise, a ripple running out through them.
                int gx = Math.round(x / unit), gy = Math.round(y / unit);
                float noise = ChopScene.rnd(Math.floorDiv(gx, 3) + (int) (t / 400), Math.floorDiv(gy, 2) * 7);
                float ripple = 0.5f + 0.5f * (float) Math.sin(d * 9f - t / 160.0 + noise * 2.5f);
                int band = Math.min(4, Math.max(0, Math.round((1f - d) * 2.4f + ripple * 1.1f + noise * 0.9f - 0.3f)));
                float edge = d > 0.85f ? (1f - d) / 0.15f : 1f;
                square(layer, cx + x, cy + y, unit, ChopScene.alpha(a * Math.max(0f, edge)) | golds[band]);
            }
        }
        // The fountain of sparks as it goes.
        float since = (t - VaultTimeline.MELT) / 1000f;
        for (int i = 0; i < 70; i++) {
            float vx = (r.nextFloat() * 2f - 1f) * 18f, vy = 14f + r.nextFloat() * 30f, vz = r.nextFloat() * 14f;
            float life = 0.9f + r.nextFloat() * 0.8f, delay = r.nextFloat() * 0.7f;
            float s = since - delay;
            if (s < 0f || s > life) continue;
            ChopScene.project(vx * s, -BODY_Y + 2f + vy * s - 32f * s * s, BODY_Z + 4f + vz * s);
            square(layer, ChopScene.P[0], ChopScene.P[1], unit * (i % 5 == 0 ? 2 : 1),
                    ChopScene.alpha(a * (1f - s / life)) | (i % 3 == 0 ? 0xFFFFFF : i % 3 == 1 ? 0xFFD860 : 0xFFAA00));
        }
    }

    static void square(CrystalVeins.Fill layer, float x, float y, float s, int argb) {
        if ((argb >>> 24) < 3) return;
        layer.fill(Math.round(x / unit) * unit, Math.round(y / unit) * unit, s, s, argb);
    }

    // ================================================================== the showcase

    static void showcase(ChopScene.Canvas c, CrystalVeins.Fill layer, VaultTimeline tl, long t, int w, int h) {
        float rise = tl.itemRise(t);
        if (rise <= 0f) return;
        Ending e = tl.ending();
        float sx, sy;
        if (e == Ending.ALLOY) {
            ChopScene.project(0f, -BODY_Y, BODY_Z + 5f);
        } else {
            ChopScene.project(0f, 0f, BODY_Z - RECESS);
        }
        sx = ChopScene.P[0];
        sy = ChopScene.P[1];
        float rx = w / 2f, ry = h * 0.40f;
        float x = sx + (rx - sx) * rise, y = sy + (ry - sy) * rise;
        float big = switch (e) {
            case OPEN -> 0.15f;
            case BLOWN -> 0.18f;
            default -> 0.21f;
        };
        float size = h * big * Math.min(1f, 0.3f + rise);
        float show = ease((t - tl.arrive() + 400) / 500f);
        if (show > 0f) rays(layer, tl, x, y, h, t, show);
        ChopScene.glow(c, x, y, size * 2.6f, tl.prize().light(), 0.45f * Math.min(1f, rise));
        if (flushLayer != null) flushLayer.run();
        float yaw = 32f + 10f * (float) Math.sin(t / 700.0);
        ChopScene.showItem(c, x, y, size, ChopScene.sceneAlpha * Math.min(1f, rise * 2f), yaw);
        float ts = Math.min(1.35f, h / 300f);
        banner(c, tl, t, x, y - size * 0.62f - 12f * ts, h);
        labels(c, tl, t, x, y + size * 0.62f, h);
    }

    /** Pixel rays turning behind the drop, in its light. */
    static void rays(CrystalVeins.Fill layer, VaultTimeline tl, float x, float y, int h, long t, float alpha) {
        float u = unit;
        boolean big = tl.banner();
        int n = big ? 12 : 8;
        float turn = t / 2600f;
        float len = h * (big ? 0.5f : 0.34f);
        int light = tl.prize().light();
        for (int i = 0; i < n; i++) {
            float ang = turn + i * (float) Math.PI * 2f / n;
            float cs = (float) Math.cos(ang), sn = (float) Math.sin(ang);
            for (float r = h * 0.08f; r < len; r += u * 1.5f) {
                float fall = 1f - r / len;
                int col = i % 2 == 0 ? light : ChopScene.mix(light, 0xFFFFFF, 0.45f);
                square(layer, x + cs * r, y + sn * r * 0.8f, u * (fall > 0.6f ? 2 : 1),
                        ChopScene.alpha(alpha * ChopScene.sceneAlpha * fall * fall * (big ? 0.7f : 0.5f)) | col);
            }
        }
    }

    static void banner(ChopScene.Canvas c, VaultTimeline tl, long t, float x, float y, int h) {
        if (!showBanner) return;
        float k = tl.bannerStamp(t);
        if (k <= 0f) return;
        float ts = Math.min(1.35f, h / 300f);
        float scale = ts * (1.5f + 1.4f * (1f - ease(k)));
        float a = Math.min(1f, k * 2.5f) * ChopScene.sceneAlpha;
        int rgb = tl.bannerRgb();
        if (k < 1f) ChopScene.glow(c, x, y, 160f * ts, rgb, 0.6f * (1f - k));
        c.text(tl.bannerWord(), x, y, scale, ChopScene.alpha(a) | rgb);
    }

    static void labels(ChopScene.Canvas c, VaultTimeline tl, long t, float x, float below, int h) {
        float na = tl.nameAlpha(t) * ChopScene.sceneAlpha;
        float ts = Math.min(1.35f, h / 300f);
        int colour = tl.prize().colour();
        if (na > 0.02f) c.name(x, below + 8f * ts, 1.6f * ts, ChopScene.alpha(na) | colour, tl.letters(t));
        float la = tl.lineAlpha(t) * ChopScene.sceneAlpha;
        if (la <= 0.02f) return;
        long oneIn = tl.prize().oneIn();
        if (oneIn > 0) {
            c.text("1 in ~" + String.format("%,d", oneIn) + " bundles" + (nthNote == null ? "" : " · " + nthNote),
                    x, below + 22f * ts, ts, ChopScene.alpha(la) | 0xC8C8C8);
        }
        long price = tl.price();
        if (price > 0 && t >= tl.countStart()) {
            float k = Math.min(1f, (t - tl.countStart()) / (float) VaultTimeline.COUNT_MS);
            long shown = Math.round(price * (1f - (1f - k) * (1f - k) * (1f - k)));
            c.text(String.format("%,d coins", shown), x, below + 35f * ts, 1.25f * ts, ChopScene.alpha(la) | 0xFFD84A);
            if (tl.priceSource() != null) c.text(tl.priceSource(), x, below + 45f * ts, 0.7f * ts, ChopScene.alpha(la) | 0x9A9A9A);
        }
    }

    /** A bust's words, under the safe: nothing rare this time, and how long it has been. */
    static void bustWords(ChopScene.Canvas c, VaultTimeline tl, long t, int w, int h) {
        float a = tl.bustText(t) * ChopScene.sceneAlpha;
        if (a <= 0.02f) return;
        float ts = Math.min(1.35f, h / 300f);
        ChopScene.project(0f, -BODY_Y, BODY_Z);
        float y = Math.min(h - 30f * ts, ChopScene.P[1] + 14f * ts);
        c.text("No rare roll", w / 2f, y, 1.3f * ts, ChopScene.alpha(a) | 0xAAAAAA);
        if (bustNote != null) c.text(bustNote, w / 2f, y + 13f * ts, ts, ChopScene.alpha(a) | 0x7F7F7F);
    }

    // ================================================================== colour

    /** A colour towards grey as the Jade Dye stops the world. */
    static int greyed(int rgb) {
        if (shade < 1f) rgb = ChopScene.mix(rgb, 0, 1f - shade);
        if (grey <= 0f) return rgb & 0xFFFFFF;
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        int l = Math.round(0.3f * r + 0.59f * g + 0.11f * b);
        int gr = ChopScene.mix(rgb, l << 16 | l << 8 | l, grey);
        return gr & 0xFFFFFF;
    }

    static float[] greyed(float r, float g, float b) {
        return greyed(new float[]{r, g, b});
    }

    static float[] greyed(float[] c) {
        if (shade < 1f) c = new float[]{c[0] * shade, c[1] * shade, c[2] * shade};
        if (grey <= 0f) return c;
        float l = 0.3f * c[0] + 0.59f * c[1] + 0.11f * c[2];
        return new float[]{c[0] + (l - c[0]) * grey, c[1] + (l - c[1]) * grey, c[2] + (l - c[2]) * grey};
    }

    static float[] mix3(float[] a, float[] b, float k) {
        return new float[]{a[0] + (b[0] - a[0]) * k, a[1] + (b[1] - a[1]) * k, a[2] + (b[2] - a[2]) * k};
    }

    static float[] rgbF(int rgb) {
        return new float[]{(rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f};
    }
}
