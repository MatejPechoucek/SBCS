import com.sbcs.anim.ChopTimeline;
import com.sbcs.anim.SeraphTimeline;
import com.sbcs.anim.SeraphTimeline.Drop;
import com.sbcs.anim.SeraphTimeline.Ending;
import com.sbcs.anim.SeraphTimeline.Glass;
import com.sbcs.audio.SeraphSound;
import com.sbcs.render.ChopScene;
import com.sbcs.render.SeraphScene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Offline checks for "The Seraph's Glyph". The one that matters: nothing drawn or heard before the reveal
 * depends on the drop - every draw call of every frame is hashed and the audio rendered, for every drop,
 * on several seeds, and they must agree.
 *
 *   javac -d out -cp slf4j.jar -sourcepath "src/main/java;tools" tools/SeraphTest.java tools/SeraphSoundtrack.java
 *   java -cp "out;slf4j.jar;src/main/resources" SeraphTest
 */
public final class SeraphTest {
    static int passed, failed;

    static void check(String what, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.out.println("FAIL " + what);
        }
    }

    /** Hashes every primitive drawn. */
    static final class Hash implements ChopScene.Canvas {
        long h = 1125899906842597L;

        void mix(float... v) {
            for (float f : v) h = 31 * h + Float.floatToIntBits(Math.round(f * 100f) / 100f);
        }

        void mix(String s) { h = 31 * h + (s == null ? 0 : s.hashCode()); }

        public void quad(String tex, float ox, float oy, float ux, float uy, float vx, float vy, int u0, int v0, int uw, int vh, int argb) {
            mix(tex); mix(ox, oy, ux, uy, vx, vy, u0, v0, uw, vh, argb);
        }
        public void sprite(String tex, float cx, float cy, float w, float h2, float rot, int argb) { mix(tex); mix(cx, cy, w, h2, rot, argb); }
        public void line(float x0, float y0, float x1, float y1, float width, int argb) { mix(x0, y0, x1, y1, width, argb); }
        public void fill(float x, float y, float w, float h2, int argb) { mix(x, y, w, h2, argb); }
        public void item(float cx, float cy, float size, float alpha, float yaw) { mix(cx, cy, size, alpha, yaw); mix("item"); }
        public void name(float cx, float cy, float scale, int argb, int letters) { mix(cx, cy, scale, argb, letters); mix("name"); }
        public void text(String text, float cx, float cy, float scale, int argb) { mix(text); mix(cx, cy, scale, argb); }
        public String creatureSkin(ChopTimeline.Creature creature) { return null; }
        public String dropSkin() { return null; }
    }

    static long frames(Drop d, long seed, long from, long to) {
        SeraphTimeline tl = new SeraphTimeline(d, seed);
        Hash hash = new Hash();
        for (long t = from; t < to; t += 16) SeraphScene.draw(hash, hash::fill, tl, t, 640, 360);
        return hash.h;
    }

    public static void main(String[] a) {
        long[] seeds = {1, 7, 42, 99_991, 123_456_789};
        for (long seed : seeds) {
            long ref = frames(Drop.BUST, seed, 0, SeraphTimeline.REVEAL);
            for (Drop d : Drop.values()) {
                check("before the reveal, " + d + " draws the same as a bust (seed " + seed + ")",
                        frames(d, seed, 0, SeraphTimeline.REVEAL) == ref);
            }
        }
        check("a win draws differently from a bust after the reveal",
                frames(Drop.ETHERWARP_MERGER, 7, SeraphTimeline.REVEAL, SeraphTimeline.REVEAL + 600)
                        != frames(Drop.BUST, 7, SeraphTimeline.REVEAL, SeraphTimeline.REVEAL + 600));

        // The sound: everything before the reveal renders to the very same samples for every drop.
        for (long seed : new long[]{3, 77}) {
            float[] base = null;
            for (Drop d : Drop.values()) {
                List<SeraphSound.Cue> before = new ArrayList<>();
                for (SeraphSound.Cue c : SeraphSound.schedule(new SeraphTimeline(d, seed))) if (c.at < SeraphTimeline.REVEAL) before.add(c);
                float[] pcm = SeraphSoundtrack.render(before, SeraphTimeline.REVEAL / 1000f);
                if (base == null) base = pcm;
                else check("before the reveal, " + d + " sounds the same as a bust (seed " + seed + ")", Arrays.equals(base, pcm));
            }
        }

        // The timing.
        SeraphTimeline bust = new SeraphTimeline(Drop.BUST, 1);
        check("a bust is over within 1 s of the reveal (" + bust.total() + ")", bust.total() - SeraphTimeline.REVEAL <= 1000);
        check("the build-up is 4.5-6 s (" + SeraphTimeline.REVEAL + ")", SeraphTimeline.REVEAL >= 4500 && SeraphTimeline.REVEAL <= 6000);
        check("the throw can be followed: lifted for 0.4 s, held, and in the air for 0.4 s or more",
                SeraphTimeline.RELEASE - SeraphTimeline.THROW >= 500 && SeraphTimeline.REVEAL - SeraphTimeline.RELEASE >= 400);
        check("the shield runs out before Broken Heart Radiation", SeraphTimeline.HANG < SeraphTimeline.THROW);
        check("the glyph is thrown after the lasers and lands at the reveal",
                SeraphTimeline.THROW > SeraphTimeline.HANG && SeraphTimeline.throwK(SeraphTimeline.REVEAL) == 1f
                        && SeraphTimeline.throwK(SeraphTimeline.REVEAL - 1) < 1f);
        check("the shield counts 100 hits down to 0", SeraphTimeline.hitsLeft(SeraphTimeline.APPEAR - 1) == 100
                && SeraphTimeline.hitsLeft(SeraphTimeline.HANG) == 0);
        boolean cycle = true;
        for (int i = 0; i < SeraphTimeline.STEPS.length; i++) {
            Glass g = bust.glass(SeraphTimeline.STEPS[i] + 1);
            if (g != SeraphTimeline.CYCLE[i % SeraphTimeline.CYCLE.length]) cycle = false;
        }
        check("every hit ticks the glass one step round the fixed cycle", cycle);
        check("black is in the cycle, so landing on it is believable", Arrays.asList(SeraphTimeline.CYCLE).contains(Glass.BLACK));
        for (Drop d : Drop.values()) {
            SeraphTimeline tl = new SeraphTimeline(d, 1);
            check(d + " fades out by its end", tl.master(tl.total() - 1) < 0.02f);
            check(d + " lands on its own glass", tl.glass(SeraphTimeline.REVEAL) == d.lands);
            check(d + ": only a bust lands on black", (d.lands == Glass.BLACK) == !d.win());
            check(d + ": its name comes after the reveal", !d.win() || tl.arrive() > SeraphTimeline.REVEAL);
        }
        // The endings' own order.
        SeraphTimeline jp = new SeraphTimeline(Drop.ARTIFACT_UPGRADE, 1);
        check("the afterimages blink in before they implode", SeraphTimeline.afterAt(SeraphTimeline.AFTERIMAGES - 1) < SeraphTimeline.IMPLODE);
        check("the jackpot's shock comes before the drop shows", SeraphTimeline.SHOCK < jp.arrive());
        SeraphTimeline ey = new SeraphTimeline(Drop.JUDGEMENT_CORE, 1);
        SeraphTimeline dr = new SeraphTimeline(Drop.BYZANTIUM_DYE, 1);
        check("the eye: freeze, crack, burst, the core forms, opens, left, right, stares, pulses, pulls, rises",
                SeraphTimeline.REVEAL < SeraphTimeline.E_FREEZE && SeraphTimeline.E_FREEZE < SeraphTimeline.E_CRACK
                        && SeraphTimeline.E_CRACK < SeraphTimeline.E_BURST && SeraphTimeline.E_BURST < SeraphTimeline.E_FORMED
                        && SeraphTimeline.E_FORMED <= SeraphTimeline.E_OPEN && SeraphTimeline.E_OPEN < SeraphTimeline.E_OPENED
                        && SeraphTimeline.E_OPENED <= SeraphTimeline.E_LEFT && SeraphTimeline.E_LEFT < SeraphTimeline.E_RIGHT
                        && SeraphTimeline.E_RIGHT < SeraphTimeline.E_STARE && SeraphTimeline.E_STARE < SeraphTimeline.E_PULSE
                        && SeraphTimeline.E_PULSE < SeraphTimeline.E_PULL && SeraphTimeline.E_PULL < SeraphTimeline.E_PULLED
                        && SeraphTimeline.E_PULLED < SeraphTimeline.E_RISE && SeraphTimeline.E_RISE < ey.arrive());
        check("the eye looks left, then right, then at the camera",
                ey.eyeLook((SeraphTimeline.E_LEFT + SeraphTimeline.E_RIGHT) / 2) < -0.5f
                        && ey.eyeLook((SeraphTimeline.E_RIGHT + SeraphTimeline.E_STARE) / 2) > 0.5f
                        && Math.abs(ey.eyeLook(SeraphTimeline.E_PULSE)) < 0.05f);
        check("the eye is shut before it opens and open at the stare",
                ey.eyeOpen(SeraphTimeline.E_OPEN - 1) < 0.01f && ey.eyeOpen(SeraphTimeline.E_STARE) > 0.99f);
        check("the seraph and crystals are fully pulled in before the core rises",
                ey.eyePull(SeraphTimeline.E_PULL) < 0.01f && ey.eyePull(SeraphTimeline.E_PULLED) > 0.99f);
        check("the dragon: deepen, freeze, the flight, landing, breath, pool, burst, the dye",
                SeraphTimeline.REVEAL < SeraphTimeline.D_DEEPEN && SeraphTimeline.D_DEEPEN < SeraphTimeline.D_FREEZE
                        && SeraphTimeline.D_FREEZE < SeraphTimeline.D_IN && SeraphTimeline.D_IN < SeraphTimeline.D_LAND
                        && SeraphTimeline.D_LAND < SeraphTimeline.D_BREATH && SeraphTimeline.D_BREATH < SeraphTimeline.D_POOL
                        && SeraphTimeline.D_BREATH_END < SeraphTimeline.D_BURST && SeraphTimeline.D_BURST < SeraphTimeline.D_RISE
                        && SeraphTimeline.D_RISE < dr.arrive());
        check("the dragon's flight is the whole baked track", SeraphTimeline.D_LAND - SeraphTimeline.D_IN == com.sbcs.anim.SeraphDragon.flightMs());
        float[] land = com.sbcs.anim.SeraphDragon.pos(com.sbcs.anim.SeraphDragon.flightMs());
        check("the dragon lands behind the beacon, at (6.09, 0, -2.48)",
                Math.abs(land[0] - 6.09f) < 0.05f && Math.abs(land[1]) < 0.05f && Math.abs(land[2] + 2.48f) < 0.05f);
        float[] start = com.sbcs.anim.SeraphDragon.pos(0);
        check("the dragon comes from far behind the pillars", start[2] < -60f && start[1] > 10f);
        check("the dragon turns three-quarters to the camera once down", Math.abs(((com.sbcs.anim.SeraphDragon.yaw(com.sbcs.anim.SeraphDragon.flightMs()) % 360f) + 360f) % 360f - com.sbcs.anim.SeraphDragon.LAND_YAW) < 1f);
        check("the paint is down before the burst and spread by the dye's rise",
                dr.paint(SeraphTimeline.D_POOL - 1) < 0.01f && dr.paint(SeraphTimeline.D_RISE) > 0.99f);
        check("everything greys when time stops", dr.grey(SeraphTimeline.D_FREEZE - 1) < 0.01f && dr.grey(SeraphTimeline.D_IN) > 0.99f);
        check("the dye holds longest of any ending", dr.total() - dr.arrive() >= ey.total() - ey.arrive()
                && dr.total() - dr.arrive() >= jp.total() - jp.arrive());
        check("the core and the dye have their own endings", Drop.JUDGEMENT_CORE.ending == Ending.EYE && Drop.BYZANTIUM_DYE.ending == Ending.DRAGON);
        check("the upgrade gets the jackpot", Drop.ARTIFACT_UPGRADE.ending == Ending.JACKPOT);
        // The item endings (2026-10-26): each drop's animation is about the item.
        check("every item has its own ending", Drop.SUMMONING_EYE.ending == Ending.ALTAR && Drop.ETHERWARP_MERGER.ending == Ending.WARP
                && Drop.ENCHANT_RUNE.ending == Ending.RUNE && Drop.PARAGON.ending == Ending.SHARD && Drop.END_STONE_IDOL.ending == Ending.IDOL);
        boolean ownEnding = true;
        for (Drop d : Drop.values()) if (d.win() && d.ending == Ending.PLAIN) ownEnding = false;
        check("no win is left on the plain ending", ownEnding);
        SeraphTimeline al = new SeraphTimeline(Drop.SUMMONING_EYE, 1);
        check("the Summoning: frames, seven eyes, the drop's eye, the portal, the lift, in order",
                SeraphTimeline.A_FRAMES < SeraphTimeline.A_EYES && SeraphTimeline.A_EYES + 6 * SeraphTimeline.A_EYE_STEP < SeraphTimeline.A_SEAT
                        && SeraphTimeline.A_SEAT < SeraphTimeline.A_SEATED && SeraphTimeline.A_SEATED <= SeraphTimeline.A_OPEN
                        && SeraphTimeline.A_OPEN < SeraphTimeline.A_LIFT && SeraphTimeline.A_LIFT < al.arrive());
        check("the eighth frame waits empty until the drop's eye sets in",
                !al.altarEye(SeraphTimeline.A_SEAT, SeraphTimeline.A_FRAMES_N - 1) && al.altarEye(SeraphTimeline.A_SEATED, SeraphTimeline.A_FRAMES_N - 1)
                        && al.altarEye(SeraphTimeline.A_SEAT, SeraphTimeline.A_FRAMES_N - 2));
        check("the frames are down again by the showcase", al.altarFrame(al.arrive() + 900, 0) < 0.01f && al.altarFrame(SeraphTimeline.A_OPEN, 7) > 0.99f);
        SeraphTimeline wp = new SeraphTimeline(Drop.ETHERWARP_MERGER, 1);
        boolean warps = true;
        for (int k = 1; k < SeraphTimeline.W_JUMPS; k++) if (SeraphTimeline.warpAt(k) <= SeraphTimeline.warpAt(k - 1)) warps = false;
        check("Etherwarp: every warp lands before the showcase, in turn", warps && SeraphTimeline.warpAt(SeraphTimeline.W_JUMPS - 1) < wp.arrive());
        check("Etherwarp: each target is lit before it lands", wp.warpAim(SeraphTimeline.warpAt(0) - 50, 0) > 0.9f && wp.warpAim(SeraphTimeline.W_FROM - 1, 0) == 0f);
        SeraphTimeline ru = new SeraphTimeline(Drop.ENCHANT_RUNE, 1);
        check("the Runecraft: table up, glyphs stream, then the rune is written",
                ru.runeTable(SeraphTimeline.R_STREAM) > 0.99f && ru.runeStream(SeraphTimeline.R_STREAM_END) > 0.99f
                        && ru.runeForm(SeraphTimeline.R_FORM - 1) == 0f && SeraphTimeline.R_FORM < ru.arrive());
        SeraphTimeline sh = new SeraphTimeline(Drop.PARAGON, 1);
        check("Paragon: the crystals stand until they burst, and the shards fuse before the showcase",
                !sh.crystalsGone(SeraphTimeline.S_BURST - 1) && sh.crystalsGone(SeraphTimeline.S_BURST)
                        && sh.shardPull(SeraphTimeline.S_FUSE) >= 1f && SeraphTimeline.S_FUSE < sh.arrive() && sh.crystalsBlow(sh.arrive()) < 0f);
        SeraphTimeline id = new SeraphTimeline(Drop.END_STONE_IDOL, 1);
        check("Set in Stone: the Seraph never falls, is stone to the top before it cracks, and is gone at the shatter",
                id.death(id.arrive()) < 0f && id.stone(SeraphTimeline.I_CRACK) >= 1f && id.seraphShown(SeraphTimeline.I_SHATTER - 1)
                        && !id.seraphShown(SeraphTimeline.I_SHATTER) && SeraphTimeline.I_SHATTER < id.arrive());
        check("the idol gets the banner and the long hold", id.banner() && id.total() - id.arrive() >= 5000);
        SeraphTimeline sk = new SeraphTimeline(Drop.VOID_SKIN, 1);
        check("the skin's head comes off after it starts to fall and lands before the spread",
                SeraphTimeline.HEAD_OFF > SeraphTimeline.DEATH && SeraphTimeline.HEAD_LANDS < SeraphTimeline.SKIN_DONE && sk.arrive() > SeraphTimeline.SKIN_DONE);
        for (SeraphTimeline st : new SeraphTimeline[]{ey, dr}) {
            boolean phys = true;
            for (long t = 0; t < st.total(); t += 7) if (st.phys(t + 7) < st.phys(t)) phys = false;
            check(st.drop() + ": the stopped clock never runs backwards", phys);
        }
        for (Drop d : Drop.values()) {
            List<SeraphSound.Cue> cues = SeraphSound.schedule(new SeraphTimeline(d, 5));
            boolean cutAtReveal = false, inOrder = true;
            for (int i = 0; i < cues.size(); i++) {
                if (cues.get(i).at == SeraphTimeline.REVEAL && cues.get(i).cut) cutAtReveal = true;
                if (i > 0 && cues.get(i).at < cues.get(i - 1).at) inOrder = false;
            }
            check(d + ": the reveal cuts the build-up dead", cutAtReveal);
            check(d + ": its cues are in order", inOrder);
        }
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
