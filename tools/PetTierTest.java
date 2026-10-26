import com.sbcs.anim.ChopTimeline;
import com.sbcs.anim.PetTierTimeline;
import com.sbcs.anim.PetTierTimeline.Tier;
import com.sbcs.audio.HellionSound;
import com.sbcs.audio.PetTierSound;
import com.sbcs.audio.SynthEngine;
import com.sbcs.audio.Voice;
import com.sbcs.render.ChopScene;
import com.sbcs.render.PetTierScene;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static com.sbcs.anim.PetTierTimeline.*;

/**
 * Offline checks for the Scatha pet's reveal per rarity. The one that matters: nothing drawn or heard before the
 * cycle lands ({@link PetTierTimeline#LAND}) depends on the rarity - every draw call of every frame is hashed and
 * every cue before it rendered, for all three tiers on five seeds, and they must agree. Then: each tier's beats
 * in order, Rare short and Legendary the longest, the cycle, and the draw-call budget.
 *
 *   javac -d out -sourcepath "src/main/java;tools" -cp slf4j-api.jar tools/PetTierTest.java
 *   java -cp "out;slf4j-api.jar" PetTierTest
 */
public final class PetTierTest {
    static int passed, failed;

    static void check(String what, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.out.println("FAIL " + what);
        }
    }

    /** Hashes every primitive drawn; counts GUI calls and pixel-layer fills apart. */
    static final class Hash implements ChopScene.Canvas {
        long h = 1125899906842597L;
        int calls, fills;

        void mix(float... v) {
            for (float f : v) h = 31 * h + Float.floatToIntBits(Math.round(f * 100f) / 100f);
        }

        void mix(String s) { h = 31 * h + (s == null ? 0 : s.hashCode()); }

        public void quad(String tex, float ox, float oy, float ux, float uy, float vx, float vy, int u0, int v0, int uw, int vh, int argb) {
            calls++; mix(tex); mix(ox, oy, ux, uy, vx, vy, u0, v0, uw, vh, argb);
        }
        public void sprite(String tex, float cx, float cy, float w, float h2, float rot, int argb) { calls++; mix(tex); mix(cx, cy, w, h2, rot, argb); }
        public void line(float x0, float y0, float x1, float y1, float width, int argb) { calls++; mix(x0, y0, x1, y1, width, argb); }
        public void fill(float x, float y, float w, float h2, int argb) { calls++; mix(x, y, w, h2, argb); }
        void pixel(float x, float y, float w, float h2, int argb) { fills++; mix(x, y, w, h2, argb); mix("px"); }
        public void item(float cx, float cy, float size, float alpha, float yaw) { calls++; mix(cx, cy, size, alpha, yaw); mix("item"); }
        public void name(float cx, float cy, float scale, int argb, int letters) { calls++; mix(cx, cy, scale, argb, letters); mix("name"); }
        public void text(String text, float cx, float cy, float scale, int argb) { calls++; mix(text); mix(cx, cy, scale, argb); }
        public String creatureSkin(ChopTimeline.Creature creature) { return null; }
        public String dropSkin() { return "head"; }
        public String bestiarySkin(String key) { return PetTierScene.BODY_SKIN.equals(key) ? "body" : null; }
    }

    /** A timeline as the screen makes one - with a different note and price per tier, which must not leak early. */
    static PetTierTimeline tl(Tier tier, long seed) {
        PetTierTimeline tl = new PetTierTimeline(tier, seed);
        tl.price(1_000_000L * (tier.ordinal() + 1), "lowest BIN, " + tier.title);
        tl.note("Your " + (tier.ordinal() + 2) + "th " + tier.title + " Scatha");
        return tl;
    }

    static long frames(Tier tier, long seed, long from, long to, int w, int h) {
        PetTierTimeline tl = tl(tier, seed);
        Hash hash = new Hash();
        for (long t = from; t < to; t += 8) PetTierScene.draw(hash, hash::pixel, null, tl, t, w, h, w * 0.7f, h * 0.62f, h * 0.2f);
        return hash.h;
    }

    static long frame(Tier tier, long seed, long t) {
        return frames(tier, seed, t, t + 1, 640, 360);
    }

    public static void main(String[] a) throws Exception {
        long[] seeds = {1, 7, 42, 99_991, 123_456_789};
        int[][] screens = {{640, 360}, {480, 270}, {854, 480}};

        // ---- honesty: every frame before the landing, every tier alike
        for (long seed : seeds) {
            for (int[] s : screens) {
                long ref = frames(Tier.RARE, seed, 0, LAND, s[0], s[1]);
                for (Tier tier : Tier.values()) {
                    check(tier + " seed " + seed + " " + s[0] + "x" + s[1] + ": every frame before the landing as a Rare's",
                            frames(tier, seed, 0, LAND, s[0], s[1]) == ref);
                }
            }
            // ...and the very last moment before it.
            check("seed " + seed + ": the frame 1 ms before the landing is the same",
                    frame(Tier.RARE, seed, LAND - 1) == frame(Tier.LEGENDARY, seed, LAND - 1)
                            && frame(Tier.EPIC, seed, LAND - 1) == frame(Tier.LEGENDARY, seed, LAND - 1));
            // Something does change after it - the landing is visible.
            for (long after : new long[]{40, 200, 600}) {
                long r = frame(Tier.RARE, seed, LAND + after), e = frame(Tier.EPIC, seed, LAND + after),
                        l = frame(Tier.LEGENDARY, seed, LAND + after);
                check("seed " + seed + ": the tiers differ " + after + " ms after the landing", r != e && e != l && r != l);
            }
        }

        // ---- honesty: the sounds before the landing
        for (long seed : seeds) {
            float[] ref = soundBefore(tl(Tier.RARE, seed));
            for (Tier tier : Tier.values()) {
                List<HellionSound.Cue> cues = PetTierSound.schedule(tl(tier, seed));
                check(tier + " seed " + seed + ": every sound before the landing as a Rare's", Arrays.equals(soundBefore(tl(tier, seed)), ref));
                boolean cutAtLand = false;
                for (HellionSound.Cue c : cues) if (c.at == LAND && c.cut) cutAtLand = true;
                check(tier + ": the landing cuts the build-up", cutAtLand);
                boolean sorted = true;
                for (int i = 1; i < cues.size(); i++) if (cues.get(i).at < cues.get(i - 1).at) sorted = false;
                check(tier + ": cues in order", sorted);
                check(tier + ": every cue inside the reveal", cues.get(cues.size() - 1).at < tl(tier, seed).total());
            }
        }

        // ---- the cycle
        check("seven steps", STEPS.length == 7);
        boolean shorter = true;
        for (int i = 2; i < STEPS.length; i++) if (STEPS[i] - STEPS[i - 1] >= STEPS[i - 1] - STEPS[i - 2]) shorter = false;
        check("each step shorter than the one before", shorter);
        check("the cycle starts after the head has arrived", STEPS[0] > RISE);
        check("the cycle ends on blue", cycleColour(LAND - 1) == Tier.RARE.rgb);
        check("the cycle passes violet and gold", seen(Tier.EPIC.rgb) && seen(Tier.LEGENDARY.rgb));
        for (Tier tier : Tier.values()) {
            PetTierTimeline tl = tl(tier, 1);
            check(tier + ": the colour before the landing is the cycle's", tl.colour(LAND - 1) == cycleColour(LAND - 1));
            check(tier + ": lands on its own colour", tl.colour(LAND + 200) == tier.rgb);
        }

        // ---- the tiers
        check("Tier.of legendary", Tier.of("LEGENDARY") == Tier.LEGENDARY && Tier.of("legendary") == Tier.LEGENDARY);
        check("Tier.of epic", Tier.of("EPIC") == Tier.EPIC);
        check("Tier.of rare / unknown / null", Tier.of("RARE") == Tier.RARE && Tier.of("unknown") == Tier.RARE && Tier.of(null) == Tier.RARE);
        long r = tl(Tier.RARE, 1).total(), e = tl(Tier.EPIC, 1).total(), l = tl(Tier.LEGENDARY, 1).total();
        check("Rare shorter than Epic shorter than Legendary (" + r + ", " + e + ", " + l + ")", r < e && e < l);
        check("a Rare is over within 5 s of the head coming free (" + r + ")", r <= 5_000);
        check("a Legendary at least twice a Rare's length", l >= 2 * r);
        for (Tier tier : Tier.values()) {
            PetTierTimeline tl = tl(tier, 1);
            check(tier + ": showcase at or after the landing", tl.showcase() >= LAND);
            check(tier + ": card, then the count, then the end", tl.cardFrom() > tl.showcase()
                    && tl.countStart() > tl.cardFrom() && tl.countStart() + COUNT_MS + 400 < tl.total());
            check(tier + ": gone by the end", tl.shrink(tl.total()) == 1f && tl.shrink(tl.total() - SHRINK_MS - 1) == 0f);
        }
        // The Legendary's beats in order: cracks, every pour home, the coil closed, gold, the second beat, the burst.
        long lastPour = L_CRACK + 5 * L_CRACK_STEP + L_POUR_AFTER + L_POUR_MS;
        check("Legendary: every pour reaches the head before the ring closes", lastPour < L_RING);
        check("Legendary: the coil before the gold", L_COIL < L_RING && L_RING < L_GOLD);
        check("Legendary: the gold done before the second beat", L_GOLD + 4 * L_GOLD_STEP + 220 <= L_BEAT2);
        check("Legendary: the beat before the burst", L_BEAT2 < L_BURST);
        check("six gem colours", GEMS.length == 6);

        // ---- the budget: GUI calls per frame (pixel squares go into one image)
        for (Tier tier : Tier.values()) {
            PetTierTimeline tl = tl(tier, 42);
            int worst = 0, worstFills = 0;
            for (long t = 0; t < tl.total(); t += 16) {
                Hash hash = new Hash();
                PetTierScene.draw(hash, hash::pixel, null, tl, t, 640, 360, 448, 223, 72);
                worst = Math.max(worst, hash.calls);
                worstFills = Math.max(worstFills, hash.fills);
            }
            System.out.println(tier + ": " + tl.total() + " ms, worst " + worst + " GUI calls, " + worstFills + " pixel squares a frame");
            check(tier + ": under 600 GUI calls a frame (" + worst + ")", worst < 600);
            check(tier + ": under 6000 pixel squares a frame (" + worstFills + ")", worstFills < 6000);
        }

        // ---- the screen: its reveal chime no longer names the rarity
        String screen = Files.readString(Path.of("src/main/java/com/sbcs/gui/ScopeMinigameScreen.java"));
        check("the screen never passes the rarity to the reveal chime", !screen.contains("onReveal(reachesHead"));
        check("the screen plays the pet's cue list", screen.contains("PetTierSound.schedule"));

        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    static boolean seen(int rgb) {
        for (long t = STEPS[0]; t < LAND; t += 5) if (cycleColour(t) == rgb) return true;
        return false;
    }

    /** Every cue due before the landing, mixed to the landing. */
    static float[] soundBefore(PetTierTimeline tl) {
        int rate = (int) SynthEngine.SAMPLE_RATE;
        float seconds = LAND / 1000f;
        float[] mix = new float[(int) (seconds * rate) * 2];
        for (HellionSound.Cue c : PetTierSound.schedule(tl)) {
            if (c.at >= LAND) continue;
            int at = (int) (c.at * rate / 1000L) * 2;
            for (Voice v : c.voices) {
                float[] one = SynthEngine.renderOffline(v, seconds - c.at / 1000f);
                for (int i = 0; i < one.length && at + i < mix.length; i++) mix[at + i] += one[i];
            }
        }
        return mix;
    }
}
