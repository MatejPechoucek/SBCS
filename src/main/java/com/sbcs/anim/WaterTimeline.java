package com.sbcs.anim;

/**
 * The water mythics' animations - "The Coil" (the Titanoboa, Backwater Bayou) and "The Totem" (the Wiki Tiki,
 * Hotspots). Pure: the whole schedule for one kill from its creature, its drop and a seed; the scene and the screen
 * only ask it what is happening now. Built on Eruption's plan (docs/ideas/02-wiki-tiki.md, 03-titanoboa.md).
 *
 * <b>The tell is a colour.</b> Both creatures cycle a fixed scale of five - the Titanoboa's scales (mud, lily green,
 * bone, gold, aquamarine), the Wiki Tiki's compacting ring (Hypixel's own terracotta stages: plain, lime, yellow, red,
 * then aquamarine) - up and down, faster each time, the same on every run; at {@link #REVEAL} it lands on the drop's
 * level: the first colour is a bust, and the further up the scale the better the drop. The creature is the kill's own
 * (the player knows it), so it may change everything before the reveal; the drop may change nothing
 * (tools/WaterTest hashes every frame and sample, per creature).
 *
 * Drops and chances are the wiki's (2026-10-23). The Aquamarine Dye is shared by both and gets the grandest ending,
 * "The Bayou Floods": time stops, the world greys, aquamarine water rises up the creature texel by texel.
 */
public final class WaterTimeline {

    /** The two water mythics. {@code key} is the stats and config key part, {@code maxHp} the wiki's. */
    public enum Creature {
        TITANOBOA("Titanoboa", 45e6, "Enchanted Lily Pad", "ENCHANTED_WATER_LILY", null,
                new int[]{0x5A4326, 0x4FA83A, 0xE8DFC4, 0xF2C230, 0x7FFFD4}),
        WIKI_TIKI("Wiki Tiki", 75e6, "Enchanted Clay Ball", "ENCHANTED_CLAY_BALL", "clay_ball",
                new int[]{0x985E43, 0x7FC422, 0xF2C02C, 0xB8312A, 0x7FFFD4});

        public final String title;
        public final double maxHp;
        /** What every kill drops anyway, shown under a bust (not the reveal - as Eruption's guaranteed drops). */
        public final String guaranteed, guaranteedId, guaranteedIcon;
        /** The tell's five colours, bust first. */
        public final int[] scale;

        Creature(String title, double maxHp, String guaranteed, String guaranteedId, String guaranteedIcon, int[] scale) {
            this.title = title;
            this.maxHp = maxHp;
            this.guaranteed = guaranteed;
            this.guaranteedId = guaranteedId;
            this.guaranteedIcon = guaranteedIcon;
            this.scale = scale;
        }
    }

    /** The Aquamarine Dye's colour, #7FFFD4. */
    public static final int AQUAMARINE = 0x7FFFD4;

    public enum Ending { BUST, BOOK, SHED, EYES, BUBBLE, MASK, FLOOD }

    /**
     * What the animation plays. {@code level} is where the tell lands (0 a bust); {@code from} the only creature that
     * drops it (null: both); {@code head} a NEU head skin in water_heads.json, else {@code icon} a vanilla item and
     * {@code pack} Hypixel's pack icon (textures/item/...), drawn when that pack is loaded.
     */
    public enum Drop {
        BUST("", 0, Ending.BUST, null, null, null, null, null, 0, 0xAAAAAA),
        SCRIPTURES("Bobbin' Scriptures", 1, Ending.BOOK, null, "BOBBIN_SCRIPTURES", null, "enchanted_book",
                "island_relevant/crimson_isle/bobbin_time/bobbin_scriptures", 100, 0x5555FF),
        SHED("Titanoboa Shed", 2, Ending.SHED, Creature.TITANOBOA, "TITANOBOA_SHED", null, "paper",
                "fishing/misc/titanboa_shed", 500, 0xFFAA00),
        BUBBLE("Troubled Bubble", 2, Ending.BUBBLE, Creature.WIKI_TIKI, "TROUBLED_BUBBLE", "TROUBLED_BUBBLE", null, null, 500, 0xFFAA00),
        SNAKE_EYES("Snake Eyes", 3, Ending.EYES, Creature.TITANOBOA, "SNAKE_EYES", "SNAKE_EYES", null, null, 909, 0xFFAA00),
        TIKI_MASK("Tiki Mask", 3, Ending.MASK, Creature.WIKI_TIKI, "TIKI_MASK", "TIKI_MASK", null, null, 1000, 0xFFAA00),
        AQUAMARINE("Aquamarine Dye", 4, Ending.FLOOD, null, "DYE_AQUAMARINE", "DYE_AQUAMARINE", null, null, 50_000, 0x55FFFF);

