package com.sbcs.anim;

/**
 * The Scatha pet's reveal, per rarity - pure, no Minecraft. Time 0 is the moment the last cut frees the head (the
 * cleaver cutscene's REVEAL phase), so everything here comes after the run has already said "pet".
 *
 * <b>Which pet is not said until {@link #LAND}</b> (tools/PetTierTest hashes every draw call and every sound before
 * it, for every tier): the head flies to the middle ({@link #RISE}), then a glow behind it and the crystal veins at
 * the edges cycle blue, violet, gold - twice round and back to blue, the steps shortening ({@link #STEPS}), the
 * Thaw's cycle - and land on the pet's own colour. Ending on blue makes a Legendary's landing the biggest jump.
 *
 * After it the tiers split by how long and how loud they are:
 * <ul>
 *   <li><b>Rare</b> (~1 in 700, the commonest pet): a blue burst, rays and the card. Short, so the player mines on.</li>
 *   <li><b>Epic</b>: the head spins once out of the landing, purple crystal shards burst out past the screen's
 *       edges, two shockwaves, the name stamped a letter at a time.</li>
 *   <li><b>Legendary</b> (~1 in 4,200), "The Hollows Answer": the screen holds still on a heartbeat, six cracks open
 *       round it one per Hollows gem colour and each pours its colour into the head, the worm's own body segments
 *       rise and coil round it in a ring, the ring turns gold, and it bursts: gold rays, coins, "PET DROP!" and
 *       "your Nth legendary".</li>
 * </ul>
 */
public final class PetTierTimeline {

    public enum Tier {
        RARE(0x5555FF, "Rare", 700, 0),
        EPIC(0xAA00AA, "Epic", 1_400, 1),
        LEGENDARY(0xFFAA00, "Legendary", 4_200, 2);

        /** SkyBlock's rarity colour. */
        public final int rgb;
        public final String title;
        /** About one kill in this many gives it (the wiki, 2026-10-23, before bestiary). */
        public final int oneIn;
        /** Its place in the cycle: 0 blue, 1 violet, 2 gold - also the Thaw's flare voice it lands on. */
        public final int colour;

        Tier(int rgb, String title, int oneIn, int colour) {
            this.rgb = rgb;
            this.title = title;
            this.oneIn = oneIn;
            this.colour = colour;
        }

        /** From petInfo's tier, any case; anything but epic or legendary plays as a Rare. */
        public static Tier of(String rarity) {
            if (rarity == null) return RARE;
            return switch (rarity.toLowerCase(java.util.Locale.ROOT)) {
                case "legendary", "mythic" -> LEGENDARY;
                case "epic" -> EPIC;
                default -> RARE;
            };
        }
    }

    /** The cycle's colours, in order: blue, violet, gold. */
    public static final int[] CYCLE = {Tier.RARE.rgb, Tier.EPIC.rgb, Tier.LEGENDARY.rgb};

    /** The head's flight from the cut into the middle of the screen. */
    public static final long RISE = 650;
    /** The cycle's first step. */
    public static final long CYCLE_FROM = RISE + 120;
    /** Blue, violet, gold, blue, violet, gold, blue: each step's start, 300 ms shrinking to ~130. */
    public static final long[] STEPS;
    /** The cycle lands on the pet's own colour. The first moment anything may differ. */
    public static final long LAND;

    static {
        int n = 7;
        STEPS = new long[n];
        double at = CYCLE_FROM, len = 300, ratio = Math.pow(130.0 / 300.0, 1.0 / (n - 1));
        for (int i = 0; i < n; i++) {
            STEPS[i] = Math.round(at);
            at += len;
            len *= ratio;
        }
        LAND = Math.round(at) + 90;
    }

    /** How long a price takes to count up. */
    public static final long COUNT_MS = 900;
    /** The head shrinks away over the last this many ms. */
    public static final long SHRINK_MS = 320;

    // ---- Epic
    public static final long E_SPIN = 620;
    public static final long E_RINGS = 140;

    // ---- Legendary, from the landing
    /** The hold: a heartbeat on a still screen. */
    public static final long L_CRACK = 520;
    /** One crack every this many ms, six of them. */
    public static final long L_CRACK_STEP = 170;
    /** Each crack starts pouring this long after it opens; a pour reaches the head in {@link #L_POUR_MS}. */
    public static final long L_POUR_AFTER = 260, L_POUR_MS = 520;
    /** The body segments rise from below and coil round the head. */
    public static final long L_COIL = 1_350, L_RING = 2_550;
    /** The ring turns gold, segment by segment. */
    public static final long L_GOLD = 2_700, L_GOLD_STEP = 110;
    /** The second heartbeat, the ring tightening; then the burst. */
    public static final long L_BEAT2 = 3_400, L_BURST = 3_900;

