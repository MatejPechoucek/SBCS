package com.sbcs.anim;

import com.sbcs.detect.superpairs.PairsReward;

import java.util.ArrayList;
import java.util.List;

/**
 * "The Hunt" - Superpairs' reveal, played once the game is over. Pure: a function of time, the boxes and a seed, so it
 * can be checked offline.
 *
 * During the game every tile from the rare row up was hidden under a plain black box with a letter on it (the cover,
 * PairsHunt). Now each box comes out in turn, in letter order - matched boxes first, then any that got away - drifts
 * to the middle and opens:
 * <ul>
 *   <li>the build-up is the same for every matched box: it turns, trembles, and cracks of light spread over it,
 *   cycling aqua, violet, pink, faster and faster, until at {@link #REVEAL} they land on one colour - the answer.
 *   <b>Nothing before a box's reveal depends on what is in it</b> (tools/PairsTest hashes every frame and sound);</li>
 *   <li>aqua - a rare reward (Experiment the Fish, the Metaphysical Serum, a Legendary Guardian): the box bursts into
 *   black shards and the reward rises;</li>
 *   <li>violet - an ultra-rare book (a VII) or an endcap: the lid blows off, the sides fall open, a book rises, opens,
 *   its pages riffle while enchanting glyphs stream into it, and it flashes into the reward;</li>
 *   <li>pink - the Nadeshiko Dye: the box cracks pink, the world stops and greys, three heartbeats, then it bursts
 *   into a storm of petals that fills the screen and gathers into the dye.</li>
 * </ul>
 * A box that never found its pair is already known to the player as lost, so it does not pretend otherwise: a short,
 * grey build-up, the lid lifts a crack, and what got away rises grey ("Got away").
 */
public final class PairsTimeline {

    /** How a box ends, and the colour its cracks land on - the tell. */
    public enum Ending {
        MISSED(0x6A6A72), RARE(0x55D8FF), ULTRA(0xC77DFF), DYE(0xFF7EC0);

        public final int tell;

        Ending(int tell) {
            this.tell = tell;
        }
    }

    /** One box, as the reveal shows it. */
    public record Box(char mark, PairsReward reward, boolean matched) {
        public Ending ending() {
            if (!matched) return Ending.MISSED;
            return switch (reward.tier()) {
                case DYE -> Ending.DYE;
                case ULTRA -> Ending.ULTRA;
                default -> Ending.RARE;
            };
        }

        public String title() { return reward.title(); }
        public int colour() { return reward.colour(); }
    }

    /** The colours the cracks cycle through before the reveal, from the lowest up; the same for every box. */
    public static final int[] CYCLE = {Ending.RARE.tell, Ending.ULTRA.tell, Ending.DYE.tell};

    // ------------------------------------------------------------------ one box's beats, ms from its start

    /** The box flies in from the corner to the middle. */
    public static final long IN = 650;
    /** The cracks start cycling... */
    public static final long WIND = 1100;
    static final double STEP_RATIO = 0.84;
    /** The shortest step: the shrinking would otherwise never reach the reveal (360 / (1 - 0.84) is 2250 ms). */
    static final long MIN_STEP = 120;
    /** ...each step shorter than the last... */
    public static final long[] STEPS = steps(WIND, 3600);
    /** ...into a flicker... */
    public static final long HANG = STEPS[STEPS.length - 1] + Math.max(MIN_STEP, Math.round(360 * Math.pow(STEP_RATIO, STEPS.length - 1)));
    /** ...and the answer. Nothing before this may depend on what is in the box. */
    public static final long REVEAL = 3600;
    /** A box that got away: no colours, a short grey wait. */
    public static final long MISSED_REVEAL = 1500;
    /** Cracks on the box, one per step; the rest come in the flicker. */
    public static final int CRACKS = 9;

    /** The Nadeshiko Dye: everything stops this long after the reveal... */
    public static final long FREEZE = REVEAL + 250;
    /** ...three heartbeats in the grey... */
    public static final long[] HEARTS = {FREEZE + 520, FREEZE + 1120, FREEZE + 1680};
    /** ...and the petals break it. */
    public static final long THAW = REVEAL + 2700;
    /** The ultra-rare book: the lid goes, the sides fall, the book rises, opens, riffles, and flashes. */
    public static final long BOOK_RISE = REVEAL + 350, BOOK_OPEN = REVEAL + 1000, RIFFLE = REVEAL + 1150,
            BOOK_FLASH = REVEAL + 2500;
    public static final long FADE = 400, BOX_FADE = 350, COUNT_MS = 900;

