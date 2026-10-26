package com.sbcs.anim;

/**
 * "The Seraph's Glyph" - the Voidgloom Seraph's animation (Enderman slayer, tier IV only, the designer's
 * call, as for Hellion). Pure: everything is a function of time, the drop and a seed, so it can be
 * checked offline.
 *
 * In the fight the Seraph "holds a beacon for 2 seconds then throws it" - the Yang Glyph. Here it is
 * the tell. The Seraph (the vanilla enderman, carrying the beacon exactly as an enderman carries a
 * block) stands behind a Malevolent Hitshield of 100 hits; every hit ticks the beacon's stained glass
 * through a fixed cycle - black, white, light blue, magenta, purple - faster and faster. At 0 hits it
 * casts Broken Heart Radiation (12 beams, 4 groups of 3, as in the fight) and the colours flicker; then
 * it slams the beacon into the ground and the beam fires up in the colour that lands. <b>Everything
 * before {@link #REVEAL} is the same for every drop</b> (SeraphTest hashes frames and audio). Black is
 * a bust; any other colour is a win, and the colour is the value tier.
 *
 * Only drops worth 500k or more play it (the designer's floor, judged on live prices by detection).
 */
public final class SeraphTimeline {

    /** The stained glass over the beacon - vanilla's own beam colours for these dyes. */
    public enum Glass {
        BLACK(0x1D1D21), WHITE(0xF9FFFE), LIGHT_BLUE(0x3AB3DA), MAGENTA(0xC74EBD), PURPLE(0x8932B8);

        public final int rgb;

        Glass(int rgb) {
            this.rgb = rgb;
        }
    }

    /** How a win ends. */
    public enum Ending { BUST, PLAIN, CHALICE, ESPRESSO, SKIN, DICE, JACKPOT, EYE, DRAGON, ALTAR, WARP, RUNE, SHARD, IDOL }

    /**
     * What dropped. Odds and prices from the Hypixel SkyBlock wiki (hypixelskyblock.minecraft.wiki) and
     * Coflnet / the bazaar on 2026-10-03: Byzantium Dye 1 in 500,000 (775M), Judgement Core 1 in 1,771
     * (190M), End Stone Idol 1 in 7,085 (43.5M), Exceedingly Rare Ender Artifact Upgrade 1 in 3,542
     * (9.4M), Enchant Rune I 1 in 2,157 (4.9M), Paragon Shard 1 in 400 (2.1M), Summoning Eye 1 in 177
     * (1.55M), Etherwarp Merger 1 in 236 (650k). The four with their own small endings - Chalice
     * (299k), Espresso (150k), Sinful Dice (350k), Void Conqueror skin (75k) - were under the floor
     * that day; they play only if their price rises past it.
     */
    public enum Drop {
        BUST("", 0, Glass.BLACK, Ending.BUST, null, null, null, false, 0),
        ETHERWARP_MERGER("Etherwarp Merger", 0xAA00AA, Glass.WHITE, Ending.WARP, "ETHERWARP_MERGER", "ETHERWARP_MERGER", "Etherwarp Merger", true, 236),
        SUMMONING_EYE("Summoning Eye", 0xAA00AA, Glass.LIGHT_BLUE, Ending.ALTAR, "SUMMONING_EYE", "SUMMONING_EYE", "ender_eye", false, 177),
        PARAGON("Paragon Shard", 0xFF55FF, Glass.LIGHT_BLUE, Ending.SHARD, "SHARD_PARAGON", "SHARD_PARAGON", "Paragon Shard", true, 400),
        ENCHANT_RUNE("Enchant Rune I", 0xAAAAAA, Glass.LIGHT_BLUE, Ending.RUNE, "RUNE_ENCHANT", "ENCHANT_RUNE", "Enchant Rune I", true, 2157),
        BLOOD_CHALICE("Handy Blood Chalice", 0xFFFFFF, Glass.WHITE, Ending.CHALICE, "HANDY_BLOOD_CHALICE", "HANDY_BLOOD_CHALICE", "Handy Blood Chalice", true, 567),
        ESPRESSO("Pocket Espresso Machine", 0xFFFFFF, Glass.WHITE, Ending.ESPRESSO, "POCKET_ESPRESSO_MACHINE", "POCKET_ESPRESSO_MACHINE", "Pocket Espresso Machine", true, 258),
        SINFUL_DICE("Sinful Dice", 0xAA00AA, Glass.WHITE, Ending.DICE, "SINFUL_DICE", "SINFUL_DICE", "Sinful Dice", true, 218),
        VOID_SKIN("Void Conqueror Enderman Skin", 0xAA00AA, Glass.WHITE, Ending.SKIN, "PET_SKIN_ENDERMAN_SLAYER", "PET_SKIN_ENDERMAN_SLAYER", "Void Conqueror Enderman Skin", true, 604),
        ARTIFACT_UPGRADE("Exceedingly Rare Ender Artifact Upgrade", 0xFFAA00, Glass.MAGENTA, Ending.JACKPOT, "EXCEEDINGLY_RARE_ENDER_ARTIFACT_UPGRADER", "EXCEEDINGLY_RARE_ENDER_ARTIFACT_UPGRADER", "Exceedingly Rare Ender Artifact Upgrade", true, 3542),
        END_STONE_IDOL("End Stone Idol", 0xFFAA00, Glass.MAGENTA, Ending.IDOL, "ENDSTONE_IDOL", "ENDSTONE_IDOL", "End Stone Idol", true, 7085),
        JUDGEMENT_CORE("Judgement Core", 0xFFAA00, Glass.PURPLE, Ending.EYE, "JUDGEMENT_CORE", "JUDGEMENT_CORE", "Judgement Core", true, 1771),
        BYZANTIUM_DYE("Byzantium Dye", 0xAA00AA, Glass.PURPLE, Ending.DRAGON, "DYE_BYZANTIUM", "DYE_BYZANTIUM", "Byzantium Dye", true, 500_000);

