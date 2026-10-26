package com.sbcs.audio;

import com.sbcs.anim.PairsTimeline;
import com.sbcs.anim.PairsTimeline.Ending;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The Hunt's whole soundtrack as one list of timed cues, worked out once from the timeline, as the Vault's is - the
 * screen plays what is due and tools/PairsSoundtrack renders the same list. Pure: no Minecraft.
 *
 * <b>Every cue of a box before its reveal is the same whatever is in it</b>: the box flying in, a heartbeat quickening
 * under the build-up, a crack ticking up a step for each colour, the flicker; a box that got away has its own short,
 * grey wait (the player already knows). At the reveal everything is cut and the box speaks.
 */
public final class PairsSound {
    private PairsSound() {}

    public static final class Cue {
        public final long at;
        public final boolean cut;
        public final Voice[] voices;

        Cue(long at, boolean cut, Voice[] voices) {
            this.at = at;
            this.cut = cut;
            this.voices = voices;
        }
    }

    /** E major for a find, out of the E minor of the build-up. */
    private static final int[] MAJOR = {0, 2, 4, 5, 7, 9, 11};
    private static final int[] LYDIAN = {0, 2, 4, 6, 7, 9, 11};

    public static List<Cue> schedule(PairsTimeline tl) {
        List<Cue> out = new ArrayList<>();
        for (int i = 0; i < tl.count(); i++) box(out, tl, i);
        return sorted(out);
    }

    static void box(List<Cue> out, PairsTimeline tl, int i) {
        long s = tl.start(i);
        long seed = tl.seed() + 1000L * i;
        Ending e = tl.box(i).ending();
        long R = PairsTimeline.reveal(e);
        out.add(new Cue(s, false, PairsVoices.boxIn(seed)));

        if (e == Ending.MISSED) {
            out.add(new Cue(s + PairsTimeline.WIND, false, Voices.heartbeat(seed + 3)));
            out.add(new Cue(s + R, true, PairsVoices.missed(seed + 5)));
            out.add(new Cue(s + R + 100, false, Voices.vaultCreak(0.7f, 240f, 160f, seed + 6)));
            return;
        }

        // ---- the build-up: identical for every matched box
        // A heartbeat that quickens: from every 700 ms down to 380.
        long beat = PairsTimeline.IN + 100;
        float gap = 700f;
        int b = 0;
        while (beat < R - 250) {
            out.add(new Cue(s + beat, false, Voices.heartbeat(seed + 10 + b++)));
            beat += Math.round(gap);
            gap = Math.max(380f, gap * 0.9f);
        }
        long[] steps = PairsTimeline.STEPS;
        for (int k = 0; k < steps.length; k++) out.add(new Cue(s + steps[k], false, PairsVoices.crack(k, seed + 40 + k)));
        out.add(new Cue(s + PairsTimeline.WIND, false, Voices.thawBuildUp((R - PairsTimeline.WIND) / 1000f)));
        int f = 0;
        for (long t = PairsTimeline.HANG; t < R - 20; t += 50) out.add(new Cue(s + t, false, Voices.eruptionFlicker(f++)));

        // ---- the reveal
        long arrive = PairsTimeline.arrive(e);
        switch (e) {
            case RARE -> {
                out.add(new Cue(s + R, true, Voices.seraphShatter(seed + 100)));
                out.add(new Cue(s + R, false, Voices.eruptionBoom(2, 0.45f, seed + 101)));
                out.add(new Cue(s + R + 150, false, Voices.hellionRise((arrive - R - 150) / 1000f)));
                out.add(new Cue(s + arrive, false, Voices.hellionArrive(false, seed + 110)));
            }
            case ULTRA -> {
                out.add(new Cue(s + R, true, Voices.eruptionBoom(3, 0.7f, seed + 100)));
                out.add(new Cue(s + R + 60, false, Voices.chiOpen(seed + 102)));
                long open = PairsTimeline.BOOK_OPEN, flash = PairsTimeline.BOOK_FLASH;
                out.add(new Cue(s + PairsTimeline.BOOK_RISE, false, Voices.hellionRise((flash - PairsTimeline.BOOK_RISE) / 1000f)));
                out.add(new Cue(s + open, false, Voices.vaultCreak(0.35f, 420f, 300f, seed + 104)));
                out.add(new Cue(s + PairsTimeline.RIFFLE, false, PairsVoices.riffle((flash - PairsTimeline.RIFFLE) / 1000f, seed + 106)));
                out.add(new Cue(s + PairsTimeline.RIFFLE - 200, false, Voices.chiShimmer((flash - PairsTimeline.RIFFLE + 200) / 1000f, seed + 108)));
                out.add(new Cue(s + PairsTimeline.RIFFLE, false, Voices.score(164.81f, LYDIAN, (flash - PairsTimeline.RIFFLE) / 1000f)));
                out.add(new Cue(s + flash, true, Voices.hellionSupernova(seed + 112)));
                out.add(new Cue(s + arrive, false, Voices.hellionArrive(true, seed + 114)));
                out.add(new Cue(s + arrive, false, Voices.fanfare()));
            }
            case DYE -> {
                long fz = PairsTimeline.FREEZE, th = PairsTimeline.THAW;
                out.add(new Cue(s + R, true, Voices.vaultCreak(0.24f, 600f, 520f, seed + 100)));
                out.add(new Cue(s + fz, true, Voices.hellionFreeze(seed + 102)));
                out.add(new Cue(s + fz + 150, false, Voices.hellionStill((th - fz - 150) / 1000f)));
                long[] hearts = PairsTimeline.HEARTS;
                for (int k = 0; k < hearts.length; k++) {
                    out.add(new Cue(s + hearts[k] - 420, false, Voices.hellionPull(0.42f)));
                    out.add(new Cue(s + hearts[k], false, Voices.hellionHeart(k)));
                }
                out.add(new Cue(s + th, true, Voices.eruptionBoom(4, 0.85f, seed + 110)));
                out.add(new Cue(s + th, false, PairsVoices.petals((arrive - th) / 1000f, seed + 112)));
                out.add(new Cue(s + th + 100, false, Voices.score(164.81f, MAJOR, (arrive - th - 100) / 1000f)));
                out.add(new Cue(s + th + 100, false, Voices.hellionRise((arrive - th - 100) / 1000f)));
                out.add(new Cue(s + arrive, false, Voices.hellionArrive(true, seed + 114)));
                out.add(new Cue(s + arrive, false, Voices.fanfare()));
            }
            default -> { }
        }
        out.add(new Cue(s + arrive + 300, false, Voices.hellionGlow((PairsTimeline.end(e) - arrive) / 1000f, seed + 900)));
    }

    /** By time; at the same moment a cut goes first. */
    private static List<Cue> sorted(List<Cue> cues) {
        cues.sort(Comparator.comparingLong((Cue c) -> c.at).thenComparing(c -> !c.cut));
        return cues;
    }
}
