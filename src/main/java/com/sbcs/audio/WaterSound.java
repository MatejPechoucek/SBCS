package com.sbcs.audio;

import com.sbcs.anim.WaterTimeline;
import com.sbcs.anim.WaterTimeline.Creature;
import com.sbcs.anim.WaterTimeline.Ending;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The Coil's and the Totem's soundtrack as one list of timed cues, worked out once from the timeline - the screen
 * plays what is due and {@code tools/WaterSoundtrack} renders the same list to a WAV. Pure.
 *
 * <b>Every cue before {@link WaterTimeline#REVEAL} is the same for every drop</b> (WaterTest renders them and
 * compares): the creature breaking the surface, the bayou's rumble, a breath per colour step pitched by its level (the
 * Wiki Tiki's faces and lasers add a zap each), the build and the flicker. At the reveal everything is cut and the
 * outcome speaks. Built from existing voices - lava fishing's, the Hunt's geyser, the Paint's stopped time.
 */
public final class WaterSound {
    private WaterSound() {}

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

    private static final int[] DORIAN = {0, 2, 3, 5, 7, 9, 10};
    private static final int[] LYDIAN = {0, 2, 4, 6, 7, 9, 11};
    /** The score's root: A. */
    private static final float ROOT = 220f;

    public static List<Cue> schedule(WaterTimeline tl) {
        long seed = tl.seed();
        long R = WaterTimeline.REVEAL;
        boolean tiki = tl.creature() == Creature.WIKI_TIKI;
        List<Cue> out = new ArrayList<>();

        // ---- before the reveal: the same for every drop
        out.add(new Cue(0, false, Voices.eruptionRumble(R / 1000f, seed)));
        out.add(new Cue(WaterTimeline.EMERGE_FROM, false, Voices.finPlunge(seed + 1)));
        out.add(new Cue(WaterTimeline.EMERGE_FROM + 500, false, Voices.paintWave(0.9f, seed + 2)));
        if (tiki) {
            for (int i = 0; i < WaterTimeline.FACES.length; i++) out.add(new Cue(WaterTimeline.FACES[i], false, Voices.eruptionZap(seed + 10 + i)));
            out.add(new Cue(WaterTimeline.LASERS_FROM + 400, false, Voices.eruptionCarmine((R - WaterTimeline.LASERS_FROM - 400) / 1000f)));
        }
        long[] steps = WaterTimeline.STEPS;
        for (int i = 0; i < steps.length; i++) {
            out.add(new Cue(steps[i], false, Voices.eruptionBreath(WaterTimeline.stepLevel(i), i / (float) Math.max(1, steps.length - 1), seed + 100 + i)));
        }
        long build = WaterTimeline.HANG - 400;
        out.add(new Cue(build, false, Voices.eruptionBuild((R - build) / 1000f)));
        int f = 0;
        for (long t = WaterTimeline.HANG; t < R; t += 55) out.add(new Cue(t, false, Voices.eruptionFlicker(f++)));

        // ---- the reveal
        Ending e = tl.ending();
        long arrive = tl.arrive();
        if (e == Ending.BUST) {
            out.add(new Cue(R, true, Voices.eruptionCool(seed + 600)));
            if (tiki) {
                out.add(new Cue(WaterTimeline.B_CRACK, false, Voices.eruptionGroundCrack(seed + 601)));
                out.add(new Cue(WaterTimeline.B_TOPPLE + 500, false, Voices.finPlunge(seed + 602)));
                out.add(new Cue(WaterTimeline.B_TOPPLE + 850, false, Voices.finPlunge(seed + 603)));
            } else {
                out.add(new Cue(WaterTimeline.B_SINK, false, Voices.eruptionSink(seed + 601)));
            }
            out.add(new Cue(WaterTimeline.B_NAME, false, Voices.eruptionConsolation()));
            return sorted(out);
        }
        switch (e) {
            case BOOK -> {
                out.add(new Cue(R, true, Voices.eruptionZap(seed + 610)));
                out.add(new Cue(WaterTimeline.K_UNDER, false, Voices.eruptionSink(seed + 611)));
            }
            case SHED -> {
                out.add(new Cue(R, true, Voices.eruptionVent(seed + 620)));
                out.add(new Cue(WaterTimeline.S_SLIDE, false, Voices.eruptionScuttle(0.4f, (WaterTimeline.S_SLIDE_TO - WaterTimeline.S_SLIDE) / 1000f - 0.4f, seed + 621)));
                out.add(new Cue(WaterTimeline.S_CRUMPLE, false, Voices.eruptionCrust((WaterTimeline.S_CRUMPLE_TO - WaterTimeline.S_CRUMPLE) / 1000f, seed + 622)));
            }
            case EYES -> {
                out.add(new Cue(R, true, Voices.hellionFreeze(seed + 630)));
                out.add(new Cue(WaterTimeline.E_DARK, false, Voices.hellionStill((WaterTimeline.E_OPEN - WaterTimeline.E_DARK) / 1000f)));
                out.add(new Cue(WaterTimeline.E_OPEN, false, Voices.eruptionEye(seed + 631)));
                out.add(new Cue(WaterTimeline.E_BLINK, false, Voices.paintBlup()));
                out.add(new Cue(WaterTimeline.E_OPEN, false, Voices.score(ROOT, DORIAN, (arrive - WaterTimeline.E_OPEN) / 1000f)));
            }
            case BUBBLE -> {
                out.add(new Cue(R, true, Voices.eruptionZap(seed + 640)));
                out.add(new Cue(WaterTimeline.U_GROW, false, Voices.hellionPull((WaterTimeline.U_GROW_TO - WaterTimeline.U_GROW) / 1000f)));
                for (int i = 0; i < 5; i++) out.add(new Cue(WaterTimeline.U_GROW + i * 280L, false, Voices.seraphGlug(i)));
                out.add(new Cue(WaterTimeline.U_FREE, false, Voices.paintBlup()));
            }
            case MASK -> {
                out.add(new Cue(R, true, Voices.eruptionZap(seed + 650)));
                out.add(new Cue(WaterTimeline.M_PEEL, false, Voices.eruptionGroundCrack(seed + 651)));
                out.add(new Cue(WaterTimeline.M_TURN, false, Voices.score(ROOT, DORIAN, (arrive - WaterTimeline.M_TURN) / 1000f)));
                out.add(new Cue(WaterTimeline.M_EYES, false, Voices.eruptionEye(seed + 652)));
            }
            case FLOOD -> {
                // Time stops; three heartbeats; the aquamarine climbs; time comes back and it goes up.
                out.add(new Cue(WaterTimeline.F_FREEZE, true, Voices.paintFreeze(seed + 660)));
                out.add(new Cue(WaterTimeline.F_FREEZE + 300, false, Voices.paintDrone((WaterTimeline.F_RESUME - WaterTimeline.F_FREEZE - 300) / 1000f)));
                for (int i = 0; i < WaterTimeline.F_HEARTS.length; i++) out.add(new Cue(WaterTimeline.F_HEARTS[i], false, Voices.hellionHeart(i + 1)));
                for (int i = 0; i < 8; i++) {
                    out.add(new Cue(WaterTimeline.F_RISE + i * (WaterTimeline.F_RISE_TO - WaterTimeline.F_RISE) / 8, false, Voices.seraphGlug(i)));
                }
                out.add(new Cue(WaterTimeline.F_RISE, false, Voices.paintWave((WaterTimeline.F_RISE_TO - WaterTimeline.F_RISE) / 1000f, seed + 661)));
                out.add(new Cue(WaterTimeline.F_RESUME, true, Voices.paintResume()));
                out.add(new Cue(WaterTimeline.F_REAR, false, Voices.huntGeyser((WaterTimeline.F_REAR_TO - WaterTimeline.F_REAR) / 1000f + 0.8f, seed + 662)));
                out.add(new Cue(WaterTimeline.F_REAR, false, Voices.gained(Voices.paintSplash(seed + 663), 0.7f)));
                out.add(new Cue(WaterTimeline.F_REAR, false, Voices.score(ROOT, LYDIAN, (arrive - WaterTimeline.F_REAR) / 1000f)));
                out.add(new Cue(WaterTimeline.F_DYE, false, Voices.gained(Voices.hellionSupernova(seed + 664), 0.8f)));
            }
            default -> { }
        }
        long from = tl.riseFrom();
        if (from < arrive) out.add(new Cue(from, false, Voices.eruptionRise((arrive - from) / 1000f)));
        out.add(new Cue(arrive, false, Voices.eruptionArrive(tl.banner(), seed + 810)));
        out.add(new Cue(arrive + 300, false, Voices.eruptionGlow((tl.total() - arrive) / 1000f + 1.0f, seed + 900)));
        return sorted(out);
    }

    /** By time; at the same moment a cut goes first, so it does not silence what it was cut for. */
    private static List<Cue> sorted(List<Cue> cues) {
        cues.sort(Comparator.comparingLong((Cue c) -> c.at).thenComparing(c -> !c.cut));
        return cues;
    }
}