        public final String title;
        public final int colour;
        public final Glass lands;
        public final Ending ending;
        /** The ID its price is looked up by (bazaar product, else Coflnet's tag). */
        public final String priceId;
        /** Its NEU id, for the preview's skin files. */
        public final String id;
        /** A head skin's name (HeadSkins.SLAYER) when {@link #head}, else a vanilla item icon. */
        public final String art;
        public final boolean head;
        /** About one in this many tier IV bosses; 0 for none. */
        public final int oneIn;

        Drop(String title, int colour, Glass lands, Ending ending, String priceId, String id, String art, boolean head, int oneIn) {
            this.title = title;
            this.colour = colour;
            this.lands = lands;
            this.ending = ending;
            this.priceId = priceId;
            this.id = id;
            this.art = art;
            this.head = head;
            this.oneIn = oneIn;
        }

        public boolean win() {
            return this != BUST;
        }
    }

    // ------------------------------------------------------------------ the beats, ms

    /** The Seraph blinks in out of the portal swirl. */
    public static final long TELEPORT_IN = 250;
    /** Everything is in place and the shield starts. */
    public static final long APPEAR = 700;
    /** The shield's hits: 20 steps of 5 hits, the first 300 ms, each 8% shorter - slow, then a rush. */
    public static final long[] STEPS = steps(20, APPEAR, 300f, 0.92f);
    /** The last hit's end: 0 hits, and Broken Heart Radiation. */
    public static final long HANG = STEPS[STEPS.length - 1] + Math.round(300f * Math.pow(0.92f, STEPS.length - 1));
    /**
     * The Yang Glyph thrown, slow enough to follow (0.2.64: it was too fast to make out): the
     * lasers run 600 ms, then it holds the glyph in the vanilla carry, charging and shaking (it never
     * lifts its arms - vanilla endermen do not), and from {@link #RELEASE} it flies in an arc and lands at
     * the reveal.
     */
    public static final long THROW = HANG + 600, LIFT_MS = 400, RELEASE = THROW + 600, FLIGHT_MS = 460;
    /** The answer. Nothing before this may depend on the drop. */
    public static final long REVEAL = RELEASE + FLIGHT_MS;
    /** A bust is over by this - back to the next boss. */
    public static final long BUST_END = REVEAL + 900;
    public static final long FADE = 400;
    /** The hits at which the Seraph dodges - teleports to another spot - and where to. */
    public static final int[] DODGE_STEPS = {5, 11, 16};
    public static final float[] DODGE_X = {-11f, 10f, 0f};
    /** The fixed cycle of the stained glass, one colour per hit. */
    public static final Glass[] CYCLE = {Glass.BLACK, Glass.WHITE, Glass.LIGHT_BLUE, Glass.MAGENTA, Glass.PURPLE};

