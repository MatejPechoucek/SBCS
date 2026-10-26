package com.sbcs.anim;

/**
 * The Revenant Horror's and the Tarantula Broodfather's animations ("Exhumed" and "The Brood", 2026-10-26): one
 * timeline per drop, pure (tools/SlayerTest).
 *
 * Unlike Hellion and the Seraph these open on the drop line, the outcome known: Matej asked for the good drops only
 * and for each to look like its item ("a digested mosquito would have a mosquito being digested"), so there is no tell
 * to keep honest and no bust. Every run restages the kill first - the boss struck three times and dying the vanilla
 * way ({@link #KILLED}) - then plays its drop's own ending from {@link #ENDING}, then the showcase at {@link #arrive()}:
 * the banner (the drop line's own tier word), the name stamped letter by letter, the odds and "your Nth", the price
 * counting up.
 *
 * The tiers are the chat's words, which are not the wiki's odds labels: Extraordinary drops print "VERY RARE DROP!",
 * Pray RNGesus ones "CRAZY RARE DROP!" (Matej's records, 2026-10-25: Beheaded Horror, Snake Rune, Darkness Within Rune,
 * Tarantula Talisman and Catalyst), RNGesus Incarnate presumably "PRAY TO RNGESUS DROP!" - nobody has had one on record.
 * The per-slayer threshold (the menu) filters on {@link Drop#tier}.
 */
public final class SlayerTimeline {

    /** The two slayers this covers, by the stats' and config's keys. */
    public enum Source {
        REVENANT("zombie", "Revenant Horror", "Exhumed", 0x55FF55),
        TARANTULA("spider", "Tarantula Broodfather", "The Brood", 0xFF5555);

        public final String key, boss, title;
        public final int colour;

        Source(String key, String boss, String title, int colour) {
            this.key = key;
            this.boss = boss;
            this.title = title;
            this.colour = colour;
        }
    }

    /** The drop tiers the chat prints, lowest first: the order is the threshold's. */
    public enum Tier {
        VERY_RARE("VERY RARE DROP!"), CRAZY_RARE("CRAZY RARE DROP!"), RNGESUS("PRAY TO RNGESUS DROP!");

        public final String word;

        Tier(String word) {
            this.word = word;
        }

        /** The tier of a drop line's opening word, or null. */
        public static Tier ofWord(String word) {
            if (word == null) return null;
            for (Tier t : values()) if (word.startsWith(t.word)) return t;
            if (word.startsWith("RARE DROP!")) return null;
            return null;
        }
    }

    /** Each drop's own ending: what happens between the kill and the showcase. */
    public enum Ending {
        // Shared.
        PLAIN, BOOK, PARAGON,
        // The Revenant's.
        BEHEAD, SNAKES, MAGGOT, SCYTHE, SINEW, HAND, WARDEN, MATCHA,
        // The Tarantula's.
        COCOON, DARKNESS, SWATTER, TALISMAN, VENOM, MOSQUITO, WASP, SNAIL, EYE, BRICK
    }

