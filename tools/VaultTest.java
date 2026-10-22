import com.sbcs.anim.VaultTimeline;
import com.sbcs.anim.VaultTimeline.Ending;
import com.sbcs.anim.VaultTimeline.Prize;
import com.sbcs.audio.SynthEngine;
import com.sbcs.audio.VaultSound;
import com.sbcs.audio.Voice;
import com.sbcs.render.VaultScene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Offline checks for "The Vault" (the Crystal Nucleus). The one that matters: nothing drawn or heard before the reveal
 * depends on the prize - every draw call of every frame is hashed, and every cue rendered, for every prize on several
 * seeds, and they must agree.
 *
 *   javac -d out -cp <out of the mod>:<slf4j> tools/VaultTest.java tools/HellionTest.java && java -cp ... VaultTest
 */
public final class VaultTest {
    static int passed, failed;

    static void check(String what, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.out.println("FAIL " + what);
        }
    }

    static long frames(Prize p, long seed, long from, long to) {
        VaultTimeline tl = new VaultTimeline(p, seed);
        HellionTest.Hash hash = new HellionTest.Hash();
        for (long t = from; t < to; t += 16) VaultScene.draw(hash, hash::fill, tl, t, 640, 360);
        return hash.h;
    }

    static float[] render(List<VaultSound.Cue> cues, float seconds) {
        int rate = (int) SynthEngine.SAMPLE_RATE;
        float[] mix = new float[(int) (seconds * rate) * 2];
        for (VaultSound.Cue c : cues) {
            int at = (int) (c.at * rate / 1000L) * 2;
            if (at >= mix.length) continue;
            if (c.cut) Arrays.fill(mix, at, mix.length, 0f);
            for (Voice v : c.voices) {
                float[] one = SynthEngine.renderOffline(v, seconds - c.at / 1000f);
                for (int i = 0; i < one.length && at + i < mix.length; i++) mix[at + i] += one[i];
            }
        }
        return mix;
    }

    public static void main(String[] a) {
        List<Prize> prizes = new ArrayList<>();
        for (String w : Prize.testWords()) prizes.add(Prize.test(w));
        long R = VaultTimeline.REVEAL;

        for (long seed : new long[]{1, 7, 42, 99_991, 123_456_789}) {
            long ref = frames(Prize.BUST, seed, 0, R);
            for (Prize p : prizes) {
                check("before the reveal, " + p.title() + " draws the same as a bust (seed " + seed + ")", frames(p, seed, 0, R) == ref);
            }
        }
        for (Prize p : prizes) {
            if (p.win()) check(p.title() + " draws differently from a bust after the reveal",
                    frames(p, 7, R, R + 600) != frames(Prize.BUST, 7, R, R + 600));
        }
        for (long seed : new long[]{3, 77}) {
            float[] base = null;
            for (Prize p : prizes) {
                List<VaultSound.Cue> before = new ArrayList<>();
                for (VaultSound.Cue c : VaultSound.schedule(new VaultTimeline(p, seed))) if (c.at < R) before.add(c);
                float[] pcm = render(before, R / 1000f);
                if (base == null) base = pcm;
                else check("before the reveal, " + p.title() + " sounds the same as a bust (seed " + seed + ")", Arrays.equals(base, pcm));
            }
        }

        // The prizes: what each word is, and each tier has its own ending and its own colour on the dial.
        check("bust is a bust", !Prize.test("bust").win());
        check("the Quick Claw swings the door open", Prize.test("claw").ending() == Ending.OPEN);
        check("a second Fine Jasper swings it open", Prize.test("jasper").ending() == Ending.OPEN);
        check("a Flawless Ruby blows it off", Prize.test("ruby").ending() == Ending.BLOWN);
        check("Divan's Alloy has its own ending, the Meltdown", Prize.test("alloy").ending() == Ending.ALLOY);
        check("the Jade Dye has its own", Prize.test("dye").ending() == Ending.DYE);
        java.util.Set<Integer> tells = new java.util.HashSet<>();
        for (Ending e : Ending.values()) tells.add(e.tell);
        check("every ending lands on a colour of its own", tells.size() == Ending.values().length);
        for (Prize p : prizes) {
            VaultTimeline tl = new VaultTimeline(p, 7);
            check(p.title() + " lands on its ending's colour", tl.dialRgb(R) == p.ending().tell);
            check(p.title() + " fades out by its end", tl.master(tl.total() - 1) < 0.02f);
            if (p.win()) {
                check(p.title() + " shows the drop after the reveal", tl.arrive() > R && tl.itemRise(R) == 0f);
                check(p.title() + " has odds", p.oneIn() > 1);
            }
        }
        // The best two get the most: the Alloy and the Dye run longest, and carry the banner.
        long alloy = new VaultTimeline(Prize.test("alloy"), 1).total(), dye = new VaultTimeline(Prize.test("dye"), 1).total();
        long jackpot = new VaultTimeline(Prize.test("ruby"), 1).total(), rare = new VaultTimeline(Prize.test("claw"), 1).total();
        check("the Alloy and the Dye run longer than a Flawless gem (" + alloy + ", " + dye + " vs " + jackpot + ")",
                alloy > jackpot && dye > jackpot);
        check("a Flawless gem runs longer than a rare roll", jackpot > rare);
        check("a bust is short (" + new VaultTimeline(Prize.BUST, 1).total() + ")", new VaultTimeline(Prize.BUST, 1).total() - R <= 2000);
        check("the build-up takes 4.5-5 s", R >= 4500 && R <= 5000);

        // The beats before the reveal: every crystal seated and every bolt back before the dial spins.
        check("every crystal is in before the wind-up", VaultTimeline.SOCKETS[4] < VaultTimeline.WIND);
        check("every bolt is back before the wind-up", VaultTimeline.bolt(4, VaultTimeline.WIND) >= 0.999f);
        check("the colour steps end before the reveal", VaultTimeline.HANG < R && VaultTimeline.STEPS.length >= 6);
        long prev = -1;
        boolean ordered = true;
        for (long s : VaultTimeline.STEPS) {
            ordered &= s > prev;
            prev = s;
        }
        check("the colour steps are in order", ordered);
        check("the dial does not jump at the reveal", Math.abs(VaultTimeline.dial(R + 1) - VaultTimeline.dial(R - 1)) < 0.3f);

        // The Dye's clock stops between its freeze and its thaw, and nothing before.
        VaultTimeline d = new VaultTimeline(Prize.test("dye"), 1);
        check("the Dye's clock stops", d.phys(VaultTimeline.FREEZE + 10) == d.phys(VaultTimeline.THAW - 10));
        check("the Dye's clock runs before the freeze", d.phys(R) == R);
        check("the Dye's door is blown off at the thaw", d.doorFlight(VaultTimeline.THAW - 1) < 0f && d.doorFlight(VaultTimeline.THAW + 100) > 0f);
        // The Meltdown: each crystal pours in turn, the door heats up, melts after the last pour, and the alloy rises after.
        VaultTimeline m = new VaultTimeline(Prize.test("alloy"), 1);
        check("the pours are after the reveal and before the melt", VaultTimeline.POURS[0] > R && VaultTimeline.POURS[4] < VaultTimeline.MELT);
        check("the door heats steadily", m.heat(VaultTimeline.POURS[0]) <= m.heat(VaultTimeline.POURS[2]) && m.heat(VaultTimeline.POURS[2]) < m.heat(VaultTimeline.MELT));
        check("the door is white hot when it melts", m.heat(VaultTimeline.MELT) >= 0.99f);
        check("it melts away entirely", m.melt(VaultTimeline.POOLED) >= 0.99f);
        check("the white-hot hold lasts a second or more", VaultTimeline.MELT - VaultTimeline.POURS[4] - 400 >= 1000);
        check("every win holds its breath after the reveal", VaultTimeline.HOLD >= 400
                && VaultTimeline.SWING > R + VaultTimeline.HOLD && VaultTimeline.KNOCKS[0] >= R + VaultTimeline.HOLD
                && VaultTimeline.POURS[0] >= R + VaultTimeline.HOLD);
        VaultTimeline jb = new VaultTimeline(Prize.test("ruby"), 1);
        check("three knocks, then the door goes", jb.knocked(VaultTimeline.BLOW - 1) == 3 && jb.blowAt() == VaultTimeline.BLOW
                && jb.doorFlight(VaultTimeline.BLOW - 1) < 0f && jb.doorFlight(VaultTimeline.BLOW + 100) > 0f);
        check("no knocks on any other ending", m.knock(VaultTimeline.KNOCKS[1] + 10) == 0f && d.knocked(VaultTimeline.THAW) == 0);
        check("the alloy rises only once it has melted", m.itemRise(VaultTimeline.MELT) == 0f && m.arrive() > VaultTimeline.POOLED);
        check("nothing heats on any other ending", new VaultTimeline(Prize.test("ruby"), 1).heat(VaultTimeline.MELT) == 0f);

        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