    // A win's common beats.
    public static final long SHATTER = REVEAL + 150, DEATH = REVEAL + 200, CRYSTALS_BLOW = REVEAL + 250, POOF = REVEAL + 1200;
    // The jackpot.
    public static final long STUTTER = REVEAL + 400, VANISH = REVEAL + 500, AFTER_FROM = REVEAL + 600, AFTER_STEP = 140,
            IMPLODE = REVEAL + 1800, SHOCK = REVEAL + 2100;
    public static final int AFTERIMAGES = 8;
    // The Judgement Core: "The Eye Opens" (0.2.66).
    public static final long E_FREEZE = REVEAL + 200, E_CRACK = REVEAL + 500, E_BURST = REVEAL + 950, E_FORMED = REVEAL + 1350,
            E_OPEN = REVEAL + 1550, E_OPENED = REVEAL + 1850, E_LEFT = REVEAL + 2050, E_RIGHT = REVEAL + 2400, E_STARE = REVEAL + 2750,
            E_PULSE = REVEAL + 2950, E_PULL = REVEAL + 3150, E_PULLED = REVEAL + 4150, E_RISE = REVEAL + 4250;
    // The Byzantium Dye: "The Dragon's Tribute" (0.2.66). The flight is the baked Blender track,
    // 93 game ticks from D_IN.
    public static final long D_DEEPEN = REVEAL + 200, D_FREEZE = REVEAL + 600, D_IN = REVEAL + 1200, D_LAND = D_IN + 92 * 50,
            D_HEAD = D_LAND + 300, D_BREATH = D_LAND + 800, D_BREATH_END = D_BREATH + 2000, D_POOL = D_BREATH + 300,
            D_POOL_END = D_POOL + 2600, D_BURST = D_BREATH_END + 900, D_RISE = D_BURST + 100;
    /** Byzantium, the dye's own purple. */
    public static final int BYZANTIUM = 0x702963;
    // The Summoning Eye: "The Summoning" (2026-10-26). Eight End Portal frames rise round the glyph's spot as the
    // altar in the Dragon's Nest; seven eyes click in, the dropped eye sets into the eighth, the portal opens for a
    // breath with the dragon's roar far off, and the beam takes the eye back out.
    public static final long A_FRAMES = REVEAL + 900, A_EYES = REVEAL + 1700, A_EYE_STEP = 190, A_SEAT = A_EYES + 7 * A_EYE_STEP + 300,
            A_SEATED = A_SEAT + 450, A_OPEN = A_SEATED + 100, A_CLOSE = A_OPEN + 1500, A_LIFT = A_CLOSE - 200;
    public static final int A_FRAMES_N = 8;
    // The Etherwarp Merger: "Etherwarp" - the drop warps round the arena the way the ability does, a target block lit
    // where it will land, then it is there.
    public static final long W_FROM = REVEAL + 1300, W_STEP = 620, W_AIM = 330;
    public static final int W_JUMPS = 4;
    // The Enchant Rune: "The Runecraft" - an enchanting table rises where the glyph stood, the table's own glyphs
    // stream into it off the fallen Seraph and the crystals, and the rune is written out of them.
    public static final long R_TABLE = REVEAL + 700, R_TABLE_UP = R_TABLE + 500, R_STREAM = REVEAL + 1100, R_STREAM_END = REVEAL + 3300,
            R_FORM = REVEAL + 3400;
    // The Paragon Shard: "Paragon" - both End Crystals crack and burst, and every shard of them spirals in along
    // their beams and is pressed into one.
    public static final long S_CRACK = REVEAL + 500, S_BURST = REVEAL + 1300, S_FUSE = REVEAL + 2700;
    // The End Stone Idol: "Set in Stone" - the Seraph does not fall. It turns to end stone from the feet up, stands
    // as a statue, cracks, and bursts; the idol is what was inside it.
    public static final long I_STONE = REVEAL + 300, I_STONED = REVEAL + 1900, I_CRACK = REVEAL + 2300, I_SHATTER = REVEAL + 3300;
    // The skin.
    public static final long HEAD_OFF = DEATH + 250, HEAD_LANDS = DEATH + 900, SKIN_DONE = DEATH + 2100;
    // The dice: thrown from where the beacon stood.
    public static final long DICE_LAUNCH = REVEAL + 250;