    /**
     * Every drop the animations play. {@code art}: a head (by its {@code HeadSkins.SLAYER} name), else an item icon
     * - Hypixel's pack's when loaded ({@code pack}), else the vanilla one. {@code oneIn}: the main pool's weight
     * against its total off the wiki's calculator (tier IV, or V for the tier V drops); 0 where the pool's total is
     * not known (the runes' extra pool, the separate Paragon and dye rolls).
     */
    public enum Drop {
        // ------------------------------------------------------------------ the Revenant Horror
        REVENANT_CATALYST(Source.REVENANT, "Revenant Catalyst", Tier.VERY_RARE, Ending.PLAIN, "REVENANT_CATALYST",
                null, "paper", "slayer/zombie/catalysts/revenant_catalyst", 19, 0xAA00AA),
        UNDEAD_CATALYST(Source.REVENANT, "Undead Catalyst", Tier.VERY_RARE, Ending.PLAIN, "UNDEAD_CATALYST",
                null, "paper", "slayer/zombie/catalysts/undead_catalyst", 32, 0x5555FF),
        SMITE(Source.REVENANT, "Smite VI", Tier.VERY_RARE, Ending.BOOK, "ENCHANTMENT_SMITE_6",
                null, "enchanted_book", null, 49, 0x5555FF),
        BEHEADED_HORROR(Source.REVENANT, "Beheaded Horror", Tier.CRAZY_RARE, Ending.BEHEAD, "BEHEADED_HORROR",
                "Beheaded Horror", null, null, 122, 0xAA00AA),
        SNAKE_RUNE(Source.REVENANT, "Snake Rune I", Tier.CRAZY_RARE, Ending.SNAKES, "SNAKE_RUNE;1",
                "Snake Rune I", null, null, 0, 0x55FF55),
        FESTERING_MAGGOT(Source.REVENANT, "Festering Maggot", Tier.CRAZY_RARE, Ending.MAGGOT, "FESTERING_MAGGOT",
                "Festering Maggot", null, null, 244, 0xAA00AA),
        SCYTHE_BLADE(Source.REVENANT, "Scythe Blade", Tier.CRAZY_RARE, Ending.SCYTHE, "SCYTHE_BLADE",
                null, "diamond", null, 348, 0xFFAA00),
        SHREDDED_SINEW(Source.REVENANT, "Shredded Sinew", Tier.CRAZY_RARE, Ending.SINEW, "SHARD_OF_THE_SHREDDED",
                "Shredded Sinew", null, null, 587, 0xFFAA00),
        SEVERED_HAND(Source.REVENANT, "Severed Hand", Tier.CRAZY_RARE, Ending.HAND, "SEVERED_HAND",
                "Severed Hand", null, null, 671, 0xFFAA00),
        WARDEN_HEART(Source.REVENANT, "Warden Heart", Tier.RNGESUS, Ending.WARDEN, "WARDEN_HEART",
                "Warden Heart", null, null, 2349, 0xFFAA00),
        MATCHA_DYE(Source.REVENANT, "Matcha Dye", Tier.RNGESUS, Ending.MATCHA, "DYE_MATCHA",
                "Matcha Dye", null, null, 5_000_000, 0x74A12E),
        PARAGON_REVENANT(Source.REVENANT, "Paragon Shard", Tier.CRAZY_RARE, Ending.PARAGON, "SHARD_PARAGON",
                "Paragon Shard", null, null, 0, 0xFFAA00),

        // ------------------------------------------------------------------ the Tarantula Broodfather
        SPIDER_CATALYST(Source.TARANTULA, "Spider Catalyst", Tier.VERY_RARE, Ending.COCOON, "SPIDER_CATALYST",
                null, "paper", "slayer/spider/spider_catalyst", 0, 0x5555FF),
        BANE(Source.TARANTULA, "Bane of Arthropods VI", Tier.VERY_RARE, Ending.BOOK, "ENCHANTMENT_BANE_OF_ARTHROPODS_6",
                null, "enchanted_book", null, 0, 0x5555FF),
        DARKNESS_WITHIN_RUNE(Source.TARANTULA, "Darkness Within Rune I", Tier.CRAZY_RARE, Ending.DARKNESS, "DARKNESS_WITHIN_RUNE;1",
                "Darkness Within Rune I", null, null, 258, 0x555555),
        TARANTULA_CATALYST(Source.TARANTULA, "Tarantula Catalyst", Tier.CRAZY_RARE, Ending.COCOON, "TARANTULA_CATALYST",
                null, "paper", "combat_1/tarantula_catalyst", 267, 0xAA00AA),
        FLY_SWATTER(Source.TARANTULA, "Fly Swatter", Tier.CRAZY_RARE, Ending.SWATTER, "FLY_SWATTER",
                null, "paper", "slayer/spider/fly_swatter", 601, 0xAA00AA),
        TARANTULA_TALISMAN(Source.TARANTULA, "Tarantula Talisman", Tier.CRAZY_RARE, Ending.TALISMAN, "TARANTULA_TALISMAN",
                "Tarantula Talisman", null, null, 601, 0xAA00AA),
        VIAL_OF_VENOM(Source.TARANTULA, "Vial of Venom", Tier.CRAZY_RARE, Ending.VENOM, "VIAL_OF_VENOM",
                "Vial of Venom", null, null, 1203, 0xAA00AA),
        DIGESTED_MOSQUITO(Source.TARANTULA, "Digested Mosquito", Tier.CRAZY_RARE, Ending.MOSQUITO, "DIGESTED_MOSQUITO",
                null, "rotten_flesh", null, 1718, 0xFFAA00),
        SHRIVELED_WASP(Source.TARANTULA, "Shriveled Wasp", Tier.CRAZY_RARE, Ending.WASP, "SHRIVELED_WASP",
                "Shriveled Wasp", null, null, 708, 0xFFAA00),
        ENSNARED_SNAIL(Source.TARANTULA, "Ensnared Snail", Tier.CRAZY_RARE, Ending.SNAIL, "ENSNARED_SNAIL",
                "Ensnared Snail", null, null, 2359, 0xFFAA00),
        PRIMORDIAL_EYE(Source.TARANTULA, "Primordial Eye", Tier.RNGESUS, Ending.EYE, "PRIMORDIAL_EYE",
                "Primordial Eye", null, null, 7077, 0xFFAA00),
        PARAGON_TARANTULA(Source.TARANTULA, "Paragon Shard", Tier.RNGESUS, Ending.PARAGON, "SHARD_PARAGON",
                "Paragon Shard", null, null, 400, 0xFFAA00),
        BRICK_RED_DYE(Source.TARANTULA, "Brick Red Dye", Tier.RNGESUS, Ending.BRICK, "DYE_BRICK_RED",
                "Brick Red Dye", null, null, 500_000, 0xCB4154);