    private static long[] steps(long from, long reveal) {
        List<Long> out = new ArrayList<>();
        double t = from, d = 360;
        while (t + d < reveal - 200) {
            out.add(Math.round(t));
            t += d;
            d = Math.max(MIN_STEP, d * STEP_RATIO);
        }
        long[] a = new long[out.size()];
        for (int i = 0; i < a.length; i++) a[i] = out.get(i);
        return a;
    }

    private final List<Box> boxes;
    private final long[] starts;
    private final long seed;
    private final long[] prices;
    private final String[] priceSources;

    public PairsTimeline(List<Box> boxes, long seed) {
        this.boxes = List.copyOf(boxes);
        this.seed = seed;
        this.starts = new long[this.boxes.size() + 1];
        for (int i = 0; i < this.boxes.size(); i++) starts[i + 1] = starts[i] + end(this.boxes.get(i).ending());
        this.prices = new long[this.boxes.size()];
        this.priceSources = new String[this.boxes.size()];
        java.util.Arrays.fill(prices, -1);
    }

    /** A timeline of one box (tests, previews, Try it). */
    public static PairsTimeline one(PairsReward reward, boolean matched, long seed) {
        return new PairsTimeline(List.of(new Box('A', reward, matched)), seed);
    }

    public List<Box> boxes() { return boxes; }
    public long seed() { return seed; }
    public int count() { return boxes.size(); }

    public long total() {
        return starts[boxes.size()];
    }

    /** When box {@code i} starts. */
    public long start(int i) {
        return starts[i];
    }

    /** Which box {@code t} is in (the last one past the end). */
    public int index(long t) {
        for (int i = boxes.size() - 1; i > 0; i--) if (t >= starts[i]) return i;
        return 0;
    }

    public Box box(int i) {
        return boxes.get(i);
    }

    public void price(int i, long coins, String source) {
        prices[i] = coins;
        priceSources[i] = source;
    }

    public long price(int i) { return prices[i]; }
    public String priceSource(int i) { return priceSources[i]; }

    /** The best price among the matched boxes (for the Punch). */
    public long bestPrice() {
        long best = 0;
        for (int i = 0; i < boxes.size(); i++) if (boxes.get(i).matched()) best = Math.max(best, prices[i]);
        return best;
    }

    // ------------------------------------------------------------------ per box

    public static long reveal(Ending e) {
        return e == Ending.MISSED ? MISSED_REVEAL : REVEAL;
    }

    /** When the reward stands where it is shown, from the box's start. */
    public static long arrive(Ending e) {
        return switch (e) {
            case MISSED -> MISSED_REVEAL + 900;
            case RARE -> REVEAL + 1100;
            case ULTRA -> BOOK_FLASH + 350;
            case DYE -> THAW + 2000;
        };
    }

    /** How long a box takes, its showcase included. */
    public static long end(Ending e) {
        return switch (e) {
            case MISSED -> MISSED_REVEAL + 2700;
            case RARE -> arrive(e) + 2700;
            case ULTRA -> arrive(e) + 3500;
            case DYE -> arrive(e) + 4800;
        };
    }

    public static long countStart(Ending e) {
        return arrive(e) + 550;
    }

    /** The box flying in, 0..1 (overshooting). */
    public static float fly(long u) {
        return backOut(Math.min(1f, u / (float) IN));
    }

    /** Which colour step {@code u} is in, -1 before the first. */
    public static int step(long u) {
        int k = -1;
        for (int i = 0; i < STEPS.length; i++) if (u >= STEPS[i]) k = i;
        return k;
    }

    /** A flash on each step, 1 at the step, gone in ~100 ms. */
    public static float flash(long u) {
        int k = step(u);
        if (k < 0 || u >= HANG) return 0f;
        return (float) Math.exp(-(u - STEPS[k]) / 90.0);
    }

    /** The colour of the cracks now: the cycle, a flicker in the hang, the answer from the reveal. Black before. */
    public static int crackRgb(Ending e, long u) {
        if (e == Ending.MISSED) return u < WIND ? 0 : Ending.MISSED.tell;
        if (u >= REVEAL) return e.tell;
        if (u < WIND) return 0;
        if (u >= HANG) return CYCLE[(int) ((u / 50) % CYCLE.length)];
        return CYCLE[Math.max(0, step(u)) % CYCLE.length];
    }

    /** How many cracks are lit on the box: one a step, all of them in the hang (two for a box that got away). */
    public static int cracks(Ending e, long u) {
        if (e == Ending.MISSED) return u < WIND ? 0 : 2;
        if (u >= HANG) return CRACKS;
        return Math.min(CRACKS, step(u) + 1);
    }