    private static long[] steps(int n, long start, float first, float ratio) {
        long[] out = new long[n];
        float t = start, d = first;
        for (int i = 0; i < n; i++) {
            out[i] = Math.round(t);
            t += d;
            d *= ratio;
        }
        return out;
    }

    private final Drop drop;
    private final long seed;

    public SeraphTimeline(Drop drop, long seed) {
        this.drop = drop;
        this.seed = seed;
    }

    public Drop drop() { return drop; }
    public long seed() { return seed; }
    public Ending ending() { return drop.ending; }

    /** When the drop stands where it is shown, with its name. */
    public long arrive() {
        return switch (drop.ending) {
            case BUST -> BUST_END;
            case PLAIN, CHALICE, ESPRESSO -> REVEAL + 1300;
            case SKIN -> SKIN_DONE + 150;
            case DICE -> DICE_LAUNCH + HellionDice.settleMs() + 250;
            case JACKPOT -> SHOCK + 600;
            case EYE -> E_RISE + 700;
            case DRAGON -> D_RISE + 1000;
            case ALTAR -> A_LIFT + 800;
            case WARP -> W_FROM + W_JUMPS * W_STEP + 250;
            case RUNE -> R_FORM + 900;
            case SHARD -> S_FUSE + 600;
            case IDOL -> I_SHATTER + 900;
        };
    }

    public long total() {
        return switch (drop.ending) {
            case BUST -> BUST_END;
            case PLAIN, SKIN, DICE -> arrive() + 3200;
            case ESPRESSO -> arrive() + 3000;
            case CHALICE -> arrive() + 3800;
            case JACKPOT -> arrive() + 5500;
            case EYE -> arrive() + 4500;
            case DRAGON -> arrive() + 7500;
            case ALTAR, RUNE -> arrive() + 3400;
            case WARP, SHARD -> arrive() + 3200;
            case IDOL -> arrive() + 5000;
        };
    }

    // ------------------------------------------------------------------ the build-up (the same for every drop)

    /** Which hit {@code t} is in, -1 before the shield starts. */
    public static int step(long t) {
        int k = -1;
        for (int i = 0; i < STEPS.length; i++) if (t >= STEPS[i]) k = i;
        return k;
    }

    /** The hits left on the shield, as its nametag shows them: 100, down 5 a hit. */
    public static int hitsLeft(long t) {
        return Math.max(0, 100 - 5 * (step(t) + 1));
    }

    /** A flash on each hit, 1 at the hit and gone in about 150 ms. */
    public static float hit(long t) {
        int k = step(t);
        if (k < 0 || t >= HANG) return 0f;
        return (float) Math.exp(-(t - STEPS[k]) / 80.0);
    }

    /** The glass now: the fixed cycle, a flicker in the hang, then the answer. */
    public Glass glass(long t) {
        if (t >= REVEAL) return drop.lands;
        if (t >= HANG) return CYCLE[(int) ((t / 45) % CYCLE.length)];
        return CYCLE[Math.max(0, step(t)) % CYCLE.length];
    }

    /** Where the Seraph stands (world x) - it dodges on three fixed hits; a blink is instant. */
    public static float seraphX(long t) {
        float x = 0f;
        for (int i = 0; i < DODGE_STEPS.length; i++) if (t >= STEPS[DODGE_STEPS[i]]) x = DODGE_X[i];
        return x;
    }

    /** How angry it looks, 0..1: the jaw drops from the tenth hit on, and wide open in the hang. */
    public static float creepy(long t) {
        if (t < STEPS[10]) return 0f;
        if (t < HANG) return 0.6f * ease((t - STEPS[10]) / 300f);
        return 0.6f + 0.4f * ease((t - HANG) / 150f);
    }

    /** Broken Heart Radiation: 0..1 through the hang, -1 outside it. */
    public static float radiation(long t) {
        if (t < HANG || t >= THROW + 60) return -1f;
        return (t - HANG) / (float) (THROW + 60 - HANG);
    }

