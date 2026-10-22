package com.sbcs.anim;

import com.sbcs.detect.nucleus.NucleusHaul;
import com.sbcs.detect.nucleus.NucleusLoot;
import com.sbcs.detect.nucleus.NucleusLoot.Tier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * "The Vault" - the Crystal Nucleus loot bundle's animation. Pure: everything here is a function of time, the
 * prize and a seed, so it can be checked offline.
 *
 * A steel safe door with a dial and five empty sockets. The five crystals the player just placed fly in one at a
 * time and click into their sockets, each pulling a bolt back; the dial spins, flashing through the five colours
 * faster and faster while the door strains; at {@link #REVEAL} the dial lands on one colour - the answer. <b>Nothing
 * before {@link #REVEAL} depends on the prize</b> (tools/VaultTest hashes every frame and every sound). Then:
 * <ul>
 *   <li>dead grey - a bust: the door creaks open a crack on an empty vault, dust puffs out;</li>
 *   <li>sapphire - a rare roll (Quick Claw, a second Fine Jasper): the door swings wide, light pours out, the drop
 *   floats out;</li>
 *   <li>amethyst - a jackpot (a Flawless gemstone): the door is blown off its hinges, rays in the gem's colour;</li>
 *   <li>gold - Divan's Alloy, "the Meltdown": the crystals pour their light into the dial, the door heats to white
 *   gold and melts down into a molten pool, and the alloy rises out of it (the designer: the Alloy and the Dye are
 *   the best drops and get the most climactic endings);</li>
 *   <li>jade - the Jade Dye: it freezes as it cracks open, the world greys, three heartbeats, then jade floods out and
 *   the door is blown away.</li>
 * </ul>
 * The black box in the world (BundleCover) keeps the real bundle hidden until it is picked up, so the floor does not
 * give this away.
 */
public final class VaultTimeline {

    /** How it ends - the haul's best tier. */
    public enum Ending {
        BUST(0x4A4A58), OPEN(0x55D8FF), BLOWN(0xD066FF), ALLOY(0xFFAA00), DYE(0x55FF55);

        /** The dial's colour at the reveal: the tell. */
        public final int tell;

        Ending(int tell) {
            this.tell = tell;
        }

        public boolean win() {
            return this != BUST;
        }
    }

    /**
     * What the vault shows. {@code art}: "head:&lt;NEU id&gt;" (HeadSkins.NUCLEUS), "gem:&lt;colour&gt;" (a
     * Flawless gemstone), "fine:jasper", "alloy", or null.
     */
    public record Prize(String title, int colour, String id, Ending ending, int light, long oneIn, String art, int amount) {
        public static final Prize BUST = new Prize("", 0xFFFFFF, null, Ending.BUST, Ending.BUST.tell, 0, null, 0);

        public boolean win() {
            return ending.win();
        }

        /** The haul's best drop, as the vault shows it. */
        public static Prize of(NucleusHaul haul) {
            if (haul == null || haul.bust()) return BUST;
            NucleusHaul.Drop d = haul.headline();
            // Divan's Alloy is the best thing in the table after the Dye (the designer), whatever its weight says:
            // with a Flawless gem beside it, the Alloy is what the vault shows.
            if (haul.tierOf(d) == Tier.JACKPOT) {
                for (NucleusHaul.Drop o : haul.drops()) {
                    if (o.name().equals("Divan's Alloy")) return of(o.entry(), Tier.JACKPOT, o.amount());
                }
            }
            return of(d.entry(), haul.tierOf(d), d.amount());
        }

        static Prize of(NucleusLoot.Entry e, Tier tier, int amount) {
            Ending ending = switch (tier) {
                case RARE -> Ending.OPEN;
                case JACKPOT -> Ending.BLOWN;
                case DYE -> Ending.DYE;
                default -> Ending.BUST;
            };
            if (ending == Ending.BUST) return BUST;
            String name = e.name(), art = null;
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.equals("divan's alloy")) ending = Ending.ALLOY;
            int light = ending.tell;
            if (lower.startsWith("flawless ")) {
                String colour = lower.substring("flawless ".length()).replace(" gemstone", "").trim();
                art = "gem:" + colour;
                light = gemRgb(colour);
            } else if (lower.equals("fine jasper gemstone")) {
                art = "fine:jasper";
            } else if (lower.equals("divan's alloy")) {
                art = "alloy";
            } else if (e.id() != null) {
                art = "head:" + e.id();
            }
            double p = NucleusLoot.perBundle(e, NucleusLoot.AVG_ROLLS);
            long oneIn = p <= 0 ? 0 : Math.round(1 / p);
            return new Prize(name, e.colour() & 0xFFFFFF, e.id(), ending, light, oneIn, art, amount);
        }

        /** A test prize by its word: bust, claw, jasper, alloy, dye, or a Flawless gemstone's colour. */
        public static Prize test(String word) {
            String w = word.toLowerCase(Locale.ROOT);
            return switch (w) {
                case "bust" -> BUST;
                case "claw" -> of(NucleusLoot.find("Quick Claw"), Tier.RARE, 1);
                case "jasper" -> of(NucleusLoot.find("Fine Jasper Gemstone"), Tier.RARE, 2);
                case "alloy" -> of(NucleusLoot.find("Divan's Alloy"), Tier.JACKPOT, 1);
                case "dye" -> of(NucleusLoot.find("Jade Dye"), Tier.DYE, 1);
                default -> {
                    NucleusLoot.Entry e = NucleusLoot.find("Flawless " + w + " Gemstone");
                    yield e == null ? BUST : of(e, Tier.JACKPOT, 1);
                }
            };
        }

        /** Every word {@link #test} takes. */
        public static List<String> testWords() {
            List<String> out = new ArrayList<>(List.of("bust", "claw", "jasper", "alloy", "dye"));
            for (String c : GEM_NAMES) out.add(c);
            return out;
        }
    }

    static final String[] GEM_NAMES = {"ruby", "amethyst", "jade", "amber", "sapphire", "topaz", "jasper"};
    private static final int[] GEM_RGB = {0xFF5555, 0xD066FF, 0x55FF55, 0xFFAA00, 0x55FFFF, 0xFFFF55, 0xFF55FF};

    /** A gemstone's colour by its name, as the Hollows show it. */
    public static int gemRgb(String colour) {
        for (int i = 0; i < GEM_NAMES.length; i++) if (GEM_NAMES[i].equals(colour)) return GEM_RGB[i];
        return 0xFFFFFF;
    }

    /** The five crystals that open the Nucleus, in the order they go in - and their colours. */
    public static final String[] CRYSTALS = {"Jade", "Amber", "Topaz", "Sapphire", "Amethyst"};
    public static final int[] CRYSTAL_RGB = {0x55FF55, 0xFFAA00, 0xFFFF55, 0x55FFFF, 0xD066FF};

    // ------------------------------------------------------------------ the beats, ms

    /** The door slides in. */
    public static final long DOOR_IN = 450;
    /** Each crystal clicks into its socket at these times... */
    public static final long[] SOCKETS = {800, 1350, 1900, 2450, 3000};
    /** ...after flying in for this long... */
    public static final long FLY_MS = 380;
    /** ...and a bolt slides back this long after the click, taking {@link #BOLT_MS}. */
    public static final long BOLT_DELAY = 140, BOLT_MS = 180;
    /** The dial starts spinning. */
    public static final long WIND = 3400;
    /** Each colour step of the wind-up is this much of the one before. */
    static final double STEP_RATIO = 0.82;
    /** The colour steps of the wind-up: from 300 ms, each 18% shorter. */
    public static final long[] STEPS = steps();
    /** The last step's end: a flicker through all five to the reveal. */
    public static final long HANG = hangEnd();
    /** The answer. Nothing before this may depend on the prize. */
    public static final long REVEAL = 4800;

    private static long hangEnd() {
        return STEPS[STEPS.length - 1] + Math.round(300 * Math.pow(STEP_RATIO, STEPS.length - 1));
    }

    private static long[] steps() {
        List<Long> out = new ArrayList<>();
        double t = WIND, d = 300;
        while (t + d < 4800 - 160) {
            out.add(Math.round(t));
            t += d;
            d *= STEP_RATIO;
        }
        long[] a = new long[out.size()];
        for (int i = 0; i < a.length; i++) a[i] = out.get(i);
        return a;
    }

    /** A bust is over by this - short, so the player is back to the next run. */
    public static final long BUST_END = REVEAL + 1900;
    /**
     * A win's held breath: the dial lands with a clunk and nothing moves for this long (Matej, 2026-10-22: "hold
     * some more pause for the good drops"). After the reveal, so it tells nothing the dial has not already said.
     */
    public static final long HOLD = 450;
    /** A rare roll: the door unlatches a crack at the end of the hold, light round its edge, then swings here. */
    public static final long SWING = REVEAL + HOLD + 650;
    /** A Flawless gem: something inside knocks the door three times, harder each time, then it is blown off. */
    public static final long[] KNOCKS = {REVEAL + HOLD, REVEAL + HOLD + 420, REVEAL + HOLD + 760};
    public static final long BLOW = REVEAL + HOLD + 1150;
    /** The Jade Dye: everything stops this long after the reveal... */
    public static final long FREEZE = REVEAL + 260;
    /** ...and the jade breaks it here, after four heartbeats, the last one followed by nothing at all. */
    public static final long THAW = REVEAL + 3500;
    public static final long[] HEARTS = {FREEZE + 500, FREEZE + 1150, FREEZE + 1750, FREEZE + 2350};
    /** Divan's Alloy: each crystal pours its light into the dial at these times... */
    public static final long[] POURS = {REVEAL + HOLD, REVEAL + HOLD + 420, REVEAL + HOLD + 840, REVEAL + HOLD + 1260,
            REVEAL + HOLD + 1680};
    /** ...the door heats from steel to white gold, holds there white hot, and melts down here... */
    public static final long MELT = REVEAL + HOLD + 3200;
    /** ...and is a pool of molten gold by this. */
    public static final long POOLED = MELT + 900;
    public static final long FADE = 400;
    public static final long COUNT_MS = 1000;

    private final Prize prize;
    private final long seed;

    public VaultTimeline(Prize prize, long seed) {
        this.prize = prize == null ? Prize.BUST : prize;
        this.seed = seed;
    }

    public Prize prize() { return prize; }
    public Ending ending() { return prize.ending; }
    public long seed() { return seed; }

    public long total() {
        return switch (prize.ending) {
            case BUST -> BUST_END;
            case OPEN -> arrive() + 3400;
            case BLOWN -> arrive() + 4300;
            case ALLOY -> arrive() + 5200;
            case DYE -> arrive() + 5200;
        };
    }

    /** When the drop stands where it is shown. */
    public long arrive() {
        return switch (prize.ending) {
            case BUST -> Long.MAX_VALUE / 4;
            case OPEN -> SWING + 1300;
            case BLOWN -> BLOW + 1400;
            case ALLOY -> MELT + 1700;
            case DYE -> THAW + 1500;
        };
    }

    /** When the door is blown off: the jackpot's and the dye's; never for the others. */
    public long blowAt() {
        return switch (prize.ending) {
            case BLOWN -> BLOW;
            case DYE -> THAW;
            default -> Long.MAX_VALUE / 4;
        };
    }

    /** The clock physical things run on: the Jade Dye's stops from FREEZE to THAW. Never before the reveal. */
    public long phys(long t) {
        if (prize.ending != Ending.DYE || t < FREEZE) return t;
        if (t < THAW) return FREEZE;
        return FREEZE + (t - THAW);
    }

    /** How frozen (grey) the Jade Dye's world is, 0..1. */
    public float frozen(long t) {
        if (prize.ending != Ending.DYE || t < FREEZE) return 0f;
        if (t < THAW) return ease((t - FREEZE) / 250f);
        return 1f - ease((t - THAW) / 500f);
    }

    /** The heartbeat swell in the frozen world: 1 at a beat, gone in ~250 ms. */
    public float heart(long t) {
        if (prize.ending != Ending.DYE) return 0f;
        float best = 0f;
        for (long h : HEARTS) if (t >= h) best = Math.max(best, (float) Math.exp(-(t - h) / 160.0));
        return t >= THAW ? 0f : best;
    }

    // ------------------------------------------------------------------ before the reveal: the same for every prize

    /** The door coming in, 0..1 (overshooting). */
    public static float doorIn(long t) {
        return backOut(Math.min(1f, t / (float) DOOR_IN));
    }

    /** Crystal {@code k} flying in: 0 at the start of its flight, 1 in its socket; -1 before it sets off. */
    public static float fly(int k, long t) {
        long from = SOCKETS[k] - FLY_MS;
        if (t < from) return -1f;
        return Math.min(1f, (t - from) / (float) FLY_MS);
    }

    /** Crystal {@code k}'s click flash: 1 as it seats, gone in ~200 ms. */
    public static float click(int k, long t) {
        if (t < SOCKETS[k]) return 0f;
        return (float) Math.exp(-(t - SOCKETS[k]) / 110.0);
    }

    /** Bolt {@code k} pulled back, 0 (locked) .. 1 (open). */
    public static float bolt(int k, long t) {
        long from = SOCKETS[k] + BOLT_DELAY;
        return ease((t - from) / (float) BOLT_MS);
    }

    /** Which wind-up step {@code t} is in, -1 before the first. */
    public static int step(long t) {
        int k = -1;
        for (int i = 0; i < STEPS.length; i++) if (t >= STEPS[i]) k = i;
        return k;
    }

    /**
     * The dial's light: the crystal colours in turn through the wind-up, a flicker in the hang, then the answer. The
     * index into {@link #CRYSTALS}, or -1 for none (before the wind-up).
     */
    public static int cycle(long t) {
        if (t < WIND) return -1;
        if (t >= HANG) return (int) ((t / 45) % 5);
        return Math.max(0, step(t)) % 5;
    }

    /** The colour on the dial now; the tell from the reveal on. */
    public int dialRgb(long t) {
        if (t >= REVEAL) return prize.ending.tell;
        int k = cycle(t);
        return k < 0 ? 0x000000 : CRYSTAL_RGB[k];
    }

    /** A flash on each colour step, 1 at the step. */
    public static float flash(long t) {
        int k = step(t);
        if (k < 0 || t >= HANG) return 0f;
        return (float) Math.exp(-(t - STEPS[k]) / 90.0);
    }

    /** How hard the door strains, 0..1 - rising through the wind-up, stopped at the reveal. */
    public static float strain(long t) {
        if (t < WIND || t >= REVEAL) return 0f;
        float x = (t - WIND) / (float) (REVEAL - WIND);
        return x * x;
    }

    /**
     * The dial's angle, radians: a notch (a fifth of a turn) per crystal as it seats, then spinning faster and faster
     * through the wind-up, stopped dead at the reveal with a little spring. The same for every prize.
     */
    public static float dial(long t) {
        float a = 0f;
        for (int k = 0; k < SOCKETS.length; k++) a += ease((t - SOCKETS[k] - 60) / 200f);
        a *= (float) (Math.PI * 2 / 5);
        if (t > WIND) {
            float r = REVEAL - WIND;
            float x = Math.min(1f, (t - WIND) / r);
            // The integral of 2 + 26 x^2 rad/s.
            a += (2f * x + 26f / 3f * x * x * x) * r / 1000f;
        }
        if (t > REVEAL) {
            float s = (t - REVEAL) / 1000f;
            a += 0.12f * (float) (Math.exp(-s * 6) * Math.sin(s * 30));
        }
        return a;
    }

    // ------------------------------------------------------------------ after the reveal

    /** How far the door has swung open, radians, about its hinge (0 shut). */
    public float doorAngle(long t) {
        if (t < REVEAL) return 0f;
        long p = phys(t);
        return switch (prize.ending) {
            // A creak open a crack, and a settle back a little.
            case BUST -> 0.24f * ease((p - REVEAL - 150) / 950f) - 0.03f * ease((p - REVEAL - 1100) / 300f);
            // Unlatched a crack, held there with light round its edge, then swung wide.
            case OPEN -> 0.07f * ease((p - REVEAL - HOLD) / 220f) + 1.68f * backOut((p - SWING) / 820f);
            // Pushed out further on every knock; blown off after (doorFlight).
            case BLOWN -> 0.025f * knocked(t) + 0.03f * knock(t);
            case ALLOY -> 0f;
            case DYE -> 0.11f * ease((p - REVEAL) / 260f);
        };
    }

    /** How many of a Flawless gem's knocks have landed, 0..3. */
    public int knocked(long t) {
        if (prize.ending != Ending.BLOWN) return 0;
        int n = 0;
        for (long k : KNOCKS) if (t >= k) n++;
        return n;
    }

    /** A knock's jolt, 1 as it lands and gone in ~150 ms, larger each time; 0 for any other ending. */
    public float knock(long t) {
        if (prize.ending != Ending.BLOWN) return 0f;
        float best = 0f;
        for (int i = 0; i < KNOCKS.length; i++) {
            if (t >= KNOCKS[i]) best = Math.max(best, (0.6f + 0.2f * i) * (float) Math.exp(-(t - KNOCKS[i]) / 110.0));
        }
        return t >= BLOW ? 0f : best;
    }

    /** Divan's Alloy: crystal {@code k}'s light poured into the dial, 0..1 (the crystal drained as it goes). */
    public float poured(int k, long t) {
        if (prize.ending != Ending.ALLOY) return 0f;
        return ease((t - POURS[k]) / 240f);
    }

    /** The door's heat, 0 (steel) .. 1 (white gold), Divan's Alloy only. */
    public float heat(long t) {
        if (prize.ending != Ending.ALLOY || t < POURS[0]) return 0f;
        // White hot by the last pour's end; held there until the melt.
        float x = (t - POURS[0]) / (float) (POURS[4] + 400 - POURS[0]);
        return Math.min(1f, x * x * 0.85f + 0.15f * x);
    }

    /** The door melting down into its pool, 0..1, Divan's Alloy only. */
    public float melt(long t) {
        if (prize.ending != Ending.ALLOY || t < MELT) return 0f;
        return ease((t - MELT) / (float) (POOLED - MELT));
    }

    /** Seconds since the door was blown off, on the physical clock; -1 before (or never). */
    public float doorFlight(long t) {
        long b = blowAt();
        if (t < b) return -1f;
        return (phys(t) - phys(b)) / 1000f;
    }

    /** The light from inside the vault, 0..1: a bust's faint, a win's pouring out. */
    public float inner(long t) {
        if (t < REVEAL) return 0f;
        long p = phys(t);
        float k = ease((p - REVEAL) / 400f);
        return switch (prize.ending) {
            case BUST -> 0.12f * k;
            case OPEN -> 0.35f * ease((p - REVEAL - HOLD) / 220f) + 0.65f * ease((p - SWING) / 400f);
            case BLOWN -> t < BLOW ? 0.12f * knocked(t) + 0.3f * knock(t) : ease((p - BLOW + 60) / 200f);
            case ALLOY -> 0.6f * heat(t) + 0.4f * melt(t);
            case DYE -> t < THAW ? 0.35f + 0.4f * heart(t) : 1f;
        };
    }

    /** The drop rising out of the vault to its showcase, 0..1. */
    public float itemRise(long t) {
        if (!prize.win()) return 0f;
        long from = switch (prize.ending) {
            case DYE -> THAW + 150;
            case ALLOY -> MELT + 500;
            case BLOWN -> BLOW + 150;
            default -> SWING + 200;
        };
        if (t < from) return 0f;
        return backOut(Math.min(1f, (t - from) / (float) (arrive() - from)));
    }

    public float nameAlpha(long t) {
        return prize.win() ? ease((t - arrive() - 50) / 300f) : 0f;
    }

    public float lineAlpha(long t) {
        return prize.win() ? ease((t - arrive() - 400) / 300f) : 0f;
    }

    /** The bust's words: "No rare roll" and the dry streak. */
    public float bustText(long t) {
        if (prize.win() || t < REVEAL + 450) return 0f;
        return ease((t - REVEAL - 450) / 300f);
    }

    /** How many letters of the name are stamped: the jackpots spell it out, the rare drop shows it whole. */
    public int letters(long t) {
        if (!banner()) return 999;
        return Math.max(0, (int) ((t - arrive() - 50) / 45) + 1);
    }

    /** "RARE DROP!" (the jackpot) or "WOW!" (the dye), stamped above the drop. */
    public boolean banner() {
        return prize.ending == Ending.BLOWN || prize.ending == Ending.ALLOY || prize.ending == Ending.DYE;
    }

    public String bannerWord() {
        return prize.ending == Ending.DYE ? "WOW!" : "RARE DROP!";
    }

    public int bannerRgb() {
        return prize.ending == Ending.DYE ? 0xFF55FF : 0xFFAA00;
    }

    public float bannerStamp(long t) {
        if (!banner() || t < arrive() - 150) return 0f;
        return Math.min(1f, (t - arrive() + 150) / 180f);
    }

    public long countStart() {
        return arrive() + 650;
    }

    private long price = -1;
    private String priceSource;

    public long price() { return price; }

    public void price(long coins, String source) {
        this.price = coins;
        this.priceSource = source;
    }

    public String priceSource() { return priceSource; }

    /** Everything fades in and out with this. */
    public float master(long t) {
        return ease(t / 200f) * (1f - ease((t - (total() - FADE)) / (float) FADE));
    }

    /** How dark the game behind is. */
    public float dim(long t) {
        return 0.8f * ease(t / 250f) * (1f - ease((t - (total() - FADE)) / (float) FADE));
    }

    /** The screen shake, GUI px per screen height: the bolts giving, the door blowing, the jade breaking out. */
    public float shake(long t) {
        float s = 0.004f * strain(t);
        long b = blowAt();
        if (t >= b && t < b + 450) s += 0.016f * (1f - (t - b) / 450f);
        s += 0.007f * knock(t);
        if (prize.ending == Ending.ALLOY) {
            // The white-hot hold trembles harder the longer it lasts.
            float held = t > POURS[4] + 400 && t < MELT ? (t - POURS[4] - 400) / (float) (MELT - POURS[4] - 400) : 0f;
            s += (0.005f * heat(t) + 0.006f * held) * (t < MELT ? 1f : 0f);
            if (t >= MELT && t < MELT + 600) s += 0.018f * (1f - (t - MELT) / 600f);
        }
        return s;
    }

    // ------------------------------------------------------------------ easing

    public static float ease(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    /** Exactly 0 at and before 0 - every beat is tested with {@code > 0}. */
    public static float backOut(float t) {
        if (t <= 0f) return 0f;
        t = Math.min(1f, t);
        float c = 1.6f, u = t - 1f;
        return 1f + (c + 1f) * u * u * u + c * u * u;
    }
}