    /** How hard the box trembles, 0..1 - rising through the build-up, stopped at the reveal. */
    public static float tremble(Ending e, long u) {
        long r = reveal(e);
        if (u < WIND || u >= r) return 0f;
        float x = (u - WIND) / (float) (r - WIND);
        return e == Ending.MISSED ? 0.15f * x : x * x;
    }

    /** The box's slow turn, degrees: the same for every box. */
    public static float yaw(long u) {
        return 28f * (float) Math.sin(u / 1300.0) - 12f * (1f - fly(u));
    }

    // ------------------------------------------------------------------ after the reveal

    /** The physical clock: the Dye's stops from FREEZE to THAW. Never before the reveal. */
    public static long phys(Ending e, long u) {
        if (e != Ending.DYE || u < FREEZE) return u;
        if (u < THAW) return FREEZE;
        return FREEZE + (u - THAW);
    }

    /** How grey the Dye's stopped world is, 0..1. */
    public static float frozen(Ending e, long u) {
        if (e != Ending.DYE || u < FREEZE) return 0f;
        if (u < THAW) return ease((u - FREEZE) / 250f);
        return 1f - ease((u - THAW) / 500f);
    }

    /** The heartbeat swell in the grey: 1 at a beat. */
    public static float heart(Ending e, long u) {
        if (e != Ending.DYE || u >= THAW) return 0f;
        float best = 0f;
        for (long h : HEARTS) if (u >= h) best = Math.max(best, (float) Math.exp(-(u - h) / 170.0));
        return best;
    }

    /** Seconds since the box burst (rare) or the petals broke out (Dye); -1 before or never. */
    public static float burst(Ending e, long u) {
        long at = switch (e) {
            case RARE -> REVEAL;
            case DYE -> THAW;
            default -> -1;
        };
        if (at < 0 || u < at) return -1f;
        return (u - at) / 1000f;
    }

    /** The lid lifting: an ultra-rare's blown off (seconds in flight), a missed box's raised a crack (radians). */
    public static float lid(Ending e, long u) {
        return switch (e) {
            case ULTRA -> u < REVEAL ? -1f : (u - REVEAL) / 1000f;
            case MISSED -> u < MISSED_REVEAL ? 0f : 0.32f * ease((u - MISSED_REVEAL - 100) / 700f);
            default -> 0f;
        };
    }

    /** An ultra-rare box's sides falling open, radians from upright (pi/2 flat). */
    public static float unfold(Ending e, long u) {
        if (e != Ending.ULTRA || u < REVEAL + 80) return 0f;
        float s = (u - REVEAL - 80) / 450f;
        if (s >= 1f) return (float) (Math.PI / 2) + 0.06f * (float) (Math.exp(-(s - 1f) * 4) * Math.sin((s - 1f) * 20));
        return (float) (Math.PI / 2) * s * s;
    }

    /** The book rising out of the open box, 0..1; 0 for any other ending. */
    public static float bookRise(Ending e, long u) {
        if (e != Ending.ULTRA) return 0f;
        return backOut(Math.min(1f, Math.max(0f, (u - BOOK_RISE) / 600f)));
    }

    /** The book's covers opening, 0 shut .. 1 flat open. */
    public static float bookOpen(Ending e, long u) {
        if (e != Ending.ULTRA) return 0f;
        return ease((u - BOOK_OPEN) / 380f);
    }

    /** Pages riffling: how many pages have turned (fractional), faster and faster. */
    public static float riffle(Ending e, long u) {
        if (e != Ending.ULTRA || u < RIFFLE) return 0f;
        float s = Math.min(BOOK_FLASH, u) - RIFFLE;
        return s / 140f + s * s / 90_000f;
    }

    /** The book's flash into the reward, 1 at the flash. */
    public static float bookFlash(Ending e, long u) {
        if (e != Ending.ULTRA || u < BOOK_FLASH) return 0f;
        return (float) Math.exp(-(u - BOOK_FLASH) / 160.0);
    }

    /** How much of the book is left, 1 .. 0 as it turns into the reward. */
    public static float bookLeft(Ending e, long u) {
        if (e != Ending.ULTRA) return 0f;
        return 1f - ease((u - BOOK_FLASH) / 180f);
    }

    /** The reward rising to its showcase, 0..1. */
    public static float itemRise(Ending e, long u) {
        long from = switch (e) {
            case MISSED -> MISSED_REVEAL + 250;
            case RARE -> REVEAL + 120;
            case ULTRA -> BOOK_FLASH;
            case DYE -> THAW + 900;
        };
        if (u < from) return 0f;
        return backOut(Math.min(1f, (u - from) / (float) (arrive(e) - from)));
    }