    /**
     * The throw: 0 before {@link #THROW}; winding up to {@link #RELEASE} (0..0.5), then the beacon in flight
     * until it lands at the reveal (0.5..1). The same on every run.
     */
    public static float throwK(long t) {
        if (t < THROW) return 0f;
        if (t < RELEASE) return 0.5f * (t - THROW) / (float) (RELEASE - THROW);
        if (t < REVEAL) return 0.5f + 0.5f * (t - RELEASE) / (float) (REVEAL - RELEASE);
        return 1f;
    }

    /** The scene appearing: 0..1. */
    public static float appear(long t) {
        return ease(t / 400f);
    }

    // ------------------------------------------------------------------ after the reveal

    /**
     * The clock the physical things run on - the beam's scroll, the crystals' spin, the particles. The
     * same as {@code t}, but the Eye and the Dragon stop it dead (everything freezes; only the core, the
     * dragon and what they do move, on the real clock), and the
     * jackpot's stutter holds it for a moment.
     */
    public long phys(long t) {
        if (drop.ending == Ending.EYE && t >= E_FREEZE) return E_FREEZE;
        if (drop.ending == Ending.DRAGON && t >= D_FREEZE) return D_FREEZE;
        if (drop.ending == Ending.JACKPOT && t >= STUTTER) {
            if (t < STUTTER + 180) return STUTTER;
            return t - 180;
        }
        return t;
    }

    /** A bust's fade-out of beam, beacon and Seraph: 0..1, -1 before the reveal or on a win. */
    public float bust(long t) {
        if (drop.win() || t < REVEAL) return -1f;
        return Math.min(1f, (t - REVEAL) / (float) (BUST_END - REVEAL - 200));
    }

    /** How tall the beam stands, 0..1 of its full reach (a bust's chokes out). */
    public float beam(long t) {
        if (t < REVEAL) return 0f;
        float up = ease((t - REVEAL) / 140f);
        if (!drop.win()) return up * (1f - ease((t - REVEAL - 120) / 300f));
        // No glyph, no beam: the Eye's bursts off the core, the Dragon's in the paint.
        if (drop.ending == Ending.EYE) return up * (1f - ease((t - E_BURST) / 250f));
        if (drop.ending == Ending.DRAGON) return up * (1f - ease((t - D_BURST) / 250f));
        // Out of the way of what comes next: the statue behind it, the altar round it, the table in its place.
        if (drop.ending == Ending.IDOL) return up * (1f - ease((t - I_STONE) / 400f));
        if (drop.ending == Ending.ALTAR) return up * (1f - ease((t - A_FRAMES - 200) / 500f)) + up * 0.9f * altarBeam(t);
        if (drop.ending == Ending.RUNE) return up * (1f - ease((t - R_TABLE) / 400f));
        if (drop.ending == Ending.WARP) return up * (1f - ease((t - W_FROM) / 300f));
        return up;
    }

    /** The beam's swell as it fires: 1 + a burst. */
    public float beamWidth(long t) {
        if (t < REVEAL) return 1f;
        float k = (t - REVEAL) / 1000f;
        float burst = drop.win() ? 1.2f * (float) Math.exp(-k * 3.5f) * Math.min(1f, k * 12f) : 0f;
        float jackpot = drop.ending == Ending.JACKPOT && t >= SHOCK ? 0.8f * (float) Math.exp(-(t - SHOCK) / 400.0) : 0f;
        return 1f + burst + jackpot + (drop.win() ? 0.4f : 0f);
    }

    /** Whether the beacon still stands: a bust's turns to obsidian and stays; a win's shatters. */
    public boolean beaconStands(long t) {
        if (t < REVEAL) return true;
        // A bust's turns to obsidian and stays; the Eye's cracks away, the Dragon's bursts at the end.
        if (!drop.win()) return true;
        if (drop.ending == Ending.EYE) return t < E_BURST;
        if (drop.ending == Ending.DRAGON) return t < D_BURST;
        return t < SHATTER;
    }

    /** The Seraph's death fall, 0..1 (vanilla: sqrt of the death clock, 90 degrees over a second); -1 if not dying. */
    public float death(long t) {
        if (!drop.win() || t < DEATH) return -1f;
        if (drop.ending == Ending.JACKPOT || drop.ending == Ending.EYE || drop.ending == Ending.DRAGON || drop.ending == Ending.IDOL) return -1f;
        float f = (float) Math.sqrt(Math.max(0f, (t - DEATH) / 1000f * 1.6f));
        return Math.min(1f, f);
    }

