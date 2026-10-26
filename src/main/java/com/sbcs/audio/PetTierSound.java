package com.sbcs.audio;

import com.sbcs.anim.PetTierTimeline;
import com.sbcs.anim.PetTierTimeline.Tier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.sbcs.anim.PetTierTimeline.*;

/**
 * The Scatha pet reveal's sounds as one list of timed cues ({@link HellionSound.Cue}), from the moment the head is
 * cut free - the screen plays what is due, {@code tools/PetTierSoundtrack} renders the same list. Pure.
 *
 * <b>Every cue before {@link PetTierTimeline#LAND} is the same for every pet</b> (PetTierTest renders them and
 * compares): the head's chime is three notes whatever it is - it used to be 3, 4 or 5 at this moment, which told
 * the rarity before anything was shown - then the Thaw's build-up and a tick per colour of the cycle. At the landing
 * everything is cut and the colour it stopped on speaks with the Thaw's own flare voice (blue, violet, gold).
 */
public final class PetTierSound {
    private PetTierSound() {}

    /** A Legendary's score: A lydian, bright and a little strange. */
    private static final int[] LYDIAN = {0, 2, 4, 6, 7, 9, 11};

    public static List<HellionSound.Cue> schedule(PetTierTimeline tl) {
        long seed = tl.seed();
        List<HellionSound.Cue> out = new ArrayList<>();

        // ---- before the landing: identical for every pet
        out.add(new HellionSound.Cue(0, false, Voices.chime(3, 330f)));
        out.add(new HellionSound.Cue(CYCLE_FROM, false, Voices.thawBuildUp((LAND - CYCLE_FROM) / 1000f)));
        for (int i = 0; i < STEPS.length; i++) {
            out.add(new HellionSound.Cue(STEPS[i], false, new Voice[]{Voices.thawCycleTick(i)}));
        }

        // ---- the landing
        Tier tier = tl.tier();
        long card = tl.cardFrom();
        switch (tier) {
            case RARE -> {
                out.add(new HellionSound.Cue(LAND, true, Voices.thawClimb(0, 1.6f)));
                out.add(new HellionSound.Cue(LAND + 60, false, Voices.chime(3, 392f)));
            }
            case EPIC -> {
                out.add(new HellionSound.Cue(LAND, true, Voices.thawClimb(1, 2.2f)));
                out.add(new HellionSound.Cue(LAND, false, Voices.chopWhoosh(2, seed)));
                out.add(new HellionSound.Cue(LAND + 40, false, Voices.seraphShatter(seed + 1)));
                out.add(new HellionSound.Cue(LAND + 120, false, Voices.chime(4, 392f)));
                letters(out, tl, card);
            }
            case LEGENDARY -> {
                long L = LAND;
                out.add(new HellionSound.Cue(L, true, Voices.thawClimb(2, 1.2f)));
                out.add(new HellionSound.Cue(L + 40, false, Voices.heartbeat(seed + 2)));
                out.add(new HellionSound.Cue(L + L_CRACK, false, Voices.score(220f, LYDIAN, (L_BURST - L_CRACK) / 1000f)));
                for (int i = 0; i < 6; i++) {
                    long open = L + L_CRACK + i * L_CRACK_STEP;
                    out.add(new HellionSound.Cue(open, false, Voices.seraphCrack(0.25f, 0.15f, seed + 10 + i)));
                    out.add(new HellionSound.Cue(open + L_POUR_AFTER, false, Voices.gained(Voices.vaultPour(i), 0.5f)));
                }
                out.add(new HellionSound.Cue(L + L_COIL, false, Voices.hellionRise((L_RING - L_COIL) / 1000f)));
                for (int i = 0; i < 5; i++) {
                    out.add(new HellionSound.Cue(L + L_GOLD + i * L_GOLD_STEP, false, Voices.vaultSocket(i, seed + 20 + i)));
                }
                out.add(new HellionSound.Cue(L + L_BEAT2, false, Voices.heartbeat(seed + 3)));
                out.add(new HellionSound.Cue(L + L_BURST, true, Voices.gained(Voices.hellionSupernova(seed + 4), 0.55f)));
                out.add(new HellionSound.Cue(L + L_BURST + 30, false, Voices.chime(5, 440f)));
                out.add(new HellionSound.Cue(L + L_BURST + 200, false, Voices.gained(Voices.hellionArrive(true, seed + 5), 0.6f)));
                out.add(new HellionSound.Cue(L + L_BURST + 600, false, Voices.hellionGlow((tl.total() - L - L_BURST - 600) / 1000f, seed + 6)));
                letters(out, tl, card);
            }
        }
        out.sort(Comparator.comparingLong(c -> c.at));
        return out;
    }

    /** A tick per letter of the name as it is stamped. */
    private static void letters(List<HellionSound.Cue> out, PetTierTimeline tl, long card) {
        int n = (tl.tier().title + " Scatha").length();
        for (int i = 0; i < n; i++) {
            out.add(new HellionSound.Cue(card + 200 + i * tl.letterMs(), false, new Voice[]{Voices.letterTick(i)}));
        }
    }
}