    /** The Hollows' six gems, in the order the cracks open: ruby, amber, topaz, jade, sapphire, amethyst. */
    public static final int[] GEMS = {0xE8323C, 0xF08A24, 0xF2D33A, 0x3FC46B, 0x3C7BE8, 0xA654E8};

    private final Tier tier;
    private final long seed;
    private long price = -1;
    private String priceSource = "";
    private String note = "";

    public PetTierTimeline(Tier tier, long seed) {
        this.tier = tier;
        this.seed = seed;
    }

    public Tier tier() { return tier; }
    public long seed() { return seed; }

    /** The card's price and where it came from ("lowest BIN"); -1 while not known. */
    public void price(long coins, String source) {
        this.price = coins;
        this.priceSource = source == null ? "" : source;
    }
    public long price() { return price; }
    public String priceSource() { return priceSource; }

    /** The card's last line: "Your 3rd Scatha pet, after 812 kills". */
    public void note(String note) { this.note = note == null ? "" : note; }
    public String note() { return note; }

    /** When the showcase starts: the head bursts out in its colour and the card follows. */
    public long showcase() {
        return switch (tier) {
            case RARE, EPIC -> LAND;
            case LEGENDARY -> LAND + L_BURST;
        };
    }

    /** The card's first line ("PET DROP!") comes in here. */
    public long cardFrom() {
        return showcase() + (tier == Tier.RARE ? 120 : 260);
    }

    /** The name is stamped this many ms a letter (the Rare's comes in whole). */
    public long letterMs() {
        return tier == Tier.RARE ? 0 : tier == Tier.EPIC ? 55 : 70;
    }

    /** The price starts counting. */
    public long countStart() {
        return cardFrom() + (tier == Tier.RARE ? 260 : 700);
    }

    /** How long the card holds, start to the head shrinking. */
    private long hold() {
        return switch (tier) {
            case RARE -> 2_150;
            case EPIC -> 3_000;
            case LEGENDARY -> 4_000;
        };
    }

    /** The end of the reveal; the cleaver cutscene flashes out from here. */
    public long total() {
        return cardFrom() + hold() + SHRINK_MS;
    }

    /** 0 until the head starts shrinking away at the end, 1 when it is gone. */
    public float shrink(long t) {
        return clamp((t - (total() - SHRINK_MS)) / (float) SHRINK_MS);
    }

    /** The cycle's step at t (0..6), -1 before it, {@code STEPS.length} once landed. */
    public static int step(long t) {
        if (t < STEPS[0]) return -1;
        if (t >= LAND) return STEPS.length;
        int i = 0;
        while (i + 1 < STEPS.length && t >= STEPS[i + 1]) i++;
        return i;
    }

    /** The cycle's colour at t: each step fades into the next over its first 40%. Never reads the tier. */
    public static int cycleColour(long t) {
        int i = step(t);
        if (i < 0) return CYCLE[0];
        if (i >= STEPS.length) i = STEPS.length - 1;
        int now = CYCLE[i % 3], before = i == 0 ? CYCLE[0] : CYCLE[(i - 1) % 3];
        long end = i + 1 < STEPS.length ? STEPS[i + 1] : LAND;
        float k = clamp((t - STEPS[i]) / (0.4f * (end - STEPS[i])));
        return mix(before, now, k);
    }

    /** What the glow and the veins show: the cycle, then the pet's own colour from the landing. */
    public int colour(long t) {
        if (t < LAND) return cycleColour(t);
        return mix(cycleColour(LAND - 1), tier.rgb, clamp((t - LAND) / 90f));
    }

    /** How hard the head trembles through the cycle, 0..1 - the same every run. */
    public static float tremble(long t) {
        if (t < CYCLE_FROM || t >= LAND) return 0f;
        float k = (t - CYCLE_FROM) / (float) (LAND - CYCLE_FROM);
        return k * k;
    }

    public static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    public static int mix(int a, int b, float t) {
        t = clamp(t);
        int r = Math.round((a >> 16 & 255) + ((b >> 16 & 255) - (a >> 16 & 255)) * t);
        int g = Math.round((a >> 8 & 255) + ((b >> 8 & 255) - (a >> 8 & 255)) * t);
        int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return r << 16 | g << 8 | bl;
    }
}