    /** Whether the Seraph is still drawn at all. */
    public boolean seraphShown(long t) {
        if (t < TELEPORT_IN) return false;
        if (t < REVEAL) return true;
        if (!drop.win()) return t < REVEAL + 250;
        return switch (drop.ending) {
            case JACKPOT -> t < VANISH;
            case EYE -> t < E_PULLED;
            case DRAGON -> t < D_LAND;
            case IDOL -> t < I_SHATTER;
            default -> t < POOF;
        };
    }

    /** The crystals blowing up: 0..1, -1 before (on the jackpot they go with the shock, on the judgement never). */
    public float crystalsBlow(long t) {
        if (!drop.win() || drop.ending == Ending.EYE || drop.ending == Ending.DRAGON || drop.ending == Ending.SHARD) return -1f;
        long at = drop.ending == Ending.JACKPOT ? SHOCK : drop.ending == Ending.IDOL ? I_SHATTER : CRYSTALS_BLOW;
        if (t < at) return -1f;
        return Math.min(1f, (t - at) / 600f);
    }

    /** A bust's crystals sinking, 0..1, -1 before. */
    public float crystalsSink(long t) {
        if (drop.win() || t < REVEAL + 300) return -1f;
        return Math.min(1f, (t - REVEAL - 300) / 500f);
    }

    /** The drop coming up the beam, 0..1 (plain, chalice, espresso); the others arrive their own way. */
    public float itemRise(long t) {
        if (!drop.win()) return 0f;
        long from;
        switch (drop.ending) {
            case PLAIN, CHALICE, ESPRESSO -> from = REVEAL + 350;
            case JACKPOT -> from = SHOCK + 150;
            case DRAGON -> from = D_RISE;
            case EYE -> from = E_RISE;
            default -> {
                return 0f;
            }
        }
        if (t < from) return 0f;
        return backOut(Math.min(1f, (t - from) / (float) (arrive() - from)));
    }

    /** The camera's pan towards the beacon once it has landed, 0..1 (a win only). */
    public float pan(long t) {
        if (!drop.win() || t < REVEAL) return 0f;
        return ease((t - REVEAL - 300) / 900f);
    }

    public float nameAlpha(long t) {
        return drop.win() ? ease((t - arrive() - 50) / 300f) : 0f;
    }

    public float lineAlpha(long t) {
        return drop.win() ? ease((t - arrive() - 400) / 300f) : 0f;
    }

    /** "PRAY TO RNGESUS DROP!" on the jackpots and the judgement. */
    public boolean banner() {
        return drop.ending == Ending.JACKPOT || drop.ending == Ending.EYE || drop.ending == Ending.DRAGON || drop.ending == Ending.IDOL;
    }

    /** Whether this ending's own spectacle is one of the big three (the score, the big arrive). */
    public boolean big() {
        return banner();
    }

    public float bannerStamp(long t) {
        if (!banner() || t < arrive() - 150) return 0f;
        return Math.min(1f, (t - arrive() + 150) / 180f);
    }

    /** How many letters of the name are stamped: the big ones spell it out. */
    public int letters(long t) {
        if (!banner()) return 999;
        return Math.max(0, (int) ((t - arrive() - 50) / 45) + 1);
    }

    /** When the price starts counting up. */
    public long countStart() {
        return arrive() + 650;
    }

    public static final long COUNT_MS = 1000;
    private long price = -1;

    public long price() { return price; }

    public void price(long coins) { this.price = coins; }

    private String priceSource;

    /** The price and where it came from ("bazaar instant sell", "lowest BIN"), shown small under it (the review, A6). */
    public void price(long coins, String source) {
        this.price = coins;
        this.priceSource = source;
    }

    public String priceSource() { return priceSource; }

    // ---- the jackpot

    /** When afterimage {@code k} blinks in round the beacon. */
    public static long afterAt(int k) {
        return AFTER_FROM + k * AFTER_STEP;
    }

    /** The afterimages imploding into the beam, 0..1, -1 before. */
    public float implode(long t) {
        if (drop.ending != Ending.JACKPOT || t < IMPLODE) return -1f;
        return Math.min(1f, (t - IMPLODE) / (float) (SHOCK - IMPLODE));
    }

    // ---- the Eye (the Judgement Core)