    public static float nameAlpha(Ending e, long u) {
        return ease((u - arrive(e) - 50) / 300f);
    }

    public static float lineAlpha(Ending e, long u) {
        return ease((u - arrive(e) - 400) / 300f);
    }

    /** Letters of the name stamped: the big two spell it out. */
    public static int letters(Ending e, long u) {
        if (!banner(e)) return 999;
        return Math.max(0, (int) ((u - arrive(e) - 50) / 45) + 1);
    }

    public static boolean banner(Ending e) {
        return e == Ending.ULTRA || e == Ending.DYE;
    }

    /** The word stamped over the big two. Hypixel's own for the dye (WOW!); for the books it is ours, not Hypixel's. */
    public static String bannerWord(Ending e) {
        return e == Ending.DYE ? "WOW!" : "ULTRA-RARE!";
    }

    public static int bannerRgb(Ending e) {
        return e == Ending.DYE ? 0xFF55FF : 0xC77DFF;
    }

    public static float bannerStamp(Ending e, long u) {
        if (!banner(e) || u < arrive(e) - 150) return 0f;
        return Math.min(1f, (u - arrive(e) + 150) / 180f);
    }

    /** The box's showcase going before the next box comes in (the last box goes with everything). */
    public float boxAlpha(int i, long u) {
        if (i == boxes.size() - 1) return 1f;
        long end = end(boxes.get(i).ending());
        return 1f - ease((u - (end - BOX_FADE)) / (float) BOX_FADE);
    }

    /** Everything fades in and out with this. */
    public float master(long t) {
        return ease(t / 200f) * (1f - ease((t - (total() - FADE)) / (float) FADE));
    }

    /** How dark the game behind is. */
    public float dim(long t) {
        return 0.78f * ease(t / 250f) * (1f - ease((t - (total() - FADE)) / (float) FADE));
    }

    /** The shake, per screen height: the trembling box, the burst, the petals breaking out. */
    public static float shake(Ending e, long u) {
        float s = 0.0035f * tremble(e, u);
        float b = burst(e, u);
        if (b >= 0f && b < 0.45f) s += (e == Ending.DYE ? 0.017f : 0.01f) * (1f - b / 0.45f);
        if (e == Ending.ULTRA && u >= REVEAL && u < REVEAL + 350) s += 0.009f * (1f - (u - REVEAL) / 350f);
        s += 0.004f * heart(e, u);
        return s;
    }

    // ------------------------------------------------------------------ easing

    public static float ease(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    /** Exactly 0 at and before 0. */
    public static float backOut(float t) {
        if (t <= 0f) return 0f;
        t = Math.min(1f, t);
        float c = 1.6f, v = t - 1f;
        return 1f + (c + 1f) * v * v * v + c * v * v;
    }

    /** The test rewards by word: fish, serum, guardian, book (Sharpness VII), dye. */
    public static PairsReward testReward(String word) {
        return switch (word.toLowerCase(java.util.Locale.ROOT)) {
            case "fish" -> PairsReward.of("Experiment the Fish", "minecraft:pufferfish", "EXPERIMENT_THE_FISH", null, null, null, List.of());
            case "serum" -> PairsReward.of("Metaphysical Serum", "minecraft:player_head", "METAPHYSICAL_SERUM", null, null, null, List.of());
            case "guardian" -> PairsReward.of("[Lvl 1] Guardian", "minecraft:player_head", "GUARDIAN;4", null, "GUARDIAN", "LEGENDARY", List.of());
            case "dye" -> PairsReward.of("Nadeshiko Dye", "minecraft:player_head", "DYE_NADESHIKO", null, null, null, List.of());
            default -> PairsReward.of("Enchanted Book", "minecraft:enchanted_book", "ENCHANTED_BOOK",
                    java.util.Map.of("sharpness", 7), null, null, List.of("Sharpness VII"));
        };
    }

    /** Every word {@link #testReward} takes, and "missed" (a book that got away), "two" (a serum then a book). */
    public static final List<String> TEST_WORDS = List.of("fish", "serum", "guardian", "book", "dye", "missed", "two");

    /** A test reveal by its word. */
    public static PairsTimeline test(String word, long seed) {
        return switch (word.toLowerCase(java.util.Locale.ROOT)) {
            case "missed" -> one(testReward("book"), false, seed);
            case "two" -> new PairsTimeline(List.of(new Box('A', testReward("serum"), true), new Box('B', testReward("book"), true)), seed);
            default -> one(testReward(word), true, seed);
        };
    }

    /** Whether the Try it word ends in something found (a box that got away is not). */
    public static boolean testWins(String word) {
        return !word.equalsIgnoreCase("missed");
    }
}
