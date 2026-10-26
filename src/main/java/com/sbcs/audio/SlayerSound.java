package com.sbcs.audio;

import com.sbcs.anim.SlayerTimeline;
import com.sbcs.anim.SlayerTimeline.Drop;
import com.sbcs.anim.SlayerTimeline.Tier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * "Exhumed" and "The Brood" - the Revenant's and the Tarantula's soundtracks as timed cues, worked out once from the
 * timeline, the way HellionSound does it. Pure: no Minecraft, so {@code tools/SlayerSoundtrack} renders the same list.
 *
 * These open on a known drop (the slayer drop line is the trigger), so there is no tell to keep honest. The beats of
 * each ending are offsets from {@link SlayerTimeline#ENDING} and mirror the constants in render/SlayerEndings (and
 * BroodEndings for the Tarantula): move them together.
 */
public final class SlayerSound {
    private SlayerSound() {}

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

    private static final int[] PHRYGIAN = {0, 1, 3, 5, 7, 8, 10};
    private static final int[] HARMONIC_MINOR = {0, 2, 3, 5, 7, 8, 11};

    public static List<Cue> schedule(SlayerTimeline tl) {
        long seed = tl.seed();
        Drop d = tl.drop();
        long E = SlayerTimeline.ENDING;
        List<Cue> out = new ArrayList<>();
        // ---- the kill: three blows, the last one fells it.
        long[] hits = SlayerTimeline.HITS;
        for (int i = 0; i < hits.length; i++) {
            out.add(new Cue(hits[i] - 120, false, Voices.chopWhoosh(i, seed + 10 + i)));
            out.add(new Cue(hits[i], false, Voices.chopHit(i, seed + 20 + i)));
        }
        out.add(new Cue(SlayerTimeline.KILLED + 60, false, Voices.chopLand(seed + 30)));

        // ---- the ending's own beats
        switch (d.ending) {
            case PLAIN -> out.add(new Cue(E + 350, false, Voices.impact(0.6f, seed + 100)));
            case BOOK -> {
                out.add(new Cue(E + 100, false, Voices.chiShimmer(1.2f, seed + 100)));
                out.add(new Cue(E + 700, false, Voices.chiOpen(seed + 101)));
            }
            case PARAGON -> {
                for (int i = 0; i < 4; i++) out.add(new Cue(E + 300 + i * 150, false, Voices.seraphPowerUp(i % 3, seed + 100 + i)));
                out.add(new Cue(E + 900, false, Voices.seraphShatter(seed + 110)));
            }
            case BEHEAD -> {
                out.add(new Cue(E + 100, false, Voices.chopSplit()));
                out.add(new Cue(E + 700, false, Voices.chopLand(seed + 100)));
                out.add(new Cue(E + 900, false, Voices.stingDrip(seed + 101)));
            }
            case SNAKES -> {
                out.add(new Cue(E + 200, false, Voices.stingHiss(2.4f, seed + 100)));
                out.add(new Cue(E + 2700, false, Voices.chiSerpent(seed + 101)));
            }
            case MAGGOT -> {
                out.add(new Cue(E + 200, false, Voices.stingCreak(2.2f, 90f, 160f, seed + 100)));
                for (int i = 0; i < 6; i++) out.add(new Cue(E + 400 + i * 330, false, Voices.paintPlip(i)));
                out.add(new Cue(E + 2600, false, Voices.paintSplash(seed + 110)));
            }
            case SCYTHE -> {
                for (int i = 0; i < 3; i++) out.add(new Cue(E + i * 380, false, Voices.stingWhoosh(0.3f, seed + 100 + i)));
                out.add(new Cue(E + 1500, false, Voices.midasClank(seed + 110)));
            }
            case SINEW -> {
                out.add(new Cue(E + 1300, false, Voices.stingCreak(1.2f, 70f, 240f, seed + 100)));
                out.add(new Cue(E + 2500, false, Voices.stingImpact(seed + 101)));
            }
            case HAND -> {
                out.add(new Cue(E + 250, false, Voices.chopSplit()));
                out.add(new Cue(E + 950, false, Voices.chopLand(seed + 100)));
                for (int i = 0; i < 5; i++) out.add(new Cue(E + 1100 + i * 220, false, Voices.stingClick(i, false, seed + 110 + i)));
                out.add(new Cue(E + 2200, false, Voices.stingWhoosh(0.4f, seed + 120)));
            }
            case WARDEN -> {
                out.add(new Cue(E, true, Voices.eruptionRumble(3.4f, seed + 100)));
                out.add(new Cue(E + 500, false, Voices.eruptionGroundCrack(seed + 101)));
                out.add(new Cue(E + 1200, false, Voices.heartbeat(seed + 102)));
                out.add(new Cue(E + 2200, false, Voices.heartbeat(seed + 103)));
                out.add(new Cue(E + 3600, false, Voices.eruptionRoar(1.8f, seed + 104)));
                for (int i = 0; i < 3; i++) out.add(new Cue(E + 4400 + i * 400, false, Voices.heartbeat(seed + 105 + i)));
                out.add(new Cue(E + 5600, false, Voices.stingStab(seed + 110)));
                out.add(new Cue(E + 5650, false, Voices.paintSplash(seed + 111)));
                out.add(new Cue(E + 5800, false, Voices.huntThud(0, true, seed + 112)));
                out.add(new Cue(E + 6000, false, Voices.heartMuffled()));
            }
            case MATCHA -> {
                out.add(new Cue(E + 200, true, Voices.paintFreeze(seed + 100)));
                out.add(new Cue(E + 400, false, Voices.paintDrone(2.3f)));
                for (int b = 0; b < 3; b++) out.add(new Cue(E + 700 + b * 560, false, Voices.hellionHeart(b)));
                out.add(new Cue(E + 2400, false, Voices.paintBlup()));
                out.add(new Cue(E + 2700, false, Voices.paintSplash(seed + 101)));
                out.add(new Cue(E + 2800, false, Voices.paintWave(2.4f, seed + 102)));
                for (int i = 0; i < 6; i++) out.add(new Cue(E + 3400 + i * 300, false, Voices.paintPlip(i)));
                out.add(new Cue(E + 5200, false, Voices.fleeceWind(1.5f, seed + 103)));
                out.add(new Cue(E + 6700, false, Voices.paintResume()));
            }
            // ---- the Tarantula's: rough beats until BroodEndings settles them
            case COCOON -> out.add(new Cue(E + 1500, false, Voices.chopCrumble(seed + 100)));
            case SWATTER -> out.add(new Cue(E + 1500, false, Voices.labSlam()));
            case TALISMAN -> out.add(new Cue(E + 2000, false, Voices.chime(3, 523.25f)));
            case VENOM -> {
                out.add(new Cue(E + 600, false, Voices.stingHiss(1.6f, seed + 100)));
                out.add(new Cue(E + 2300, false, Voices.stingClick(0, false, seed + 101)));
            }
            case DARKNESS -> {
                out.add(new Cue(E + 300, true, Voices.labSnuff(seed + 100)));
                out.add(new Cue(E + 1000, false, Voices.seraphScream(1.2f, seed + 101)));
                out.add(new Cue(E + 2600, false, Voices.labIgnite(1.0f)));
            }
            case WASP, SNAIL -> out.add(new Cue(E + 1500, false, Voices.stingClick(1, false, seed + 100)));
            case MOSQUITO -> {
                out.add(new Cue(E + 200, false, Voices.seraphSwirl(1.0f, seed + 100)));
                out.add(new Cue(E + 1900, false, Voices.chopSplit()));
                out.add(new Cue(E + 2100, false, Voices.brainHum(2.2f)));
                out.add(new Cue(E + 4500, false, Voices.stingImpact(seed + 101)));
            }
            case EYE -> {
                out.add(new Cue(E + 400, false, Voices.seraphDrone(3.4f, seed + 100)));
                out.add(new Cue(E + 4000, false, Voices.seraphEyeOpen(seed + 101)));
            }
            case BRICK -> {
                out.add(new Cue(E + 200, true, Voices.paintFreeze(seed + 100)));
                out.add(new Cue(E + 400, false, Voices.paintDrone(1.6f)));
                for (int i = 0; i < 8; i++) out.add(new Cue(E + 2000 + i * 280, false, Voices.huntThud(i, false, seed + 110 + i)));
                out.add(new Cue(E + 4800, false, Voices.vaultHeat(1.1f, seed + 120)));
                out.add(new Cue(E + 5900, false, Voices.eruptionBoom(3, 0.8f, seed + 121)));
                out.add(new Cue(E + 6000, false, Voices.paintResume()));
            }
        }

        // ---- the showcase
        long arrive = tl.arrive();
        boolean big = d.tier == Tier.RNGESUS;
        long riseFrom = Math.max(E, arrive - 1400);
        out.add(new Cue(riseFrom, false, Voices.hellionRise((arrive - riseFrom) / 1000f)));
        if (big) out.add(new Cue(riseFrom, false, Voices.score(d.source == SlayerTimeline.Source.REVENANT ? 146.83f : 164.81f,
                d.dye() ? HARMONIC_MINOR : PHRYGIAN, (arrive - riseFrom) / 1000f)));
        out.add(new Cue(arrive, false, Voices.hellionArrive(big, seed + 800)));
        out.add(new Cue(arrive + 300, false, Voices.hellionGlow((tl.total() - arrive) / 1000f + 1.0f, seed + 900)));
        cues(out);
        return out;
    }

    /** By time; at the same moment a cut goes first, so it does not silence what it was cut for. */
    private static void cues(List<Cue> cues) {
        cues.sort(Comparator.comparingLong((Cue c) -> c.at).thenComparing(c -> !c.cut));
    }
}
