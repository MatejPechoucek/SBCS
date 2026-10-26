package com.sbcs.detect;

import com.sbcs.anim.WaterTimeline.Creature;
import com.sbcs.anim.WaterTimeline.Drop;

import java.util.Locale;

/**
 * The water mythics' chat lines and nametags, read. Pure (tools/WaterDetectTest).
 *
 * <b>Wiki and theory - nothing here has been seen in a real log yet</b> (DropRecorder keeps every such line as
 * {@code fishing_line}, SlayerRecorder every nametag as {@code water_mob}; check the first records):
 * <ul>
 *   <li>Catch lines are the wiki's: "A massive Titanoboa surfaces. Its body stretches as far as the eye can see." and
 *       "The water bubbles and froths. A massive form emerges- you have disturbed the Wiki Tiki! You shall pay the
 *       price." Feesh's rewrite ("A Titanoboa has spawned!") is read too, as for lava.</li>
 *   <li>Drop lines are taken in the lava format - {@code RARE DROP! Titanoboa Shed (+N ✯ Magic Find)}, no brackets -
 *       and in the slayer's, with them; any "... DROP!" prefix.</li>
 *   <li>The dye's line is unknown: any drop line naming it, or a WOW! line naming it and the player.</li>
 *   <li>Charm and loot share as lava fishing's (proven there).</li>
 * </ul>
 *
 * Each creature is a row of {@link #ROWS}: the words its nametag and its catch lines carry. A later water mythic
 * (Nessie) is another row and its own drops, not another class.
 */
public final class WaterDrops {
    private WaterDrops() {}

    /** One creature: the name it goes by, and the words only its own catch line has (any one is enough). */
    record Row(Creature creature, String name, String[] catchWords) {
    }

    static final Row[] ROWS = {
            new Row(Creature.TITANOBOA, "Titanoboa", new String[]{"surfaces", "stretches as far as the eye"}),
            new Row(Creature.WIKI_TIKI, "Wiki Tiki", new String[]{"disturbed the wiki tiki", "you shall pay the price", "bubbles and froths"}),
    };

    private static final String[] PREFIXES = {"RARE DROP!", "VERY RARE DROP!", "CRAZY RARE DROP!", "PRAY TO RNGESUS DROP!",
            "INSANE DROP!", "LEGENDARY DROP!", "PET DROP!", "WOW!"};

    /**
     * A drop line's drop: a win, {@link Drop#BUST} for any other drop, or null for a line that is not a drop (or is
     * someone else's WOW!). {@code self} is the player's name, for the WOW! line; null takes any.
     */
    public static Drop of(String line, String self) {
        if (line == null) return null;
        String s = DianaWatcher.plain(line).trim();
        boolean drop = false;
        for (String p : PREFIXES) {
            if (s.startsWith(p)) {
                drop = true;
                break;
            }
        }
        if (!drop) return null;
        String l = s.toLowerCase(Locale.ROOT);
        if (l.contains("aquamarine dye")) {
            if (s.startsWith("WOW!") && self != null && !s.contains(self)) return null;
            return Drop.AQUAMARINE;
        }
        if (s.startsWith("WOW!")) return null;
        if (l.contains("titanoboa shed")) return Drop.SHED;
        if (l.contains("snake eyes")) return Drop.SNAKE_EYES;
        if (l.contains("troubled bubble")) return Drop.BUBBLE;
        if (l.contains("tiki mask")) return Drop.TIKI_MASK;
        if (l.contains("bobbin' scriptures") || l.contains("bobbin scriptures") || l.contains("bobbin’ scriptures")) return Drop.SCRIPTURES;
        return Drop.BUST;
    }

    /** The creature a nametag names, only while it shows health ("... Titanoboa 30M/45M❤"); never a Laser Totem. */
    public static Creature ofNametag(String name) {
        if (name == null || !name.contains("/") || !name.contains("❤")) return null;
        return named(DianaWatcher.plain(name));
    }

    /** The creature any nametag names, health or not (a followed one keeps its name through an immune phase). */
    public static Creature ofAnyNametag(String name) {
        return name == null ? null : named(DianaWatcher.plain(name));
    }

    /** The creature a text names, or null. The Laser Totems (the Wiki Tiki's, "Laser Totem") are never it. */
    static Creature named(String s) {
        if (s.contains("Totem")) return null;
        for (Row r : ROWS) if (s.contains(r.name)) return r.creature;
        return null;
    }

    /**
     * The creature a catch line announces, or null: the wiki's own line (by its words), or a rewrite naming it with
     * "spawned", "emerges", "surfaces" or "appeared" - never a drop, a charm, a loot share, a death or a shard.
     */
    public static Creature ofCatch(String line) {
        if (line == null) return null;
        String s = DianaWatcher.plain(line).trim();
        if (s.contains("DROP!") || s.startsWith("CHARM!") || s.startsWith("LOOT SHARE") || s.contains("killed by")
                || s.contains("Shard") || s.startsWith("WOW!")) return null;
        Creature c = named(s);
        if (c == null) return null;
        String l = s.toLowerCase(Locale.ROOT);
        for (Row r : ROWS) {
            if (r.creature != c) continue;
            for (String w : r.catchWords) if (l.contains(w)) return c;
        }
        if (l.contains("spawned") || l.contains("emerges") || l.contains("surfaces") || l.contains("appeared") || l.contains("has arrived")) return c;
        return null;
    }

    /** "CHARM! You charmed the Titanoboa and received ...": the creature, or null. */
    public static Creature ofCharm(String line) {
        if (line == null) return null;
        String s = DianaWatcher.plain(line).trim();
        if (!s.startsWith("CHARM!") || !s.contains("You charmed")) return null;
        int recv = s.indexOf(" and received");
        return named(recv > 0 ? s.substring(0, recv) : s);
    }

    /** A loot share line: the creature it names, {@link #SHARE_UNNAMED} when it names none, or null for another line. */
    public static Object ofShare(String line) {
        if (line == null) return null;
        String s = DianaWatcher.plain(line).trim();
        if (!s.startsWith("LOOT SHARE") || !s.contains("for assisting")) return null;
        Creature c = named(s.substring(0, s.indexOf("for assisting")));
        return c != null ? c : SHARE_UNNAMED;
    }

    public static final Object SHARE_UNNAMED = new Object();
}