        public final String title;
        public final int level;
        public final Ending ending;
        public final Creature from;
        public final String id, head, icon, pack;
        /** About one in this many kills (the wiki's chance, before Magic Find). */
        public final int oneIn;
        /** Its name's colour: the item's rarity. */
        public final int colour;

        Drop(String title, int level, Ending ending, Creature from, String id, String head, String icon, String pack, int oneIn, int colour) {
            this.title = title;
            this.level = level;
            this.ending = ending;
            this.from = from;
            this.id = id;
            this.head = head;
            this.icon = icon;
            this.pack = pack;
            this.oneIn = oneIn;
            this.colour = colour;
        }

        public boolean win() {
            return this != BUST;
        }

        /** Whether {@code c} can drop it. */
        public boolean from(Creature c) {
            return this == BUST || from == null || from == c;
        }
    }

    // ------------------------------------------------------------------ the build-up, ms (every run)

    /** The creature comes up out of the water: the snake's loops break the surface, the totem rises. */
    public static final long EMERGE_FROM = 100, EMERGE_TO = 1300;
    /** The cycle: one colour step each, shorter each time. */
    public static final long CYCLE_FROM = 1500;
    public static final long[] STEPS = EruptionTimeline.steps(CYCLE_FROM, 360f, 0.93f, 4300);
    /** The hold: everything trembles, the colour flickers through every level. */
    public static final long HANG = 4300;
    public static final long REVEAL = 4800;
    public static final long FADE = 400;
    /** The Wiki Tiki's three carved faces light bottom-up; the Laser Totems come up beside it. */
    public static final long[] FACES = {1700, 2500, 3300};
    public static final long LASERS_FROM = 1400;

    /** The level each step goes to: up the scale and down again. */
    public static int stepLevel(int i) {
        return EruptionTimeline.stepLevel(i);
    }

    private final Creature creature;
    private final Drop drop;
    private final long seed;

    public WaterTimeline(Creature creature, Drop drop, long seed) {
        this.creature = creature;
        this.drop = drop == null ? Drop.BUST : drop;
        this.seed = seed;
    }

    public Creature creature() { return creature; }
    public Drop drop() { return drop; }
    public Ending ending() { return drop.ending; }
    public long seed() { return seed; }

    // ------------------------------------------------------------------ the endings' beats, from the reveal

    public static final long R = REVEAL;
    /** A bust: the Titanoboa slackens and sinks, the Wiki Tiki cracks and topples; the guaranteed drop is named. */
    public static final long B_SINK = R + 100, B_SINK_TO = R + 1100, B_CRACK = R + 80, B_TOPPLE = R + 350, B_TOPPLE_TO = R + 1250,
            B_NAME = R + 900;
    /** The book: the creature glows green and goes under, the book comes up where it was. */
    public static final long K_GLOW = R, K_UNDER = R + 300, K_UNDER_TO = R + 1300;
    /** The Shed: the snake slides out of its own skin and away; the pale skin left behind crumples into the drop. */
    public static final long S_SLIDE = R + 300, S_SLIDE_TO = R + 2100, S_CRUMPLE = R + 2000, S_CRUMPLE_TO = R + 2800;
    /** Snake Eyes: the coil goes black, two gold eyes open in its middle, blink once, and are the drop. */
    public static final long E_DARK = R + 150, E_DARK_TO = R + 900, E_OPEN = R + 1100, E_BLINK = R + 1950, E_FUSE = R + 2500;
    /** The Troubled Bubble: the lasers meet in the top face's mouth, a bubble swallows them and floats free. */
    public static final long U_MEET = R + 100, U_GROW = R + 450, U_GROW_TO = R + 1900, U_FREE = R + 2000;
    /** The Tiki Mask: the top face peels off, turns to the camera, its eyes light gold. */
    public static final long M_PEEL = R + 300, M_PEEL_TO = R + 1200, M_TURN = R + 1200, M_TURN_TO = R + 1900, M_EYES = R + 2050;
    /**
     * "The Bayou Floods": time stops ({@link #phys}) and the world greys; aquamarine water climbs the creature; time
     * comes back and the snake rears up into a column (the totem drowns and a geyser goes up); the dye at the top.
     */
    public static final long F_FREEZE = R + 250, F_GREY_TO = R + 900, F_HEARTS0 = R + 1100, F_RISE = R + 1400, F_RISE_TO = R + 3700,
            F_RESUME = R + 3800, F_REAR = R + 3900, F_REAR_TO = R + 5100, F_DYE = R + 4900;
    /** The three heartbeats in the stopped world. */
    public static final long[] F_HEARTS = {R + 1100, R + 1750, R + 2400};

