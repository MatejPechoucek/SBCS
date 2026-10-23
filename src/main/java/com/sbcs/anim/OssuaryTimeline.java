package com.sbcs.anim;

import com.sbcs.detect.dungeon.ChestTier;
import com.sbcs.detect.dungeon.ChestValue;
import com.sbcs.detect.dungeon.DungeonFloor;
import com.sbcs.detect.dungeon.Loot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * "The Ossuary" - a dungeon reward chest's roll. Pure: no Minecraft. Unlike every other SBCS timeline this one is
 * played, not only watched, so it is an event log rather than a fixed schedule: the scene asks it where things are at
 * scene time {@code t}, the host feeds it what the player did (a seal clicked, the chain dragged, a lever thrown, a
 * Livid picked), and {@link #update} fires the automatic version of each after its wait.
 *
 * <h2>The wheel (2026-10-23)</h2>
 * One big prize wheel on every floor (Matej's friends: "the whole 4 levers is not it, one big wheel that looks super
 * cool"). It replaced a drum of niches, and on F7/M7 the four lords' drums. The wheel has {@link #SEGMENTS} segments:
 * {@link #VEILS} of them are veiled (a cobweb over a mystery), the rest hold filler from the floor's pool, fixed per
 * segment by the seed, so the wheel looks the same all the way round each turn. The spin is "drum" 0.
 *
 * <h2>Honest</h2>
 * Nothing before {@link #reveal()} depends on what the chest holds. The wheel always stops with a veiled segment under
 * the pointer - which of the veiled ones depends on the pull alone; the bulbs (M7: the five dragon gems) cycle the same
 * colours every time and land on the truth at the reveal. <b>The pull never changes the result</b>: whatever the
 * player does only changes how long and how wild the spin is - the stop is solved backwards from the speed so the
 * wheel stops exactly on a veiled segment.
 *
 * <h2>Shape</h2>
 * Arrive ({@link #ARRIVE}); on Master Mode the seals ({@code floor} of them, the last of M7's three clicks); the spin
 * (chain mode: the player drags the wheel round, a skeleton hand yanks it after {@link #AUTO_WAIT}; watch mode: the
 * hand yanks it at once); on F5/M5 the Livid shell game; the tell ({@link #TELL_MS}); the reveal and its ending;
 * the whole chest laid out with its profit; the fade back to the real menu.
 */
public final class OssuaryTimeline {

    // ------------------------------------------------------------------ fixed beats

    public static final long ARRIVE = 700;
    /** Chain mode: how long the chain waits for the player before the skeleton hand pulls it. */
    public static final long AUTO_WAIT = 2000;
    /** Watch mode: the hand pulls this long after everything is ready. */
    public static final long WATCH_WAIT = 380;
    /** The hand's stroke, down then up; the pull lands at its bottom. */
    public static final long HAND_STROKE = 420;
    /** Watch mode: one seal cracks this often. */
    public static final long SEAL_STEP = 240;
    /** How long a cracked seal takes to fall away; the next interaction unlocks after the last. */
    public static final long SEAL_FALL = 260;
    /** The lantern's (the dragons') colour cycle, identical every run. */
    public static final long TELL_MS = 2300;
    /** The F5 shell game: the Livids line up, then wait for a pick (watch mode: this long). */
    public static final long LIVID_LINE = 520, LIVID_WATCH = 900, LIVID_AFTER = 650;
    public static final long FADE_MS = 520;
    /** Spin length bounds, ms: a weak pull still spins a while, a wild one does not go on forever. */
    static final float SPIN_MIN = 2.1f, SPIN_MAX = 4.6f;
    /** The default pull - the hand's, and watch mode's. */
    public static final float DEFAULT_PULL = 0.5f;
    /** Re-pulls a drum takes while it spins. */
    public static final int MAX_PULLS = 3;

    // ------------------------------------------------------------------ bands and endings

    /** What the tell lands on: by worth, not rarity - a Legendary Ultimate Wise is cheap. */
    public enum Band {
        BUST(0x8E8E8E), BLUE(0x4FA8FF), PURPLE(0xB45CFF), GOLD(0xFFB52E), JACKPOT(0);

        public final int rgb;

        Band(int rgb) {
            this.rgb = rgb;
        }
    }

    public enum Ending { BUST, WIN, JACKPOT, DYE }

    /** Bands by the best item's worth. BLUE from the floor ({@code minWorth}, at least 1M), PURPLE 5M, GOLD 25M. */
    public static final long PURPLE_AT = 5_000_000, GOLD_AT = 25_000_000, JACKPOT_AT = 150_000_000;

    /** The floors' dealers: each floor's boss deals the hand. */
    public enum Dealer {
        BONZO("Bonzo", 0xFF6FA8), SCARF("Scarf", 0xE0303A), PROFESSOR("The Professor", 0x3BD1C2),
        THORN("Thorn", 0xE8F4FF), LIVID("Livid", 0x46D26A), SADAN("Sadan", 0xD0703C), NECRON("Necron", 0xA23BFF);

        public final String title;
        /** The floor's signature colour: its jackpot's. */
        public final int rgb;

        Dealer(String title, int rgb) {
            this.title = title;
            this.rgb = rgb;
        }

        public static Dealer of(DungeonFloor f) {
            int n = f == null ? 7 : Math.max(1, f.number());
            return values()[Math.min(6, n - 1)];
        }
    }

    /** The wheel's segments, and which of them are veiled - the stop is always one of these. */
    public static final int SEGMENTS = 16;
    public static final int[] VEILS = {0, 6, 11};

    /** Whether niche (segment index) {@code i} is veiled. */
    public static boolean veiled(long i) {
        int k = (int) Math.floorMod(i, (long) SEGMENTS);
        for (int v : VEILS) if (v == k) return true;
        return false;
    }

    /** M7's five dragons, the order their statues light: Power, Flame, Ice, Soul, Apex. */
    public static final int[] DRAGONS = {0xE33A3A, 0xFF8A1F, 0x5CC8FF, 0xA040FF, 0x49E36B};
    public static final String[] DRAGON_NAMES = {"Power", "Flame", "Ice", "Soul", "Apex"};

    /** Each floor's jackpot items (the plan's), by price key. Normal and Master Mode share them; M7 adds its own. */
    public static final List<Set<String>> JACKPOTS = List.of(
            Set.of("BONZO_STAFF"),
            Set.of("RED_SCARF"),
            Set.of("ADAPTIVE_CHESTPLATE", "ADAPTIVE_LEGGINGS"),
            Set.of("SPIRIT_SWORD", "ITEM_SPIRIT_BOW", "THORNS_BOOTS", "PET_SPIRIT?Rarity=LEGENDARY"),
            Set.of("SHADOW_FURY", "LAST_BREATH", "SHADOW_ASSASSIN_CHESTPLATE", "DYE_LIVID"),
            Set.of("GIANTS_SWORD", "PRECURSOR_EYE", "NECROMANCER_LORD_CHESTPLATE", "SUMMONING_RING"),
            Set.of("NECRON_HANDLE", "MAXOR_THE_FISH", "STORM_THE_FISH", "GOLDOR_THE_FISH", "SHADOW_WARP_SCROLL",
                    "WITHER_SHIELD_SCROLL", "IMPLOSION_SCROLL", "AUTO_RECOMBOBULATOR", "WITHER_CHESTPLATE",
                    "DARK_CLAYMORE", "DYE_NECRON", "FIFTH_MASTER_STAR", "MASTER_SKULL_TIER_5", "ENCHANTMENT_THUNDERLORD_7"));
    /** The dyes - every animation's dye gets its grandest ending. */
    public static final Set<String> DYES = Set.of("DYE_LIVID", "DYE_NECRON");

    public static int dyeRgb(String key) {
        return "DYE_NECRON".equals(key) ? 0xE7413C : 0xCEB7AA;
    }

    // ------------------------------------------------------------------ setup (never the outcome)

    private final DungeonFloor floor;
    private final ChestTier tier;
    private final boolean red, chain, throne, dragons, livid;
    private final long seed;
    private final int seals;
    private final List<Loot> filler;
    private final int[] fillerWeights;
    private final int fillerTotal;

    // ------------------------------------------------------------------ the event log

    private final long[] sealAt;
    private final int[] sealHits;
    private long unlockAt = -1;

    private final Drum[] drums;
    private long lastInputAt;
    private long liviLineAt = -1, liviPickAt = -1;
    private int liviPick = -1;
    private long tellAt = -1;
    /**
     * Chain mode: once the hand has had to act for an idle player, it carries on at watch pace - seven seals and
     * four levers at two seconds each would be half a minute. Any input from the player hands control back.
     */
    private boolean handTook;

    // ------------------------------------------------------------------ the outcome

    /** The priced chest; may be replaced until it locks ({@link #locked}), as late prices arrive. */
    private ChestValue value;
    private boolean locked;
    private Band band = Band.BUST;
    private Ending ending = Ending.BUST;
    private int jackpotRgb;
    private int dragon = -1;
    private long floorCoins;
    private List<Integer> ranked = List.of();

    public static final class Drum {
        /** Every pull: {time, strength x1000}. Read only. */
        public final List<long[]> pulls = new ArrayList<>();
        /** The current segment: start time, start position (niches), speed (niches/s), stop time, stop index. */
        public long t0 = -1, stopAt = -1;
        float p0, v0;
        public long target = -1;
        public boolean big;
    }

    /**
     * @param chain   the menu's "Pull the chain" (else "Just watch")
     * @param red     Master Mode's red chest (the floor is Master Mode and the setting is on)
     * @param filler  the floor's pool, what goes past; {@code weights} its weights
     */
    public OssuaryTimeline(DungeonFloor floor, ChestTier tier, boolean red, boolean chain, long seed,
                           List<Loot> filler, int[] weights) {
        this.floor = floor == null ? DungeonFloor.UNKNOWN : floor;
        this.tier = tier;
        this.red = red;
        this.chain = chain;
        this.seed = seed;
        this.throne = this.floor.number() == 7;
        this.dragons = throne && this.floor.master();
        this.livid = this.floor.number() == 5;
        this.seals = red ? Math.max(1, this.floor.number()) : 0;
        this.sealAt = new long[seals];
        this.sealHits = new int[seals];
        java.util.Arrays.fill(sealAt, -1);
        this.filler = filler == null || filler.isEmpty() ? List.of(Loot.named(null, "?", 1)) : filler;
        this.fillerWeights = new int[this.filler.size()];
        int sum = 0;
        for (int i = 0; i < this.filler.size(); i++) {
            int w = weights != null && i < weights.length ? Math.max(1, weights[i]) : 1;
            fillerWeights[i] = w;
            sum += w;
        }
        this.fillerTotal = sum;
        this.drums = new Drum[]{new Drum()};
        drums[0].big = true;
        if (seals == 0) unlockAt = ARRIVE;
    }

    // ------------------------------------------------------------------ the outcome

    /**
     * The chest, priced. Called by the host with the latest prices until it locks at the tell. {@code floorCoins} is
     * the menu's minimum worth.
     */
    public void resolve(ChestValue v, long floorCoins) {
        if (locked || v == null) return;
        this.value = v;
        this.floorCoins = floorCoins;
        this.ranked = v.ranked();
        Loot best = v.bestItem();
        long worth = v.bestWorth();
        String key = best == null ? null : best.key();
        boolean jackpot = key != null && (JACKPOTS.get(Math.max(1, floor.number()) - 1).contains(key) || worth >= JACKPOT_AT);
        long blueAt = Math.max(1_000_000L, floorCoins);
        if (key != null && DYES.contains(key)) {
            band = Band.JACKPOT;
            ending = Ending.DYE;
            jackpotRgb = dyeRgb(key);
        } else if (jackpot) {
            band = Band.JACKPOT;
            ending = Ending.JACKPOT;
            jackpotRgb = Dealer.of(floor).rgb;
        } else if (worth >= GOLD_AT) {
            band = Band.GOLD;
            ending = Ending.WIN;
        } else if (worth >= PURPLE_AT) {
            band = Band.PURPLE;
            ending = Ending.WIN;
        } else if (worth >= blueAt) {
            band = Band.BLUE;
            ending = Ending.WIN;
        } else {
            band = Band.BUST;
            ending = Ending.BUST;
        }
        if (dragons) {
            // A lit Apex is the scream: the Dark Claymore and the Necron Dye. Power for the other jackpots, then by band.
            dragon = switch (band) {
                case BUST -> -1;
                case BLUE -> 2;
                case PURPLE -> 3;
                case GOLD -> 1;
                case JACKPOT -> key != null && (key.equals("DARK_CLAYMORE") || key.equals("DYE_NECRON")) ? 4 : 0;
            };
            if (band == Band.JACKPOT && ending != Ending.DYE) jackpotRgb = DRAGONS[dragon];
        }
    }

    // ------------------------------------------------------------------ input

    /** A seal clicked (chain mode). Every seal but M7's last cracks on one click; that one on three. */
    public void clickSeal(int i, long t) {
        if (i < 0 || i >= seals || sealAt[i] >= 0 || t < ARRIVE) return;
        lastInputAt = t;
        handTook = false;
        sealHits[i]++;
        if (sealHits[i] >= hitsFor(i)) sealAt[i] = t;
        checkSeals(t);
    }

    public int hitsFor(int i) {
        return floor.number() == 7 && i == seals - 1 ? 3 : 1;
    }

    /** Which seal the player should click next: the first unbroken one, left to right. */
    public int nextSeal() {
        for (int i = 0; i < seals; i++) if (sealAt[i] < 0) return i;
        return -1;
    }

    private void checkSeals(long t) {
        if (unlockAt >= 0) return;
        long last = -1;
        for (long a : sealAt) {
            if (a < 0) return;
            last = Math.max(last, a);
        }
        unlockAt = Math.max(t, last + SEAL_FALL);
    }

    /** Whether the wheel may be spun now: unlocked, and not about to stop. */
    public boolean canPull(int d, long t) {
        if (d < 0 || d >= drums.length || unlockAt < 0 || t < unlockAt) return false;
        Drum dr = drums[d];
        if (dr.pulls.size() >= MAX_PULLS) return false;
        return dr.stopAt < 0 || t < dr.stopAt - 450;
    }

    /**
     * The wheel let go: {@code strength} 0..1 from how far and how fast it was dragged. Solves the stop so the wheel
     * lands exactly on a veiled segment whatever the strength.
     */
    public void pull(int d, float strength, long t) {
        if (!canPull(d, t)) return;
        lastInputAt = t;
        Drum dr = drums[d];
        strength = Math.max(0f, Math.min(1f, strength));
        float p = pos(d, t), v = Math.max(0f, vel(d, t));
        float v0 = v + 7f + 15f * strength;
        float span = SPIN_MIN + (SPIN_MAX - SPIN_MIN) * (0.25f + 0.55f * strength);
        if (dr.pulls.size() > 0) span = Math.max(span * 0.7f, (dr.stopAt - t) / 1000f + 0.6f);
        span = Math.min(SPIN_MAX, span);
        long target = (long) Math.ceil(p + v0 * span / 2f);
        if (target <= p + 3) target = (long) Math.ceil(p) + 3;
        while (!veiled(target)) target++;
        float dist = target - p;
        dr.t0 = t;
        dr.p0 = p;
        dr.v0 = v0;
        dr.target = target;
        dr.stopAt = t + Math.round(2000f * dist / v0);
        dr.pulls.add(new long[]{t, Math.round(strength * 1000)});
    }

    /** The player is dragging the chain or a lever: the hand holds off while they do. */
    public void touch(long t) {
        lastInputAt = Math.max(lastInputAt, t);
        handTook = false;
    }

    /** The F5 shell game: one of the nine Livids picked. Any pick is the real one. */
    public void pickLivid(int i, long t) {
        if (!livid || liviLineAt < 0 || liviPickAt >= 0 || t < liviLineAt + LIVID_LINE || i < 0 || i > 8) return;
        liviPick = i;
        liviPickAt = t;
        lastInputAt = t;
    }

    /** Fires every automatic action that is due. Call every frame with the scene time, before drawing. */
    public void update(long t) {
        if (t < ARRIVE) return;
        // The seals: watch mode cracks them one by one; chain mode waits for clicks (a nudge after AUTO_WAIT).
        if (seals > 0 && unlockAt < 0) {
            long from = Math.max(ARRIVE, lastInputAt);
            long wait = chain && !handTook ? AUTO_WAIT : SEAL_STEP;
            int next = nextSeal();
            if (next >= 0 && t >= from + wait) {
                handTook = true;
                sealHits[next] = hitsFor(next);
                sealAt[next] = from + wait;
                lastInputAt = from + wait;
                checkSeals(from + wait);
            }
            if (unlockAt < 0) return;
        }
        if (unlockAt < 0 || t < unlockAt) return;
        // The pulls.
        for (int d = 0; d < drums.length; d++) {
            Drum dr = drums[d];
            if (dr.t0 >= 0) continue;
            long ready = readyAt(d);
            if (ready < 0) continue;
            boolean waitForPlayer = chain && !handTook;
            long wait = waitForPlayer ? AUTO_WAIT : WATCH_WAIT;
            long at = (waitForPlayer ? Math.max(ready, lastInputAt) : ready) + wait;
            if (t >= at + HAND_STROKE / 2) {
                handTook = true;
                // The hand's stroke bottoms out half way through.
                handAt[d] = at;
                pull(d, DEFAULT_PULL, at + HAND_STROKE / 2);
            } else if (t >= at) {
                handAt[d] = at;
            }
        }
        // All stopped: the shell game, then the tell.
        long allStop = -1;
        for (Drum dr : drums) {
            if (dr.stopAt < 0 || t < dr.stopAt) return;
            allStop = Math.max(allStop, dr.stopAt);
        }
        if (livid) {
            if (liviLineAt < 0) liviLineAt = allStop + 200;
            if (liviPickAt < 0) {
                long at = liviLineAt + LIVID_LINE + (chain ? AUTO_WAIT : LIVID_WATCH);
                if (t >= at) {
                    liviPick = (int) Math.floorMod(seed, 9L);
                    liviPickAt = at;
                } else return;
            }
            if (tellAt < 0) tellAt = liviPickAt + LIVID_AFTER;
        } else if (tellAt < 0) {
            tellAt = allStop + 260;
        }
        if (t >= tellAt) locked = true;
    }

    /** When the hand started its stroke on each drum, or -1. */
    private final long[] handAt = {-1, -1, -1, -1};

    public long handAt(int d) {
        return d < handAt.length ? handAt[d] : -1;
    }

    /** When the wheel became spinnable, or -1 if it is not yet. */
    long readyAt(int d) {
        return unlockAt;
    }

    // ------------------------------------------------------------------ the drums

    /** Idle drift before a pull, niches a second: the drum is never quite still. */
    static final float IDLE = 0.18f;

    /** Drum {@code d}'s position at {@code t}, in niches: whole numbers are a niche square in the window. */
    public float pos(int d, long t) {
        Drum dr = drums[d];
        if (dr.t0 < 0 || t < dr.t0) {
            float idle = IDLE * Math.max(0, t - ARRIVE) / 1000f;
            return idle + d * 0.37f;
        }
        if (t >= dr.stopAt) return dr.target;
        float tau = (t - dr.t0) / 1000f, T = (dr.stopAt - dr.t0) / 1000f;
        float a = dr.v0 / T;
        float p = dr.p0 + dr.v0 * tau - 0.5f * a * tau * tau;
        // A little give at the very end: the ratchet catching the niche.
        return Math.min(p, dr.target);
    }

    /** Its speed, niches a second. */
    public float vel(int d, long t) {
        Drum dr = drums[d];
        if (dr.t0 < 0 || t < dr.t0) return IDLE;
        if (t >= dr.stopAt) return 0f;
        float tau = (t - dr.t0) / 1000f, T = (dr.stopAt - dr.t0) / 1000f;
        return dr.v0 * (1f - tau / T);
    }

    /**
     * What segment {@code i} of the wheel holds: an index into the chest's items (>= 0, the stop once revealed), -1
     * for filler (then {@link #fillerAt} says which), -2 veiled.
     */
    public int niche(int d, long i, long t) {
        if (!veiled(i)) return -1;
        Drum dr = drums[d];
        if (i == dr.target && reveal() >= 0 && t >= reveal()) return value == null ? -1 : value.best();
        return -2;
    }

    /** The filler on segment {@code i}: fixed by the seed and the segment, never by the outcome. */
    public Loot fillerAt(int d, long i) {
        long seg = Math.floorMod(i, (long) SEGMENTS);
        Random r = new Random(seed * 1_000_003L + d * 7_919L + seg * 104_729L);
        r.nextInt();
        int pick = r.nextInt(fillerTotal);
        for (int k = 0; k < fillerWeights.length; k++) {
            pick -= fillerWeights[k];
            if (pick < 0) return filler.get(k);
        }
        return filler.get(0);
    }

    public Drum drum(int d) {
        return drums[d];
    }

    public int drumCount() {
        return drums.length;
    }

    /** Whether any drum is moving fast enough to blur. */
    public float blur(int d, long t) {
        return Math.max(0f, Math.min(1f, (vel(d, t) - 6f) / 14f));
    }

    // ------------------------------------------------------------------ the tell

    /** The cycle's colours, from the floor alone: grey, blue, purple, gold, the floor's own. */
    int[] cycle() {
        if (dragons) return DRAGONS;
        return new int[]{Band.BUST.rgb, Band.BLUE.rgb, Band.PURPLE.rgb, Band.GOLD.rgb, Dealer.of(floor).rgb};
    }

    /** The tell's steps, ms after it starts: three rounds, each step shorter. */
    static final long[] STEPS;

    static {
        List<Long> s = new ArrayList<>();
        float at = 0f, step = 300f;
        while (at < TELL_MS - 60) {
            s.add((long) at);
            at += step;
            step = Math.max(70f, step * 0.86f);
        }
        STEPS = new long[s.size()];
        for (int i = 0; i < STEPS.length; i++) STEPS[i] = s.get(i);
    }

    /** -1 before the tell starts. */
    public long tellAt() {
        return tellAt;
    }

    public long reveal() {
        return tellAt < 0 ? -1 : tellAt + TELL_MS;
    }

    /** Which step of the cycle the tell is on (an index into {@link #cycle()}), or -1 outside it. */
    public int tellStep(long t) {
        if (tellAt < 0 || t < tellAt || t >= reveal()) return -1;
        long r = t - tellAt;
        int i = 0;
        while (i + 1 < STEPS.length && STEPS[i + 1] <= r) i++;
        return i;
    }

    public int stepCount() {
        return STEPS.length;
    }

    public long stepStart(int i) {
        return tellAt + STEPS[i];
    }

    /** The tell's colour now: cycling, then the truth from the reveal. */
    public int tellRgb(long t) {
        int[] c = cycle();
        int s = tellStep(t);
        if (s >= 0) return c[s % c.length];
        if (reveal() >= 0 && t >= reveal()) return outcomeRgb();
        return 0x6A5A40;
    }

    /** The outcome's colour: its band's, or the jackpot's own. */
    public int outcomeRgb() {
        return band == Band.JACKPOT ? jackpotRgb : band.rgb;
    }

    /** How far through the current step, 0..1 - the flame swells on each one. */
    public float stepPulse(long t) {
        int s = tellStep(t);
        if (s < 0) return 0f;
        long a = tellAt + STEPS[s], b = s + 1 < STEPS.length ? tellAt + STEPS[s + 1] : reveal();
        return Math.max(0f, Math.min(1f, (t - a) / (float) Math.max(1, b - a)));
    }

    /** The M7 dragon lit now (an index into {@link #DRAGONS}), -1 none - every statue grey after a bust. */
    public int dragonLit(long t) {
        int s = tellStep(t);
        if (s >= 0) return s % DRAGONS.length;
        if (reveal() >= 0 && t >= reveal()) return dragon;
        return -1;
    }

    // ------------------------------------------------------------------ after the reveal

    /** How long each ending runs after the reveal. */
    public long endingMs() {
        return switch (ending) {
            case BUST -> 2700;
            case WIN -> 5600;
            case JACKPOT -> 7600;
            case DYE -> 10600;
        };
    }

    /** The world stops for a dye: from the reveal to {@link #THAW_AFTER}, with three heartbeats. */
    public static final long FREEZE_AFTER = 200, THAW_AFTER = 3600;
    public static final long[] HEARTS = {1100, 1950, 2800};

    /** The drop comes forward this long after the reveal; a dye's after its thaw. */
    public long arrive() {
        long r = reveal();
        if (r < 0) return Long.MAX_VALUE;
        return r + switch (ending) {
            case BUST -> 450;
            case WIN -> 650;
            case JACKPOT -> 1250;
            case DYE -> THAW_AFTER + 600;
        };
    }

    /** The whole chest laid out under the drop. */
    public long stripAt() {
        long r = reveal();
        if (r < 0) return Long.MAX_VALUE;
        return ending == Ending.BUST ? r + 500 : arrive() + 1500;
    }

    public long total() {
        long r = reveal();
        return r < 0 ? Long.MAX_VALUE : r + endingMs();
    }

    /** Whether the world is stopped (a dye's freeze): the drums, the dealer and every particle hold still. */
    public boolean frozen(long t) {
        long r = reveal();
        return ending == Ending.DYE && r >= 0 && t >= r + FREEZE_AFTER && t < r + THAW_AFTER;
    }

    /** The clock physical things run on: held through a dye's freeze. */
    public long phys(long t) {
        long r = reveal();
        if (ending != Ending.DYE || r < 0 || t < r + FREEZE_AFTER) return t;
        if (t < r + THAW_AFTER) return r + FREEZE_AFTER;
        return t - (THAW_AFTER - FREEZE_AFTER);
    }

    /** How grey the frozen world is, 0..1. */
    public float grey(long t) {
        long r = reveal();
        if (ending != Ending.DYE || r < 0) return 0f;
        float in = ease((t - r - FREEZE_AFTER) / 900f), out = 1f - ease((t - r - THAW_AFTER) / 500f);
        return Math.min(in, out);
    }

    /** The whole scene's opacity: in over the first 300 ms, out over the last {@link #FADE_MS}. */
    public float master(long t) {
        float in = ease(t / 300f);
        long end = total();
        float out = end == Long.MAX_VALUE ? 1f : 1f - ease((t - (end - FADE_MS)) / (float) FADE_MS);
        return Math.min(in, out);
    }

    public float dim(long t) {
        return 0.82f * master(t);
    }

    /** A thump's shake, as a fraction of the screen height. */
    public float shake(long t) {
        float s = 0f;
        for (int i = 0; i < seals; i++) if (sealAt[i] >= 0) s += 0.006f * bump(t - sealAt[i], 160);
        for (Drum dr : drums) {
            for (long[] p : dr.pulls) s += 0.008f * bump(t - p[0], 220);
            if (dr.stopAt >= 0) s += (dr.big ? 0.012f : 0.007f) * bump(t - dr.stopAt, 260);
        }
        long r = reveal();
        if (r >= 0 && ending != Ending.BUST) {
            float k = ending == Ending.WIN ? 0.010f : 0.020f;
            s += k * bump(t - (ending == Ending.DYE ? r + THAW_AFTER : r), 500);
        }
        return s;
    }

    static float bump(long dt, long len) {
        if (dt < 0 || dt >= len) return 0f;
        float x = dt / (float) len;
        return (1f - x) * (1f - x);
    }

    public static float ease(float x) {
        x = Math.max(0f, Math.min(1f, x));
        return x * x * (3 - 2 * x);
    }

    // ------------------------------------------------------------------ getters

    public DungeonFloor floor() { return floor; }
    public ChestTier tier() { return tier; }
    public boolean red() { return red; }
    public boolean chain() { return chain; }
    public boolean throne() { return throne; }
    public boolean dragons() { return dragons; }
    public boolean livid() { return livid; }
    public long seed() { return seed; }
    public int seals() { return seals; }
    public long sealAt(int i) { return sealAt[i]; }
    public int sealHits(int i) { return sealHits[i]; }
    public long unlockAt() { return unlockAt; }
    public long liviLineAt() { return liviLineAt; }
    public long liviPickAt() { return liviPickAt; }
    public int liviPick() { return liviPick; }
    public ChestValue value() { return value; }
    public boolean locked() { return locked; }
    public Band band() { return band; }
    public Ending ending() { return ending; }
    public int dragon() { return dragon; }
    public Dealer dealer() { return Dealer.of(floor); }
    public List<Integer> ranked() { return ranked; }
    public long lastInputAt() { return lastInputAt; }
    public long floorCoins() { return floorCoins; }

    /** Whether the player is being waited on now (chain mode): the hint and the chain's glow. */
    public boolean waitingForPlayer(long t) {
        if (!chain || t < ARRIVE) return false;
        if (seals > 0 && unlockAt < 0) return true;
        if (unlockAt >= 0 && t >= unlockAt) {
            for (int d = 0; d < drums.length; d++) if (drums[d].t0 < 0 && readyAt(d) >= 0 && handAt[d] < 0) return true;
        }
        return livid && liviLineAt >= 0 && liviPickAt < 0 && t >= liviLineAt + LIVID_LINE;
    }
}
