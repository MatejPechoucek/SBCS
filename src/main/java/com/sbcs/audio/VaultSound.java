package com.sbcs.audio;

import com.sbcs.anim.VaultTimeline;
import com.sbcs.anim.VaultTimeline.Ending;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The Vault's whole soundtrack as one list of timed cues, worked out once from the timeline, as Hellion's is - the
 * screen plays what is due, and an offline tool can render the same list. Pure: no Minecraft.
 *
 * <b>Every cue before {@link VaultTimeline#REVEAL} is the same for every prize</b>: the door, each crystal's flight,
 * click and note, each bolt, the dial ticking through the colours over the door's strain. At the reveal everything
 * is cut and the outcome speaks.
 */
public final class VaultSound {
    private VaultSound() {}

    /** Some voices, due at {@code at} ms; {@code cut} silences everything still sounding first. */
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

    /** A major: what a win turns to, from the minor pentatonic of the build-up. */
    private static final int[] MAJOR = {0, 2, 4, 5, 7, 9, 11};
    private static final int[] HARMONIC_MINOR = {0, 2, 3, 5, 7, 8, 11};

    public static List<Cue> schedule(VaultTimeline tl) {
        long seed = tl.seed();
        long R = VaultTimeline.REVEAL;
        List<Cue> out = new ArrayList<>();

        // ---- before the reveal: identical for every prize
        out.add(new Cue(0, false, Voices.vaultDoor(seed)));
        for (int k = 0; k < 5; k++) {
            long s = VaultTimeline.SOCKETS[k];
            out.add(new Cue(s - VaultTimeline.FLY_MS, false, Voices.vaultFly(k, seed + 10 + k)));
            out.add(new Cue(s, false, Voices.vaultSocket(k, seed + 20 + k)));
            out.add(new Cue(s + VaultTimeline.BOLT_DELAY, false, Voices.vaultBolt(seed + 30 + k)));
        }
        out.add(new Cue(VaultTimeline.WIND, false, Voices.vaultStrain((R - VaultTimeline.WIND) / 1000f)));
        long[] steps = VaultTimeline.STEPS;
        for (int i = 0; i < steps.length; i++) out.add(new Cue(steps[i], false, Voices.vaultTick(i)));
        int f = 0;
        for (long t = VaultTimeline.HANG; t < R - 20; t += 45) out.add(new Cue(t, false, Voices.eruptionFlicker(f++)));

        // ---- the reveal
        Ending e = tl.ending();
        long arrive = tl.arrive();
        switch (e) {
            case BUST -> {
                out.add(new Cue(R, true, Voices.vaultEmpty(seed + 100)));
                out.add(new Cue(R + 150, false, Voices.vaultCreak(0.95f, 260f, 150f, seed + 110)));
                return sorted(out);
            }
            case OPEN -> {
                // The dial lands with a clunk; a held breath; the latch gives; then the door swings.
                long sw = VaultTimeline.SWING;
                out.add(new Cue(R, true, Voices.vaultBolt(seed + 100)));
                out.add(new Cue(R + VaultTimeline.HOLD, false, Voices.vaultCreak(0.22f, 520f, 200f, seed + 105)));
                out.add(new Cue(sw, false, Voices.vaultOpen(seed + 108)));
                out.add(new Cue(sw + 80, false, Voices.vaultCreak(0.7f, 420f, 300f, seed + 110)));
                out.add(new Cue(sw + 200, false, Voices.hellionRise((arrive - sw - 200) / 1000f)));
                out.add(new Cue(arrive, false, Voices.hellionArrive(false, seed + 120)));
            }
            case BLOWN -> {
                long b = tl.blowAt();
                out.add(new Cue(R, true, Voices.vaultBolt(seed + 100)));
                // Three knocks from inside, each heavier, and the strain building under them.
                long[] knocks = VaultTimeline.KNOCKS;
                for (int i = 0; i < knocks.length; i++) {
                    out.add(new Cue(knocks[i], false, Voices.vaultKnock(i, seed + 102 + i)));
                }
                out.add(new Cue(knocks[0], false, Voices.vaultStrain((b - knocks[0]) / 1000f)));
                out.add(new Cue(b, true, Voices.eruptionBoom(4, 0.9f, seed + 110)));
                out.add(new Cue(b + 60, false, Voices.seraphShatter(seed + 115)));
                out.add(new Cue(b + 200, false, Voices.hellionRise((arrive - b - 200) / 1000f)));
                out.add(new Cue(b + 200, false, Voices.score(220f, MAJOR, (arrive - b - 200) / 1000f)));
                out.add(new Cue(arrive, false, Voices.hellionArrive(true, seed + 120)));
                out.add(new Cue(arrive, false, Voices.fanfare()));
            }
            case ALLOY -> {
                // The Meltdown: each crystal pours into the dial, the door roars hotter, then melts.
                out.add(new Cue(R, true, Voices.vaultBolt(seed + 100)));
                for (int k = 0; k < 5; k++) out.add(new Cue(VaultTimeline.POURS[k], false, Voices.vaultPour(k)));
                long h0 = VaultTimeline.POURS[0];
                out.add(new Cue(h0, false, Voices.vaultHeat((VaultTimeline.MELT - h0) / 1000f, seed + 110)));
                out.add(new Cue(h0, false, Voices.score(220f, HARMONIC_MINOR, (VaultTimeline.MELT - h0) / 1000f)));
                out.add(new Cue(VaultTimeline.MELT, true, Voices.vaultMelt(seed + 120)));
                out.add(new Cue(VaultTimeline.MELT, false, Voices.hellionSupernova(seed + 125)));
                out.add(new Cue(VaultTimeline.MELT + 300, false, Voices.hellionRise((arrive - VaultTimeline.MELT - 300) / 1000f)));
                out.add(new Cue(VaultTimeline.MELT + 300, false, Voices.score(220f, MAJOR, (arrive - VaultTimeline.MELT - 300) / 1000f)));
                out.add(new Cue(arrive, false, Voices.hellionArrive(true, seed + 130)));
                out.add(new Cue(arrive, false, Voices.fanfare()));
            }
            case DYE -> {
                long fz = VaultTimeline.FREEZE, th = VaultTimeline.THAW;
                out.add(new Cue(R, true, Voices.vaultCreak(0.26f, 520f, 470f, seed + 100)));
                out.add(new Cue(fz, true, Voices.hellionFreeze(seed + 105)));
                out.add(new Cue(fz + 150, false, Voices.hellionStill((th - fz - 150) / 1000f)));
                long[] hearts = VaultTimeline.HEARTS;
                for (int i = 0; i < hearts.length; i++) {
                    out.add(new Cue(hearts[i] - 450, false, Voices.hellionPull(0.45f)));
                    out.add(new Cue(hearts[i], false, Voices.hellionHeart(i)));
                }
                out.add(new Cue(th, true, Voices.eruptionBoom(4, 0.9f, seed + 110)));
                out.add(new Cue(th, false, Voices.hellionSupernova(seed + 115)));
                out.add(new Cue(th + 150, false, Voices.hellionRise((arrive - th - 150) / 1000f)));
                out.add(new Cue(th + 150, false, Voices.score(220f, MAJOR, (arrive - th - 150) / 1000f)));
                out.add(new Cue(arrive, false, Voices.hellionArrive(true, seed + 120)));
                out.add(new Cue(arrive, false, Voices.fanfare()));
            }
        }
        out.add(new Cue(arrive + 300, false, Voices.hellionGlow((tl.total() - arrive) / 1000f + 1.0f, seed + 900)));
        return sorted(out);
    }

    /** By time; at the same moment a cut goes first, so it does not silence what it was cut for. */
    private static List<Cue> sorted(List<Cue> cues) {
        cues.sort(Comparator.comparingLong((Cue c) -> c.at).thenComparing(c -> !c.cut));
        return cues;
    }
}
