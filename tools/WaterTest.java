import com.sbcs.anim.ChopTimeline;
import com.sbcs.anim.WaterTimeline;
import com.sbcs.anim.WaterTimeline.Creature;
import com.sbcs.anim.WaterTimeline.Drop;
import com.sbcs.anim.WaterTimeline.Ending;
import com.sbcs.audio.WaterSound;
import com.sbcs.render.ChopScene;
import com.sbcs.render.WaterScene;

/**
 * Offline checks for "The Coil" (the Titanoboa) and "The Totem" (the Wiki Tiki). The one that matters: nothing drawn
 * or heard before the reveal depends on the drop - every draw call of every frame (the rasterised 3D layer and the
 * water floor pixel by pixel) is hashed, for every drop each creature can give, on several seeds, and they must agree.
 * The textures are stand-ins: only sameness matters here.
 *
 *   java -cp "out:src/main/resources" WaterTest
 */
public final class WaterTest {
    static int passed, failed;

    static void check(String what, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.out.println("FAIL " + what);
        }
    }

    /** Hashes every primitive drawn, the images pixel by pixel. */
    static class Hash implements ChopScene.Canvas {
        long h = 1125899906842597L;
        int calls, squares;

        void mix(float... v) {
            calls++;
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
        public void imageAffine(String key, int[] argb, int iw, int ih, float ox, float oy, float ux, float uy, float vx, float vy) {
            mix(key); mix(iw, ih, ox, oy, ux, uy, vx, vy);
            for (int i = 0; i < iw * ih; i++) h = 31 * h + argb[i];
        }
        public void image(String key, int[] argb, int iw, int ih, float x, float y, float w, float hh) {
            mix(key); mix(iw, ih, x, y, w, hh);
            for (int i = 0; i < iw * ih; i++) h = 31 * h + argb[i];
        }
    }

    static int[] pattern(int w, int h, int seed) {
        int[] px = new int[w * h];
        java.util.Random r = new java.util.Random(seed);
        for (int i = 0; i < px.length; i++) px[i] = 0xFF000000 | r.nextInt(0xFFFFFF);
        return px;
    }

    static long frames(Creature c, Drop d, long seed, long from, long to, int step) {
        WaterTimeline tl = new WaterTimeline(c, d, seed);
        tl.price(1_000_000, "test");
        Hash hash = new Hash();
        for (long t = from; t < to; t += step) WaterScene.draw(hash, hash::fill, hash::fill, tl, t, 640, 360);
        return hash.h;
    }

    public static void main(String[] a) {
        for (int slot = 0; slot < 10; slot++) WaterScene.RASTER.texture(slot, pattern(64, 64, 100 + slot), 64, 64);
        WaterScene.rasterScale = 1f;
        WaterScene.waterPixels = pattern(16, 16 * 32, 77);
        WaterScene.waterFrames = 32;

        // ---- honesty: the drop changes nothing before the reveal, per creature
        long[] seeds = {1, 7, 42, 99_991};
        for (Creature c : Creature.values()) {
            for (long seed : seeds) {
                long ref = frames(c, Drop.BUST, seed, 0, WaterTimeline.REVEAL, 48);
                for (Drop d : Drop.values()) {
                    if (d == Drop.BUST || !d.from(c)) continue;
                    check("before the reveal, " + c + " / " + d + " draws the same as a bust (seed " + seed + ")",
                            frames(c, d, seed, 0, WaterTimeline.REVEAL, 48) == ref);
                }
            }
        }
        for (Creature c : Creature.values()) {
            check(c + ": a win draws differently from a bust after the reveal",
                    frames(c, Drop.AQUAMARINE, 7, WaterTimeline.REVEAL, WaterTimeline.REVEAL + 900, 48)
                            != frames(c, Drop.BUST, 7, WaterTimeline.REVEAL, WaterTimeline.REVEAL + 900, 48));
        }

        // ---- the tell: fixed steps, every colour visited, it lands on the drop's level
        long prev = -1;
        boolean ordered = true;
        for (long s : WaterTimeline.STEPS) {
            ordered &= s > prev && s < WaterTimeline.HANG;
            prev = s;
        }
        check("the colour steps are in order and before the hang", ordered);
        check("the build-up is 4.5-5 s (" + WaterTimeline.REVEAL + ")", WaterTimeline.REVEAL >= 4500 && WaterTimeline.REVEAL <= 5000);
        boolean[] seen = new boolean[5];
        for (int i = 0; i < WaterTimeline.STEPS.length; i++) seen[WaterTimeline.stepLevel(i)] = true;
        boolean every = true;
        for (boolean b : seen) every &= b;
        check("the tell visits every colour, bust to dye", every);
        for (Creature c : Creature.values()) {
            for (Drop d : Drop.values()) {
                if (!d.from(c)) continue;
                WaterTimeline tl = new WaterTimeline(c, d, 3);
                check(c + " / " + d + " lands on its level", Math.abs(tl.level(WaterTimeline.REVEAL + 300) - d.level) < 0.01f);
                check(c + " / " + d + " fades out by its end", tl.master(tl.total() - 1) < 0.02f);
                if (d.win()) {
                    check(c + " / " + d + " is shown after the reveal", tl.arrive() > WaterTimeline.REVEAL);
                    check(c + " / " + d + " rises before it is shown", tl.riseFrom() < tl.arrive());
                    check(c + " / " + d + " leaves the showcase on screen at least 3 s", tl.total() - tl.arrive() >= 3000);
                }
            }
        }
        for (Drop d : Drop.values()) check(d + ": only a bust lands on level 0", (d.level == 0) == !d.win());
        for (int i = 1; i < Drop.values().length; i++) {
            check(Drop.values()[i] + " lands no lower than the drop before it", Drop.values()[i].level >= Drop.values()[i - 1].level);
        }
        WaterTimeline bust = new WaterTimeline(Creature.TITANOBOA, Drop.BUST, 7);
        check("a bust is over within 2.5 s of the reveal (" + (bust.total() - WaterTimeline.REVEAL) + ")", bust.total() - WaterTimeline.REVEAL <= 2500);
        check("each creature's head drops are its own", Drop.SHED.from(Creature.TITANOBOA) && !Drop.SHED.from(Creature.WIKI_TIKI)
                && Drop.SNAKE_EYES.from(Creature.TITANOBOA) && !Drop.SNAKE_EYES.from(Creature.WIKI_TIKI)
                && Drop.BUBBLE.from(Creature.WIKI_TIKI) && !Drop.BUBBLE.from(Creature.TITANOBOA)
                && Drop.TIKI_MASK.from(Creature.WIKI_TIKI) && !Drop.TIKI_MASK.from(Creature.TITANOBOA));
        check("the Aquamarine Dye and the Scriptures come from both", Drop.AQUAMARINE.from == null && Drop.SCRIPTURES.from == null);

        // ---- the dye: time stops and resumes without a jump; the grandest
        WaterTimeline dye = new WaterTimeline(Creature.TITANOBOA, Drop.AQUAMARINE, 1);
        check("the dye's clock stops", dye.phys(WaterTimeline.F_FREEZE + 10) == dye.phys(WaterTimeline.F_RESUME - 10));
        check("the dye's clock runs before the freeze", dye.phys(WaterTimeline.REVEAL) == WaterTimeline.REVEAL);
        check("the dye's clock resumes without a jump", Math.abs(dye.phys(WaterTimeline.F_RESUME + 1) - dye.phys(WaterTimeline.F_RESUME - 1)) <= 2);
        check("the world is grey by the first heartbeat", dye.grey(WaterTimeline.F_HEARTS[0]) > 0.99f);
        check("the heartbeats come before the water climbs", WaterTimeline.F_HEARTS[2] > WaterTimeline.F_HEARTS[1]
                && WaterTimeline.F_HEARTS[1] > WaterTimeline.F_HEARTS[0] && WaterTimeline.F_HEARTS[0] < WaterTimeline.F_RISE);
        check("the water has climbed before time comes back", dye.flood(WaterTimeline.F_RESUME) > 0.99f);
        check("it rears before the dye is shown", WaterTimeline.F_REAR < dye.arrive());
        for (Drop d : Drop.values()) {
            if (d == Drop.AQUAMARINE || !d.win()) continue;
            Creature c = d.from != null ? d.from : Creature.WIKI_TIKI;
            check("the dye is the grandest: longer than " + d, dye.total() > new WaterTimeline(c, d, 1).total());
        }

        // ---- the other endings' beats come in order
        check("the Shed: the snake slides out, then the skin crumples", WaterTimeline.S_SLIDE < WaterTimeline.S_CRUMPLE && WaterTimeline.S_CRUMPLE < WaterTimeline.S_CRUMPLE_TO);
        check("Snake Eyes: dark, the eyes open, blink, fuse", WaterTimeline.E_DARK < WaterTimeline.E_OPEN && WaterTimeline.E_OPEN < WaterTimeline.E_BLINK
                && WaterTimeline.E_BLINK < WaterTimeline.E_FUSE);
        check("the Bubble: the beams meet, it grows, it floats free", WaterTimeline.U_MEET < WaterTimeline.U_GROW && WaterTimeline.U_GROW_TO <= WaterTimeline.U_FREE);
        check("the Mask: peels, turns, its eyes light", WaterTimeline.M_PEEL < WaterTimeline.M_TURN && WaterTimeline.M_TURN_TO < WaterTimeline.M_EYES);

        // ---- the sound: before the reveal, the very same samples for every drop, per creature
        for (Creature c : Creature.values()) {
            float[] base = null;
            for (Drop d : Drop.values()) {
                if (!d.from(c)) continue;
                java.util.List<WaterSound.Cue> before = new java.util.ArrayList<>();
                for (WaterSound.Cue cue : WaterSound.schedule(new WaterTimeline(c, d, 11))) {
                    if (cue.at < WaterTimeline.REVEAL) before.add(cue);
                }
                float[] pcm = WaterSoundtrack.render(before, WaterTimeline.REVEAL / 1000f);
                if (base == null) base = pcm;
                else check("before the reveal, " + c + " / " + d + " sounds the same as a bust", java.util.Arrays.equals(base, pcm));
            }
        }
        for (Creature c : Creature.values()) {
            for (Drop d : Drop.values()) {
                if (!d.from(c)) continue;
                java.util.List<WaterSound.Cue> cues = WaterSound.schedule(new WaterTimeline(c, d, 5));
                boolean cut = false, inOrder = true;
                long cutBy = d.ending == Ending.FLOOD ? WaterTimeline.F_FREEZE : WaterTimeline.REVEAL;
                for (int i = 0; i < cues.size(); i++) {
                    if (cues.get(i).cut && cues.get(i).at >= WaterTimeline.REVEAL && cues.get(i).at <= cutBy) cut = true;
                    if (i > 0 && cues.get(i).at < cues.get(i - 1).at) inOrder = false;
                }
                check(c + " / " + d + ": the build-up is cut dead at the reveal", cut);
                check(c + " / " + d + ": its cues are in order", inOrder);
                float[] pcm = WaterSoundtrack.render(cues, new WaterTimeline(c, d, 5).total() / 1000f + 1f);
                float peak = 0f;
                for (float v : pcm) peak = Math.max(peak, Math.abs(v));
                check(c + " / " + d + ": the mix does not clip (" + peak + ")", peak <= 1f);
            }
        }

        // ---- cost: GUI draw calls a frame (the 3D layer and the floor are one image each)
        int worst = 0;
        long sum = 0, n = 0;
        String worstAt = "";
        for (Creature c : Creature.values()) {
            for (Drop d : Drop.values()) {
                if (!d.from(c)) continue;
                WaterTimeline tl = new WaterTimeline(c, d, 9);
                tl.price(1_000_000, "test");
                for (long t = 0; t < tl.total(); t += 100) {
                    Hash hash = new Hash();
                    com.sbcs.render.CrystalVeins.Fill layer = (x, y, w, h2, argb) -> hash.squares++;
                    WaterScene.draw(hash, layer, layer, tl, t, 640, 360);
                    sum += hash.calls;
                    n++;
                    if (hash.calls > worst) {
                        worst = hash.calls;
                        worstAt = c + " / " + d + " at " + t;
                    }
                }
            }
        }
        System.out.println("GUI draw calls a frame (pixel squares go to one image): mean " + sum / Math.max(1, n) + ", worst " + worst + " (" + worstAt + ")");
        check("no frame costs more than the Seraph's worst (5,100): " + worst, worst <= 5_100);

        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