    /** When the drop is shown: name, odds and price follow it. */
    public long arrive() {
        return switch (drop.ending) {
            case BUST -> B_NAME;
            case BOOK -> R + 1700;
            case SHED -> R + 3300;
            case EYES -> R + 2900;
            case BUBBLE -> R + 2900;
            case MASK -> R + 2700;
            case FLOOD -> R + 6300;
        };
    }

    public long total() {
        return switch (drop.ending) {
            case BUST -> R + 2200;
            case BOOK -> arrive() + 3200;
            case SHED, EYES, BUBBLE, MASK -> arrive() + 4200;
            case FLOOD -> arrive() + 7000;
        };
    }

    /** The drop's rise into the showcase, 0..1. */
    public long riseFrom() {
        return switch (drop.ending) {
            case BUST -> Long.MAX_VALUE;
            case BOOK -> R + 700;
            case SHED -> S_CRUMPLE_TO - 300;
            case EYES -> E_FUSE;
            case BUBBLE -> U_FREE;
            case MASK -> M_EYES + 200;
            case FLOOD -> F_DYE;
        };
    }

    public long riseTo() {
        return riseFrom() == Long.MAX_VALUE ? Long.MAX_VALUE : arrive();
    }

    public float rise(long t) {
        if (t < riseFrom()) return 0f;
        return backOut((t - riseFrom()) / (float) (riseTo() - riseFrom()));
    }

    // ------------------------------------------------------------------ the clock

    /** The physical clock - water, ripples, the snake's slither. It stops only for the dye. */
    public long phys(long t) {
        if (drop.ending != Ending.FLOOD || t <= F_FREEZE) return t;
        return t < F_RESUME ? F_FREEZE : F_FREEZE + (t - F_RESUME);
    }

    /** How grey the stopped world is, 0..1 (the dye only). */
    public float grey(long t) {
        if (drop.ending != Ending.FLOOD) return 0f;
        return ease((t - F_FREEZE) / (float) (F_GREY_TO - F_FREEZE)) * (1f - ease((t - F_RESUME) / 700f));
    }

    /** How far the aquamarine water has climbed, 0..1 of the creature's height (the dye only). */
    public float flood(long t) {
        if (drop.ending != Ending.FLOOD) return 0f;
        return ease((t - F_RISE) / (float) (F_RISE_TO - F_RISE));
    }

    /** A heartbeat in the stopped world: 1 at the beat, gone in ~250 ms. */
    public float heart(long t) {
        if (drop.ending != Ending.FLOOD) return 0f;
        float h = 0f;
        for (long at : F_HEARTS) if (t >= at) h = Math.max(h, (float) Math.exp(-(t - at) / 120.0));
        return h;
    }

    /** The Titanoboa rearing up into a column (the totem's geyser), 0..1. */
    public float rear(long t) {
        if (drop.ending != Ending.FLOOD) return 0f;
        return ease((t - F_REAR) / (float) (F_REAR_TO - F_REAR));
    }

    // ------------------------------------------------------------------ the build-up

    /** The creature coming up, 0 under .. 1 up. */
    public static float emerge(long t) {
        return ease((t - EMERGE_FROM) / (float) (EMERGE_TO - EMERGE_FROM));
    }

    /** How tight the coil is, 0 loose .. 1 wound round the lily pad; the same every run. */
    public static float tighten(long t) {
        return ease((t - 900) / (float) (HANG + 200 - 900));
    }