    /** The glass shell cracking, 0..1 until it bursts. */
    public float eyeCrack(long t) {
        if (drop.ending != Ending.EYE || t < E_CRACK) return 0f;
        return Math.min(1f, (t - E_CRACK) / (float) (E_BURST - E_CRACK));
    }

    /** The beacon's core turning into the Judgement Core, 0..1. */
    public float eyeForm(long t) {
        if (drop.ending != Ending.EYE || t < E_BURST) return 0f;
        return ease((t - E_BURST) / (float) (E_FORMED - E_BURST));
    }

    /** How open the eye is: 0 shut, 1 open (three steps of a three-texel slit). */
    public float eyeOpen(long t) {
        if (drop.ending != Ending.EYE || t < E_OPEN) return 0f;
        return Math.min(1f, (t - E_OPEN) / (float) (E_OPENED - E_OPEN));
    }

    /** Where it looks, in texels: 0 ahead, -1 left, +1 right - a glance each way, then the stare. */
    public int eyeLook(long t) {
        if (drop.ending != Ending.EYE || t < E_LEFT) return 0;
        if (t < E_LEFT + 260) return -1;
        if (t < E_RIGHT) return 0;
        if (t < E_RIGHT + 260) return 1;
        return 0;
    }

    /** The heavy pulse as it stares, 0..1 decaying. */
    public float eyePulse(long t) {
        if (drop.ending != Ending.EYE || t < E_PULSE) return 0f;
        return (float) Math.exp(-(t - E_PULSE) / 260.0);
    }

    /** The Seraph and the crystals pulled into the pupil, 0..1. */
    public float eyePull(long t) {
        if (drop.ending != Ending.EYE || t < E_PULL) return 0f;
        return Math.min(1f, (t - E_PULL) / (float) (E_PULLED - E_PULL));
    }

    // ---- the Dragon (the Byzantium Dye)

    /** The beam deepening from purple to Byzantium, 0..1. */
    public float deepen(long t) {
        if (drop.ending != Ending.DRAGON || t < D_DEEPEN) return 0f;
        return ease((t - D_DEEPEN) / 500f);
    }

    /** How grey the stopped world is, 0..1. */
    public float grey(long t) {
        if (drop.ending != Ending.DRAGON || t < D_FREEZE) return 0f;
        return ease((t - D_FREEZE) / 500f);
    }

    /** The dragon's own clock, ms since it appeared (-1 before). */
    public long dragonMs(long t) {
        if (drop.ending != Ending.DRAGON || t < D_IN) return -1;
        return t - D_IN;
    }

    /** The paint spreading from the beacon, 0..1 of its reach. */
    public float paint(long t) {
        if (drop.ending != Ending.DRAGON || t < D_POOL) return 0f;
        return ease((t - D_POOL) / (float) (D_POOL_END - D_POOL));
    }

    /** The rearing roar once the dye stands, 0..1. */
    public float rear(long t) {
        if (drop.ending != Ending.DRAGON || t < arrive() - 200) return 0f;
        return ease((t - arrive() + 200) / 700f);
    }

    // ---- the Summoning (the Summoning Eye)

    /** Frame {@code k}'s rise out of the ground, 0..1 - staggered round the ring. */
    public float altarFrame(long t, int k) {
        if (drop.ending != Ending.ALTAR) return 0f;
        // Up out of the ground in turn, and back down once the eye is out, to leave the showcase clear.
        return backOut(Math.min(1f, Math.max(0f, (t - A_FRAMES - k * 70L) / 420f))) * (1f - ease((t - A_LIFT - 300 - k * 40L) / 500f));
    }

    /** Whether frame {@code k} has its eye: seven click in, the eighth is the drop's. */
    public boolean altarEye(long t, int k) {
        if (drop.ending != Ending.ALTAR) return false;
        if (k < A_FRAMES_N - 1) return t >= A_EYES + k * A_EYE_STEP;
        return t >= A_SEATED && t < A_LIFT;
    }

    /** The portal open in the middle of the ring, 0..1 and back. */
    public float altarPortal(long t) {
        if (drop.ending != Ending.ALTAR || t < A_OPEN) return 0f;
        return ease((t - A_OPEN) / 250f) * (1f - ease((t - A_CLOSE + 300) / 300f));
    }