        public final Source source;
        public final String title, priceId, head, icon, pack;
        public final Tier tier;
        public final Ending ending;
        public final int oneIn, colour;

        Drop(Source source, String title, Tier tier, Ending ending, String priceId, String head, String icon, String pack,
             int oneIn, int colour) {
            this.source = source;
            this.title = title;
            this.tier = tier;
            this.ending = ending;
            this.priceId = priceId;
            this.head = head;
            this.icon = icon;
            this.pack = pack;
            this.oneIn = oneIn;
            this.colour = colour;
        }

        public boolean isHead() {
            return head != null;
        }

        /** Whether it is a dye: the grandest ending of its slayer, and its own showcase. */
        public boolean dye() {
            return ending == Ending.MATCHA || ending == Ending.BRICK;
        }

        /** The drop a slayer drop line's item names for {@code source}, or null. */
        public static Drop of(Source source, String item) {
            if (item == null) return null;
            String s = item.trim();
            // "Enchanted Book (Smite VI)" and plain "Smite VI" both.
            if (s.startsWith("Enchanted Book")) s = s.replace("Enchanted Book", "").replace("(", "").replace(")", "").trim();
            // A rune drops as "◆ Snake Rune I": the symbol is stripped by the caller, the level kept.
            for (Drop d : values()) {
                if (d.source != source) continue;
                if (s.equalsIgnoreCase(d.title)) return d;
            }
            // The runes without their numeral, the dyes as just the name.
            for (Drop d : values()) {
                if (d.source != source) continue;
                if (d.title.endsWith(" I") && s.equalsIgnoreCase(d.title.substring(0, d.title.length() - 2))) return d;
            }
            return null;
        }
    }

    // ================================================================== the beats

    /** The three blows of the kill, then it dies. */
    public static final long[] HITS = {260, 620, 980};
    /** The killing blow: the boss starts to fall (vanilla's death tip, 20 ticks). */
    public static final long KILLED = 980;
    /** The death tip takes vanilla's 20 ticks. */
    public static final long DEATH_MS = 1000;
    /** The drop's own ending starts. */
    public static final long ENDING = 1400;
    /** The name is stamped this fast, a letter. */
    public static final long LETTER_MS = 45;
    public static final long COUNT_MS = 900;

    private final Drop drop;
    private final long seed;
    private final long arrive, total;
    private long price;
    private String priceSource;

    public SlayerTimeline(Drop drop, long seed) {
        this.drop = drop;
        this.seed = seed;
        this.arrive = ENDING + endingMs(drop.ending);
        this.total = arrive + (drop.tier == Tier.RNGESUS ? 4400 : 3600);
    }