    /** Which step {@code t} is in, -1 before. */
    public static int step(long t) {
        int k = -1;
        for (int i = 0; i < STEPS.length; i++) if (t >= STEPS[i]) k = i;
        return k;
    }

    /** A pop at each step: 1 at the step, gone in ~200 ms. Same for every drop. */
    public static float pop(long t) {
        int k = step(t);
        if (k < 0 || t >= HANG) return 0f;
        return (float) Math.exp(-(t - STEPS[k]) / 110.0);
    }

    /** The hang's fixed flicker: a level per 55 ms, visiting every level and favouring none. */
    private static final int[] FLICKER = {3, 0, 4, 1, 2, 4, 0, 3, 1, 4, 2, 0, 3, 1};

    /** The tremble in the hang, 0..1. Same for every drop. */
    public static float tremble(long t) {
        return t >= HANG && t < REVEAL ? ease((t - HANG) / 400f) : 0f;
    }

    /**
     * The level now, continuous 0 .. 4: a soft start before the cycle, the steps, the flicker in the hang, and from
     * the reveal the landing on the drop's own level.
     */
    public float level(long t) {
        if (t < CYCLE_FROM) return 0.5f * ease((t - EMERGE_TO + 300) / 500f);
        if (t < HANG) {
            int k = step(t);
            if (k < 0) return 0.5f;
            float from = k == 0 ? 0.5f : stepLevel(k - 1), to = stepLevel(k);
            long len = k + 1 < STEPS.length ? STEPS[k + 1] - STEPS[k] : HANG - STEPS[k];
            return from + (to - from) * ease((t - STEPS[k]) / (0.6f * len));
        }
        if (t < REVEAL) return FLICKER[(int) ((t - HANG) / 55) % FLICKER.length];
        float prev = FLICKER[(int) ((REVEAL - 1 - HANG) / 55) % FLICKER.length];
        return prev + (drop.level - prev) * ease((t - REVEAL) / 120f);
    }

    /** The level as a colour of this creature's scale, blended between the two round it. */
    public int colour(long t) {
        return colourAt(creature, level(t));
    }

    public static int colourAt(Creature c, float level) {
        float h = Math.max(0f, Math.min(4f, level));
        int lo = (int) Math.floor(h), hi = Math.min(4, lo + 1);
        return mix(c.scale[lo], c.scale[hi], h - lo);
    }

    /** The landed colour, from the reveal on. */
    public int landed() {
        return creature.scale[drop.level];
    }

    // ------------------------------------------------------------------ the showcase

    /** The banner: Hypixel's own "RARE DROP!" on the gold ones, "WOW!" on the dye; none on the book or a bust. */
    public boolean banner() {
        return drop.ending != Ending.BUST && drop.ending != Ending.BOOK;
    }

    public String bannerText() {
        return drop.ending == Ending.FLOOD ? "WOW!" : "RARE DROP!";
    }

    public int bannerColour() {
        return drop.ending == Ending.FLOOD ? 0xFF55FF : 0xFFAA00;
    }

    public float bannerStamp(long t) {
        if (!banner() || t < arrive() - 150) return 0f;
        return Math.min(1f, (t - arrive() + 150) / 180f);
    }

    /** Letters of the name stamped: the big ones spell it out. */
    public int letters(long t) {
        if (!banner()) return 999;
        return Math.max(0, (int) ((t - arrive() - 50) / 45) + 1);
    }

    public float nameAlpha(long t) {
        return ease((t - arrive() - 50) / 300f);
    }

    public float lineAlpha(long t) {
        return drop.win() ? ease((t - arrive() - 400) / 300f) : 0f;
    }

    public long countStart() {
        return drop.win() ? arrive() + 650 : arrive() + 300;
    }

    public static final long COUNT_MS = 1000;

    private long price = -1;
    private String priceSource;

    public long price() { return price; }

    public void price(long coins) { this.price = coins; }

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

    // ------------------------------------------------------------------ helpers

    public static float ease(float t) {
        return EruptionTimeline.ease(t);
    }

    public static float backOut(float t) {
        return EruptionTimeline.backOut(t);
    }

    /** {@code t} through [from, from + len), 0..1, eased. */
    public static float span(long t, long from, long len) {
        return ease((t - from) / (float) Math.max(1, len));
    }

    public static int mix(int a, int b, float k) {
        return EruptionTimeline.mix(a, b, k);
    }
}