    /** The beam coming back for the eye as it lifts out of its frame. */
    public float altarBeam(long t) {
        if (drop.ending != Ending.ALTAR || t < A_LIFT - 250) return 0f;
        return ease((t - A_LIFT + 250) / 300f);
    }

    // ---- Etherwarp (the Etherwarp Merger)

    /** Which warp {@code t} is in: -1 before the first aim, then 0..W_JUMPS-1 (the last lands at the showcase). */
    public int warp(long t) {
        if (drop.ending != Ending.WARP || t < W_FROM) return -1;
        return (int) Math.min(W_JUMPS - 1, (t - W_FROM) / W_STEP);
    }

    /** The target block lit for warp {@code k}, 0..1 (its aim, then gone as it lands). */
    public float warpAim(long t, int k) {
        long at = W_FROM + k * W_STEP;
        if (drop.ending != Ending.WARP || t < at || t >= at + W_AIM + 60) return 0f;
        return ease((t - at) / 120f);
    }

    /** When warp {@code k} lands. */
    public static long warpAt(int k) {
        return W_FROM + k * W_STEP + W_AIM;
    }

    // ---- the Runecraft (the Enchant Rune)

    /** The table rising out of the ground, 0..1. */
    public float runeTable(long t) {
        if (drop.ending != Ending.RUNE || t < R_TABLE) return 0f;
        return backOut(Math.min(1f, (t - R_TABLE) / (float) (R_TABLE_UP - R_TABLE)));
    }

    /** How thick the stream of glyphs is, 0..1 (it thickens, then stops when the rune forms). */
    public float runeStream(long t) {
        if (drop.ending != Ending.RUNE || t < R_STREAM || t >= R_FORM + 200) return 0f;
        return ease((t - R_STREAM) / (float) (R_STREAM_END - R_STREAM));
    }

    /** The rune written out of the glyphs above the table, 0..1. */
    public float runeForm(long t) {
        if (drop.ending != Ending.RUNE || t < R_FORM) return 0f;
        return ease((t - R_FORM) / 400f);
    }

    // ---- Paragon (the Paragon Shard)

    /** The crystals cracking before they burst, 0..1. */
    public float shardCrack(long t) {
        if (drop.ending != Ending.SHARD || t < S_CRACK) return 0f;
        return Math.min(1f, (t - S_CRACK) / (float) (S_BURST - S_CRACK));
    }

    /** The shards' spiral in from the burst to the fuse, 0..1; -1 before the burst. */
    public float shardPull(long t) {
        if (drop.ending != Ending.SHARD || t < S_BURST) return -1f;
        return Math.min(1f, (t - S_BURST) / (float) (S_FUSE - S_BURST));
    }

    /** Whether the End Crystals are gone (burst into shards). */
    public boolean crystalsGone(long t) {
        return drop.ending == Ending.SHARD && t >= S_BURST;
    }

    // ---- Set in Stone (the End Stone Idol)

    /** How far up the Seraph the stone has crept, 0..1 of its height (a little past, so the head is taken). */
    public float stone(long t) {
        if (drop.ending != Ending.IDOL || t < I_STONE) return 0f;
        return 1.15f * ease((t - I_STONE) / (float) (I_STONED - I_STONE));
    }

    /** The statue cracking, 0..1 until it bursts. */
    public float statueCrack(long t) {
        if (drop.ending != Ending.IDOL || t < I_CRACK) return 0f;
        return Math.min(1f, (t - I_CRACK) / (float) (I_SHATTER - I_CRACK));
    }

    /** The idol rising out of the rubble, 0..1. */
    public float idolRise(long t) {
        if (drop.ending != Ending.IDOL || t < I_SHATTER) return 0f;
        return backOut(Math.min(1f, (t - I_SHATTER) / (float) (arrive() - I_SHATTER)));
    }

    // ---- everything

    public float master(long t) {
        return ease(t / 200f) * (1f - ease((t - (total() - FADE)) / (float) FADE));
    }

    public float dim(long t) {
        return 0.8f * ease(t / 250f) * (1f - ease((t - (total() - FADE)) / (float) FADE));
    }

    public static float ease(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    public static float backOut(float t) {
        t = Math.max(0f, Math.min(1f, t));
        float c = 1.6f, u = t - 1f;
        return 1f + (c + 1f) * u * u * u + c * u * u;
    }
}
