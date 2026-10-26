package com.sbcs.render;

/**
 * How hard the effects hit (2026-10-24, Matej: "make all effects more exaggerated").
 *
 * Every screen shake, glow and burst count in the cutscenes goes through this. Before a reveal it is
 * {@link #BASE} on every run, so nothing before the reveal depends on the drop and the honesty tests still
 * hold; from the reveal on, a win pushes it up with its worth (bigger drops, more excitement). A bust stays
 * at the base. The screens set it each frame ({@link #set}); the offline previews and tests leave it at the
 * base.
 */
public final class Punch {
    private Punch() {}

    /** Every effect, before the reveal and on a bust: half as hard again as the effects were built. */
    public static final float BASE = 1.5f;

    /** At the top: a win worth {@link #TOP_COINS} or more. */
    public static final float MAX = 2.4f;

    static final double FLOOR_COINS = 500_000, TOP_COINS = 200_000_000;

    /** Now. */
    public static float now = BASE;

    /**
     * @param revealed past the reveal (the first moment the drop may show)
     * @param win      a win, not a bust
     * @param coins    its price, or 0 / negative while unknown
     */
    public static void set(boolean revealed, boolean win, long coins) {
        now = revealed && win ? BASE * worth(coins) : BASE;
    }

    /** By worth, log scale: 1 at 500k or less, MAX / BASE at 200M; an unknown price counts as a middling win. */
    public static float worth(long coins) {
        float top = MAX / BASE;
        if (coins <= 0) return 1f + (top - 1f) * 0.35f;
        double f = Math.log(coins / FLOOR_COINS) / Math.log(TOP_COINS / FLOOR_COINS);
        return 1f + (top - 1f) * (float) Math.max(0, Math.min(1, f));
    }

    /** For a band of outcomes with no price (0 bust .. 1 the best there is). */
    public static void setLevel(boolean revealed, float level) {
        now = revealed ? BASE + (MAX - BASE) * Math.max(0f, Math.min(1f, level)) : BASE;
    }

    /** Screen shakes. */
    public static float shake() {
        return now;
    }

    /** Glows: brighter, and a little bigger. Kept short of a wash - a full-screen glow over the game is fog. */
    public static float glowAlpha() {
        return 1f + (now - 1f) * 0.45f;
    }

    public static float glowSize() {
        return 1f + (now - 1f) * 0.3f;
    }

    /** Bursts of squares, debris, sparks: more of them. */
    public static int count(int n) {
        return Math.round(n * (1f + (now - 1f) * 0.8f));
    }
}
