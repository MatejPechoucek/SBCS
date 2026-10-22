package com.sbcs.audio;

import com.sbcs.anim.OssuaryTimeline;
import com.sbcs.anim.OssuaryTimeline.Ending;

import java.util.List;

/**
 * The Ossuary's sounds. Unlike the Vault's fixed cue list these follow the roll as it happens - the player pulls the
 * chain when they like - so the host asks every frame for what fell due between the last frame and this one
 * ({@link #between}). Pure: no Minecraft, so OssuaryTest can check the honesty.
 *
 * <b>Nothing before the reveal depends on the outcome</b>: the chain, the drums' ticks, the stops, the seals, the
 * levers and the tell's steps only read the timeline's pre-reveal state, which OssuaryTest shows is the same for every
 * chest. At the reveal everything is cut ({@link Cue#cut}) and the outcome speaks.
 */
public final class OssuarySound {
    private OssuarySound() {}

    public record Cue(boolean cut, Voice[] voices) {}

    /** At most this many drum ticks a frame - a fast drum is a rattle, not a burst. */
    static final int MAX_TICKS = 2;

    /** Adds every cue due in ({@code from}, {@code to}] to {@code out}. */
    public static void between(OssuaryTimeline tl, long from, long to, List<Cue> out) {
        if (to <= from) return;
        long seed = tl.seed();
        if (due(0, from, to)) out.add(new Cue(false, Voices.vaultDoor(seed)));
        // The seals: a crack each, the last one heavier.
        for (int i = 0; i < tl.seals(); i++) {
            long a = tl.sealAt(i);
            if (a >= 0 && due(a, from, to)) out.add(new Cue(false, Voices.vaultKnock(i == tl.seals() - 1 ? 2 : i % 2, seed + 40 + i)));
        }
        if (tl.unlockAt() > OssuaryTimeline.ARRIVE && due(tl.unlockAt(), from, to)) out.add(new Cue(false, Voices.vaultOpen(seed + 50)));
        // The drums: the hand reaching in, every pull, a tick per niche going past, the stop.
        for (int d = 0; d < tl.drumCount(); d++) {
            OssuaryTimeline.Drum dr = tl.drum(d);
            long hand = tl.handAt(d);
            if (hand >= 0 && due(hand, from, to)) out.add(new Cue(false, new Voice[]{Voices.swing(0.3f, seed + 60 + d)}));
            for (long[] p : dr.pulls) {
                if (!due(p[0], from, to)) continue;
                float strength = p[1] / 1000f;
                out.add(new Cue(false, Voices.vaultBolt(seed + 70 + d)));
                out.add(new Cue(false, Voices.stingClick(d, false, seed + 80 + d)));
                out.add(new Cue(false, Voices.hellionRodPass(0.6f + strength, seed + 90 + d)));
            }
            if (dr.t0 >= 0 && to > dr.t0 && (dr.stopAt < 0 || from < dr.stopAt)) {
                long a = Math.max(from, dr.t0), b = dr.stopAt < 0 ? to : Math.min(to, dr.stopAt);
                long passed = (long) Math.floor(tl.pos(d, b)) - (long) Math.floor(tl.pos(d, a));
                int n = (int) Math.min(MAX_TICKS, Math.max(0, passed));
                for (int k = 0; k < n; k++) out.add(new Cue(false, new Voice[]{Voices.thawCycleTick(d * 2 + k)}));
            }
            if (dr.stopAt >= 0 && due(dr.stopAt, from, to)) {
                out.add(new Cue(false, Voices.chopLand(seed + 100 + d)));
                if (dr.big) out.add(new Cue(false, Voices.vaultBolt(seed + 110 + d)));
            }
        }
        // Livid's nine, lining up and the pick.
        if (tl.livid()) {
            if (tl.liviLineAt() >= 0 && due(tl.liviLineAt(), from, to)) out.add(new Cue(false, Voices.seraphSwirl(0.5f, seed + 120)));
            if (tl.liviPickAt() >= 0 && due(tl.liviPickAt(), from, to)) out.add(new Cue(false, Voices.seraphPoof(seed + 125)));
        }
        // The tell: the riser under it and a tick a step, the lantern (or a dragon) changing colour.
        long tell = tl.tellAt();
        if (tell >= 0) {
            if (due(tell, from, to)) out.add(new Cue(false, Voices.vaultStrain(OssuaryTimeline.TELL_MS / 1000f)));
            for (int i = 0; i < tl.stepCount(); i++) {
                if (due(tl.stepStart(i), from, to)) out.add(new Cue(false, tl.dragons()
                        ? Voices.seraphPlip(i) : Voices.vaultTick(i)));
            }
        }
        long r = tl.reveal();
        if (r < 0) return;
        if (due(r, from, to)) reveal(tl, out);
        if (to <= r) return;
        // After the reveal: the ending's own.
        Ending e = tl.ending();
        long arrive = tl.arrive();
        if (e == Ending.DYE) {
            if (due(r + OssuaryTimeline.FREEZE_AFTER, from, to)) out.add(new Cue(false, Voices.hellionStill(
                    (OssuaryTimeline.THAW_AFTER - OssuaryTimeline.FREEZE_AFTER) / 1000f)));
            for (int k = 0; k < OssuaryTimeline.HEARTS.length; k++) {
                if (due(r + OssuaryTimeline.HEARTS[k], from, to)) out.add(new Cue(false, Voices.hellionHeart(k)));
            }
            if (due(r + OssuaryTimeline.THAW_AFTER, from, to)) out.add(new Cue(true, Voices.hellionSupernova(seed + 140)));
        }
        if (e != Ending.BUST) {
            if (due(arrive - 600, from, to) && arrive - 600 > r) out.add(new Cue(false, Voices.hellionRise(0.6f)));
            if (due(arrive, from, to)) {
                out.add(new Cue(false, Voices.hellionArrive(e != Ending.WIN, seed + 150)));
                if (e == Ending.JACKPOT) out.add(new Cue(false, Voices.hellionJackpot(seed + 155)));
                if (e != Ending.WIN) out.add(new Cue(false, Voices.fanfare()));
            }
            if (due(arrive + 300, from, to)) out.add(new Cue(false, Voices.hellionGlow(Math.max(0.5f,
                    (tl.total() - arrive - 300) / 1000f), seed + 160)));
        }
    }

    /** The reveal: everything cut, then the lantern landing on the truth. */
    static void reveal(OssuaryTimeline tl, List<Cue> out) {
        long seed = tl.seed();
        switch (tl.ending()) {
            case BUST -> {
                out.add(new Cue(true, Voices.vaultEmpty(seed + 130)));
                out.add(new Cue(false, Voices.labSnuff(seed + 131)));
            }
            case WIN -> out.add(new Cue(true, Voices.vaultBolt(seed + 130)));
            case JACKPOT -> {
                out.add(new Cue(true, Voices.vaultBolt(seed + 130)));
                out.add(new Cue(false, tl.dragons() ? Voices.seraphRoar(1.6f, true, 0.7f, seed + 132) : Voices.hellionSupernova(seed + 132)));
            }
            case DYE -> out.add(new Cue(true, Voices.hellionFreeze(seed + 130)));
        }
    }

    static boolean due(long at, long from, long to) {
        return at > from && at <= to;
    }
}