    /** How long each ending takes, from {@link #ENDING} to the showcase. The biggest drops take the longest. */
    public static long endingMs(Ending e) {
        return switch (e) {
            case PLAIN -> 1300;
            case BOOK -> 1500;
            case PARAGON -> 2300;
            case BEHEAD -> 2500;
            case SCYTHE -> 2400;
            case SNAKES -> 2900;
            case MAGGOT -> 3000;
            case SINEW -> 2900;
            case HAND -> 3100;
            // The Tarantula's are set by its endings (render/BroodEndings, pure).
            case COCOON, SWATTER, TALISMAN, VENOM, DARKNESS, WASP, SNAIL, MOSQUITO, EYE, BRICK -> com.sbcs.render.BroodEndings.endingMs(e);
            case WARDEN -> 7600;
            case MATCHA -> 8200;
        };
    }

    public Drop drop() { return drop; }
    public Ending ending() { return drop.ending; }
    public long seed() { return seed; }
    public long arrive() { return arrive; }
    public long total() { return total; }
    /** The price starts counting a little after the name is in. */
    public long countStart() { return arrive + Math.min(900, 250 + LETTER_MS * drop.title.length()); }

    public void price(long coins, String source) {
        price = coins;
        priceSource = source;
    }

    public void price(long coins) {
        price(coins, null);
    }

    public long price() { return price; }
    public String priceSource() { return priceSource; }

    /** Seconds into the ending (0 before it starts). */
    public float endingT(long t) {
        return Math.max(0f, (t - ENDING) / 1000f);
    }

    /** 0..1 through the ending. */
    public float endingK(long t) {
        return clamp((t - ENDING) / (float) (arrive - ENDING));
    }

    /** How far the boss has fallen: vanilla's death tip, sqrt of the time, over 20 ticks. */
    public static float deathTip(long t) {
        if (t < KILLED) return 0f;
        float f = (t - KILLED) / (float) DEATH_MS * 1.6f;
        return Math.min(1f, (float) Math.sqrt(f));
    }

    /** The red hurt flash of each blow (1 at the blow, gone in 250 ms). */
    public static float hurt(long t) {
        float k = 0f;
        for (long h : HITS) if (t >= h && t < h + 250) k = Math.max(k, 1f - (t - h) / 250f);
        return k;
    }

    /** The index of the last blow landed (-1 none). */
    public static int hitIndex(long t) {
        int i = -1;
        for (int k = 0; k < HITS.length; k++) if (t >= HITS[k]) i = k;
        return i;
    }

    /** The drop rising into its showcase: 0 before {@code arrive - 700}, 1 at arrive. */
    public float rise(long t) {
        return ease((t - (arrive - 700)) / 700f);
    }

    public float nameAlpha(long t) {
        return clamp((t - arrive) / 200f) * fadeOut(t);
    }

    public int letters(long t) {
        if (t < arrive) return 0;
        return (int) Math.min(drop.title.length(), 1 + (t - arrive) / LETTER_MS);
    }

    public float lineAlpha(long t) {
        return clamp((t - countStart() + 200) / 300f) * fadeOut(t);
    }

    /** The banner is stamped as the drop arrives. */
    public float bannerStamp(long t) {
        return clamp((t - arrive + 120) / 260f);
    }

    /** Everything fades over the last 500 ms. */
    public float fadeOut(long t) {
        return 1f - clamp((t - (total - 500)) / 500f);
    }

    /** The dim behind it: up in 300 ms, out with everything else. */
    public float dim(long t) {
        return 0.62f * clamp(t / 300f) * fadeOut(t);
    }

    /** A random number in [0, 1) fixed per seed and index. */
    public float rand(int i) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L;
        x ^= x >>> 31;
        x *= 0x94D049BB133111EBL;
        x ^= x >>> 29;
        return (x >>> 40) / (float) (1L << 24);
    }

    public static float clamp(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    public static float ease(float v) {
        v = clamp(v);
        return v * v * (3f - 2f * v);
    }

    /** Overshoots a little and settles. */
    public static float backOut(float v) {
        if (v <= 0f) return 0f;
        if (v >= 1f) return 1f;
        float c = 1.70158f, x = v - 1f;
        return 1f + (c + 1f) * x * x * x + c * x * x;
    }

    /** 0..1 over [from, from + ms]. */
    public static float span(long t, long from, long ms) {
        return clamp((t - from) / (float) ms);
    }
}
